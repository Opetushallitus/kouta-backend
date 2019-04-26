package fi.oph.kouta.integration.fixture

import fi.oph.kouta.MockSecurityContext
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.security._
import fi.oph.kouta.servlet.AuthServlet

class KayttooikeusUserDetailsServiceMock(securityContext: SecurityContext) extends KayttooikeusUserDetailsService {
  override def getUserByUsername(username: String): Either[Throwable, KayttooikeusUserDetails] = {
    username match {
      case "testuser" => Right(KayttooikeusUserDetails(securityContext.requiredRoles, "test-user-oid"))
      case _ => Left(new AuthenticationFailedException(s"User not found with username: $username"))
    }
  }
}

trait AuthFixture {
  this: KoutaIntegrationSpec =>

  val authPath = "/auth"
  val loginPath = s"$authPath/login"
  val sessionPath = s"$authPath/session"

  val casUrl = "testCasUrl"

  val securityContext: SecurityContext = MockSecurityContext(casUrl, serviceIdentifier, Set(Role.CrudUser))
  val kayttooikeusUserDetailsService = new KayttooikeusUserDetailsServiceMock(securityContext)

  object MockCasSessionService extends CasSessionService(securityContext, kayttooikeusUserDetailsService)

  addServlet(new AuthServlet(MockCasSessionService), authPath)

  def getSessionFromCookies(cookies: String) = {
    "session=[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}".r
      .findFirstIn(cookies)
      .map(s => s.replace("session=", ""))
  }
}
