package fi.oph.kouta.config

import com.typesafe.config.{Config => TypesafeConfig}
import fi.oph.kouta.security.Role
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.config.{ApplicationSettings, ApplicationSettingsLoader, ApplicationSettingsParser, ConfigTemplateProcessor}
import fi.vm.sade.utils.slf4j.Logging

case class KoutaDatabaseConfiguration(
  url: String,
  username: String,
  password: String,
  numThreads: Option[Int],
  maxConnections: Option[Int],
  minConnections: Option[Int],
  registerMbeans: Option[Boolean],
  initializationFailTimeout: Option[Int],
  leakDetectionThresholdMillis: Option[Int]
)

case class CasConfiguration(
  securityContext: String,
  url: String,
  serviceIdentifier: String,
  username: String,
  password: String,
  requiredRoles: Set[Role]
)

case class IndexingConfiguration(priorityQueue: String, endpoint: Option[String], region: Option[String])

case class KoutaConfiguration(config: TypesafeConfig, urlProperties: OphProperties) extends ApplicationSettings(config) {
  val databaseConfiguration = KoutaDatabaseConfiguration(
    url = config.getString("kouta-backend.db.url"),
    username = config.getString("kouta-backend.db.user"),
    password = config.getString("kouta-backend.db.password"),
    numThreads = Option(config.getInt("kouta-backend.db.numThreads")),
    maxConnections = Option(config.getInt("kouta-backend.db.maxConnections")),
    minConnections = Option(config.getInt("kouta-backend.db.minConnections")),
    registerMbeans = Option(config.getBoolean("kouta-backend.db.registerMbeans")),
    initializationFailTimeout = Option(config.getInt("kouta-backend.db.initializationFailTimeout")),
    leakDetectionThresholdMillis = Option(config.getInt("kouta-backend.db.leakDetectionThresholdMillis"))
  )

  val indexingConfiguration = IndexingConfiguration(
    config.getString("kouta-backend.sqs.queue.priority"),
    scala.util.Try(config.getString("kouta-backend.sqs.endpoint")).filter(_.trim.nonEmpty).toOption,
    scala.util.Try(config.getString("kouta-backend.sqs.region")).filter(_.trim.nonEmpty).toOption
  )

  val casConfiguration = CasConfiguration(
    url = config.getString("cas.url"),
    securityContext = config.getString("kouta-backend.cas.type"),
    serviceIdentifier = config.getString("kouta-backend.cas.service"),
    username = config.getString("kouta-backend.cas.username"),
    password = config.getString("kouta-backend.cas.password"),

    requiredRoles = Set("APP_KOUTA_USER").map(Role(_))
  )
}

trait KoutaConfigurationConstants {
  val SYSTEM_PROPERTY_NAME_CONFIG_PROFILE = "kouta-backend.config-profile"
  val SYSTEM_PROPERTY_NAME_TEMPLATE = "kouta-backend.template-file"

  val CONFIG_PROFILE_DEFAULT = "default"
  val CONFIG_PROFILE_TEMPLATE = "template"
}

object KoutaConfigurationFactory extends Logging with KoutaConfigurationConstants {

  val profile = System.getProperty(SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, CONFIG_PROFILE_DEFAULT)
  logger.info(s"Using profile '${profile}'")

  val configuration: KoutaConfiguration = profile match {
    case CONFIG_PROFILE_DEFAULT => loadOphConfiguration()
    case CONFIG_PROFILE_TEMPLATE => loadTemplatedConfiguration()
    case _ => throw new IllegalArgumentException(
      s"Unknown profile '${profile}'! Cannot load oph-properties! Use either " +
      s"'${CONFIG_PROFILE_DEFAULT}' or '${CONFIG_PROFILE_TEMPLATE}' profiles.")
  }

  def init() = {}

  private def loadOphConfiguration(): KoutaConfiguration = {
    val configFilePath = System.getProperty("user.home") + "/oph-configuration/kouta-backend.properties"

    implicit val applicationSettingsParser = new ApplicationSettingsParser[KoutaConfiguration] {
      override def parse(config: TypesafeConfig): KoutaConfiguration = KoutaConfiguration(config, new OphProperties(configFilePath))
    }

    logger.info(s"Reading properties from '${configFilePath}'")
    ApplicationSettingsLoader.loadSettings(configFilePath)
  }

  private def loadTemplatedConfiguration(overrideFromSystemProperties: Boolean = false): KoutaConfiguration = {
    val templateFilePath = Option(System.getProperty(SYSTEM_PROPERTY_NAME_TEMPLATE)).getOrElse(
      throw new IllegalArgumentException(s"Using 'template' profile but '${SYSTEM_PROPERTY_NAME_TEMPLATE}' " +
        "system property is missing. Cannot create oph-properties!"))

    implicit val applicationSettingsParser = new ApplicationSettingsParser[KoutaConfiguration] {
      override def parse(c: TypesafeConfig): KoutaConfiguration = KoutaConfiguration(c,
        new OphProperties("src/test/resources/kouta-backend.properties") {
          addDefault("host.virkailija", c.getString("host.virkailija"))
        })
    }

    logger.info(s"Reading template variables from '${templateFilePath}'")
    ConfigTemplateProcessor.createSettings("kouta-backend", templateFilePath)
  }
}
