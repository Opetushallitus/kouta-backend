package fi.oph.kouta.integration

import java.time.{Instant, LocalDateTime}
import fi.oph.kouta.TestData
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.validation.ValidationError
import fi.oph.kouta.validation.Validations._

class HakuSpec extends KoutaIntegrationSpec with AccessControlSpec with EverythingFixture {

  override val roleEntities: Seq[Role.Haku.type] = Seq(Role.Haku)

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

  it should "validate dates only when adding a new julkaistu haku" in {
    val thisHaku = hakuWithAlkamisvuosi(haku, "2007")

    put(thisHaku.copy(tila = Tallennettu))

    put(HakuPath, bytes(thisHaku.copy(tila = Julkaistu)), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(pastDateMsg("2007"), "metadata.koulutuksenAlkamiskausi.koulutuksenAlkamisvuosi"))
    }
  }

  it should "validate alkamiskausi is given for yhteishaku" in {
    put(HakuPath, yhteishakuWithoutAlkamiskausi, ophSession, 400)
  }

  it should "not validate alkamiskausi is given if not yhteishaku" in {
    put(HakuPath, bytes(jatkuvaHakuWithoutAlkamiskausi), defaultHeaders) {
      withClue(body) {
        status should equal(200)
      }
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

  it should "store and update unfinished haku" in {
    val unfinishedHaku = Haku(muokkaaja = TestUserOid, organisaatioOid = LonelyOid, modified = None, kielivalinta = Seq(Fi), nimi = Map(Fi -> "haku"))
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

  it should "validate dates when moving from other states to julkaistu" in {
    val thisHaku = hakuWithAlkamisvuosi(haku, "2017").copy(tila = Tallennettu)

    val oid = put(thisHaku)
    val thisHakuWithOid = thisHaku.copy(oid = Some(HakuOid(oid)))
    val lastModified = get(oid, thisHakuWithOid)

    post(HakuPath, bytes(thisHakuWithOid.copy(tila = Julkaistu)), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(pastDateMsg("2017"), "metadata.koulutuksenAlkamiskausi.koulutuksenAlkamisvuosi"))
    }

    update(thisHakuWithOid.copy(tila = Poistettu), lastModified)
  }

  it should "not validate dates when updating a julkaistu haku" in {
    val oid = put(haku)
    val lastModified = get(oid, haku(oid))

    update(hakuWithAlkamisvuosi(haku(oid), "2017"), lastModified)
  }


  it should "update haun päivämäärät" in {
    val pvmHaku = haku.copy(
      hakukohteenLiittamisenTakaraja = Some(TestData.inFuture(50000)),
      hakukohteenMuokkaamisenTakaraja = None)
    val oid = put(pvmHaku)
    val lastModified = get(oid, pvmHaku.copy(oid = Some(HakuOid(oid))))

    val updatedPvmHaku = haku.copy(
      oid = Some(HakuOid(oid)),
      hakukohteenMuokkaamisenTakaraja = Some(TestData.inFuture(50000)),
      hakukohteenLiittamisenTakaraja = None)

    update(updatedPvmHaku, lastModified)
    get(oid, updatedPvmHaku)
  }

  it should "delete all hakuajat if none is given" in {
    val oid = put(haku)
    val thisHaku = haku(oid)
    val lastModified = get(oid, thisHaku)
    update(thisHaku.copy(hakuajat = List()), lastModified)
    get(oid, thisHaku.copy(hakuajat = List()))
  }

  it should "pass legal state changes" in {
    val id = put(haku.copy(tila = Tallennettu))
    var lastModified = get(id, haku(id).copy(tila = Tallennettu))
    update(haku(id).copy(tila = Julkaistu), lastModified, expectUpdate = true)
    lastModified = get(id, haku(id).copy(tila = Julkaistu))
    update(haku(id).copy(tila = Arkistoitu), lastModified, expectUpdate = true)
    lastModified = get(id, haku(id).copy(tila = Arkistoitu))
    update(haku(id).copy(tila = Julkaistu), lastModified, expectUpdate = true)
    lastModified = get(id, haku(id).copy(tila = Julkaistu))

    val tallennettuId = put(haku.copy(tila = Tallennettu))
    lastModified = get(tallennettuId, haku(tallennettuId).copy(tila = Tallennettu))
    update(haku(tallennettuId).copy(tila = Poistettu), lastModified, expectUpdate = true)
  }

  it should "fail illegal state changes" in {
    val tallennettuId = put(haku.copy(tila = Tallennettu))
    val julkaistuId = put(haku.copy(tila = Julkaistu))
    val arkistoituId = put(haku.copy(tila = Arkistoitu))

    var lastModified = get(tallennettuId, haku(tallennettuId).copy(tila = Tallennettu))
    update(HakuPath, haku(tallennettuId).copy(tila = Arkistoitu), ophSession, lastModified, 400, List(ValidationError("tila", illegalStateChange("haulle", Tallennettu, Arkistoitu))))
    lastModified = get(julkaistuId, haku(julkaistuId).copy(tila = Julkaistu))
    update(HakuPath, haku(julkaistuId).copy(tila = Poistettu), ophSession, lastModified, 400, List(ValidationError("tila", illegalStateChange("haulle", Julkaistu, Poistettu))))
    lastModified = get(arkistoituId, haku(arkistoituId).copy(tila = Arkistoitu))
    update(HakuPath, haku(arkistoituId).copy(tila = Tallennettu), ophSession, lastModified, 400, List(ValidationError("tila", illegalStateChange("haulle", Arkistoitu, Tallennettu))))
    update(HakuPath, haku(arkistoituId).copy(tila = Poistettu), ophSession, lastModified, 400, List(ValidationError("tila", illegalStateChange("haulle", Arkistoitu, Poistettu))))
  }

  private def createHakuWithHakukohteet(markAllHakukohteetDeleted: Boolean) = {
    val koulutusOid = put(koulutus.copy(tila = Tallennettu), ophSession)
    val toteutusOid = put(toteutus(koulutusOid).copy(tila = Tallennettu), ophSession)
    val hakuOid = put(haku.copy(tila = Tallennettu))
    put(hakukohde(toteutusOid, hakuOid).copy(tila = Poistettu), ophSession)
    put(hakukohde(toteutusOid, hakuOid).copy(tila = if (markAllHakukohteetDeleted) Poistettu else Tallennettu), ophSession)
    val lastModified = get(hakuOid, haku(hakuOid).copy(tila = Tallennettu))
    (hakuOid, lastModified)
  }

  it should "pass deletion when related hakukohteet deleted" in {
    val (hakuOid: String, lastModified: String) = createHakuWithHakukohteet(true)
    update(haku(hakuOid).copy(tila = Poistettu), lastModified, expectUpdate = true)
  }

  it should "fail deletion when all related hakukohteet not deleted" in {
    val (hakuOid: String, lastModified: String) = createHakuWithHakukohteet(false)
    update(HakuPath, haku(hakuOid).copy(tila = Poistettu), ophSession, lastModified, 400, List(ValidationError("tila", integrityViolationMsg("Hakua", "hakukohteita"))))
  }

}
