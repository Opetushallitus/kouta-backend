package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaAuthorizationConfigFactory
import fi.oph.kouta.domain.oid.RootOrganisaatioOid
import fi.oph.kouta.mocks.{OrganisaatioServiceMock, ServiceMocks}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

trait OrganisaatioFixture extends AnyFlatSpec with BeforeAndAfterAll with BeforeAndAfterEach with Matchers with OrganisaatioServiceMock {
  this: ServiceMocks =>

  var organisaatioService: OrganisaatioService = _

  case class OrganisaatioServiceImpl(organisaatioUrl: String) extends  OrganisaatioService {
    case class CachedOrganisaatioHierarkiaClientImpl(organisaatioUrl: String) extends CachedOrganisaatioHierarkiaClient {
      val callerId = "kouta-authorization"
    }
    val cachedOrganisaatioHierarkiaClient = new CachedOrganisaatioHierarkiaClientImpl(organisaatioUrl)
  }

  override def startServiceMocking(): Unit = {
    super.startServiceMocking()
    urlProperties = Some(KoutaAuthorizationConfigFactory.loadTemplatedConfiguration().addOverride("host.virkailija", s"localhost:$mockPort"))
    organisaatioService = OrganisaatioServiceImpl(s"http://localhost:$mockPort/organisaatio-service/rest/organisaatio/v4/$RootOrganisaatioOid/jalkelaiset")
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
