package fi.oph.kouta.integration

import java.util.UUID

import fi.oph.kouta.{EventuallyMessages, KonfoIndexingQueues, TestData}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.repository.SQLHelpers
import fi.oph.kouta.validation.Validations

class HakukohdeSpec extends KoutaIntegrationSpec with EverythingFixture with Validations with SQLHelpers with KonfoIndexingQueues with EventuallyMessages {

  var (koulutusOid, toteutusOid, hakuOid) = ("", "", "")
  var valintaperusteId:UUID = null

  override def beforeAll() = {
    super.beforeAll()
    koulutusOid = put(koulutus)
    toteutusOid = put(toteutus(koulutusOid))
    hakuOid = put(haku)
    valintaperusteId = put(valintaperuste)
  }

  lazy val uusiHakukohde = hakukohde(toteutusOid, hakuOid, valintaperusteId)
  lazy val tallennettuHakukohde: String => Hakukohde = {oid:String => getIds(hakukohde(oid, toteutusOid, hakuOid, valintaperusteId))}

  def getIds(hakukohde:Hakukohde) = {
    import slick.jdbc.PostgresProfile.api._
    hakukohde.copy(
    liitteet = hakukohde.liitteet.map(l => l.copy(id = db.runBlocking(
      sql"""select id from hakukohteiden_liitteet where hakukohde_oid = ${hakukohde.oid} and tyyppi = ${l.tyyppi}""".as[String]).headOption.map(UUID.fromString))),
    valintakokeet = hakukohde.valintakokeet.map(l => l.copy(id = db.runBlocking(
      sql"""select id from hakukohteiden_valintakokeet where hakukohde_oid = ${hakukohde.oid} and tyyppi = ${l.tyyppi}""".as[String]).headOption.map(UUID.fromString))),
  )}

  def addInvalidHakuaika(hakukohde:Hakukohde) = hakukohde.copy(
    hakuajat = List(Ajanjakso(TestData.inFuture(9000), TestData.inFuture(3000))))

  "GET /hakukohde/:oid" should "return 404 if hakukohde not found" in {
    get(s"$HakukohdePath/123", headers = Seq(sessionHeader)) {
      status should equal (404)
      body should include ("Unknown hakukohde oid")
    }
  }

  it should "return 401 if a valid session is not found" in {
    get(s"$HakukohdePath/123") {
      status should equal (401)
      body should include ("Unauthorized")
    }
  }

  "PUT /hakukohde" should "store hakukohde" in {
    val oid = put(uusiHakukohde)
    get(oid, tallennettuHakukohde(oid))
  }

  it should "return 401 if a valid session is not found" in {
    put(uri = s"$HakukohdePath", bytes(uusiHakukohde), Seq(jsonHeader)) {
      status should equal (401)
      body should include ("Unauthorized")
    }
  }

  it should "validate new hakukohde" in {
    put(HakukohdePath, bytes(addInvalidHakuaika(uusiHakukohde)), List(jsonHeader, sessionHeader)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validateErrorBody(InvalidHakuaika))
    }
  }

  it should "send indexing message after creating hakukohde" in {
    val oid = put(uusiHakukohde)
    eventuallyIndexingMessages { _ should contain (s"""{"hakukohteet":["$oid"]}""") }
  }

  "POST /hakukohde" should "update hakukohde" in {
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
    update(thisHakukohde, lastModified, expectUpdate = false)
    get(oid, thisHakukohde)
  }

  it should "fail update if 'If-Unmodified-Since' header is missing" in {
    val oid = put(uusiHakukohde)
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified = get(oid, thisHakukohde)
    post(HakukohdePath, bytes(thisHakukohde), Seq(sessionHeader)) {
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
    post(HakukohdePath, bytes(thisHakukohde), List(("If-Unmodified-Since", lastModified), sessionHeader)) {
      status should equal (409)
    }
  }

  it should "return 401 if a valid session is not found" in {
    val oid = put(uusiHakukohde)
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified = get(oid, tallennettuHakukohde(oid))
    post(HakukohdePath, bytes(thisHakukohde), List(("If-Unmodified-Since", lastModified))) {
      status should equal (401)
    }
  }

  it should "update hakukohteen tekstit ja hakuajat" in {
    val oid = put(uusiHakukohde)
    val lastModified = get(oid, tallennettuHakukohde(oid))
    val muokattuHakukohde = tallennettuHakukohde(oid).copy(
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      hakulomaketyyppi = Some(Ataru),
      hakulomake = Map(Fi -> "http://ataru/kivahakulomake", Sv -> "http://ataru/kivahakulomake/sv", En -> "http://ataru/kivahakulomake/en"),
      hakuajat = List(Ajanjakso(alkaa = TestData.now(), paattyy = TestData.inFuture(12000))))
    update(muokattuHakukohde, lastModified, expectUpdate = true)
    get(oid, muokattuHakukohde)
  }

  it should "delete all hakuajat and read last modified from history" in {
    val oid = put(uusiHakukohde)
    val lastModified = get(oid, tallennettuHakukohde(oid))
    Thread.sleep(1500)
    val muokattuHakukohde = tallennettuHakukohde(oid).copy(hakuajat = List())
    update(muokattuHakukohde, lastModified, expectUpdate = true)
    get(oid, muokattuHakukohde) should not equal lastModified
  }

  it should "store and update unfinished hakukohde" in {
    val unfinishedHakukohde = Hakukohde(muokkaaja = UserOid("7.7.7.7"), toteutusOid = ToteutusOid(toteutusOid),
      hakuOid = HakuOid(hakuOid), organisaatioOid = OrganisaatioOid("1.2"), modified = None)
    val oid = put(unfinishedHakukohde)
    val lastModified = get(oid, unfinishedHakukohde.copy(oid = Some(HakukohdeOid(oid))))
    val newToteutusOid = put(toteutus(koulutusOid))
    val newUnfinishedHakukohde = unfinishedHakukohde.copy(oid = Some(HakukohdeOid(oid)), toteutusOid = ToteutusOid(newToteutusOid))
    update(newUnfinishedHakukohde, lastModified)
    get(oid, newUnfinishedHakukohde)
  }

  it should "validate updated hakukohde" in {
    val oid = put(uusiHakukohde)
    val lastModified = get(oid, tallennettuHakukohde(oid))
    post(HakukohdePath, bytes(addInvalidHakuaika(tallennettuHakukohde(oid))), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validateErrorBody(InvalidHakuaika))
    }
  }

  it should "update hakukohteen liitteet ja valintakokeet" in {
    val oid = put(uusiHakukohde)
    val tallennettu = tallennettuHakukohde(oid)
    val lastModified = get(oid, tallennettu)
    val muokattuHakukohde = tallennettu.copy(
      valintakokeet = List(TestData.Valintakoe1.copy(tyyppi = Some("tyyyyppi"))),
      liitteet = tallennettu.liitteet.map(_.copy(toimitusaika = Some(TestData.now()))))
    update(muokattuHakukohde, lastModified, expectUpdate = true)
    get(oid, getIds(muokattuHakukohde))
  }

  it should "delete all hakuajat, liitteet ja valintakokeet nicely" in {
    val oid = put(uusiHakukohde)
    val tallennettu = tallennettuHakukohde(oid)
    val lastModified = get(oid, tallennettu)
    val muokattuHakukohde = tallennettu.copy(liitteet = List(), hakuajat = List(), valintakokeet = List())
    update(muokattuHakukohde, lastModified, expectUpdate = true)
    get(oid, muokattuHakukohde)
  }

  it should "send indexing message after updating hakukohde" in {
    val oid = put(uusiHakukohde)
    eventuallyIndexingMessages { _ should contain (s"""{"hakukohteet":["$oid"]}""") }

    val lastModified = get(oid, tallennettuHakukohde(oid))
    val muokattuHakukohde = tallennettuHakukohde(oid).copy(
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      hakulomaketyyppi = Some(Ataru),
      hakulomake = Map(Fi -> "http://ataru/kivahakulomake"),
      hakuajat = List(Ajanjakso(alkaa = TestData.now(), paattyy = TestData.inFuture(12000))))
    update(muokattuHakukohde, lastModified, expectUpdate = true)

    eventuallyIndexingMessages { _ should contain (s"""{"hakukohteet":["$oid"]}""") }
  }
}
