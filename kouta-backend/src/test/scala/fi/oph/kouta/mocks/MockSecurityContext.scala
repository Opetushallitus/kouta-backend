package fi.oph.kouta.mocks

import fi.oph.kouta.client.CallerId
import fi.oph.kouta.security.{Authority, KayttooikeusUserDetails, SecurityContext}
import fi.vm.sade.javautils.nio.cas.impl.{CasClientImpl, CasSessionFetcher}
import fi.vm.sade.javautils.nio.cas.{CasClient, CasClientBuilder, CasConfig}
import org.apache.commons.lang3.concurrent.BasicThreadFactory
import org.asynchttpclient.DefaultAsyncHttpClientConfig

import java.util.concurrent.{CompletableFuture, ThreadFactory}
import org.asynchttpclient.Dsl.asyncHttpClient

class MockSecurityContext(val casUrl: String, val casServiceIdentifier: String, users: Map[String, KayttooikeusUserDetails]) extends SecurityContext with CallerId {

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
    override def validateServiceTicketWithVirkailijaUsername(service: String, ticket: String): CompletableFuture[String] = {
      println(s"TEST: $service | $ticket | ${MockSecurityContext.ticketPrefix(service)}")
      if (ticket.startsWith(MockSecurityContext.ticketPrefix(service))) {
        val username = ticket.stripPrefix(MockSecurityContext.ticketPrefix(service))
        CompletableFuture.completedFuture(username)
      } else {
        CompletableFuture.failedFuture(new RuntimeException("unrecognized ticket: " + ticket))
      }
    }
  }
}

object MockSecurityContext {

  def apply(casUrl: String, casServiceIdentifier: String, defaultAuthorities: Set[Authority]): MockSecurityContext = {
    val users = Map("testuser" -> KayttooikeusUserDetails(defaultAuthorities, "mockoid"))

    new MockSecurityContext(casUrl, casServiceIdentifier, users)
  }

  def ticketFor(service: String, username: String): String = ticketPrefix(service) + username

  private def ticketPrefix(service: String): String = "mock-ticket-" + service + "-"
}
