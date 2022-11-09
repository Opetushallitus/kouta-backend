package fi.oph.kouta.mocks

import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.Formats
import org.json4s.jackson.Serialization.write
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.matchers.MatchType
import org.mockserver.model.Header.header
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.Parameter.param
import org.mockserver.model.{Body, HttpRequest, JsonBody}

import scala.collection.JavaConverters._
import scala.io.Source

/* If you need to debug mocks,
   change log4j.logger.org.mockserver=INFO
   in test/resources/log4j.properties */

abstract class ServiceMocker extends Logging {

  var mockServer: Option[ClientAndServer]  = None
  var mockPort: Int                        = _

  private def assertInitialized(): Unit = {
    if (mockServer.isEmpty) {
      throw new AssertionError(
        "Mock server is uninitialized! Please call startServiceMocking() before trying to use mock server."
      )
    }
  }

  def startServiceMocking(): Unit = {
    mockServer = Some(startClientAndServer())
    mockPort = mockServer.get.getLocalPort
    logger.info(s"Mocking oph services in port $mockPort")
  }

  def getMockBaseUrl(withProtocol: Boolean): String = {
    assertInitialized
    val protocol = if (withProtocol) "http://" else ""
    s"${protocol}localhost:$mockPort"
  }

  def stopServiceMocking(): Unit = {
    assertInitialized
    mockServer.foreach(_.stop())
  }

  def clearServiceMocks(): Unit = {
    assertInitialized
    mockServer.foreach(_.reset())
  }

  def clearMock(request: HttpRequest): Unit = {
    assertInitialized
    mockServer.foreach(_.clear(request))
  }

  def mockRequest(request: HttpRequest, responseString: String, statusCode: Int) = {
    assertInitialized
    mockServer.foreach(
      _.when(
        request
      ).respond(
        response(responseString).withStatusCode(statusCode)
      )
    )
    request
  }
}

object ServiceMocker extends ServiceMocker

abstract trait ServiceMockBase {
  private val mocker = ServiceMocker

  def getMockPath(key: String, param: Option[String] = None): String = TestGlobals.urlProperties match {
    case None => "/"
    case Some(up) =>
      new java.net.URL(param match {
        case None    => up.url(key)
        case Some(p) => up.url(key, p)
      }).getPath
  }

  def clearMock(request: HttpRequest): Unit = mocker.clearMock(request)

  def clearServiceMocks(): Unit = mocker.clearServiceMocks()

  def responseFromResource(filename: String): String =
    Source
      .fromInputStream(
        getClass.getClassLoader.getResourceAsStream(s"data/$filename.json")
      )
      .mkString

  def mockGet(path: String, params: Map[String, String], responseString: String, statusCode: Int = 200): HttpRequest = {
    val req: HttpRequest = request()
      .withMethod("GET")
      //.withSecure(true) TODO: https toimimaan
      .withPath(path)
      .withQueryStringParameters(params.map(x => param(x._1, x._2)).toList.asJava)
    mocker.mockRequest(req, responseString, statusCode)
  }

  private def postRequest[B <: AnyRef](
      path: String,
      body: B,
      params: Map[String, String],
      headers: Map[String, String],
      matchType: MatchType
  )(implicit jsonFormats: Formats): HttpRequest =
    request()
      .withMethod("POST")
      .withPath(path)
      .withQueryStringParameters(params.map(x => param(x._1, x._2)).toList.asJava)
      .withHeaders(headers.map(x => header(x._1, x._2)).toList.asJava)
      .withBody(JsonBody.json(write[B](body), matchType).asInstanceOf[Body[_]])

  def mockPost[B <: AnyRef](
      path: String,
      body: B,
      params: Map[String, String] = Map.empty,
      responseString: String,
      statusCode: Int = 200,
      headers: Map[String, String] = Map.empty,
      matchType: MatchType = MatchType.STRICT
  )(implicit jsonFormats: Formats): HttpRequest = {
    val req: HttpRequest = postRequest(path, body, params, headers, matchType)
    mocker.mockRequest(req, responseString, statusCode)
  }

  def mockPut[B <: AnyRef](
      path: String,
      body: B,
      params: Map[String, String] = Map.empty,
      responseString: String,
      statusCode: Int = 200,
      headers: Map[String, String] = Map.empty,
      matchType: MatchType = MatchType.STRICT
  )(implicit jsonFormats: Formats): HttpRequest = {
    val req: HttpRequest = postRequest(path, body, params, headers, matchType).withMethod("PUT")
    mocker.mockRequest(req, responseString, statusCode)
  }
}
