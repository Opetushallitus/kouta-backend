package fi.oph.kouta.service

import fi.oph.kouta.client.CachedOrganisaatioHierarkiaClient
import fi.oph.kouta.config.KoutaCommonConfigFactory
import fi.oph.kouta.domain.oid.RootOrganisaatioOid
import fi.oph.kouta.mocks.{OrganisaatioServiceMock, SpecWithMocks}
import org.scalatest.BeforeAndAfterAll

trait OrganisaatioFixture extends OrganisaatioServiceMock with BeforeAndAfterAll {
  this: SpecWithMocks =>

  KoutaCommonConfigFactory.setupWithDefaultTemplateFile()
  setUrlProperties(KoutaCommonConfigFactory.configuration.urlProperties)

  var organisaatioService: OrganisaatioService = _

  case class OrganisaatioServiceImpl(organisaatioUrl: String) extends OrganisaatioService {
    case class CachedOrganisaatioHierarkiaClientImpl(organisaatioUrl: String) extends CachedOrganisaatioHierarkiaClient {
      val callerId = "kouta-common"
    }
    val cachedOrganisaatioHierarkiaClient = new CachedOrganisaatioHierarkiaClientImpl(organisaatioUrl)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    organisaatioService = OrganisaatioServiceImpl(
      s"${mocker.getMockBaseUrl(withProtocol = true)}/organisaatio-service/api/$RootOrganisaatioOid/jalkelaiset"
    )
    mockOrganisaatioResponse()
  }
}
