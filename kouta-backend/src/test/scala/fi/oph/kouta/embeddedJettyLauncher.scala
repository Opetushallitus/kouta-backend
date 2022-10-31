package fi.oph.kouta

import com.amazonaws.services.sqs.AmazonSQSClient
import fi.oph.kouta.config.KoutaConfigurationConstants.{
  SYSTEM_PROPERTY_NAME_CONFIG_PROFILE,
  SYSTEM_PROPERTY_NAME_TEMPLATE,
  CONFIG_PROFILE_TEMPLATE
}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.repository.SessionDAO
import fi.oph.kouta.security.{CasSession, ServiceTicket}
import fi.oph.kouta.util.CommandLine
import fi.vm.sade.utils.slf4j.Logging
import io.atlassian.aws.sqs.SQSClient

import java.util.UUID
import scala.util.Try

object EmbeddedJettyLauncher extends Logging {

  val DefaultPort = "8099"

  val TestDataGeneratorSessionId = "ea596a9c-5940-497e-b5b7-aded3a2352a7"

  def main(args: Array[String]) {
    System.setProperty("kouta-backend.useSecureCookies", "false")
    System.setProperty(SYSTEM_PROPERTY_NAME_TEMPLATE, Templates.DEV_TEMPLATE_FILE_PATH)
    TestSetups.setupPostgres()
    TestSetups.setupAwsKeysForSqs()
    TestSetups.setupSqsQueues()
    TestSetups.setupCasSessionIdForTestDataGenerator()
    new JettyLauncher(System.getProperty("kouta-backend.port", DefaultPort).toInt, true).start.join()
  }
}

object TestSetups extends Logging {

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
    import scala.collection.JavaConverters._
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

  def setupWithTemplate(): String = {
    System.setProperty(SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, CONFIG_PROFILE_TEMPLATE)
  }

  def setupPostgres() = {
    System.getProperty("kouta-backend.embedded", "true") match {
      case x if "false".equalsIgnoreCase(x) => setupWithoutEmbeddedPostgres()
      case _                                => setupWithEmbeddedPostgres()
    }
  }

  def setupWithEmbeddedPostgres() = {
    logger.info("Starting embedded PostgreSQL!")
    startDockerPostgres()
  }

  private def startDockerPostgres() = {
    TempDockerDb.start()
  }

  def setupWithoutEmbeddedPostgres(): Object =
    (
      Option(System.getProperty(SYSTEM_PROPERTY_NAME_CONFIG_PROFILE)),
      Option(System.getProperty(SYSTEM_PROPERTY_NAME_TEMPLATE))
    ) match {
      case (Some(CONFIG_PROFILE_TEMPLATE), None) => setupWithDefaultTestTemplateFile()
      case _                                     => Unit
    }

  def setupWithDefaultTestTemplateFile(): String = {
    logger.info(s"Using default test template ${Templates.DEFAULT_TEMPLATE_FILE_PATH}")
    System.setProperty(SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, CONFIG_PROFILE_TEMPLATE)
    System.setProperty(SYSTEM_PROPERTY_NAME_TEMPLATE, Templates.DEFAULT_TEMPLATE_FILE_PATH)
  }

  def setupCasSessionIdForTestDataGenerator(): UUID = {
    logger.info(s"Adding session for TestDataGenerator")
    Try(SessionDAO.delete(UUID.fromString(EmbeddedJettyLauncher.TestDataGeneratorSessionId)))
    SessionDAO.store(
      CasSession(ServiceTicket(""), "1.2.246.562.24.1", KoutaIntegrationSpec.defaultAuthorities),
      UUID.fromString(EmbeddedJettyLauncher.TestDataGeneratorSessionId)
    )
  }
}

object Templates extends Logging {
  val TEST_TEMPLATE_FILE_PATH: String = "src/test/resources/test-vars.yml"
  val DEV_TEMPLATE_FILE_PATH: String  = "src/test/resources/dev-vars.yml"

  val DEFAULT_TEMPLATE_FILE_PATH: String = Option(System.getProperty("kouta-backend.template-file")) match {
    case Some(templateFilePath) => templateFilePath
    case _                      => TEST_TEMPLATE_FILE_PATH
  }
}
