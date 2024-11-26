package fi.oph.kouta.servlet

import java.util.UUID

import fi.oph.kouta.config.{KoutaConfigurationFactory, SecurityConfiguration}
import fi.oph.kouta.repository.SessionDAO
import fi.oph.kouta.security.AuthenticationFailedException
import fi.oph.kouta.logging.Logging
import org.scalatra._

trait CasAuthenticatedServlet {
  this: ScalatraServlet with Logging =>

  lazy val securityConfiguration: SecurityConfiguration = KoutaConfigurationFactory.configuration.securityConfiguration
  lazy val sessionCookieName: String = securityConfiguration.sessionCookieName

  protected def authenticate(): Authenticated = {
    val sessionCookie = cookies.get(sessionCookieName)

    logger.debug("Session cookie {}", sessionCookie)

    val session = sessionCookie.map(UUID.fromString).flatMap(id => SessionDAO.get(id).map((id, _)))

    logger.debug("Session found {}", session)

    session match {
      case None =>
        throw new AuthenticationFailedException(s"No session found. Session cookie: ${sessionCookie}.")
      case Some((id, s)) =>
        Authenticated(id, s)
    }
  }
}
