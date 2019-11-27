package fi.oph.kouta.integration

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData.MinYoValintaperuste
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.integration.fixture.ValintaperusteFixture
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.validation.Validations

class ValintaperusteSpec extends KoutaIntegrationSpec with AccessControlSpec with ValintaperusteFixture with Validations {

  override val roleEntities = Seq(Role.Valintaperuste)

  val ophValintaperuste = valintaperuste.copy(julkinen = true, organisaatioOid = rootOrganisaatio)

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


  "Create valintaperuste" should "store valintaperuste" in {
    val id = put(valintaperuste)
    get(id, valintaperuste(id))
  }

  it should "write create valintaperuste in log" in {
    MockAuditLogger.clean()
    val id = put(valintaperuste.withModified(LocalDateTime.parse("1000-01-01T00:00:00")))
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
      body should equal (validateErrorBody(validationMsg("saippua")))
    }
  }

  "Update valintaperuste" should "update valintaperuste" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(valintaperuste(id, Arkistoitu), lastModified)
    get(id, valintaperuste(id, Arkistoitu))
  }

  it should "write valintaperuste update to audit log" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    MockAuditLogger.clean()
    update(valintaperuste(id, Arkistoitu), lastModified)
    MockAuditLogger.findFieldChange("tila", "julkaistu", "arkistoitu", id.toString, "valintaperuste_update")
    get(id, valintaperuste(id, Arkistoitu))
  }

  it should "not update valintaperuste" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(tallennettuValintaperuste(id), lastModified, false)
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
    update(tallennettuValintaperuste(id), lastModified, false, crudSessions(valintaperuste.organisaatioOid))
  }

  it should "deny a user without access to the valintaperuste organization" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(tallennettuValintaperuste(id), lastModified, 403, crudSessions(LonelyOid))
  }

  it should "allow a user of an ancestor organization to create the valintaperuste" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(tallennettuValintaperuste(id), lastModified, false, crudSessions(ParentOid))
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
    update(valintaperuste(id, Arkistoitu), lastModified)
    post(ValintaperustePath, bytes(valintaperuste(id)), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "update valintakokeet" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    val uusiValintaperuste = valintaperuste(id).copy(
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      valintakokeet = List(TestData.Valintakoe1.copy(tyyppiKoodiUri = Some("tyyyyppi_1#2"))))
    update(getIds(uusiValintaperuste), lastModified, expectUpdate = true)
    get(id, uusiValintaperuste)
  }

  it should "delete all valintakokeet and read last modified from history" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    Thread.sleep(1500)
    val uusiValintaperuste = valintaperuste(id).copy(valintakokeet = List())
    update(uusiValintaperuste, lastModified, expectUpdate = true)
    get(id, uusiValintaperuste) should not equal (lastModified)
  }

  it should "store and update unfinished valintaperuste" in {
    val unfinishedValintaperuste = MinYoValintaperuste
    val id = put(unfinishedValintaperuste)
    val lastModified = get(id, unfinishedValintaperuste.copy(id = Some(id)))
    val newUnfinishedValintaperuste = unfinishedValintaperuste.copy(id = Some(id), organisaatioOid = OrganisaatioOid("6.6.6.6.6"))
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
      body should equal (validateErrorBody(validationMsg("saippua")))
    }
  }
}
