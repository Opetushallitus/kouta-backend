package fi.oph.kouta.service

import fi.oph.kouta.TestOids._
import fi.oph.kouta.config.KoutaCommonConfigFactory
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain._
import fi.oph.kouta.mocks.SpecWithMocks

class OrganisaatioServiceSpec extends SpecWithMocks with OrganisaatioFixture {

  "getAllChildOidsAndOppilaitostyypitFlat" should "return flat list of child organisations" in {
    organisaatioService.getAllChildOidsAndKoulutustyypitFlat(ChildOid)._1 should contain theSameElementsAs
      List(ChildOid, GrandChildOid, EvilGrandChildOid)
  }

  it should "return the root organization oid when called with the root organization oid" in {
    val response = organisaatioService.getAllChildOidsAndKoulutustyypitFlat(OphOid)
    response._1 should contain theSameElementsAs List(OphOid)
    response._2 should contain theSameElementsAs Koulutustyyppi.values
  }

  it should "return empty list with unknown oid when requesting children" in {
    organisaatioService
      .getAllChildOidsAndKoulutustyypitFlat(OrganisaatioOid("1.2.3"))
      ._1 should contain theSameElementsAs List()
  }
  it should "return a flat list of oppilaitostyyppi present in parents and children" in {
    organisaatioService.getAllChildOidsAndKoulutustyypitFlat(ChildOid)._2 should contain theSameElementsAs
      List(
        Amm,
        AmmTutkinnonOsa,
        AmmOsaamisala,
        AmmMuu,
        Lk,
        Muu,
        Tuva,
        Telma,
        VapaaSivistystyoOpistovuosi,
        VapaaSivistystyoMuu,
        AikuistenPerusopetus
      )
  }
  it should "return a flat list of oppilaitostyyppi present in parents and children 2" in {
    organisaatioService.getAllChildOidsAndKoulutustyypitFlat(YoOid)._2 should contain theSameElementsAs List(Yo, KkOpintojakso, KkOpintokokonaisuus, Erikoistumiskoulutus)
  }

  it should "return a flat list of oppilaitostyyppi present in parents and children 3" in {
    organisaatioService.getAllChildOidsAndKoulutustyypitFlat(GrandChildOid)._2 should contain theSameElementsAs
      List(
        Amm,
        AmmTutkinnonOsa,
        AmmOsaamisala,
        AmmMuu,
        Lk,
        Muu,
        Tuva,
        Telma,
        VapaaSivistystyoOpistovuosi,
        VapaaSivistystyoMuu,
        AikuistenPerusopetus
      )
  }

  it should "return correct oppilaitostyypit for koulutustoimija when requesting only children" in {
    organisaatioService.getAllChildOidsAndKoulutustyypitFlat(OrganisaatioOid("1.2.246.562.10.53814745062"))._2 should
      contain theSameElementsAs List(Yo, KkOpintojakso, KkOpintokokonaisuus, Erikoislaakari, Erikoistumiskoulutus, OpePedagOpinnot, Lk, Muu, Tuva, AikuistenPerusopetus)
  }

  it should "return correct oppilaitostyypit for koulutustoimija when requesting both parents and children" in {
    organisaatioService
      .getAllChildAndParentOidsWithKoulutustyypitFlat(OrganisaatioOid("1.2.246.562.10.53814745062"))
      ._2 should contain theSameElementsAs List(Yo, KkOpintojakso, KkOpintokokonaisuus, Erikoislaakari, Erikoistumiskoulutus, OpePedagOpinnot, Lk, Muu, Tuva, AikuistenPerusopetus)
  }

  "findUnknownOrganisaatioOidsFromHierarkia" should "return correct organisaatioOids" in {
    organisaatioService.findUnknownOrganisaatioOidsFromHierarkia(Set(ChildOid, OrganisaatioOid("1.2.246.562.10.121212121212"))) should equal(Right(Set(OrganisaatioOid("1.2.246.562.10.121212121212"))))
  }
}
