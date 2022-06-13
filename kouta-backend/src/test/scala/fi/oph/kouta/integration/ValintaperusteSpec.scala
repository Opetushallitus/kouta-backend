package fi.oph.kouta.integration

import java.time.LocalDateTime
import java.util.UUID
import fi.oph.kouta.TestData
import fi.oph.kouta.TestData.MinYoValintaperuste
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.validation.ValidationError
import fi.oph.kouta.validation.Validations._

class ValintaperusteSpec extends KoutaIntegrationSpec with AccessControlSpec with EverythingFixture {

  override val roleEntities: Seq[RoleEntity] = Seq(Role.Valintaperuste)

  def ophValintaperuste: Valintaperuste = valintaperuste.copy(julkinen = true, organisaatioOid = OphOid)

  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  "Get valintaperuste by id" should "return 404 if valintaperuste not found" in {
    get(s"/valintaperuste/${UUID.randomUUID()}", headers = defaultHeaders) {
      status should equal (404)
      body should include ("Unknown valintaperuste id")
    }
  }

  it should "return 401 if no session is found" in {
    get(s"/valintaperuste/${UUID.randomUUID()}") {
      status should equal(401)
    }
  }

  it should "allow a user of the valintaperuste organization to read the valintaperuste" in {
    val id = put(valintaperuste)
    get(id, crudSessions(valintaperuste.organisaatioOid), valintaperuste(id))
  }

  it should "deny a user without access to the valintaperuste organization" in {
    val id = put(valintaperuste)
    get(s"$ValintaperustePath/$id", crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to read the valintaperuste" in {
    val id = put(valintaperuste)
    get(id, crudSessions(ParentOid), valintaperuste(id))
  }

  it should "allow a user with only access to a descendant organization" in {
    val id = put(valintaperuste)
    get(id, crudSessions(GrandChildOid), valintaperuste(id))
  }

  it should "deny a user with the wrong role" in {
    val id = put(valintaperuste)
    get(s"$ValintaperustePath/$id", otherRoleSession, 403)
  }

  it should "allow the user of proper koulutustyyppi to read julkinen valintaperuste created by oph" in {
    val id = put(ophValintaperuste)
    get(id, readSessions(AmmOid), ophValintaperuste.copy(id = Some(id)))
  }

  it should "deny the user of wrong koulutustyyppi to read julkinen valintaperuste created by oph" in {
    val id = put(ophValintaperuste)
    get(s"$ValintaperustePath/$id", readSessions(YoOid), 403)
  }

  it should "allow indexer access" in {
    val id = put(valintaperuste)
    get(id, indexerSession, valintaperuste(id))
  }

  it should "return error when trying to get deleted valintaperuste" in {
    val oid = put(valintaperuste.copy(tila = Poistettu))
    get(s"$ValintaperustePath/$oid", ophSession, 404)
  }

  it should "return ok when getting deleted valintaperuste with myosPoistetut = true" in {
    val oid = put(valintaperuste.copy(tila = Poistettu))
    get(s"$ValintaperustePath/$oid?myosPoistetut=true", ophSession, 200)
  }

  "Create valintaperuste" should "store valintaperuste" in {
    val id = put(valintaperuste)
    get(id, valintaperuste(id))
  }

  it should "read muokkaaja from the session" in {
    val id = put(valintaperuste.copy(muokkaaja = UserOid("random")))
    get(id, valintaperuste(id).copy(muokkaaja = testUser.oid))
  }

  it should "write create valintaperuste in audit log" in {
    MockAuditLogger.clean()
    val id = put(valintaperuste.withModified(LocalDateTime.parse("1000-01-01T12:00:00")))
    MockAuditLogger.find(id.toString, "valintaperuste_create")
    MockAuditLogger.find("1000-01-01") should not be defined
    get(id, valintaperuste(id))
  }

  it should "store korkeakoulutus valintaperuste" in {
    val id = put(TestData.YoValintaperuste)
    get(id, TestData.YoValintaperuste.copy(id = Some(id)))
  }

  it should "return 401 if no session is found" in {
    put(s"$ValintaperustePath", bytes(valintaperuste)) {
      status should equal (401)
    }
  }

  it should "allow a user of the valintaperuste organization to create the valintaperuste" in {
    put(valintaperuste, crudSessions(valintaperuste.organisaatioOid))
  }

  it should "deny a user without access to the valintaperuste organization" in {
    put(ValintaperustePath, valintaperuste, crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to create the valintaperuste" in {
    put(valintaperuste, crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    put(ValintaperustePath, valintaperuste, crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    put(ValintaperustePath, valintaperuste, readSessions(ChildOid), 403)
  }

  it should "deny indexer access" in {
    put(ValintaperustePath, valintaperuste, indexerSession, 403)
  }

  it should "validate new valintaperuste" in {
    put(ValintaperustePath, bytes(valintaperuste.copy(organisaatioOid = OrganisaatioOid("saippua"))), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(validationMsg("saippua"), "organisaatioOid"))
    }
  }

  it should "validate dates only when adding a new julkaistu valintaperuste" in {
    val ajanjakso = Ajanjakso(alkaa = TestData.inPast(4000), paattyy = Some(TestData.inPast(2000)))
    val tilaisuus = TestData.Valintakoe1.tilaisuudet.head.copy(aika = Some(ajanjakso))
    val koe = TestData.Valintakoe1.copy(tilaisuudet = List(tilaisuus))
    val thisValintaperuste = valintaperuste.copy(valintakokeet = List(koe))

    put(thisValintaperuste.copy(tila = Tallennettu))

    put(ValintaperustePath, bytes(thisValintaperuste.copy(tila = Julkaistu)), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(pastDateMsg(ajanjakso.paattyy.get), "valintakokeet[0].tilaisuudet[0].aika.paattyy"))
    }
  }

  "Update valintaperuste" should "update valintaperuste" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(getIds(valintaperuste(id, Arkistoitu)), lastModified)
    get(id, valintaperuste(id, Arkistoitu))
  }

  it should "read muokkaaja from the session" in {
    val id = put(valintaperuste, crudSessions(ChildOid))
    val userOid = userOidForTestSessionId(crudSessions(ChildOid))
    val lastModified = get(id, valintaperuste(id).copy(muokkaaja = userOid))
    update(getIds(valintaperuste(id, Arkistoitu).copy(muokkaaja = userOid)), lastModified)
    get(id, valintaperuste(id, Arkistoitu).copy(muokkaaja = testUser.oid))
  }

  it should "write valintaperuste update to audit log" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    MockAuditLogger.clean()
    update(getIds(valintaperuste(id, Arkistoitu)), lastModified)
    MockAuditLogger.findFieldChange("tila", "julkaistu", "arkistoitu", id.toString, "valintaperuste_update")
    get(id, valintaperuste(id, Arkistoitu))
  }

  it should "not update valintaperuste" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    MockAuditLogger.clean()
    update(tallennettuValintaperuste(id), lastModified, expectUpdate = false)
    MockAuditLogger.logs shouldBe empty
    get(id, valintaperuste(id)) should equal (lastModified)
  }

  it should "return 401 if no session is found" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    post(ValintaperustePath, bytes(valintaperuste(id)), Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified)) {
      status should equal (401)
    }
  }

  it should "allow a user of the valintaperuste organization to update the valintaperuste" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(tallennettuValintaperuste(id), lastModified, expectUpdate = false, crudSessions(valintaperuste.organisaatioOid))
  }

  it should "deny a user without access to the valintaperuste organization" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(tallennettuValintaperuste(id), lastModified, 403, crudSessions(LonelyOid))
  }

  it should "allow a user of an ancestor organization to create the valintaperuste" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(tallennettuValintaperuste(id), lastModified, expectUpdate = false, crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(tallennettuValintaperuste(id), lastModified, 403, crudSessions(GrandChildOid))
  }

  it should "deny a user with the wrong role" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(tallennettuValintaperuste(id), lastModified, 403, readSessions(valintaperuste.organisaatioOid))
  }

  it should "deny indexer access" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(tallennettuValintaperuste(id), lastModified, 403, indexerSession)
  }

  it should "fail update if 'x-If-Unmodified-Since' header is missing" in {
    val id = put(valintaperuste)
    post(ValintaperustePath, bytes(valintaperuste(id)), defaultHeaders) {
      status should equal (400)
      body should include (KoutaServlet.IfUnmodifiedSinceHeader)
    }
  }

  it should "fail update if modified in between get and update" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    Thread.sleep(1500)
    update(getIds(valintaperuste(id, Arkistoitu)), lastModified)
    post(ValintaperustePath, bytes(valintaperuste(id)), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "update valintakokeet" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    val uusiValintaperuste = valintaperuste(id).copy(
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      valintakokeet = List(TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_42#2"))))
    update(getIds(uusiValintaperuste), lastModified, expectUpdate = true)
    get(id, uusiValintaperuste)
  }

  it should "put, update and delete valintakokeet correctly" in {
    val valintaperusteWithValintakokeet = valintaperuste.copy(
      valintakokeet = Seq(TestData.Valintakoe1, TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_66#6")))
    )
    val id = put(valintaperusteWithValintakokeet)
    val lastModified = get(id, getIds(valintaperusteWithValintakokeet.copy(id = Some(id))))
    val newValintakoe = TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_57#2"))
    val updateValintakoe = getIds(valintaperuste(id)).valintakokeet.head.copy(nimi = Map(Fi -> "Uusi nimi", Sv -> "Uusi nimi på svenska"))
    update(valintaperuste(id).copy(valintakokeet = Seq(newValintakoe, updateValintakoe)), lastModified)
    get(id, getIds(valintaperuste(id).copy(valintakokeet = Seq(newValintakoe, updateValintakoe))))
  }

  it should "delete all valintakokeet and read last modified from history" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    Thread.sleep(1500)
    val uusiValintaperuste = getIds(valintaperuste(id).copy(valintakokeet = List()))
    update(uusiValintaperuste, lastModified, expectUpdate = true)
    get(id, uusiValintaperuste) should not equal lastModified
  }

  it should "store and update unfinished valintaperuste" in {
    val unfinishedValintaperuste = MinYoValintaperuste
    val id = put(unfinishedValintaperuste)
    val lastModified = get(id, unfinishedValintaperuste.copy(id = Some(id)))
    val newUnfinishedValintaperuste = unfinishedValintaperuste.copy(id = Some(id), organisaatioOid = LonelyOid)
    update(newUnfinishedValintaperuste, lastModified)
    get(id, newUnfinishedValintaperuste)
  }

  it should "validate updated valintaperuste" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    post(ValintaperustePath, bytes(valintaperuste(id).copy(organisaatioOid = OrganisaatioOid("saippua"))), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(validationMsg("saippua"), "organisaatioOid"))
    }
  }

  it should "validate dates when moving from other states to julkaistu" in {
    val ajanjakso = Ajanjakso(alkaa = TestData.inPast(4000), paattyy = Some(TestData.inPast(2000)))
    val tilaisuus = TestData.Valintakoe1.tilaisuudet.head.copy(aika = Some(ajanjakso))
    val koe = TestData.Valintakoe1.copy(tilaisuudet = List(tilaisuus))
    val thisValintaperuste = valintaperuste.copy(valintakokeet = List(koe), tila = Tallennettu)

    val id = put(thisValintaperuste)
    val thisValintaperusteWithOid = thisValintaperuste.copy(id = Some(id))
    val lastModified = get(id, thisValintaperusteWithOid)

    post(ValintaperustePath, bytes(thisValintaperusteWithOid.copy(tila = Julkaistu)), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(pastDateMsg(ajanjakso.paattyy.get), "valintakokeet[0].tilaisuudet[0].aika.paattyy"))
    }

    update(getIds(thisValintaperusteWithOid.copy(tila = Poistettu)), lastModified)
  }

  it should "not validate dates when update a julkaistu valintaperuste" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))

    val ajanjakso = Ajanjakso(alkaa = TestData.inPast(4000), paattyy = Some(TestData.inPast(2000)))
    val tilaisuus = TestData.Valintakoe1.tilaisuudet.head.copy(aika = Some(ajanjakso))
    val koe = TestData.Valintakoe1.copy(tilaisuudet = List(tilaisuus))
    val thisValintaperuste = valintaperuste(id).copy(valintakokeet = List(koe), tila = Julkaistu)

    update(thisValintaperuste, lastModified)
  }

  it should "allow oph user to update from julkaistu to tallennettu" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(valintaperuste(id).copy(tila = Tallennettu), lastModified, expectUpdate = true, ophSession)
    get(id, valintaperuste(id).copy(tila = Tallennettu, muokkaaja = OphUserOid, metadata = Some(TestData.AmmValintaperusteMetadata.copy(isMuokkaajaOphVirkailija = Some(true)))))
  }

  it should "not allow non oph user to update from julkaistu to tallennettu" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(valintaperuste(id).copy(tila = Tallennettu), lastModified, 403, crudSessions(valintaperuste.organisaatioOid))
  }

  it should "pass legal state changes" in {
    val id = put(valintaperuste.copy(tila = Tallennettu))
    var lastModified = get(id, valintaperuste(id).copy(tila = Tallennettu))
    update(valintaperuste(id).copy(tila = Julkaistu), lastModified, expectUpdate = true)
    lastModified = get(id, valintaperuste(id).copy(tila = Julkaistu))
    update(valintaperuste(id).copy(tila = Arkistoitu), lastModified, expectUpdate = true)
    lastModified = get(id, valintaperuste(id).copy(tila = Arkistoitu))
    update(valintaperuste(id).copy(tila = Julkaistu), lastModified, expectUpdate = true)
    lastModified = get(id, valintaperuste(id).copy(tila = Julkaistu))

    val tallennettuId = put(valintaperuste.copy(tila = Tallennettu))
    lastModified = get(tallennettuId, valintaperuste(tallennettuId).copy(tila = Tallennettu))
    update(valintaperuste(tallennettuId).copy(tila = Poistettu), lastModified, expectUpdate = true)
  }

  it should "fail illegal state changes" in {
    val tallennettuId = put(valintaperuste.copy(tila = Tallennettu))
    val julkaistuId = put(valintaperuste.copy(tila = Julkaistu))
    val arkistoituId = put(valintaperuste.copy(tila = Arkistoitu))

    var lastModified = get(tallennettuId, valintaperuste(tallennettuId).copy(tila = Tallennettu))
    update(ValintaperustePath, valintaperuste(tallennettuId).copy(tila = Arkistoitu), lastModified, 400, List(ValidationError("tila", illegalStateChange("valintaperusteelle", Tallennettu, Arkistoitu))))
    lastModified = get(julkaistuId, valintaperuste(julkaistuId).copy(tila = Julkaistu))
    update(ValintaperustePath, valintaperuste(julkaistuId).copy(tila = Poistettu), lastModified, 400, List(ValidationError("tila", illegalStateChange("valintaperusteelle", Julkaistu, Poistettu))))
    lastModified = get(arkistoituId, valintaperuste(arkistoituId).copy(tila = Arkistoitu))
    update(ValintaperustePath, valintaperuste(arkistoituId).copy(tila = Tallennettu), lastModified, 400, List(ValidationError("tila", illegalStateChange("valintaperusteelle", Arkistoitu, Tallennettu))))
    update(ValintaperustePath, valintaperuste(arkistoituId).copy(tila = Poistettu), lastModified, 400, List(ValidationError("tila", illegalStateChange("valintaperusteelle", Arkistoitu, Poistettu))))
  }

  private def createValintaperusteWithHakukohteet(markAllHakukohteetDeleted: Boolean) = {
    val koulutusOid = put(koulutus.copy(tila = Tallennettu), ophSession)
    val toteutusOid = put(toteutus(koulutusOid).copy(tila = Tallennettu), ophSession)
    val hakuOid = put(haku.copy(tila = Tallennettu))
    val valintaperusteId = put(valintaperuste.copy(tila = Tallennettu))
    put(hakukohde(toteutusOid, hakuOid, valintaperusteId).copy(tila = Poistettu))
    put(hakukohde(toteutusOid, hakuOid, valintaperusteId).copy(tila = if (markAllHakukohteetDeleted) Poistettu else Tallennettu))
    val lastModified = get(valintaperusteId, valintaperuste(valintaperusteId).copy(tila = Tallennettu))
    (valintaperusteId, lastModified)
  }

  it should "pass deletion when related hakukohteet deleted" in {
    val (valintaperusteId: UUID, lastModified: String) = createValintaperusteWithHakukohteet(true)
    update(valintaperuste(valintaperusteId).copy(tila = Poistettu), lastModified, expectUpdate = true)
  }

  it should "fail deletion when all related hakukohteet not deleted" in {
    val (valintaperusteId: UUID, lastModified: String) = createValintaperusteWithHakukohteet(false)
    update(ValintaperustePath, valintaperuste(valintaperusteId).copy(tila = Poistettu), lastModified, 400, List(ValidationError("tila", integrityViolationMsg("Valintaperustetta", "hakukohteita"))))
  }
}
