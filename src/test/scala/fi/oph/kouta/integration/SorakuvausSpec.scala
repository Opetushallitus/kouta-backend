package fi.oph.kouta.integration

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.TestData
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain.Arkistoitu
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.integration.fixture.SorakuvausFixture
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.validation.Validations._

class SorakuvausSpec extends KoutaIntegrationSpec with AccessControlSpec with SorakuvausFixture {

  override val roleEntities = Seq(Role.Valintaperuste)

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

  it should "allow a user of the sorakuvaus organization to read the sorakuvaus" in {
    val id = put(sorakuvaus)
    get(id, crudSessions(sorakuvaus.organisaatioOid), sorakuvaus(id))
  }

  it should "deny a user without access to the sorakuvaus organization" in {
    val id = put(sorakuvaus)
    get(s"$SorakuvausPath/$id", crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to read the sorakuvaus" in {
    val id = put(sorakuvaus)
    get(id, crudSessions(ParentOid), sorakuvaus(id))
  }

  it should "allow a user with only access to a descendant organization" in {
    val id = put(sorakuvaus)
    get(id, crudSessions(GrandChildOid), sorakuvaus(id))
  }

  it should "deny a user with the wrong role" in {
    val id = put(sorakuvaus)
    get(s"$SorakuvausPath/$id", otherRoleSession, 403)
  }

  it should "allow indexer access" in {
    val id = put(sorakuvaus)
    get(id, indexerSession, sorakuvaus(id))
  }


  "Create sorakuvaus" should "store sorakuvaus" in {
    val id = put(sorakuvaus)
    get(id, sorakuvaus(id))
  }

  it should "store korkeakoulutus sorakuvaus" in {
    val id = put(TestData.YoSorakuvaus)
    get(id, TestData.YoSorakuvaus.copy(id = Some(id)))
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

  it should "allow a user of the sorakuvaus organization to create the sorakuvaus" in {
    put(sorakuvaus, crudSessions(sorakuvaus.organisaatioOid))
  }

  it should "deny a user without access to the sorakuvaus organization" in {
    put(SorakuvausPath, sorakuvaus, crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to create the sorakuvaus" in {
    put(sorakuvaus, crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    put(SorakuvausPath, sorakuvaus, crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    put(SorakuvausPath, sorakuvaus, readSessions(ChildOid), 403)
  }

  it should "deny indexer access" in {
    put(SorakuvausPath, sorakuvaus, indexerSession, 403)
  }

  it should "validate new sorakuvaus" in {
    put(SorakuvausPath, bytes(sorakuvaus.copy(organisaatioOid = OrganisaatioOid("saippua"))), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(validationMsg("saippua"), "organisaatioOid"))
    }
  }

  "Update sorakuvaus" should "update sorakuvaus" in {
    val id = put(sorakuvaus)
    val lastModified = get(id, sorakuvaus(id))
    update(sorakuvaus(id, Arkistoitu), lastModified)
    get(id, sorakuvaus(id, Arkistoitu))
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

  it should "allow a user of the sorakuvaus organization to update the sorakuvaus" in {
    val id = put(sorakuvaus)
    val thisSorakuvaus = sorakuvaus(id)
    val lastModified = get(id, thisSorakuvaus)
    update(thisSorakuvaus, lastModified, false, crudSessions(sorakuvaus.organisaatioOid))
  }

  it should "deny a user without access to the sorakuvaus organization" in {
    val id = put(sorakuvaus)
    val thisSorakuvaus = sorakuvaus(id)
    val lastModified = get(id, thisSorakuvaus)
    update(thisSorakuvaus, lastModified, 403, crudSessions(LonelyOid))
  }

  it should "allow a user of an ancestor organization to create the sorakuvaus" in {
    val id = put(sorakuvaus)
    val thisSorakuvaus = sorakuvaus(id)
    val lastModified = get(id, thisSorakuvaus)
    update(thisSorakuvaus, lastModified, false, crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    val id = put(sorakuvaus)
    val thisSorakuvaus = sorakuvaus(id)
    val lastModified = get(id, thisSorakuvaus)
    update(thisSorakuvaus, lastModified, 403, crudSessions(GrandChildOid))
  }

  it should "deny a user with the wrong role" in {
    val id = put(sorakuvaus)
    val thisSorakuvaus = sorakuvaus(id)
    val lastModified = get(id, thisSorakuvaus)
    update(thisSorakuvaus, lastModified, 403, readSessions(sorakuvaus.organisaatioOid))
  }

  it should "deny indexer access" in {
    val id = put(sorakuvaus)
    val thisSorakuvaus = sorakuvaus(id)
    val lastModified = get(id, thisSorakuvaus)
    update(thisSorakuvaus, lastModified, 403, indexerSession)
  }

  it should "fail update if 'x-If-Unmodified-Since' header is missing" in {
    val id = put(sorakuvaus)
    post(SorakuvausPath, bytes(sorakuvaus(id)), defaultHeaders) {
      status should equal (400)
      body should include (KoutaServlet.IfUnmodifiedSinceHeader)
    }
  }

  it should "fail update if modified in between get and update" in {
    val id = put(sorakuvaus)
    val lastModified = get(id, sorakuvaus(id))
    Thread.sleep(1500)
    update(sorakuvaus(id, Arkistoitu), lastModified)
    post(SorakuvausPath, bytes(sorakuvaus(id)), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "store and update unfinished sorakuvaus" in {
    val unfinishedSorakuvaus = TestData.MinSorakuvaus
    val id = put(unfinishedSorakuvaus)
    val lastModified = get(id, unfinishedSorakuvaus.copy(id = Some(id)))
    val newUnfinishedSorakuvaus = unfinishedSorakuvaus.copy(id = Some(id), organisaatioOid = LonelyOid)
    update(newUnfinishedSorakuvaus, lastModified)
    get(id, newUnfinishedSorakuvaus)
  }

  it should "validate updated sorakuvaus" in {
    val id = put(sorakuvaus)
    val lastModified = get(id, sorakuvaus(id))
    post(SorakuvausPath, bytes(sorakuvaus(id).copy(organisaatioOid = OrganisaatioOid("saippua"))), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(validationMsg("saippua"), "organisaatioOid"))
    }
  }
}
