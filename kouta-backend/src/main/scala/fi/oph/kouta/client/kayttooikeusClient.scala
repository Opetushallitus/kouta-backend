package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.UserOid
import fi.oph.kouta.logging.Logging
import fi.vm.sade.javautils.nio.cas.{CasClient, CasClientBuilder, CasConfig}
import org.asynchttpclient.RequestBuilder
import org.json4s.jackson.JsonMethods.parse

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

case class OrganisaatioHenkilo(organisaatioOid: String)

object KayttooikeusClient extends KayttooikeusClient

trait KayttooikeusClient extends HttpClient with CallerId with Logging {

  import org.json4s._

  private implicit val formats: DefaultFormats.type = DefaultFormats
  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  private lazy val config = KoutaConfigurationFactory.configuration.kayttooikeusClientConfiguration

  private lazy val casConfig: CasConfig = new CasConfig.CasConfigBuilder(
    config.username,
    config.password,
    config.casUrl,
    urlProperties.url("kayttooikeus-service"),
    callerId,
    callerId,
    "/j_spring_cas_security_check")
    .setJsessionName("JSESSIONID").build

  val casClient: CasClient = CasClientBuilder.build(casConfig)

  implicit val kayttooikeusCache: Cache[UserOid, List[OrganisaatioHenkilo]] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()

  private def getOrganisaatiot(oid: UserOid): List[OrganisaatioHenkilo] =  {
    val url = urlProperties.url(s"kayttooikeus-service.organisaatiohenkilo", oid)

    val request = new RequestBuilder().setMethod("GET").setUrl(url).build
    val future = Future {
      casClient.executeBlocking(request)
    }
    val result = future.map {
      case r if r.getStatusCode == 200 =>
        parse(r.getResponseBodyAsStream()).extract[List[OrganisaatioHenkilo]]
      case r =>
        throw new RuntimeException(s"Henkilön $oid käyttöoikeustietojen hakeminen käyttöoikeus-servicestä epäonnistui: $r")
    }
    Await.result(result, Duration(1, TimeUnit.MINUTES))
  }

  def getOrganisaatiotFromCache(oid: UserOid): List[OrganisaatioHenkilo] = {
    try {
      kayttooikeusCache.get(oid, oid => getOrganisaatiot(oid))
    } catch {
      case error: Throwable => throw error
    }
  }
}
