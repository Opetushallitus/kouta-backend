package fi.oph.kouta.client

import fi.oph.kouta.OrganisaatioServiceMock
import fi.oph.kouta.domain.oid.OrganisaatioOid

class OrganisaatioClientSpec extends KoutaClientSpec with OrganisaatioServiceMock {

  "OrganisaatioClient" should "return flat list of parent and child organisations" in {
    mockOrganisaatioResponse(ParentOid)
    OrganisaatioClient.getAllParentAndChildOidsFlat(OrganisaatioOid(ParentOid)) should contain theSameElementsAs(
      List(OphOid, ParentOid, ChildOid, EvilChildOid, EvilCousin, GrandChildOid, EvilGrandChildOid).map(OrganisaatioOid))
  }
  it should "not return organizations in different branches when returning parent and child organisations" in {
    mockOrganisaatioResponse(GrandChildOid)
    OrganisaatioClient.getAllParentAndChildOidsFlat(OrganisaatioOid(GrandChildOid)) should contain theSameElementsAs(
      List(OphOid, ParentOid, ChildOid, GrandChildOid).map(OrganisaatioOid))
  }
  it should "return empty list with unknown oid when requesting parent and children" in {
    mockOrganisaatioResponse("1.2.3", NotFoundOrganisaatioResponse)
    OrganisaatioClient.getAllParentAndChildOidsFlat(OrganisaatioOid("1.2.3")) should contain theSameElementsAs(List())
  }
  it should "return flat list of child organisations" in {
    mockOrganisaatioResponse(ChildOid)
    OrganisaatioClient.getAllChildOidsFlat(OrganisaatioOid(ChildOid)) should contain theSameElementsAs(
      List(ChildOid, GrandChildOid, EvilGrandChildOid).map(OrganisaatioOid))
  }
  it should "return empty list with unknown oid when requesting children" in {
    mockOrganisaatioResponse("1.2.3", NotFoundOrganisaatioResponse)
    OrganisaatioClient.getAllChildOidsFlat(OrganisaatioOid("1.2.3")) should contain theSameElementsAs(List())
  }

}
