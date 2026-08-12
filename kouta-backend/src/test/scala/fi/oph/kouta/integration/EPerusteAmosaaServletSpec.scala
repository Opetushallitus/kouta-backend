package fi.oph.kouta.integration

import com.github.tomakehurst.wiremock.client.WireMock._
import fi.oph.kouta.TestOids._
import fi.oph.kouta.servlet.EPerusteAmosaaServlet

class EPerusteAmosaaServletSpec extends KoutaIntegrationSpec {

  addServlet(new EPerusteAmosaaServlet(), "/eperuste-amosaa")

  private val amosaaOpetussuunnitelmatPath = "/eperusteet-amosaa-service/api/external/opetussuunnitelmat"

  override def beforeAll(): Unit = {
    super.beforeAll()
    mockGet(amosaaOpetussuunnitelmatPath, Map.empty, """{"data": []}""")
  }

  override def afterEach(): Unit = {
    super.afterEach()
    mocker.mockServer.foreach(_.resetRequests())
  }

  private def verifyAmosaaCalledWithOrganisaatio(oid: String): Unit =
    mocker.mockServer.get.verify(
      getRequestedFor(urlPathEqualTo(amosaaOpetussuunnitelmatPath))
        .withQueryParam("organisaatio", equalTo(oid))
    )

  "GET /eperuste-amosaa/opetussuunnitelmat" should "resolve oppilaitos OID to koulutustoimija before calling amosaa" in {
    get(s"/eperuste-amosaa/opetussuunnitelmat?organisaatiot=${ChildOid.s}", headers = Seq(defaultSessionHeader)) {
      status should equal(200)
    }
    verifyAmosaaCalledWithOrganisaatio(ParentOid.s)
  }

  it should "pass koulutustoimija OID directly to amosaa unchanged" in {
    get(s"/eperuste-amosaa/opetussuunnitelmat?organisaatiot=${ParentOid.s}", headers = Seq(defaultSessionHeader)) {
      status should equal(200)
    }
    verifyAmosaaCalledWithOrganisaatio(ParentOid.s)
  }

  it should "resolve toimipiste OID to koulutustoimija before calling amosaa" in {
    get(s"/eperuste-amosaa/opetussuunnitelmat?organisaatiot=${GrandChildOid.s}", headers = Seq(defaultSessionHeader)) {
      status should equal(200)
    }
    verifyAmosaaCalledWithOrganisaatio(ParentOid.s)
  }
}
