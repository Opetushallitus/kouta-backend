package fi.oph.kouta.integration

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData.{organisaatio, organisaatioServiceOrgWithOid}
import fi.oph.kouta.TestOids._
import fi.oph.kouta.client.OrganisaatioServiceClient
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.domain._
import fi.oph.kouta.integration.fixture.{MockS3Client, OppilaitoksenOsaFixture, OppilaitosFixture, UploadFixture}
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.validation.Validations._
import org.json4s.jackson.Serialization.read

import java.time.LocalDateTime
import java.util.UUID

class OppilaitosSpec extends KoutaIntegrationSpec with AccessControlSpec with OppilaitosFixture with OppilaitoksenOsaFixture with UploadFixture {
  override val roleEntities     = Seq(Role.Oppilaitos)
  override val mockOrganisaatioServiceClient = mock[OrganisaatioServiceClient]

  override def beforeAll(): Unit = {
    super.beforeAll()

    when(mockOrganisaatioServiceClient.getOrganisaatioChildrenFromCache(any[OrganisaatioOid])).thenReturn(OrganisaatioHierarkia(organisaatiot = List()))
  }

  def organisaatioHierarkia(oid: String) = OrganisaatioHierarkia(
    List(
      Organisaatio(
        oid,
        List(OrganisaatioOid(oid), OrganisaatioOid("1.2.246.562.10.47941294986"), OrganisaatioOid("1.2.246.562.10.00000000001")),
        Map(Fi -> "Opisto fi", Sv -> "Opisto sv", En -> "Opisto en"),
        kieletUris = List(),
        yhteystiedot = None,
        kotipaikkaUri = None,
        children = None,
        oppilaitostyyppiUri = Some("oppilaitostyyppi_63#1"),
        Some(List("organisaatiotyyppi_02")))))

  val notSavedInKoutaOrgOid = OrganisaatioOid("1.2.246.562.10.404")

  "Get oppilaitos by oid" should "return only oid and yhteystiedot in _enrichedData for oppilaitos that hasn't been stored in kouta" in {
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(notSavedInKoutaOrgOid)).
      thenReturn(organisaatioServiceOrgWithOid.copy(oid = notSavedInKoutaOrgOid.s))

    val expected = OppilaitosWithOrganisaatioData(
      oid = notSavedInKoutaOrgOid,
      _enrichedData = Some(OppilaitosEnrichedData(
        muokkaajanNimi = None,
        organisaatio = Some(TestData.organisaatio.copy(oid = notSavedInKoutaOrgOid.s)))))

    get(s"$OppilaitosPath/${notSavedInKoutaOrgOid.toString()}", headers = Seq(sessionHeader(defaultSessionId))) {
      read[OppilaitosWithOrganisaatioData](body) should equal(expected)
    }
  }

  it should "return oppilaitos without organisaatio data if organisaatio not found" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenThrow(new RuntimeException())

    get(oid, oppilaitos(oid))
  }

  it should "return 401 if no session is found" in {
    get(s"$OppilaitosPath/${UUID.randomUUID()}") {
      status should equal(401)
    }
  }

  it should "allow a user of the oppilaitos organization to read the oppilaitos" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    val oppilaitosWithEnrichedData = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))

    get(oid, crudSessions(oppilaitos.organisaatioOid), oppilaitosWithEnrichedData)
  }

  it should "deny a user without access to the oppilaitos organization" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    get(s"$OppilaitosPath/$oid", crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to read the oppilaitos" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    val oppilaitosWithEnrichedData = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))

    get(oid, crudSessions(ParentOid), oppilaitosWithEnrichedData)
  }

  it should "deny a user with only access to a descendant organization" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    get(s"$OppilaitosPath/$oid", crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    get(s"$OppilaitosPath/$oid", otherRoleSession, 403)
  }

  it should "allow indexer access" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)
    when(mockOrganisaatioServiceClient.getOrganisaatioChildrenFromCache(OrganisaatioOid(oid))).
      thenReturn(OrganisaatioHierarkia(organisaatiot = List(TestData.organisaatio.copy(oid = oid, children = Some(List(TestData.organisaatioChild))))))

    val oppilaitosWithEnrichedData = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio.copy(children = Some(List(TestData.organisaatioChild))))))

    get(oid, indexerSession, oppilaitosWithEnrichedData)
  }

  "Create oppilaitos" should "store oppilaitos" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)
    when(mockOrganisaatioServiceClient.getOrganisaatioChildrenFromCache(OrganisaatioOid(oid))).
      thenReturn(OrganisaatioHierarkia(organisaatiot = List(TestData.organisaatio.copy(oid = oid, children = Some(List(TestData.organisaatioChild))))))

    val oppilaitosWithEnrichedData = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(
        organisaatio = Some(organisaatio.copy(children = Some(List(TestData.organisaatioChild))))))

    get(oid, oppilaitosWithEnrichedData)
  }

  it should "read muokkaaja from the session" in {
    val oid = put(oppilaitos.copy(muokkaaja = UserOid("random")))
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)
    when(mockOrganisaatioServiceClient.getOrganisaatioChildrenFromCache(OrganisaatioOid(oid))).
      thenReturn(OrganisaatioHierarkia(organisaatiot = List(TestData.organisaatio.copy(oid = oid, children = Some(List(TestData.organisaatioChild))))))

    val oppilaitosWithEnrichedData = oppilaitos(oid)
      .copy(muokkaaja = testUser.oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(
        organisaatio = Some(organisaatio.copy(children = Some(List(TestData.organisaatioChild))))))

    get(oid, oppilaitosWithEnrichedData)
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
    val oppilaitosWithTeemakuva = oppilaitos.withTeemakuva(Some(s"$PublicImageServer/temp/image.png"))
    val oid = put(oppilaitosWithTeemakuva)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)
    when(mockOrganisaatioServiceClient.getOrganisaatioChildrenFromCache(OrganisaatioOid(oid))).thenReturn(OrganisaatioHierarkia(List()))

    val oppilaitosWithEnrichedData = oppilaitosWithTeemakuva
      .withTeemakuva(Some(s"$PublicImageServer/oppilaitos-teemakuva/$oid/image.png"))
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))

    get(oid, oppilaitosWithEnrichedData)

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"oppilaitos-teemakuva/$oid/image.png"))
    MockS3Client.getLocal("konfo-files", s"temp/image.png") shouldBe empty
  }

  it should "not touch a teemakuva that's not in the temporary location" in {
    val oppilaitosWithImage = oppilaitos.withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))
    val oid = put(oppilaitosWithImage)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    val oppilaitosWithEnrichedData = oppilaitosWithImage
      .copy(oid = OrganisaatioOid(oid))
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))

    MockS3Client.storage shouldBe empty
    get(oid, oppilaitosWithEnrichedData)
  }

  it should "copy a temporary logo to a permanent location while creating the oppilaitos" in {
    saveLocalPng("temp/image.png")
    val oid = put(oppilaitos.copy(logo = Some(s"$PublicImageServer/temp/image.png")))
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    val oppilaitosWithEnrichedData = oppilaitos(oid)
      .copy(logo = Some(s"$PublicImageServer/oppilaitos-logo/$oid/image.png"))
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))

    get(oid, oppilaitosWithEnrichedData)

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"oppilaitos-logo/$oid/image.png"))
    MockS3Client.getLocal("konfo-files", s"temp/image.png") shouldBe empty
  }

  it should "not touch a logo that's not in the temporary location" in {
    val oppilaitosWithImage = oppilaitos.copy(logo = Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))
    val oid = put(oppilaitosWithImage)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    val oppilaitosWithEnrichedData = oppilaitosWithImage
      .copy(oid = OrganisaatioOid(oid))
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))

    MockS3Client.storage shouldBe empty
    get(oid, oppilaitosWithEnrichedData)
  }

  "Update oppilaitos" should "update oppilaitos" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)
    var oppilaitosWithEnrichedData = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))

    val lastModified = get(oid, oppilaitosWithEnrichedData)
    val arkistoituOppilaitos = oppilaitos(oid, Arkistoitu)
    update(arkistoituOppilaitos, lastModified)

    oppilaitosWithEnrichedData = arkistoituOppilaitos
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))

    get(oid, oppilaitosWithEnrichedData)
  }

  it should "read muokkaaja from the session" in {
    val oid = put(oppilaitos, crudSessions(ChildOid))
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)
    val userOid = userOidForTestSessionId(crudSessions(ChildOid))

    var oppilaitosWithEnrichedData = oppilaitos(oid)
      .copy(muokkaaja = userOid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))

    val lastModified = get(oid, oppilaitosWithEnrichedData)

    val arkistoituOppilaitos = oppilaitos(oid, Arkistoitu)
    update(arkistoituOppilaitos.copy(muokkaaja = userOid), lastModified)

    oppilaitosWithEnrichedData = arkistoituOppilaitos
      .copy(muokkaaja = testUser.oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))

    get(oid, oppilaitosWithEnrichedData)
  }

  it should "write oppilaitos update to audit log" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    var oppilaitosWithEnrichedData = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))

    val lastModified = get(oid, oppilaitosWithEnrichedData)
    MockAuditLogger.clean()

    val arkistoituOppilaitos = oppilaitos(oid, Arkistoitu).withModified(LocalDateTime.parse("1000-01-01T12:00:00"))
    update(arkistoituOppilaitos, lastModified)

    oppilaitosWithEnrichedData = arkistoituOppilaitos
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))

    get(oid, oppilaitosWithEnrichedData)
    MockAuditLogger.findFieldChange("tila", "julkaistu", "arkistoitu", oid, "oppilaitos_update") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "not update oppilaitos" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    val oppilaitosWithEnrichedData = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))

    val lastModified = get(oid, oppilaitosWithEnrichedData)
    MockAuditLogger.clean()
    update(oppilaitosWithEnrichedData, lastModified, false)
    MockAuditLogger.logs shouldBe empty
    get(oid, oppilaitosWithEnrichedData) should equal (lastModified)
  }

  it should "return 401 if no session is found" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    val oppilaitosWithEnrichedData = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))

    val lastModified = get(oid, oppilaitosWithEnrichedData)
    post(OppilaitosPath, bytes(oppilaitos(oid)), Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified)) {
      status should equal (401)
    }
  }

  it should "allow a user of the oppilaitos organization to update the oppilaitos" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)
    val thisOppilaitoksenOsa = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, false, crudSessions(oppilaitos.organisaatioOid))
  }

  it should "deny a user without access to the oppilaitos organization" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)
    val thisOppilaitoksenOsa = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, 403, crudSessions(LonelyOid))
  }

  it should "allow a user of an ancestor organization to create the oppilaitos" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)
    val thisOppilaitoksenOsa = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, false, crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)
    val thisOppilaitoksenOsa = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, 403, crudSessions(GrandChildOid))
  }

  it should "deny a user with the wrong role" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)
    val thisOppilaitoksenOsa = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, 403, readSessions(oppilaitos.organisaatioOid))
  }

  it should "deny indexer access" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)
    val thisOppilaitoksenOsa = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))
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
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    val oppilaitosWithEnrichedData = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))

    val lastModified = get(oid, oppilaitosWithEnrichedData)
    Thread.sleep(1500)

    val arkistoituOppilaitos = oppilaitos(oid, Arkistoitu)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))

    update(arkistoituOppilaitos, lastModified)
    post(OppilaitosPath, bytes(oppilaitos(oid)), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "store and update unfinished oppilaitos" in {
    val unfinishedOppilaitos = TestData.MinOppilaitos
    val oid = put(unfinishedOppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    val unfinishedOppilaitosWithYhteystiedot = unfinishedOppilaitos
      .copy(oid = OrganisaatioOid(oid))
      .withEnrichedData(oppilaitos._enrichedData.get.copy(muokkaajanNimi = Some("Testi Muokkaaja"), organisaatio = Some(organisaatio)))

    val lastModified = get(oid, unfinishedOppilaitosWithYhteystiedot)
    val newUnfinishedOppilaitos = unfinishedOppilaitos.copy(oid = OrganisaatioOid(oid), organisaatioOid = LonelyOid)
    update(newUnfinishedOppilaitos, lastModified)

    val newUnfinishedOppilaitosWithYhteystiedot = newUnfinishedOppilaitos
      .withEnrichedData(oppilaitos._enrichedData.get.copy(muokkaajanNimi = Some("Testi Muokkaaja"), organisaatio = Some(organisaatio)))
    get(oid, newUnfinishedOppilaitosWithYhteystiedot)
  }

  it should "validate updated oppilaitos" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    val lastModified = get(oid, oppilaitos(oid).withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio))))
    post(OppilaitosPath, bytes(oppilaitos(oid).copy(organisaatioOid = OrganisaatioOid("saippua"))), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(validationMsg("saippua"), "organisaatioOid"))
    }
  }

  it should "copy a temporary teemakuva to a permanent location while updating the oppilaitos" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    var oppilaitosWithEnrichedData = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))
    val lastModified = get(oid, oppilaitosWithEnrichedData)

    saveLocalPng("temp/image.png")
    oppilaitosWithEnrichedData = oppilaitosWithEnrichedData.withTeemakuva(Some(s"$PublicImageServer/temp/image.png"))
    update(oppilaitosWithEnrichedData, lastModified)

    oppilaitosWithEnrichedData = oppilaitosWithEnrichedData
      .withTeemakuva(Some(s"$PublicImageServer/oppilaitos-teemakuva/$oid/image.png"))
    get(oid, oppilaitosWithEnrichedData)

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"oppilaitos-teemakuva/$oid/image.png"))
  }

  it should "not touch a teemakuva that's not in the temporary location" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    var oppilaitosWithEnrichedData = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))
    val lastModified = get(oid, oppilaitosWithEnrichedData)

    oppilaitosWithEnrichedData = oppilaitosWithEnrichedData.withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))
    update(oppilaitosWithEnrichedData, lastModified)

    oppilaitosWithEnrichedData = oppilaitosWithEnrichedData.copy(oid = OrganisaatioOid(oid))

    MockS3Client.storage shouldBe empty
    get(oid, oppilaitosWithEnrichedData)
  }

  it should "copy a temporary logo to a permanent location while updating the oppilaitos" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    var oppilaitosWithEnrichedData = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))
    val lastModified = get(oid, oppilaitosWithEnrichedData)

    saveLocalPng("temp/image.png")
    oppilaitosWithEnrichedData = oppilaitosWithEnrichedData.copy(logo = Some(s"$PublicImageServer/temp/image.png"))
    update(oppilaitosWithEnrichedData, lastModified)

    oppilaitosWithEnrichedData = oppilaitosWithEnrichedData
      .copy(logo = Some(s"$PublicImageServer/oppilaitos-logo/$oid/image.png"))
    get(oid, oppilaitosWithEnrichedData)

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"oppilaitos-logo/$oid/image.png"))
  }

  it should "not touch a logo that's not in the temporary location" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    var oppilaitosWithEnrichedData = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))
    val lastModified = get(oid, oppilaitosWithEnrichedData)

    oppilaitosWithEnrichedData = oppilaitosWithEnrichedData.copy(logo = Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))
    update(oppilaitosWithEnrichedData, lastModified)

    oppilaitosWithEnrichedData = oppilaitosWithEnrichedData
      .copy(oid = OrganisaatioOid(oid))

    MockS3Client.storage shouldBe empty
    get(oid, oppilaitosWithEnrichedData)
  }

  it should "copy both, a temporary teemakuva and a temporary logo, to a permanent location while updating the oppilaitos" in {
    val oid = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid))).
      thenReturn(organisaatioServiceOrgWithOid)

    var oppilaitosWithEnrichedData = oppilaitos(oid)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))
    val lastModified = get(oid, oppilaitosWithEnrichedData)

    saveLocalPng("temp/teemakuva.png")
    saveLocalPng("temp/logo.png")
    oppilaitosWithEnrichedData = oppilaitosWithEnrichedData.copy(
      teemakuva = Some(s"$PublicImageServer/temp/teemakuva.png"),
      logo = Some(s"$PublicImageServer/temp/logo.png")
    )
    update(oppilaitosWithEnrichedData, lastModified)

    oppilaitosWithEnrichedData = oppilaitosWithEnrichedData
      .copy(
        teemakuva = Some(s"$PublicImageServer/oppilaitos-teemakuva/$oid/teemakuva.png"),
        logo = Some(s"$PublicImageServer/oppilaitos-logo/$oid/logo.png")
      )
    get(oid, oppilaitosWithEnrichedData)

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"oppilaitos-logo/$oid/logo.png"))
    checkLocalPng(MockS3Client.getLocal("konfo-files", s"oppilaitos-teemakuva/$oid/teemakuva.png"))
  }

  it should "not change other oppilaitos while updating teemakuva and logo" in {
    val oid1 = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid1))).
      thenReturn(organisaatioServiceOrgWithOid)
    val createdOppilaitos1 = oppilaitos(oid1)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))

    val lastModified1 = get(oid1, createdOppilaitos1)
    val oid2 = put(oppilaitos)
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oid2))).
      thenReturn(organisaatioServiceOrgWithOid)

    val createdOppilaitos2 = oppilaitos(oid2)
      .withEnrichedData(oppilaitos._enrichedData.get.copy(organisaatio = Some(organisaatio)))

    saveLocalPng("temp/teemakuva.png")
    saveLocalPng("temp/logo.png")
    val oppilaitosWithImages = oppilaitos(oid1).copy(
      teemakuva = Some(s"$PublicImageServer/temp/teemakuva.png"),
      logo = Some(s"$PublicImageServer/temp/logo.png")
    )
    update(oppilaitosWithImages, lastModified1)

    get(oid2, createdOppilaitos2)
  }

  "Get oppilaitokset by oids" should "not return oppilaitos data for oppilaitos not in kouta" in {
    val organisaatioServiceHierarkia = organisaatioHierarkia(UnknownOid.toString)
    when(mockOrganisaatioServiceClient.getOrganisaatioHierarkiaWithOidsFromCache(List(UnknownOid))).thenReturn(organisaatioServiceHierarkia)
    when(mockOppilaitosDao.get(List(UnknownOid))).thenReturn(Vector())
    post(s"$OppilaitosPath/oppilaitokset", bytes(List(UnknownOid)), headers = defaultHeaders) {
      status should equal (200)
      body should include ("{\"oppilaitokset\":[]")
    }
  }

  it should "succeed in sending oppilaitos data for existing oppilaitos" in {
    val oid = put(oppilaitos)
    val organisaatioServiceHierarkia = organisaatioHierarkia(oid)
    when(mockOrganisaatioServiceClient.getOrganisaatioHierarkiaWithOidsFromCache(List(OrganisaatioOid(oid)))).thenReturn(organisaatioServiceHierarkia)
    when(mockOppilaitosDao.get(List(OrganisaatioOid(oid)))).thenReturn(
      Vector(OppilaitosAndOsa(oppilaitos = oppilaitos.copy(oid = OrganisaatioOid(oid)), osa = Some(oppilaitoksenOsa))))

    post(s"$OppilaitosPath/oppilaitokset", bytes(List(oid)), headers = defaultHeaders) {
      status should equal (200)
      body should include (s"""{\"oppilaitokset\":[{\"oid\":\"$oid\"""")
    }
  }
}
