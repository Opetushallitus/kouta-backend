package fi.oph.kouta.servlet

import java.util.UUID

import fi.oph.kouta.repository.SessionDAO
import fi.oph.kouta.security.{AuthenticationFailedException, Session}
import fi.vm.sade.utils.slf4j.Logging
import org.scalatra._

trait CasAuthenticatedServlet {
  this: ScalatraServlet with Logging =>

  protected def authenticate: Authenticated = {
    val sessionCookie = cookies.get("session")
    val sessionAttribute = Option(request.getAttribute("session")).map(_.toString)

    logger.debug("Session cookie {}", sessionCookie)
    logger.debug("Session attribute {}", sessionAttribute)

    val sessionId = sessionCookie
      .orElse(sessionAttribute)
      .map(UUID.fromString)
      .getOrElse(throw new AuthenticationFailedException("Session cookie or attribute not in request"))

    val session = SessionDAO.get(sessionId).map((sessionId, _))

    logger.debug("Session found {}", session)

    Authenticated.tupled(session.getOrElse(throw new AuthenticationFailedException(s"No session found in DB. Session cookie: ${sessionCookie}. Session attribute: ${sessionAttribute}.")))
  }

  /*
    protected def auditInfo(implicit authenticated: Authenticated): AuditInfo = {
      AuditInfo(
        Authenticated.unapply(authenticated).get, InetAddress.getByName(HttpServletRequestUtils.getRemoteAddress(request)),
          request.headers.get("User-Agent").getOrElse(throw new IllegalArgumentException("Otsake User-Agent on pakollinen."))
      )
    }
  */
}

case class Authenticated(id: UUID, session: Session)
