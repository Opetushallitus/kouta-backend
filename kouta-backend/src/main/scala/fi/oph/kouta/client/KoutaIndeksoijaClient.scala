package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import java.util.concurrent.{TimeUnit, TimeoutException}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

case class Entity(oid: Option[String])
case class IndeksointiResult(result: List[Entity])

case class ValintaperusteEntity(id: Option[String])
case class ValintaperusteIndeksointiResult(result: List[ValintaperusteEntity])



object KoutaIndeksoijaClient extends KoutaIndeksoijaClient

class KoutaIndeksoijaClient extends HttpClient with CallerId with Logging {
  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties
  implicit val formats = DefaultFormats

  def quickIndexEntity(tyyppi: String, oid: String): List[String] = {
    try {
      val url = tyyppi match {
        case "koulutus" => urlProperties.url("kouta-indeksoija.koulutus.quick", oid)
        case "toteutus" => urlProperties.url("kouta-indeksoija.toteutus.quick", oid)
        case "hakukohde" => urlProperties.url("kouta-indeksoija.hakukohde.quick", oid)
        case "haku" => urlProperties.url("kouta-indeksoija.haku.quick", oid)
        case _ => throw new RuntimeException(s"Tuntematon tyyppi: $tyyppi")
      }
      logger.info(s"Pikaindeksoidaan $tyyppi $oid. Url: $url")
      val resultF: Future[Boolean] = Future {
        post(url, oid) { response =>
          val isSuccess = parse(response).extract[IndeksointiResult].result.exists(e => e.oid.contains(oid))
          if (!isSuccess) {
            logger.warn(s"Pikaindeksointi epäonnistui: ($tyyppi $oid)")
          }
          isSuccess
        }
      }
      val result = Await.result(resultF, Duration(5, TimeUnit.SECONDS))
      if (!result) List("varoitukset.indeksointiEpaonnistui") else List.empty
    } catch {
      case e: TimeoutException => logger.error(s"Pikaindeksointi aikakatkaistiin ($tyyppi $oid)")
        List("varoitukset.indeksointiAikakatkaisu")
      case e: Exception => logger.error(s"Virhe pikaindeksoinnissa ($tyyppi $oid): $e")
        List("varoitukset.indeksointiEpaonnistui")
    }

  }

  def quickIndexValintaperuste(id: String): List[String] = {
    try {
      val resultF: Future[Boolean] = Future {
        post(urlProperties.url("kouta-indeksoija.valintaperuste.quick", id), id) { response =>
          val isSuccess = parse(response).extract[ValintaperusteIndeksointiResult].result.exists(e => e.id.contains(id))
          if (!isSuccess) {
            logger.warn(s"Pikaindeksointi epäonnistui (valintaperuste $id)")
          }
          isSuccess
        }
      }
      val result = Await.result(resultF, Duration(5, TimeUnit.SECONDS))
      if (!result) List("varoitukset.indeksointiEpaonnistui") else List.empty
    } catch {
      case e: TimeoutException => logger.error(s"Pikaindeksointi aikakatkaistiin! (Valintaperuste $id)", e)
        List("varoitukset.indeksointiAikakatkaisu")
      case e: Exception => logger.error(s"Virhe valintaperusteen $id pikaindeksoinnissa: $e")
        List("varoitukset.indeksointiEpaonnistui")
    }
  }
}

class MockKoutaIndeksoijaClient extends KoutaIndeksoijaClient {

  override def quickIndexEntity(tyyppi: String, oid: String) = List.empty
  override def quickIndexValintaperuste(id: String) = List.empty
}