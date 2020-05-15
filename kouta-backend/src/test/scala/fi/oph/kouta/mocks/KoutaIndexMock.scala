package fi.oph.kouta.mocks

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.util.KoutaJsonFormats
import org.mockserver.model

trait KoutaIndexMock extends ServiceMocks with KoutaJsonFormats {

  override def startServiceMocking(): Unit = {
    super.startServiceMocking()
    urlProperties = Some(KoutaConfigurationFactory.configuration.urlProperties.addOverride("host.virkailija", s"localhost:$mockPort"))
  }

  def idResult(idField: String, id: String) = s"""{
    "nimi": {
     "fi": "Nimi fi $id",
     "sv": "Nimi sv $id",
     "en": "Nimi en $id"},
    "organisaatio": {
     "paikkakunta": {
       "koodiUri": "kunta_398",
       "nimi": {
         "sv": "Lahtis",
         "fi": "Lahti"
       }
     },
     "nimi": {
       "fi": "Koulutuskeskus"
     },
     "oid": "1.2.246.562.10.594252633210"
    },
    "muokkaaja": {
     "nimi": "Keijo Antero Kana",
     "oid": "1.2.246.562.24.62301161440"
    },
    "modified": "2019-02-08T09:57",
    "$idField": "$id",
    "tila": "julkaistu"}"""

  def createResponse(responseOids: Seq[String], idField: String = "oid"): String = {
    val result = responseOids.sorted.reverse.map(idResult(idField, _)).mkString(",")
    s"""{"totalCount": ${responseOids.size}, "result": [$result]}"""
  }

  private def mock(key: String, body: List[String], params: Map[String, String], response: String, statusCode: Int = 200): model.HttpRequest = statusCode match {
    case 200 => mockPost(key, body.sorted, params, response)
    case _   => mockPost(key, body.sorted, params, s"Error $statusCode", statusCode)
  }

  def mockKoulutusResponse(body: List[String], params: Map[String, String], responseOids: Seq[String] = Seq(), statusCode: Int = 200): model.HttpRequest =
    mock("kouta-index.koulutus.filtered-list", body, params, createResponse(responseOids), statusCode)

  def mockToteutusResponse(body: List[String], params: Map[String, String], responseOids: Seq[String] = Seq(), statusCode: Int = 200): model.HttpRequest =
    mock("kouta-index.toteutus.filtered-list", body, params, createResponse(responseOids), statusCode)

  def mockHakuResponse(body: List[String], params: Map[String, String], responseOids: Seq[String] = Seq(), statusCode: Int = 200): model.HttpRequest =
    mock("kouta-index.haku.filtered-list", body, params, createResponse(responseOids), statusCode)

  def mockHakukohdeResponse(body: List[String], params: Map[String, String], responseOids: Seq[String] = Seq(), statusCode: Int = 200): model.HttpRequest =
    mock("kouta-index.hakukohde.filtered-list", body, params, createResponse(responseOids), statusCode)

  def mockValintaperusteResponse(body: List[String], params: Map[String, String], responseIds: Seq[String] = Seq(), statusCode: Int = 200): model.HttpRequest =
    mock("kouta-index.valintaperuste.filtered-list", body, params, createResponse(responseIds, "id"), statusCode)

}

object KoutaIndexMock extends KoutaIndexMock
