package fi.oph.kouta.integration

import fi.oph.kouta.{TestData, TestOids}
import fi.oph.kouta.TestOids._
import fi.oph.kouta.client.{OrganisaatioServiceClient, OrganisaatioServiceQueryException}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.domain.{Arkistoitu, OppilaitoksenOsa, OppilaitosEnrichedData, OppilaitosWithOrganisaatioData}
import fi.oph.kouta.integration.fixture.{MockS3Client, OppilaitoksenOsaFixture, OppilaitosFixture, UploadFixture}
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.util.OppilaitosServiceUtil
import fi.oph.kouta.validation.Validations._
import org.json4s.jackson.Serialization.read

import java.time.{Instant, LocalDateTime}
import java.util.UUID

class OppilaitoksenOsaSpec extends KoutaIntegrationSpec with AccessControlSpec with OppilaitoksenOsaFixture
  with OppilaitosFixture with UploadFixture {

  override val roleEntities = Seq(Role.Oppilaitos)
  override val mockOrganisaatioServiceClient = mock[OrganisaatioServiceClient]

  var oppilaitoksenOsaOid = TestOids.GrandChildOid.toString
  var oppilaitosOid = TestOids.ChildOid.toString
  var defaultOppilaitoksenOsa: OppilaitoksenOsa = _
  var defaultOrganisaatio = TestData.koutaOrganisaationOsa
  var defaulOppilaitosEnrichedData = OppilaitosEnrichedData(
    muokkaajanNimi = Some(TestData.muokkaajanNimi),
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    when(mockOrganisaatioServiceClient.getOrganisaatioChildrenFromCache(any[OrganisaatioOid])).thenReturn(List())

    put(oppilaitos.copy(OrganisaatioOid(oppilaitosOid)))
    defaultOppilaitoksenOsa = oppilaitoksenOsa(oppilaitoksenOsaOid).copy(
      oppilaitosOid = Some(OrganisaatioOid(oppilaitosOid)),
      _enrichedData = Some(OppilaitosEnrichedData(
        muokkaajanNimi = Some(TestData.muokkaajanNimi),
        organisaatio = Some(TestData.koutaOrganisaationOsa.copy(oid = oppilaitoksenOsaOid)))))
  }

  override def beforeEach(): Unit = {
    deleteOppilaitostenOsat()

    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oppilaitoksenOsaOid))).
      thenReturn(TestData.organisaationOsa.copy(oid = oppilaitoksenOsaOid))

    val parentOids = OppilaitosServiceUtil.getParentOids(TestData.organisaationOsa.parentOidPath)
    when(mockOrganisaatioServiceClient.getOrganisaatiotWithOidsFromCache(parentOids)).
      thenReturn(Seq(
        TestData.organisaatio.copy(oid = oppilaitosOid),
        TestData.organisaatio.copy(oid = TestOids.randomOrganisaatioOid.s, organisaatiotyypit = List("organisaatiotyyppi_03"))))

    when(mockOppilaitoksenOsaDao.get(OrganisaatioOid(oppilaitoksenOsaOid))).
      thenReturn(Some(TestData.JulkaistuOppilaitoksenOsa.copy(oid = OrganisaatioOid(oppilaitoksenOsaOid)), Instant.now()))
  }

  val notSavedInKoutaOrgOid = OrganisaatioOid("1.2.246.562.10.404")

  "Get oppilaitoksen osa by oid" should "return only oid and yhteystiedot in _enrichedData for oppilaitos that hasn't been stored in kouta" in {
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(notSavedInKoutaOrgOid)).
      thenReturn(TestData.organisaationOsa.copy(oid = notSavedInKoutaOrgOid.s))
    when(mockOrganisaatioServiceClient.getOrganisaatioChildrenFromCache(notSavedInKoutaOrgOid)).
      thenReturn(List(TestData.organisaationOsa))

    val expected = OppilaitosWithOrganisaatioData(
      oid = notSavedInKoutaOrgOid,
      _enrichedData = Some(OppilaitosEnrichedData(
        muokkaajanNimi = None,
        organisaatio = Some(TestData.koutaOrganisaationOsa.copy(oid = notSavedInKoutaOrgOid.s, children = List(TestData.koutaOrganisaationOsa))))))

    get(s"$OppilaitoksenOsaPath/${notSavedInKoutaOrgOid.toString()}", headers = Seq(sessionHeader(defaultSessionId))) {
      read[OppilaitosWithOrganisaatioData](body) should equal(expected)
    }
  }

  it should "return 401 if no session is found" in {
    get(s"$OppilaitoksenOsaPath/${UUID.randomUUID()}") {
      status should equal(401)
    }
  }

  it should "allow a user of the oppilaitoksen osa organization to read the oppilaitoksen osa" in {
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oppilaitoksenOsaOid))).
      thenReturn(TestData.organisaationOsa.copy(oid = oppilaitoksenOsaOid))
    val parentOids = OppilaitosServiceUtil.getParentOids(TestData.organisaationOsa.parentOidPath)

    when(mockOrganisaatioServiceClient.getOrganisaatiotWithOidsFromCache(parentOids)).
      thenReturn(Seq(TestData.organisaatio.copy(oid = oppilaitosOid)))
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    get(oid, crudSessions(oppilaitoksenOsa.organisaatioOid), defaultOppilaitoksenOsa)
  }

  it should "deny a user without access to the oppilaitoksen osa organization" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    get(s"$OppilaitoksenOsaPath/$oid", crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to read the oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    get(oid, crudSessions(ParentOid), defaultOppilaitoksenOsa)
  }

  it should "deny a user with only access to a descendant organization" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    get(s"$OppilaitoksenOsaPath/$oid", crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    get(s"$OppilaitoksenOsaPath/$oid", otherRoleSession, 403)
  }

  it should "allow indexer access" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    get(oid, indexerSession, defaultOppilaitoksenOsa)
  }

  "Create oppilaitoksen osa" should "store oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    get(oid, defaultOppilaitoksenOsa)
  }

  it should "read muokkaaja from the session" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid).copy(muokkaaja = UserOid("random")))
    get(oid, defaultOppilaitoksenOsa.copy(muokkaaja = testUser.oid))
  }

  it should "return 401 if no session is found" in {
    put(s"$OppilaitoksenOsaPath", bytes(oppilaitoksenOsa)) {
      status should equal (401)
    }
  }

 it should "return 404 when oppilaitoksen osa is not found from organisaatio-service" in {
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(UnknownOid.s)))
      .thenThrow(OrganisaatioServiceQueryException(url = s"organisaatio-service/api/$UnknownOid", status = 404, message = "Error"))
    put(s"$OppilaitoksenOsaPath", bytes(oppilaitoksenOsa.copy(oid = UnknownOid)), defaultHeaders) {
      status should equal(404)
    }
  }

  it should "return 404 when oppilaitoksen osan parent oppilaitos is not found from organisaatio-service" in {
    val parentOidPath = s"${UnknownOid.toString}/1.2.246.562.10.00000000001"
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oppilaitoksenOsaOid))).
      thenReturn(TestData.organisaationOsa.copy(oid = oppilaitoksenOsaOid, parentOidPath = parentOidPath))

    val parentOids = OppilaitosServiceUtil.getParentOids(parentOidPath)
    when(mockOrganisaatioServiceClient.getOrganisaatiotWithOidsFromCache(parentOids))
      .thenThrow(OrganisaatioServiceQueryException(url = s"organisaatio-service/api/$UnknownOid", status = 404, message = "Error"))

    put(s"$OppilaitoksenOsaPath", bytes(oppilaitoksenOsa.copy(oid = OrganisaatioOid(oppilaitoksenOsaOid))), defaultHeaders) {
      status should equal(404)
    }
  }

  it should "write create oppilaitoksen osa to audit log" in {
    MockAuditLogger.clean()
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid).withModified(LocalDateTime.parse("1000-01-01T12:00:00")))
    MockAuditLogger.find(oid, "oppilaitoksen_osa_create") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "allow a user of the oppilaitoksen osa organization to create the oppilaitoksen osa" in {
    put(oppilaitoksenOsa(oppilaitoksenOsaOid), crudSessions(oppilaitoksenOsa.organisaatioOid))
  }

  it should "deny a user without access to the oppilaitoksen osa organization" in {
    put(OppilaitoksenOsaPath, oppilaitoksenOsa(oppilaitoksenOsaOid), crudSessions(LonelyOid), 403)
  }

  it should "allow a user of an ancestor organization to create the oppilaitoksen osa" in {
    put(oppilaitoksenOsa(oppilaitoksenOsaOid), crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    put(OppilaitoksenOsaPath, oppilaitoksenOsa(oppilaitoksenOsaOid), crudSessions(GrandChildOid), 403)
  }

  it should "deny a user with the wrong role" in {
    put(OppilaitoksenOsaPath, oppilaitoksenOsa(oppilaitoksenOsaOid), readSessions(ChildOid), 403)
  }

  it should "deny indexer access" in {
    put(OppilaitoksenOsaPath, oppilaitoksenOsa(oppilaitoksenOsaOid), indexerSession, 403)
  }

  it should "validate new oppilaitoksen osa" in {
    put(OppilaitoksenOsaPath, bytes(defaultOppilaitoksenOsa.copy(organisaatioOid = OrganisaatioOid("saippua"))), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(validationMsg("saippua"), "organisaatioOid"))
    }
  }

  it should "copy a temporary image to a permanent location while creating the oppilaitoksen osa" in {
    saveLocalPng("temp/image.png")
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid).withTeemakuva(Some(s"$PublicImageServer/temp/image.png")))

    get(oid, defaultOppilaitoksenOsa.withTeemakuva(Some(s"$PublicImageServer/oppilaitoksen-osa-teemakuva/$oid/image.png")))

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"oppilaitoksen-osa-teemakuva/$oid/image.png"))
    MockS3Client.getLocal("konfo-files", s"temp/image.png") shouldBe empty
  }

  it should "not touch an image that's not in the temporary location" in {
    val oppilaitoksenOsaWithImage = oppilaitoksenOsa(oppilaitoksenOsaOid).withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))
    val oid = put(oppilaitoksenOsaWithImage)
    MockS3Client.storage shouldBe empty
    get(oid, defaultOppilaitoksenOsa.withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png")))
  }

  "Update oppilaitoksen osa" should "update oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    val lastModified = get(oid, defaultOppilaitoksenOsa)
    update(oppilaitoksenOsa(oid, Arkistoitu), lastModified)
    get(oid, defaultOppilaitoksenOsa.copy(tila = Arkistoitu))
  }

  it should "read muokkaaja from the session" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid), crudSessions(ChildOid))
    val userOid = userOidForTestSessionId(crudSessions(ChildOid))
    val lastModified = get(oid, defaultOppilaitoksenOsa.copy(muokkaaja = userOid))
    update(oppilaitoksenOsa(oid, Arkistoitu).copy(muokkaaja = userOid), lastModified)
    get(oid, defaultOppilaitoksenOsa.copy(tila = Arkistoitu, muokkaaja = testUser.oid))
  }

  it should "write oppilaitoksen osa update to audit log" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    val lastModified = get(oid, defaultOppilaitoksenOsa)
    MockAuditLogger.clean()
    update(defaultOppilaitoksenOsa.copy(tila = Arkistoitu).withModified(LocalDateTime.parse("1000-01-01T12:00:00")), lastModified)
    MockAuditLogger.findFieldChange("tila", "julkaistu", "arkistoitu", oid, "oppilaitoksen_osa_update") shouldBe defined
    MockAuditLogger.find("1000-01-01") should not be defined
  }

  it should "not update oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    val lastModified = get(oid, defaultOppilaitoksenOsa)
    MockAuditLogger.clean()
    update(defaultOppilaitoksenOsa, lastModified, false)
    MockAuditLogger.logs shouldBe empty
    get(oid, defaultOppilaitoksenOsa) should equal (lastModified)
  }

  it should "return 401 if no session is found" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    val lastModified = get(oid, defaultOppilaitoksenOsa)
    post(OppilaitoksenOsaPath, bytes(defaultOppilaitoksenOsa), Seq(KoutaServlet.IfUnmodifiedSinceHeader -> lastModified)) {
      status should equal (401)
    }
  }

  it should "not update oppilaitos oid" in {
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(GrandChildOid)).
      thenReturn(TestData.organisaationOsa.copy(oid = GrandChildOid.s))
    val oid = put(oppilaitoksenOsa(GrandChildOid.s))

    val oppilaitoksenOsaWithEnriched = defaultOppilaitoksenOsa.copy(
      oid = OrganisaatioOid(oid),
      oppilaitosOid = Some(OrganisaatioOid(oppilaitosOid)),
      _enrichedData = Some(defaulOppilaitosEnrichedData.copy(
        organisaatio = Some(defaultOrganisaatio.copy(oid = oid))
      )))
    val lastModified = get(oid, oppilaitoksenOsaWithEnriched)

    val newOppilaitosOid = put(oppilaitos(randomOrganisaatioOid.s))

    update(oppilaitoksenOsa(oid).copy(oppilaitosOid = Some(OrganisaatioOid(newOppilaitosOid))), lastModified, 200, crudSessions(ParentOid))
    get(oid, oppilaitoksenOsaWithEnriched.copy(oppilaitosOid = Some(OrganisaatioOid(oppilaitosOid))))
  }

  it should "allow a user of the oppilaitoksen osa organization to update the oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    val thisOppilaitoksenOsa = defaultOppilaitoksenOsa
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, false, crudSessions(oppilaitoksenOsa.organisaatioOid))
  }

  it should "deny a user without access to the oppilaitoksen osa organization" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    val thisOppilaitoksenOsa = defaultOppilaitoksenOsa
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, 403, crudSessions(LonelyOid))
  }

  it should "allow a user of an ancestor organization to create the oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    val thisOppilaitoksenOsa = defaultOppilaitoksenOsa
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, false, crudSessions(ParentOid))
  }

  it should "deny a user with only access to a descendant organization" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    val thisOppilaitoksenOsa = defaultOppilaitoksenOsa
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, 403, crudSessions(GrandChildOid))
  }

  it should "deny a user with the wrong role" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    val thisOppilaitoksenOsa = defaultOppilaitoksenOsa
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, 403, readSessions(oppilaitoksenOsa.organisaatioOid))
  }

  it should "deny indexer access" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    val thisOppilaitoksenOsa = defaultOppilaitoksenOsa
    val lastModified = get(oid, thisOppilaitoksenOsa)
    update(thisOppilaitoksenOsa, lastModified, 403, indexerSession)
  }

  it should "fail update if 'x-If-Unmodified-Since' header is missing" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    post(OppilaitoksenOsaPath, bytes(defaultOppilaitoksenOsa), defaultHeaders) {
      status should equal (400)
      body should include (KoutaServlet.IfUnmodifiedSinceHeader)
    }
  }

  it should "fail update if modified in between get and update" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    val lastModified = get(oid, defaultOppilaitoksenOsa)
    Thread.sleep(1500)
    update(oppilaitoksenOsa(oid, Arkistoitu), lastModified)
    post(OppilaitoksenOsaPath, bytes(defaultOppilaitoksenOsa.copy(tila = Arkistoitu)), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "store and update unfinished oppilaitoksen osa" in {
    val unfinishedOppilaitoksenOsa = TestData.MinOppilaitoksenOsa.copy(OrganisaatioOid(oppilaitoksenOsaOid), Some(OrganisaatioOid(oppilaitosOid)))
    val oid = put(unfinishedOppilaitoksenOsa)
    val unfinishedOppilaitoksenOsaWithOrg = unfinishedOppilaitoksenOsa.copy(
      oid = OrganisaatioOid(oid),
      _enrichedData = Some(OppilaitosEnrichedData(muokkaajanNimi = Some("Testi Muokkaaja"), organisaatio = Some(defaultOrganisaatio.copy(oid = oppilaitoksenOsaOid)))))
    val lastModified = get(oid, unfinishedOppilaitoksenOsaWithOrg)
    val newUnfinishedOppilaitoksenOsa = unfinishedOppilaitoksenOsa.copy(oid = OrganisaatioOid(oid), organisaatioOid = LonelyOid)
    update(newUnfinishedOppilaitoksenOsa, lastModified)
    get(oid, unfinishedOppilaitoksenOsaWithOrg.copy(organisaatioOid = LonelyOid))
  }

  it should "validate updated oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    val lastModified = get(oid, defaultOppilaitoksenOsa)
    post(OppilaitoksenOsaPath, bytes(oppilaitoksenOsa(oid).copy(organisaatioOid = OrganisaatioOid("saippua"))), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validationErrorBody(validationMsg("saippua"), "organisaatioOid"))
    }
  }

  it should "copy a temporary image to a permanent location while updating the oppilaitoksen osa" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    val lastModified = get(oid, defaultOppilaitoksenOsa)

    saveLocalPng("temp/image.png")
    val oppilaitoksenOsaWithImage = oppilaitoksenOsa(oid).withTeemakuva(Some(s"$PublicImageServer/temp/image.png"))

    update(oppilaitoksenOsaWithImage, lastModified)
    get(oid, defaultOppilaitoksenOsa.withTeemakuva(Some(s"$PublicImageServer/oppilaitoksen-osa-teemakuva/$oid/image.png")))

    checkLocalPng(MockS3Client.getLocal("konfo-files", s"oppilaitoksen-osa-teemakuva/$oid/image.png"))
  }

  it should "not touch an image that's not in the temporary location" in {
    val oid = put(oppilaitoksenOsa(oppilaitoksenOsaOid))
    val lastModified = get(oid, defaultOppilaitoksenOsa)
    val oppilaitoksenOsaWithImage = oppilaitoksenOsa(oid).withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))

    update(oppilaitoksenOsaWithImage, lastModified)

    MockS3Client.storage shouldBe empty
    get(oid, defaultOppilaitoksenOsa.withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png")))
  }

  it should "not change other oppilaitoksen osa while updating teemakuva" in {
    val oid1 = put(oppilaitoksenOsa(oppilaitoksenOsaOid))

    val oppilaitoksenOsaOid2 = TestOids.EvilChildOid.toString
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(OrganisaatioOid(oppilaitoksenOsaOid2))).
      thenReturn(TestData.organisaationOsa.copy(oid = oppilaitoksenOsaOid2))
    val oid2 = put(oppilaitoksenOsa(oppilaitoksenOsaOid2))

    val lastModified = get(oid1, defaultOppilaitoksenOsa)
    val oppilaitoksenOsaWithImage = oppilaitoksenOsa(oid1).withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png"))

    update(oppilaitoksenOsaWithImage, lastModified)

    get(oid1, defaultOppilaitoksenOsa.withTeemakuva(Some(s"$PublicImageServer/kuvapankki-tai-joku/image.png")))

    get(oid2, defaultOppilaitoksenOsa.copy(
      oid = OrganisaatioOid(oid2),
      _enrichedData = Some(defaulOppilaitosEnrichedData.copy(organisaatio = Some(defaultOrganisaatio.copy(oid = oid2))))))
  }
}
