package fi.oph.kouta.servlet

import java.util.UUID

import fi.oph.kouta.security.{AuthenticationFailedException, CasSessionService, ServiceTicket}
import fi.vm.sade.utils.cas.CasLogout
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.DefaultFormats
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.swagger.{Swagger, SwaggerSupport}

import scala.util.{Failure, Success}
import scala.util.control.NonFatal

class AuthServlet(casUrl: String, cas: CasSessionService)(implicit val swagger: Swagger)
  extends ScalatraServlet
    with JacksonJsonSupport
    with Logging
    with SwaggerSupport {

  override val applicationDescription = "Kirjautumisen API"

  override protected implicit def jsonFormats: DefaultFormats.type = DefaultFormats

  error {
    case e: IllegalArgumentException =>
      logger.info("Bad request", e)
      contentType = formats("json")
      halt(BadRequest("error" -> s"Bad request: ${e.getMessage}"))
    case e: AuthenticationFailedException =>
      logger.warn("Login failed", e)
      contentType = formats("json")
      halt(Forbidden("error" -> "Forbidden"))
    case NonFatal(e) =>
      logger.error("Login failed unexpectedly", e)
      contentType = formats("json")
      halt(InternalServerError("error" -> "Internal server error"))
  }

  get(
    "/login",
    operation(
      apiOperation[Unit]("Kirjaudu sisään")
        tags "Auth"
        summary "Kirjaudu sisään"
        parameter queryParam[String]("ticket").optional.description("CAS tiketti")
    )
  ) {
    val ticket = params.get("ticket").map(ServiceTicket)

    val existingSession = cookies
      .get("session")
      .orElse(Option(request.getAttribute("session")).map(_.toString))
      .map(UUID.fromString)

    cas.getSession(ticket, existingSession).map { case (id, session) =>
      contentType = formats("json")
      implicit val cookieOptions: CookieOptions = CookieOptions(
        path = "/kouta-backend",
        secure = false,
        httpOnly = true
      )
      cookies += ("session" -> id.toString)
      request.setAttribute("session", id.toString)
      Ok(Map("personOid" -> session.personOid))
    }.recoverWith { case _ if ticket.isEmpty =>
      Success(Found(s"$casUrl/login?service=${cas.serviceIdentifier}"))
    }.get
  }

  get("/session",
    operation(
      apiOperation[Unit]("Tarkista sessio")
        tags "Auth"
        summary "Tarkista sessio"
    )
  ) {
    val existingSession = cookies
      .get("session")
      .orElse(Option(request.getAttribute("session")).map(_.toString))
      .map(UUID.fromString)

    cas.getSession(None, existingSession).map { case (_, session) =>
      Ok(Map("personOid" -> session.personOid))
    }.get
  }

  post("/login",
    operation(apiOperation[Unit]("Kirjaudu ulos")
      tags "Auth"
      summary "Kirjaudu ulos"
      parameter bodyParam[String](name = "logoutRequest"))) {
    params
      .get("logoutRequest")
      .toRight(
        new IllegalArgumentException("Not 'logoutRequest' parameter given")
      )
      .flatMap(request =>
        CasLogout.parseTicketFromLogoutRequest(request).toRight(
          new RuntimeException(
            s"Failed to parse CAS logout request $request"
          )
        )
      )
      .toTry
      .flatMap(ticket => cas.deleteSession(ServiceTicket(ticket)))
      .map(_ => NoContent)
      .get
  }
}
