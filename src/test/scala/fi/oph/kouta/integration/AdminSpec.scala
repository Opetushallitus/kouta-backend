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

  it should "have correct database encoding" in {
    import slick.jdbc.PostgresProfile.api._
    db.runBlocking(sql"""show server_encoding""".as[String].head) should be("UTF8")
    db.runBlocking(sql"""show lc_collate""".as[String].head) should be("C")
    db.runBlocking(sql"""show lc_ctype""".as[String].head) should be("C")
  }
}
