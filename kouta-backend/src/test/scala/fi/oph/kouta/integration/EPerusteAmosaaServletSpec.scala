package fi.oph.kouta.integration

import com.github.tomakehurst.wiremock.client.WireMock._
import fi.oph.kouta.TestOids._
import fi.oph.kouta.servlet.EPerusteAmosaaServlet

import scala.collection.JavaConverters._

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

  private def amosaaRequestOrganisaatiot(): Seq[Seq[String]] =
    mocker.mockServer.get
      .findAll(getRequestedFor(urlPathEqualTo(amosaaOpetussuunnitelmatPath)))
      .asScala
      .map { request =>
        val param = request.queryParameter("organisaatio")
        if (param.isPresent) param.values().asScala.toSeq else Seq()
      }
      .toSeq

  private def verifyAmosaaCalledWithOrganisaatiot(oids: String*): Unit = {
    val requests = amosaaRequestOrganisaatiot()
    requests should have size 1
    requests.head should contain theSameElementsAs oids
  }

  "GET /eperuste-amosaa/opetussuunnitelmat" should "pass ammatillinen oppilaitos OID to amosaa" in {
    get(s"/eperuste-amosaa/opetussuunnitelmat?organisaatiot=${ChildOid.s}", headers = Seq(defaultSessionHeader)) {
      status should equal(200)
    }
    verifyAmosaaCalledWithOrganisaatiot(ChildOid.s)
  }

  it should "pass koulutustoimija OID and its ammatilliset oppilaitokset to amosaa" in {
    get(s"/eperuste-amosaa/opetussuunnitelmat?organisaatiot=${ParentOid.s}", headers = Seq(defaultSessionHeader)) {
      status should equal(200)
    }
    verifyAmosaaCalledWithOrganisaatiot(ParentOid.s, ChildOid.s)
  }

  it should "pass the ammatillinen oppilaitos above toimipiste to amosaa" in {
    get(s"/eperuste-amosaa/opetussuunnitelmat?organisaatiot=${GrandChildOid.s}", headers = Seq(defaultSessionHeader)) {
      status should equal(200)
    }
    verifyAmosaaCalledWithOrganisaatiot(ChildOid.s)
  }

  it should "call amosaa without organisaatio restriction when no organisaatiot given" in {
    get(s"/eperuste-amosaa/opetussuunnitelmat", headers = Seq(defaultSessionHeader)) {
      status should equal(200)
    }
    verifyAmosaaCalledWithOrganisaatiot()
  }

  it should "return empty list without calling amosaa for non-ammatillinen oppilaitos" in {
    get(s"/eperuste-amosaa/opetussuunnitelmat?organisaatiot=${YoOid.s}", headers = Seq(defaultSessionHeader)) {
      status should equal(200)
      body should include(""""data":[]""")
    }
    amosaaRequestOrganisaatiot() shouldBe empty
  }

  it should "return empty list without calling amosaa for toimipiste under non-ammatillinen oppilaitos" in {
    get(s"/eperuste-amosaa/opetussuunnitelmat?organisaatiot=${LutYoChildOid.s}", headers = Seq(defaultSessionHeader)) {
      status should equal(200)
      body should include(""""data":[]""")
    }
    amosaaRequestOrganisaatiot() shouldBe empty
  }
}
