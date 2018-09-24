package fi.oph.kouta.servlet

import fi.oph.kouta.domain.{Julkaisutila, Koulutustyyppi}
import org.json4s.{DefaultFormats, Formats}
import org.json4s.ext.EnumNameSerializer
import org.scalatra.{CorsSupport, Ok, ScalatraServlet}
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.swagger.{Swagger, SwaggerSupport}

trait KoutaServlet extends ScalatraServlet with SwaggerSupport with JacksonJsonSupport with CorsSupport {
  override protected implicit def jsonFormats: Formats = KoutaServlet.koutaFormats

  before() {
    contentType = formats("json")
  }

  options("/*") {
    response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"))
  }
}

object KoutaServlet {
  val koutaFormats: Formats = DefaultFormats +
    new EnumNameSerializer(Julkaisutila) + new EnumNameSerializer(Koulutustyyppi)
}

class HealthcheckServlet(implicit val swagger:Swagger) extends KoutaServlet {

  override val applicationDescription = "Healthcheck API"

  import org.json4s.JsonDSL._

  get("/", operation(apiOperation[String]("Healthcheck") summary "Healthcheck")) {
    Ok("message" -> "ok")
  }
}

