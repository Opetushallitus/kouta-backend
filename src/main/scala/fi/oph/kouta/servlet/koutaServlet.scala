package fi.oph.kouta.servlet

import java.text.ParseException
import java.time.Instant
import java.util.{ConcurrentModificationException, NoSuchElementException}

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.security.AuthenticationFailedException
import fi.oph.kouta.service.{KoutaValidationException, OrganizationAuthorizationFailedException, RoleAuthorizationFailedException}
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.MappingException
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport

import scala.util.control.NonFatal
import scala.util.{Failure, Try}
import fi.oph.kouta.util.TimeUtils.{parseHttpDate, renderHttpDate}
import org.scalatra.servlet.SizeConstraintExceededException

trait KoutaServlet extends ScalatraServlet with JacksonJsonSupport
  with Logging with KoutaJsonFormats with CasAuthenticatedServlet {

  before() {
    contentType = formats("json")
  }

  protected def createLastModifiedHeader(instant: Instant): String = {
    //- system_time range in database is of form ["2017-02-28 13:40:02.442277+02",)
    //- RFC-1123 date-time format used in headers has no millis
    //- if x-Last-Modified/x-If-Unmodified-Since header is set to 2017-02-28 13:40:02, it will never be inside system_time range
    //-> this is why we wan't to set it to 2017-02-28 13:40:03 instead
    renderHttpDate(instant.truncatedTo(java.time.temporal.ChronoUnit.SECONDS).plusSeconds(1))
  }

  val SampleHttpDate = renderHttpDate(Instant.EPOCH)

  protected def parseIfUnmodifiedSince: Option[Instant] = request.headers.get(KoutaServlet.IfUnmodifiedSinceHeader) match {
    case Some(s) =>
      Try(parseHttpDate(s)) match {
        case x if x.isSuccess => Some(x.get)
        case Failure(e) => throw new IllegalArgumentException(s"Ei voitu jäsentää otsaketta ${KoutaServlet.IfUnmodifiedSinceHeader} muodossa $SampleHttpDate.", e)
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

    s"Error ${request.getMethod} $contextPath${request.getServletPath}$requestPath${request.queryString} => $msgBody"
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
    case e: KoutaValidationException => BadRequest(e.errorMessages.distinct)
    case e: IllegalStateException => badRequest(e)
    case e: IllegalArgumentException => badRequest(e)
    case e: MappingException => badRequest(e)
    case e: ParseException => badRequest(e)
    case e: ConcurrentModificationException =>
      Conflict("error" -> e.getMessage)
    case e: NoSuchElementException =>
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
}

object KoutaServlet {
  val IfUnmodifiedSinceHeader = "x-If-Unmodified-Since"
  val LastModifiedHeader = "x-Last-Modified"
}

class HealthcheckServlet extends KoutaServlet {

  registerPath("/healthcheck/",
    s"""    get:
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

