package fi.oph.kouta

import fi.oph.kouta.config.KoutaConfigurationFactory
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

  protected def organisaationServiceParams(oid:String) = Map(
    "oid" -> oid,
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

  val OphOid = "1.2.246.562.10.00000000001"
  val ParentOid = "1.2.246.562.10.594252633210"
  val ChildOid = "1.2.246.562.10.81934895871"
  val EvilChildOid = "1.2.246.562.10.66634895871"
  val GrandChildOid = "1.2.246.562.10.67603619189"
  val EvilGrandChildOid = "1.2.246.562.10.66603619189"
  val EvilCousin = "1.2.246.562.10.66634895666"

  val NotFoundOrganisaatioResponse = s"""{ "numHits": 0, "organisaatiot": []}"""
  lazy val DefaultResponse = responseFromResource("organisaatio")

  def singleOidOrganisaatioResponse(oid:String) = s"""{ "numHits": 1, "organisaatiot": [{"oid": "$oid", "parentOidPath": "$oid/$OphOid", "children" : []}]}"""

  def singleLevelChildrenResponse(parentOid: String, childOids: Seq[String]): String = {
    import org.json4s.jackson.Serialization.write
    import org.json4s.{DefaultFormats, Formats}

    implicit val formats: Formats = DefaultFormats

    val s = write(Map("numHits" -> (childOids.size + 1), "organisaatiot" -> Seq(
      Map("oid" -> parentOid, "parentOidPath" -> s"$parentOid/$OphOid", "children" ->
        childOids.map(oid => Map("oid" -> oid, "parentOidPath" -> s"$oid/$parentOid/$OphOid", "children" -> Seq.empty))
      )
    )))
    logger.error(s)
    s
  }

  def mockOrganisaatioResponse(oid:String, response:String = DefaultResponse) =
    mockGet("organisaatio-service.organisaatio.hierarkia", organisaationServiceParams(oid), response)
}
