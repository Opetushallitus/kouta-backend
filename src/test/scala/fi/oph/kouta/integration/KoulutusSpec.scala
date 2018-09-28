package fi.oph.kouta.integration

import fi.oph.kouta.domain.Julkaisutila._
import fi.oph.kouta.domain.{Koulutus, Kieli}
import fi.oph.kouta.domain.Koulutustyyppi._
import fi.oph.kouta.servlet.KoulutusServlet

import org.json4s.jackson.Serialization.write

class KoulutusSpec extends KoutaIntegrationSpec {

  addServlet(new KoulutusServlet(), "/koulutus")

  val koulutus = Koulutus(
    oid = "1.2.3",
    johtaaTutkintoon = true,
    koulutustyyppi = amm,
    koulutusKoodiUri = "koulutus_123#1",
    tila = julkaistu,
    nimi = Map(Kieli.fi -> "nimi", Kieli.sv -> "nimi sv"),
    kuvaus = Map(),
    tarjoajat = List("1.2", "2.2"),
    muokkaaja = "Mörkö Muokkaaja")

  it should "return 404 if koulutus not found" in {
    get("/koulutus/123") {
      status should equal (404)
      body should include ("Unknown koulutus oid")
    }
  }

  it should "store koulutus" in {
    val json = write(koulutus)
    println(json)
    post("/koulutus", json.getBytes) {
      status should equal (200)
      get(s"/koulutus/${koulutus.oid}") {
        status should equal (200)
        println(body)
        body should equal (json)
      }
    }
  }
}
