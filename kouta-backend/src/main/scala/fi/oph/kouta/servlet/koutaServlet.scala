package fi.oph.kouta.servlet

import java.text.ParseException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.ConcurrentModificationException
import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.security.AuthenticationFailedException
import fi.oph.kouta.service.{ExternalModifyAuthorizationFailedException, KoulutustyyppiAuthorizationFailedException, KoutaValidationException, OrganizationAuthorizationFailedException, RoleAuthorizationFailedException}
import fi.oph.kouta.util.KoutaJsonFormats
import fi.oph.kouta.util.TimeUtils.{parseHttpDate, renderHttpDate}
import fi.oph.kouta.logging.Logging
import org.json4s.{JValue, MappingException}
import jakarta.servlet.http.HttpServletRequest
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport

import scala.util.control.NonFatal
import scala.util.{Failure, Try}

trait KoutaServlet extends ScalatraServlet with JacksonJsonSupport
  with Logging with KoutaJsonFormats with CasAuthenticatedServlet {

  before() {
    contentType = formats("json")
  }

  protected def createLastModifiedHeader(instant: Instant): String = {
    //- Last modification time is read from last_modified or system_time and may have microsecond precision,
    //  e.g. "2017-02-28 13:40:02.442277+02"
    //- x-Last-Modified/x-If-Unmodified-Since are our own headers (not the standard HTTP Last-Modified), so we're
    //  free to use ISO-8601 with millisecond precision instead of the second-granularity RFC-1123 date format
    //- truncating to millis can still round down past the actual modification instant (microsecond remainder),
    //  so we add 1 ms to guarantee the header is never before the last_modified it was derived from
    instant.truncatedTo(ChronoUnit.MILLIS).plusMillis(1).toString
  }

  protected def parseIfUnmodifiedSince: Option[Instant] = request.headers.get(KoutaServlet.IfUnmodifiedSinceHeader) match {
    case Some(s) =>
      // Hyväksytään sekä uusi ISO-8601-muoto että vanha RFC-1123, koska osa asiakkaista (esim. kouta-ui:n
      // joukkotilanmuutokset) muodostaa otsakkeen arvon itse RFC-1123-muodossa sen sijaan että palauttaisi takaisin
      // GETin x-Last-Modified-arvon
      Try(parseHttpDate(s)) match {
        case x if x.isSuccess => Some(x.get)
        case Failure(e) => throw new IllegalArgumentException(s"Ei voitu jäsentää otsaketta ${KoutaServlet.IfUnmodifiedSinceHeader} muodossa ${KoutaServlet.SampleHeaderDate}.", e)
      }
    case None => None
  }

  protected def getIfUnmodifiedSince: Instant = parseIfUnmodifiedSince match {
    case Some(s) => s
    case None => throw new IllegalArgumentException(s"Otsake ${KoutaServlet.IfUnmodifiedSinceHeader} on pakollinen.")
  }

  def errorMsgFromRequest(): String = {
    def msgBody = Try {
      request.body.length match {
        case x if x > 500000 => request.body.substring(0, 500000)
        case _ => request.body
      }
    }.getOrElse("")

    val queryStr = if (request.queryString.isEmpty) "" else "?" + request.queryString

    s"Error ${request.getMethod} $contextPath${request.getServletPath}$requestPath${queryStr} => $msgBody"
  }

  def badRequest(t: Throwable): ActionResult = {
    logger.warn(errorMsgFromRequest(), t)
    BadRequest("error" -> t.getMessage)
  }

  error {
    case e: AuthenticationFailedException =>
      logger.warn(s"authentication failed: ${e.getMessage}", e.cause)
      Unauthorized("error" -> "Unauthorized")
    case e: RoleAuthorizationFailedException =>
      logger.warn(s"authorization failed: ${e.getMessage}", e.getCause)
      Forbidden("error" -> "Forbidden")
    case e: OrganizationAuthorizationFailedException =>
      logger.warn(s"authorization failed: ${e.getMessage}", e.getCause)
      Forbidden("error" -> s"Forbidden ${e.getMessage}")
    case e: KoulutustyyppiAuthorizationFailedException =>
      logger.warn(s"authorization failed: ${e.getMessage}", e.getCause)
      Forbidden("error" -> s"Forbidden ${e.getMessage}")
    case e: ExternalModifyAuthorizationFailedException =>
      logger.warn(s"authorization failed: ${e.getMessage}", e.getCause)
      Forbidden("error" -> s"Forbidden ${e.getMessage}")
    case e: KoutaValidationException =>
      logger.warn(
        s"""${errorMsgFromRequest()}
           |  ${e.getClass.getCanonicalName}: ${e.getMessage}
           |""".stripMargin)
      BadRequest(e.errorMessages)
    case e: IllegalStateException => badRequest(e)
    case e: IllegalArgumentException => badRequest(e)
    case e: MappingException => badRequest(e)
    case e: ParseException => badRequest(e)
    case e: ConcurrentModificationException =>
      Conflict("error" -> e.getMessage)
    case e: EntityNotFoundException =>
      NotFound("error" -> e.getMessage)
    case e: PayloadTooLargeException =>
      logger.warn(s"PayloadTooLargeException: ${e.getMessage}")
      RequestEntityTooLarge("error" -> e.getMessage)
    case e: MediaNotSupportedException =>
      logger.warn(s"MediaNotSupportedException: ${e.getMessage}")
      UnsupportedMediaType("error" -> e.getMessage)
    case NonFatal(e) =>
      logger.error(errorMsgFromRequest(), e)
      InternalServerError("error" -> "500 Internal Server Error")
  }

  /**
   * Json4s seems to have a bug when optional class values with optional values are literal nulls in JSON.
   *
   * Circumvent this by removing all nulls from the incoming JSON.
   */
  override def parsedBody(implicit request: HttpServletRequest): JValue = super.parsedBody(request).noNulls
}

object KoutaServlet {
  val IfUnmodifiedSinceHeader: String = "x-If-Unmodified-Since"
  val LastModifiedHeader: String = "x-Last-Modified"
  // RFC-1123 sample, used only by IndexerServlet's /modifiedSince/:since, which still uses TimeUtils.(render|parse)HttpDate
  val SampleHttpDate: String = renderHttpDate(Instant.EPOCH)
  // ISO-8601-with-millis sample for the x-Last-Modified/x-If-Unmodified-Since headers
  val SampleHeaderDate: String = Instant.EPOCH.toString
}

class HealthcheckServlet extends KoutaServlet {

  registerPath("/healthcheck/",
    """    get:
      |      summary: Healthcheck-rajapinta
      |      description: Healthcheck-rajapinta
      |      tags:
      |        - Admin
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin)
  get("/") {
    Ok("message" -> "ok")
  }

}

case class PayloadTooLargeException(message: String) extends RuntimeException(message)

case class MediaNotSupportedException(message: String) extends RuntimeException(message)
