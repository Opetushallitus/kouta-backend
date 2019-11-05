package fi.oph.kouta.integration

import java.util.UUID

import fi.oph.kouta.TestData
import fi.oph.kouta.domain.Arkistoitu
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.integration.fixture.{MockS3Client, OppilaitoksenOsaFixture, OppilaitosFixture, UploadFixture}
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.validation.Validations

class OppilaitoksenOsaSpec
  extends KoutaIntegrationSpec
    with AccessControlSpec
    with OppilaitoksenOsaFixture
    with OppilaitosFixture
    with UploadFixture
    with Validations {

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

  it should "return 401 if no session is found" in {
    put(s"$OppilaitoksenOsaPath", bytes(oppilaitoksenOsa(oppilaitosOid))) {
      status should equal (401)
    }
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
      body should equal (validateErrorBody(validationMsg("saippua")))
    }
  }

  it should "copy a temporary image to a permanent location while creating the oppilaitoksen osa" in {
    saveLocalPng("temp/image.png")
    val oid = put(oppilaitoksenOsa(oppilaitosOid).copy(metadata = oppilaitoksenOsa.metadata.map(_.withTeemakuva(Some(s"$PublicImageServer/temp/image.png")))))

    get(oid, oppilaitoksenOsa(oid, oppilaitosOid).copy(metadata = oppilaitoksenOsa.metadata.map(_.withTeemakuva(Some(s"$PublicImageServer/oppilaitoksen-osa-teemakuva/$oid/image.png")))))

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"oppilaitoksenOsa-teemakuva/$oid/image.png"))
    MockS3Client.getLocal("konfo-files", s"temp/image.png") shouldBe empty
    MockS3Client.reset()
  }

  it should "not touch an image that's not in the temporary location" in {
    val oppilaitoksenOsaWithImage = oppilaitoksenOsa(oppilaitosOid).copy(metadata = oppilaitoksenOsa.metadata.map(_.withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))))
    val oid = put(oppilaitoksenOsaWithImage)
    MockS3Client.storage shouldBe empty
    get(oid, oppilaitoksenOsaWithImage.copy(oid = OrganisaatioOid(oid)))
    MockS3Client.reset()
  }

  "Update oppilaitoksen osa" should "update oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    val lastModified = get(oid, oppilaitoksenOsa(oid, oppilaitosOid))
    update(oppilaitoksenOsa(oid, oppilaitosOid, Arkistoitu), lastModified)
    get(oid, oppilaitoksenOsa(oid, oppilaitosOid, Arkistoitu))
  }

  it should "not update oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    val lastModified = get(oid, oppilaitoksenOsa(oid, oppilaitosOid))
    update(oppilaitoksenOsa(oid, oppilaitosOid), lastModified, false)
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
    val newUnfinishedOppilaitoksenOsa = unfinishedOppilaitoksenOsa.copy(oid = OrganisaatioOid(oid), organisaatioOid = OrganisaatioOid("6.6.6.6.6"))
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
      body should equal (validateErrorBody(validationMsg("saippua")))
    }
  }

  it should "copy a temporary image to a permanent location while updating the oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitosOid))
    val lastModified = get(oid, oppilaitoksenOsa(oid, oppilaitosOid))

    saveLocalPng("temp/image.png")
    val oppilaitoksenOsaWithImage = oppilaitoksenOsa(oid, oppilaitosOid).copy(metadata = oppilaitoksenOsa.metadata.map(_.withTeemakuva(Some(s"$PublicImageServer/temp/image.png"))))

    update(oppilaitoksenOsaWithImage, lastModified)
    get(oid, oppilaitoksenOsaWithImage.copy(metadata = oppilaitoksenOsa.metadata.map(_.withTeemakuva(Some(s"$PublicImageServer/oppilaitoksenOsa-teemakuva/$oid/image.png")))))

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"oppilaitoksenOsa-teemakuva/$oid/image.png"))
    MockS3Client.reset()
  }

  it should "not touch an image that's not in the temporary location" in {
    val oid = put(oppilaitoksenOsa)
    val lastModified = get(oid, oppilaitoksenOsa(oid))
    val oppilaitoksenOsaWithImage = oppilaitoksenOsa(oid).copy(metadata = oppilaitoksenOsa.metadata.map(_.withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))))

    update(oppilaitoksenOsaWithImage, lastModified)

    MockS3Client.storage shouldBe empty
    get(oid, oppilaitoksenOsaWithImage.copy(oid = OrganisaatioOid(oid)))
    MockS3Client.reset()
  }
}
