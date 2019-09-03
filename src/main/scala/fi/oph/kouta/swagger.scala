package fi.oph.kouta

import fi.oph.kouta.util.SwaggerModel
import org.reflections.Reflections
import org.scalatra.ScalatraServlet

import scala.collection.JavaConverters._

object SwaggerPaths {

  var paths: Map[String, List[String]] = Map[String, List[String]]()

  def registerPath(path: String, yaml: String) =
    paths += (path -> (paths.getOrElse(path, List[String]()) ++ List(yaml)))

}

class SwaggerServlet extends ScalatraServlet {

  get("/swagger.yaml") {
    response.setContentType("text/yaml")
    renderOpenapi3Yaml()
  }

  protected def renderOpenapi3Yaml(): String = {
    var yaml =
      s"""
         |openapi: 3.0.0
         |info:
         |  title: kouta-backend
         |  description: "Uusi koulutustarjonta"
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
         |  - url: http://localhost:8099/kouta-backend/
         |  - url: https://virkailija.untuvaopintopolku.fi/kouta-backend/
         |  - url: https://virkailija.hahtuvaopintopolku.fi/kouta-backend/
         |  - url: https://virkailija.testiopintopolku.fi/kouta-backend/
         |  - url: https://virkailija.opintopolku.fi/kouta-backend/
         |paths:
         |""".stripMargin

    SwaggerPaths.paths.foreach {
      case (path, op) =>
        yaml +=
          s"""  $path:
             |""".stripMargin
        yaml += op.mkString
    }

    yaml +=
      s"""
         |components:
         |  schemas:
         |""".stripMargin +
        getModelAnnotations
    yaml
  }

  def getModelAnnotations: String = {
    val reflections = new Reflections("fi.oph.kouta")

    val annotatedClasses = reflections.getTypesAnnotatedWith(classOf[SwaggerModel]).asScala.toSeq.sortBy(_.getSimpleName)

    annotatedClasses
      .map(_.getAnnotation(classOf[SwaggerModel]))
      .flatMap(Option(_))
      .map(_.value.stripMargin)
      .mkString
  }
}
