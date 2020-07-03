package fi.oph.kouta.client

import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Amm, Koulutustyyppi, Lk, Yo}
import fi.oph.kouta.mocks.OrganisaatioServiceMock

class OrganisaatioClientSpec extends KoutaClientSpec with OrganisaatioServiceMock {

  override def beforeAll() = {
    super.beforeAll()
    mockOrganisaatioResponse()
  }

  "getAllChildOidsAndOppilaitostyypitFlat" should "return flat list of child organisations" in {
    OrganisaatioService.getAllChildOidsAndOppilaitostyypitFlat(ChildOid)._1 should contain theSameElementsAs
      List(ChildOid, GrandChildOid, EvilGrandChildOid)
  }
  it should "return the root organization oid when called with the root organization oid" in {
    val response = OrganisaatioService.getAllChildOidsAndOppilaitostyypitFlat(OphOid)
    response._1 should contain theSameElementsAs List(OphOid)
    response._2 should contain theSameElementsAs Koulutustyyppi.values
  }
  it should "return empty list with unknown oid when requesting children" in {
    OrganisaatioService.getAllChildOidsAndOppilaitostyypitFlat(OrganisaatioOid("1.2.3"))._1 should contain theSameElementsAs List()
  }
  it should "return a flat list of oppilaitostyyppi present in parents and children" in {
    OrganisaatioService.getAllChildOidsAndOppilaitostyypitFlat(ChildOid)._2 should contain theSameElementsAs List(Amm)
  }
  it should "return a flat list of oppilaitostyyppi present in parents and children 2" in {
    OrganisaatioService.getAllChildOidsAndOppilaitostyypitFlat(YoOid)._2 should contain theSameElementsAs List(Yo)
  }
  it should "return correct oppilaitostyypit for koulutustoimija when requesting only children" in {
    OrganisaatioService.getAllChildOidsAndOppilaitostyypitFlat(OrganisaatioOid("1.2.246.562.10.53814745062"))._2 should contain theSameElementsAs List(Yo, Lk)
  }
  it should "return correct oppilaitostyypit for koulutustoimija when requesting both parents and children" in {
    OrganisaatioService.getAllChildAndParentOidsWithOppilaitostyypitFlat(OrganisaatioOid("1.2.246.562.10.53814745062"))._2 should contain theSameElementsAs List(Yo, Lk)
  }
}