package fi.oph.kouta.indexing

import com.amazonaws.ClientConfiguration
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.{Region, RegionUtils, Regions}
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSClientBuilder}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.indexing.indexing._
import fi.vm.sade.utils.slf4j.Logging

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

  private val sqsClient: AmazonSQS = {
    val clientBuilder = AmazonSQSClientBuilder.standard().withClientConfiguration(new ClientConfiguration().withMaxErrorRetry(5))
    val endpoint = config.endpoint
    val region = config.region.getOrElse(Regions.EU_WEST_1.getName())

    endpoint match {
      case Some(endpoint) => clientBuilder.setEndpointConfiguration(
        new EndpointConfiguration(
          endpoint,
          region))
      case None => case Some(region) => clientBuilder.setRegion(region)
    }

    clientBuilder.build()
  }

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

  import fi.oph.kouta.repository.KoutaDatabase
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
    logger.info(s"Sending a message to Kouta-indeksoija SQS queue: $values with priority $priority")
    SqsService.addToQueue(priority, values) match {
      case Left(t) => {
        logger.error(s"SQS queue message to Kouta-indeksoija with values: $values and priority $priority failed: ${t.getStackTrace}")
        DBIO.failed(t)
      }
      case Right(s) => {
        logger.info(s"SQS queue message to Kouta-indeksoija with values: $values and priority $priority success.")
        DBIO.successful(s)
      }
    }
  }
}
