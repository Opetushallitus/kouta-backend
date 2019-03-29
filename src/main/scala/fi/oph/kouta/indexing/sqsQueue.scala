package fi.oph.kouta.indexing

import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.sqs.AmazonSQSClient
import io.atlassian.aws.sqs.SQSClient
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.indexing.indexing._
import fi.vm.sade.utils.slf4j.Logging
import io.atlassian.aws.AmazonClientConnectionDef
import slick.dbio.DBIO

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
}

object SqsService extends Logging {

  private val config = KoutaConfigurationFactory.configuration.indexingConfiguration

  private val sqsClient: AmazonSQSClient = SQSClient.create(
    config = Some( AmazonClientConnectionDef.default.copy(
      maxErrorRetry = Some(5),
      endpointUrl = config.endpoint,
      region = config.region.map(RegionUtils.getRegion))))

  private val queues = Map(HighPriority -> sqsClient.getQueueUrl(config.priorityQueue).getQueueUrl)

  private def createMessage(stuff: Map[IndexType, Seq[String]]): Either[Throwable, String] = {
    import org.json4s.jackson.JsonMethods.{compact, render}
    import org.json4s.JsonDSL._
    Try(compact(render(stuff))) match {
      case Success(message) => Right(message)
      case Failure(t) => {
        logger.error(s"Unable to create SQS message for oids [${stuff.values.flatten.mkString(",")}]", t)
        Left(t)
      }
    }
  }

  def sendMessage(priority: Priority, message: String): Either[Throwable, String] =
    Try(sqsClient.sendMessage(queues(priority), message)) match {
      case Success(r) => Right(r.getMessageId)
      case Failure(t) => {
        logger.error(s"Got exception from SQS queue. Unable to index message ${message}.", t)
        Left(t)
      }
    }

  def addToQueue(priority: Priority, stuff: Map[IndexType, Seq[String]]): Either[Throwable, String] = {
    for {
      message <- createMessage(stuff).right
      messageId <- sendMessage(priority, message).right
    } yield messageId
  }
}

trait SQSQueueDAO extends Logging {

  def toSQSQueue(priority: Priority, index: IndexType, value: String): DBIO[Unit] =
    toSQSQueue(priority, index, Seq(value))

  def toSQSQueue(priority: Priority, index: IndexType, values: Seq[String]): DBIO[Unit] =
    toSQSQueue(priority, Map(index -> values))

  def toSQSQueue(priority: Priority, values: Map[IndexType, Seq[String]]): DBIO[Unit] = {
    SqsService.addToQueue(priority, values) match {
      case Left(t) => DBIO.failed(t)
      case Right(_) => DBIO.successful()
    }
  }
}
