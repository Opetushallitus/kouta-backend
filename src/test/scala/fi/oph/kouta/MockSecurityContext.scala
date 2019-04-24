package fi.oph.kouta

import fi.oph.kouta.security.{KayttooikeusUserDetails, Role, SecurityContext}
import fi.vm.sade.utils.cas.CasClient.{SessionCookie, Username}
import fi.vm.sade.utils.cas.{CasClient, CasParams}
import scalaz.concurrent.Task

class MockSecurityContext(val casUrl: String, val casServiceIdentifier: String, val requiredRoles: Set[Role], users: Map[String, KayttooikeusUserDetails]) extends SecurityContext {

  val casClient: CasClient = new CasClient("", null) {
    override def validateServiceTicket(service: String)(ticket: String): Task[Username] =
      if (ticket.startsWith(MockSecurityContext.ticketPrefix(service))) {
        val username = ticket.stripPrefix(MockSecurityContext.ticketPrefix(service))
        Task.now(username)
      } else {
        Task.fail(new RuntimeException("unrecognized ticket: " + ticket))
      }

    override def fetchCasSession(params: CasParams, sessionCookieName: String): Task[SessionCookie] =
      Task.now("jsessionidFromMockSecurityContext")
  }
}

object MockSecurityContext {
  def apply(casUrl: String, casServiceIdentifier: String, requiredRoles: Set[Role]): MockSecurityContext = {
    val users = Map("testuser" -> KayttooikeusUserDetails(requiredRoles, "mockoid"))

    new MockSecurityContext(casUrl, casServiceIdentifier, requiredRoles, users)
  }

  def ticketFor(service: String, username: String): SessionCookie = ticketPrefix(service) + username

  private def ticketPrefix(service: String): SessionCookie = "mock-ticket-" + service + "-"
}
