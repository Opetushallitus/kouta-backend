package fi.oph.kouta.client

import fi.oph.kouta.OrganisaatioServiceMock

class OrganisaatioClientSpec extends KoutaClientSpec with OrganisaatioServiceMock {

  "OrganisaatioClient" should "return flat list of parent and child organisations" in {
    mockOrganisaatioResponse(ParentOid)
    OrganisaatioClient.getAllParentAndChildOidsFlat(ParentOid) should contain theSameElementsAs(
      List(OphOid, ParentOid, ChildOid, EvilChildOid, EvilCousin, GrandChildOid, EvilGrandChildOid))
  }
  it should "not return organizations in different branches when returning parent and child organisations" in {
    mockOrganisaatioResponse(GrandChildOid)
    OrganisaatioClient.getAllParentAndChildOidsFlat(GrandChildOid) should contain theSameElementsAs(
      List(OphOid, ParentOid, ChildOid, GrandChildOid))
  }
  it should "return empty list with unknown oid when requesting parent and children" in {
    mockOrganisaatioResponse("1.2.3", NotFoundOrganisaatioResponse)
    OrganisaatioClient.getAllParentAndChildOidsFlat("1.2.3") should contain theSameElementsAs(List())
  }
  it should "return flat list of child organisations" in {
    mockOrganisaatioResponse(ChildOid)
    OrganisaatioClient.getAllChildOidsFlat(ChildOid) should contain theSameElementsAs(
      List(ChildOid, GrandChildOid, EvilGrandChildOid))
  }
  it should "return empty list with unknown oid when requesting children" in {
    mockOrganisaatioResponse("1.2.3", NotFoundOrganisaatioResponse)
    OrganisaatioClient.getAllChildOidsFlat("1.2.3") should contain theSameElementsAs(List())
  }

}
