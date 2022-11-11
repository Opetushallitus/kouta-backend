package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.mocks.{KoodistoServiceMock, SpecWithMocks, UrlProperties}

class EPerusteKoodiClientSpec extends SpecWithMocks with KoodistoServiceMock {

  var koodiClient: EPerusteKoodiClient = _
  KoutaConfigurationFactory.setupWithDefaultTemplateFile()
  setUrlProperties(KoutaConfigurationFactory.configuration.urlProperties)

  override def beforeAll() = {
    super.beforeAll()
    koodiClient = new EPerusteKoodiClient(urlProperties.get)
  }

  "Querying koulutusKoodiUrit for ePeruste" should "return koodiurit if ePeruste was valid" in {
    mockKoulutusKoodiUritForEPerusteResponse(11L, None, Seq("koulutus_371101", "koulutus_371102"))
    mockKoulutusKoodiUritForEPerusteResponse(123L, Some(System.currentTimeMillis() - (5 * 60 * 1000)), Seq("koulutus_371105", "koulutus_371106"))
    koodiClient.getKoulutusKoodiUritForEPerusteFromCache(11L) should equal(Right(Seq(KoodiUri("koulutus_371101", 1), KoodiUri("koulutus_371102", 1))))
    koodiClient.getKoulutusKoodiUritForEPerusteFromCache(123L) should equal(Right(Seq[KoodiUri]()))
    koodiClient.getKoulutusKoodiUritForEPerusteFromCache(100L) should equal(Right(Seq[KoodiUri]()))
    clearServiceMocks()
    mockKoulutusKoodiUritForEPerusteResponse(11L, None, Seq("koulutus_371107", "koulutus_371108"))
    // Should still use values from cache
    koodiClient.getKoulutusKoodiUritForEPerusteFromCache(11L) should equal(Right(Seq(KoodiUri("koulutus_371101", 1), KoodiUri("koulutus_371102", 1))))
  }

  "Querying tutkinnonosa viitteet and id:t for ePeruste" should "return viitteet and id:t if ePeruste was existing" in {
    mockTutkinnonOsatByEPeruste(123L, Seq((122L, 1234L)))
    koodiClient.getTutkinnonosaViitteetAndIdtForEPerusteFromCache(123L) should equal(Right(Seq((122L, 1234L))))
    koodiClient.getTutkinnonosaViitteetAndIdtForEPerusteFromCache(11L) should equal(Right(Seq[(Long, Long)]()))
    clearServiceMocks()
    mockTutkinnonOsatByEPeruste(123L, Seq((125L, 1235L)))
    // Should still use values from cache
    koodiClient.getTutkinnonosaViitteetAndIdtForEPerusteFromCache(123L) should equal(Right(Seq((122L, 1234L))))
  }

  "Querying osaamisalaKoodiurit for ePeruste" should "return koodiurit if ePeruste was existing" in {
    mockOsaamisalaKoodiUritByEPeruste(11L, Seq("osaamisala_01", "osaamisala_02"))
    koodiClient.getOsaamisalaKoodiuritForEPerusteFromCache(11L) should equal(Right(Seq(KoodiUri("osaamisala_01", 1), KoodiUri("osaamisala_02", 1))))
    koodiClient.getOsaamisalaKoodiuritForEPerusteFromCache(123L) should equal(Right(Seq[KoodiUri]()))
    clearServiceMocks()
    mockOsaamisalaKoodiUritByEPeruste(11L, Seq("osaamisala_03", "osaamisala_04"))
    // Should still use values from cache
    koodiClient.getOsaamisalaKoodiuritForEPerusteFromCache(11L) should equal(Right(Seq(KoodiUri("osaamisala_01", 1), KoodiUri("osaamisala_02", 1))))
  }

  "When cache data is expired or removed" should "data fetched to cache again" in {
    mockKoulutusKoodiUritForEPerusteResponse(11L, None, Seq("koulutus_371101", "koulutus_371102"))
    mockTutkinnonOsatByEPeruste(123L, Seq((122L, 1234L)))
    mockOsaamisalaKoodiUritByEPeruste(11L, Seq("osaamisala_01", "osaamisala_02"))
    koodiClient.getKoulutusKoodiUritForEPerusteFromCache(11L) should equal(Right(Seq(KoodiUri("koulutus_371101", 1), KoodiUri("koulutus_371102", 1))))
    koodiClient.getTutkinnonosaViitteetAndIdtForEPerusteFromCache(123L) should equal(Right(Seq((122L, 1234L))))
    koodiClient.getOsaamisalaKoodiuritForEPerusteFromCache(11L) should equal(Right(Seq(KoodiUri("osaamisala_01", 1), KoodiUri("osaamisala_02", 1))))

    koodiClient.ePerusteToOsaamisalaCache.invalidateAll()
    koodiClient.ePerusteToKoodiuritCache.invalidateAll()
    koodiClient.ePerusteToTutkinnonosaCache.invalidateAll()

    clearServiceMocks()
    mockKoulutusKoodiUritForEPerusteResponse(11L, None, Seq("koulutus_371107", "koulutus_371108"))
    mockTutkinnonOsatByEPeruste(123L, Seq((125L, 1235L)))
    mockOsaamisalaKoodiUritByEPeruste(11L, Seq("osaamisala_03", "osaamisala_04"))
    koodiClient.getKoulutusKoodiUritForEPerusteFromCache(11L) should equal(Right(Seq(KoodiUri("koulutus_371107", 1), KoodiUri("koulutus_371108", 1))))
    koodiClient.getTutkinnonosaViitteetAndIdtForEPerusteFromCache(123L) should equal(Right(Seq((125L, 1235L))))
    koodiClient.getOsaamisalaKoodiuritForEPerusteFromCache(11L) should equal(Right(Seq(KoodiUri("osaamisala_03", 1), KoodiUri("osaamisala_04", 1))))
  }

  "When koulutusKoodiUri query failed" should "return error status" in {
    mockKoulutusKoodiUritForEPerusteFailure(66L)
    koodiClient.getKoulutusKoodiUritForEPerusteFromCache(66L).left.get.getMessage should
      equal("Failed to get ePerusteet with id 66 after retry, got response 500, " +
        "Failure in eperuste-service for ePerusteId 66")
  }

  "When tutkinnonosa query failed" should "return error status" in {
    mockTutkinnonOsatFailure(66L)
    koodiClient.getTutkinnonosaViitteetAndIdtForEPerusteFromCache(66L).left.get.getMessage should
      equal("Failed to get tutkinnonosat for ePeruste with id 66 after retry, got response 500, " +
        "Failure in eperuste-service for tutkinnonosat by ePerusteId 66")
  }

  "When osaamisala query failed" should "return error status" in {
    mockOsaamisalaKoodiUritFailure(66L)
    koodiClient.getOsaamisalaKoodiuritForEPerusteFromCache(66L).left.get.getMessage should
      equal("Failed to get osaamisalat for ePeruste with id 66 after retry, got response 500, " +
      "Failure in eperuste-service for osaamisalat by ePerusteId 66")
  }
}