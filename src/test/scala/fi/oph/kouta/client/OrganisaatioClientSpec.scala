package fi.oph.kouta.client

import fi.oph.kouta.OrganisaatioServiceMock
import fi.oph.kouta.domain.{Amm, Yo}
import fi.oph.kouta.domain.oid.OrganisaatioOid

class OrganisaatioClientSpec extends KoutaClientSpec with OrganisaatioServiceMock {

  val YoOid = OrganisaatioOid("1.2.246.562.10.46312206843")

  "getAllChildOidsFlat" should "return flat list of child organisations" in {
    mockOrganisaatioResponse(ChildOid)
    OrganisaatioClient.getAllChildOidsFlat(ChildOid) should contain theSameElementsAs
      List(ChildOid, GrandChildOid, EvilGrandChildOid)
  }
  "getAllChildOidsFlat" should "return flat list of child organisations 2" in {
    mockOrganisaatioResponse(ChildOid)
    OrganisaatioClient.getAllChildOidsFlat(ChildOid) should contain theSameElementsAs
      List(ChildOid, GrandChildOid, EvilGrandChildOid)
  }
  it should "return empty list with unknown oid when requesting children" in {
    mockOrganisaatioResponse(OrganisaatioOid("1.2.3"), NotFoundOrganisaatioResponse)
    OrganisaatioClient.getAllChildOidsFlat(OrganisaatioOid("1.2.3")) should contain theSameElementsAs List()
  }

  "getAllChildOidsAndOppilaitostyypitFlat" should "return flat list of child organisations" in {
    mockOrganisaatioResponse(ChildOid)
    OrganisaatioClient.getAllChildOidsAndOppilaitostyypitFlat(ChildOid)._1 should contain theSameElementsAs
      List(ChildOid, GrandChildOid, EvilGrandChildOid)
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

}
