package fi.oph.kouta.indexing

import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.sqs.AmazonSQSClient
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.indexing.indexing._
import fi.vm.sade.utils.slf4j.Logging
import io.atlassian.aws.AmazonClientConnectionDef
import io.atlassian.aws.sqs.SQSClient

import scala.util.{Failure, Success, Try}

package object indexing {
  type Priority = Int
  val HighPriority: Priority = 1

  type IndexType = String
  val IndexTypeKoulutus: IndexType = "koulutukset"
  val IndexTypeToteutus: IndexType = "toteutukset"
  val IndexTypeHaku: IndexType = "haut"
  val IndexTypeHakukohde: IndexType = "hakukohteet"
  val IndexTypeValintaperuste: IndexType = "valintaperusteet"
  val IndexTypeSorakuvaus: IndexType = "sorakuvaukset"
  val IndexTypeOppilaitos: IndexType = "oppilaitokset"
}

object SqsService extends Logging {

  private val config = KoutaConfigurationFactory.configuration.indexingConfiguration

  private val sqsClient: AmazonSQSClient = SQSClient.create(
    config = Some( AmazonClientConnectionDef.default.copy(
      maxErrorRetry = Some(5),
      endpointUrl = config.endpoint,
      region = config.region.map(RegionUtils.getRegion))))

  private val queues = {
    logger.info(s"""Got priority queue name '${config.priorityQueue}' from oph configuration""")
    val queueUrl = sqsClient.getQueueUrl(config.priorityQueue).getQueueUrl
    logger.info(s"""AWS queue url for priority queue in current environment is '$queueUrl'""")
    Map(HighPriority -> sqsClient.getQueueUrl(config.priorityQueue).getQueueUrl)
  }

  private def createMessage(stuff: Map[IndexType, Seq[String]]): Either[Throwable, String] = {
    import org.json4s.JsonDSL._
    import org.json4s.jackson.JsonMethods.{compact, render}
    Try(compact(render(stuff))).recoverWith {
      case t:Throwable =>
        logger.error(s"Unable to create SQS message for oids [${stuff.values.flatten.mkString(",")}]", t)
        Failure(t)
    }.toEither
  }

  def sendMessage(priority: Priority, message: String): Either[Throwable, String] =
    Try(sqsClient.sendMessage(queues(priority), message)) match {
      case Success(r) => Right(r.getMessageId)
      case Failure(t) =>
        logger.error(s"Got exception from SQS queue. Unable to index message ${message}.", t)
        Left(t)
    }

  def addToQueue(priority: Priority, stuff: Map[IndexType, Seq[String]]): Either[Throwable, String] = {
    for {
      message <- createMessage(stuff)
      messageId <- sendMessage(priority, message)
    } yield messageId
  }
}

object SqsInTransactionService extends SqsInTransactionService

abstract class SqsInTransactionService extends Logging {

  import slick.dbio.DBIO

  def toSQSQueue(priority: Priority, index: IndexType, maybeValue: Option[String]): DBIO[_] =
    maybeValue match {
      case Some(value) => toSQSQueue(priority, index, value)
      case None        => DBIO.successful(true)
    }

  def toSQSQueue(priority: Priority, index: IndexType, value: String): DBIO[_] =
    toSQSQueue(priority, index, Seq(value))

  def toSQSQueue(priority: Priority, index: IndexType, values: Seq[String]): DBIO[_] =
    toSQSQueue(priority, Map(index -> values))

  def toSQSQueue(priority: Priority, values: Map[IndexType, Seq[String]]): DBIO[_] = {
    SqsService.addToQueue(priority, values) match {
      case Left(t) => DBIO.failed(t)
      case Right(s) => DBIO.successful(s)
    }
  }
}
