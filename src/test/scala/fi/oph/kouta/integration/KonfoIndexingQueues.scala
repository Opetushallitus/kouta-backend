package fi.oph.kouta.integration

import scala.collection.JavaConverters._
import scala.util.Try

import cloud.localstack.docker.LocalstackDocker.{INSTANCE => localstack}
import cloud.localstack.docker.annotation.LocalstackDockerConfiguration
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.{Message, PurgeQueueRequest, ReceiveMessageRequest}
import io.atlassian.aws.sqs.SQSClient
import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait KonfoIndexingQueues extends BeforeAndAfterAll with BeforeAndAfterEach with PatienceConfiguration {
  this: Suite =>

  def dockerConfig: LocalstackDockerConfiguration = {
    LocalstackDockerConfiguration.builder()
      .randomizePorts(false)
      .environmentVariables(mapAsJavaMap(Map("SERVICES" -> "sqs")))
      .build()
  }

  private val queueNames: Seq[String] = Seq("konfoindeksoijapriority")
  lazy val indexingQueue: String = getQueue("konfoindeksoijapriority")
  lazy val sqs: AmazonSQSClient = SQSClient.withEndpoint(localstack.getEndpointSQS)

  override def beforeAll(): Unit = {
    super.beforeAll()
    localstack.startup(dockerConfig)
    createAndVerifyQueues() // so if test creates data in beforeAll block we don't get errors from them
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    createAndVerifyQueues()
  }

  private def createAndVerifyQueues(retry: Int = 3): Unit = {
    try {
      queueNames foreach { sqs.createQueue }
      waitUntil("All queues are created.") {
        queueNames forall { q => Try { sqs.getQueueUrl(q) }.isSuccess }
      }
    } catch {
      case e: Exception if retry > 0 =>
        // Localstack seems to be little bit unreliable and sometimes get Bad Gateway error, restart instance
        localstack.stop()
        localstack.startup(dockerConfig)

        createAndVerifyQueues(retry - 1)
      case e: Exception => throw new Exception(s"Failed to create test queues. ${e.getMessage}")
    }
  }

  override def afterAll(): Unit = {
    localstack.stop()
    super.afterAll()
  }

  override def afterEach(): Unit = {
    queueNames foreach { q =>
      Try { sqs.getQueueUrl(q) }
        .map { _.getQueueUrl }
        .map { url =>
          sqs.purgeQueue(new PurgeQueueRequest(url))
          sqs.deleteQueue(url)
        }
    }
    waitUntil("All queues are deleted") { sqs.listQueues.getQueueUrls.size == 0 }

    super.afterEach()
  }

  def waitUntil(message: String)(condition: => Boolean)(implicit patience: PatienceConfig): Unit = {
    val end = System.currentTimeMillis + patience.timeout.millisPart
    while (System.currentTimeMillis < end) {
      if (condition) return
      else Thread.sleep(patience.interval.millisPart)
    }
    throw new Exception(s"Waiting for condition failed. '$message' was not true in ${patience.timeout.millisPart} ms.")
  }


  def receiveFromQueue(queue: String): Seq[Message] = {
    sqs.receiveMessage(new ReceiveMessageRequest(queue)
      .withMaxNumberOfMessages(10)
      .withVisibilityTimeout(0)
    )
      .getMessages
      .asScala
  }

  def getQueue(name: String): String = sqs.getQueueUrl(name).getQueueUrl
}
