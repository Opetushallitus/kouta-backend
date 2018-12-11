package fi.oph.kouta.client

import fi.oph.kouta.TestSetups
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.utils.tcp.PortChecker
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.{response => mockresponse}
import org.mockserver.model.Parameter.param
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach, DoNotDiscover}
import org.scalatra.test.scalatest.ScalatraFlatSpec

import scala.io.Source

@DoNotDiscover
class KoutaClientSpec extends ScalatraFlatSpec with BeforeAndAfter with BeforeAndAfterEach {

  val MockPort = PortChecker.findFreeLocalPort
  var mockServer:ClientAndServer = null

  TestSetups.setupWithTemplate(1234)
  val urlProperties = KoutaConfigurationFactory.configuration.urlProperties.addOverride("host.virkailija", s"localhost:$MockPort")

  override def beforeAll() = {
    super.beforeAll()
    mockServer = startClientAndServer(MockPort)
  }

  override def afterAll() = {
    super.afterAll()
    mockServer.stop()
  }

  override def afterEach {
    super.afterEach()
    mockServer.reset()
  }

  def getMockPath(key:String) = new java.net.URL(urlProperties.url(key)).getPath

  def getJsonResourceAsString(filename:String) = Source.fromInputStream(
    getClass().getClassLoader().getResourceAsStream(s"data/$filename.json")).mkString

  def mockGet(key:String, params:Map[String,String], responseString:String) = {
    import scala.collection.JavaConverters._
    mockServer.when(
      request()
        .withMethod("GET")
        //.withSecure(true) TODO: https toimimaan
        .withPath(getMockPath(key))
        .withQueryStringParameters(
          params.map(x => param(x._1, x._2)).toList.asJava
        )
    ).respond(
      mockresponse(responseString)
    )
  }
}
