package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.HakuOid
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.cas.{CasAuthenticatingClient, CasClient, CasParams}
import fi.vm.sade.utils.slf4j.Logging
import org.http4s.Method.GET
import org.http4s.{Request, Uri}
import org.http4s.client.blaze.defaultClient
import org.json4s.jackson.JsonMethods.parse
import scalaz.concurrent.Task

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
  private lazy val params = CasParams(
    urlProperties.url("valinta-tulos-service"),
    "auth/login",
    config.username,
    config.password
  )

  private lazy val client = CasAuthenticatingClient(
    casClient = new CasClient(KoutaConfigurationFactory.configuration.securityConfiguration.casUrl, defaultClient, callerId),
    casParams = params,
    serviceClient = defaultClient,
    clientCallerId = callerId,
    sessionCookieName = "session"
  )

  def fetchPisteet(hakuOid: HakuOid): List[JononAlimmatPisteet] = {
    logger.info(s"Haetaan pisteet haulle $hakuOid")
    val uri = Uri.fromString(urlProperties.url("valinta-tulos-service.haku.alimmatpisteet", hakuOid.toString))
    logger.info(s"uri $uri")
    Uri.fromString(urlProperties.url("valinta-tulos-service.haku.alimmatpisteet", hakuOid.toString))
      .fold(Task.fail, url => {
        client.fetch(Request(method = GET, uri = url)) {
          case r if r.status.code == 200 =>
            r.bodyAsText
              .runLog
              .map(_.mkString)
              .map(responseBody => {
                parse(responseBody).extract[List[JononAlimmatPisteet]]
              })
          case r if r.status.code == 404 =>
            logger.info(s"Ei löydetty pisteitä valinta-tulos-servicestä haulle $hakuOid")
            Task.now(List())
          case r =>
            r.bodyAsText
              .runLog
              .map(_.mkString)
              .flatMap(_ => Task.fail(new RuntimeException(s"Failed to fetch pistees from VTS for haku $hakuOid, status ${r.status.code}")))
        }
      }).unsafePerformSyncAttemptFor(60*1000).fold(throw _, x => x)
  }
}
