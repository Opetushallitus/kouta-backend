package fi.oph.kouta.integration

import java.util.UUID

import fi.oph.kouta.integration.fixture.AuthFixture
import fi.oph.kouta.mocks.MockAuditLogger
import org.scalatest.BeforeAndAfterEach

class AuthSpec extends KoutaIntegrationSpec with AuthFixture with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    MockAuditLogger.clean()
  }

  "Get session" should "return 200 if the session is active" in {
    get(sessionPath, headers = defaultHeaders) {
      body should include(testUser.oid.s)
      status should equal(200)
    }
    MockAuditLogger.logs shouldBe empty
  }

  it should "return 401 if no session or the session is not active" in {
    get(sessionPath, headers = Seq(jsonHeader)) {
      withClue(body) {
        status should equal(401)
      }
    }
  }

  "Do login" should "redirect to Cas service if neither ticket nor session is found" in {
    get(loginPath, headers = Seq(jsonHeader)) {
      withClue(body) {
        status should equal(302)
        header("Location") should (include(casUrl) and include(serviceIdentifier))
      }
    }
  }

  it should "reuse an existing session" in {
    get(loginPath, headers = defaultHeaders) {
      body should include(testUser.oid.s)
      status should equal(200)
    }
    MockAuditLogger.logs shouldBe empty
  }

  it should "reject an invalid session id" in {
    get(loginPath, headers = Seq(jsonHeader, sessionHeader("invalid UUID"))) {
      body should include("Invalid UUID string")
      status should equal(400)
    }
  }

  it should "redirect to Cas service if ticket is not specified and session is unknown" in {
    get(loginPath, headers = Seq(jsonHeader, sessionHeader(UUID.randomUUID()))) {
      withClue(body) {
        status should equal(302)
        header("Location") should (include(casUrl) and include(serviceIdentifier))
      }
    }
  }

  it should "create a new session from a verifiable CAS ticket" in {
    val cookieHeader = get(loginPath, params = Seq("ticket" -> testUser.ticket), headers = Seq(jsonHeader)) {
      body should include(testUser.oid.s)
      status should equal(200)

      header.get("Set-Cookie") should not be empty
      header("Set-Cookie")
    }
    MockAuditLogger.find(testUser.ticket, testUser.oid.s, "kirjautuminen") shouldBe defined

    val sessionId = getSessionFromCookies(cookieHeader)
    sessionId should not be empty

    get(sessionPath, headers = Seq(sessionHeader(sessionId.get))) {
      status should equal(200)
      body should include(testUser.oid.s)
    }
  }

  it should "reject an unknown CAS ticket" in {
    get(loginPath, params = Seq("ticket" -> "invalid"), headers = Seq(jsonHeader)) {
      status should equal(401)
      body should include("Unauthorized")
    }
  }

  it should "reuse an existing session even if a ticket is provided" in {
    get(loginPath, params = Seq("ticket" -> "invalid"), headers = defaultHeaders) {
      status should equal(200)
      body should include(testUser.oid.s)
    }
    MockAuditLogger.logs shouldBe empty
  }

  it should "create a new session from a CAS ticket also when an unknown session id is provided" in {
    val cookieHeader = get(
      loginPath,
      params = Seq("ticket" -> testUser.ticket),
      headers = Seq(jsonHeader, sessionHeader(UUID.randomUUID()))
    ) {
      status should equal(200)
      body should include(testUser.oid.s)

      header.get("Set-Cookie") should not be empty
      header("Set-Cookie")
    }
    MockAuditLogger.find(testUser.ticket, testUser.oid.s, "kirjautuminen") shouldBe defined
    val sessionId = getSessionFromCookies(cookieHeader)
    sessionId should not be empty

    get(sessionPath, headers = Seq(sessionHeader(sessionId.get))) {
      status should equal(200)
      body should include(testUser.oid.s)
    }
  }

  "Logout" should "" in {
    //TODO: tests
  }

}
