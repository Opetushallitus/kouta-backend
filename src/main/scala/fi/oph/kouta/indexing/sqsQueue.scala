package fi.oph.kouta.indexing

import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.sqs.AmazonSQSClient
import io.atlassian.aws.sqs.SQSClient
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.indexing.indexing._
import fi.vm.sade.utils.slf4j.Logging
import io.atlassian.aws.AmazonClientConnectionDef

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

  private val queues = {
    logger.info(s"""Got priority queue name '${config.priorityQueue}' from oph configuration""")
    val queueUrl = sqsClient.getQueueUrl(config.priorityQueue).getQueueUrl
    logger.info(s"""AWS queue url for priority queue in current environment is '$queueUrl'""")
    Map(HighPriority -> sqsClient.getQueueUrl(config.priorityQueue).getQueueUrl)
  }

  private def createMessage(stuff: Map[IndexType, Seq[String]]): Either[Throwable, String] = {
    import org.json4s.jackson.JsonMethods.{compact, render}
    import org.json4s.JsonDSL._
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

object SqsInTransactionService extends Logging {

  import slick.dbio.DBIO
  import fi.oph.kouta.repository.KoutaDatabase
  import scala.concurrent.ExecutionContext.Implicits.global

  def runActionAndUpdateIndex[R](priority: Priority,
                                 index: IndexType,
                                 action: () => DBIO[R],
                                 indexableValue: String): R =
    runActionAndUpdateIndex(priority, index, action, (r:R) => indexableValue)

  def runActionAndUpdateIndex[R](priority: Priority,
                                 index: IndexType,
                                 action: () => DBIO[R]): R =
    runActionAndUpdateIndex(priority, index, action, (r:R) => r.toString)

  def runActionAndUpdateIndex[R](priority: Priority,
                                 index: IndexType,
                                 action: () => DBIO[R],
                                 getIndexableValue: (R) => String): R =
    KoutaDatabase.runBlockingTransactionally(
      for {
        result <- action()
        _      <- toSQSQueue(priority, index, getIndexableValue(result))
      } yield result ).get

  def toSQSQueue(priority: Priority, index: IndexType, value: String): DBIO[String] =
    toSQSQueue(priority, index, Seq(value))

  def toSQSQueue(priority: Priority, index: IndexType, values: Seq[String]): DBIO[String] =
    toSQSQueue(priority, Map(index -> values))

  def toSQSQueue(priority: Priority, values: Map[IndexType, Seq[String]]): DBIO[String] = {
    SqsService.addToQueue(priority, values) match {
      case Left(t) => DBIO.failed(t)
      case Right(s) => DBIO.successful(s)
    }
  }
}
