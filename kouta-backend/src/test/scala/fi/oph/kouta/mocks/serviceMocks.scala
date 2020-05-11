package fi.oph.kouta.mocks

import fi.oph.kouta.TestOids.OphOid
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.Serialization.write
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.matchers.MatchType
import org.mockserver.model
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.Parameter.param
import org.mockserver.model.{Body, JsonBody}

import scala.collection.JavaConverters._
import scala.io.Source

/* If you need to debug mocks,
   change log4j.logger.org.mockserver=INFO
   in test/resources/log4j.properties */

sealed trait ServiceMocks extends Logging with KoutaJsonFormats {

  var mockServer:Option[ClientAndServer] = None
  var urlProperties:Option[OphProperties] = None

  def startServiceMocking() = {
    mockServer = Some(startClientAndServer())
    val port = mockServer.get.getLocalPort
    logger.info(s"Mocking oph services in port $port")
    urlProperties = Some(KoutaConfigurationFactory.configuration.urlProperties.addOverride("host.virkailija", s"localhost:$port"))
  }

  def stopServiceMocking() = mockServer.foreach(_.stop())

  def clearServiceMocks() = mockServer.foreach(_.reset())

  def clearMock(request:model.HttpRequest) = mockServer.foreach(_.clear(request))

  protected def getMockPath(key:String) = urlProperties.map(p => new java.net.URL(p.url(key)).getPath).getOrElse("/")

  protected def responseFromResource(filename:String) = Source.fromInputStream(
    getClass().getClassLoader().getResourceAsStream(s"data/$filename.json")).mkString

  protected def organisaationServiceParams(oid: OrganisaatioOid, lakkautetut: Boolean = false) = Map(
    "oid" -> oid.s,
    "aktiiviset" -> "true",
    "suunnitellut" -> "true",
    "lakkautetut" -> lakkautetut.toString)

  protected def mockGet(key:String, params:Map[String,String], responseString:String, statusCode:Int = 200): model.HttpRequest = {
    val req: model.HttpRequest = request()
      .withMethod("GET")
      //.withSecure(true) TODO: https toimimaan
      .withPath(getMockPath(key))
      .withQueryStringParameters(params.map(x => param(x._1, x._2)).toList.asJava)
    mockServer.foreach(_.when(
      req
    ).respond(
      response(responseString).withStatusCode(statusCode)
    ))
    req
  }

  protected def mockPost[B <: AnyRef](key: String, body: B, params: Map[String, String], responseString: String, statusCode: Int = 200): model.HttpRequest = {
    val req: model.HttpRequest = request()
      .withMethod("POST")
      .withPath(getMockPath(key))
      .withQueryStringParameters(params.map(x => param(x._1, x._2)).toList.asJava)
      .withBody(JsonBody.json(write[B](body), MatchType.STRICT).asInstanceOf[Body[_]])
    mockServer.foreach(_.when(
      req
    ).respond(
      response(responseString).withStatusCode(statusCode)
    ))
    req
  }
}

trait OrganisaatioServiceMock extends ServiceMocks {

  val NotFoundOrganisaatioResponse = s"""{ "numHits": 0, "organisaatiot": []}"""
  lazy val DefaultResponse = responseFromResource("organisaatio")

  def singleOidOrganisaatioResponse(oid:String) = s"""{ "numHits": 1, "organisaatiot": [{"oid": "$oid", "parentOidPath": "$oid/$OphOid", "oppilaitostyyppi": "oppilaitostyyppi_21#1", "children" : []}]}"""

  def mockOrganisaatioResponse(oid: OrganisaatioOid, response: String = DefaultResponse, lakkautetut: Boolean = false): Unit =
    mockGet("organisaatio-service.organisaatio.hierarkia", organisaationServiceParams(oid, lakkautetut), response)

  def mockOrganisaatioResponses(oids: OrganisaatioOid*): Unit = oids.foreach(mockOrganisaatioResponse(_))

  def mockSingleOrganisaatioResponses(organisaatioOids: OrganisaatioOid*): Unit = organisaatioOids.foreach { oid =>
    mockOrganisaatioResponse(oid, singleOidOrganisaatioResponse(oid.s))
  }

  def mockSingleOrganisaatioResponses(first: String, organisaatioOids: String*): Unit =
    mockSingleOrganisaatioResponses((organisaatioOids :+ first).map(OrganisaatioOid):_*)
}

object OrganisaatioServiceMock extends OrganisaatioServiceMock

trait KoutaIndexMock extends ServiceMocks {

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
