package fi.oph.kouta.client

class OrganisaatioClientSpec extends KoutaClientSpec {

  val oid = "1.2.3"
  val params = Map(
    "oid" -> oid,
    "aktiiviset" -> "true",
    "suunnitellut" -> "true",
    "lakkautetut" -> "false")

  def mockHierarkiaRequest(response:String) = mockGet(
    "organisaatio-service.organisaatio.hierarkia", params, response)

  "OrganisaatioClient" should "return flat list of parent and child organisations" in {
    mockHierarkiaRequest(getJsonResourceAsString("organisaatio"))
    OrganisaatioClient.getAllParentAndChildOidsFlat("1.2.3") should contain theSameElementsAs(
      List("1.2.246.562.10.594252633210", "1.2.246.562.10.81934895871", "1.2.246.562.10.67603619189"))
  }

  "OrganisaatioClient" should "return empty list with unknown oid" in {
    mockHierarkiaRequest(s"""{ "numHits": 0, "organisaatiot": []}""")
    OrganisaatioClient.getAllParentAndChildOidsFlat("1.2.3") should contain theSameElementsAs(List())
  }

}
