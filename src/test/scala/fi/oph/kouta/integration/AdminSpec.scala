package fi.oph.kouta.integration

import fi.oph.kouta.servlet.HealthcheckServlet

class AdminSpec extends KoutaIntegrationSpec {
  addServlet(new HealthcheckServlet(), "/healthcheck")

  "Healthcheck" should "return 200" in {
    get("/healthcheck") {
      status should equal (200)
      body should equal ("{\"message\":\"ok\"}")
    }
  }
}
