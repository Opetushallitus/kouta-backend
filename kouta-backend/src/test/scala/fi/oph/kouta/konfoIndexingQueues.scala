package fi.oph.kouta

import cloud.localstack.Localstack.{INSTANCE => localstack}
import cloud.localstack.docker.annotation.LocalstackDockerConfiguration
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.sqs.model.{Message, PurgeQueueRequest, ReceiveMessageRequest}
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSClientBuilder}
import org.scalactic.source
import org.scalactic.source.Position
import org.scalatest.concurrent.{Eventually, PatienceConfiguration}
import org.scalatest.enablers.Retrying
import org.scalatest.time.SpanSugar._
import org.scalatest.time.{Nanoseconds, Span}
import org.scalatest.{Assertion, BeforeAndAfterAll, BeforeAndAfterEach, Suite}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.Try

trait KonfoIndexingQueues extends BeforeAndAfterAll with BeforeAndAfterEach with PatienceConfiguration {
  this: Suite =>

  def dockerConfig: LocalstackDockerConfiguration = {
    LocalstackDockerConfiguration.builder()
      .randomizePorts(false)
      .pullNewImage(false)
      .imageTag("1.0.4")
      .build()
  }

  private val queueNames: Seq[String] = Seq("koutaIndeksoijaPriority")
  lazy val indexingQueue: String = getQueue("koutaIndeksoijaPriority")
  val awsTestCreds = new BasicAWSCredentials("test", "test");
  val sqs: AmazonSQS = AmazonSQSClientBuilder.standard()
    .withEndpointConfiguration(new EndpointConfiguration(
      "http://localhost:4566",
      Regions.EU_WEST_1.getName()))
    .withCredentials(new AWSStaticCredentialsProvider(awsTestCreds))
    .build()

  override def beforeAll(): Unit = {
    super.beforeAll()
    localstack.startup(dockerConfig)
    createAndVerifyQueues() // so if test creates data in beforeAll block we don't get errors from them
  }

  override def beforeEach(): Unit = {
    deleteAndPurgeQueues()
    super.beforeEach()
    createAndVerifyQueues()
  }

  private def createAndVerifyQueues(retry: Int = 3): Unit = {
    try {
      queueNames.foreach(sqs.createQueue)
      waitUntil("All queues are created.") {
        queueNames.forall(q => Try(sqs.getQueueUrl(q)).isSuccess)
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
    super.afterEach()
  }

  def deleteAndPurgeQueues(): Unit = {
    queueNames.foreach(q =>
      Try(sqs.getQueueUrl(q))
        .map(_.getQueueUrl)
        .map { url =>
          sqs.purgeQueue(new PurgeQueueRequest(url))
          sqs.deleteQueue(url)
        }
    )
    waitUntil("All queues are deleted") {
      sqs.listQueues.getQueueUrls.size == 0
    }
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
    ).getMessages.asScala
  }

  def getQueue(name: String): String = sqs.getQueueUrl(name).getQueueUrl
}


trait EventuallyMessages extends Eventually {
  this: KonfoIndexingQueues with PatienceConfiguration =>

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = 3.seconds, interval = 100.microseconds)

  def eventuallyMessages(queue: String)
                        (check: Seq[String] => Assertion)
                        (implicit patienceConfig: PatienceConfig, retrying: Retrying[Assertion], pos: Position): Seq[String] = {
    eventually {
      val received = receiveFromQueue(queue)
      val messages = received.map(_.getBody)

      check(messages)
      received.map(_.getReceiptHandle).foreach(sqs.deleteMessage(queue, _))
      messages
    }
  }

  def eventuallyIndexingMessages(check: Seq[String] => Assertion)
                                (implicit patienceConfig: PatienceConfig, retrying: Retrying[Assertion], pos: Position): Seq[String] = {
    eventuallyMessages(indexingQueue)(check)(patienceConfig, retrying, pos)
  }
}

trait WaitIfFails {
  this: PatienceConfiguration =>

  def waitIfFails[T](fun: => T)(implicit config: PatienceConfig, pos: source.Position): T = {
    val startNanos = System.nanoTime
    val initialInterval = Span(config.interval.totalNanos * 0.1, Nanoseconds) // config.interval scaledBy 0.1

    def checkIfStillOk(): T = fun

    @tailrec
    def tryTryAgain(attempt: Int): T = {
      val duration = System.nanoTime - startNanos
      if (duration < config.timeout.totalNanos) {
        checkIfStillOk()

        // For first interval, we wake up every 1/10 of the interval.  This is mainly for optimization purpose.
        val sleepTime = if (duration < config.interval.totalNanos) initialInterval else config.interval
        Thread.sleep(sleepTime.millisPart, sleepTime.nanosPart)

        tryTryAgain(attempt + 1)
      } else {
        checkIfStillOk()
      }
    }

    tryTryAgain(1)
  }
}
