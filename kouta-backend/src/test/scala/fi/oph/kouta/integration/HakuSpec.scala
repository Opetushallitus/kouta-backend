package fi.oph.kouta.integration

import java.time.{Instant, LocalDateTime}
import fi.oph.kouta.TestData
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.fixture.HakuFixture
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.validation.Validations._

class HakuSpec extends KoutaIntegrationSpec with HakuFixture {

  override val roleEntities: Seq[RoleEntity] = Seq(Role.Haku)

  val ophHaku: Haku = haku.copy(organisaatioOid = OphOid)
  val yhteisHaku: Haku = haku.copy(hakutapaKoodiUri = Some("hakutapa_01#1"))

  "Get haku by oid" should "return 404 if haku not found" in {
    get("/haku/123", headers = Seq(defaultSessionHeader)) {
      status should equal (404)
      body should include ("Unknown haku oid")
    }
  }

  it should "return 401 without a valid session" in {
    get("/haku/123") {
      status should equal (401)
      body should include ("Unauthorized")
    }
  }

  it should "allow a user of the haku organization to read the haku" in {
    val oid = put(haku)
    get(oid, crudSessions(haku.organisaatioOid), haku(oid))
  }

  it should "deny a user without access to the haku organization" in {
    val oid = put(haku)
    get(s"$HakuPath/$oid", crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to read the haku" in {
    val oid = put(haku)
    get(oid, crudSessions(ParentOid), haku(oid))
  }

  it should "allow a user with only access to a descendant organization" in {
    val oid = put(haku)
    get(oid, crudSessions(GrandChildOid), haku(oid))
  }

  it should "deny a user with the wrong role" in {
    val oid = put(haku)
    get(s"$HakuPath/$oid", otherRoleSession, 403)
  }

  it should "not allow a non-OPH-pääkäyttäjä user to create yhteishaku" in {
    put(HakuPath, yhteishakuWithoutAlkamiskausi, crudSessions(haku.organisaatioOid), 403)
  }

  it should "allow the user of proper koulutustyyppi to read julkinen haku created by oph" in {
    val oid = put(ophHaku)
    get(oid, readSessions(AmmOid), ophHaku.copy(oid = Some(HakuOid(oid))))
  }

  ignore should "deny the user of wrong koulutustyyppi to read julkinen haku created by oph" in {
    val oid = put(ophHaku)
    get(s"$HakuPath/$oid", readSessions(YoOid), 403)
  }

  it should "allow indexer access" in {
    val oid = put(haku)
    get(oid, indexerSession, haku(oid))
  }

  it should "return error when trying to get deleted haku" in {
    val oid = put(ophHaku.copy(tila = Poistettu))
    get(s"$HakuPath/$oid", ophSession, 404)
  }

  it should "return ok when getting deleted haku with myosPoistetut = true" in {
    val oid = put(ophHaku.copy(tila = Poistettu))
    get(s"$HakuPath/$oid?myosPoistetut=true", ophSession, 200)
  }

  "Create haku" should "store haku" in {
    val oid = put(haku)
    get(oid, haku(oid))
  }

  it should "read muokkaaja from the session" in {
    val oid = put(haku.copy(muokkaaja = UserOid("random")))
    get(oid, haku(oid).copy(muokkaaja = testUser.oid))
  }

  it should "write create haku to audit log" in {
    MockAuditLogger.clean()
    val oid = put(haku.withModified(LocalDateTime.parse("1000-01-01T12:00:00")))
    MockAuditLogger.find(oid, "haku_create") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "set haun ohjausparametrit" in {
    val oid = HakuOid(put(haku))
    val ohjausparametrit = ohjausparametritClient.mockedValues(oid)
    ohjausparametrit.hakuOid should equal(oid)
    ohjausparametrit.paikanVastaanottoPaattyy should equal(Some(Instant.ofEpochMilli(46800000L)))
    ohjausparametrit.hakijakohtainenPaikanVastaanottoaika should equal(Some(14))
    ohjausparametrit.sijoittelu should equal(Some(false))
    ohjausparametrit.useitaHakemuksia should equal(Some(true))
    ohjausparametrit.jarjestetytHakutoiveet should equal(Some(false))
    ohjausparametrit.hakutoiveidenMaaraRajoitettu should equal(Some(false))
  }

  it should "return 401 without a valid session" in {
    put(HakuPath, bytes(haku), Seq(jsonHeader)) {
      status should equal(401)
      body should include ("Unauthorized")
    }
  }

  it should "allow a user of the haku organization to create the haku" in {
    put(haku, crudSessions(haku.organisaatioOid))
  }

  it should "deny a user without access to the haku organization" in {
    put(HakuPath, haku, crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to create the haku" in {
    put(haku, crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    put(HakuPath, haku, crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    put(HakuPath, haku, readSessions(ChildOid), 403)
  }

  it should "deny indexer access" in {
    put(HakuPath, haku, indexerSession, 403)
  }

  it should "validate new haku" in {
    val invalidHakuajat = TestData.getInvalidHakuajat
    put(HakuPath, bytes(haku.copy(hakuajat = invalidHakuajat)), Seq(jsonHeader, defaultSessionHeader)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(invalidAjanjaksoMsg(invalidHakuajat.head), "hakuajat[0]"))
    }
  }

  "Update haku" should "update haku" in {
    val oid = put(haku)
    val thisHaku = haku(oid)
    val lastModified = get(oid, thisHaku)
    update(thisHaku.copy(tila = Arkistoitu), lastModified)
    get(oid, thisHaku.copy(tila = Arkistoitu))
  }

  it should "read muokkaaja from the session" in {
    val oid = put(haku, crudSessions(ChildOid))
    val userOid = userOidForTestSessionId(crudSessions(ChildOid))
    val lastModified = get(oid, haku(oid).copy(muokkaaja = userOid))
    update(haku(oid, Arkistoitu).copy(muokkaaja = userOid), lastModified)
    get(oid, haku(oid, Arkistoitu).copy(muokkaaja = testUser.oid))
  }

  it should "write haku update to audit log" in {
    val oid = put(haku)
    val thisHaku = haku(oid)
    val lastModified = get(oid, thisHaku)
    MockAuditLogger.clean()
    update(thisHaku.copy(tila = Arkistoitu).withModified(LocalDateTime.parse("1000-01-01T12:00:00")), lastModified)
    MockAuditLogger.findFieldChange("tila", "julkaistu", "arkistoitu", oid, "haku_update") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "not update haku" in {
    val oid = put(haku)
    val thisHaku = haku(oid)
    val lastModified = get(oid, thisHaku)
    MockAuditLogger.clean()
    update(thisHaku, lastModified, expectUpdate = false)
    MockAuditLogger.logs shouldBe empty
    get(oid, thisHaku) should equal (lastModified)
  }

  it should "fail update if 'x-If-Unmodified-Since' header is missing" in {
    val oid = put(haku)
    val thisHaku = haku(oid)
    get(oid, thisHaku)
    post(HakuPath, bytes(thisHaku), Seq(defaultSessionHeader)) {
      status should equal (400)
      body should include (KoutaServlet.IfUnmodifiedSinceHeader)
    }
  }

  it should "return 401 without a valid session" in {
    val oid = put(haku)
    val thisHaku = haku(oid)
    get(oid, thisHaku)
    post(HakuPath, bytes(thisHaku), Map.empty) {
      status should equal (401)
      body should include ("Unauthorized")
    }
  }

  it should "allow a user of the haku organization to update the haku" in {
    val oid = put(haku)
    val thisHaku = haku(oid)
    val lastModified = get(oid, thisHaku)
    update(thisHaku, lastModified, expectUpdate = false, crudSessions(haku.organisaatioOid))
  }

  it should "deny a user without access to the haku organization" in {
    val oid = put(haku)
    val thisHaku = haku(oid)
    val lastModified = get(oid, thisHaku)
    update(thisHaku, lastModified, 403, crudSessions(LonelyOid))
  }

  it should "allow a user of an ancestor organization to create the haku" in {
    val oid = put(haku)
    val thisHaku = haku(oid)
    val lastModified = get(oid, thisHaku)
    update(thisHaku, lastModified, expectUpdate = false, crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    val oid = put(haku)
    val thisHaku = haku(oid)
    val lastModified = get(oid, thisHaku)
    update(thisHaku, lastModified, 403, crudSessions(GrandChildOid))
  }

  it should "deny a user with the wrong role" in {
    val oid = put(haku)
    val thisHaku = haku(oid)
    val lastModified = get(oid, thisHaku)
    update(thisHaku, lastModified, 403, readSessions(haku.organisaatioOid))
  }

  it should "allow organisaatioOid change if user had rights to new organisaatio" in {
    val oid = put(haku.copy(organisaatioOid = HkiYoOid))
    val thisHaku = haku(oid).copy(organisaatioOid = HkiYoOid)
    val lastModified = get(oid, thisHaku)
    update(thisHaku.copy(organisaatioOid = YoOid), lastModified, expectUpdate = true, yliopistotSession)
  }

  it should "fail organisaatioOid change if user doesn't have rights to new organisaatio" in {
    val oid = put(haku.copy(organisaatioOid = HkiYoOid))
    val thisHaku = haku(oid).copy(organisaatioOid = HkiYoOid)
    val lastModified = get(oid, thisHaku)
    update(thisHaku.copy(organisaatioOid = LukioOid), lastModified, 403, yliopistotSession)
  }

  it should "deny indexer access" in {
    val oid = put(haku)
    val thisHaku = haku(oid)
    val lastModified = get(oid, thisHaku)
    update(thisHaku, lastModified, 403, indexerSession)
  }

  it should "fail update if modified in between get and update" in {
    val oid = put(haku)
    val thisHaku = haku(oid)
    val lastModified = get(oid, thisHaku)
    Thread.sleep(1500)
    update(thisHaku.copy(tila = Arkistoitu), lastModified)
    post(HakuPath, bytes(thisHaku), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "delete all hakuajat and read last modified from history" in {
    val oid = put(haku)
    val thisHaku = haku(oid)
    val lastModified = get(oid, thisHaku)
    Thread.sleep(1500)
    val uusiHaku = thisHaku.copy(hakuajat = List())
    update(uusiHaku, lastModified, expectUpdate = true)
    get(oid, uusiHaku) should not equal lastModified
  }

  it should "add right amount of rows to history tables" in {
    resetTableHistory("haut")
    resetTableHistory("hakujen_hakuajat")

    val oid          = put(haku)
    val thisHaku     = haku(oid)
    val lastModified = get(oid, thisHaku)
    val uusiHaku = thisHaku.copy(hakuajat = List())
    update(uusiHaku, lastModified, expectUpdate = true)

    assert(getTableHistorySize("haut") == 1)
    assert(getTableHistorySize("hakujen_hakuajat") == 1)
  }

  it should "store and update unfinished haku" in {
    val unfinishedHaku = Haku(muokkaaja = TestUserOid, organisaatioOid = LonelyOid, modified = None, kielivalinta = Seq(Fi), nimi = Map(Fi -> "haku"), kohdejoukkoKoodiUri = Some("haunkohdejoukko_17#1"), hakutapaKoodiUri = Some("hakutapa_03#1"))
    val oid = put(unfinishedHaku)
    val lastModified = get(oid, unfinishedHaku.copy(oid = Some(HakuOid(oid)), _enrichedData = Some(HakuEnrichedData(muokkaajanNimi = Some("Testi Muokkaaja")))))
    val newUnfinishedHaku = unfinishedHaku.copy(oid = Some(HakuOid(oid)), organisaatioOid = AmmOid, _enrichedData = Some(HakuEnrichedData(muokkaajanNimi = Some("Testi Muokkaaja"))))
    update(newUnfinishedHaku, lastModified)
    get(oid, newUnfinishedHaku)
  }

  it should "validate updated haku" in {
    val oid = put(haku)
    val lastModified = get(oid, haku(oid))
    val invalidHakuajat = TestData.getInvalidHakuajat
    val thisHaku = haku(oid).copy(hakuajat = invalidHakuajat)
    post(HakuPath, bytes(thisHaku), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(invalidAjanjaksoMsg(invalidHakuajat.head), "hakuajat[0]"))
    }
  }

  it should "delete all hakuajat if none is given" in {
    val oid = put(haku)
    val thisHaku = haku(oid)
    val lastModified = get(oid, thisHaku)
    update(thisHaku.copy(hakuajat = List()), lastModified)
    get(oid, thisHaku.copy(hakuajat = List()))
  }

  it should "allow oph user to update from julkaistu to tallennettu" in {
    val id = put(haku)
    val lastModified = get(id, haku(id))
    update(haku(id).copy(tila = Tallennettu), lastModified, expectUpdate = true, ophSession)
    get(id, haku(id).copy(tila = Tallennettu, muokkaaja = OphUserOid, metadata = Some(haku.metadata.get.copy(isMuokkaajaOphVirkailija = Some(true)))))
  }

  it should "not allow non oph user to update from julkaistu to tallennettu" in {
    val id = put(haku)
    val lastModified = get(id, haku(id))
    update(haku(id).copy(tila = Tallennettu), lastModified, 403, crudSessions(haku.organisaatioOid))
  }
}
