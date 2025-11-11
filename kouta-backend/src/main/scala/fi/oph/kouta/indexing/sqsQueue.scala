package fi.oph.kouta.indexing

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.indexing.indexing._
import fi.oph.kouta.logging.Logging
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.{GetQueueUrlRequest, SendMessageRequest}

import java.net.URI
import scala.util.{Failure, Success, Try}

package object indexing {
  type Priority = Int
  val HighPriority: Priority = 1

  type IndexType = String
  val IndexTypeKoulutus: IndexType       = "koulutukset"
  val IndexTypeToteutus: IndexType       = "toteutukset"
  val IndexTypeHaku: IndexType           = "haut"
  val IndexTypeHakukohde: IndexType      = "hakukohteet"
  val IndexTypeValintaperuste: IndexType = "valintaperusteet"
  val IndexTypeSorakuvaus: IndexType     = "sorakuvaukset"
  val IndexTypeOppilaitos: IndexType     = "oppilaitokset"
}

object SqsService extends Logging {

  private val config = KoutaConfigurationFactory.configuration.indexingConfiguration

  var sqsClient: SqsClient = {
    val retryStrategy = AwsRetryStrategy.standardRetryStrategy().toBuilder.maxAttempts(5).build()
    val sqsClientBuilder = SqsClient
      .builder()
      .region(config.region.map(Region.of).getOrElse(Region.EU_WEST_1))
      .overrideConfiguration(o => o.retryStrategy(retryStrategy))

    config.endpoint match {
      case Some(endpoint) =>
        val endpointURI = new URI(endpoint)
        sqsClientBuilder.endpointOverride(endpointURI)
      case None =>
    }

    sqsClientBuilder.build()
  }

  private lazy val queues = {
    logger.info(s"""Got priority queue name '${config.priorityQueue}' from oph configuration""")
    val getQueueUrlRequest = GetQueueUrlRequest.builder().queueName(config.priorityQueue).build()
    val queueUrl = sqsClient.getQueueUrl(getQueueUrlRequest).queueUrl()
    logger.info(s"""AWS queue url for priority queue in current environment is '$queueUrl'""")
    Map(HighPriority -> queueUrl)
  }

  private def createMessage(stuff: Map[IndexType, Seq[String]]): Either[Throwable, String] = {
    import org.json4s.JsonDSL._
    import org.json4s.jackson.JsonMethods.{compact, render}
    Try(compact(render(stuff))).recoverWith { case t: Throwable =>
      logger.error(s"Unable to create SQS message for oids [${stuff.values.flatten.mkString(",")}]", t)
      Failure(t)
    }.toEither
  }

  private def sendMessage(priority: Priority, message: String): Either[Throwable, String] = {
    val sendMessageRequest = SendMessageRequest
      .builder()
      .queueUrl(queues(priority))
      .messageBody(message)
      .build()

    Try(sqsClient.sendMessage(sendMessageRequest)) match {
      case Success(r) => Right(r.messageId)
      case Failure(t) =>
        logger.error(s"Got exception from SQS queue. Unable to index message $message.", t)
        Left(t)
    }
  }

  def addToQueue(priority: Priority, stuff: Map[IndexType, Seq[String]]): Either[Throwable, String] = {
    for {
      message   <- createMessage(stuff)
      messageId <- sendMessage(priority, message)
    } yield messageId
  }
}

object SqsInTransactionService extends SqsInTransactionService

abstract class SqsInTransactionService extends Logging {
  def toSQSQueue(priority: Priority, index: IndexType, maybeValue: Option[String]): List[String] =
    maybeValue match {
      case Some(value) => toSQSQueue(priority, index, value)
      case None        => List.empty
    }

  def toSQSQueue(priority: Priority, index: IndexType, value: String): List[String] =
    toSQSQueue(priority, index, Seq(value))

  def toSQSQueue(priority: Priority, index: IndexType, values: Seq[String]): List[String] =
    toSQSQueue(priority, Map(index -> values))

  def toSQSQueue(priority: Priority, values: Map[IndexType, Seq[String]]): List[String] = {
    logger.info(s"Sending a message to Kouta-indeksoija SQS queue: $values with priority $priority")
    SqsService.addToQueue(priority, values) match {
      case Left(t) =>
        logger.error(
          s"SQS queue message to Kouta-indeksoija with values: $values and priority $priority failed: ${t.getStackTrace}"
        )
        List("varoitukset.indeksointiEpaonnistui")
      case Right(_) =>
        logger.info(s"SQS queue message to Kouta-indeksoija with values: $values and priority $priority success.")
        List.empty
    }
  }
}
