package fi.oph.kouta.config

import com.typesafe.config.{Config => TypesafeConfig}
import fi.oph.kouta.util.{KoutaBaseConfig, KoutaConfigFactory}
import fi.vm.sade.properties.OphProperties

import scala.util.Try

case class KoutaDatabaseConfiguration(
    url: String,
    port: Int,
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

case class S3Configuration(imageBucket: String, imageBucketPublicUrl: String, transferFileBucket: String, region: Option[String])

case class OhjausparametritClientConfiguration(username: String, password: String)

case class OppijanumerorekisteriClientConfiguration(username: String, password: String)

case class KayttooikeusClientConfiguration(username: String, password: String)

case class HakemuspalveluClientConfiguration(username: String, password: String)

case class ValintaperusteetServiceClientConfiguration(username: String, password: String)

case class ValintaTulosServiceClientConfiguration(username: String, password: String)

case class ElasticSearchConfiguration(
    elasticUrl: String,
    authEnabled: Boolean,
    username: String,
    password: String
)

case class KoutaConfiguration(config: TypesafeConfig, urlProperties: OphProperties)
    extends KoutaBaseConfig(config, urlProperties) {
  val databaseConfiguration: KoutaDatabaseConfiguration = KoutaDatabaseConfiguration(
    url = config.getString("kouta-backend.db.url"),
    port = config.getInt("kouta-backend.db.port"),
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

  val quickIndexTimeoutSeconds: Int = Try(config.getInt("kouta-backend.quick-index-timeout-seconds")).getOrElse(10)
  val isTestEnvironment: Boolean = Try(config.getBoolean("kouta-backend.test-environment")).getOrElse(false)

  val s3Configuration: S3Configuration = S3Configuration(
    config.getString("kouta-backend.s3.imageBucket"),
    config.getString("kouta-backend.s3.imageBucketPublicUrl"),
    config.getString("kouta-backend.s3.transferFileBucket"),
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

  val valintaperusteetServiceClientConfiguration: ValintaperusteetServiceClientConfiguration = ValintaperusteetServiceClientConfiguration(
    username = config.getString("kouta-backend.cas.username"),
    password = config.getString("kouta-backend.cas.password")
  )

  val valintaTulosServiceClientConfiguration: ValintaTulosServiceClientConfiguration = ValintaTulosServiceClientConfiguration(
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

object KoutaConfigurationFactory extends KoutaConfigFactory[KoutaConfiguration]("kouta-backend") {
  def createConfigCaseClass(config: TypesafeConfig, urlProperties: OphProperties) =
    KoutaConfiguration(config, urlProperties)
}
