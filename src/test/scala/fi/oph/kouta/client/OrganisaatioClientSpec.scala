package fi.oph.kouta.client

import fi.oph.kouta.OrganisaatioServiceMock

class OrganisaatioClientSpec extends KoutaClientSpec with OrganisaatioServiceMock {

  "OrganisaatioClient" should "return flat list of parent and child organisations" in {
    mockOrganisaatioServiceFromResource("1.2.246.562.10.594252633210")
    OrganisaatioClient.getAllParentAndChildOidsFlat("1.2.246.562.10.594252633210") should contain theSameElementsAs(
      List("1.2.246.562.10.594252633210", "1.2.246.562.10.81934895871", "1.2.246.562.10.67603619189"))
  }
  it should "return empty list with unknown oid" in {
    mockOrganisaatioService("1.2.3", EmptyOrganisaatioResponse)
    OrganisaatioClient.getAllParentAndChildOidsFlat("1.2.3") should contain theSameElementsAs(List())
  }

}
