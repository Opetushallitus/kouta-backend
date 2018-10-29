package fi.oph.kouta.integration

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.domain._
import fi.oph.kouta.integration.fixture._

class HakukohdeSpec extends KoutaIntegrationSpec with HakukohdeFixture
  with KoulutusFixture with ToteutusFixture with HakuFixture with ValintaperusteFixture {

  var (koulutusOid, toteutusOid, hakuOid) = ("", "", "")
  var valintaperusteId:UUID = null

  override def beforeAll() = {
    super.beforeAll()
    koulutusOid = put(koulutus)
    toteutusOid = put(toteutus(koulutusOid))
    hakuOid = put(haku)
    valintaperusteId = put(valintaperuste)
  }

  lazy val uusiHakukohde = hakukohde(koulutusOid, hakuOid, valintaperusteId)
  lazy val tallennettuHakukohde: String => Hakukohde = hakukohde(_, koulutusOid, hakuOid, valintaperusteId)

  it should "return 404 if hakukohde not found" in {
    get(s"$HakukohdePath/123") {
      status should equal (404)
      body should include ("Unknown hakukohde oid")
    }
  }

  it should "store hakukohde" in {
    val oid = put(uusiHakukohde)
    get(oid, tallennettuHakukohde(oid))
  }

  it should "update hakukohde" in {
    val oid = put(uusiHakukohde)
    val lastModified = get(oid, tallennettuHakukohde(oid))
    val updatedHakukohde = tallennettuHakukohde(oid).copy(tila = Arkistoitu)
    update(updatedHakukohde, lastModified)
    get(oid, updatedHakukohde)
  }

  it should "not update koulutus" in {
    val oid = put(uusiHakukohde)
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified = get(oid, thisHakukohde)
    update(thisHakukohde, lastModified, false)
    get(oid, thisHakukohde)
  }

  it should "fail update if 'If-Unmodified-Since' header is missing" in {
    val oid = put(uusiHakukohde)
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified = get(oid, thisHakukohde)
    post(HakukohdePath, bytes(thisHakukohde)) {
      status should equal (400)
      body should include ("If-Unmodified-Since")
    }
  }

  it should "fail update if modified in between get and update" in {
    val oid = put(uusiHakukohde)
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified = get(oid, thisHakukohde)
    Thread.sleep(1500)
    update(tallennettuHakukohde(oid).copy(tila = Arkistoitu), lastModified)
    post(HakukohdePath, bytes(thisHakukohde), List(("If-Unmodified-Since", lastModified))) {
      status should equal (409)
    }
  }

  it should "update hakukohteen tekstit ja hakuajat" in {
    val oid = put(uusiHakukohde)
    val lastModified = get(oid, tallennettuHakukohde(oid))
    val muokattuHakukohde = tallennettuHakukohde(oid).copy(
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      hakulomaketyyppi = Some(Ataru),
      hakulomake = Some("http://ataru/kivahakulomake"),
      hakuajat = List(Hakuaika(alkaa = Instant.now(), paattyy = Instant.now.plusSeconds(12000))))
    update(muokattuHakukohde, lastModified, true)
    get(oid, muokattuHakukohde)
  }

  it should "delete all hakuajat and read last modified from history" in {
    val oid = put(uusiHakukohde)
    val lastModified = get(oid, tallennettuHakukohde(oid))
    Thread.sleep(1500)
    val muokattuHakukohde = tallennettuHakukohde(oid).copy(hakuajat = List())
    update(muokattuHakukohde, lastModified, true)
    get(oid, muokattuHakukohde) should not equal (lastModified)
  }

  it should "store and update unfinished hakukohde" in {
    val unfinishedHakukohde = new Hakukohde(muokkaaja = "Muikea Muokkaaja", koulutusOid = koulutusOid, hakuOid = hakuOid)
    val oid = put(unfinishedHakukohde)
    val lastModified = get(oid, unfinishedHakukohde.copy(oid = Some(oid)))
    val newKoulutusOid = put(koulutus)
    val newUnfinishedHakukohde = unfinishedHakukohde.copy(oid = Some(oid), koulutusOid = newKoulutusOid)
    update(newUnfinishedHakukohde, lastModified)
    get(oid, newUnfinishedHakukohde)
  }
}
