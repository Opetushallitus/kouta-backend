package fi.oph.kouta.client

import fi.oph.kouta.OrganisaatioServiceMock
import fi.oph.kouta.domain.oid.OrganisaatioOid

class OrganisaatioClientSpec extends KoutaClientSpec with OrganisaatioServiceMock {

  "OrganisaatioClient" should "return flat list of child organisations" in {
    mockOrganisaatioResponse(ChildOid)
    OrganisaatioClient.getAllChildOidsFlat(ChildOid) should contain theSameElementsAs (
      List(ChildOid, GrandChildOid, EvilGrandChildOid))
  }
  it should "return empty list with unknown oid when requesting children" in {
    mockOrganisaatioResponse(OrganisaatioOid("1.2.3"), NotFoundOrganisaatioResponse)
    OrganisaatioClient.getAllChildOidsFlat(OrganisaatioOid("1.2.3")) should contain theSameElementsAs(List())
  }

}
