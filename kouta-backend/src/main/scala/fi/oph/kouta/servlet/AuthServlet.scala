package fi.oph.kouta.servlet

import java.util.{Optional, UUID}
import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.security.{CasSessionService, ServiceTicket}
import fi.oph.kouta.util.MiscUtils.toScalaOption
import fi.vm.sade.javautils.nio.cas.CasLogout
import org.scalatra._

class AuthServlet(casSessionService: CasSessionService) extends KoutaServlet {

  def this() = this(CasSessionService)

  private val casLogout = new CasLogout

  registerPath("/auth/login",
    """    get:
      |      summary: Kirjaudu sisään
      |      operationId: Kirjaudu sisaan
      |      description: Kirjaudu sisään
      |      tags:
      |        - Auth
      |      parameters:
      |        - in: query
      |          name: ticket
      |          schema:
      |            type: string
      |          required: true
      |          description: CAS-tiketti
      |      responses:
      |        '200':
      |          description: Ok
      |        '401':
      |          description: Unauthorized
       |""".stripMargin)
  get("/login") {

    val ticket = params.get("ticket").map(ServiceTicket)

    casSessionService.getSession(ticket, getExistingSession) match {
      case Left(_) if ticket.isEmpty =>
        Found(s"${casSessionService.casUrl}/login?service=${casSessionService.serviceIdentifier}")
      case Left(t) => throw t
      case Right((id, session)) =>
        // Lisätään cookie käsin, koska Scalatran cookiet ei tue SameSite-attribuuttia
        response.setHeader("Set-Cookie", cookieValue(id))

        Ok(Map("personOid" -> session.personOid))
    }
  }

  registerPath("/auth/session",
    """    get:
      |      summary: Tarkista käyttäjän sessio
      |      operationId: Tarkista sessio
      |      description: Tarkista käyttäjän sessio
      |      tags:
      |        - Auth
      |      responses:
      |        '200':
      |          description: Ok
      |        '401':
      |          description: Unauthorized
      |""".stripMargin)
  get("/session") {

    casSessionService.getSession(None, getExistingSession) match {
      case Left(t) => throw t
      case Right((_, session)) => Ok(Map("personOid" -> session.personOid))
    }
  }

  registerPath("/auth/login",
    """    post:
      |      summary: Kirjaudu ulos
      |      operationId: Kirjaudu ulos
      |      description: Kirjaudu ulos
      |      tags:
      |        - Auth
      |      requestBody:
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: object
      |              properties:
      |                logoutRequest:
      |                  type: string
      |              required:
      |                - logoutRequest
      |      responses:
      |        '200':
      |          description: Ok
       |""".stripMargin)
  post("/login") {

    val logoutRequest = params.get("logoutRequest")
      .getOrElse(throw new IllegalArgumentException("Not 'logoutRequest' parameter given"))

    val ticket: Option[String] = toScalaOption(casLogout.parseTicketFromLogoutRequest(logoutRequest))
    if(ticket.isEmpty) throw new RuntimeException(s"Failed to parse CAS logout request $request")

    casSessionService.deleteSession(ServiceTicket(ticket.get))
    NoContent()
  }

  private def getExistingSession: Option[UUID] =
    cookies.get(sessionCookieName).map(UUID.fromString)

  private def cookieValue(id: UUID): String = {
    val cookie = s"$sessionCookieName=${id.toString};Path=/kouta-backend;HttpOnly;SameSite=Strict"
    if (securityConfiguration.useSecureCookies) s"$cookie;Secure" else cookie
  }
}
