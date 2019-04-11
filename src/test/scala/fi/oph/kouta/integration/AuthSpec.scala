package fi.oph.kouta.integration

import java.util.UUID

import fi.oph.kouta.integration.fixture.AuthFixture

class AuthSpec extends KoutaIntegrationSpec with AuthFixture {

  "GET /session" should "return 200 if the session is active" in {
    get(sessionPath, headers = defaultHeaders) {
      status should equal(200)
      body should include(testUserOid)
    }
  }

  it should "return 403 if no session or the session is not active" in {
    get(sessionPath, headers = Seq(jsonHeader)) {
      body should include("Forbidden")
      status should equal(403)
    }
  }

  "GET /login" should "redirect to Cas service if neither ticket nor session is found" in {
    get(loginPath, headers = Seq(jsonHeader)) {
      status should equal(302)
      header("Location") should (include(casUrl) and include(serviceIdentifier))
    }
  }

  it should "reuse an existing session" in {
    get(loginPath, headers = defaultHeaders) {
      status should equal(200)
      body should include(testUserOid)
    }
  }

  it should "reject an invalid session id" in {
    get(loginPath, headers = Seq(jsonHeader, sessionHeader("invalid UUID"))) {
      status should equal(400)
      body should include("Invalid UUID string")
    }
  }

  it should "redirect to Cas service if ticket is not specified and session is unknown" in {
    get(loginPath, headers = Seq(jsonHeader, sessionHeader(UUID.randomUUID().toString))) {
      status should equal(302)
      header("Location") should (include(casUrl) and include(serviceIdentifier))
    }
  }

  it should "create a new session from a verifiable CAS ticket" in {
    val setCookie = get(loginPath, params = Seq("ticket" -> s"mock-ticket-$serviceIdentifier-$testUserOid"), headers = Seq(jsonHeader)) {
      status should equal(200)
      body should include(testUserOid)

      header.get("Set-Cookie") should not be empty
      header("Set-Cookie")
    }

    val sessionId = getSessionFromCookies(setCookie)
    sessionId should not be empty

    get(sessionPath, headers = Seq(sessionHeader(sessionId.get))) {
      status should equal(200)
      body should include(testUserOid)
    }
  }

  it should "reject an unknown CAS ticket" in {
    get(loginPath, params = Seq("ticket" -> "invalid"), headers = Seq(jsonHeader)) {
      status should equal(403)
      body should include("Forbidden")
    }
  }

  it should "reuse an existing session even if a ticket is provided" in {
    get(loginPath, params = Seq("ticket" -> "invalid"), headers = defaultHeaders) {
      status should equal(200)
      body should include(testUserOid)
    }
  }

  it should "create a new session from a CAS ticket also when an unknown session id is provided" in {
    val setCookie = get(
      loginPath,
      params = Seq("ticket" -> s"mock-ticket-$serviceIdentifier-$testUserOid"),
      headers = Seq(jsonHeader, sessionHeader(UUID.randomUUID().toString))
    ) {
      status should equal(200)
      body should include(testUserOid)

      header.get("Set-Cookie") should not be empty
      header("Set-Cookie")
    }
    val sessionId = getSessionFromCookies(setCookie)
    sessionId should not be empty

    get(sessionPath, headers = Seq(sessionHeader(sessionId.get))) {
      status should equal(200)
      body should include(testUserOid)
    }

  }

  "POST /login" should "" in {

  }

}
