package fi.oph.kouta.integration

import java.util.UUID

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData.MinYoValintaperuste
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.integration.fixture.ValintaperusteFixture
import fi.oph.kouta.security.Role
import fi.oph.kouta.validation.Validations

class ValintaperusteSpec extends KoutaIntegrationSpec with AccessControlSpec with ValintaperusteFixture with Validations {

  override def beforeAll(): Unit = {
    super.beforeAll()
    startServiceMocking()
    addTestSessions(Role.Valintaperuste)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    stopServiceMocking()
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

  it should "deny a user with only access to a descendant organization" in {
    val id = put(valintaperuste)
    get(s"$ValintaperustePath/$id", crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    val id = put(valintaperuste)
    get(s"$ValintaperustePath/$id", otherRoleSession, 403)
  }

  it should "allow indexer access" in {
    val id = put(valintaperuste)
    get(id, indexerSession, valintaperuste(id))
  }


  "Create valintaperuste" should "store valintaperuste" in {
    val id = put(valintaperuste)
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

  it should "not update valintaperuste" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    update(valintaperuste(id), lastModified, false)
    get(id, valintaperuste(id)) should equal (lastModified)
  }

  it should "return 401 if no session is found" in {
    val id = put(valintaperuste)
    val lastModified = get(id, valintaperuste(id))
    post(ValintaperustePath, bytes(valintaperuste(id)), Seq("If-Unmodified-Since" -> lastModified)) {
      status should equal (401)
    }
  }

  it should "allow a user of the valintaperuste organization to update the valintaperuste" in {
    val oid = put(valintaperuste)
    val thisValintaperuste = valintaperuste(oid)
    val lastModified = get(oid, thisValintaperuste)
    update(thisValintaperuste, lastModified, false, crudSessions(valintaperuste.organisaatioOid))
  }

  it should "deny a user without access to the valintaperuste organization" in {
    val oid = put(valintaperuste)
    val thisValintaperuste = valintaperuste(oid)
    val lastModified = get(oid, thisValintaperuste)
    update(thisValintaperuste, lastModified, 403, crudSessions(LonelyOid))
  }

  it should "allow a user of an ancestor organization to create the valintaperuste" in {
    val oid = put(valintaperuste)
    val thisValintaperuste = valintaperuste(oid)
    val lastModified = get(oid, thisValintaperuste)
    update(thisValintaperuste, lastModified, false, crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    val oid = put(valintaperuste)
    val thisValintaperuste = valintaperuste(oid)
    val lastModified = get(oid, thisValintaperuste)
    update(thisValintaperuste, lastModified, 403, crudSessions(GrandChildOid))
  }

  it should "deny a user with the wrong role" in {
    val oid = put(valintaperuste)
    val thisValintaperuste = valintaperuste(oid)
    val lastModified = get(oid, thisValintaperuste)
    update(thisValintaperuste, lastModified, 403, readSessions(valintaperuste.organisaatioOid))
  }

  it should "deny indexer access" in {
    val oid = put(valintaperuste)
    val thisValintaperuste = valintaperuste(oid)
    val lastModified = get(oid, thisValintaperuste)
    update(thisValintaperuste, lastModified, 403, indexerSession)
  }

  it should "fail update if 'If-Unmodified-Since' header is missing" in {
    val id = put(valintaperuste)
    post(ValintaperustePath, bytes(valintaperuste(id)), defaultHeaders) {
      status should equal (400)
      body should include ("If-Unmodified-Since")
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
