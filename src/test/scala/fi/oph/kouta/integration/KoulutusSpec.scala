package fi.oph.kouta.integration

import fi.oph.kouta.domain._
import fi.oph.kouta.integration.fixture.KoulutusFixture
import org.json4s.jackson.Serialization.read

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
      metadata = Some(new KoulutusMetadata(Map(Fi -> "kuvaus", En -> "description"))),
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

  it should "store and update unfinished koulutus" in {
    val unfinishedKoulutus = new Koulutus(johtaaTutkintoon = true, muokkaaja = "Muikea Muokkaaja")
    val oid = putKoulutusOk(unfinishedKoulutus)
    val lastModified = getKoulutusOk(oid, unfinishedKoulutus.copy(oid = Some(oid)))
    val newUnfinishedKoulutus = unfinishedKoulutus.copy(oid = Some(oid), johtaaTutkintoon = false)
    updateKoulutusOk(newUnfinishedKoulutus, lastModified)
    getKoulutusOk(oid, newUnfinishedKoulutus)
  }

  /*it should "validate julkaistu koulutus" in {
    val unfinishedKoulutus = new Koulutus(johtaaTutkintoon = true, muokkaaja = "Muikea Muokkaaja", tila = Julkaistu)
    put("/koulutus", bytes(unfinishedKoulutus)) {
      status should equal(400)
      body should include ("Pakollisia tietoja puuttuu")
    }
  }*/

  it should "search koulutuksia" in {
    import slick.jdbc.PostgresProfile.api._
    db.runBlocking(sqlu"""delete from koulutusten_tarjoajat""")
    db.runBlocking(sqlu"""delete from koulutukset""")
    val oid1 = putKoulutusOk(koulutus.copy(tarjoajat = List("5.5", "6.5")))
    val oid2 = putKoulutusOk(koulutus.copy(tarjoajat = List("6.5"), tila = Tallennettu))
    val oid3 = putKoulutusOk(koulutus.copy(tarjoajat = List("5.5"), tila = Tallennettu))
    listKoulutusOk(List(("tila", "julkaistu"), ("tarjoaja", "6.5")), List(oid1))
    listKoulutusOk(List(("tarjoaja", "5.5")), List(oid1, oid3))
    listKoulutusOk(List(("tila", "tallennettu")), List(oid2, oid3))
    listKoulutusOk(List(), List(oid1, oid2, oid3))
  }
}
