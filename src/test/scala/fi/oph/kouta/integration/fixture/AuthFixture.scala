package fi.oph.kouta.integration.fixture

import fi.oph.kouta.MockSecurityContext
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.security.{CasSessionService, Role, SecurityContext}
import fi.oph.kouta.servlet.AuthServlet

trait AuthFixture {
  this: KoutaIntegrationSpec =>

  val authPath = "/auth"
  val loginPath = s"$authPath/login"
  val sessionPath = s"$authPath/session"

  val casUrl = "testCasUrl"
  val serviceIdentifier = "testService"

  val securityContext: SecurityContext = MockSecurityContext(casUrl, serviceIdentifier, Set(Role.GenericUser))

  object MockCasSessionService extends CasSessionService(securityContext)

  addServlet(new AuthServlet(MockCasSessionService), authPath)

  def getSessionFromCookies(cookies: String) = {
    "session=[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}".r
      .findFirstIn(cookies)
      .map(s => s.replace("session=", ""))
  }
}
