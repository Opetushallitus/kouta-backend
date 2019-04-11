package fi.oph.kouta.security

import fi.vm.sade.utils.cas.CasClient.{SessionCookie, Username}
import fi.vm.sade.utils.cas.{CasClient, CasParams}
import scalaz.concurrent.Task

trait SecurityContext {
  def casServiceIdentifier: String
  def requiredRoles: Set[Role]
  def casClient: CasClient
}

class MockSecurityContext(val casServiceIdentifier: String, val requiredRoles: Set[Role], users: Map[String, KayttooikeusUserDetails]) extends SecurityContext {

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
  def apply(casServiceIdentifier: String, requiredRoles: Set[Role]): MockSecurityContext = {
    val users = Map("testuser" -> KayttooikeusUserDetails(requiredRoles, "mockoid"))

    new MockSecurityContext(casServiceIdentifier, requiredRoles, users)
  }

  def ticketFor(service: String, username: String): SessionCookie = ticketPrefix(service) + username

  private def ticketPrefix(service: String): SessionCookie = "mock-ticket-" + service + "-"
}

case class ProductionSecurityContext(casClient: CasClient, casServiceIdentifier: String, requiredRoles: Set[Role]) extends SecurityContext
