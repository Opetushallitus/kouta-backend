package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.HakuOid
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.javautils.nio.cas.{CasClient, CasClientBuilder, CasConfig}
import org.asynchttpclient.RequestBuilder
import org.json4s.jackson.JsonMethods.{compact, render}
import org.json4s.Extraction
import org.json4s.jackson.JsonMethods.parse

import java.time.Instant
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

case class DateParametriDTO(date: Long)
case class IntParametriDTO(value: Int)
case class HaunOhjausparametritDTO(PH_OPVP: Option[DateParametriDTO],
                                   PH_HPVOA: Option[IntParametriDTO],
                                   PH_HKP: Option[DateParametriDTO],
                                   sijoittelu: Option[Boolean],
                                   useitaHakemuksia: Option[Boolean],
                                   jarjestetytHakutoiveet: Option[Boolean],
                                   hakutoiveidenMaaraRajoitettu: Option[Boolean])

case class HaunOhjausparametrit(hakuOid: HakuOid,
                                paikanVastaanottoPaattyy: Option[Instant],
                                hakijakohtainenPaikanVastaanottoaika: Option[Int],
                                hakukierrosPaattyy: Option[Instant],
                                sijoittelu: Option[Boolean],
                                useitaHakemuksia: Option[Boolean],
                                jarjestetytHakutoiveet: Option[Boolean],
                                hakutoiveidenMaaraRajoitettu: Option[Boolean])

trait OhjausparametritClient {
  def postHaunOhjausparametrit(haunOhjausparametrit: HaunOhjausparametrit): Unit
}

object OhjausparametritClient extends OhjausparametritClient with CallerId with KoutaJsonFormats {
  private val config = KoutaConfigurationFactory.configuration.ohjausparametritClientConfiguration
  private val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  private lazy val casConfig: CasConfig = new CasConfig.CasConfigBuilder(
    config.username,
    config.password,
    urlProperties.url("cas.url"),
    urlProperties.url("ohjausparametrit-service.service"),
    callerId,
    callerId,
    "j_spring_cas_security_check")
    .setJsessionName("JSESSIONID").build

  val casClient: CasClient = CasClientBuilder.build(casConfig)

  override def postHaunOhjausparametrit(haunOhjausparametrit: HaunOhjausparametrit): Unit = {
    val url = urlProperties.url("ohjausparametrit-service.parametri", haunOhjausparametrit.hakuOid)
    val body = Extraction.decompose(HaunOhjausparametritDTO(
      PH_OPVP = haunOhjausparametrit.paikanVastaanottoPaattyy.map(i => DateParametriDTO(i.toEpochMilli)),
      PH_HPVOA = haunOhjausparametrit.hakijakohtainenPaikanVastaanottoaika.map(IntParametriDTO),
      PH_HKP = haunOhjausparametrit.hakukierrosPaattyy.map(i => DateParametriDTO(i.toEpochMilli)),
      sijoittelu = haunOhjausparametrit.sijoittelu,
      useitaHakemuksia = haunOhjausparametrit.useitaHakemuksia,
      jarjestetytHakutoiveet = haunOhjausparametrit.jarjestetytHakutoiveet,
      hakutoiveidenMaaraRajoitettu = haunOhjausparametrit.hakutoiveidenMaaraRajoitettu
    ))
    val bodyString = compact(render(body))

    val request = new RequestBuilder().setMethod("POST").setUrl(url).setBody(bodyString).build
    val future = Future {
      casClient.executeBlocking(request)
    }
    val result = future.map {
      case r if r.getStatusCode == 200 =>
        parse(r.getResponseBodyAsStream()).extract[ValintatapajonoDTO]
      case r =>
        throw new RuntimeException(s"Haun ${haunOhjausparametrit.hakuOid} ohjausparametrien tallentaminen ohjausparametrit-serviceen epäonnistui: $r")
    }
    Await.result(result, Duration(1, TimeUnit.MINUTES))
  }
}
