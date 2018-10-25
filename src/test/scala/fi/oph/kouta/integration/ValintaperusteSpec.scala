package fi.oph.kouta.integration

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.domain._
import fi.oph.kouta.integration.fixture.ValintaperusteFixture

class ValintaperusteSpec extends KoutaIntegrationSpec with ValintaperusteFixture {

  it should "return 404 if valintaperuste not found" in {
    get(s"/valintaperuste/${UUID.randomUUID()}") {
      status should equal (404)
      body should include ("Unknown valintaperuste id")
    }
  }

  it should "store valintaperuste" in {
    val id = putValintaperusteOk(valintaperuste)
    getValintaperusteOk(id, valintaperuste(id))
  }

  it should "update valintaperuste" in {
    val id = putValintaperusteOk(valintaperuste)
    val lastModified = getValintaperusteOk(id, valintaperuste(id))
    updateValintaperusteOk(valintaperuste(id, Arkistoitu), lastModified)
    getValintaperusteOk(id, valintaperuste(id, Arkistoitu))
  }

  it should "not update valintaperuste" in {
    val id = putValintaperusteOk(valintaperuste)
    val lastModified = getValintaperusteOk(id, valintaperuste(id))
    updateValintaperusteOk(valintaperuste(id), lastModified, false)
    getValintaperusteOk(id, valintaperuste(id)) should equal (lastModified)
  }

  it should "fail update if 'If-Unmodified-Since' header is missing" in {
    val id = putValintaperusteOk(valintaperuste)
    val lastModified = getValintaperusteOk(id, valintaperuste(id))
    post("/valintaperuste", bytes(valintaperuste(id))) {
      status should equal (400)
      body should include ("If-Unmodified-Since")
    }
  }

  it should "fail update if modified in between get and update" in {
    val id = putValintaperusteOk(valintaperuste)
    val lastModified = getValintaperusteOk(id, valintaperuste(id))
    Thread.sleep(1500)
    updateValintaperusteOk(valintaperuste(id, Arkistoitu), lastModified)
    post("/valintaperuste", bytes(valintaperuste(id)), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "store and update unfinished valintaperuste" in {
    val unfinishedValintaperuste = new Valintaperuste(muokkaaja = "Muikea Muokkaaja", organisaatio = "Muokkaajan organisaatio")
    val id = putValintaperusteOk(unfinishedValintaperuste)
    val lastModified = getValintaperusteOk(id, unfinishedValintaperuste.copy(id = Some(id)))
    val newUnfinishedValintaperuste = unfinishedValintaperuste.copy(id = Some(id), organisaatio = "Muokkaajan toinen organisaatio")
    updateValintaperusteOk(newUnfinishedValintaperuste, lastModified)
    getValintaperusteOk(id, newUnfinishedValintaperuste)
  }

  /*it should "validate julkaistu valintaperuste" in {
    val unfinishedValintaperuste = new Valintaperuste(muokkaaja = "Muikea Muokkaaja", organisaatio = "Muokkaajan organisaatio", tila = Julkaistu)
    put("/valintaperuste", bytes(unfinishedValintaperuste)) {
      status should equal(400)
      body should include ("Pakollisia tietoja puuttuu")
    }
  }*/

}
