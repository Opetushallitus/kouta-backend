package fi.oph.kouta.servlet

import java.net.InetAddress
import java.util.UUID

import fi.oph.kouta.repository.SessionDAO
import fi.oph.kouta.security.{AuthenticationFailedException, Session}
import fi.vm.sade.javautils.http.HttpServletRequestUtils
import fi.vm.sade.utils.slf4j.Logging
import javax.servlet.http.HttpServletRequest
import org.scalatra._

trait CasAuthenticatedServlet {
  this: ScalatraServlet with Logging =>

  protected def authenticate: Authenticated = {
    val sessionCookie = cookies.get("session")
    val sessionAttribute = Option(request.getAttribute("session")).map(_.toString)

    logger.debug("Session cookie {}", sessionCookie)
    logger.debug("Session attribute {}", sessionAttribute)

    val session = sessionCookie
      .orElse(sessionAttribute)
      .map(UUID.fromString)
      .flatMap(id => SessionDAO.get(id).map((id, _)))

    logger.debug("Session found {}", session)

    session match {
      case None =>
        throw new AuthenticationFailedException(s"No session found. Session cookie: ${sessionCookie}. Session attribute: ${sessionAttribute}.")
      case Some((id, s)) =>
        Authenticated(id, s)
    }
  }
}
