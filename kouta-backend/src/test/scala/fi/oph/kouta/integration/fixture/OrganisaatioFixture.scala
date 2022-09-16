package fi.oph.kouta.integration.fixture

import fi.oph.kouta.client.{CachedOrganisaatioHierarkiaClient, OrganisaatioResponse}
import fi.oph.kouta.domain.oid.RootOrganisaatioOid
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.mocks.{OrganisaatioServiceMock, ServiceMocks}
import fi.oph.kouta.service.OrganisaatioService
import org.json4s.jackson.JsonMethods.parse
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}


trait OrganisaatioFixture extends KoutaIntegrationSpec with BeforeAndAfterAll with BeforeAndAfterEach with Matchers with OrganisaatioServiceMock {
  this: ServiceMocks =>

  var organisaatioService = OrganisaatioServiceImpl(s"http://localhost:$mockPort/organisaatio-service/rest/organisaatio/v4/$RootOrganisaatioOid/jalkelaiset")

  case class OrganisaatioServiceImpl(organisaatioUrl: String) extends  OrganisaatioService {
    case class CachedOrganisaatioHierarkiaClientImpl(organisaatioUrl: String) extends CachedOrganisaatioHierarkiaClient {
      val callerId = "kouta-backend"

      override def getWholeOrganisaatioHierarkiaCached(): OrganisaatioResponse = {
        parse(responseFromResource("organisaatio")).extract[OrganisaatioResponse]
      }
    }
    val cachedOrganisaatioHierarkiaClient = new CachedOrganisaatioHierarkiaClientImpl(organisaatioUrl)
  }

  override def startServiceMocking(): Unit = {
    super.startServiceMocking()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    startServiceMocking()
  }

  override def afterAll(): Unit = {
    super.afterAll()
    stopServiceMocking()
  }

  override def afterEach {
    super.afterEach()
    clearServiceMocks()
  }
}
