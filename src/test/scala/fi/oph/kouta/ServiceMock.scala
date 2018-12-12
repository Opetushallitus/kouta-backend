package fi.oph.kouta

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.utils.tcp.PortChecker
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.Parameter.param
import org.mockserver.model.HttpResponse.response

import scala.io.Source

/* If you need to debug mocks,
   change log4j.logger.org.mockserver=INFO
   in test/resources/log4j.properties */

object ServiceMocks {
  lazy val MockPort = PortChecker.findFreeLocalPort
  lazy val mockServer = startClientAndServer(MockPort)
  lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties.addOverride("host.virkailija", s"localhost:$MockPort")

  def stopServiceMocks() = mockServer.stop()

  def resetServiceMocks() = mockServer.reset()

  def getMockPath(key:String) = new java.net.URL(urlProperties.url(key)).getPath

  def getJsonResourceAsString(filename:String) = Source.fromInputStream(
    getClass().getClassLoader().getResourceAsStream(s"data/$filename.json")).mkString

  def organisaationServiceParams(oid:String) = Map(
    "oid" -> oid,
    "aktiiviset" -> "true",
    "suunnitellut" -> "true",
    "lakkautetut" -> "false")

  val EmptyOrganisaatioResponse = s"""{ "numHits": 0, "organisaatiot": []}"""
  def singleOidOrganisaatioResponse(oid:String) = s"""{ "numHits": 1, "organisaatiot": [{"oid": "$oid"}]}"""

  def mockOrganisaatioServiceFromResource(oid:String, filename:String = "organisaatio") =
    mockGet("organisaatio-service.organisaatio.hierarkia", organisaationServiceParams(oid), getJsonResourceAsString(filename))

  def mockOrganisaatioService(oid:String, response:String) =
    mockGet("organisaatio-service.organisaatio.hierarkia", organisaationServiceParams(oid), response)

  def mockGet(key:String, params:Map[String,String], responseString:String) = {
    import scala.collection.JavaConverters._
    mockServer.when(
      request()
        .withMethod("GET")
        //.withSecure(true) TODO: https toimimaan
        .withPath(getMockPath(key))
        .withQueryStringParameters(params.map(x => param(x._1, x._2)).toList.asJava)
    ).respond(
      response(responseString)
    )
  }
}