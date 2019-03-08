package fi.oph.kouta.integration

import scala.collection.JavaConverters._
import scala.util.Try

import cloud.localstack.docker.LocalstackDocker.{INSTANCE => localstack}
import cloud.localstack.docker.annotation.LocalstackDockerConfiguration
import com.amazonaws.services.sqs.model.{Message, PurgeQueueRequest, ReceiveMessageRequest}
import io.atlassian.aws.sqs.SQSClient
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

trait KonfoIndexingQueues extends BeforeAndAfterAll with BeforeAndAfterEach {
  this: Suite =>

  def dockerConfig: LocalstackDockerConfiguration = {
    LocalstackDockerConfiguration.builder()
      .randomizePorts(false)
      .environmentVariables(mapAsJavaMap(Map("SERVICES" -> "sqs")))
      .build()
  }

  private val queueNames: Seq[String] = Seq("konfoindeksoijapriority")
  lazy val indexingQueue = getQueue("konfoindeksoijapriority")

  override def beforeAll(): Unit = {
    super.beforeAll()
    localstack.startup(dockerConfig)
    createAndVerifyQueues() // so if test creates data in beforeAll block we don't get errors from them
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    createAndVerifyQueues()
  }

  private def createAndVerifyQueues(): Unit = {
    import org.scalatest.Matchers._

    queueNames foreach { sqs.createQueue }

    // there seems to be some random problem connecting to Localstack, verify and fail here
    sqs.listQueues().getQueueUrls should have size queueNames.size
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
    super.afterEach()
  }

  lazy val sqs = SQSClient.withEndpoint(localstack.getEndpointSQS)

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
