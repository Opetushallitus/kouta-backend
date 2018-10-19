package fi.oph.kouta.integration

import java.time.Instant

import fi.oph.kouta.domain._
import fi.oph.kouta.integration.fixture.HakuFixture

class HakuSpec extends KoutaIntegrationSpec with HakuFixture {

  it should "return 404 if haku not found" in {
    get("/haku/123") {
      status should equal (404)
      body should include ("Unknown haku oid")
    }
  }

  it should "store haku" in {
    val oid = putHakuOk(haku)
    getHakuOk(oid, haku(oid))
  }

  it should "update haku" in {
    val oid = putHakuOk(haku)
    val lastModified = getHakuOk(oid, haku(oid))
    updateHakuOk(haku(oid, Arkistoitu), lastModified)
    getHakuOk(oid, haku(oid, Arkistoitu))
  }

  it should "not update haku" in {
    val oid = putHakuOk(haku)
    val lastModified = getHakuOk(oid, haku(oid))
    updateHakuOk(haku(oid), lastModified, false)
    getHakuOk(oid, haku(oid)) should equal (lastModified)
  }

  it should "fail update if 'If-Unmodified-Since' header is missing" in {
    val oid = putHakuOk(haku)
    val lastModified = getHakuOk(oid, haku(oid))
    post("/haku", bytes(haku(oid))) {
      status should equal (400)
      body should include ("If-Unmodified-Since")
    }
  }

  it should "fail update if modified in between get and update" in {
    val oid = putHakuOk(haku)
    val lastModified = getHakuOk(oid, haku(oid))
    Thread.sleep(1500)
    updateHakuOk(haku(oid, Arkistoitu), lastModified)
    post("/haku", bytes(haku(oid)), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "update haun tekstit ja hakuajat" in {
    val oid = putHakuOk(haku)
    val lastModified = getHakuOk(oid, haku(oid))
    val uusiHaku = haku(oid).copy(
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      hakulomaketyyppi = Some(Ataru),
      hakulomake = Some("http://ataru/kivahakulomake"),
      metadata = Some(new HakuMetadata(Some(new Yhteystieto("Aku Ankka")))),
      hakuajat = List(HaunHakuaika(alkaa = Instant.now(), paattyy = Instant.now.plusSeconds(12000))))
    updateHakuOk(uusiHaku, lastModified, true)
    getHakuOk(oid, uusiHaku)
  }

  it should "delete all hakuajat and read last modified from history" in {
    val oid = putHakuOk(haku)
    val lastModified = getHakuOk(oid, haku(oid))
    Thread.sleep(1500)
    val uusiHaku = haku(oid).copy(hakuajat = List())
    updateHakuOk(uusiHaku, lastModified, true)
    getHakuOk(oid, uusiHaku) should not equal (lastModified)
  }

  it should "store and update unfinished haku" in {
    val unfinishedHaku = new Haku(muokkaaja = "Muikea Muokkaaja", organisaatio = "Muokkaajan organisaatio")
    val oid = putHakuOk(unfinishedHaku)
    val lastModified = getHakuOk(oid, unfinishedHaku.copy(oid = Some(oid)))
    val newUnfinishedHaku = unfinishedHaku.copy(oid = Some(oid), organisaatio = "Muokkaajan toinen organisaatio")
    updateHakuOk(newUnfinishedHaku, lastModified)
    getHakuOk(oid, newUnfinishedHaku)
  }

  /*it should "validate julkaistu haku" in {
    val unfinishedHaku = new Haku(muokkaaja = "Muikea Muokkaaja", organisaatio = "Muokkaajan organisaatio", tila = Julkaistu)
    put("/haku", bytes(unfinishedHaku)) {
      status should equal(400)
      body should include ("Pakollisia tietoja puuttuu")
    }
  }*/
}
