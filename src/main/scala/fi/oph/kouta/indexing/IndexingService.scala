package fi.oph.kouta.indexing

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.indexing.Indexing.ops.indexMessage
import io.atlassian.aws.sqs.SQSClient

object IndexingService {
  private lazy val config = KoutaConfigurationFactory.configuration.indexingConfiguration
  private lazy val sqs = config.endpoint map { SQSClient.withEndpoint } getOrElse { SQSClient.default }
  private lazy val priorityQueue = sqs.getQueueUrl(config.priorityQueue).getQueueUrl

  def index[A: Indexing](a: A): Future[Unit] = {
    indexMessage(a)
      .map { sendMessage }
      .getOrElse { Future.successful[Unit]() }
  }

  private def sendMessage(msg: String): Future[Unit] = Future { sqs.sendMessage(priorityQueue, msg) }
}
