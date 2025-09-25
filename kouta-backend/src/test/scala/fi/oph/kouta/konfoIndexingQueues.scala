package fi.oph.kouta

import org.scalactic.source
import org.scalactic.source.Position
import org.scalatest.concurrent.{Eventually, PatienceConfiguration}
import org.scalatest.enablers.Retrying
import org.scalatest.time.SpanSugar._
import org.scalatest.time.{Nanoseconds, Span}
import org.scalatest.{Assertion, BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.localstack.LocalStackContainer.Service
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.Try

trait KonfoIndexingQueues extends BeforeAndAfterAll with BeforeAndAfterEach with PatienceConfiguration {
  this: Suite =>

  val localstackImage: DockerImageName = DockerImageName.parse("localstack/localstack:3.5.0")
  val localstack: LocalStackContainer = new LocalStackContainer(localstackImage)
    .withServices(Service.SQS)
    .withEnv("SQS_ENDPOINT_STRATEGY", "dynamic")

  private val queueNames: Seq[String]   = Seq("koutaIndeksoijaPriority")
  lazy val indexingQueue: String        = getQueue("koutaIndeksoijaPriority")
  val awsTestCreds: AwsBasicCredentials = AwsBasicCredentials.create("test", "test")

  var sqs: SqsClient = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    localstack.start()

    val credentials               = AwsBasicCredentials.create("test", "test")
    val staticCredentialsProvider = StaticCredentialsProvider.create(credentials)
    sqs = SqsClient
      .builder()
      .endpointOverride(localstack.getEndpoint)
      .region(Region.EU_WEST_1)
      .credentialsProvider(staticCredentialsProvider)
      .build()

    createAndVerifyQueues() // so if test creates data in beforeAll block we don't get errors from them
  }

  override def beforeEach(): Unit = {
    deleteAndPurgeQueues()
    super.beforeEach()
    createAndVerifyQueues()
  }

  private def createAndVerifyQueues(retry: Int = 3): Unit = {
    try {
      queueNames.foreach(q => {
        val createQueueRequest = CreateQueueRequest.builder().queueName(q).build()
        sqs.createQueue(createQueueRequest)
      })
      waitUntil("All queues are created.") {
        queueNames.forall(q => {
          val getQueueRequest = GetQueueUrlRequest.builder().queueName(q).build()
          Try(sqs.getQueueUrl(getQueueRequest)).isSuccess
        })
      }
    } catch {
      case e: Exception if retry > 0 =>
        // Localstack seems to be little bit unreliable and sometimes get Bad Gateway error, restart instance
        localstack.stop()
        localstack.start()

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
    queueNames.foreach(q => {
      val getQueueRequest = GetQueueUrlRequest.builder().queueName(q).build()
      Try(sqs.getQueueUrl(getQueueRequest))
        .map(_.queueUrl())
        .map { url =>
          val purgeQueueRequest = PurgeQueueRequest.builder().queueUrl(url).build()
          sqs.purgeQueue(purgeQueueRequest)
          val deleteQueueRequest = DeleteQueueRequest.builder().queueUrl(url).build()
          sqs.deleteQueue(deleteQueueRequest)
        }
    })
    waitUntil("All queues are deleted") {
      sqs.listQueues.queueUrls.size == 0
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
    val receiveMessageRequest =
      ReceiveMessageRequest.builder().queueUrl(queue).maxNumberOfMessages(10).visibilityTimeout(0).build()
    sqs.receiveMessage(receiveMessageRequest).messages().asScala
  }

  def getQueue(name: String): String = {
    val getQueueUrlRequest = GetQueueUrlRequest.builder().queueName(name).build()
    sqs.getQueueUrl(getQueueUrlRequest).queueUrl
  }
}

trait EventuallyMessages extends Eventually {
  this: KonfoIndexingQueues with PatienceConfiguration =>

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = 3.seconds, interval = 100.microseconds)

  def eventuallyMessages(queue: String)
                        (check: Seq[String] => Assertion)
                        (implicit patienceConfig: PatienceConfig, retrying: Retrying[Assertion], pos: Position): Seq[String] = {
    eventually {
      val received = receiveFromQueue(queue)
      val messages = received.map(_.body)

      check(messages)
      received
        .map(_.receiptHandle)
        .foreach(receiptHandle => {
          val deleteMessageRequest = DeleteMessageRequest.builder().queueUrl(queue).receiptHandle(receiptHandle).build()
          sqs.deleteMessage(deleteMessageRequest)
        })
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
