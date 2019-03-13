package fi.oph.kouta.indexing

import scala.concurrent.Future
import scala.concurrent.duration._

import com.amazonaws.services.sqs.AmazonSQSClient
import io.atlassian.aws.sqs.SQSClient
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.indexing.Indexing.ops.indexMessage
import fi.oph.kouta.util.Retry
import fi.vm.sade.utils.slf4j.Logging

object IndexingService extends Logging with Retry {
  private lazy val config = KoutaConfigurationFactory.configuration.indexingConfiguration
  private lazy val sqs: AmazonSQSClient = {
    config.endpoint
      .map(SQSClient.withEndpoint)
      .getOrElse(SQSClient.default)
  }
  private lazy val priorityQueue = sqs.getQueueUrl(config.priorityQueue).getQueueUrl

  def index[A: Indexing](a: A): Future[Unit] = {
    indexMessage(a)
      .map(sendWithRetry)
      .getOrElse(Future.successful[Unit]())
  }

  private def sendWithRetry(msg: String): Future[Unit] = {
    retry(s"sending indexing message: '$msg'", delay = 1.second, retries = 3)(sendMessage(msg))
  }

  private def sendMessage(msg: String): Unit = sqs.sendMessage(priorityQueue, msg)
}
