package fi.oph.kouta.integration

import fi.oph.kouta.domain.{Arkistoitu, En, Fi, Sv}
import fi.oph.kouta.integration.fixture.{KoulutusFixture, ToteutusFixture}

class ToteutusSpec extends KoutaIntegrationSpec with KoulutusFixture with ToteutusFixture {

  var koulutusOid = ""

  override def beforeAll() = {
    super.beforeAll()
    koulutusOid = putKoulutusOk(koulutus)
  }

  it should "return 404 if toteutus not found" in {
    get("/toteutus/123") {
      status should equal (404)
      body should include ("Unknown toteutus oid")
    }
  }

  it should "store toteutus" in {
    val oid = putToteutusOk(toteutus(koulutusOid))
    getToteutusOk(oid, toteutus(oid, koulutusOid))
  }

  it should "update toteutus" in {
    val oid = putToteutusOk(toteutus(koulutusOid))
    val lastModified = getToteutusOk(oid, toteutus(oid, koulutusOid))
    updateToteutusOk(toteutus(oid, koulutusOid, Arkistoitu), lastModified)
    getToteutusOk(oid, toteutus(oid, koulutusOid, Arkistoitu))
  }

  it should "not update koulutus" in {
    val oid = putToteutusOk(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = getToteutusOk(oid, thisToteutus)
    updateToteutusOk(thisToteutus, lastModified, false)
    getToteutusOk(oid, thisToteutus)
  }

  it should "fail update if 'If-Unmodified-Since' header is missing" in {
    val oid = putToteutusOk(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = getToteutusOk(oid, thisToteutus)
    post("/toteutus", bytes(thisToteutus)) {
      status should equal (400)
      body should include ("If-Unmodified-Since")
    }
  }

  it should "fail update if modified in between get and update" in {
    val oid = putToteutusOk(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = getToteutusOk(oid, thisToteutus)
    Thread.sleep(1500)
    updateToteutusOk(toteutus(oid, koulutusOid, Arkistoitu), lastModified)
    post("/toteutus", bytes(thisToteutus), List(("If-Unmodified-Since", lastModified))) {
      status should equal (409)
    }
  }

  it should "update toteutuksen nimi, metadata ja tarjoajat" in {
    val oid = putToteutusOk(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = getToteutusOk(oid, thisToteutus)
    val uusiToteutus = thisToteutus.copy(
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      metadata = thisToteutus.metadata.copy(kuvaus = Map(Fi -> "kuvaus", En -> "description")),
      tarjoajat = List("2.2", "3.2", "4.2"))
    updateToteutusOk(uusiToteutus, lastModified, true)
    getToteutusOk(oid, uusiToteutus)
  }

  it should "delete all tarjoajat and read last modified from history" in {
    val oid = putToteutusOk(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = getToteutusOk(oid, thisToteutus)
    Thread.sleep(1500)
    val uusiToteutus = thisToteutus.copy(tarjoajat = List())
    updateToteutusOk(uusiToteutus, lastModified, true)
    getToteutusOk(oid, uusiToteutus) should not equal (lastModified)
  }
}
