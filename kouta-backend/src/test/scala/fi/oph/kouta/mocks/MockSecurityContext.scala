package fi.oph.kouta.mocks

import fi.oph.kouta.TestOids.TestUserOid
import fi.oph.kouta.client.CallerId
import fi.oph.kouta.security.{AuthenticationFailedException, Authority, SecurityContext}
import fi.vm.sade.javautils.nio.cas.impl.{CasClientImpl, CasSessionFetcher}
import fi.vm.sade.javautils.nio.cas.{CasClient, CasConfig, UserDetails}
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.asynchttpclient.Dsl.asyncHttpClient

import java.util.concurrent.{CompletableFuture, ThreadFactory}
import scala.collection.JavaConverters._

class MockSecurityContext(val casUrl: String, val casServiceIdentifier: String, defaultAuthorities: Set[Authority]) extends SecurityContext with CallerId {

  val casConfig: CasConfig = new CasConfig.CasConfigBuilder(
    "",
    "",
    "",
    "",
    callerId,
    callerId,
    "").build
  val factory: ThreadFactory = new BasicThreadFactory.Builder().namingPattern("test-async-cas-client-thread-%d").daemon(true).priority(Thread.NORM_PRIORITY).build
  val httpClient = asyncHttpClient(new DefaultAsyncHttpClientConfig.Builder().setThreadFactory(factory).build)
  val casSessionFetcher: CasSessionFetcher = new CasSessionFetcher(casConfig, httpClient, casConfig.getSessionTicketValidMs, casConfig.getTicketGrantingTicketValidMs) {
    override def fetchSessionToken(): CompletableFuture[String] =
      CompletableFuture.completedFuture("jsessionidFromMockSecurityContext")
  }

  override val casClient: CasClient = new CasClientImpl(
    casConfig,
    httpClient,
    casSessionFetcher
  ) {
    override def validateServiceTicketWithVirkailijaUserDetails(service: String, ticket: String): CompletableFuture[UserDetails] = {
      println(s"TEST: $service | $ticket | ${MockSecurityContext.ticketPrefix(service)}")
      if (ticket.startsWith(MockSecurityContext.ticketPrefix(service))) {
        val username = ticket.stripPrefix(MockSecurityContext.ticketPrefix(service))
        if (username == "testuser") {
          val henkiloOid = TestUserOid.s
          val roles = defaultAuthorities.map(a => s"ROLE_${a.role}").asJava
          val userDetails = new UserDetails(username, henkiloOid, null, null, roles)
          CompletableFuture.completedFuture(userDetails)
        } else {
          CompletableFuture.failedFuture(new AuthenticationFailedException(s"User not found with username: $username"))
        }
      } else {
        CompletableFuture.failedFuture(new RuntimeException("unrecognized ticket: " + ticket))
      }
    }
  }
}

object MockSecurityContext {

  def apply(casUrl: String, casServiceIdentifier: String, defaultAuthorities: Set[Authority]): MockSecurityContext = {
    new MockSecurityContext(casUrl, casServiceIdentifier, defaultAuthorities)
  }

  def ticketFor(service: String, username: String): String = ticketPrefix(service) + username

  private def ticketPrefix(service: String): String = "mock-ticket-" + service + "-"
}
