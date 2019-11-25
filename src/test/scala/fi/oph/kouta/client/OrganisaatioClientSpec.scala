package fi.oph.kouta.client

import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Amm, Koulutustyyppi, Lk, Yo}
import fi.oph.kouta.mocks.OrganisaatioServiceMock

class OrganisaatioClientSpec extends KoutaClientSpec with OrganisaatioServiceMock {

  val YoOid = OrganisaatioOid("1.2.246.562.10.46312206843")

  "getAllChildOidsAndOppilaitostyypitFlat" should "return flat list of child organisations" in {
    mockOrganisaatioResponse(ChildOid)
    OrganisaatioClient.getAllChildOidsAndOppilaitostyypitFlat(ChildOid)._1 should contain theSameElementsAs
      List(ChildOid, GrandChildOid, EvilGrandChildOid)
  }
  it should "return the root organization oid when called with the root organization oid" in {
    mockOrganisaatioResponse(OphOid, """{"numHits": 0,"organisaatiot": []}""")
    val response = OrganisaatioClient.getAllChildOidsAndOppilaitostyypitFlat(OphOid)
    response._1 should contain theSameElementsAs List(OphOid)
    response._2 should contain theSameElementsAs Koulutustyyppi.values
  }
  it should "return empty list with unknown oid when requesting children" in {
    mockOrganisaatioResponse(OrganisaatioOid("1.2.3"), NotFoundOrganisaatioResponse)
    OrganisaatioClient.getAllChildOidsAndOppilaitostyypitFlat(OrganisaatioOid("1.2.3"))._1 should contain theSameElementsAs List()
  }
  it should "return a flat list of oppilaitostyyppi present in parents and children" in {
    mockOrganisaatioResponse(ChildOid)
    OrganisaatioClient.getAllChildOidsAndOppilaitostyypitFlat(ChildOid)._2 should contain theSameElementsAs List(Amm)
  }
  it should "return a flat list of oppilaitostyyppi present in parents and children 2" in {
    mockOrganisaatioResponse(YoOid, responseFromResource("mpkk"))
    OrganisaatioClient.getAllChildOidsAndOppilaitostyypitFlat(YoOid)._2 should contain theSameElementsAs List(Yo)
  }
  it should "return correct oppilaitostyypit for koulutustoimija when requesting only children" in {
    mockOrganisaatioResponse(OrganisaatioOid("1.2.246.562.10.53814745062"), responseFromResource("1.2.246.562.10.53814745062"))
    OrganisaatioClient.getAllChildOidsAndOppilaitostyypitFlat(OrganisaatioOid("1.2.246.562.10.53814745062"))._2 should contain theSameElementsAs List(Yo, Lk)
  }
  it should "return correct oppilaitostyypit for koulutustoimija when requesting both parents and children" in {
    mockOrganisaatioResponse(OrganisaatioOid("1.2.246.562.10.53814745062"), responseFromResource("1.2.246.562.10.53814745062"))
    OrganisaatioClient.getAllChildAndParentOidsWithOppilaitostyypitFlat(OrganisaatioOid("1.2.246.562.10.53814745062"))._2 should contain theSameElementsAs List(Yo, Lk)
  }
}
