package fi.oph.kouta.integration.fixture

import fi.oph.kouta.TestOids.TestUserOid
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.KayttooikeusClient
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.mocks.{MockAuditLogger, MockSecurityContext}
import fi.oph.kouta.security._
import fi.oph.kouta.servlet.AuthServlet

class KayttooikeusClientMock(securityContext: SecurityContext, defaultAuthorities: Set[Authority]) extends KayttooikeusClient {
  override def getUserByUsername(username: String): KayttooikeusUserDetails = {
    username match {
      case "testuser" => KayttooikeusUserDetails(defaultAuthorities, TestUserOid.s)
      case _ => throw new AuthenticationFailedException(s"User not found with username: $username")
    }
  }
}

trait AuthFixture {
  this: KoutaIntegrationSpec =>

  val authPath = "/auth"
  val loginPath = s"$authPath/login"
  val sessionPath = s"$authPath/session"

  val casUrl = "testCasUrl"

  val securityContext: SecurityContext = MockSecurityContext(casUrl, serviceIdentifier, defaultAuthorities)
  val kayttooikeusClient = new KayttooikeusClientMock(securityContext, defaultAuthorities)

  object MockCasSessionService extends CasSessionService(securityContext, kayttooikeusClient, new AuditLog(MockAuditLogger))

  addServlet(new AuthServlet(MockCasSessionService), authPath)

  def getSessionFromCookies(cookies: String) = {
    "session=[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}".r
      .findFirstIn(cookies)
      .map(s => s.replace("session=", ""))
  }
}
