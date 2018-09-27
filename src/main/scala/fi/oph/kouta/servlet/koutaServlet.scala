package fi.oph.kouta.servlet

import java.text.ParseException
import java.util.NoSuchElementException

import fi.oph.kouta.domain.{Julkaisutila, Kieli, Koulutustyyppi}
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.JsonAST.JString
import org.json4s.{CustomKeySerializer, CustomSerializer, DefaultFormats, Formats, MappingException}
import org.json4s.ext.EnumNameSerializer
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.swagger.{Swagger, SwaggerSupport}

trait KoutaServlet extends ScalatraServlet with SwaggerSupport with JacksonJsonSupport with Logging {
  override protected implicit def jsonFormats: Formats = KoutaServlet.koutaFormats

  before() {
    contentType = formats("json")
  }

  def errorMsgFromRequest() = {
    def msgBody = request.body.length match {
      case x if x > 500000 => request.body.substring(0, 500000)
      case _ => request.body
    }
    s"""Error ${request.getMethod} ${request.getContextPath} => ${msgBody}"""
  }

  error {
    case t: Throwable => {
      t match {
        /*case e: AuthenticationFailedException =>
          logger.warn("authentication failed", e)
          Unauthorized("error" -> "Unauthorized")
        case e: AuthorizationFailedException =>
          logger.warn("authorization failed", e)
          Forbidden("error" -> "Forbidden")*/
        case e: IllegalStateException =>
        case e: IllegalArgumentException =>
        case e: MappingException =>
        case e: ParseException =>
        case e: NoSuchElementException =>
          logger.warn(errorMsgFromRequest(), e)
          BadRequest("error" -> e.getMessage)
        case e =>
          logger.error(errorMsgFromRequest(), e)
          InternalServerError("error" -> "500 Internal Server Error")
      }
    }
  }
}

object KoutaServlet {
  val kieliSerializer = new EnumNameSerializer(Kieli)
  val koutaFormats: Formats = DefaultFormats +
    new EnumNameSerializer(Julkaisutila) + new EnumNameSerializer(Koulutustyyppi) +
    new CustomKeySerializer[Kieli.Kieli](formats => ( {
      case s: String => Kieli.values.find(_.toString == s.toLowerCase).get
    }, {
      case k: Kieli.Kieli => k.toString
    }))
}

class HealthcheckServlet(implicit val swagger:Swagger) extends KoutaServlet {

  override val applicationDescription = "Healthcheck API"

  import org.json4s.JsonDSL._

  get("/", operation(apiOperation[String]("Healthcheck") summary "Healthcheck")) {
    Ok("message" -> "ok")
  }
}

