package fi.oph.kouta.mocks

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.slf4j.Logging
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.Parameter.param

import scala.io.Source

/* If you need to debug mocks,
   change log4j.logger.org.mockserver=INFO
   in test/resources/log4j.properties */

sealed trait ServiceMocks extends Logging {

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

  protected def organisaationServiceParams(oid: OrganisaatioOid) = Map(
    "oid" -> oid.s,
    "aktiiviset" -> "true",
    "suunnitellut" -> "true",
    "lakkautetut" -> "false")

  protected def mockGet(key:String, params:Map[String,String], responseString:String, statusCode:Int = 200): model.HttpRequest = {
    import scala.collection.JavaConverters._
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
}

trait OrganisaatioServiceMock extends ServiceMocks {

  val OphOid = OrganisaatioOid("1.2.246.562.10.00000000001")
  val ParentOid = OrganisaatioOid("1.2.246.562.10.594252633210")
  val ChildOid = OrganisaatioOid("1.2.246.562.10.81934895871")
  val EvilChildOid = OrganisaatioOid("1.2.246.562.10.66634895871")
  val GrandChildOid = OrganisaatioOid("1.2.246.562.10.67603619189")
  val EvilGrandChildOid = OrganisaatioOid("1.2.246.562.10.66603619189")
  val EvilCousin = OrganisaatioOid("1.2.246.562.10.66634895666")

  val NotFoundOrganisaatioResponse = s"""{ "numHits": 0, "organisaatiot": []}"""
  lazy val DefaultResponse = responseFromResource("organisaatio")

  def singleOidOrganisaatioResponse(oid:String) = s"""{ "numHits": 1, "organisaatiot": [{"oid": "$oid", "parentOidPath": "$oid/$OphOid", "oppilaitostyyppi": "oppilaitostyyppi_21#1", "children" : []}]}"""

  def mockOrganisaatioResponse(oid: OrganisaatioOid, response: String = DefaultResponse): Unit =
    mockGet("organisaatio-service.organisaatio.hierarkia", organisaationServiceParams(oid), response)

  def mockOrganisaatioResponses(oids: OrganisaatioOid*): Unit = oids.foreach(mockOrganisaatioResponse(_))

  def mockSingleOrganisaatioResponses(organisaatioOids: OrganisaatioOid*): Unit = organisaatioOids.foreach { oid =>
    mockOrganisaatioResponse(oid, singleOidOrganisaatioResponse(oid.s))
  }

  def mockSingleOrganisaatioResponses(first: String, organisaatioOids: String*): Unit =
    mockSingleOrganisaatioResponses((organisaatioOids :+ first).map(OrganisaatioOid):_*)
}

object OrganisaatioServiceMock extends OrganisaatioServiceMock

trait KoutaIndexMock extends ServiceMocks {

  def oidResult(oid:String) = s"""{
    "nimi": {
     "fi": "Nimi fi $oid",
     "sv": "Nimi sv $oid",
     "en": "Nimi en $oid"},
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
    "oid": "$oid",
    "tila": "julkaistu"}"""

  def idResult(id:String) = s"""{
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
    "id": "$id",
    "tila": "julkaistu"}"""

  def createResponse(responseOids: Seq[String], isOids: Boolean = true): String = {
    val result = responseOids.sorted.reverse.map { o =>
      if(isOids) oidResult(o) else idResult(o)
    }.mkString(",")
    s"""{"totalCount": ${responseOids.size}, "result": [$result]}"""
  }

  def mockKoulutusResponse(params: Map[String, String], responseOids: Seq[String] = Seq(), statusCode: Int = 200): model.HttpRequest = statusCode match {
    case 200 => mockGet("kouta-index.koulutus.filtered-list", params, createResponse(responseOids))
    case _   => mockGet("kouta-index.koulutus.filtered-list", params, s"Error $statusCode", statusCode)
  }

  def mockToteutusResponse(params: Map[String, String], responseOids: Seq[String] = Seq(), statusCode: Int = 200): model.HttpRequest = statusCode match {
    case 200 => mockGet("kouta-index.toteutus.filtered-list", params, createResponse(responseOids))
    case _   => mockGet("kouta-index.toteutus.filtered-list", params, s"Error $statusCode", statusCode)
  }

  def mockHakuResponse(params: Map[String, String], responseOids: Seq[String] = Seq(), statusCode: Int = 200): model.HttpRequest = statusCode match {
    case 200 => mockGet("kouta-index.haku.filtered-list", params, createResponse(responseOids))
    case _   => mockGet("kouta-index.haku.filtered-list", params, s"Error $statusCode", statusCode)
  }

  def mockHakukohdeResponse(params: Map[String, String], responseOids: Seq[String] = Seq(), statusCode: Int = 200): model.HttpRequest = statusCode match {
    case 200 => mockGet("kouta-index.hakukohde.filtered-list", params, createResponse(responseOids))
    case _   => mockGet("kouta-index.hakukohde.filtered-list", params, s"Error $statusCode", statusCode)
  }

  def mockValintaperusteResponse(params: Map[String, String], responseIds: Seq[String] = Seq(), statusCode: Int = 200): model.HttpRequest = statusCode match {
    case 200 => mockGet("kouta-index.valintaperuste.filtered-list", params, createResponse(responseIds, false))
    case _   => mockGet("kouta-index.valintaperuste.filtered-list", params, s"Error $statusCode", statusCode)
  }
}

object KoutaIndexMock extends KoutaIndexMock
