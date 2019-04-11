package fi.oph.kouta.servlet

import java.net.InetAddress
import java.util.UUID

import fi.oph.kouta.repository.SessionDAO
import fi.oph.kouta.security.{AuthenticationFailedException, AuthorizationFailedException, Role, Session}
import fi.vm.sade.utils.slf4j.Logging
import org.scalatra._

trait CasAuthenticatedServlet {
  this: ScalatraServlet with Logging =>

  protected def authenticate: Authenticated = {
    val sessionCookie = cookies.get("session")
    val sessionAttribute = Option(request.getAttribute("session")).map(_.toString)

    logger.trace("Session cookie {}", sessionCookie)
    logger.trace("Session attribute {}", sessionAttribute)

    val session = sessionCookie.orElse(sessionAttribute).map(UUID.fromString).flatMap(id => SessionDAO.get(id).map((id, _)))

    logger.trace("Session found {}", session)

    Authenticated.tupled(session.getOrElse(throw new AuthenticationFailedException))
  }

  /*
    def authorize(roles:Role*)(implicit authenticated: Authenticated) = {
      if (!authenticated.session.hasAnyRole(roles.toSet)) {
        throw new AuthorizationFailedException
      }
    }
  */

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
