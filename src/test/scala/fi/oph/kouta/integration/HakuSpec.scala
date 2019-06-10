package fi.oph.kouta.integration

import fi.oph.kouta.TestData
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.fixture.HakuFixture
import fi.oph.kouta.security.Role
import fi.oph.kouta.validation.Validations

class HakuSpec extends KoutaIntegrationSpec with AccessControlSpec with HakuFixture with Validations {

  override def beforeAll(): Unit = {
    super.beforeAll()
    startServiceMocking()
    addTestSessions(Role.Haku)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    stopServiceMocking()
  }

  def addInvalidHakuaika(haku:Haku) = haku.copy(
    hakuajat = List(Ajanjakso(TestData.inFuture(9000), TestData.inFuture(3000))))

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

  it should "deny a user with only access to a descendant organization" in {
    val oid = put(haku)
    get(s"$HakuPath/$oid", crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    val oid = put(haku)
    get(s"$HakuPath/$oid", otherRoleSession, 403)
  }

  it should "allow indexer access" in {
    val oid = put(haku)
    get(oid, indexerSession, haku(oid))
  }

  "Create haku" should "store haku" in {
    val oid = put(haku)
    get(oid, haku(oid))
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
    put(HakuPath, bytes(addInvalidHakuaika(haku)), Seq(jsonHeader, defaultSessionHeader)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validateErrorBody(InvalidHakuaika))
    }
  }

  "Update haku" should "update haku" in {
    val oid = put(haku)
    val lastModified = get(oid, haku(oid))
    update(haku(oid, Arkistoitu), lastModified)
    get(oid, haku(oid, Arkistoitu))
  }

  it should "not update haku" in {
    val oid = put(haku)
    val lastModified = get(oid, haku(oid))
    update(haku(oid), lastModified, false)
    get(oid, haku(oid)) should equal (lastModified)
  }

  it should "fail update if 'If-Unmodified-Since' header is missing" in {
    val oid = put(haku)
    val lastModified = get(oid, haku(oid))
    post(HakuPath, bytes(haku(oid)), Seq(defaultSessionHeader)) {
      status should equal (400)
      body should include ("If-Unmodified-Since")
    }
  }

  it should "return 401 without a valid session" in {
    val oid = put(haku)
    val lastModified = get(oid, haku(oid))
    post(HakuPath, bytes(haku(oid)), Map.empty) {
      status should equal (401)
      body should include ("Unauthorized")
    }
  }

  it should "allow a user of the haku organization to update the haku" in {
    val oid = put(haku)
    val thisHaku = haku(oid)
    val lastModified = get(oid, thisHaku)
    update(thisHaku, lastModified, false, crudSessions(haku.organisaatioOid))
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
    update(thisHaku, lastModified, false, crudSessions(ParentOid))
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
    val lastModified = get(oid, haku(oid))
    Thread.sleep(1500)
    update(haku(oid, Arkistoitu), lastModified)
    post(HakuPath, bytes(haku(oid)), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "update haun tekstit ja hakuajat" in {
    val oid = put(haku)
    val lastModified = get(oid, haku(oid))
    val uusiHaku = haku(oid).copy(
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      hakulomaketyyppi = Some(Ataru),
      hakulomake = Map(Fi -> "http://ataru/kivahakulomake", Sv -> "http://ataru/kivahakulomake/sv", En -> "http://ataru/kivahakulomake/en"),
      metadata = Some(new HakuMetadata(Some(TestData.Yhteystieto1))),
      hakuajat = List(Ajanjakso(alkaa = TestData.now(), paattyy = TestData.inFuture(12000))))
    update(uusiHaku, lastModified, true)
    get(oid, uusiHaku)
  }

  it should "delete all hakuajat and read last modified from history" in {
    val oid = put(haku)
    val lastModified = get(oid, haku(oid))
    Thread.sleep(1500)
    val uusiHaku = haku(oid).copy(hakuajat = List())
    update(uusiHaku, lastModified, true)
    get(oid, uusiHaku) should not equal (lastModified)
  }

  it should "store and update unfinished haku" in {
    val unfinishedHaku = new Haku(muokkaaja = UserOid("9.9.9.9.9"), organisaatioOid = OrganisaatioOid("5.5.5"), modified = None)
    val oid = put(unfinishedHaku)
    val lastModified = get(oid, unfinishedHaku.copy(oid = Some(HakuOid(oid))))
    val newUnfinishedHaku = unfinishedHaku.copy(oid = Some(HakuOid(oid)), organisaatioOid = OrganisaatioOid("6.6.6"))
    update(newUnfinishedHaku, lastModified)
    get(oid, newUnfinishedHaku)
  }

  it should "validate updated haku" in {
    val oid = put(haku)
    val lastModified = get(oid, haku(oid))
    post(HakuPath, bytes(addInvalidHakuaika(haku(oid))), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validateErrorBody(InvalidHakuaika))
    }
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
    val lastModified = get(oid, haku(oid))
    update(haku(oid).copy(hakuajat = List()), lastModified)
    get(oid, haku(oid).copy(hakuajat = List()))
  }
}
