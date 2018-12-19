package fi.oph.kouta.servlet

import java.text.ParseException
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.{ConcurrentModificationException, NoSuchElementException, UUID}

import fi.oph.kouta.PrettySwaggerSupport
import fi.oph.kouta.service.{KoutaAuthorizationFailedException, KoutaValidationException}
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.MappingException
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.swagger._

import scala.util.{Failure, Try}

trait KoutaServlet extends ScalatraServlet with JacksonJsonSupport
  with Logging with KoutaJsonFormats with PrettySwaggerSupport {

  before() {
    contentType = formats("json")
  }

  protected def renderHttpDate(instant: Instant): String = {
    DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.ofInstant(instant, ZoneId.of("GMT")))
  }

  protected def parseHttpDate(string: String): Instant = {
    Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(string))
  }

  protected def createLastModifiedHeader(instant: Instant): String = {
    //- system_time range in database is of form ["2017-02-28 13:40:02.442277+02",)
    //- RFC-1123 date-time format used in headers has no millis
    //- if Last-Modified/If-Unmodified-Since header is set to 2017-02-28 13:40:02, it will never be inside system_time range
    //-> this is why we wan't to set it to 2017-02-28 13:40:03 instead
    renderHttpDate(instant.truncatedTo(java.time.temporal.ChronoUnit.SECONDS).plusSeconds(1))
  }

  val SampleHttpDate = renderHttpDate(Instant.EPOCH)
  protected def parseIfUnmodifiedSince: Option[Instant] = request.headers.get("If-Unmodified-Since") match {
    case Some(s) =>
      Try(parseHttpDate(s)) match {
        case x if x.isSuccess => Some(x.get)
        case Failure(e) => throw new IllegalArgumentException(s"Ei voitu jäsentää otsaketta If-Unmodified-Since muodossa $SampleHttpDate.", e)
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

  def badRequest(t: Throwable) = {
    logger.warn(errorMsgFromRequest(), t)
    BadRequest("error" -> t.getMessage)
  }

  error {
    case t: Throwable => {
      t match {
        /*case e: AuthenticationFailedException =>
          logger.warn("authentication failed", e)
          Unauthorized("error" -> "Unauthorized")*/
        case e: KoutaAuthorizationFailedException =>
          logger.warn("authorization failed", e)
          Forbidden("error" -> s"Forbidden ${e.oid}")
        case e: KoutaValidationException => BadRequest(e.errorMessages.distinct)
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

