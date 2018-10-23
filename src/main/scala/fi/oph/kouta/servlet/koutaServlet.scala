package fi.oph.kouta.servlet

import java.text.ParseException
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.{ConcurrentModificationException, NoSuchElementException}

import fi.oph.kouta.domain._
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.MappingException
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.swagger.DataType.{ContainerDataType, ValueDataType}
import org.scalatra.swagger._

import scala.util.{Failure, Try}

trait KoutaServlet extends ScalatraServlet with SwaggerSupport with JacksonJsonSupport with Logging with KoutaJsonFormats {

  val modelName:String
  val EXAMPLE_DATE_TIME = ISO_OFFSET_DATE_TIME_FORMATTER.format(Instant.EPOCH)

  before() {
    contentType = formats("json")
  }

  private def modelProperty(position:Int, values:List[String]) = ModelProperty(DataType.String, position, required = false, description = Some(values.mkString("/")))

  def overrideModels() = {
    models.update(modelName, models(modelName).copy(properties = overrideEnumModels(models(modelName))))
    models.remove("Alkamiskausi")
    models.remove("Hakutapa")
    models.remove("Hakulomaketyyppi")
    models.remove("Opetusaika")
    models.remove("Julkaisutila")
    models.remove("Koulutustyyppi")
    models.remove("Kieli")
    models.put("Kielistetty", new Model("Kielistetty", "Kielistetty", properties = Kieli.values.zipWithIndex.map { case (k, i) =>
      (k.toString, modelProperty(i+1, List(s"nimi ${k.toString}")))
    }))
    models.foreach(m => models.update(m._1, m._2.copy(properties = overrideDatatypes(m._2))))
  }

  private def overrideEnumModels(model:Model) = model.properties.map {
      case ("alkamiskausi", mp) => ("alkamiskausi", modelProperty(mp.position, Alkamiskausi.values().map(_.toString)))
      case ("hakutapa", mp) => ("hakutapa", modelProperty(mp.position, Hakutapa.values().map(_.toString)))
      case ("hakulomaketyyppi", mp) => ("hakulomaketyyppi", modelProperty(mp.position, Hakulomaketyyppi.values().map(_.toString)))
      case ("opetusaika", mp) => ("opetusaika", modelProperty(mp.position, Opetusaika.values().map(_.toString)))
      case ("tila", mp) => ("tila", modelProperty(mp.position, Julkaisutila.values().map(_.toString)))
      case ("koulutustyyppi", mp) => ("koulutustyyppi", modelProperty(mp.position, Koulutustyyppi.values().map(_.toString)))
      case ("kieli", mp) => ("kieli", modelProperty(mp.position, Kieli.values.map(_.toString)))
      case ("kielivalinta", mp) => ("kielivalinta", ModelProperty(DataType.GenList(DataType.String), mp.position, description = Some(s"[${Kieli.values.mkString(",")}]")))
      case p => p
    }

  private def overrideDatatypes(model:Model) = model.properties.map {
    case (name, mp) if mp.`type`.name.equals("Instant") => (name, modelProperty(mp.position, List(EXAMPLE_DATE_TIME)))
    case (name, mp) if mp.`type`.name.equals("Map") => (name, ModelProperty(new ValueDataType("Kielistetty", None, Some("fi.oph.kouta.domain.Kielistetty")), mp.position))
    //case (name, mp) if mp.`type`.name.equals("Kieli") => (name, ModelProperty(DataType.GenList(DataType.String), mp.position, description = Some(s"[${Kieli.values.mkString(",")}]")))
    case p => p
    }

  protected def createLastModifiedHeader(instant: Instant): String = {
    //- system_time range in database is of form ["2017-02-28 13:40:02.442277+02",)
    //- RFC-1123 date-time format used in headers has no millis
    //- if Last-Modified/If-Unmodified-Since header is set to 2017-02-28 13:40:02, it will never be inside system_time range
    //-> this is why we wan't to set it to 2017-02-28 13:40:03 instead
    renderHttpDate(instant.truncatedTo(java.time.temporal.ChronoUnit.SECONDS).plusSeconds(1))
  }

  protected def renderHttpDate(instant: Instant): String = {
    DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.ofInstant(instant, ZoneId.of("GMT")))
  }

  val sample = renderHttpDate(Instant.EPOCH)
  protected def parseIfUnmodifiedSince: Option[Instant] = request.headers.get("If-Unmodified-Since") match {
    case Some(s) =>
      Try(Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(s))) match {
        case x if x.isSuccess => Some(x.get)
        case Failure(e) => throw new IllegalArgumentException(s"Ei voitu jäsentää otsaketta If-Unmodified-Since muodossa $sample.", e)
      }
    case None => None
  }

  protected def getIfUnmodifiedSince: Instant = parseIfUnmodifiedSince match {
    case Some(s) => s
    case None => throw new IllegalArgumentException("Otsake If-Unmodified-Since on pakollinen.")
  }

  def errorMsgFromRequest() = {
    def msgBody = request.body.length match {
      case x if x > 500000 => request.body.substring(0, 500000)
      case _ => request.body
    }
    s"""Error ${request.getMethod} ${request.getContextPath} => ${msgBody}"""
  }

  def badRequest(t:Throwable) = {
    logger.warn(errorMsgFromRequest(), t)
    BadRequest("error" -> t.getMessage)
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
        case e: IllegalStateException =>  badRequest(t)
        case e: IllegalArgumentException => badRequest(t)
        case e: MappingException =>  badRequest(t)
        case e: ParseException =>  badRequest(t)
        case e: ConcurrentModificationException =>
          Conflict("error" -> e.getMessage)
        case e: NoSuchElementException =>
          NotFound("error" -> e.getMessage)
        case e =>
          logger.error(errorMsgFromRequest(), e)
          InternalServerError("error" -> "500 Internal Server Error")
      }
    }
  }
}

class HealthcheckServlet(implicit val swagger:Swagger) extends KoutaServlet {

  override val modelName: String = "Healthcheck"
  override val applicationDescription = "Healthcheck API"

  import org.json4s.JsonDSL._

  get("/", operation(apiOperation[String]("Healthcheck") summary "Healthcheck" tags "Admin")) {
    Ok("message" -> "ok")
  }

}

