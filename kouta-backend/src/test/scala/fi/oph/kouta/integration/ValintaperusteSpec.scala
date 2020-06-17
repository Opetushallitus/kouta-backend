package fi.oph.kouta.integration

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData.MinYoValintaperuste
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.integration.fixture.{SorakuvausFixture, ValintaperusteFixture}
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.validation.Validations._

class ValintaperusteSpec extends KoutaIntegrationSpec with AccessControlSpec with ValintaperusteFixture with SorakuvausFixture {

  override val roleEntities = Seq(Role.Valintaperuste)

  def ophValintaperuste(sorakuvausId: UUID) = valintaperuste(sorakuvausId).copy(julkinen = true, organisaatioOid = OphOid)

  var sorakuvausId: UUID = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    sorakuvausId = put(sorakuvaus)
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
    val id = put(valintaperuste(sorakuvausId))
    get(id, crudSessions(valintaperuste.organisaatioOid), valintaperuste(id, sorakuvausId))
  }

  it should "deny a user without access to the valintaperuste organization" in {
    val id = put(valintaperuste(sorakuvausId))
    get(s"$ValintaperustePath/$id", crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to read the valintaperuste" in {
    val id = put(valintaperuste(sorakuvausId))
    get(id, crudSessions(ParentOid), valintaperuste(id, sorakuvausId))
  }

  it should "allow a user with only access to a descendant organization" in {
    val id = put(valintaperuste(sorakuvausId))
    get(id, crudSessions(GrandChildOid), valintaperuste(id, sorakuvausId))
  }

  it should "deny a user with the wrong role" in {
    val id = put(valintaperuste(sorakuvausId))
    get(s"$ValintaperustePath/$id", otherRoleSession, 403)
  }

  it should "allow the user of proper koulutustyyppi to read julkinen valintaperuste created by oph" in {
    val id = put(ophValintaperuste(sorakuvausId))
    get(id, readSessions(AmmOid), ophValintaperuste(sorakuvausId).copy(id = Some(id)))
  }

  it should "deny the user of wrong koulutustyyppi to read julkinen valintaperuste created by oph" in {
    val id = put(ophValintaperuste(sorakuvausId))
    get(s"$ValintaperustePath/$id", readSessions(YoOid), 403)
  }

  it should "allow indexer access" in {
    val sorakuvausId = put(sorakuvaus)
    val id = put(valintaperuste(sorakuvausId))
    get(id, indexerSession, valintaperuste(id, sorakuvausId))
  }


  "Create valintaperuste" should "store valintaperuste" in {
    val sorakuvausId = put(sorakuvaus)
    val id = put(valintaperuste(sorakuvausId))
    get(id, valintaperuste(id, sorakuvausId))
  }

  it should "read muokkaaja from the session" in {
    val sorakuvausId = put(sorakuvaus)
    val id = put(valintaperuste(sorakuvausId).copy(muokkaaja = UserOid("random")))
    get(id, valintaperuste(id, sorakuvausId).copy(muokkaaja = testUser.oid))
  }

  it should "fail to store valintaperuste if sorakuvaus doesn't exist" in {
    val sorakuvausId = UUID.randomUUID()
    put(ValintaperustePath, valintaperuste(sorakuvausId), 400, "sorakuvausId", nonExistent("Sorakuvausta", sorakuvausId))
  }

  it should "fail to store julkaistu valintaperuste if sorakuvaus is not yet julkaistu" in {
    val sorakuvausId = put(sorakuvaus.copy(tila = Tallennettu))
    put(ValintaperustePath, valintaperuste(sorakuvausId), 400, "tila", notYetJulkaistu("Sorakuvausta", sorakuvausId))
  }

  it should "fail to store valintaperuste if koulutustyyppi doesn't match sorakuvaus koulutustyyppi" in {
    val sorakuvausId = put(TestData.YoSorakuvaus)
    put(ValintaperustePath, valintaperuste(sorakuvausId), 400, "koulutustyyppi", tyyppiMismatch("sorakuvauksen", sorakuvausId))
  }

  it should "write create valintaperuste in audit log" in {
    val sorakuvausId = put(sorakuvaus)
    MockAuditLogger.clean()
    val id = put(valintaperuste(sorakuvausId).withModified(LocalDateTime.parse("1000-01-01T12:00:00")))
    MockAuditLogger.find(id.toString, "valintaperuste_create")
    MockAuditLogger.find("1000-01-01") should not be defined
    get(id, valintaperuste(id, sorakuvausId))
  }

  it should "store korkeakoulutus valintaperuste" in {
    val sorakuvausId = put(TestData.YoSorakuvaus)
    val id = put(TestData.YoValintaperuste.copy(sorakuvausId = Some(sorakuvausId)))
    get(id, TestData.YoValintaperuste.copy(id = Some(id), sorakuvausId = Some(sorakuvausId)))
  }

  it should "return 401 if no session is found" in {
    val sorakuvausId = put(sorakuvaus)
    put(s"$ValintaperustePath", bytes(valintaperuste(sorakuvausId))) {
      status should equal (401)
    }
  }

  it should "allow a user of the valintaperuste organization to create the valintaperuste" in {
    val sorakuvausId = put(sorakuvaus)
    put(valintaperuste(sorakuvausId), crudSessions(valintaperuste.organisaatioOid))
  }

  it should "deny a user without access to the valintaperuste organization" in {
    put(ValintaperustePath, valintaperuste(sorakuvausId), crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to create the valintaperuste" in {
    put(valintaperuste(sorakuvausId), crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    put(ValintaperustePath, valintaperuste(sorakuvausId), crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    put(ValintaperustePath, valintaperuste(sorakuvausId), readSessions(ChildOid), 403)
  }

  it should "deny indexer access" in {
    put(ValintaperustePath, valintaperuste(sorakuvausId), indexerSession, 403)
  }

  it should "validate new valintaperuste" in {
    put(ValintaperustePath, bytes(valintaperuste(sorakuvausId).copy(organisaatioOid = OrganisaatioOid("saippua"))), defaultHeaders) {
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
    val thisValintaperuste = valintaperuste(sorakuvausId).copy(valintakokeet = List(koe))

    put(thisValintaperuste.copy(tila = Tallennettu))

    put(ValintaperustePath, bytes(thisValintaperuste.copy(tila = Julkaistu)), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(pastDateMsg(ajanjakso.paattyy.get), "valintakokeet[0].tilaisuudet[0].aika.paattyy"))
    }
  }

  "Update valintaperuste" should "update valintaperuste" in {
    val id = put(valintaperuste(sorakuvausId))
    val lastModified = get(id, valintaperuste(id, sorakuvausId))
    update(getIds(valintaperuste(id, sorakuvausId, Arkistoitu)), lastModified)
    get(id, valintaperuste(id, sorakuvausId, Arkistoitu))
  }

  it should "read muokkaaja from the session" in {
    val id = put(valintaperuste(sorakuvausId), crudSessions(ChildOid))
    val userOid = userOidForTestSessionId(crudSessions(ChildOid))
    val lastModified = get(id, valintaperuste(id, sorakuvausId).copy(muokkaaja = userOid))
    update(getIds(valintaperuste(id, sorakuvausId, Arkistoitu).copy(muokkaaja = userOid)), lastModified)
    get(id, valintaperuste(id, sorakuvausId, Arkistoitu).copy(muokkaaja = testUser.oid))
  }

  it should "fail to update valintaperuste if sorakuvaus doesn't exist" in {
    val id = put(valintaperuste(sorakuvausId))
    val lastModified = get(id, valintaperuste(id, sorakuvausId))
    val nonExistentSorakuvausId = UUID.randomUUID()
    update(ValintaperustePath, valintaperuste(id, nonExistentSorakuvausId), lastModified, 400, "sorakuvausId", nonExistent("Sorakuvausta", nonExistentSorakuvausId))
  }

  it should "fail to update julkaistu valintaperuste if sorakuvaus is not yet julkaistu" in {
    val sorakuvausId = put(sorakuvaus.copy(tila = Tallennettu))
    put(ValintaperustePath, valintaperuste(sorakuvausId), 400, "tila", notYetJulkaistu("Sorakuvausta", sorakuvausId))
  }

  it should "fail to update valintaperuste if koulutustyyppi doesn't match sorakuvaus koulutustyyppi" in {
    val sorakuvausId = put(TestData.YoSorakuvaus)
    put(ValintaperustePath, valintaperuste(sorakuvausId), 400, "koulutustyyppi", tyyppiMismatch("sorakuvauksen", sorakuvausId))
  }

  it should "write valintaperuste update to audit log" in {
    val id = put(valintaperuste(sorakuvausId))
    val lastModified = get(id, valintaperuste(id, sorakuvausId))
    MockAuditLogger.clean()
    update(getIds(valintaperuste(id, sorakuvausId, Arkistoitu)), lastModified)
    MockAuditLogger.findFieldChange("tila", "julkaistu", "arkistoitu", id.toString, "valintaperuste_update")
    get(id, valintaperuste(id, sorakuvausId, Arkistoitu))
  }

  it should "not update valintaperuste" in {
    val id = put(valintaperuste(sorakuvausId))
    val lastModified = get(id, valintaperuste(id, sorakuvausId))
    MockAuditLogger.clean()
    update(tallennettuValintaperuste(id), lastModified, false)
    MockAuditLogger.logs shouldBe empty
    get(id, valintaperuste(id, sorakuvausId)) should equal (lastModified)
  }

  it should "return 401 if no session is found" in {
    val id = put(valintaperuste(sorakuvausId))
    val lastModified = get(id, valintaperuste(id, sorakuvausId))
    post(ValintaperustePath, bytes(valintaperuste(id, sorakuvausId)), Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified)) {
      status should equal (401)
    }
  }

  it should "allow a user of the valintaperuste organization to update the valintaperuste" in {
    val id = put(valintaperuste(sorakuvausId))
    val lastModified = get(id, valintaperuste(id, sorakuvausId))
    update(tallennettuValintaperuste(id), lastModified, false, crudSessions(valintaperuste.organisaatioOid))
  }

  it should "deny a user without access to the valintaperuste organization" in {
    val id = put(valintaperuste(sorakuvausId))
    val lastModified = get(id, valintaperuste(id, sorakuvausId))
    update(tallennettuValintaperuste(id), lastModified, 403, crudSessions(LonelyOid))
  }

  it should "allow a user of an ancestor organization to create the valintaperuste" in {
    val id = put(valintaperuste(sorakuvausId))
    val lastModified = get(id, valintaperuste(id, sorakuvausId))
    update(tallennettuValintaperuste(id), lastModified, false, crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    val id = put(valintaperuste(sorakuvausId))
    val lastModified = get(id, valintaperuste(id, sorakuvausId))
    update(tallennettuValintaperuste(id), lastModified, 403, crudSessions(GrandChildOid))
  }

  it should "deny a user with the wrong role" in {
    val id = put(valintaperuste(sorakuvausId))
    val lastModified = get(id, valintaperuste(id, sorakuvausId))
    update(tallennettuValintaperuste(id), lastModified, 403, readSessions(valintaperuste(sorakuvausId).organisaatioOid))
  }

  it should "deny indexer access" in {
    val id = put(valintaperuste(sorakuvausId))
    val lastModified = get(id, valintaperuste(id, sorakuvausId))
    update(tallennettuValintaperuste(id), lastModified, 403, indexerSession)
  }

  it should "fail update if 'x-If-Unmodified-Since' header is missing" in {
    val id = put(valintaperuste(sorakuvausId))
    post(ValintaperustePath, bytes(valintaperuste(id, sorakuvausId)), defaultHeaders) {
      status should equal (400)
      body should include (KoutaServlet.IfUnmodifiedSinceHeader)
    }
  }

  it should "fail update if modified in between get and update" in {
    val id = put(valintaperuste(sorakuvausId))
    val lastModified = get(id, valintaperuste(id, sorakuvausId))
    Thread.sleep(1500)
    update(getIds(valintaperuste(id, sorakuvausId, Arkistoitu)), lastModified)
    post(ValintaperustePath, bytes(valintaperuste(id, sorakuvausId)), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "update valintakokeet" in {
    val id = put(valintaperuste(sorakuvausId))
    val lastModified = get(id, valintaperuste(id, sorakuvausId))
    val uusiValintaperuste = valintaperuste(id, sorakuvausId).copy(
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      valintakokeet = List(TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_42#2"))))
    update(getIds(uusiValintaperuste), lastModified, expectUpdate = true)
    get(id, uusiValintaperuste)
  }

  it should "put, update and delete valintakokeet correctly" in {
    val valintaperusteWithValintakokeet = valintaperuste(sorakuvausId).copy(
      valintakokeet = Seq(TestData.Valintakoe1, TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_66#6")))
    )
    val id = put(valintaperusteWithValintakokeet)
    val lastModified = get(id, getIds(valintaperusteWithValintakokeet.copy(id = Some(id))))
    val newValintakoe = TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("valintakokeentyyppi_57#2"))
    val updateValintakoe = (getIds(valintaperuste(id, sorakuvausId))).valintakokeet.head.copy(nimi = Map(Fi -> "Uusi nimi", Sv -> "Uusi nimi på svenska"))
    update(valintaperuste(id, sorakuvausId).copy(valintakokeet = Seq(newValintakoe, updateValintakoe)), lastModified)
    get(id, getIds(valintaperuste(id, sorakuvausId).copy(valintakokeet = Seq(newValintakoe, updateValintakoe))))
  }

  it should "delete all valintakokeet and read last modified from history" in {
    val id = put(valintaperuste(sorakuvausId))
    val lastModified = get(id, valintaperuste(id, sorakuvausId))
    Thread.sleep(1500)
    val uusiValintaperuste = getIds(valintaperuste(id, sorakuvausId).copy(valintakokeet = List()))
    update(uusiValintaperuste, lastModified, expectUpdate = true)
    get(id, uusiValintaperuste) should not equal (lastModified)
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
    val id = put(valintaperuste(sorakuvausId))
    val lastModified = get(id, valintaperuste(id, sorakuvausId))
    post(ValintaperustePath, bytes(valintaperuste(id, sorakuvausId).copy(organisaatioOid = OrganisaatioOid("saippua"))), headersIfUnmodifiedSince(lastModified)) {
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
    val thisValintaperuste = valintaperuste(sorakuvausId).copy(valintakokeet = List(koe), tila = Tallennettu)

    val id = put(thisValintaperuste)
    val thisValintaperusteWithOid = thisValintaperuste.copy(id = Some(id))
    val lastModified = get(id, thisValintaperusteWithOid)

    post(ValintaperustePath, bytes(thisValintaperusteWithOid.copy(tila = Julkaistu)), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal(validationErrorBody(pastDateMsg(ajanjakso.paattyy.get), "valintakokeet[0].tilaisuudet[0].aika.paattyy"))
    }

    update(getIds(thisValintaperusteWithOid.copy(tila = Arkistoitu)), lastModified)
  }

  it should "not validate dates when update a julkaistu valintaperuste" in {
    val id = put(valintaperuste(sorakuvausId))
    val lastModified = get(id, valintaperuste(id, sorakuvausId))

    val ajanjakso = Ajanjakso(alkaa = TestData.inPast(4000), paattyy = Some(TestData.inPast(2000)))
    val tilaisuus = TestData.Valintakoe1.tilaisuudet.head.copy(aika = Some(ajanjakso))
    val koe = TestData.Valintakoe1.copy(tilaisuudet = List(tilaisuus))
    val thisValintaperuste = valintaperuste(id, sorakuvausId).copy(valintakokeet = List(koe), tila = Julkaistu)

    update(thisValintaperuste, lastModified)
  }
}
