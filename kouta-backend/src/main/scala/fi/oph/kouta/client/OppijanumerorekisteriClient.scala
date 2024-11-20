package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.UserOid
import fi.oph.kouta.logging.Logging
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.javautils.nio.cas.{CasClient, CasClientBuilder, CasConfig}
import org.asynchttpclient.RequestBuilder
import org.json4s.jackson.JsonMethods._

import java.util.concurrent.TimeUnit
import scala.compat.java8.FutureConverters.toScala
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

case class Henkilo(kutsumanimi: Option[String],
                   sukunimi: Option[String],
                   etunimet: Option[String])

trait OppijanumerorekisteriClient {
  def getHenkilöFromCache(oid: UserOid): Henkilo
}

object OppijanumerorekisteriClient
  extends OppijanumerorekisteriClient
    with CallerId
    with Logging
    with KoutaJsonFormats {
  private val config = KoutaConfigurationFactory.configuration.oppijanumerorekisteriClientConfiguration
  private val isTestEnvironment = KoutaConfigurationFactory.configuration.isTestEnvironment
  private val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  val casConfig: CasConfig = new CasConfig.CasConfigBuilder(
    config.username,
    config.password,
    urlProperties.url("cas.url"),
    urlProperties.url("oppijanumerorekisteri-service"),
    callerId,
    callerId,
    "/j_spring_cas_security_check")
    .setJsessionName("JSESSIONID").build

  val casClient: CasClient = CasClientBuilder.build(casConfig)

  implicit val OppijanumeroCache: Cache[UserOid, Henkilo] = Scaffeine()
    .expireAfterWrite(60.minutes)
    .build()

  private def getHenkilö(oid: UserOid): Henkilo = {
    val oppijanumerorekisteriUrl: String =
      urlProperties.url(
        "oppijanumerorekisteri-service.henkilo",
        oid
      )

    val request = new RequestBuilder().setMethod("GET").setUrl(oppijanumerorekisteriUrl).build
    val future = Future {
      casClient.executeBlocking(request)
    }
    val result = future.map {
      case r if r.getStatusCode == 200 =>
        parse(r.getResponseBodyAsStream()).extract[Henkilo]
      case r if r.getStatusCode == 404 && isTestEnvironment =>
        Henkilo(kutsumanimi = Some("Henkilö"), sukunimi = Some("Puuttuu"), etunimet = Some("Henkilö"))
      case r =>
        throw new RuntimeException(s"Henkilön $oid tietojen hakeminen oppijanumerorekisteristä epäonnistui: $r")
    }
    Await.result(result, Duration(1, TimeUnit.MINUTES))
  }

  override def getHenkilöFromCache(oid: UserOid): Henkilo = {
      OppijanumeroCache.get(oid, oid => getHenkilö(oid))
  }
}
