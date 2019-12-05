package fi.oph.kouta

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.tcp.PortChecker
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.Parameter.param
import org.mockserver.model.HttpResponse.response

import scala.io.Source
import fi.vm.sade.utils.slf4j.Logging

/* If you need to debug mocks,
   change log4j.logger.org.mockserver=INFO
   in test/resources/log4j.properties */

sealed trait ServiceMocks extends Logging {

  var mockServer:Option[ClientAndServer] = None
  var urlProperties:Option[OphProperties] = None

  def startServiceMocking(port:Int = PortChecker.findFreeLocalPort) = {
    logger.info(s"Mocking oph services in port $port")
    mockServer = Some(startClientAndServer(port))
    urlProperties = Some(KoutaConfigurationFactory.configuration.urlProperties.addOverride("host.virkailija", s"localhost:$port"))
  }

  def stopServiceMocking() = mockServer.foreach(_.stop())

  def clearServiceMocks() = mockServer.foreach(_.reset())

  protected def getMockPath(key:String) = urlProperties.map(p => new java.net.URL(p.url(key)).getPath).getOrElse("/")

  protected def responseFromResource(filename:String) = Source.fromInputStream(
    getClass().getClassLoader().getResourceAsStream(s"data/$filename.json")).mkString

  protected def organisaationServiceParams(oid: OrganisaatioOid) = Map(
    "oid" -> oid.s,
    "aktiiviset" -> "true",
    "suunnitellut" -> "true",
    "lakkautetut" -> "false")

  protected def mockGet(key:String, params:Map[String,String], responseString:String) = {
    import scala.collection.JavaConverters._
    mockServer.foreach(_.when(
      request()
        .withMethod("GET")
        //.withSecure(true) TODO: https toimimaan
        .withPath(getMockPath(key))
        .withQueryStringParameters(params.map(x => param(x._1, x._2)).toList.asJava)
    ).respond(
      response(responseString)
    ))
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
