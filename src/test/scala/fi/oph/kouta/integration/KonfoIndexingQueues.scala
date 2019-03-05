package fi.oph.kouta.integration

import scala.collection.JavaConverters._
import scala.util.Try

import cloud.localstack.docker.LocalstackDocker.{INSTANCE => localstack}
import cloud.localstack.docker.annotation.LocalstackDockerConfiguration
import com.amazonaws.services.sqs.model.{Message, ReceiveMessageRequest}
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
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    queueNames foreach { sqs.createQueue }
  }

  override def afterAll(): Unit = {
    localstack.stop()
    super.afterAll()
  }

  override def afterEach(): Unit = {
    queueNames foreach { q =>
      Try { sqs.getQueueUrl(q) }
        .map { _.getQueueUrl }
        .map { sqs.deleteQueue }
    }
    super.afterEach()
  }

  lazy val sqs = {
    SQSClient.withEndpoint(localstack.getEndpointSQS)
  }

  def receiveFromQueue(queue: String): Seq[Message] = {
    sqs.receiveMessage(new ReceiveMessageRequest(queue).withMaxNumberOfMessages(10))
      .getMessages
      .asScala
  }

  def getQueue(name: String): String = sqs.getQueueUrl(name).getQueueUrl
}
