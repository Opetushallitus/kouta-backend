package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaAuthorizationConfigFactory
import fi.oph.kouta.mocks.{OrganisaatioServiceMock, ServiceMocks}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}

trait OrganisaatioClientFixture extends FlatSpec with BeforeAndAfterAll with BeforeAndAfterEach with Matchers with OrganisaatioServiceMock {
  this: ServiceMocks =>

  var organisaatioClient: OrganisaatioClient = _

  override def startServiceMocking(): Unit = {
    super.startServiceMocking()
    urlProperties = Some(KoutaAuthorizationConfigFactory.loadTemplatedConfiguration().addOverride("host.virkailija", s"localhost:$mockPort"))
    organisaatioClient = new OrganisaatioClient(urlProperties.get, "kouta-backend")
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
