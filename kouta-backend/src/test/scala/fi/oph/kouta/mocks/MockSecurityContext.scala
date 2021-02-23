package fi.oph.kouta.mocks

import fi.oph.kouta.client.CallerId
import fi.oph.kouta.security.{Authority, KayttooikeusUserDetails, SecurityContext}
import fi.vm.sade.utils.cas.CasClient.{ServiceTicket, SessionCookie, Username}
import fi.vm.sade.utils.cas.{CasClient, CasParams}
import scalaz.concurrent.Task

class MockSecurityContext(val casUrl: String, val casServiceIdentifier: String, users: Map[String, KayttooikeusUserDetails]) extends SecurityContext with CallerId {

  val casClient: CasClient = new CasClient("", null, callerId) {

    override def validateServiceTicketWithVirkailijaUsername(service: String)(serviceTicket: ServiceTicket): Task[Username] =
      if (serviceTicket.startsWith(MockSecurityContext.ticketPrefix(service))) {
        val username = serviceTicket.stripPrefix(MockSecurityContext.ticketPrefix(service))
        Task.now(username)
      } else {
        Task.fail(new RuntimeException("unrecognized ticket: " + serviceTicket))
      }

    override def fetchCasSession(params: CasParams, sessionCookieName: String): Task[SessionCookie] =
      Task.now("jsessionidFromMockSecurityContext")
  }
}

object MockSecurityContext {

  def apply(casUrl: String, casServiceIdentifier: String, defaultAuthorities: Set[Authority]): MockSecurityContext = {
    val users = Map("testuser" -> KayttooikeusUserDetails(defaultAuthorities, "mockoid"))

    new MockSecurityContext(casUrl, casServiceIdentifier, users)
  }

  def ticketFor(service: String, username: String): SessionCookie = ticketPrefix(service) + username

  private def ticketPrefix(service: String): SessionCookie = "mock-ticket-" + service + "-"
}
