package fi.oph.kouta.mocks

import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.Formats
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

trait ServiceMocks extends Logging {

  var mockServer: Option[ClientAndServer] = None
  var urlProperties: Option[OphProperties] = None
  var mockPort: Int = _

  def startServiceMocking(): Unit = {
    mockServer = Some(startClientAndServer())
    mockPort = mockServer.get.getLocalPort
    logger.info(s"Mocking oph services in port $mockPort")
  }

  def stopServiceMocking(): Unit = mockServer.foreach(_.stop())

  def clearServiceMocks(): Unit = mockServer.foreach(_.reset())

  def clearMock(request: model.HttpRequest): Unit = mockServer.foreach(_.clear(request))

  protected def getMockPath(key: String): String =
    urlProperties match {
      case Some(p) => new java.net.URL(p.url(key)).getPath
      case None => throw new RuntimeException("urlProperties not set")
    }

  protected def responseFromResource(filename: String): String =
    Source.fromInputStream(
      getClass.getClassLoader.getResourceAsStream(s"data/$filename.json")
    ).mkString

  protected def organisaationServiceParams(oid: OrganisaatioOid, lakkautetut: Boolean = false) = Map(
    "oid" -> oid.s,
    "aktiiviset" -> "true",
    "suunnitellut" -> "true",
    "lakkautetut" -> lakkautetut.toString)

  protected def mockGet(key: String, params: Map[String, String], responseString: String, statusCode: Int = 200): model.HttpRequest = {
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

  protected def mockPost[B <: AnyRef](key: String, body: B, params: Map[String, String], responseString: String, statusCode: Int = 200)(implicit jsonFormats: Formats): model.HttpRequest = {
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
