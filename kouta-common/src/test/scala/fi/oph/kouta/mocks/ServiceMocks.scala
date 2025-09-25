package fi.oph.kouta.mocks

import fi.oph.kouta.logging.Logging
import org.json4s.Formats
import org.json4s.jackson.Serialization.write
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.github.tomakehurst.wiremock.stubbing.StubMapping

import scala.collection.JavaConverters._
import scala.io.Source

abstract class ServiceMocker extends Logging {
  var mockServer: Option[WireMockServer] = None
  var mockPort: Int                     = _

  private def assertInitialized(): Unit = {
    if (mockServer.isEmpty) {
      throw new AssertionError(
        "Mock server is uninitialized! Please call startServiceMocking() before trying to use mock server."
      )
    }
  }

  def startServiceMocking(port: Int): Unit = {
    val config = wireMockConfig()
      .port(port)
    mockServer = Some(new WireMockServer(config))
    mockServer.foreach(_.start())
    mockPort = mockServer.get.port()
    logger.info(s"Mocking oph services in port $mockPort")
  }

  def getMockBaseUrl(withProtocol: Boolean): String = {
    assertInitialized()
    val protocol = if (withProtocol) "http://" else ""
    s"${protocol}localhost:$mockPort"
  }

  def stopServiceMocking(): Unit = {
    assertInitialized()
    mockServer.foreach(_.stop())
  }

  def clearServiceMocks(): Unit = {
    assertInitialized()
    mockServer.foreach(_.resetAll())
  }

  def clearMock(url: String, method: String): Unit = {
    assertInitialized()
    mockServer.foreach { server =>
      server.removeStub(server.listAllStubMappings().getMappings.asScala.find { stub =>
        stub.getRequest.getMethod.getName.equalsIgnoreCase(method) &&
        stub.getRequest.getUrl == url
      }.orNull)
    }
  }

  def mockRequest(method: String, path: String, queryParams: Map[String, String],
                  headers: Map[String, String], requestBody: Option[String],
                  responseString: String, statusCode: Int): StubMapping = {
    assertInitialized()
    
    val requestBuilder = request(method, urlPathEqualTo(path))

    queryParams.foreach { case (key, value) =>
      requestBuilder.withQueryParam(key, equalTo(value))
    }

    headers.foreach { case (key, value) =>
      requestBuilder.withHeader(key, equalTo(value))
    }

    requestBody.foreach { body =>
      requestBuilder.withRequestBody(equalToJson(body))
    }

    mockServer.get.stubFor(
      requestBuilder.willReturn(
        aResponse()
          .withStatus(statusCode)
          .withBody(responseString)
      )
    )
  }
}

object ServiceMocker extends ServiceMocker

trait ServiceMockBase extends UrlProperties {
  private val mocker = ServiceMocker

  def getMockPath(key: String, param: Option[String] = None): String =
    urlProperties match {
      case None => "/"
      case Some(up) =>
        new java.net.URL(param match {
          case None => up.url(key)
          case Some(p) => up.url(key, p)
        }).getPath
    }

  protected def getMockPath(key: String, params: Seq[String]): String = urlProperties match {
    case None     => "/"
    case Some(up) => new java.net.URL(if (params.nonEmpty) up.url(key, params: _*) else up.url(key)).getPath
  }

  def clearMock(url: String, method: String = "GET"): Unit = mocker.clearMock(url, method)

  def clearServiceMocks(): Unit = mocker.clearServiceMocks()

  def responseFromResource(filename: String): String =
    Source
      .fromInputStream(
        getClass.getClassLoader.getResourceAsStream(s"data/$filename.json")
      )
      .mkString

  def mockGet(path: String, params: Map[String, String], responseString: String, statusCode: Int = 200): StubMapping = {
    mocker.mockRequest("GET", path, params, headers = Map.empty, requestBody = None, responseString, statusCode)
  }

  def mockPost[B <: AnyRef](
      path: String,
      body: B,
      params: Map[String, String] = Map.empty,
      responseString: String,
      statusCode: Int = 200,
      headers: Map[String, String] = Map.empty
  )(implicit jsonFormats: Formats): StubMapping = {
    val jsonBody = write[B](body)
    mocker.mockRequest("POST", path, params, headers, Some(jsonBody), responseString, statusCode)
  }

  def mockPut[B <: AnyRef](
      path: String,
      body: B,
      params: Map[String, String] = Map.empty,
      responseString: String,
      statusCode: Int = 200,
      headers: Map[String, String] = Map.empty
  )(implicit jsonFormats: Formats): StubMapping = {
    val jsonBody = write[B](body)
    mocker.mockRequest("PUT", path, params, headers, Some(jsonBody), responseString, statusCode)
  }
}
