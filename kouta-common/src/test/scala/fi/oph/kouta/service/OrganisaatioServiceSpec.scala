package fi.oph.kouta.service

import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Amm, Koulutustyyppi, Lk, Tuva, Telma, VapaaSivistystyoMuu, VapaaSivistystyoOpistovuosi, Yo}

class OrganisaatioServiceSpec extends OrganisaatioFixture {

  override def beforeAll(): Unit = {
    super.beforeAll()
    mockOrganisaatioResponse()
  }

  "getAllChildOidsAndOppilaitostyypitFlat" should "return flat list of child organisations" in {
    organisaatioService.getAllChildOidsAndOppilaitostyypitFlat(ChildOid)._1 should contain theSameElementsAs
      List(ChildOid, GrandChildOid, EvilGrandChildOid)
  }
  it should "return the root organization oid when called with the root organization oid" in {
    val response = organisaatioService.getAllChildOidsAndOppilaitostyypitFlat(OphOid)
    response._1 should contain theSameElementsAs List(OphOid)
    response._2 should contain theSameElementsAs Koulutustyyppi.values
  }
  it should "return empty list with unknown oid when requesting children" in {
    organisaatioService.getAllChildOidsAndOppilaitostyypitFlat(OrganisaatioOid("1.2.3"))._1 should contain theSameElementsAs List()
  }
  it should "return a flat list of oppilaitostyyppi present in parents and children" in {
    organisaatioService.getAllChildOidsAndOppilaitostyypitFlat(ChildOid)._2 should contain theSameElementsAs List(Amm, Tuva, Telma, VapaaSivistystyoOpistovuosi, VapaaSivistystyoMuu)
  }
  it should "return a flat list of oppilaitostyyppi present in parents and children 2" in {
    organisaatioService.getAllChildOidsAndOppilaitostyypitFlat(YoOid)._2 should contain theSameElementsAs List(Yo)
  }
  it should "return a flat list of oppilaitostyyppi present in parents and children 3" in {
    organisaatioService.getAllChildOidsAndOppilaitostyypitFlat(GrandChildOid)._2 should contain theSameElementsAs List(Amm, Tuva, Telma, VapaaSivistystyoOpistovuosi, VapaaSivistystyoMuu)
  }
  it should "return correct oppilaitostyypit for koulutustoimija when requesting only children" in {
    organisaatioService.getAllChildOidsAndOppilaitostyypitFlat(OrganisaatioOid("1.2.246.562.10.53814745062"))._2 should contain theSameElementsAs List(Yo, Lk, Tuva)
  }
  it should "return correct oppilaitostyypit for koulutustoimija when requesting both parents and children" in {
    organisaatioService.getAllChildAndParentOidsWithOppilaitostyypitFlat(OrganisaatioOid("1.2.246.562.10.53814745062"))._2 should contain theSameElementsAs List(Yo, Lk, Tuva)
  }
}
