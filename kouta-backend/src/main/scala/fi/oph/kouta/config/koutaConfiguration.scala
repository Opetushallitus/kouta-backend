package fi.oph.kouta.config

import com.typesafe.config.{Config => TypesafeConfig}
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.config.{ApplicationSettings, ApplicationSettingsLoader, ApplicationSettingsParser, ConfigTemplateProcessor}
import fi.vm.sade.utils.slf4j.Logging

import java.io.File
import java.net.URL
import scala.util.Try

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

case class SecurityConfiguration(
    casUrl: String,
    casServiceIdentifier: String,
    kayttooikeusUrl: String,
    useSecureCookies: Boolean,
    externalApiModifyEnabled: Boolean
) {
  def sessionCookieName: String = if (useSecureCookies) {
    //TODO: Kouta-indeksoija ja muut olettaa, että session nimi on session
    //"__Secure-session"
    "session"
  } else {
    "session"
  }
}

case class IndexingConfiguration(priorityQueue: String, endpoint: Option[String], region: Option[String])

case class S3Configuration(imageBucket: String, imageBucketPublicUrl: String, region: Option[String])

case class OhjausparametritClientConfiguration(username: String, password: String)

case class OppijanumerorekisteriClientConfiguration(username: String, password: String)

case class KayttooikeusClientConfiguration(username: String, password: String)

case class HakemuspalveluClientConfiguration(username: String, password: String)

case class ElasticSearchConfiguration(
    elasticUrl: String,
    authEnabled: Boolean,
    username: String,
    password: String
)

case class KoutaConfiguration(config: TypesafeConfig, urlProperties: OphProperties)
    extends ApplicationSettings(config) {
  val databaseConfiguration: KoutaDatabaseConfiguration = KoutaDatabaseConfiguration(
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

  val indexingConfiguration: IndexingConfiguration = IndexingConfiguration(
    config.getString("kouta-backend.sqs.queue.priority"),
    Try(config.getString("kouta-backend.sqs.endpoint")).filter(_.trim.nonEmpty).toOption,
    Try(config.getString("kouta-backend.sqs.region")).filter(_.trim.nonEmpty).toOption
  )

  val s3Configuration: S3Configuration = S3Configuration(
    config.getString("kouta-backend.s3.imageBucket"),
    config.getString("kouta-backend.s3.imageBucketPublicUrl"),
    Try(config.getString("kouta-backend.s3.region")).filter(_.trim.nonEmpty).toOption
  )

  val securityConfiguration: SecurityConfiguration = SecurityConfiguration(
    casUrl = config.getString("cas.url"),
    casServiceIdentifier = config.getString("kouta-backend.cas.service"),
    kayttooikeusUrl = config.getString("kayttooikeus-service.userDetails.byUsername"),
    useSecureCookies = !"false".equals(System.getProperty("kouta-backend.useSecureCookies")),
    Try(config.getBoolean("kouta.external-api.modify.enabled")).getOrElse(false)
  )

  val ohjausparametritClientConfiguration: OhjausparametritClientConfiguration = OhjausparametritClientConfiguration(
    username = config.getString("kouta-backend.cas.username"),
    password = config.getString("kouta-backend.cas.password")
  )

  val oppijanumerorekisteriClientConfiguration: OppijanumerorekisteriClientConfiguration =
    OppijanumerorekisteriClientConfiguration(
      username = config.getString("kouta-backend.cas.username"),
      password = config.getString("kouta-backend.cas.password")
    )

  val kayttooikeusClientConfiguration: KayttooikeusClientConfiguration = KayttooikeusClientConfiguration(
    username = config.getString("kouta-backend.cas.username"),
    password = config.getString("kouta-backend.cas.password")
  )

  val hakemuspalveluClientConfiguration: HakemuspalveluClientConfiguration = HakemuspalveluClientConfiguration(
    username = config.getString("kouta-backend.cas.username"),
    password = config.getString("kouta-backend.cas.password")
  )
  val elasticSearchConfiguration = ElasticSearchConfiguration(
    config.getString("kouta-backend.elasticsearch.url"),
    config.getBoolean("kouta-backend.elasticsearch.auth-enabled"),
    config.getString("kouta-backend.elasticsearch.username"),
    config.getString("kouta-backend.elasticsearch.password")
  )
}

trait KoutaConfigurationConstants {
  val SYSTEM_PROPERTY_NAME_CONFIG_PROFILE = "kouta-backend.config-profile"
  val SYSTEM_PROPERTY_NAME_TEMPLATE       = "kouta-backend.template-file"

  val CONFIG_PROFILE_DEFAULT  = "default"
  val CONFIG_PROFILE_TEMPLATE = "template"
}

object KoutaConfigurationFactory extends Logging with KoutaConfigurationConstants {

  val profile: String = System.getProperty(SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, CONFIG_PROFILE_DEFAULT)
  logger.info(s"Using profile '$profile'")

  val configuration: KoutaConfiguration = profile match {
    case CONFIG_PROFILE_DEFAULT  => loadOphConfiguration()
    case CONFIG_PROFILE_TEMPLATE => loadTemplatedConfiguration()
    case _ =>
      throw new IllegalArgumentException(
        s"Unknown profile '$profile'! Cannot load oph-properties! Use either " +
          s"'$CONFIG_PROFILE_DEFAULT' or '$CONFIG_PROFILE_TEMPLATE' profiles."
      )
  }

  def init(): Unit = {}

  private def loadOphConfiguration(): KoutaConfiguration = {
    val configFilePath = System.getProperty("user.home") + "/oph-configuration/kouta-backend.properties"

    implicit val applicationSettingsParser: ApplicationSettingsParser[KoutaConfiguration] = (config: TypesafeConfig) =>
      KoutaConfiguration(config, new OphProperties(configFilePath))

    logger.info(s"Reading properties from '$configFilePath'")
    ApplicationSettingsLoader.loadSettings(configFilePath)
  }

  private def loadTemplatedConfiguration(): KoutaConfiguration = {
    val templateFilePath = Option(System.getProperty(SYSTEM_PROPERTY_NAME_TEMPLATE)).getOrElse(
      throw new IllegalArgumentException(
        s"Using 'template' profile but '$SYSTEM_PROPERTY_NAME_TEMPLATE' " +
          "system property is missing. Cannot create oph-properties!"
      )
    )
    val isLocalDev = Option(System.getProperty("kouta-backend.localDev")).exists("true".equalsIgnoreCase)
    val prefix = if(isLocalDev) "kouta-backend/" else ""

    implicit val applicationSettingsParser: ApplicationSettingsParser[KoutaConfiguration] = (c: TypesafeConfig) =>
      KoutaConfiguration(
        c,
        new OphProperties(s"${prefix}src/test/resources/kouta-backend.properties") {
          addDefault("host.virkailija", c.getString("host.virkailija"))
        }
      )

    logger.info(s"Reading template variables from '$templateFilePath'. ${if(isLocalDev) "STARTING IN DEVELOPMENT MODE!"}")
    val templateURL: URL = new File(
      s"${prefix}src/main/resources/oph-configuration/kouta-backend.properties.template").toURI.toURL
    val attributesURL = new File(templateFilePath).toURI.toURL
    ConfigTemplateProcessor.createSettings(templateURL, attributesURL)
  }
}
