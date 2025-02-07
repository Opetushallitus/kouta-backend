package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.logging.Logging
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.javautils.nio.cas.{CasClient, CasClientBuilder, CasConfig}
import org.asynchttpclient.RequestBuilder
import org.json4s.jackson.JsonMethods.parse

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

trait ValintaperusteetServiceClient {

  def getValintatapajono(valintatapajonoOid: String): ValintatapajonoDTO

}

case class ValintatapajonoDTO(aloituspaikat: Int,
                              nimi: String,
                              kuvaus: String,
                              tyyppi: String,
                              siirretaanSijoitteluun: Boolean,
                              tasapistesaanto: String,
                              aktiivinen: Boolean,
                              valisijoittelu: Boolean,
                              automaattinenSijoitteluunSiirto: Boolean,
                              eiVarasijatayttoa: Boolean,
                              kaikkiEhdonTayttavatHyvaksytaan: Boolean,
                              varasijat: Int,
                              varasijaTayttoPaivat: Int,
                              poissaOlevaTaytto: Boolean,
                              poistetaankoHylatyt: Boolean,
                              varasijojaKaytetaanAlkaen: Option[LocalDateTime],
                              varasijojaTaytetaanAsti: Option[LocalDateTime],
                              eiLasketaPaivamaaranJalkeen: Option[LocalDateTime],
                              kaytetaanValintalaskentaa: Boolean,
                              tayttojono: String,
                              oid: String,
                              inheritance: Boolean,
                              prioriteetti: Int)

object ValintaperusteetServiceClient extends ValintaperusteetServiceClient with HttpClient with CallerId with Logging with KoutaJsonFormats {
  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties
  private lazy val config = KoutaConfigurationFactory.configuration.valintaperusteetServiceClientConfiguration

  private lazy val casConfig: CasConfig = new CasConfig.CasConfigBuilder(
    config.username,
    config.password,
    urlProperties.url("cas.url"),
    urlProperties.url("valintaperusteet-service"),
    callerId,
    callerId,
    "/j_spring_cas_security_check")
    .setJsessionName("JSESSIONID").build

  val casClient: CasClient = CasClientBuilder.build(casConfig)

  def getValintatapajono(valintatapajonoOid: String): ValintatapajonoDTO = {
    val request = new RequestBuilder().setMethod("GET").setUrl(urlProperties.url("valintaperusteet-service.valintatapajono.oid", valintatapajonoOid)).build
    val future = Future {
      casClient.executeBlocking(request)
    }
    val result = future.map {
      case r if r.getStatusCode == 200 =>
        parse(r.getResponseBodyAsStream()).extract[ValintatapajonoDTO]
      case r =>
        throw new RuntimeException(s"Valintatapajonon $valintatapajonoOid tietojen hakeminen valimtaperusteet-servicestä epäonnistui: $r")
    }
    Await.result(result, Duration(1, TimeUnit.MINUTES))
  }

}
