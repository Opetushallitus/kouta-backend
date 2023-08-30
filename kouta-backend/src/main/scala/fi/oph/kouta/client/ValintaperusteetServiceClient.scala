package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory

import java.time.LocalDateTime
import fi.oph.kouta.domain.oid.HakukohdeOid
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.cas.{CasAuthenticatingClient, CasClient, CasParams}
import fi.vm.sade.utils.slf4j.Logging
import org.http4s.Method.GET
import org.http4s.{Request, Uri}
import org.http4s.client.blaze.defaultClient
import org.json4s.jackson.JsonMethods.parse
import scalaz.concurrent.Task

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
  private lazy val params = CasParams(
    urlProperties.url("valintaperusteet-service"),
    "j_spring_cas_security_check",
    config.username,
    config.password
  )

  private lazy val client = CasAuthenticatingClient(
    casClient = new CasClient(KoutaConfigurationFactory.configuration.securityConfiguration.casUrl, defaultClient, callerId),
    casParams = params,
    serviceClient = defaultClient,
    clientCallerId = callerId,
    sessionCookieName = "JSESSIONID"
  )

  def getValintatapajono(valintatapajonoOid: String): ValintatapajonoDTO = {
    Uri.fromString(urlProperties.url("valintaperusteet-service.valintatapajono.oid", valintatapajonoOid))
      .fold(Task.fail, url => {
        client.fetch(Request(method = GET, uri = url)) {
          case r if r.status.code == 200 =>
            r.bodyAsText
              .runLog
              .map(_.mkString)
              .map(responseBody => {
                parse(responseBody).extract[ValintatapajonoDTO]
              })
          case r =>
            r.bodyAsText
              .runLog
              .map(_.mkString)
              .flatMap(_ => Task.fail(new RuntimeException(s"Failed to fetch valintatapajono from valintaperusteet-service for valintatapajono-oid $valintatapajonoOid, status ${r.status.code}")))
        }
      }).unsafePerformSyncAttemptFor(60 * 1000).fold(throw _, x => x)
  }

}
