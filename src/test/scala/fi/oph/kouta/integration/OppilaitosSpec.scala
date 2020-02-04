package fi.oph.kouta.integration

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.TestData
import fi.oph.kouta.domain.Arkistoitu
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.integration.fixture.{MockS3Client, OppilaitoksenOsaFixture, OppilaitosFixture, UploadFixture}
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.validation.Validations

class OppilaitosSpec extends KoutaIntegrationSpec with AccessControlSpec with OppilaitosFixture with OppilaitoksenOsaFixture with UploadFixture with Validations {
  override val roleEntities = Seq(Role.Oppilaitos)

  "Get oppilaitos by oid" should "return 404 if oppilaitos not found" in {
    get(s"$OppilaitosPath/${UUID.randomUUID()}", headers = defaultHeaders) {
      status should equal (404)
      body should include ("Unknown organisaatio oid")
    }
  }

  it should "return 401 if no session is found" in {
    get(s"$OppilaitosPath/${UUID.randomUUID()}") {
      status should equal(401)
    }
  }

  it should "allow a user of the oppilaitos organization to read the oppilaitos" in {
    val oid = put(oppilaitos)
    get(oid, crudSessions(oppilaitos.organisaatioOid), oppilaitos(oid))
  }

  it should "deny a user without access to the oppilaitos organization" in {
    val oid = put(oppilaitos)
    get(s"$OppilaitosPath/$oid", crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to read the oppilaitos" in {
    val oid = put(oppilaitos)
    get(oid, crudSessions(ParentOid), oppilaitos(oid))
  }

  it should "deny a user with only access to a descendant organization" in {
    val oid = put(oppilaitos)
    get(s"$OppilaitosPath/$oid", crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    val oid = put(oppilaitos)
    get(s"$OppilaitosPath/$oid", otherRoleSession, 403)
  }

  it should "allow indexer access" in {
    val oid = put(oppilaitos)
    get(oid, indexerSession, oppilaitos(oid))
  }

  "Create oppilaitos" should "store oppilaitos" in {
    val oid = put(oppilaitos)
    get(oid, oppilaitos(oid))
  }

  it should "write create oppilaitos to audit log" in {
    MockAuditLogger.clean()
    val oid = put(oppilaitos.withModified(LocalDateTime.parse("1000-01-01T12:00:00")))
    MockAuditLogger.find(oid, "oppilaitos_create") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "return 401 if no session is found" in {
    put(s"$OppilaitosPath", bytes(oppilaitos)) {
      status should equal (401)
    }
  }

  it should "allow a user of the oppilaitos organization to create the oppilaitos" in {
    put(oppilaitos, crudSessions(oppilaitos.organisaatioOid))
  }

  it should "deny a user without access to the oppilaitos organization" in {
    put(OppilaitosPath, oppilaitos, crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to create the oppilaitos" in {
    put(oppilaitos, crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    put(OppilaitosPath, oppilaitos, crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    put(OppilaitosPath, oppilaitos, readSessions(ChildOid), 403)
  }

  it should "deny indexer access" in {
    put(OppilaitosPath, oppilaitos, indexerSession, 403)
  }

  it should "validate new oppilaitos" in {
    put(OppilaitosPath, bytes(oppilaitos.copy(organisaatioOid = OrganisaatioOid("saippua"))), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validateErrorBody(validationMsg("saippua")))
    }
  }

  it should "copy a temporary teemakuva to a permanent location while creating the oppilaitos" in {
    saveLocalPng("temp/image.png")
    val oid = put(oppilaitos.withTeemakuva(Some(s"$PublicImageServer/temp/image.png")))

    get(oid, oppilaitos(oid).withTeemakuva(Some(s"$PublicImageServer/oppilaitos-teemakuva/$oid/image.png")))

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"oppilaitos-teemakuva/$oid/image.png"))
    MockS3Client.getLocal("konfo-files", s"temp/image.png") shouldBe empty
  }

  it should "not touch a teemakuva that's not in the temporary location" in {
    val oppilaitosWithImage = oppilaitos.withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))
    val oid = put(oppilaitosWithImage)
    MockS3Client.storage shouldBe empty
    get(oid, oppilaitosWithImage.copy(oid = OrganisaatioOid(oid)))
  }

  "Update oppilaitos" should "update oppilaitos" in {
    val oid = put(oppilaitos)
    val lastModified = get(oid, oppilaitos(oid))
    update(oppilaitos(oid, Arkistoitu), lastModified)
    get(oid, oppilaitos(oid, Arkistoitu))
  }

  it should "write oppilaitos update to audit log" in {
    val oid = put(oppilaitos)
    val lastModified = get(oid, oppilaitos(oid))
    MockAuditLogger.clean()
    update(oppilaitos(oid, Arkistoitu).withModified(LocalDateTime.parse("1000-01-01T12:00:00")), lastModified)
    get(oid, oppilaitos(oid, Arkistoitu))
    MockAuditLogger.findFieldChange("tila", "julkaistu", "arkistoitu", oid, "oppilaitos_update") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "not update oppilaitos" in {
    val oid = put(oppilaitos)
    val lastModified = get(oid, oppilaitos(oid))
    MockAuditLogger.clean()
    update(oppilaitos(oid), lastModified, false)
    MockAuditLogger.logs shouldBe empty
    get(oid, oppilaitos(oid)) should equal (lastModified)
  }

  it should "return 401 if no session is found" in {
    val oid = put(oppilaitos)
    val lastModified = get(oid, oppilaitos(oid))
    post(OppilaitosPath, bytes(oppilaitos(oid)), Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified)) {
      status should equal (401)
    }
  }

  it should "allow a user of the oppilaitos organization to update the oppilaitos" in {
    val oid = put(oppilaitos)
    val thisOppilaitoksenOsa = oppilaitos(oid)
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, false, crudSessions(oppilaitos.organisaatioOid))
  }

  it should "deny a user without access to the oppilaitos organization" in {
    val oid = put(oppilaitos)
    val thisOppilaitoksenOsa = oppilaitos(oid)
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, 403, crudSessions(LonelyOid))
  }

  it should "allow a user of an ancestor organization to create the oppilaitos" in {
    val oid = put(oppilaitos)
    val thisOppilaitoksenOsa = oppilaitos(oid)
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, false, crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    val oid = put(oppilaitos)
    val thisOppilaitoksenOsa = oppilaitos(oid)
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, 403, crudSessions(GrandChildOid))
  }

  it should "deny a user with the wrong role" in {
    val oid = put(oppilaitos)
    val thisOppilaitoksenOsa = oppilaitos(oid)
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, 403, readSessions(oppilaitos.organisaatioOid))
  }

  it should "deny indexer access" in {
    val oid = put(oppilaitos)
    val thisOppilaitoksenOsa = oppilaitos(oid)
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, 403, indexerSession)
  }

  it should "fail update if 'x-If-Unmodified-Since' header is missing" in {
    val oid = put(oppilaitos)
    post(OppilaitosPath, bytes(oppilaitos(oid)), defaultHeaders) {
      status should equal (400)
      body should include (KoutaServlet.IfUnmodifiedSinceHeader)
    }
  }

  it should "fail update if modified in between get and update" in {
    val oid = put(oppilaitos)
    val lastModified = get(oid, oppilaitos(oid))
    Thread.sleep(1500)
    update(oppilaitos(oid, Arkistoitu), lastModified)
    post(OppilaitosPath, bytes(oppilaitos(oid)), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "store and update unfinished oppilaitos" in {
    val unfinishedOppilaitos = TestData.MinOppilaitos
    val oid = put(unfinishedOppilaitos)
    val lastModified = get(oid, unfinishedOppilaitos.copy(oid = OrganisaatioOid(oid)))
    val newUnfinishedOppilaitos = unfinishedOppilaitos.copy(oid = OrganisaatioOid(oid), organisaatioOid = OrganisaatioOid("6.6.6.6.6"))
    update(newUnfinishedOppilaitos, lastModified)
    get(oid, newUnfinishedOppilaitos)
  }

  it should "validate updated oppilaitos" in {
    val oid = put(oppilaitos)
    val lastModified = get(oid, oppilaitos(oid))
    post(OppilaitosPath, bytes(oppilaitos(oid).copy(organisaatioOid = OrganisaatioOid("saippua"))), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validateErrorBody(validationMsg("saippua")))
    }
  }

  it should "copy a temporary teemakuva to a permanent location while updating the oppilaitos" in {
    val oid = put(oppilaitos)
    val lastModified = get(oid, oppilaitos(oid))

    saveLocalPng("temp/image.png")
    val oppilaitosWithImage = oppilaitos(oid).withTeemakuva(Some(s"$PublicImageServer/temp/image.png"))

    update(oppilaitosWithImage, lastModified)
    get(oid, oppilaitosWithImage.withTeemakuva(Some(s"$PublicImageServer/oppilaitos-teemakuva/$oid/image.png")))

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"oppilaitos-teemakuva/$oid/image.png"))
  }

  it should "not touch a teemakuva that's not in the temporary location" in {
    val oid = put(oppilaitos)
    val lastModified = get(oid, oppilaitos(oid))
    val oppilaitosWithImage = oppilaitos(oid).withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))

    update(oppilaitosWithImage, lastModified)

    MockS3Client.storage shouldBe empty
    get(oid, oppilaitosWithImage.copy(oid = OrganisaatioOid(oid)))
  }
}
