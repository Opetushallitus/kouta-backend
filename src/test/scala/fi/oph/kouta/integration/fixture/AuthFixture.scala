package fi.oph.kouta.integration.fixture

import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.security.{CasSessionService, MockSecurityContext, Role, SecurityContext}
import fi.oph.kouta.servlet.AuthServlet

trait AuthFixture {
  this: KoutaIntegrationSpec =>

  val authPath = "/auth"
  val loginPath = s"$authPath/login"
  val sessionPath = s"$authPath/session"

  val casUrl = "testCasUrl"
  val serviceIdentifier = "testService"

  val securityContext: SecurityContext = MockSecurityContext(serviceIdentifier, Set(Role.GenericUser))

  addServlet(
    new AuthServlet(
      casUrl = casUrl,
      cas = new CasSessionService(
        securityContext.casClient,
        securityContext.casServiceIdentifier
        //userDetailsService
      )
    ), authPath)

  def getSessionFromCookies(cookies: String) = {
    "session=[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}".r
      .findFirstIn(cookies)
      .map(s => s.replace("session=", ""))
  }
}
