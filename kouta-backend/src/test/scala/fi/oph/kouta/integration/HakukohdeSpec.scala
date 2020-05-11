package fi.oph.kouta.integration

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.TestData
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.validation.Validations._

class HakukohdeSpec extends KoutaIntegrationSpec with AccessControlSpec with EverythingFixture {

  override val roleEntities = Seq(Role.Hakukohde)

  var (koulutusOid, toteutusOid, hakuOid) = ("", "", "")
  var valintaperusteId: UUID = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    koulutusOid = put(koulutus)
    toteutusOid = put(toteutus(koulutusOid).copy(tarjoajat = List(AmmOid)))
    hakuOid = put(haku)
    val sorakuvausId = put(sorakuvaus)
    valintaperusteId = put(valintaperuste(sorakuvausId))
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  lazy val uusiHakukohde = hakukohde(toteutusOid, hakuOid, valintaperusteId)
  lazy val tallennettuHakukohde: String => Hakukohde = { oid: String =>
    getIds(hakukohde(oid, toteutusOid, hakuOid, valintaperusteId))
  }

  "Get hakukohde by oid" should "return 404 if hakukohde not found" in {
    get(s"$HakukohdePath/123", headers = Seq(defaultSessionHeader)) {
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

  it should "allow a user of the hakukohde organization to read the hakukohde" in {
    val oid = put(uusiHakukohde)
    get(oid, crudSessions(hakukohde.organisaatioOid), tallennettuHakukohde(oid))
  }

  it should "allow a user of toteutuksen tarjoaja organization to read the hakukohde" in {
    val oid = put(uusiHakukohde)
    get(oid, crudSessions(AmmOid), tallennettuHakukohde(oid))
  }

  it should "deny a user without access to the hakukohde organization" in {
    val oid = put(uusiHakukohde)
    get(s"$HakukohdePath/$oid", crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to read the hakukohde" in {
    val oid = put(uusiHakukohde)
    get(oid, crudSessions(ParentOid), tallennettuHakukohde(oid))
  }

  it should "deny a user with only access to a descendant organization" in {
    val oid = put(uusiHakukohde)
    get(s"$HakukohdePath/$oid", crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    val oid = put(uusiHakukohde)
    get(s"$HakukohdePath/$oid", otherRoleSession, 403)
  }

  it should "allow indexer access" in {
    val oid = put(uusiHakukohde)
    get(oid, indexerSession, tallennettuHakukohde(oid))
  }

  "Create hakukohde" should "store hakukohde" in {
    val oid = put(uusiHakukohde)
    get(oid, tallennettuHakukohde(oid))
  }

  it should "read muokkaaja from the session" in {
    val oid = put(uusiHakukohde.copy(muokkaaja = UserOid("random")))
    get(oid, tallennettuHakukohde(oid).copy(muokkaaja = testUser.oid))
  }

  it should "write create hakukohde to audit log" in {
    MockAuditLogger.clean()
    val oid = put(uusiHakukohde.withModified(LocalDateTime.parse("1000-01-01T12:00:00")))
    get(oid, tallennettuHakukohde(oid))
    MockAuditLogger.find(oid, "hakukohde_create") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "return 401 if a valid session is not found" in {
    put(uri = s"$HakukohdePath", bytes(uusiHakukohde), Seq(jsonHeader)) {
      status should equal (401)
      body should include ("Unauthorized")
    }
  }

  it should "allow a user of the hakukohde organization to create the hakukohde" in {
    put(uusiHakukohde, crudSessions(hakukohde.organisaatioOid))
  }

  it should "deny a user without access to the hakukohde organization" in {
    put(HakukohdePath, uusiHakukohde, crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to create the hakukohde" in {
    put(uusiHakukohde, crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    put(HakukohdePath, uusiHakukohde, crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    put(HakukohdePath, uusiHakukohde, readSessions(ChildOid), 403)
  }

  it should "deny indexer access" in {
    put(HakukohdePath, uusiHakukohde, indexerSession, 403)
  }

  it should "validate new hakukohde" in {
    val invalidHakuajat = TestData.getInvalidHakuajat
    put(HakukohdePath, bytes(uusiHakukohde.copy(hakuajat = invalidHakuajat)), List(jsonHeader, defaultSessionHeader)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(invalidAjanjaksoMsg(invalidHakuajat.head), "hakuajat[0]"))
    }
  }

  it should "validate dates only when adding a new julkaistu hakukohde" in {
    val thisHakukohde = uusiHakukohde.copy(alkamisvuosi = Some("2017"))

    put(thisHakukohde.copy(tila = Tallennettu))

    put(HakukohdePath, bytes(thisHakukohde.copy(tila = Julkaistu)), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(pastDateMsg("2017"), "alkamisvuosi"))
    }
  }

  "Update hakukohde" should "update hakukohde" in {
    val oid = put(uusiHakukohde)
    val lastModified = get(oid, tallennettuHakukohde(oid))
    val updatedHakukohde = tallennettuHakukohde(oid).copy(tila = Arkistoitu)
    update(updatedHakukohde, lastModified)
    get(oid, updatedHakukohde)
  }

  it should "read muokkaaja from the session" in {
    val oid = put(uusiHakukohde, crudSessions(ChildOid))
    val userOid = userOidForTestSessionId(crudSessions(ChildOid))
    val lastModified = get(oid, tallennettuHakukohde(oid).copy(muokkaaja = userOid))
    val updatedHakukohde = tallennettuHakukohde(oid).copy(tila = Arkistoitu, muokkaaja = userOid)
    update(updatedHakukohde, lastModified)
    get(oid, updatedHakukohde.copy(muokkaaja = testUser.oid))
  }

  it should "write hakukohde update to audit log" in {
    val oid = put(uusiHakukohde)
    val lastModified = get(oid, tallennettuHakukohde(oid))
    val updatedHakukohde = tallennettuHakukohde(oid).copy(tila = Arkistoitu, modified = Some(LocalDateTime.parse("1000-01-01T12:00:00")))
    MockAuditLogger.clean()
    update(updatedHakukohde, lastModified)
    MockAuditLogger.findFieldChange("tila", "julkaistu", "arkistoitu", oid, "hakukohde_update") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "not update hakukohde" in {
    val oid = put(uusiHakukohde)
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified = get(oid, thisHakukohde)
    MockAuditLogger.clean()
    update(thisHakukohde, lastModified, expectUpdate = false)
    MockAuditLogger.logs shouldBe empty
    get(oid, thisHakukohde)
  }

  it should "fail update if 'x-If-Unmodified-Since' header is missing" in {
    val oid = put(uusiHakukohde)
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified = get(oid, thisHakukohde)
    post(HakukohdePath, bytes(thisHakukohde), Seq(defaultSessionHeader)) {
      status should equal (400)
      body should include (KoutaServlet.IfUnmodifiedSinceHeader)
    }
  }

  it should "fail update if modified in between get and update" in {
    val oid = put(uusiHakukohde)
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified = get(oid, thisHakukohde)
    Thread.sleep(1500)
    update(tallennettuHakukohde(oid).copy(tila = Arkistoitu), lastModified)
    post(HakukohdePath, bytes(thisHakukohde), List((KoutaServlet.IfUnmodifiedSinceHeader, lastModified), defaultSessionHeader)) {
      status should equal (409)
    }
  }

  it should "return 401 if a valid session is not found" in {
    val oid = put(uusiHakukohde)
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified = get(oid, tallennettuHakukohde(oid))
    post(HakukohdePath, bytes(thisHakukohde), List((KoutaServlet.IfUnmodifiedSinceHeader, lastModified))) {
      status should equal (401)
    }
  }

  it should "allow a user of the hakukohde organization to update the hakukohde" in {
    val oid = put(uusiHakukohde)
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified = get(oid, thisHakukohde)
    update(thisHakukohde, lastModified, false, crudSessions(hakukohde.organisaatioOid))
  }

  it should "allow a user of toteutuksen tarjoaja organization to update the hakukohde" in {
    val oid = put(uusiHakukohde)
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified = get(oid, thisHakukohde)
    update(thisHakukohde, lastModified, false, crudSessions(AmmOid))
  }

  it should "deny a user without access to the hakukohde organization" in {
    val oid = put(uusiHakukohde)
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified = get(oid, thisHakukohde)
    update(thisHakukohde, lastModified, 403, crudSessions(LonelyOid))
  }

  it should "allow a user of an ancestor organization to create the hakukohde" in {
    val oid = put(uusiHakukohde)
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified = get(oid, thisHakukohde)
    update(thisHakukohde, lastModified, false, crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    val oid = put(uusiHakukohde)
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified = get(oid, thisHakukohde)
    update(thisHakukohde, lastModified, 403, crudSessions(GrandChildOid))
  }

  it should "deny a user with the wrong role" in {
    val oid = put(uusiHakukohde)
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified = get(oid, thisHakukohde)
    update(thisHakukohde, lastModified, 403, readSessions(hakukohde.organisaatioOid))
  }

  it should "deny indexer access" in {
    val oid = put(uusiHakukohde)
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified = get(oid, thisHakukohde)
    update(thisHakukohde, lastModified, 403, indexerSession)
  }

  it should "update hakukohteen tekstit ja hakuajat" in {
    val oid = put(uusiHakukohde)
    val lastModified = get(oid, tallennettuHakukohde(oid))
    val muokattuHakukohde = tallennettuHakukohde(oid).copy(
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      hakulomaketyyppi = Some(Ataru),
      hakulomakeKuvaus = Map(Fi -> "http://ataru/kivahakulomake", Sv -> "http://ataru/kivahakulomake/sv", En -> "http://ataru/kivahakulomake/en"),
      hakuajat = List(Ajanjakso(alkaa = TestData.now(), paattyy = Some(TestData.inFuture(12000)))))
    update(muokattuHakukohde, lastModified, expectUpdate = true)
    get(oid, muokattuHakukohde)
  }

  it should "delete all hakuajat and read last modified from history" in {
    val oid = put(uusiHakukohde)
    val lastModified = get(oid, tallennettuHakukohde(oid))
    Thread.sleep(1500)
    val muokattuHakukohde = tallennettuHakukohde(oid).copy(hakuajat = List(), tila = Tallennettu)
    update(muokattuHakukohde, lastModified, expectUpdate = true)
    get(oid, muokattuHakukohde) should not equal lastModified
  }

  it should "store and update unfinished hakukohde" in {
    val unfinishedHakukohde = Hakukohde(muokkaaja = TestUserOid, toteutusOid = ToteutusOid(toteutusOid),
      hakuOid = HakuOid(hakuOid), organisaatioOid = ChildOid, modified = None, kielivalinta = Seq(Fi), nimi = Map(Fi -> "hakukohde"))
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
    val invalidHakuajat = TestData.getInvalidHakuajat
    post(HakukohdePath, bytes(tallennettuHakukohde(oid).copy(hakuajat = invalidHakuajat)), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(invalidAjanjaksoMsg(invalidHakuajat.head), "hakuajat[0]"))
    }
  }

  it should "validate dates only when moving from other states to julkaistu" in {
    val thisHakukohde = uusiHakukohde.copy(alkamisvuosi = Some("2017"), tila = Tallennettu, liitteet = List(), valintakokeet = List())

    val oid = put(thisHakukohde)
    val thisHakukohdeWithOid = thisHakukohde.copy(oid = Some(HakukohdeOid(oid)))
    val lastModified = get(oid, thisHakukohdeWithOid)

    post(HakukohdePath, bytes(thisHakukohdeWithOid.copy(tila = Julkaistu)), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(pastDateMsg("2017"), "alkamisvuosi"))
    }

    update(thisHakukohdeWithOid.copy(tila = Arkistoitu), lastModified)
  }

  it should "update hakukohteen liitteet ja valintakokeet" in {
    val oid = put(uusiHakukohde)
    val tallennettu = tallennettuHakukohde(oid)
    val lastModified = get(oid, tallennettu)
    val muokattuHakukohde = tallennettu.copy(
      valintakokeet = List(TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_42#2"))),
      liitteet = tallennettu.liitteet.map(_.copy(toimitusaika = Some(TestData.inFuture(9000)))))
    update(muokattuHakukohde, lastModified, expectUpdate = true)
    get(oid, getIds(muokattuHakukohde))
  }

  it should "delete all hakuajat, liitteet ja valintakokeet nicely" in {
    val oid = put(uusiHakukohde)
    val tallennettu = tallennettuHakukohde(oid)
    val lastModified = get(oid, tallennettu)
    val muokattuHakukohde = tallennettu.copy(liitteet = List(), hakuajat = List(), valintakokeet = List(), tila = Tallennettu)
    update(muokattuHakukohde, lastModified, expectUpdate = true)
    get(oid, muokattuHakukohde)
  }

}
