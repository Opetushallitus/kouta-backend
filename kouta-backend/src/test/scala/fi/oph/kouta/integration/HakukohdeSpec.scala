package fi.oph.kouta.integration

import fi.oph.kouta.TestData.{Liite1, Liite2}
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.mocks.{LokalisointiServiceMock, MockAuditLogger}
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.util.UnitSpec
import fi.oph.kouta.validation.ValidationError
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.{TestData, TestOids}

import java.time.LocalDateTime
import java.util.UUID

class HakukohdeSpec extends UnitSpec with KoutaIntegrationSpec with AccessControlSpec with EverythingFixture with LokalisointiServiceMock {

  override val roleEntities = Seq(Role.Hakukohde)

  var (koulutusOid, toteutusOid, hakuOid) = ("", "", "")
  var valintaperusteId: UUID = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    koulutusOid = put(koulutus, ophSession)
    toteutusOid = put(toteutus(koulutusOid).copy(tarjoajat = List(AmmOid)))
    hakuOid = put(haku)
    valintaperusteId = put(valintaperuste)
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }

  lazy val uusiHakukohde: Hakukohde = hakukohde(toteutusOid, hakuOid, valintaperusteId)
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

  it should "use lokalisointiclient for fetching nimi postfix for tuva vaativana erityisenä tukena" in {
    mockLokalisointiResponse("yleiset.vaativanaErityisenaTukena")
    val tuvaKoulutusOid = put(tuvaKoulutus, ophSession)
    val tuvaToteutusOid = put(tuvaToteutus(tuvaKoulutusOid))
    val tuvaValintaperusteId = put(tuvaValintaperuste)
    val tuvaHakukohde: Hakukohde = hakukohde(tuvaToteutusOid, hakuOid, tuvaValintaperusteId)
    val oid = put(tuvaHakukohde)

    get(oid, tallennettuHakukohde(oid).copy(
      toteutusOid = ToteutusOid(tuvaToteutusOid),
      valintaperusteId = Some(tuvaValintaperusteId),
      _enrichedData = Some(HakukohdeEnrichedData(esitysnimi = Map(
        Fi -> s"""Hakukohde fi (yleiset.vaativanaErityisenaTukena fi)""".stripMargin,
        Sv -> s"""Hakukohde sv (yleiset.vaativanaErityisenaTukena sv)""".stripMargin),
        muokkaajanNimi = Some("Testi Muokkaaja")
      ))))
  }

  it should "return error when trying to get deleted hakukohde" in {
    val oid = put(uusiHakukohde.copy(tila = Poistettu), ophSession)
    get(s"$HakukohdePath/$oid", ophSession, 404)
  }

  it should "return ok when getting deleted hakukohde with myosPoistetut = true" in {
    val oid = put(uusiHakukohde.copy(tila = Poistettu), ophSession)
    get(s"$HakukohdePath/$oid?myosPoistetut=true", ophSession, 200)
  }

  "Create hakukohde" should "store hakukohde" in {
    val oid = put(uusiHakukohde)
    get(oid, tallennettuHakukohde(oid))
  }

  it should "store true as the value of isMuokkaajaOphVirkailija when muokkaaja is OPH virkailija" in {
    val uusiHk = uusiHakukohde.copy(muokkaaja = OphUserOid)
    val oid = put(uusiHk, ophSession)
    val tallennettuHk = tallennettuHakukohde(oid)
    val tallennettuHkMetadata = tallennettuHk.metadata.get
    val tallennettuHkCopy = tallennettuHk.copy(muokkaaja = OphUserOid, metadata = Some(tallennettuHkMetadata.copy(isMuokkaajaOphVirkailija = Some(true))))
    get(oid, tallennettuHkCopy)
  }

  it should "read muokkaaja from the session" in {
    val oid = put(uusiHakukohde.copy(muokkaaja = UserOid("random")))
    get(oid, tallennettuHakukohde(oid).copy(muokkaaja = testUser.oid))
  }

  it should "fail to store hakukohde if the toteutus does not exist" in {
    val toteutusOid = TestOids.randomToteutusOid.s
    put(HakukohdePath, hakukohde(toteutusOid, hakuOid, valintaperusteId), 400, "toteutusOid", nonExistent("Toteutusta", toteutusOid))
  }

  it should "fail to store hakukohde without jarjestyspaikkaOid" in {
    put(HakukohdePath, hakukohde(toteutusOid, hakuOid, valintaperusteId).copy(jarjestyspaikkaOid = None), 400, "jarjestyspaikkaOid", missingMsg)
  }

  it should "fail to store hakukohde if the haku does not exist" in {
    val hakuOid = TestOids.randomHakuOid.s
    put(HakukohdePath, hakukohde(toteutusOid, hakuOid, valintaperusteId), 400, "hakuOid", nonExistent("Hakua", hakuOid))
  }

  it should "fail to store hakukohde if the valintaperuste does not exist" in {
    val valintaperusteId = UUID.randomUUID()
    put(HakukohdePath, hakukohde(toteutusOid, hakuOid, valintaperusteId), 400, "valintaperusteId", nonExistent("Valintaperustetta", valintaperusteId))
  }

  it should "fail to store julkaistu hakukohde if the toteutus is not yet julkaistu" in {
    val toteutusOid = put(toteutus(koulutusOid).copy(tila = Tallennettu))
    put(HakukohdePath, hakukohde(toteutusOid, hakuOid, valintaperusteId), 400, "tila", notYetJulkaistu("Toteutusta", toteutusOid))
  }

  it should "store julkaistu hakukohde while haku is not yet julkaistu" in {
    val hakuOid = put(haku.copy(tila = Tallennettu))
    put(hakukohde(toteutusOid, hakuOid, valintaperusteId))
  }

  it should "fail to store julkaistu hakukohde if the valintaperuste is not yet julkaistu" in {
    val valintaperusteId = put(valintaperuste.copy(tila = Tallennettu, metadata = None))
    put(HakukohdePath, hakukohde(toteutusOid, hakuOid, valintaperusteId), 400, "tila", notYetJulkaistu("Valintaperustetta", valintaperusteId))
  }

  it should "fail to store hakukohde if the tyyppi of valintaperuste does not match the tyyppi of toteutus" in {
    val valintaperusteId = put(valintaperuste.copy(koulutustyyppi = Yo, metadata = Some(TestData.YoValintaperusteMetadata)))
    put(HakukohdePath, hakukohde(toteutusOid, hakuOid, valintaperusteId), 400, "valintaperusteId", tyyppiMismatch("Toteutuksen", toteutusOid, "valintaperusteen", valintaperusteId))
  }

  it should "fail to store lukiokoulutus if hakukohde does not contain hakukohteenLinja" in {
    val koulutusOid = put(koulutus.copy(koulutustyyppi = Lk, metadata = Some(LukioKoulutusMetadata())), ophSession)
    val toteutusOid = put(toteutus.copy(koulutusOid = KoulutusOid(koulutusOid), metadata = Some(TestData.LukioToteutus.metadata.get)))
    val valintaperusteId = put(valintaperuste.copy(koulutustyyppi = Lk, metadata = Some(TestData.LkValintaperusteMetadata)))
    put(HakukohdePath, hakukohde(toteutusOid, hakuOid, valintaperusteId), 400, "metadata.hakukohteenLinja", missingMsg)
  }

  it should "store ammatillinen tutkinnon osa hakukohde if toteutus uses hakemuspalvelu" in {
    val koulutusOid = put(TestData.AmmTutkinnonOsaKoulutus)
    val ammToToteutus = TestData.AmmTutkinnonOsaToteutus.copy(
      koulutusOid = KoulutusOid(koulutusOid),
      metadata = Some(TestData.AmmTutkinnonOsaToteutusMetadataHakemuspalvelu))
    val toteutusOid = put(ammToToteutus)
    put(hakukohde(toteutusOid, hakuOid))
  }

  it should "fail to store ammatillinen tutkinnon osa hakukohde if toteutus does not use hakemuspalvelu" in {
    val koulutusOid = put(TestData.AmmTutkinnonOsaKoulutus)
    val ammToToteutus = TestData.AmmTutkinnonOsaToteutus.copy(koulutusOid = KoulutusOid(koulutusOid))
    val toteutusOid = put(ammToToteutus)
    put(HakukohdePath, hakukohde(toteutusOid, hakuOid), 400, "toteutusOid", cannotLinkToHakukohde(toteutusOid))
  }

  it should "store vapaa sivistystyo muu hakukohde if toteutus uses hakemuspalvelu" in {
    val koulutusOid = put(TestData.VapaaSivistystyoMuuKoulutus, ophSession)
    val vsToToteutus = TestData.VapaaSivistystyoMuuToteutus.copy(
      koulutusOid = KoulutusOid(koulutusOid),
      metadata = Some(TestData.VapaaSivistystyoMuuToteutusHakemuspalveluMetatieto))
    val toteutusOid = put(vsToToteutus)
    put(hakukohde(toteutusOid, hakuOid))
  }

  it should "fail to store vapaa sivistystyo muu hakukohde if toteutus does not use hakemuspalvelu" in {
    val koulutusOid = put(TestData.VapaaSivistystyoMuuKoulutus, ophSession)
    val vsToToteutus = TestData.VapaaSivistystyoMuuToteutus.copy(koulutusOid = KoulutusOid(koulutusOid))
    val toteutusOid = put(vsToToteutus)
    put(HakukohdePath, hakukohde(toteutusOid, hakuOid), 400, "toteutusOid", cannotLinkToHakukohde(toteutusOid))
  }

  it should "store hakukohde without nimi if hakukohdeKoodiUri given" in {
    val koulutusOid = put(TestData.AmmKoulutus, ophSession)
    val ammToToteutus = TestData.JulkaistuAmmToteutus.copy(koulutusOid = KoulutusOid(koulutusOid))
    val toteutusOid = put(ammToToteutus)
    val ammHakukohde = hakukohde(toteutusOid, hakuOid).copy(nimi = Map(), hakukohdeKoodiUri = Some("hakukohteetperusopetuksenjalkeinenyhteishaku_101#1"))
    put(ammHakukohde)
  }

  it should "fail to store hakukohde if both nimi and hakukohdeKoodiUri given" in {
    val koulutusOid = put(TestData.AmmKoulutus, ophSession)
    val ammToToteutus = TestData.JulkaistuAmmToteutus.copy(koulutusOid = KoulutusOid(koulutusOid))
    val toteutusOid = put(ammToToteutus)
    val ammHakukohde = hakukohde(toteutusOid, hakuOid).copy(nimi = Map( Fi -> "Hakukohteen nimi fi", Sv -> "Hakukohteen nimi sv"), hakukohdeKoodiUri = Some("hakukohteetperusopetuksenjalkeinenyhteishaku_101#1"))

    put(HakukohdePath, ammHakukohde, 400, "nimi", oneNotBoth("nimi", "hakukohdeKoodiUri"))
  }

  it should "fail to store julkaistu toisen asteen yhteishaku hakukohde without valintaperuste" in {
    val koulutusOid = put(TestData.AmmKoulutus, ophSession)
    val ammToToteutus = TestData.JulkaistuAmmToteutus.copy(koulutusOid = KoulutusOid(koulutusOid))
    val toteutusOid = put(ammToToteutus)
    val hakuOid = put(TestData.JulkaistuHaku.copy(hakutapaKoodiUri = Some("hakutapa_01#1")), ophSession)
    val ammHakukohde = hakukohde(toteutusOid, hakuOid).copy(tila = Julkaistu)

    put(HakukohdePath, ammHakukohde, 400, "valintaperusteId", missingMsg)
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

  it should "fail to update hakukohde if the toteutus does not exist" in {
    val oid = put(uusiHakukohde)
    val lastModified = get(oid, tallennettuHakukohde(oid))
    val toteutusOid = TestOids.randomToteutusOid
    val updatedHakukohde = tallennettuHakukohde(oid).copy(toteutusOid = toteutusOid)
    update(HakukohdePath, updatedHakukohde, lastModified, 400, "toteutusOid", nonExistent("Toteutusta", toteutusOid))
  }

  it should "fail to update hakukohde if the haku does not exist" in {
    val oid = put(uusiHakukohde)
    val lastModified = get(oid, tallennettuHakukohde(oid))
    val hakuOid = TestOids.randomHakuOid
    val updatedHakukohde = tallennettuHakukohde(oid).copy(hakuOid = hakuOid)
    update(HakukohdePath, updatedHakukohde, lastModified, 400, "hakuOid", nonExistent("Hakua", hakuOid))
  }

  it should "fail to update hakukohde if the valintaperuste does not exist" in {
    val oid = put(hakukohde(toteutusOid, hakuOid))
    val savedHakukohde = getIds(hakukohde(oid, toteutusOid, hakuOid))
    val lastModified = get(oid, savedHakukohde)
    val valintaperusteId = UUID.randomUUID()
    val updatedHakukohde = savedHakukohde.copy(valintaperusteId = Some(valintaperusteId))
    update(HakukohdePath, updatedHakukohde, lastModified, 400, "valintaperusteId", nonExistent("Valintaperustetta", valintaperusteId))
  }

  it should "fail to update julkaistu hakukohde if the toteutus is not yet julkaistu" in {
    val toteutusOid = put(toteutus(koulutusOid).copy(tila = Tallennettu))
    val oid = put(hakukohde(toteutusOid, hakuOid, valintaperusteId).copy(tila = Tallennettu))
    val savedHakukohde = tallennettuHakukohde(oid).copy(toteutusOid = ToteutusOid(toteutusOid), tila = Tallennettu)
    val lastModified = get(oid, savedHakukohde)
    val updatedHakukohde = savedHakukohde.copy(tila = Julkaistu)
    update(HakukohdePath, updatedHakukohde, lastModified, 400, "tila", notYetJulkaistu("Toteutusta", toteutusOid))
  }

  it should "update julkaistu hakukohde while haku is not yet julkaistu" in {
    val hakuOid = put(haku.copy(tila = Tallennettu))
    val oid = put(hakukohde(toteutusOid, hakuOid, valintaperusteId).copy(tila = Tallennettu))
    val savedHakukohde = tallennettuHakukohde(oid).copy(hakuOid = HakuOid(hakuOid), tila = Tallennettu)
    val lastModified = get(oid, savedHakukohde)
    val updatedHakukohde = savedHakukohde.copy(tila = Julkaistu)
    update(updatedHakukohde, lastModified)
  }

  it should "fail to update julkaistu hakukohde if the valintaperuste is not yet julkaistu" in {
    val valintaperusteId = put(valintaperuste.copy(tila = Tallennettu))
    val oid = put(hakukohde(toteutusOid, hakuOid, valintaperusteId).copy(tila = Tallennettu))
    val savedHakukohde = tallennettuHakukohde(oid).copy(valintaperusteId = Some(valintaperusteId), tila = Tallennettu)
    val lastModified = get(oid, savedHakukohde)
    val updatedHakukohde = savedHakukohde.copy(tila = Julkaistu)
    update(HakukohdePath, updatedHakukohde, lastModified, 400, "tila", notYetJulkaistu("Valintaperustetta", valintaperusteId))
  }

  it should "fail to update hakukohde if the tyyppi of valintaperuste does not match the tyyppi of toteutus" in {
    val oid = put(hakukohde(toteutusOid, hakuOid))
    val savedHakukohde = getIds(hakukohde(oid, toteutusOid, hakuOid))
    val lastModified = get(oid, savedHakukohde)
    val valintaperusteId = put(TestData.YoValintaperuste)
    val updatedHakukohde = savedHakukohde.copy(valintaperusteId = Some(valintaperusteId))
    update(HakukohdePath, updatedHakukohde, lastModified, 400, "valintaperusteId", tyyppiMismatch("Toteutuksen", toteutusOid, "valintaperusteen", valintaperusteId))
  }

  it should "write hakukohde update to audit log" in {
    val oid = put(uusiHakukohde)
    val lastModified = get(oid, tallennettuHakukohde(oid))
    val updatedHakukohde = tallennettuHakukohde(oid).copy(tila = Arkistoitu).withModified(LocalDateTime.parse("1000-01-01T12:00:00"))
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
    update(thisHakukohde, lastModified, expectUpdate = false, crudSessions(hakukohde.organisaatioOid))
  }

  it should "allow a user of toteutuksen tarjoaja organization to update the hakukohde" in {
    val oid = put(uusiHakukohde)
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified = get(oid, thisHakukohde)
    update(thisHakukohde, lastModified, expectUpdate = false, crudSessions(AmmOid))
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
    update(thisHakukohde, lastModified, expectUpdate = false, crudSessions(ParentOid))
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
      hakuajat = List(Ajanjakso(alkaa = TestData.now(), paattyy = Some(TestData.inFuture(12000)))),
      _enrichedData = Some(
      HakukohdeEnrichedData(
        esitysnimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
        muokkaajanNimi = Some("Testi Muokkaaja")
      )
    ))
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
      hakuOid = HakuOid(hakuOid), organisaatioOid = ChildOid, modified = None, kielivalinta = Seq(Fi), nimi = Map(Fi -> "hakukohde"),
      _enrichedData = Some(
        HakukohdeEnrichedData(
          esitysnimi = Map(Fi -> "hakukohde"),
          muokkaajanNimi = Some("Testi Muokkaaja")
        )))
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

  it should "put, update and delete valintakokeet correctly" in {
    val hakukohdeWithValintakokeet = uusiHakukohde.copy(
      valintakokeet = Seq(TestData.Valintakoe1, TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_66#6")))
    )
    val oid = put(hakukohdeWithValintakokeet)
    val lastModified = get(oid, getIds(
      hakukohdeWithValintakokeet.copy(
        oid = Some(HakukohdeOid(oid)),
        liitteet = List(Liite1, Liite2),
        valintakokeet = Seq(TestData.Valintakoe1, TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_66#6")))
      )))
    val newValintakoe = TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_57#2"))
    val updateValintakoe = getIds(tallennettuHakukohde(oid)).valintakokeet.head.copy(nimi = Map(Fi -> "Uusi nimi", Sv -> "Uusi nimi på svenska"))
    update(tallennettuHakukohde(oid).copy(valintakokeet = Seq(newValintakoe, updateValintakoe)), lastModified)
    get(oid, getIds(tallennettuHakukohde(oid).copy(
      valintakokeet = Seq(newValintakoe, updateValintakoe),
      liitteet = List(Liite1, Liite2))))
  }

  it should "delete all hakuajat, liitteet ja valintakokeet nicely" in {
    val oid = put(uusiHakukohde)
    val tallennettu = tallennettuHakukohde(oid)
    val lastModified = get(oid, tallennettu)
    val muokattuHakukohde = tallennettu.copy(liitteet = List(), hakuajat = List(), valintakokeet = List(), tila = Tallennettu)
    update(muokattuHakukohde, lastModified, expectUpdate = true)
    get(oid, muokattuHakukohde)
  }

  it should "fail to post julkaistu hakukohde as an oppilaitos user if hakukohteen liittamisen takaraja has expired" in {
    val pastDate = TestData.inPast(100)
    val hakuOid = put(haku.copy(tila = Julkaistu, hakukohteenLiittamisenTakaraja = Some(pastDate), hakukohteenMuokkaamisenTakaraja = Some(pastDate)))
    val newHakukohde = hakukohde(toteutusOid, hakuOid, valintaperusteId)
    val session = addTestSession(Seq(Role.Hakukohde.Crud.asInstanceOf[Role]), newHakukohde.organisaatioOid)
    put(HakukohdePath, newHakukohde, session, 400)
  }

  it should "fail to update julkaistu hakukohde as an oppilaitos user if hakukohteen muokkaamisen takaraja has expired" in {
    val pastDate = TestData.inPast(100)
    val hakuOid = put(haku.copy(tila = Julkaistu, hakukohteenLiittamisenTakaraja = Some(pastDate), hakukohteenMuokkaamisenTakaraja = Some(pastDate)))
    val oid = put(hakukohde(toteutusOid, hakuOid, valintaperusteId), ophSession)
    var tallennettuHk = tallennettuHakukohde(oid).copy(hakuOid = HakuOid(hakuOid), muokkaaja = OphUserOid)
    tallennettuHk = tallennettuHk.copy(metadata = Some(tallennettuHk.metadata.get.copy(isMuokkaajaOphVirkailija = Some(true))))
    val muokattu = tallennettuHk.copy(tila = Tallennettu, muokkaaja = TestUserOid)
    val lastModified = get(oid, tallennettuHk)
    update(HakukohdePath, muokattu, lastModified, 400, "hakukohteenMuokkaamisenTakaraja", pastDateMsg(pastDate))
  }

  it should "change hakukohteen tila from julkaistu to tallennettu as an Oph-virkailija when hakukohteen muokkaamisen takaraja has expired" in {
    val pastDate = TestData.inPast(100)
    val hakuOid = put(haku.copy(tila = Julkaistu, hakukohteenMuokkaamisenTakaraja = Some(pastDate)))
    val oid = put(hakukohde(toteutusOid, hakuOid, valintaperusteId), ophSession)
    var tallennettuHk = tallennettuHakukohde(oid).copy(hakuOid = HakuOid(hakuOid), muokkaaja = OphUserOid)
    tallennettuHk = tallennettuHk.copy(metadata = Some(tallennettuHk.metadata.get.copy(isMuokkaajaOphVirkailija = Some(true))))
    val muokattu = tallennettuHk.copy(tila = Tallennettu)
    val lastModified = get(oid, tallennettuHk)
    update(muokattu, lastModified, expectUpdate = true, ophSession)
  }

  it should "pass legal state changes" in {
    val id = put(uusiHakukohde.copy(tila = Tallennettu), ophSession)
    val tallennettuHk = tallennettuHakukohde(id)
    val theHakukohde = tallennettuHk.copy(muokkaaja = OphUserOid, metadata = Some(tallennettuHk.metadata.get.copy(isMuokkaajaOphVirkailija = Some(true))))
    var lastModified = get(id, theHakukohde.copy(tila = Tallennettu))
    val updatedHakukohde = theHakukohde.copy(tila = Julkaistu)
    update(updatedHakukohde, lastModified, true, ophSession)
    lastModified = get(id, updatedHakukohde)
    update(updatedHakukohde.copy(tila = Arkistoitu), lastModified, true, ophSession)
    lastModified = get(id, updatedHakukohde.copy(tila = Arkistoitu))
    update(updatedHakukohde.copy(tila = Julkaistu), lastModified, true, ophSession)
    lastModified = get(id, updatedHakukohde.copy(tila = Julkaistu))
    update(updatedHakukohde.copy(tila = Tallennettu), lastModified, true, ophSession)
    lastModified = get(id, updatedHakukohde.copy(tila = Tallennettu))
    update(updatedHakukohde.copy(tila = Poistettu), lastModified, true, ophSession)

    val arkistoituId = put(uusiHakukohde.copy(tila = Arkistoitu), ophSession)
    val tallennettu = tallennettuHakukohde(arkistoituId).copy(muokkaaja = OphUserOid)
    lastModified = get(arkistoituId, tallennettu.copy(tila = Arkistoitu, metadata = Some(tallennettuHk.metadata.get.copy(isMuokkaajaOphVirkailija = Some(true)))))
    update(tallennettu.copy(tila = Julkaistu, metadata = Some(tallennettu.metadata.get.copy(isMuokkaajaOphVirkailija = Some(true)))), lastModified, true, ophSession)
    get(arkistoituId, tallennettu.copy(tila = Julkaistu, metadata = Some(tallennettu.metadata.get.copy(isMuokkaajaOphVirkailija = Some(true)))))
  }

  it should "fail illegal state changes" in {
    val tallennettuId = put(uusiHakukohde.copy(tila = Tallennettu), ophSession)
    val tallennettu = tallennettuHakukohde(tallennettuId).copy(tila = Tallennettu, muokkaaja = OphUserOid)
    val julkaistuId = put(uusiHakukohde.copy(tila = Julkaistu), ophSession)
    val julkaistu = tallennettuHakukohde(julkaistuId).copy(tila = Julkaistu, muokkaaja = OphUserOid)
    val arkistoituId = put(uusiHakukohde.copy(tila = Arkistoitu), ophSession)
    val arkistoitu = tallennettuHakukohde(arkistoituId).copy(tila = Arkistoitu, muokkaaja = OphUserOid)

    var lastModified = get(tallennettuId, tallennettu.copy(metadata = Some(tallennettu.metadata.get.copy(isMuokkaajaOphVirkailija = Some(true)))))
    update(HakukohdePath, tallennettu.copy(tila = Arkistoitu), ophSession, lastModified, 400, List(ValidationError("tila", illegalStateChange("hakukohteelle", Tallennettu, Arkistoitu))))
    lastModified = get(julkaistuId, julkaistu.copy(metadata = Some(tallennettu.metadata.get.copy(isMuokkaajaOphVirkailija = Some(true)))))
    update(HakukohdePath, julkaistu.copy(tila = Poistettu), ophSession, lastModified, 400, List(ValidationError("tila", illegalStateChange("hakukohteelle", Julkaistu, Poistettu))))
    lastModified = get(arkistoituId, arkistoitu.copy(metadata = Some(tallennettu.metadata.get.copy(isMuokkaajaOphVirkailija = Some(true)))))
    update(HakukohdePath, arkistoitu.copy(tila = Tallennettu), ophSession, lastModified, 400, List(ValidationError("tila", illegalStateChange("hakukohteelle", Arkistoitu, Tallennettu))))
    update(HakukohdePath, arkistoitu.copy(tila = Poistettu), ophSession, lastModified, 400, List(ValidationError("tila", illegalStateChange("hakukohteelle", Arkistoitu, Poistettu))))
  }

  "Copy hakukohteet" should "make copies of two julkaistu hakukohde and related toteutus and store them as tallennettu" in {
    val julkaistuHakukohde1Oid = put(hakukohde(toteutusOid, hakuOid, valintaperusteId))
    val julkaistuHakukohde2Oid = put(hakukohde(toteutusOid, hakuOid, valintaperusteId))
    val hakukohteet = List(julkaistuHakukohde1Oid, julkaistuHakukohde2Oid)
    val copyResponse = put(hakukohteet, hakuOid)
    val hakukohde1CopyOid = copyResponse.head.created.hakukohdeOid
    val toteutus1CopyOid = copyResponse.head.created.toteutusOid

    val tallennettuHakukohdeCopy = tallennettuHakukohde(copyResponse.head.oid.toString).copy(
      oid = hakukohde1CopyOid,
      toteutusOid = toteutus1CopyOid.get,
      hakuOid = HakuOid(hakuOid),
      tila = Tallennettu,
      liitteet = List(Liite2, Liite1))
    val tallennettuToteutusCopy = toteutus(koulutusOid).copy(
      oid = toteutus1CopyOid,
      tila = Tallennettu,
      tarjoajat = List(AmmOid),
      koulutuksetKoodiUri = Seq("koulutus_371101#1"))

    get(hakukohde1CopyOid.get.toString, getIds(tallennettuHakukohdeCopy))
    get(toteutus1CopyOid.get.toString, tallennettuToteutusCopy)

    val hakukohde2CopyOid = copyResponse.last.created.hakukohdeOid.get.toString

    val toteutus2CopyOid = copyResponse.last.created.toteutusOid.get.toString
    get(hakukohde2CopyOid, getIds(tallennettuHakukohdeCopy.copy(oid = Some(HakukohdeOid(hakukohde2CopyOid)), toteutusOid = ToteutusOid(toteutus2CopyOid))))
    get(toteutus2CopyOid, tallennettuToteutusCopy.copy(oid = Some(ToteutusOid(toteutus2CopyOid)), koulutuksetKoodiUri = Seq("koulutus_371101#1")))
  }

  it should "fail to copy non-existing hakukohde" in {
    val julkaistuHakukohde1Oid = put(hakukohde(toteutusOid, hakuOid, valintaperusteId))
    val unknownHakukohdeOid = julkaistuHakukohde1Oid + 1
    val hakukohteet = List(julkaistuHakukohde1Oid, unknownHakukohdeOid)
    val copyResponse = put(hakukohteet, hakuOid)

    copyResponse.length shouldBe 2
    copyResponse.head.status shouldBe "success"
    copyResponse.last.status shouldBe "error"
    copyResponse.last.created.hakukohdeOid shouldBe empty
    copyResponse.last.created.toteutusOid shouldBe empty
  }
}
