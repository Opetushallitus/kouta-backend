package fi.oph.kouta.integration

import fi.oph.kouta.domain.Julkaisutila._
import fi.oph.kouta.domain.Koulutus
import fi.oph.kouta.domain.Koulutustyyppi._
import fi.oph.kouta.servlet.KoulutusServlet

import org.json4s.jackson.Serialization.write

class KoulutusSpec extends KoutaIntegrationSpec {

  addServlet(new KoulutusServlet(), "/koulutus")

  it should "return 404 if koulutus not found" in {
    get("/koulutus/123") {
      status should equal (404)
      body should include ("Unknown koulutus oid")
    }
  }

  it should "store koulutus" in {

    val koulutus = Koulutus(
      oid = "1.2.3",
      johtaaTutkintoon = true,
      koulutustyyppi = amm,
      koulutusKoodiUri = "koulutus_123#1",
      tila = julkaistu,
      tarjoajat = List("1.2", "2.2"),
      muokkaaja = "Mörkö Muokkaaja")

    val json = write(koulutus)
    post("/koulutus", json.getBytes) {
      status should equal (200)
      get(s"/koulutus/${koulutus.oid}") {
        status should equal (200)
        body should equal (json)
      }
    }
  }
}
