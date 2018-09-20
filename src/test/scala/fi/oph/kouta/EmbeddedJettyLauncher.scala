package fi.oph.kouta

import fi.oph.kouta.config.KoutaConfigurationConstants
import fi.vm.sade.utils.slf4j.Logging

object EmbeddedJettyLauncher extends Logging {

  val DEFAULT_PORT = "8099"

  def main(args: Array[String]) {
    System.getProperty("kouta-backend.embedded", "true") match {
      case x if "false".equalsIgnoreCase(x) => TestSetups.setupWithoutEmbeddedPostgres()
      case _ => TestSetups.setupWithEmbeddedPostgres()
    }
    new JettyLauncher(System.getProperty("kouta-backend.port", DEFAULT_PORT).toInt).start.join
  }
}

object TestSetups extends Logging with KoutaConfigurationConstants {

  def setupWithEmbeddedPostgres() = {
    logger.info("Starting embedded PostgreSQL!")
    TempDb.start()
    Templates.createTestTemplate(TempDb.port)
    System.setProperty(SYSTEM_PROPERTY_NAME_TEMPLATE, Templates.TEST_TEMPLATE_FILE_PATH)
    System.setProperty(SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, CONFIG_PROFILE_TEMPLATE)
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
}

object Templates {

  val DEFAULT_TEMPLATE_FILE_PATH = "src/test/resources/dev-vars.yml"
  val TEST_TEMPLATE_FILE_PATH = "src/test/resources/embedded-jetty-vars.yml"

  import java.io.File
  import java.nio.file.Files
  import java.io.PrintWriter
  import scala.io.Source
  import scala.util.{Try, Success, Failure}

  def createTestTemplate(port:Int, deleteAutomatically:Boolean = true) = Try(new PrintWriter(new File(TEST_TEMPLATE_FILE_PATH))) match {
    case Failure(t) => throw t
    case Success(w) => try {
      Source.fromFile(DEFAULT_TEMPLATE_FILE_PATH)
        .getLines
        .map(l => l match {
          case x if x.contains("host_postgresql_kouta_port") => s"host_postgresql_kouta_port: ${port}"
          case x if x.contains("host_postgresql_kouta_user") => "host_postgresql_kouta_user: oph"
          case x if x.contains("host_postgresql_kouta_password") => "host_postgresql_kouta_password:"
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