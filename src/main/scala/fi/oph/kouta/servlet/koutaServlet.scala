package fi.oph.kouta.servlet

import fi.oph.kouta.domain.{Julkaisutila, Koulutustyyppi}
import org.json4s.{DefaultFormats, Formats}
import org.json4s.ext.EnumNameSerializer
import org.scalatra.{Ok, ScalatraServlet}
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.swagger.{Swagger, SwaggerSupport}

trait KoutaServlet extends ScalatraServlet with SwaggerSupport with JacksonJsonSupport {
  override protected implicit def jsonFormats: Formats = KoutaServlet.koutaFormats

  before() {
    contentType = formats("json")
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

