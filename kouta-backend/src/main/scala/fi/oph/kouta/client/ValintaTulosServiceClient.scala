package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.HakuOid
import fi.oph.kouta.logging.Logging
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.javautils.nio.cas.{CasClient, CasClientBuilder, CasConfig}
import org.asynchttpclient.RequestBuilder
import org.json4s.jackson.JsonMethods.parse

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

trait ValintaTulosServiceClient {
  def fetchPisteet(hakuOid: HakuOid): List[JononAlimmatPisteet]
}

case class JononAlimmatPisteet(valintatapajonoOid: String,
                               hakukohdeOid: String,
                               alinHyvaksyttyPistemaara: Double,
                               sijoitteluajoId: Long)

object ValintaTulosServiceClient extends ValintaTulosServiceClient with HttpClient with CallerId with Logging with KoutaJsonFormats{
  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties
  private lazy val config = KoutaConfigurationFactory.configuration.valintaTulosServiceClientConfiguration

  private lazy val casConfig: CasConfig = new CasConfig.CasConfigBuilder(
    config.username,
    config.password,
    urlProperties.url("cas.url"),
    urlProperties.url("valinta-tulos-service"),
    callerId,
    callerId,
    "/auth/login")
    .setJsessionName("session").build

  val casClient: CasClient = CasClientBuilder.build(casConfig)

  def fetchPisteet(hakuOid: HakuOid): List[JononAlimmatPisteet] = {
    val request = new RequestBuilder().setMethod("GET").setUrl(urlProperties.url("valinta-tulos-service.haku.alimmatpisteet", hakuOid.toString)).build
    val future = Future {
      casClient.executeBlocking(request)
    }
    val result = future.map {
      case r if r.getStatusCode == 200 =>
        parse(r.getResponseBodyAsStream()).extract[List[JononAlimmatPisteet]]
      case r =>
        throw new RuntimeException(s"Haun $hakuOid tietojen hakeminen valinta-tulos-servicestä epäonnistui: $r")
    }
    Await.result(result, Duration(10, TimeUnit.MINUTES))
  }
}
