package fi.oph.kouta.integration

import fi.oph.kouta.domain._
import fi.oph.kouta.integration.fixture.KoulutusFixture

class KoulutusSpec extends KoutaIntegrationSpec with KoulutusFixture {

  it should "return 404 if koulutus not found" in {
    get("/koulutus/123") {
      status should equal (404)
      body should include ("Unknown koulutus oid")
    }
  }

  it should "store koulutus" in {
    val oid = putKoulutusOk(koulutus)
    getKoulutusOk(oid, koulutus(oid))
  }

  it should "update koulutus" in {
    val oid = putKoulutusOk(koulutus)
    val lastModified = getKoulutusOk(oid, koulutus(oid))
    updateKoulutusOk(koulutus(oid, Arkistoitu), lastModified)
    getKoulutusOk(oid, koulutus(oid, Arkistoitu))
  }

  it should "not update koulutus" in {
    val oid = putKoulutusOk(koulutus)
    val lastModified = getKoulutusOk(oid, koulutus(oid))
    updateKoulutusOk(koulutus(oid), lastModified, false)
    getKoulutusOk(oid, koulutus(oid))
  }

  it should "fail update if 'If-Unmodified-Since' header is missing" in {
    val oid = putKoulutusOk(koulutus)
    val lastModified = getKoulutusOk(oid, koulutus(oid))
    post("/koulutus", bytes(koulutus(oid))) {
      status should equal (400)
      body should include ("If-Unmodified-Since")
    }
  }

  it should "fail update if modified in between get and update" in {
    val oid = putKoulutusOk(koulutus)
    val lastModified = getKoulutusOk(oid, koulutus(oid))
    Thread.sleep(1500)
    updateKoulutusOk(koulutus(oid, Arkistoitu), lastModified)
    post("/koulutus", bytes(koulutus(oid)), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "update koulutuksen tekstit ja tarjoajat" in {
    val oid = putKoulutusOk(koulutus)
    val lastModified = getKoulutusOk(oid, koulutus(oid))
    val uusiKoulutus = koulutus(oid).copy(
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      metadata = new KoulutusMetadata(Map(Fi -> "kuvaus", En -> "description")),
      tarjoajat = List("2.2", "3.2", "4.2"))
    updateKoulutusOk(uusiKoulutus, lastModified, true)
    getKoulutusOk(oid, uusiKoulutus)
  }

  it should "delete all tarjoajat and read last modified from history" in {
    val oid = putKoulutusOk(koulutus)
    val lastModified = getKoulutusOk(oid, koulutus(oid))
    Thread.sleep(1500)
    val uusiKoulutus = koulutus(oid).copy(tarjoajat = List())
    updateKoulutusOk(uusiKoulutus, lastModified, true)
    getKoulutusOk(oid, uusiKoulutus) should not equal (lastModified)
  }
}
