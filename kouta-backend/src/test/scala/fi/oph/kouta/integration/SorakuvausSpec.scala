package fi.oph.kouta.integration

import java.time.LocalDateTime
import java.util.UUID
import fi.oph.kouta.TestData
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain.{Arkistoitu, Julkaistu, Poistettu, Tallennettu}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.integration.fixture.{KoulutusFixture, SorakuvausFixture}
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.validation.ValidationError
import fi.oph.kouta.validation.Validations._

class SorakuvausSpec extends KoutaIntegrationSpec with AccessControlSpec with SorakuvausFixture with KoulutusFixture {

  override val roleEntities = Seq(Role.Valintaperuste)

  def OphHeaders: Seq[(String, String)] = Seq(sessionHeader(ophSession), jsonHeader)

  "Get sorakuvaus by id" should "return 404 if sorakuvaus not found" in {
    get(s"/sorakuvaus/${UUID.randomUUID()}", headers = defaultHeaders) {
      status should equal (404)
      body should include ("Unknown SORA-kuvaus id")
    }
  }

  it should "return 401 if no session is found" in {
    get(s"/sorakuvaus/${UUID.randomUUID()}") {
      status should equal(401)
    }
  }

  it should "allow a user of the non oph organization to read the sorakuvaus 1" in {
    val id = put(sorakuvaus)
    get(id, crudSessions(ChildOid), sorakuvaus(id))
  }

  it should "allow a user of the non oph organization to read the sorakuvaus 2" in {
    val id = put(sorakuvaus)
    get(id, crudSessions(LonelyOid), sorakuvaus(id))
  }

  it should "allow a user of the non oph organization to read the sorakuvaus 3" in {
    val id = put(sorakuvaus)
    get(id, crudSessions(ParentOid), sorakuvaus(id))
  }

  it should "allow a user of the non oph organization to read the sorakuvaus 4" in {
    val id = put(sorakuvaus)
    get(id, crudSessions(GrandChildOid), sorakuvaus(id))
  }

  it should "allow a user to read only sorakuvaus with correct koulutustyyppi" in {
    val ammId = put(sorakuvaus)
    val yoId = put(yoSorakuvaus, ophSession)
    get(s"$SorakuvausPath/$ammId", crudSessions(YoOid), 403)
    get(yoId, crudSessions(YoOid), yoSorakuvaus.copy(id = Some(yoId)))
  }

  it should "deny a user with the wrong role" in {
    val id = put(sorakuvaus)
    get(s"$SorakuvausPath/$id", otherRoleSession, 403)
  }

  it should "allow indexer access" in {
    val id = put(sorakuvaus)
    get(id, indexerSession, sorakuvaus(id))
  }

  it should "return error when trying to get deleted sorakuvaus" in {
    val oid = put(sorakuvaus.copy(tila = Poistettu), ophSession)
    get(s"$SorakuvausPath/$oid", ophSession, 404)
  }

  it should "return ok when getting deleted sorakuvaus with myosPoistetut = true" in {
    val oid = put(sorakuvaus.copy(tila = Poistettu), ophSession)
    get(s"$SorakuvausPath/$oid?myosPoistetut=true", ophSession, 200)
  }

  "Create sorakuvaus" should "allow oph user to store sorakuvaus" in {
    val id = put(sorakuvaus)
    get(id, sorakuvaus(id).copy(muokkaaja = OphUserOid))
  }

  it should "allow oph user to store korkeakoulutus sorakuvaus" in {
    val id = put(TestData.YoSorakuvaus)
    get(id, TestData.YoSorakuvaus.copy(id = Some(id), muokkaaja = OphUserOid))
  }

  it should "read muokkaaja from the session" in {
    val oid = put(sorakuvaus.copy(muokkaaja = UserOid("random")))
    get(oid, sorakuvaus(oid).copy(muokkaaja = OphUserOid))
  }

  it should "write create haku to audit log" in {
    MockAuditLogger.clean()
    val id = put(sorakuvaus.withModified(LocalDateTime.parse("1000-01-01T12:00:00")))
    MockAuditLogger.find(id.toString, "sorakuvaus_create") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "return 401 if no session is found" in {
    put(s"$SorakuvausPath", bytes(sorakuvaus)) {
      status should equal (401)
    }
  }

  it should "deny non oph user to create sorakuvaus 1" in {
    put(SorakuvausPath, sorakuvaus, defaultSessionId, 403)
  }

  it should "deny non oph user to create the sorakuvaus 2" in {
    put(SorakuvausPath, sorakuvaus, crudSessions(ChildOid), 403)
  }

  it should "deny non oph user to create the sorakuvaus 3" in {
    put(SorakuvausPath, sorakuvaus, crudSessions(LonelyOid), 403)
  }

  it should "deny non oph user to create the sorakuvaus 4" in {
    put(SorakuvausPath, sorakuvaus, crudSessions(ParentOid), 403)
  }

  it should "deny non oph user to create the sorakuvaus 5" in {
    put(SorakuvausPath, sorakuvaus, crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    put(SorakuvausPath, sorakuvaus, readSessions(ChildOid), 403)
  }

  it should "deny indexer access" in {
    put(SorakuvausPath, sorakuvaus, indexerSession, 403)
  }

  it should "validate new sorakuvaus" in {
    put(SorakuvausPath, bytes(sorakuvaus.copy(organisaatioOid = OrganisaatioOid("saippua"))), OphHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(validationMsg("saippua"), "organisaatioOid"))
    }
  }

  "Update sorakuvaus" should "allow oph user to update sorakuvaus" in {
    val id = put(sorakuvaus)
    val lastModified = get(id, sorakuvaus(id))
    update(sorakuvaus(id, Arkistoitu), lastModified)
    get(id, sorakuvaus(id, Arkistoitu))
  }

  it should "read muokkaaja from the session" in {
    val oid = put(sorakuvaus)
    val userOid = userOidForTestSessionId(crudSessions(ChildOid))
    val lastModified = get(oid, sorakuvaus(oid))
    update(sorakuvaus(oid, Arkistoitu).copy(muokkaaja = userOid), lastModified)
    get(oid, sorakuvaus(oid, Arkistoitu))
  }

  it should "write sorakuvaus update to audit log" in {
    val id = put(sorakuvaus)
    val lastModified = get(id, sorakuvaus(id))
    MockAuditLogger.clean()
    update(sorakuvaus(id, Arkistoitu).withModified(LocalDateTime.parse("1000-01-01T12:00:00")), lastModified)
    MockAuditLogger.findFieldChange("tila", "julkaistu", "arkistoitu", id.toString, "sorakuvaus_update") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "not update sorakuvaus" in {
    val id = put(sorakuvaus)
    val lastModified = get(id, sorakuvaus(id))
    MockAuditLogger.clean()
    update(sorakuvaus(id), lastModified, false)
    MockAuditLogger.logs shouldBe empty
    get(id, sorakuvaus(id)) should equal (lastModified)
  }

  it should "return 401 if no session is found" in {
    val id = put(sorakuvaus)
    val lastModified = get(id, sorakuvaus(id))
    post(SorakuvausPath, bytes(sorakuvaus(id)), Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified)) {
      status should equal (401)
    }
  }

  it should "deny non oph user to update sorakuvaus 1" in {
    val id = put(sorakuvaus)
    val thisSorakuvaus = sorakuvaus(id)
    val lastModified = get(id, thisSorakuvaus)
    update(thisSorakuvaus, lastModified, 403, defaultSessionId)
  }

  it should "deny non oph user to update sorakuvaus 2" in {
    val id = put(sorakuvaus)
    val thisSorakuvaus = sorakuvaus(id)
    val lastModified = get(id, thisSorakuvaus)
    update(thisSorakuvaus, lastModified, 403, crudSessions(ChildOid))
  }

  it should "deny non oph user to update sorakuvaus 3" in {
    val id = put(sorakuvaus)
    val thisSorakuvaus = sorakuvaus(id)
    val lastModified = get(id, thisSorakuvaus)
    update(thisSorakuvaus, lastModified, 403, crudSessions(LonelyOid))
  }

  it should "deny non oph user to update sorakuvaus 4" in {
    val id = put(sorakuvaus)
    val thisSorakuvaus = sorakuvaus(id)
    val lastModified = get(id, thisSorakuvaus)
    update(thisSorakuvaus, lastModified, 403, crudSessions(ParentOid))
  }

  it should "deny non oph user to update sorakuvaus 5" in {
    val id = put(sorakuvaus)
    val thisSorakuvaus = sorakuvaus(id)
    val lastModified = get(id, thisSorakuvaus)
    update(thisSorakuvaus, lastModified, 403, crudSessions(GrandChildOid))
  }

  it should "deny a user with the wrong role" in {
    val id = put(sorakuvaus)
    val thisSorakuvaus = sorakuvaus(id)
    val lastModified = get(id, thisSorakuvaus)
    update(thisSorakuvaus, lastModified, 403, readSessions(ChildOid))
  }

  it should "deny indexer access" in {
    val id = put(sorakuvaus)
    val thisSorakuvaus = sorakuvaus(id)
    val lastModified = get(id, thisSorakuvaus)
    update(thisSorakuvaus, lastModified, 403, indexerSession)
  }

  it should "fail update if 'x-If-Unmodified-Since' header is missing" in {
    val id = put(sorakuvaus)
    post(SorakuvausPath, bytes(sorakuvaus(id)), OphHeaders) {
      status should equal (400)
      body should include (KoutaServlet.IfUnmodifiedSinceHeader)
    }
  }

  it should "fail update if modified in between get and update" in {
    val id = put(sorakuvaus)
    val lastModified = get(id, sorakuvaus(id))
    Thread.sleep(1500)
    update(sorakuvaus(id, Arkistoitu), lastModified, ophSession)
    post(SorakuvausPath, bytes(sorakuvaus(id)), headersIfUnmodifiedSince(lastModified, sessionHeader(ophSession))) {
      status should equal (409)
    }
  }

  it should "store and update unfinished sorakuvaus" in {
    val unfinishedSorakuvaus = TestData.MinSorakuvaus
    val id = put(unfinishedSorakuvaus, ophSession)
    val lastModified = get(id, unfinishedSorakuvaus.copy(id = Some(id), muokkaaja = OphUserOid))
    val newUnfinishedSorakuvaus = unfinishedSorakuvaus.copy(id = Some(id), organisaatioOid = LonelyOid, muokkaaja = OphUserOid)
    update(newUnfinishedSorakuvaus, lastModified, ophSession)
    get(id, newUnfinishedSorakuvaus)
  }

  it should "validate updated sorakuvaus" in {
    val id = put(sorakuvaus)
    val lastModified = get(id, sorakuvaus(id))
    post(SorakuvausPath, bytes(sorakuvaus(id).copy(organisaatioOid = OrganisaatioOid("saippua"))), headersIfUnmodifiedSince(lastModified, sessionHeader(ophSession))) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(validationMsg("saippua"), "organisaatioOid"))
    }
  }

  it should "pass legal state changes" in {
    val id = put(sorakuvaus.copy(tila = Tallennettu), ophSession)
    var lastModified = get(id, sorakuvaus(id).copy(tila = Tallennettu))
    update(sorakuvaus(id).copy(tila = Julkaistu), lastModified, true, ophSession)
    lastModified = get(id, sorakuvaus(id).copy(tila = Julkaistu))
    update(sorakuvaus(id).copy(tila = Arkistoitu), lastModified, true, ophSession)
    lastModified = get(id, sorakuvaus(id).copy(tila = Arkistoitu))
    update(sorakuvaus(id).copy(tila = Julkaistu), lastModified, true, ophSession)
    lastModified = get(id, sorakuvaus(id).copy(tila = Julkaistu))
    update(sorakuvaus(id).copy(tila = Tallennettu), lastModified, true, ophSession)
    lastModified = get(id, sorakuvaus(id).copy(tila = Tallennettu))
    update(sorakuvaus(id).copy(tila = Poistettu), lastModified, true, ophSession)

    val arkistoituId = put(sorakuvaus.copy(tila = Arkistoitu), ophSession)
    lastModified = get(arkistoituId, sorakuvaus(arkistoituId).copy(tila = Arkistoitu))
    update(sorakuvaus(arkistoituId).copy(tila = Julkaistu), lastModified, true, ophSession)
    get(arkistoituId, sorakuvaus(arkistoituId).copy(tila = Julkaistu))
  }

  it should "fail illegal state changes" in {
    val tallennettuId = put(sorakuvaus.copy(tila = Tallennettu), ophSession)
    val julkaistuId = put(sorakuvaus.copy(tila = Julkaistu), ophSession)
    val arkistoituId = put(sorakuvaus.copy(tila = Arkistoitu), ophSession)

    var lastModified = get(tallennettuId, sorakuvaus(tallennettuId).copy(tila = Tallennettu))
    update(SorakuvausPath, sorakuvaus(tallennettuId).copy(tila = Arkistoitu), ophSession, lastModified, 400, List(ValidationError("tila", illegalStateChange("sorakuvaukselle", Tallennettu, Arkistoitu))))
    lastModified = get(julkaistuId, sorakuvaus(julkaistuId).copy(tila = Julkaistu))
    update(SorakuvausPath, sorakuvaus(julkaistuId).copy(tila = Poistettu), ophSession, lastModified, 400, List(ValidationError("tila", illegalStateChange("sorakuvaukselle", Julkaistu, Poistettu))))
    lastModified = get(arkistoituId, sorakuvaus(arkistoituId).copy(tila = Arkistoitu))
    update(SorakuvausPath, sorakuvaus(arkistoituId).copy(tila = Poistettu), ophSession, lastModified, 400, List(ValidationError("tila", illegalStateChange("sorakuvaukselle", Arkistoitu, Poistettu))))
  }

  private def createSorakuvausWithKoulutukset(markAllKoulutuksetDeleted: Boolean) = {
    val sorakuvausId = put(sorakuvaus.copy(tila = Tallennettu), ophSession)
    put(koulutus.copy(sorakuvausId = Some(sorakuvausId), tila = Poistettu), ophSession)
    put(koulutus.copy(sorakuvausId = Some(sorakuvausId), tila = if (markAllKoulutuksetDeleted) Poistettu else Tallennettu), ophSession)
    val lastModified = get(sorakuvausId, sorakuvaus(sorakuvausId).copy(tila = Tallennettu))
    (sorakuvausId, lastModified)
  }

  it should "pass deletion when related koulutukset deleted" in {
    val (sorakuvausId: UUID, lastModified: String) = createSorakuvausWithKoulutukset(true)
    update(sorakuvaus(sorakuvausId).copy(tila = Poistettu), lastModified, true, ophSession)
  }

  it should "fail deletion when all related hakukohteet not deleted" in {
    val (sorakuvausId: UUID, lastModified: String) = createSorakuvausWithKoulutukset(false)
    update(SorakuvausPath, sorakuvaus(sorakuvausId).copy(tila = Poistettu), ophSession, lastModified, 400, List(ValidationError("tila", integrityViolationMsg("Sorakuvausta", "koulutuksia"))))
  }

}
