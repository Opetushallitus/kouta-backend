package fi.oph.kouta.client

import java.time.Instant
import java.util.concurrent.TimeUnit

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.HakuOid
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.cas.{CasAuthenticatingClient, CasClient, CasParams}
import org.http4s.Method.POST
import org.http4s.client.blaze.defaultClient
import org.http4s.json4s.jackson.jsonEncoderOf
import org.http4s.{Request, Uri}
import org.json4s.DefaultWriters._
import org.json4s.{Extraction, JValue}
import scalaz.concurrent.Task

import scala.concurrent.duration.Duration

case class DateParametriDTO(date: Long)
case class IntParametriDTO(value: Int)
case class HaunOhjausparametritDTO(PH_OPVP: Option[DateParametriDTO],
                                   PH_HPVOA: Option[IntParametriDTO],
                                   PH_HKP: Option[DateParametriDTO])

case class HaunOhjausparametrit(hakuOid: HakuOid,
                                paikanVastaanottoPaattyy: Option[Instant],
                                hakijakohtainenPaikanVastaanottoaika: Option[Int],
                                hakukierrosPaattyy: Option[Instant])

trait OhjausparametritClient {
  def postHaunOhjausparametrit(haunOhjausparametrit: HaunOhjausparametrit): Unit
}

object OhjausparametritClient extends OhjausparametritClient with KoutaJsonFormats {
  private val config = KoutaConfigurationFactory.configuration.ohjausparametritClientConfiguration
  private val urlProperties = KoutaConfigurationFactory.configuration.urlProperties
  private val params = CasParams(
    urlProperties.url("ohjausparametrit-service.service"),
    "j_spring_cas_security_check",
    config.username,
    config.password
  )
  private val client = CasAuthenticatingClient(
    casClient = new CasClient(KoutaConfigurationFactory.configuration.securityConfiguration.casUrl, defaultClient),
    casParams = params,
    serviceClient = defaultClient,
    clientCallerId = "kouta-backend",
    sessionCookieName = "JSESSIONID"
  )

  override def postHaunOhjausparametrit(haunOhjausparametrit: HaunOhjausparametrit): Unit = {
    val url = urlProperties.url("ohjausparametrit-service.parametri", haunOhjausparametrit.hakuOid)
    val body = Extraction.decompose(HaunOhjausparametritDTO(
      PH_OPVP = haunOhjausparametrit.paikanVastaanottoPaattyy.map(i => DateParametriDTO(i.toEpochMilli)),
      PH_HPVOA = haunOhjausparametrit.hakijakohtainenPaikanVastaanottoaika.map(IntParametriDTO),
      PH_HKP = haunOhjausparametrit.hakukierrosPaattyy.map(i => DateParametriDTO(i.toEpochMilli))
    ))

    Uri.fromString(url)
      .fold(Task.fail, url => {
        client.fetch(Request(method = POST, uri = url).withBody(body)(jsonEncoderOf[JValue])) {
          case r if r.status.code == 200 =>
            Task.now(())
          case r =>
            r.bodyAsText
              .runLog
              .map(_.mkString)
              .flatMap(response => Task.fail(new RuntimeException(s"Url $url returned status code ${r.status} $response")))
        }
      }).unsafePerformSyncAttemptFor(Duration(5, TimeUnit.SECONDS)).fold(throw _, x => x)
  }
}
