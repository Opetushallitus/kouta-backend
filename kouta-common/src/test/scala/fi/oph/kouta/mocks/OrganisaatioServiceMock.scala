package fi.oph.kouta.mocks

import fi.oph.kouta.domain.oid.RootOrganisaatioOid

trait OrganisaatioServiceMock extends ServiceMockBase {

  val NotFoundOrganisaatioResponse = s"""{ "numHits": 0, "organisaatiot": []}"""
  lazy val DefaultResponse         = responseFromResource("organisaatio")

  def mockOrganisaatioResponse(response: String = DefaultResponse): Unit = {
    mockGet(
      getMockPath("organisaatio-service.organisaatio.oid.jalkelaiset", Some(RootOrganisaatioOid.s)),
      Map.empty,
      response
    )
  }
}
