package fi.oph.kouta.integration

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData.{JulkaistuHakukohde, Liite1, Liite2, LukioHakukohteenLinja, LukioKoulutus, MinHakukohde}
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.fixture.{HakuFixture, HakukohdeFixture, KoulutusFixture, ValintaperusteFixture}
import fi.oph.kouta.mocks.{LokalisointiServiceMock, MockAuditLogger}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.validation.Validations._
import org.json4s.jackson.Serialization.read

import java.time.LocalDateTime
import java.util.UUID

class HakukohdeSpec
    extends KoutaIntegrationSpec
    with HakukohdeFixture
    with KoulutusFixture
    with HakuFixture
    with ValintaperusteFixture
    with LokalisointiServiceMock {

  override val roleEntities: Seq[RoleEntity] = Seq(Role.Hakukohde)

  var (koulutusOid, toteutusOid, hakuOid, yhteisHakuOid) = ("", "", "", "")
  var valintaperusteId: UUID                             = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    koulutusOid = put(koulutus, ophSession)
    toteutusOid = put(toteutus(koulutusOid).copy(tarjoajat = List(AmmOid, OtherOid, ChildOid)))
    hakuOid = put(haku)
    yhteisHakuOid = put(haku.copy(hakutapaKoodiUri = Some("hakutapa_01#1")), ophSession)
    valintaperusteId = put(valintaperuste)
  }

  lazy val uusiHakukohde: Hakukohde = hakukohde(toteutusOid, hakuOid, valintaperusteId)
  lazy val tallennettuHakukohde: String => Hakukohde = { oid: String =>
    withValintaperusteenValintakokeet(getIds(hakukohde(oid, toteutusOid, hakuOid, valintaperusteId)))
  }

  "Get hakukohde by oid" should "return 404 if hakukohde not found" in {
    get(s"$HakukohdePath/123", headers = Seq(defaultSessionHeader)) {
      status should equal(404)
      body should include("Unknown hakukohde oid")
    }
  }

  it should "return 401 if a valid session is not found" in {
    get(s"$HakukohdePath/123") {
      status should equal(401)
      body should include("Unauthorized")
    }
  }

  it should "allow a user of the hakukohde organization to read the hakukohde" in {
    val oid = put(withValintaperusteenValintakokeet(uusiHakukohde))
    get(oid, crudSessions(hakukohde.organisaatioOid), tallennettuHakukohde(oid))
  }

  it should "deny a user without access to the hakukohde organization" in {
    val oid = put(withValintaperusteenValintakokeet(uusiHakukohde))
    get(s"$HakukohdePath/$oid", crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to read the hakukohde" in {
    val oid = put(withValintaperusteenValintakokeet(uusiHakukohde))
    get(oid, crudSessions(ParentOid), tallennettuHakukohde(oid))
  }

  it should "deny a user with only access to a descendant organization" in {
    val oid = put(withValintaperusteenValintakokeet(uusiHakukohde))
    get(s"$HakukohdePath/$oid", crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    val oid = put(withValintaperusteenValintakokeet(uusiHakukohde))
    get(s"$HakukohdePath/$oid", otherRoleSession, 403)
  }

  it should "allow indexer access" in {
    val oid = put(withValintaperusteenValintakokeet(uusiHakukohde))
    get(oid, indexerSession, tallennettuHakukohde(oid))
  }

  it should "use lokalisointiclient for fetching nimi postfix for tuva vaativana erityisenä tukena" in {
    mockLokalisointiResponse("yleiset.vaativanaErityisenaTukena")
    val tuvaToteutusOid      = put(tuvaToteutus(put(tuvaKoulutus, ophSession)))
    val tuvaValintaperusteId = put(tuvaValintaperuste)
    val tuvaHakukohde: Hakukohde =
      hakukohde(tuvaToteutusOid, hakuOid, tuvaValintaperusteId).copy(nimi = Map(Fi -> "nimi", Sv -> "nimi sv"))
    val oid = put(withValintaperusteenValintakokeet(tuvaHakukohde))

    get(
      oid,
      tallennettuTuvaHakukohde(oid, tuvaToteutusOid, hakuOid, tuvaValintaperusteId)
    )
  }

  it should "return error when trying to get deleted hakukohde" in {
    val oid = put(withValintaperusteenValintakokeet(uusiHakukohde.copy(tila = Poistettu)), ophSession)
    get(s"$HakukohdePath/$oid", ophSession, 404)
  }

  it should "return ok when getting deleted hakukohde with myosPoistetut = true" in {
    val oid = put(withValintaperusteenValintakokeet(uusiHakukohde.copy(tila = Poistettu)), ophSession)
    get(s"$HakukohdePath/$oid?myosPoistetut=true", ophSession, 200)
  }

  "Create hakukohde" should "store hakukohde" in {
    val oid = put(withValintaperusteenValintakokeet(uusiHakukohde))
    get(oid, tallennettuHakukohde(oid))
  }

  it should "store true as the value of isMuokkaajaOphVirkailija when muokkaaja is OPH virkailija" in {
    val uusiHk                = uusiHakukohde.copy(muokkaaja = OphUserOid)
    val oid                   = put(withValintaperusteenValintakokeet(uusiHk), ophSession)
    val tallennettuHk         = tallennettuHakukohde(oid)
    val tallennettuHkMetadata = tallennettuHk.metadata.get
    val tallennettuHkCopy = tallennettuHk.copy(
      muokkaaja = OphUserOid,
      metadata = Some(tallennettuHkMetadata.copy(isMuokkaajaOphVirkailija = Some(true)))
    )
    get(oid, tallennettuHkCopy)
  }

  it should "read muokkaaja from the session" in {
    val oid = put(withValintaperusteenValintakokeet(uusiHakukohde.copy(muokkaaja = UserOid("random"))))
    get(oid, tallennettuHakukohde(oid).copy(muokkaaja = testUser.oid))
  }

  it should "fail to store hakukohde without jarjestyspaikkaOid" in {
    put(
      HakukohdePath,
      hakukohde(toteutusOid, hakuOid, valintaperusteId).copy(jarjestyspaikkaOid = None),
      400,
      "jarjestyspaikkaOid",
      missingMsg
    )
  }

  it should "store ammatillinen tutkinnon osa hakukohde if toteutus uses hakemuspalvelu" in {
    val koulutusOid = put(TestData.AmmTutkinnonOsaKoulutus)
    val ammToToteutus = TestData.AmmTutkinnonOsaToteutus.copy(
      koulutusOid = KoulutusOid(koulutusOid),
      metadata = Some(TestData.AmmTutkinnonOsaToteutusMetadataHakemuspalvelu)
    )
    val toteutusOid = put(ammToToteutus)
    put(hakukohde(toteutusOid, hakuOid))
  }

  it should "store hakukohde without nimi if hakukohdeKoodiUri given" in {
    val koulutusOid   = put(TestData.AmmKoulutus, ophSession)
    val ammToToteutus = TestData.JulkaistuAmmToteutus.copy(koulutusOid = KoulutusOid(koulutusOid))
    val toteutusOid   = put(ammToToteutus)
    val ammHakukohde = hakukohde(toteutusOid, yhteisHakuOid).copy(
      nimi = Map(),
      hakukohdeKoodiUri = Some("hakukohteetperusopetuksenjalkeinenyhteishaku_101#1")
    )
    put(ammHakukohde)
  }

  it should "populate empty nimi with toteutusnimi for tuva-hakukohde" in {
    val tuvaToteutusOid      = put(tuvaToteutus(put(tuvaKoulutus, ophSession)))
    val tuvaValintaperusteId = put(tuvaValintaperuste)
    val tuvaHakukohde        = hakukohde(tuvaToteutusOid, hakuOid, tuvaValintaperusteId).copy(nimi = Map())
    val oid                  = put(withValintaperusteenValintakokeet(tuvaHakukohde))
    get(oid, tallennettuTuvaHakukohde(oid, tuvaToteutusOid, hakuOid, tuvaValintaperusteId))
  }

  it should "populate empty nimi with toteutusnimi for EB- and DIA -lukiokoulutus hakukohde" in {
    val lkToteutusOid = put(
      lukioToteutus(put(LukioKoulutus.copy(koulutuksetKoodiUri = Seq("koulutus_301104#1")), ophSession))
        .copy(nimi = Map(Fi -> "EB", Sv -> "EB sv"))
    )
    val lkHakukohde = hakukohde(lkToteutusOid, hakuOid).copy(nimi = Map())
    val oid         = put(lkHakukohde)
    get(oid, tallennettuLukioEBDIAHakukohde(oid, lkToteutusOid, hakuOid))
  }

  it should "populate empty nimi as yleislinja if selected for lukio-hakukohde" in {
    mockLokalisointiResponse("hakukohdelomake.lukionYleislinja")
    val lkToteutusOid = put(lukioToteutus(put(LukioKoulutus, ophSession)))
    val lkHakukohde = hakukohde(lkToteutusOid, hakuOid).copy(
      nimi = Map(),
      metadata = Some(
        hakukohde.metadata.get
          .copy(valintaperusteenValintakokeidenLisatilaisuudet = Seq(), hakukohteenLinja = Some(LukioHakukohteenLinja))
      )
    )
    val oid = put(lkHakukohde)
    get(
      oid,
      tallennettuLukioHakukohde(
        oid,
        lkToteutusOid,
        hakuOid,
        Map(Fi -> "hakukohdelomake.lukionYleislinja fi", Sv -> "hakukohdelomake.lukionYleislinja sv"),
        None
      )
    )
  }

  it should "populate empty nimi as painotus or erityiskoulutustehtävä if selected for lukio-hakukohde" in {
    val lkToteutusOid = put(lukioToteutus(put(LukioKoulutus, ophSession)))
    val lkHakukohde = hakukohde(lkToteutusOid, hakuOid).copy(
      nimi = Map(),
      metadata = Some(
        hakukohde.metadata.get
          .copy(
            valintaperusteenValintakokeidenLisatilaisuudet = Seq(),
            hakukohteenLinja = Some(LukioHakukohteenLinja.copy(linja = Some("lukiopainotukset_1#1")))
          )
      )
    )
    val oid = put(lkHakukohde)
    get(
      oid,
      tallennettuLukioHakukohde(
        oid,
        lkToteutusOid,
        hakuOid,
        Map(Fi -> "nimi", Sv -> "nimi sv"),
        Some("lukiopainotukset_1#1")
      )
    )
  }

  it should "write create hakukohde to audit log" in {
    MockAuditLogger.clean()
    val oid =
      put(withValintaperusteenValintakokeet(uusiHakukohde.withModified(LocalDateTime.parse("1000-01-01T12:00:00"))))
    get(oid, tallennettuHakukohde(oid))
    MockAuditLogger.find(oid, "hakukohde_create") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "return 401 if a valid session is not found" in {
    put(uri = s"$HakukohdePath", bytes(uusiHakukohde), Seq(jsonHeader)) {
      status should equal(401)
      body should include("Unauthorized")
    }
  }

  it should "allow a user of the hakukohde organization to create the hakukohde" in {
    put(
      withValintaperusteenValintakokeet(uusiHakukohde.copy(tila = Tallennettu, jarjestyspaikkaOid = None)),
      crudSessions(hakukohde.organisaatioOid)
    )
  }

  it should "allow a user belonging to hakukohde- and järjestäjä-organizations to create the hakukohde" in {
    put(
      withValintaperusteenValintakokeet(
        uusiHakukohde.copy(organisaatioOid = AmmOid, jarjestyspaikkaOid = Some(ChildOid))
      ),
      ammAndChildSession
    )
  }

  it should "deny a user without access to the hakukohde organization" in {
    put(
      HakukohdePath,
      uusiHakukohde.copy(jarjestyspaikkaOid = Some(hakukohde.organisaatioOid)),
      crudSessions(LonelyOid),
      403
    )
  }

  it should "deny a user without access to järjestäjä organisation" in {
    put(
      HakukohdePath,
      uusiHakukohde.copy(jarjestyspaikkaOid = Some(OtherOid)),
      crudSessions(hakukohde.organisaatioOid),
      403
    )
  }

  it should "allow a user of an ancestor organization to create the hakukohde" in {
    put(
      withValintaperusteenValintakokeet(
        uusiHakukohde.copy(organisaatioOid = ChildOid, jarjestyspaikkaOid = Some(GrandChildOid))
      ),
      crudSessions(ParentOid)
    )
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
      body should equal(validationErrorBody(invalidAjanjaksoMsg(invalidHakuajat.head), "hakuajat[0]"))
    }
  }

  "Update hakukohde" should "update hakukohde" in {
    val oid              = put(withValintaperusteenValintakokeet(uusiHakukohde))
    val lastModified     = get(oid, tallennettuHakukohde(oid))
    val updatedHakukohde = tallennettuHakukohde(oid).copy(tila = Arkistoitu)
    update(updatedHakukohde, lastModified)
    get(oid, updatedHakukohde)
  }

  it should "read muokkaaja from the session" in {
    val oid = put(
      withValintaperusteenValintakokeet(uusiHakukohde.copy(jarjestyspaikkaOid = Some(GrandChildOid))),
      crudSessions(ChildOid)
    )
    val userOid = userOidForTestSessionId(crudSessions(ChildOid))
    val lastModified =
      get(oid, tallennettuHakukohde(oid).copy(muokkaaja = userOid, jarjestyspaikkaOid = Some(GrandChildOid)))
    val updatedHakukohde = tallennettuHakukohde(oid).copy(tila = Arkistoitu, muokkaaja = userOid)
    update(updatedHakukohde, lastModified)
    get(oid, updatedHakukohde.copy(muokkaaja = testUser.oid))
  }

  it should "write hakukohde update to audit log" in {
    val oid          = put(withValintaperusteenValintakokeet(uusiHakukohde))
    val lastModified = get(oid, tallennettuHakukohde(oid))
    val updatedHakukohde =
      tallennettuHakukohde(oid).copy(tila = Arkistoitu).withModified(LocalDateTime.parse("1000-01-01T12:00:00"))
    MockAuditLogger.clean()
    update(updatedHakukohde, lastModified)
    MockAuditLogger.findFieldChange("tila", "julkaistu", "arkistoitu", oid, "hakukohde_update") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "not update hakukohde when nothing changes" in {
    val oid           = put(withValintaperusteenValintakokeet(uusiHakukohde))
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified  = get(oid, thisHakukohde)
    MockAuditLogger.clean()
    update(thisHakukohde, lastModified, expectUpdate = false)
    MockAuditLogger.logs shouldBe empty
    get(oid, thisHakukohde)
  }

  it should "fail update if 'x-If-Unmodified-Since' header is missing" in {
    val oid           = put(withValintaperusteenValintakokeet(uusiHakukohde))
    val thisHakukohde = tallennettuHakukohde(oid)
    get(oid, thisHakukohde)
    post(HakukohdePath, bytes(thisHakukohde), Seq(defaultSessionHeader)) {
      status should equal(400)
      body should include(KoutaServlet.IfUnmodifiedSinceHeader)
    }
  }

  it should "fail update if modified in between get and update" in {
    val oid           = put(withValintaperusteenValintakokeet(uusiHakukohde))
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified  = get(oid, thisHakukohde)
    Thread.sleep(1500)
    update(tallennettuHakukohde(oid).copy(tila = Arkistoitu), lastModified)
    post(
      HakukohdePath,
      bytes(thisHakukohde),
      List((KoutaServlet.IfUnmodifiedSinceHeader, lastModified), defaultSessionHeader)
    ) {
      status should equal(409)
    }
  }

  it should "return 401 if a valid session is not found" in {
    val oid           = put(withValintaperusteenValintakokeet(uusiHakukohde))
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified  = get(oid, tallennettuHakukohde(oid))
    post(HakukohdePath, bytes(thisHakukohde), List((KoutaServlet.IfUnmodifiedSinceHeader, lastModified))) {
      status should equal(401)
    }
  }

  it should "allow a user of the hakukohde organization to update the hakukohde" in {
    val oid           = put(withValintaperusteenValintakokeet(uusiHakukohde.copy(tila = Tallennettu, jarjestyspaikkaOid = None)))
    val thisHakukohde = tallennettuHakukohde(oid).copy(tila = Tallennettu, jarjestyspaikkaOid = None)
    val lastModified  = get(oid, thisHakukohde)
    MockAuditLogger.clean()
    update(thisHakukohde, lastModified, expectUpdate = true, crudSessions(hakukohde.organisaatioOid)) // muokkaaja changed
    MockAuditLogger.logs should not be empty
    get(s"$HakukohdePath/${oid}", headers = Seq(sessionHeader(defaultSessionId))) {
      withClue(body) {
        status should equal(200)
      }
      val actualHakukohde = read[Hakukohde](body)
      actualHakukohde.muokkaaja should equal(userOidForTestSessionId(crudSessions(hakukohde.organisaatioOid)))
    }
  }

  it should "allow a user belonging to hakukohde- and järjestäjä-organizations to update the hakukohde" in {
    val oid           = put(withValintaperusteenValintakokeet(uusiHakukohde))
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified  = get(oid, thisHakukohde)
    update(
      thisHakukohde.copy(organisaatioOid = ChildOid, jarjestyspaikkaOid = Some(AmmOid)),
      lastModified,
      expectUpdate = true,
      ammAndChildSession
    )
  }

  it should "deny a user without access to the hakukohde organization" in {
    val oid = put(
      withValintaperusteenValintakokeet(uusiHakukohde.copy(jarjestyspaikkaOid = Some(uusiHakukohde.organisaatioOid)))
    )
    val thisHakukohde = tallennettuHakukohde(oid).copy(jarjestyspaikkaOid = Some(uusiHakukohde.organisaatioOid))
    val lastModified  = get(oid, thisHakukohde)
    update(thisHakukohde, lastModified, 403, crudSessions(LonelyOid))
  }

  it should "deny a user without access to järjestäjä organisation" in {
    val oid           = put(withValintaperusteenValintakokeet(uusiHakukohde.copy(jarjestyspaikkaOid = Some(OtherOid))))
    val thisHakukohde = tallennettuHakukohde(oid).copy(jarjestyspaikkaOid = Some(OtherOid))
    val lastModified  = get(oid, thisHakukohde)
    update(thisHakukohde, lastModified, 403, crudSessions(uusiHakukohde.organisaatioOid))
  }

  it should "allow a user of an ancestor organization to update the hakukohde" in {
    val oid           = put(withValintaperusteenValintakokeet(uusiHakukohde.copy(organisaatioOid = ChildOid)))
    val thisHakukohde = tallennettuHakukohde(oid).copy(organisaatioOid = ChildOid)
    val lastModified  = get(oid, thisHakukohde)
    update(
      thisHakukohde.copy(jarjestyspaikkaOid = Some(GrandChildOid)),
      lastModified,
      expectUpdate = true,
      crudSessions(ParentOid)
    )
  }

  it should "deny a user with only access to a descendant organization" in {
    val oid           = put(withValintaperusteenValintakokeet(uusiHakukohde))
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified  = get(oid, thisHakukohde)
    update(thisHakukohde, lastModified, 403, crudSessions(GrandChildOid))
  }

  it should "deny a user with the wrong role" in {
    val oid           = put(withValintaperusteenValintakokeet(uusiHakukohde))
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified  = get(oid, thisHakukohde)
    update(thisHakukohde, lastModified, 403, readSessions(hakukohde.organisaatioOid))
  }

  it should "allow organisaatioOid change if user had rights to new organisaatio" in {
    val oid           = put(withValintaperusteenValintakokeet(uusiHakukohde.copy(jarjestyspaikkaOid = Some(AmmOid))))
    val thisHakukohde = tallennettuHakukohde(oid).copy(jarjestyspaikkaOid = Some(AmmOid))
    val lastModified  = get(oid, thisHakukohde)
    update(thisHakukohde.copy(organisaatioOid = AmmOid), lastModified, expectUpdate = true, ammAndChildSession)
  }

  it should "fail organisaatioOid change if user doesn't have rights to new organisaatio" in {
    val oid           = put(withValintaperusteenValintakokeet(uusiHakukohde.copy(jarjestyspaikkaOid = Some(AmmOid))))
    val thisHakukohde = tallennettuHakukohde(oid).copy(jarjestyspaikkaOid = Some(AmmOid))
    val lastModified  = get(oid, thisHakukohde)
    update(thisHakukohde.copy(organisaatioOid = LukioOid), lastModified, 403, ammAndChildSession)
  }

  it should "deny indexer access" in {
    val oid           = put(withValintaperusteenValintakokeet(uusiHakukohde))
    val thisHakukohde = tallennettuHakukohde(oid)
    val lastModified  = get(oid, thisHakukohde)
    update(thisHakukohde, lastModified, 403, indexerSession)
  }

  it should "update hakukohteen tekstit ja hakuajat" in {
    val oid          = put(withValintaperusteenValintakokeet(uusiHakukohde))
    val lastModified = get(oid, tallennettuHakukohde(oid))
    val muokattuHakukohde = tallennettuHakukohde(oid).copy(
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv"),
      hakulomaketyyppi = Some(Ataru),
      hakulomakeAtaruId = Some(UUID.randomUUID()),
      hakulomakeKuvaus = Map(),
      hakuajat = List(Ajanjakso(alkaa = TestData.now(), paattyy = Some(TestData.inFuture(12000)))),
      _enrichedData = Some(
        HakukohdeEnrichedData(
          esitysnimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv"),
          muokkaajanNimi = Some("Testi Muokkaaja")
        )
      )
    )
    update(muokattuHakukohde, lastModified, expectUpdate = true)
    get(oid, muokattuHakukohde)
  }

  it should "update hakukohteen muokkaaja on hakuajat change" in {
    val oid          = put(withValintaperusteenValintakokeet(uusiHakukohde), ophSession)
    val ophTallennettuHakukohde = tallennettuHakukohde(oid).copy(muokkaaja = OphUserOid,
      metadata = Some(tallennettuHakukohde(oid).metadata.get.copy(isMuokkaajaOphVirkailija = Some(true))))
    val lastModified = get(oid, ophTallennettuHakukohde)
    assert(readHakukohdeMuokkaaja(oid) == OphUserOid.toString)
    val muokattuHakukohde = tallennettuHakukohde(oid).copy(
      hakuajat = List(Ajanjakso(alkaa = TestData.now(), paattyy = Some(TestData.inFuture(12000))))
    )
    update(muokattuHakukohde, lastModified, expectUpdate = true, ophSession2)
    assert(readHakukohdeMuokkaaja(oid) == OphUserOid2.toString)
  }

  it should "update hakukohteen muokkaaja on hakuajat delete" in {
    val hakuajat = TestData.getHakuajatWeeksInFuture(1, 4) ++ TestData.getHakuajatWeeksInFuture(6, 8)
    // ei julkaistu jotta hakuajan voi poistaa
    val eiJulkaistuHakukohde = withValintaperusteenValintakokeet(uusiHakukohde).copy(
      tila = Tallennettu,
      hakuajat = hakuajat,
      liitteet = List(),
      valintakokeet = List(),
      metadata = Some(withValintaperusteenValintakokeet(uusiHakukohde).metadata.get.copy(isMuokkaajaOphVirkailija = Some(true)))
    )
    val oid                = put(eiJulkaistuHakukohde, ophSession)
    val eiJulkaistuWithOid = eiJulkaistuHakukohde.copy(oid = Some(HakukohdeOid(oid)), muokkaaja = OphUserOid)
    var lastModified       = get(oid, eiJulkaistuWithOid)
    assert(readHakukohdeMuokkaaja(oid) == OphUserOid.toString)
    // poistetaan yksi hakuaika
    val muokattuHakukohde = eiJulkaistuWithOid.copy(
      hakuajat = TestData.getHakuajatWeeksInFuture(1, 4)
    )
    update(muokattuHakukohde, lastModified, expectUpdate = true, ophSession2)
    assert(readHakukohdeMuokkaaja(oid) == OphUserOid2.toString)
    // poistetaan kaikki hakuajat
    val muokattuHakukohde2 = eiJulkaistuWithOid.copy(
      hakuajat = List()
    )
    lastModified = get(oid, muokattuHakukohde.copy(muokkaaja = OphUserOid2))
    update(muokattuHakukohde2, lastModified, expectUpdate = true, ophSession)
    assert(readHakukohdeMuokkaaja(oid) == OphUserOid.toString)
  }

  it should "delete all hakuajat and read last modified from history" in {
    val oid          = put(withValintaperusteenValintakokeet(uusiHakukohde.copy(tila = Tallennettu)))
    val lastModified = get(oid, tallennettuHakukohde(oid).copy(tila = Tallennettu))
    Thread.sleep(1500)
    val muokattuHakukohde = tallennettuHakukohde(oid).copy(hakuajat = List(), tila = Tallennettu)
    update(muokattuHakukohde, lastModified, expectUpdate = true)
    get(oid, muokattuHakukohde) should not equal lastModified
  }

  it should "add right amount of rows to history tables" in {
    resetTableHistory("hakukohteet")
    resetTableHistory("hakukohteiden_hakuajat")
    resetTableHistory("hakukohteiden_valintakokeet")
    resetTableHistory("hakukohteiden_liitteet")

    val oid          = put(withValintaperusteenValintakokeet(uusiHakukohde.copy(tila = Tallennettu)))
    val lastModified = get(oid, tallennettuHakukohde(oid).copy(tila = Tallennettu))
    val muokattuHakukohde =
      tallennettuHakukohde(oid).copy(liitteet = List(), hakuajat = List(), valintakokeet = List(), tila = Tallennettu)
    update(muokattuHakukohde, lastModified, expectUpdate = true)

    assert(getTableHistorySize("hakukohteet") == 1)
    assert(getTableHistorySize("hakukohteiden_hakuajat") == 1)
    assert(getTableHistorySize("hakukohteiden_valintakokeet") == 1)
    assert(getTableHistorySize("hakukohteiden_liitteet") == 2)
  }

  it should "store and update unfinished hakukohde" in {
    val unfinishedHakukohde = Hakukohde(
      muokkaaja = TestUserOid,
      toteutusOid = ToteutusOid(toteutusOid),
      hakuOid = HakuOid(hakuOid),
      organisaatioOid = ChildOid,
      modified = None,
      kielivalinta = Seq(Fi),
      nimi = Map(Fi -> "hakukohde"),
      _enrichedData = Some(
        HakukohdeEnrichedData(
          esitysnimi = Map(Fi -> "hakukohde"),
          muokkaajanNimi = Some("Testi Muokkaaja")
        )
      )
    )
    val oid            = put(unfinishedHakukohde)
    val lastModified   = get(oid, unfinishedHakukohde.copy(oid = Some(HakukohdeOid(oid))))
    val newToteutusOid = put(toteutus(koulutusOid))
    val newUnfinishedHakukohde =
      unfinishedHakukohde.copy(oid = Some(HakukohdeOid(oid)), toteutusOid = ToteutusOid(newToteutusOid))
    update(newUnfinishedHakukohde, lastModified)
    get(oid, newUnfinishedHakukohde)
  }

  it should "validate updated hakukohde" in {
    val oid             = put(withValintaperusteenValintakokeet(uusiHakukohde))
    val lastModified    = get(oid, tallennettuHakukohde(oid))
    val invalidHakuajat = TestData.getInvalidHakuajat
    post(
      HakukohdePath,
      bytes(tallennettuHakukohde(oid).copy(hakuajat = invalidHakuajat)),
      headersIfUnmodifiedSince(lastModified)
    ) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(invalidAjanjaksoMsg(invalidHakuajat.head), "hakuajat[0]"))
    }
  }

  it should "update hakukohteen liitteet ja valintakokeet" in {
    val oid          = put(withValintaperusteenValintakokeet(uusiHakukohde))
    val tallennettu  = tallennettuHakukohde(oid)
    val lastModified = get(oid, tallennettu)
    val muokattuHakukohde = tallennettu.copy(
      valintakokeet = List(TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_42#2"))),
      liitteet = tallennettu.liitteet.map(_.copy(toimitusaika = Some(TestData.inFuture(9000))))
    )
    update(muokattuHakukohde, lastModified, expectUpdate = true)
    get(oid, getIds(muokattuHakukohde))
  }

  it should "update muokkaaja on liitteet change" in {
    val oid         = put(withValintaperusteenValintakokeet(uusiHakukohde), ophSession)
    val tallennettu = tallennettuHakukohde(oid).copy(muokkaaja = OphUserOid,
      metadata = Some(tallennettuHakukohde(oid).metadata.get.copy(isMuokkaajaOphVirkailija = Some(true))))
    assert(readHakukohdeMuokkaaja(oid) == OphUserOid.toString)
    val lastModified = get(oid, tallennettu)
    val muokattuHakukohde = tallennettu.copy(
      liitteet = tallennettu.liitteet.map(_.copy(toimitusaika = Some(TestData.inFuture(9000))))
    )
    update(muokattuHakukohde, lastModified, expectUpdate = true, ophSession2)
    assert(readHakukohdeMuokkaaja(oid) == OphUserOid2.toString)
  }

  it should "update muokkaaja on liitteet delete" in {
    val oid = put(withValintaperusteenValintakokeet(uusiHakukohde), ophSession)
    val tallennettu = tallennettuHakukohde(oid).copy(muokkaaja = OphUserOid,
      metadata = Some(tallennettuHakukohde(oid).metadata.get.copy(isMuokkaajaOphVirkailija = Some(true))))
    assert(readHakukohdeMuokkaaja(oid) == OphUserOid.toString)
    val lastModified = get(oid, tallennettu)
    val muokattuHakukohde = tallennettu.copy(
      liitteet = List(tallennettu.liitteet.head)
    )
    update(muokattuHakukohde, lastModified, expectUpdate = true, ophSession2)
    assert(readHakukohdeMuokkaaja(oid) == OphUserOid2.toString)
    val muokattuHakukohde2 = tallennettu.copy(
      liitteet = List()
    )
    update(muokattuHakukohde2, lastModified, expectUpdate = true, ophSession)
    assert(readHakukohdeMuokkaaja(oid) == OphUserOid.toString)
  }

  it should "update muokkaaja on valintakokeet change" in {
    val oid         = put(withValintaperusteenValintakokeet(uusiHakukohde), ophSession)
    val tallennettu = tallennettuHakukohde(oid).copy(muokkaaja = OphUserOid,
      metadata = Some(tallennettuHakukohde(oid).metadata.get.copy(isMuokkaajaOphVirkailija = Some(true))))
    assert(readHakukohdeMuokkaaja(oid) == OphUserOid.toString)
    val lastModified = get(oid, tallennettu)
    val muokattuHakukohde = tallennettu.copy(
      valintakokeet = List(TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_42#2")))
    )
    update(muokattuHakukohde, lastModified, expectUpdate = true, ophSession2)
    assert(readHakukohdeMuokkaaja(oid) == OphUserOid2.toString)
  }

  it should "update muokkaaja on valintakokeet delete" in {
    val hakukohdeWithTwoValintakokeet = withValintaperusteenValintakokeet(
      uusiHakukohde.copy(
        valintakokeet =
          Seq(TestData.Valintakoe1, TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_66#6")))
      )
    )
    val oid = put(hakukohdeWithTwoValintakokeet, ophSession)
    assert(readHakukohdeMuokkaaja(oid) == OphUserOid.toString)
    val hakukohdeWithTwoValintakokeetWithIds = getIds(
      hakukohdeWithTwoValintakokeet.copy(
        oid = Some(HakukohdeOid(oid)),
        liitteet = List(Liite1, Liite2),
        valintakokeet =
          Seq(TestData.Valintakoe1, TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_66#6"))),
        muokkaaja = OphUserOid,
        metadata = Some(hakukohdeWithTwoValintakokeet.metadata.get.copy(isMuokkaajaOphVirkailija = Some(true)))
      )
    )
    var lastModified = get(oid, hakukohdeWithTwoValintakokeetWithIds)
    // delete one valintakoe
    val hakukohdeWithOneValintakokeet = hakukohdeWithTwoValintakokeetWithIds.copy(
      valintakokeet = List(hakukohdeWithTwoValintakokeetWithIds.valintakokeet.head))
    update(hakukohdeWithOneValintakokeet, lastModified, expectUpdate = true, ophSession2)
    assert(readHakukohdeMuokkaaja(oid) == OphUserOid2.toString)
    val hakukohdeWithNoValintakokeet = hakukohdeWithOneValintakokeet.copy(
      valintakokeet = List()
    )
    lastModified = get(oid, hakukohdeWithOneValintakokeet.copy(muokkaaja = OphUserOid2))
    update(hakukohdeWithNoValintakokeet, lastModified, expectUpdate = true, ophSession)
    assert(readHakukohdeMuokkaaja(oid) == OphUserOid.toString)
  }

  it should "put, update and delete valintakokeet correctly" in {
    val hakukohdeWithValintakokeet = withValintaperusteenValintakokeet(
      uusiHakukohde.copy(
        valintakokeet =
          Seq(TestData.Valintakoe1, TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_66#6")))
      )
    )
    val oid = put(hakukohdeWithValintakokeet)
    val lastModified = get(
      oid,
      getIds(
        hakukohdeWithValintakokeet.copy(
          oid = Some(HakukohdeOid(oid)),
          liitteet = List(Liite1, Liite2),
          valintakokeet =
            Seq(TestData.Valintakoe1, TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_66#6")))
        )
      )
    )
    val newValintakoe = TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_57#2"))
    val updateValintakoe = getIds(tallennettuHakukohde(oid)).valintakokeet.head
      .copy(nimi = Map(Fi -> "Uusi nimi", Sv -> "Uusi nimi på svenska"))
    update(tallennettuHakukohde(oid).copy(valintakokeet = Seq(newValintakoe, updateValintakoe)), lastModified)
    get(
      oid,
      withValintaperusteenValintakokeet(
        getIds(
          tallennettuHakukohde(oid)
            .copy(valintakokeet = Seq(newValintakoe, updateValintakoe), liitteet = List(Liite1, Liite2))
        )
      )
    )
  }

  it should "delete all hakuajat, liitteet ja valintakokeet nicely" in {
    val oid          = put(withValintaperusteenValintakokeet(uusiHakukohde.copy(tila = Tallennettu)))
    val tallennettu  = tallennettuHakukohde(oid).copy(tila = Tallennettu)
    val lastModified = get(oid, tallennettu)
    val muokattuHakukohde =
      tallennettu.copy(liitteet = List(), hakuajat = List(), valintakokeet = List(), tila = Tallennettu)
    update(muokattuHakukohde, lastModified, expectUpdate = true)
    get(oid, muokattuHakukohde)
  }

  it should "populate empty nimi with toteutusnimi for tuva-hakukohde" in {
    val tuvaToteutusOid      = put(tuvaToteutus(put(tuvaKoulutus, ophSession)))
    val tuvaValintaperusteId = put(tuvaValintaperuste)
    val tuvaHakukohde        = hakukohde(tuvaToteutusOid, hakuOid, tuvaValintaperusteId).copy(nimi = Map())
    val oid                  = put(withValintaperusteenValintakokeet(tuvaHakukohde))
    get(oid, tallennettuTuvaHakukohde(oid, tuvaToteutusOid, hakuOid, tuvaValintaperusteId))
  }

  it should "allow oph user to update from julkaistu to tallennettu" in {
    val oid          = put(withValintaperusteenValintakokeet(uusiHakukohde))
    val hk           = withValintaperusteenValintakokeet(tallennettuHakukohde(oid))
    val lastModified = get(oid, hk)
    update(hk.copy(tila = Tallennettu), lastModified, expectUpdate = true, ophSession)
    get(
      oid,
      hk.copy(
        tila = Tallennettu,
        muokkaaja = OphUserOid,
        metadata = Some(hk.metadata.get.copy(isMuokkaajaOphVirkailija = Some(true)))
      )
    )
  }

  it should "not allow non oph user to update from julkaistu to tallennettu" in {
    val oid          = put(withValintaperusteenValintakokeet(uusiHakukohde))
    val lastModified = get(oid, tallennettuHakukohde(oid))
    update(
      tallennettuHakukohde(oid).copy(tila = Tallennettu),
      lastModified,
      403,
      crudSessions(hakukohde.organisaatioOid)
    )
  }

  "Copy hakukohteet" should "make copies of two julkaistu hakukohde and related toteutus and store them as tallennettu" in {
    val julkaistuHakukohde1Oid =
      put(withValintaperusteenValintakokeet(hakukohde(toteutusOid, hakuOid, valintaperusteId)))
    val julkaistuHakukohde2Oid =
      put(withValintaperusteenValintakokeet(hakukohde(toteutusOid, hakuOid, valintaperusteId)))
    val hakukohteet       = List(julkaistuHakukohde1Oid, julkaistuHakukohde2Oid)
    val copyResponse      = put(hakukohteet, hakuOid)
    val hakukohde1CopyOid = copyResponse.head.created.hakukohdeOid
    val toteutus1CopyOid  = copyResponse.head.created.toteutusOid

    val tallennettuHakukohdeCopy = tallennettuHakukohde(copyResponse.head.oid.toString).copy(
      oid = hakukohde1CopyOid,
      toteutusOid = toteutus1CopyOid.get,
      hakuOid = HakuOid(hakuOid),
      tila = Tallennettu,
      liitteet = List(Liite2, Liite1)
    )
    val tallennettuToteutusCopy = toteutus(koulutusOid).copy(
      oid = toteutus1CopyOid,
      tila = Tallennettu,
      tarjoajat = List(AmmOid, OtherOid, ChildOid),
      koulutuksetKoodiUri = Seq("koulutus_371101#1")
    )

    // Hakukohteille erillinen tarkistus satunnaisen ongelman takia: Jossain tilanteissa hakukohteella listojen
    // järjestys vaihtuu, tämä havaittu ainakin liitteet-listan osalta. Seuraava metodi tarkistaa listojen sisällön
    // välittämättä järjestyksestä
    def checkHakukohde(hkOid: HakukohdeOid, hk: Hakukohde): Unit = {
      val expectedHakukohde = hk.copy(modified = Some(readHakukohdeModified(hkOid)))
      get(s"$HakukohdePath/${hkOid.s}", headers = Seq(sessionHeader(defaultSessionId))) {
        withClue(body) {
          status should equal(200)
        }
        debugJson(body)
        val actualHakukohde = read[Hakukohde](body)
        actualHakukohde.copy(liitteet = Seq(), valintakokeet = Seq()) should equal(
          expectedHakukohde.copy(liitteet = Seq(), valintakokeet = Seq())
        )
        actualHakukohde.liitteet.toSet should equal(expectedHakukohde.liitteet.toSet)
        actualHakukohde.valintakokeet.toSet should equal(expectedHakukohde.valintakokeet.toSet)
      }
    }

    checkHakukohde(hakukohde1CopyOid.get, getIds(tallennettuHakukohdeCopy))
    get(toteutus1CopyOid.get.toString, tallennettuToteutusCopy)

    val hakukohde2CopyOid = copyResponse.last.created.hakukohdeOid.get

    val toteutus2CopyOid = copyResponse.last.created.toteutusOid.get.toString
    checkHakukohde(
      hakukohde2CopyOid,
      getIds(tallennettuHakukohdeCopy.copy(oid = Some(hakukohde2CopyOid), toteutusOid = ToteutusOid(toteutus2CopyOid)))
    )
    get(
      toteutus2CopyOid,
      tallennettuToteutusCopy.copy(
        oid = Some(ToteutusOid(toteutus2CopyOid)),
        koulutuksetKoodiUri = Seq("koulutus_371101#1")
      )
    )
  }

  it should "fail to copy non-existing hakukohde" in {
    val julkaistuHakukohde1Oid =
      put(withValintaperusteenValintakokeet(hakukohde(toteutusOid, hakuOid, valintaperusteId)))
    val unknownHakukohdeOid = julkaistuHakukohde1Oid + 1
    val hakukohteet         = List(julkaistuHakukohde1Oid, unknownHakukohdeOid)
    val copyResponse        = put(hakukohteet, hakuOid)

    copyResponse.length shouldBe 2
    copyResponse.head.status shouldBe "success"
    copyResponse.last.status shouldBe "error"
    copyResponse.last.created.hakukohdeOid shouldBe empty
    copyResponse.last.created.toteutusOid shouldBe empty
  }

  "Change hakukohteet tila" should "change two julkaistu hakukohteet to arkistoitu when muokkaaja is OPH virkailija" in {
    val julkaistuHakukohde1 = withValintaperusteenValintakokeet(hakukohde(toteutusOid, hakuOid, valintaperusteId))
      .copy(liitteet = Seq(), valintakokeet = Seq())
    val julkaistuHakukohde2 = withValintaperusteenValintakokeet(hakukohde(toteutusOid, hakuOid, valintaperusteId))
      .copy(liitteet = Seq(), valintakokeet = Seq())

    val julkaistuHakukohde1Oid = put(julkaistuHakukohde1)
    val julkaistuHakukohde2Oid = put(julkaistuHakukohde2)

    val hakukohteet = List(julkaistuHakukohde1Oid, julkaistuHakukohde2Oid)

    val lastModified =
      get(julkaistuHakukohde1Oid, julkaistuHakukohde1.copy(oid = Some(HakukohdeOid(julkaistuHakukohde1Oid))))
    val response = changeTila(hakukohteet, "arkistoitu", lastModified, ophSession, 200)

    val metadata1 = julkaistuHakukohde1.metadata.get
    val metadata2 = julkaistuHakukohde2.metadata.get

    val arkistoituHakukohde1 = julkaistuHakukohde1.copy(
      oid = Some(HakukohdeOid(julkaistuHakukohde1Oid)),
      tila = Arkistoitu,
      muokkaaja = OphUserOid,
      metadata = Some(metadata1.copy(isMuokkaajaOphVirkailija = Some(true)))
    )
    val arkistoituHakukohde2 = julkaistuHakukohde2.copy(
      oid = Some(HakukohdeOid(julkaistuHakukohde2Oid)),
      tila = Arkistoitu,
      muokkaaja = OphUserOid,
      metadata = Some(metadata2.copy(isMuokkaajaOphVirkailija = Some(true)))
    )

    response.length shouldBe 2
    response.head.oid.toString shouldBe julkaistuHakukohde1Oid
    response.head.status shouldBe "success"
    response.last.oid.toString shouldBe julkaistuHakukohde2Oid
    response.last.status shouldBe "success"

    get(julkaistuHakukohde1Oid, arkistoituHakukohde1)
    get(julkaistuHakukohde2Oid, arkistoituHakukohde2)
  }

  it should "fail to change tila of hakukohteet from julkaistu to arkistoitu when muokkaaja is non oph user and contains random hakukohdeOid" in {
    val julkaistuHakukohde1 = withValintaperusteenValintakokeet(hakukohde(toteutusOid, hakuOid, valintaperusteId))
      .copy(liitteet = Seq(), valintakokeet = Seq())
    val julkaistuHakukohde2 = withValintaperusteenValintakokeet(hakukohde(toteutusOid, hakuOid, valintaperusteId))
      .copy(liitteet = Seq(), valintakokeet = Seq())

    val julkaistuHakukohde1Oid = put(julkaistuHakukohde1)
    val julkaistuHakukohde2Oid = put(julkaistuHakukohde2)
    val randomOid              = randomHakukohdeOid.toString

    val hakukohteet = List(julkaistuHakukohde1Oid, randomOid, julkaistuHakukohde2Oid)

    val lastModified =
      get(julkaistuHakukohde1Oid, julkaistuHakukohde1.copy(oid = Some(HakukohdeOid(julkaistuHakukohde1Oid))))
    val response = changeTila(hakukohteet, "arkistoitu", lastModified, crudSessions(LonelyOid), 200)

    response.length shouldBe 3

    response.head.oid.toString shouldBe julkaistuHakukohde1Oid
    response.head.status shouldBe "error"
    response.head.errorPaths shouldBe List("hakukohde")
    response.head.errorMessages should not be empty
    response.head.errorTypes shouldBe List("organizationauthorization")

    response(1).oid.toString shouldBe julkaistuHakukohde2Oid
    response(1).status shouldBe "error"
    response(1).errorPaths shouldBe List("hakukohde")
    response(1).errorMessages should not be empty
    response(1).errorTypes shouldBe List("organizationauthorization")

    response.last.oid.toString shouldBe randomOid
    response.last.status shouldBe "error"
    response.last.errorPaths shouldBe List("hakukohde")
    response.last.errorMessages should not be empty
    response.last.errorTypes shouldBe List("not found")
  }

  it should "allow to change tila of hakukohteet from julkaistu to arkistoitu when muokkaaja has rights to hakukohde" in {
    val julkaistuHakukohde1 = withValintaperusteenValintakokeet(hakukohde(toteutusOid, hakuOid, valintaperusteId)).copy(
      liitteet = Seq(),
      valintakokeet = Seq(),
      jarjestyspaikkaOid = Some(ChildOid)
    )
    val julkaistuHakukohde2 = withValintaperusteenValintakokeet(hakukohde(toteutusOid, hakuOid, valintaperusteId)).copy(
      liitteet = Seq(),
      valintakokeet = Seq(),
      jarjestyspaikkaOid = Some(ChildOid)
    )

    val julkaistuHakukohde1Oid = put(julkaistuHakukohde1)
    val julkaistuHakukohde2Oid = put(julkaistuHakukohde2)

    val hakukohteet = List(julkaistuHakukohde1Oid, julkaistuHakukohde2Oid)

    val lastModified =
      get(julkaistuHakukohde1Oid, julkaistuHakukohde1.copy(oid = Some(HakukohdeOid(julkaistuHakukohde1Oid))))
    val response = changeTila(hakukohteet, "arkistoitu", lastModified, ammAndChildSession, 200)

    response.length shouldBe 2
    response.head.oid.toString shouldBe julkaistuHakukohde1Oid
    response.head.status shouldBe "success"
    response.last.oid.toString shouldBe julkaistuHakukohde2Oid
    response.last.status shouldBe "success"
  }
}
