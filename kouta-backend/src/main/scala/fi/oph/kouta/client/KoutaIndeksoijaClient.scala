package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

case class Entity(oid: Option[String])
case class IndeksointiResult(result: List[Entity])

case class ValintaperusteEntity(id: Option[String])
case class ValintaperusteIndeksointiResult(result: List[ValintaperusteEntity])



object KoutaIndeksoijaClient extends KoutaIndeksoijaClient

class KoutaIndeksoijaClient extends HttpClient with CallerId with Logging {
  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties
  val foo = KoutaConfigurationFactory.configuration

  val errorHandler = (url: String, status: Int, response: String) => throw KoodistoQueryException(url, status, response)
  implicit val formats = DefaultFormats

  //todo, ehkä joku timeout jonnekin, tänne tai kutsujan päähän
  def quickIndexEntity(tyyppi: String, oid: String): Boolean = {
    try {
      val url = tyyppi match {
        case "koulutus" => urlProperties.url("kouta-indeksoija.koulutus.quick", oid)
        case "toteutus" => urlProperties.url("kouta-indeksoija.toteutus.quick", oid)
        case "hakukohde" => urlProperties.url("kouta-indeksoija.hakukohde.quick", oid)
        case "haku" => urlProperties.url("kouta-indeksoija.haku.quick", oid)
        case _ => throw new RuntimeException(s"Tuntematon tyyppi: $tyyppi")
      }
      logger.info(s"Pikaindeksoidaan $tyyppi $oid. Url: $url")
      post(url, oid) { response =>
        val isSuccess = parse(response).extract[IndeksointiResult].result.exists(e => e.oid.contains(oid))
        if (!isSuccess) {
          logger.warn(s"Pikaindeksointi epäonnistui: ($tyyppi $oid)")
        }
        isSuccess
      }
    } catch {
      case e: Exception => logger.error(s"Virhe pikaindeksoinnissa ($tyyppi $oid): $e")
      false
    }

  }

  def quickIndexValintaperuste(id: String): Boolean = {
    try {
      post(urlProperties.url("kouta-indeksoija.valintaperuste.quick", id), id) { response =>
        logger.info("indeksoija-response: " + response)
        parse(response).extract[ValintaperusteIndeksointiResult].result.exists(e => e.id.contains(id))
      }
    } catch {
      case e: Exception => logger.error(s"Virhe valintaperusteen $id pikaindeksoinnissa: $e")
      false
    }


  }

}

class MockKoutaIndeksoijaClient extends KoutaIndeksoijaClient {

  override def quickIndexEntity(tyyppi: String, oid: String) = true
  override def quickIndexValintaperuste(id: String) = true

}