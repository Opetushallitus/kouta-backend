package fi.oph.kouta.integration

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.TestData
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain.Arkistoitu
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.integration.fixture.{MockS3Client, OppilaitoksenOsaFixture, OppilaitosFixture, UploadFixture}
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.validation.Validations._

class OppilaitoksenOsaSpec extends KoutaIntegrationSpec with AccessControlSpec with OppilaitoksenOsaFixture
  with OppilaitosFixture with UploadFixture {

  override val roleEntities = Seq(Role.Oppilaitos)

  var oppilaitosOid = ""

  override def beforeAll(): Unit = {
    super.beforeAll()
    oppilaitosOid = put(oppilaitos)
  }

  "Get oppilaitoksen osa by oid" should "return 404 if oppilaitoksen osa not found" in {
    get(s"$OppilaitoksenOsaPath/${UUID.randomUUID()}", headers = defaultHeaders) {
      status should equal (404)
      body should include ("Unknown organisaatio oid")
    }
  }

  it should "return 401 if no session is found" in {
    get(s"$OppilaitoksenOsaPath/${UUID.randomUUID()}") {
      status should equal(401)
    }
  }

  it should "allow a user of the oppilaitoksen osa organization to read the oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    get(oid, crudSessions(oppilaitoksenOsa.organisaatioOid), oppilaitoksenOsa(oid, oppilaitosOid))
  }

  it should "deny a user without access to the oppilaitoksen osa organization" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    get(s"$OppilaitoksenOsaPath/$oid", crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to read the oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    get(oid, crudSessions(ParentOid), oppilaitoksenOsa(oid, oppilaitosOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    get(s"$OppilaitoksenOsaPath/$oid", crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    get(s"$OppilaitoksenOsaPath/$oid", otherRoleSession, 403)
  }

  it should "allow indexer access" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    get(oid, indexerSession, oppilaitoksenOsa(oid, oppilaitosOid))
  }

  "Create oppilaitoksen osa" should "store oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    get(oid, oppilaitoksenOsa(oid, oppilaitosOid))
  }

  it should "read muokkaaja from the session" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid).copy(muokkaaja = UserOid("random")))
    get(oid, oppilaitoksenOsa(oid, oppilaitosOid).copy(muokkaaja = testUser.oid))
  }

  it should "return 401 if no session is found" in {
    put(s"$OppilaitoksenOsaPath", bytes(oppilaitoksenOsa(oppilaitosOid))) {
      status should equal (401)
    }
  }

  it should "write create oppilaitoksen osa to audit log" in {
    MockAuditLogger.clean()
    val oid = put(oppilaitoksenOsa(oppilaitosOid).withModified(LocalDateTime.parse("1000-01-01T12:00:00")))
    MockAuditLogger.find(oid, "oppilaitoksen_osa_create") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "return 404 if oppilaitos not found" in {
    put(OppilaitoksenOsaPath, oppilaitoksenOsa, crudSessions(ParentOid), 404)
  }

  it should "allow a user of the oppilaitoksen osa organization to create the oppilaitoksen osa" in {
    put(oppilaitoksenOsa(oppilaitosOid), crudSessions(oppilaitoksenOsa.organisaatioOid))
  }

  it should "deny a user without access to the oppilaitoksen osa organization" in {
    put(OppilaitoksenOsaPath, oppilaitoksenOsa(oppilaitosOid), crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to create the oppilaitoksen osa" in {
    put(oppilaitoksenOsa(oppilaitosOid), crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    put(OppilaitoksenOsaPath, oppilaitoksenOsa(oppilaitosOid), crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    put(OppilaitoksenOsaPath, oppilaitoksenOsa(oppilaitosOid), readSessions(ChildOid), 403)
  }

  it should "deny indexer access" in {
    put(OppilaitoksenOsaPath, oppilaitoksenOsa(oppilaitosOid), indexerSession, 403)
  }

  it should "validate new oppilaitoksen osa" in {
    put(OppilaitoksenOsaPath, bytes(oppilaitoksenOsa.copy(organisaatioOid = OrganisaatioOid("saippua"))), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(validationMsg("saippua"), "organisaatioOid"))
    }
  }

  it should "copy a temporary image to a permanent location while creating the oppilaitoksen osa" in {
    saveLocalPng("temp/image.png")
    val oid = put(oppilaitoksenOsa(oppilaitosOid).withTeemakuva(Some(s"$PublicImageServer/temp/image.png")))

    get(oid, oppilaitoksenOsa(oid, oppilaitosOid).withTeemakuva(Some(s"$PublicImageServer/oppilaitoksen-osa-teemakuva/$oid/image.png")))

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"oppilaitoksen-osa-teemakuva/$oid/image.png"))
    MockS3Client.getLocal("konfo-files", s"temp/image.png") shouldBe empty
  }

  it should "not touch an image that's not in the temporary location" in {
    val oppilaitoksenOsaWithImage = oppilaitoksenOsa(oppilaitosOid).withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))
    val oid = put(oppilaitoksenOsaWithImage)
    MockS3Client.storage shouldBe empty
    get(oid, oppilaitoksenOsaWithImage.copy(oid = OrganisaatioOid(oid)))
  }

  "Update oppilaitoksen osa" should "update oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    val lastModified = get(oid, oppilaitoksenOsa(oid, oppilaitosOid))
    update(oppilaitoksenOsa(oid, oppilaitosOid, Arkistoitu), lastModified)
    get(oid, oppilaitoksenOsa(oid, oppilaitosOid, Arkistoitu))
  }

  it should "read muokkaaja from the session" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid), crudSessions(ChildOid))
    val userOid = userOidForTestSessionId(crudSessions(ChildOid))
    val lastModified = get(oid, oppilaitoksenOsa(oid, oppilaitosOid).copy(muokkaaja = userOid))
    update(oppilaitoksenOsa(oid, oppilaitosOid, Arkistoitu).copy(muokkaaja = userOid), lastModified)
    get(oid, oppilaitoksenOsa(oid, oppilaitosOid, Arkistoitu).copy(muokkaaja = testUser.oid))
  }

  it should "write oppilaitoksen osa update to audit log" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    val lastModified = get(oid, oppilaitoksenOsa(oid, oppilaitosOid))
    MockAuditLogger.clean()
    update(oppilaitoksenOsa(oid, oppilaitosOid, Arkistoitu).withModified(LocalDateTime.parse("1000-01-01T12:00:00")), lastModified)
    MockAuditLogger.findFieldChange("tila", "julkaistu", "arkistoitu", oid, "oppilaitoksen_osa_update") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "not update oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    val lastModified = get(oid, oppilaitoksenOsa(oid, oppilaitosOid))
    MockAuditLogger.clean()
    update(oppilaitoksenOsa(oid, oppilaitosOid), lastModified, false)
    MockAuditLogger.logs shouldBe empty
    get(oid, oppilaitoksenOsa(oid, oppilaitosOid)) should equal (lastModified)
  }

  it should "return 401 if no session is found" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    val lastModified = get(oid, oppilaitoksenOsa(oid, oppilaitosOid))
    post(OppilaitoksenOsaPath, bytes(oppilaitoksenOsa(oid, oppilaitosOid)), Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified)) {
      status should equal (401)
    }
  }

  it should "not update oppilaitos oid" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    val lastModified = get(oid, oppilaitoksenOsa(oid, oppilaitosOid))
    update(oppilaitoksenOsa(oid, ChildOid.s), lastModified, 200, crudSessions(ParentOid))
    get(oid, oppilaitoksenOsa(oid, oppilaitosOid))
  }

  it should "allow a user of the oppilaitoksen osa organization to update the oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    val thisOppilaitoksenOsa = oppilaitoksenOsa(oid, oppilaitosOid)
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, false, crudSessions(oppilaitoksenOsa.organisaatioOid))
  }

  it should "deny a user without access to the oppilaitoksen osa organization" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    val thisOppilaitoksenOsa = oppilaitoksenOsa(oid, oppilaitosOid)
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, 403, crudSessions(LonelyOid))
  }

  it should "allow a user of an ancestor organization to create the oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    val thisOppilaitoksenOsa = oppilaitoksenOsa(oid, oppilaitosOid)
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, false, crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    val thisOppilaitoksenOsa = oppilaitoksenOsa(oid, oppilaitosOid)
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, 403, crudSessions(GrandChildOid))
  }

  it should "deny a user with the wrong role" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    val thisOppilaitoksenOsa = oppilaitoksenOsa(oid, oppilaitosOid)
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, 403, readSessions(oppilaitoksenOsa.organisaatioOid))
  }

  it should "deny indexer access" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    val thisOppilaitoksenOsa = oppilaitoksenOsa(oid, oppilaitosOid)
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, 403, indexerSession)
  }

  it should "fail update if 'x-If-Unmodified-Since' header is missing" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    post(OppilaitoksenOsaPath, bytes(oppilaitoksenOsa(oid, oppilaitosOid)), defaultHeaders) {
      status should equal (400)
      body should include (KoutaServlet.IfUnmodifiedSinceHeader)
    }
  }

  it should "fail update if modified in between get and update" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    val lastModified = get(oid, oppilaitoksenOsa(oid, oppilaitosOid))
    Thread.sleep(1500)
    update(oppilaitoksenOsa(oid, oppilaitosOid, Arkistoitu), lastModified)
    post(OppilaitoksenOsaPath, bytes(oppilaitoksenOsa(oid, oppilaitosOid)), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "store and update unfinished oppilaitoksen osa" in {
    val unfinishedOppilaitoksenOsa = TestData.MinOppilaitoksenOsa.copy(oppilaitosOid = OrganisaatioOid(oppilaitosOid))
    val oid = put(unfinishedOppilaitoksenOsa)
    val lastModified = get(oid, unfinishedOppilaitoksenOsa.copy(oid = OrganisaatioOid(oid)))
    val newUnfinishedOppilaitoksenOsa = unfinishedOppilaitoksenOsa.copy(oid = OrganisaatioOid(oid), organisaatioOid = LonelyOid)
    update(newUnfinishedOppilaitoksenOsa, lastModified)
    get(oid, newUnfinishedOppilaitoksenOsa)
  }

  it should "validate updated oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    val lastModified = get(oid, oppilaitoksenOsa(oid, oppilaitosOid))
    post(OppilaitoksenOsaPath, bytes(oppilaitoksenOsa(oid, oppilaitosOid).copy(organisaatioOid = OrganisaatioOid("saippua"))), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(validationMsg("saippua"), "organisaatioOid"))
    }
  }

  it should "copy a temporary image to a permanent location while updating the oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    val lastModified = get(oid, oppilaitoksenOsa(oid, oppilaitosOid))

    saveLocalPng("temp/image.png")
    val oppilaitoksenOsaWithImage = oppilaitoksenOsa(oid, oppilaitosOid).withTeemakuva(Some(s"$PublicImageServer/temp/image.png"))

    update(oppilaitoksenOsaWithImage, lastModified)
    get(oid, oppilaitoksenOsaWithImage.withTeemakuva(Some(s"$PublicImageServer/oppilaitoksen-osa-teemakuva/$oid/image.png")))

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"oppilaitoksen-osa-teemakuva/$oid/image.png"))
  }

  it should "not touch an image that's not in the temporary location" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    val lastModified = get(oid, oppilaitoksenOsa(oid, oppilaitosOid))
    val oppilaitoksenOsaWithImage = oppilaitoksenOsa(oid, oppilaitosOid).withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))

    update(oppilaitoksenOsaWithImage, lastModified)

    MockS3Client.storage shouldBe empty
    get(oid, oppilaitoksenOsaWithImage)
  }

  it should "not change other oppilaitoksen osa while updating teemakuva" in {
    val oid1 = put(oppilaitoksenOsa(oppilaitosOid))
    val oid2 = put(oppilaitoksenOsa(oppilaitosOid))
    val lastModified = get(oid1, oppilaitoksenOsa(oid1, oppilaitosOid))
    val oppilaitoksenOsaWithImage = oppilaitoksenOsa(oid1, oppilaitosOid).withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))

    update(oppilaitoksenOsaWithImage, lastModified)

    get(oid2, oppilaitoksenOsa(oid2, oppilaitosOid))
  }
}
