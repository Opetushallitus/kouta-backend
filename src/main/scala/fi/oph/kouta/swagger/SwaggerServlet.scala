package fi.oph.kouta.swagger

import fi.oph.kouta.servlet.KoutaServlet
import org.reflections.Reflections
import org.scalatra.ScalatraServlet

import scala.collection.JavaConverters._

class SwaggerServlet extends ScalatraServlet {

  get("/swagger.yaml") {
    response.setContentType("text/yaml")
    renderOpenapi3Yaml
  }

  protected lazy val renderOpenapi3Yaml: String = {
    val header =
      """
        |openapi: 3.0.0
        |info:
        |  title: kouta-backend
        |  description: >
        |    Uuden koulutustarjonnan sisäinen API
        |
        |
        |    Helpoin tapa kirjautua sisään Swagger-ui:n käyttäjälle on avata
        |    [/kouta-backend/auth/login](/kouta-backend/auth/login) uuteen selainikkunaan.
        |    Jos näkyviin tulee `{"personOid":"1.2.246.562.24.xxxx"}` on kirjautuminen onnistunut. Jos näkyviin tulee
        |    opintopolun kirjautumisikkuna, kirjaudu sisään.
        |  version: 0.1-SNAPSHOT
        |  termsOfService: https://opintopolku.fi/wp/fi/opintopolku/tietoa-palvelusta/
        |  contact:
        |    name: ""
        |    email: verkkotoimitus_opintopolku@oph.fi
        |    url: ""
        |  license:
        |    name: "EUPL 1.1 or latest approved by the European Commission"
        |    url: "http://www.osor.eu/eupl/"
        |servers:
        |  - url: /kouta-backend/
        |  - url: http://localhost:8099/kouta-backend/
        |  - url: https://virkailija.untuvaopintopolku.fi/kouta-backend/
        |  - url: https://virkailija.hahtuvaopintopolku.fi/kouta-backend/
        |  - url: https://virkailija.testiopintopolku.fi/kouta-backend/
        |  - url: https://virkailija.opintopolku.fi/kouta-backend/
        |paths:
        |""".stripMargin

    val paths = SwaggerPaths.paths.map {
      case (path, ops) =>
        s"""  $path:
           |    parameters:
           |      - $$ref: '#/components/parameters/callerId'
           |""".stripMargin +
          ops.mkString
    }.mkString

    val modelHeader =
      s"""
         |components:
         |  headers:
         |    x-Last-Modified:
         |      schema:
         |        type: string
         |        example: Mon, 23 Sep 2019 17:55:31 GMT
         |      description: Viimeisimmän muokkauksen aika
         |  parameters:
         |    callerId:
         |      in: header
         |      name: Caller-Id
         |      schema:
         |        type: string
         |        default: kouta-backend-swagger
         |      required: true
         |      description: Kutsujan <a href="https://confluence.csc.fi/pages/viewpage.action?pageId=50858064">Caller ID</a>
         |    ifUnmodifiedSince:
         |      in: header
         |      name: ${KoutaServlet.IfUnmodifiedSinceHeader}
         |      schema:
         |        type: string
         |        example: Mon, 23 Sep 2019 17:55:31 GMT
         |      required: true
         |      description: Mitä ${KoutaServlet.LastModifiedHeader} on tästä sanonut
         |  schemas:
         |""".stripMargin

    Seq(header, paths, modelHeader, getModelAnnotations).mkString
  }

  private def getModelAnnotations: String = {
    val reflections = new Reflections("fi.oph.kouta")

    reflections
      .getTypesAnnotatedWith(classOf[SwaggerModel])
      .asScala
      .toSeq
      .sortBy(_.getSimpleName)
      .map(_.getAnnotation(classOf[SwaggerModel]))
      .filter(_ != null)
      .map(_.value.stripMargin)
      .mkString
  }
}
