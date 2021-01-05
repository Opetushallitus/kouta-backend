package fi.oph.kouta

import java.util.UUID

import com.amazonaws.services.sqs.AmazonSQSClient
import fi.oph.kouta.config.{KoutaConfigurationConstants, KoutaConfigurationFactory}
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.repository.SessionDAO
import fi.oph.kouta.security.{CasSession, ServiceTicket}
import fi.vm.sade.utils.slf4j.Logging
import io.atlassian.aws.sqs.SQSClient

import scala.util.Try

object EmbeddedJettyLauncher extends Logging {

  val DefaultPort = "8099"

  val TestDataGeneratorSessionId = "ea596a9c-5940-497e-b5b7-aded3a2352a7"

  def main(args: Array[String]) {
    System.setProperty("kouta-backend.useSecureCookies", "false")
    System.getProperty("kouta-backend.embedded", "true") match {
      case x if "false".equalsIgnoreCase(x) => TestSetups.setupWithoutEmbeddedPostgres()
      case _ => TestSetups.setupWithEmbeddedPostgres()
    }
    TestSetups.setupAwsKeysForSqs()
    TestSetups.setupSqsQueues()
    TestSetups.setupCasSessionIdForTestDataGenerator()
    new JettyLauncher(System.getProperty("kouta-backend.port", DefaultPort).toInt).start.join
  }
}

object TestSetups extends Logging with KoutaConfigurationConstants {

  def setupSqsQueues() = {
    val home = System.getProperty("user.home")
    if(new java.io.File(s"$home/.kouta_localstack").exists()) {
      logger.warn(s"Localstack is already running. Skipping ./tools/start_localstack....")
    } else {
      logger.info(s"Running ../tools/start_localstack....")
      CommandLine.runBlocking(s"../tools/start_localstack")
      Runtime.getRuntime.addShutdownHook(new Thread(() => CommandLine.runBlocking(s"../tools/stop_localstack")))
    }
    logSqsQueues()
  }

  def logSqsQueues() = {
    val config = KoutaConfigurationFactory.configuration.indexingConfiguration
    val sqsClient: AmazonSQSClient = config.endpoint.map(SQSClient.withEndpoint).getOrElse(SQSClient.default)
    import scala.collection.JavaConverters._
    val queues = sqsClient.listQueues().getQueueUrls.asScala
    logger.info(s"Found ${queues.size} SQS queues:")
    queues.foreach(queueUrl => logger.info(queueUrl))
  }

  def setupAwsKeysForSqs() = {
    if(System.getProperty("kouta-backend.awsKeys", "false") == "false") {
      if (!Option(System.getProperty("aws.accessKeyId", null)).isDefined) {
        System.setProperty("aws.accessKeyId", "randomKeyIdForLocalstack")
        System.setProperty("aws.secretKey", "randomKeyForLocalstack")
      }
    }
  }

  def setupWithTemplate(port:Int) = {
    logger.info(s"Setting up test template with Postgres port ${port}")
    Templates.createTestTemplate(port)
    System.setProperty(SYSTEM_PROPERTY_NAME_TEMPLATE, Templates.TEST_TEMPLATE_FILE_PATH)
    System.setProperty(SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, CONFIG_PROFILE_TEMPLATE)
  }

  def setupWithEmbeddedPostgres() = {
    logger.info("Starting embedded PostgreSQL!")
    System.getProperty("kouta-backend.embeddedPostgresType", "host") match {
      case x if "docker".equalsIgnoreCase(x) => startDockerPostgres()
      case _ => startHostPostgres()
    }
  }

  private def startHostPostgres() = {
    TempLocalDb.start()
    setupWithTemplate(TempLocalDb.port)
  }

  private def startDockerPostgres() = {
    TempDockerDb.start()
    setupWithTemplate(TempDockerDb.port)
  }

  def setupWithoutEmbeddedPostgres()=
    (Option(System.getProperty(SYSTEM_PROPERTY_NAME_CONFIG_PROFILE)),
     Option(System.getProperty(SYSTEM_PROPERTY_NAME_TEMPLATE))) match {
      case (Some(CONFIG_PROFILE_TEMPLATE), None) => setupWithDefaultTestTemplateFile()
      case _ => Unit
  }

  def setupWithDefaultTestTemplateFile() = {
    logger.info(s"Using default test template ${Templates.DEFAULT_TEMPLATE_FILE_PATH}")
    System.setProperty(SYSTEM_PROPERTY_NAME_TEMPLATE, Templates.TEST_TEMPLATE_FILE_PATH)
    System.setProperty(SYSTEM_PROPERTY_NAME_TEMPLATE, Templates.DEFAULT_TEMPLATE_FILE_PATH)
  }

  def setupCasSessionIdForTestDataGenerator()= {
    logger.info(s"Adding session for TestDataGenerator")
    Try(SessionDAO.delete(UUID.fromString(EmbeddedJettyLauncher.TestDataGeneratorSessionId)))
    SessionDAO.store(
      CasSession(ServiceTicket(""), "1.2.246.562.24.1", KoutaIntegrationSpec.defaultAuthorities),
      UUID.fromString(EmbeddedJettyLauncher.TestDataGeneratorSessionId)
    )
  }
}

object Templates {

  val DEFAULT_TEMPLATE_FILE_PATH = "src/test/resources/dev-vars.yml"
  val TEST_TEMPLATE_FILE_PATH = "src/test/resources/embedded-jetty-vars.yml"

  import java.io.{File, PrintWriter}
  import java.nio.file.Files

  import scala.io.Source
  import scala.util.{Failure, Success, Try}

  def createTestTemplate(port:Int, deleteAutomatically:Boolean = true) = Try(new PrintWriter(new File(TEST_TEMPLATE_FILE_PATH))) match {
    case Failure(t) => throw t
    case Success(w) => try {
      Source.fromFile(DEFAULT_TEMPLATE_FILE_PATH)
        .getLines
        .map(l => l match {
          case x if x.contains("host_postgresql_kouta_port") => s"host_postgresql_kouta_port: ${port}"
          case x if x.contains("postgres_app_user") => "postgres_app_user: oph"
          case x if x.contains("host_postgresql_kouta_app_password") => "host_postgresql_kouta_app_password: oph"
          case x if x.contains("host_postgresql_kouta") => "host_postgresql_kouta: localhost"
          case x => x
        })
        .foreach(l => w.println(l))
      w.flush
    } finally { w.close() }
    if(deleteAutomatically) {
      Runtime.getRuntime.addShutdownHook(new Thread(() => Templates.deleteTestTemplate()))
    }
  }

  def deleteTestTemplate() = {
    Files.deleteIfExists(new File(TEST_TEMPLATE_FILE_PATH).toPath)
  }
}