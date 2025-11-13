package fi.oph.kouta

import com.amazonaws.services.sqs.AmazonSQSClient
import fi.oph.kouta.TestOids.OphOid
import fi.oph.kouta.config.KoutaConfigurationFactory.{CONFIG_PROFILE_TEMPLATE, SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, SYSTEM_PROPERTY_NAME_TEMPLATE}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.repository.SessionDAO
import fi.oph.kouta.security.{Authority, CasSession, RoleEntity, ServiceTicket}
import fi.oph.kouta.util.CommandLine
import fi.oph.kouta.logging.Logging
import io.atlassian.aws.sqs.SQSClient

import java.util.UUID
import scala.util.Try

object EmbeddedJettyLauncher extends Logging {

  val DefaultPort = "8099"

  def main(args: Array[String]): Unit = {
    System.setProperty("kouta-backend.useSecureCookies", "false")
    KoutaConfigurationFactory.setupWithDevTemplate()
    TestSetups.setupPostgres()
    TestSetups.setupAwsKeysForSqs()
    TestSetups.setupSqsQueues()
    TestSetups.setupFixedCasSessionId()
    new JettyLauncher(System.getProperty("kouta-backend.port", DefaultPort).toInt, true).start.join()
  }
}

object TestSetups extends Logging {

  private val FixedSessionId = "ea596a9c-5940-497e-b5b7-aded3a2352a7"
  val defaultAuthorities: Set[Authority] = RoleEntity.all.map(re => Authority(re.Crud, OphOid)).toSet
  val defaultServiceIdentifier = "test-session"

  def setupSqsQueues(): Unit = {
    val home = System.getProperty("user.home")
    if (new java.io.File(s"$home/.kouta_localstack").exists()) {
      logger.warn(s"Localstack is already running. Skipping ./tools/start_localstack....")
    } else {
      logger.info(s"Running tools/start_localstack....")
      CommandLine.runBlocking(s"tools/start_localstack")
      Runtime.getRuntime.addShutdownHook(new Thread(() => CommandLine.runBlocking(s"tools/stop_localstack")))
    }
    logSqsQueues()
  }
  def logSqsQueues(): Unit = {
    val config                     = KoutaConfigurationFactory.configuration.indexingConfiguration
    val sqsClient: AmazonSQSClient = config.endpoint.map(SQSClient.withEndpoint).getOrElse(SQSClient.default)
    import scala.jdk.CollectionConverters._
    val queues = sqsClient.listQueues().getQueueUrls.asScala
    logger.info(s"Found ${queues.size} SQS queues:")
    queues.foreach(queueUrl => logger.info(queueUrl))
  }

  def setupAwsKeysForSqs(): Any = {
    if (System.getProperty("kouta-backend.awsKeys", "false") == "false") {
      if (Option(System.getProperty("aws.accessKeyId", null)).isEmpty) {
        System.setProperty("aws.accessKeyId", "randomKeyIdForLocalstack")
        System.setProperty("aws.secretKey", "randomKeyForLocalstack")
      }
    }
  }

  def setupPostgres() = {
    System.getProperty("kouta-backend.embedded", "true") match {
      case x if "false".equalsIgnoreCase(x) => setupWithoutEmbeddedPostgres()
      case _                                => setupWithEmbeddedPostgres()
    }
  }

  def setupWithEmbeddedPostgres() = TempDockerDb.start()

  def setupWithoutEmbeddedPostgres() =
    (
      Option(System.getProperty(SYSTEM_PROPERTY_NAME_CONFIG_PROFILE)),
      Option(System.getProperty(SYSTEM_PROPERTY_NAME_TEMPLATE))
    ) match {
      case (Some(CONFIG_PROFILE_TEMPLATE), None) => KoutaConfigurationFactory.setupWithDefaultTemplateFile()
      case _                                     => ()
    }

  def setupFixedCasSessionId(): UUID = {
    logger.info(s"Adding fixed session for Jetty")
    Try(SessionDAO.delete(UUID.fromString(FixedSessionId)))
    SessionDAO.store(
      CasSession(ServiceTicket(""), "1.2.246.562.24.1", defaultAuthorities),
      UUID.fromString(FixedSessionId)
    )
  }
}

