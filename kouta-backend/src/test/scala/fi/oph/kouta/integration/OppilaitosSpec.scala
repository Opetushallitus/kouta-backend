package fi.oph.kouta.integration

import fi.oph.kouta.TestData
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.domain.{Arkistoitu, OppilaitosEnrichedData}
import fi.oph.kouta.integration.fixture.{MockS3Client, OppilaitoksenOsaFixture, OppilaitosFixture, UploadFixture}
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.validation.Validations._

import java.time.LocalDateTime
import java.util.UUID

class OppilaitosSpec extends KoutaIntegrationSpec with AccessControlSpec with OppilaitosFixture with OppilaitoksenOsaFixture with UploadFixture {
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

  it should "read muokkaaja from the session" in {
    val oid = put(oppilaitos.copy(muokkaaja = UserOid("random")))
    get(oid, oppilaitos(oid).copy(muokkaaja = testUser.oid))
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
      body should equal (validationErrorBody(validationMsg("saippua"), "organisaatioOid"))
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

  it should "copy a temporary logo to a permanent location while creating the oppilaitos" in {
    saveLocalPng("temp/image.png")
    val oid = put(oppilaitos.copy(logo = Some(s"$PublicImageServer/temp/image.png")))

    get(oid, oppilaitos(oid).copy(logo = Some(s"$PublicImageServer/oppilaitos-logo/$oid/image.png")))

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"oppilaitos-logo/$oid/image.png"))
    MockS3Client.getLocal("konfo-files", s"temp/image.png") shouldBe empty
  }

  it should "not touch a logo that's not in the temporary location" in {
    val oppilaitosWithImage = oppilaitos.copy(logo = Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))
    val oid = put(oppilaitosWithImage)
    MockS3Client.storage shouldBe empty
    get(oid, oppilaitosWithImage.copy(oid = OrganisaatioOid(oid)))
  }

  it should "fail validation if wwwPage undefined for published oppilaitos" in {
    val oppilaitosWithoutWwwPage = oppilaitos.copy(metadata = Some(oppilaitos.metadata.get.copy(wwwSivu = None)))
    put(OppilaitosPath, bytes(oppilaitosWithoutWwwPage), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(missingMsg, "metadata.wwwSivu"))
    }
  }

  "Update oppilaitos" should "update oppilaitos" in {
    val oid = put(oppilaitos)
    val lastModified = get(oid, oppilaitos(oid))
    update(oppilaitos(oid, Arkistoitu), lastModified)
    get(oid, oppilaitos(oid, Arkistoitu))
  }

  it should "read muokkaaja from the session" in {
    val oid = put(oppilaitos, crudSessions(ChildOid))
    val userOid = userOidForTestSessionId(crudSessions(ChildOid))
    val lastModified = get(oid, oppilaitos(oid).copy(muokkaaja = userOid))
    update(oppilaitos(oid, Arkistoitu).copy(muokkaaja = userOid), lastModified)
    get(oid, oppilaitos(oid, Arkistoitu).copy(muokkaaja = testUser.oid))
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
    val lastModified = get(oid, unfinishedOppilaitos.copy(oid = OrganisaatioOid(oid), _enrichedData = Some(OppilaitosEnrichedData(muokkaajanNimi = Some("Testi Muokkaaja")))))
    val newUnfinishedOppilaitos = unfinishedOppilaitos.copy(oid = OrganisaatioOid(oid), organisaatioOid = LonelyOid)
    update(newUnfinishedOppilaitos, lastModified)
    get(oid, newUnfinishedOppilaitos.copy(_enrichedData = Some(OppilaitosEnrichedData(muokkaajanNimi = Some("Testi Muokkaaja")))))
  }

  it should "validate updated oppilaitos" in {
    val oid = put(oppilaitos)
    val lastModified = get(oid, oppilaitos(oid))
    post(OppilaitosPath, bytes(oppilaitos(oid).copy(organisaatioOid = OrganisaatioOid("saippua"))), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(validationMsg("saippua"), "organisaatioOid"))
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

  it should "copy a temporary logo to a permanent location while updating the oppilaitos" in {
    val oid = put(oppilaitos)
    val lastModified = get(oid, oppilaitos(oid))

    saveLocalPng("temp/image.png")
    val oppilaitosWithLogo = oppilaitos(oid).copy(logo = Some(s"$PublicImageServer/temp/image.png"))

    update(oppilaitosWithLogo, lastModified)
    get(oid, oppilaitosWithLogo.copy(logo = Some(s"$PublicImageServer/oppilaitos-logo/$oid/image.png")))

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"oppilaitos-logo/$oid/image.png"))
  }

  it should "not touch a logo that's not in the temporary location" in {
    val oid = put(oppilaitos)
    val lastModified = get(oid, oppilaitos(oid))
    val oppilaitosWithImage = oppilaitos(oid).copy(logo = Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))

    update(oppilaitosWithImage, lastModified)

    MockS3Client.storage shouldBe empty
    get(oid, oppilaitosWithImage.copy(oid = OrganisaatioOid(oid)))
  }

  it should "copy both, a temporary teemakuva and a temporary logo, to a permanent location while updating the oppilaitos" in {
    val oid = put(oppilaitos)
    val lastModified = get(oid, oppilaitos(oid))

    saveLocalPng("temp/teemakuva.png")
    saveLocalPng("temp/logo.png")
    val oppilaitosWithImages = oppilaitos(oid).copy(
      teemakuva = Some(s"$PublicImageServer/temp/teemakuva.png"),
      logo = Some(s"$PublicImageServer/temp/logo.png")
    )

    update(oppilaitosWithImages, lastModified)
    get(oid, oppilaitosWithImages.copy(
      teemakuva = Some(s"$PublicImageServer/oppilaitos-teemakuva/$oid/teemakuva.png"),
      logo = Some(s"$PublicImageServer/oppilaitos-logo/$oid/logo.png")
    ))

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"oppilaitos-logo/$oid/logo.png"))
    checkLocalPng(MockS3Client.getLocal("konfo-files", s"oppilaitos-teemakuva/$oid/teemakuva.png"))
  }

  it should "not change other oppilaitos while updating teemakuva and logo" in {
    val oid1 = put(oppilaitos)
    val lastModified1 = get(oid1, oppilaitos(oid1))
    val oid2 = put(oppilaitos)

    saveLocalPng("temp/teemakuva.png")
    saveLocalPng("temp/logo.png")
    val oppilaitosWithImages = oppilaitos(oid1).copy(
      teemakuva = Some(s"$PublicImageServer/temp/teemakuva.png"),
      logo = Some(s"$PublicImageServer/temp/logo.png")
    )

    update(oppilaitosWithImages, lastModified1)

    get(oid2, oppilaitos(oid2))
  }

  "Get oppilaitokset by oids" should "return organisaatio oid without oppilaitos data for oppilaitos not in kouta" in {
    post(s"$OppilaitosPath/oppilaitokset", bytes(List(UnknownOid)), headers = defaultHeaders) {
      status should equal (200)
      body should include ("{\"oppilaitokset\":[{\"oid\":\"1.2.246.562.10.99999999998\"}]")
    }
  }

  it should "succeed in sending oppilaitos data for existing oppilaitos" in {
    val oid = put(oppilaitos)
    post(s"$OppilaitosPath/oppilaitokset", bytes(List(oid)), headers = defaultHeaders) {
      status should equal (200)
      body should include (s"""{\"oppilaitokset\":[{\"oid\":\"$oid\",\"oppilaitos\":{\"oid\":\"$oid\"""")
    }
  }
}
