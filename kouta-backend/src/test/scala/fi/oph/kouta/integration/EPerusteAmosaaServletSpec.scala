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

  private def verifyAmosaaCalledWithOrganisaatiot(oids: String*): Unit = {
    val requestPattern = oids.foldLeft(getRequestedFor(urlPathEqualTo(amosaaOpetussuunnitelmatPath))) {
      (pattern, oid) => pattern.withQueryParam("organisaatio", equalTo(oid))
    }
    mocker.mockServer.get.verify(requestPattern)
  }

  "GET /eperuste-amosaa/opetussuunnitelmat" should "pass both the oppilaitos OID and its koulutustoimija ancestor to amosaa" in {
    get(s"/eperuste-amosaa/opetussuunnitelmat?organisaatiot=${ChildOid.s}", headers = Seq(defaultSessionHeader)) {
      status should equal(200)
    }
    verifyAmosaaCalledWithOrganisaatiot(ChildOid.s, ParentOid.s)
  }

  it should "pass koulutustoimija OID directly to amosaa unchanged" in {
    get(s"/eperuste-amosaa/opetussuunnitelmat?organisaatiot=${ParentOid.s}", headers = Seq(defaultSessionHeader)) {
      status should equal(200)
    }
    verifyAmosaaCalledWithOrganisaatiot(ParentOid.s)
  }

  it should "pass toimipiste OID and all its ancestors up to koulutustoimija to amosaa" in {
    get(s"/eperuste-amosaa/opetussuunnitelmat?organisaatiot=${GrandChildOid.s}", headers = Seq(defaultSessionHeader)) {
      status should equal(200)
    }
    verifyAmosaaCalledWithOrganisaatiot(GrandChildOid.s, ChildOid.s, ParentOid.s)
  }
}
