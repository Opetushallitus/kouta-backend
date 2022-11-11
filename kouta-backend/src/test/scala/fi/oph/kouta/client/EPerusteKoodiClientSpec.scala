package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.mocks.{KoodistoServiceMock, SpecWithMocks, UrlProperties}

class EPerusteKoodiClientSpec extends SpecWithMocks with KoodistoServiceMock {
import fi.oph.kouta.domain.{En, Fi, Kielistetty, Sv}

  var koodiClient: EPerusteKoodiClient = _
  KoutaConfigurationFactory.setupWithDefaultTemplateFile()
  setUrlProperties(KoutaConfigurationFactory.configuration.urlProperties)
  val defaultNimi: Kielistetty = Map(Fi -> "nimi", Sv -> "nimi sv", En -> "nimi en")

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

  "Querying tutkinnonosa viitteet, id:t and nimi for ePerusteet" should "return viitteet, id:t and nimi if ePeruste was existing" in {
    mockTutkinnonOsatByEPeruste(200, Seq((100, 1000)))
    mockTutkinnonOsatByEPeruste(201, Seq((101, 1001), (102, 1002)))
    mockTutkinnonOsatByEPeruste(202, Seq((103, 1003)))

    def item(id: Long, viiteId: Long): TutkinnonOsaServiceItem = TutkinnonOsaServiceItem(id, viiteId, defaultNimi)

    val expectedVal = Map(200 -> Seq(item(1000, 100)), 201 -> Seq(item(1001, 101), item(1002, 102)), 202 -> Seq(item(1003, 103)), 203 -> Seq())
    koodiClient.getTutkinnonosatForEPerusteetFromCache(Seq(200, 201, 201, 202, 203)) should equal(Right(expectedVal))
    koodiClient.getTutkinnonosatForEPerusteetFromCache(Seq(11)) should equal(Right(Map(11 -> Seq())))
    clearServiceMocks()
    mockTutkinnonOsatByEPeruste(200, Seq((125L, 1235L)))
    // Should still use values from cache
    koodiClient.getTutkinnonosatForEPerusteetFromCache(Seq(200)) should equal(Right(Map(200 -> Seq(item(1000, 100)))))
  }

  "Querying osaamisalaKoodiurit for ePeruste" should "return koodiurit if ePeruste was existing" in {
    mockOsaamisalaKoodiUritByEPeruste(11L, Seq("osaamisala_01", "osaamisala_02"))
    koodiClient.getOsaamisalaKoodiuritForEPerusteFromCache(11L) should equal(Right(Seq(KoodiUri("osaamisala_01", 1, defaultNimi), KoodiUri("osaamisala_02", 1, defaultNimi))))
    koodiClient.getOsaamisalaKoodiuritForEPerusteFromCache(123L) should equal(Right(Seq[KoodiUri]()))
    clearServiceMocks()
    mockOsaamisalaKoodiUritByEPeruste(11L, Seq("osaamisala_03", "osaamisala_04"))
    // Should still use values from cache
    koodiClient.getOsaamisalaKoodiuritForEPerusteFromCache(11L) should equal(Right(Seq(KoodiUri("osaamisala_01", 1, defaultNimi), KoodiUri("osaamisala_02", 1, defaultNimi))))
  }

  "When cache data is expired or removed" should "data fetched to cache again" in {
    mockKoulutusKoodiUritForEPerusteResponse(11L, None, Seq("koulutus_371101", "koulutus_371102"))
    mockTutkinnonOsatByEPeruste(123L, Seq((122L, 1234L)))
    mockOsaamisalaKoodiUritByEPeruste(11L, Seq("osaamisala_01", "osaamisala_02"))
    koodiClient.getKoulutusKoodiUritForEPerusteFromCache(11L) should equal(Right(Seq(KoodiUri("koulutus_371101", 1), KoodiUri("koulutus_371102", 1))))
    koodiClient.getTutkinnonosatForEPerusteetFromCache(Seq(123)) should equal(Right(Map(123 -> Seq(TutkinnonOsaServiceItem(1234, 122, defaultNimi)))))
    koodiClient.getOsaamisalaKoodiuritForEPerusteFromCache(11L) should equal(Right(Seq(KoodiUri("osaamisala_01", 1, defaultNimi), KoodiUri("osaamisala_02", 1, defaultNimi))))

    koodiClient.ePerusteToOsaamisalaCache.invalidateAll()
    koodiClient.ePerusteToKoodiuritCache.invalidateAll()
    koodiClient.ePerusteToTutkinnonosaCache.invalidateAll()

    clearServiceMocks()
    mockKoulutusKoodiUritForEPerusteResponse(11, None, Seq("koulutus_371107", "koulutus_371108"))
    mockTutkinnonOsatByEPeruste(123, Seq((125, 1235)))
    mockOsaamisalaKoodiUritByEPeruste(11, Seq("osaamisala_03", "osaamisala_04"))
    koodiClient.getKoulutusKoodiUritForEPerusteFromCache(11) should equal(Right(Seq(KoodiUri("koulutus_371107", 1), KoodiUri("koulutus_371108", 1))))
    koodiClient.getTutkinnonosatForEPerusteetFromCache(Seq(123)) should equal(Right(Map(123 -> Seq(TutkinnonOsaServiceItem(1235, 125, defaultNimi)))))
    koodiClient.getOsaamisalaKoodiuritForEPerusteFromCache(11) should equal(Right(Seq(KoodiUri("osaamisala_03", 1, defaultNimi), KoodiUri("osaamisala_04", 1, defaultNimi))))
  }

  "When koulutusKoodiUri query failed" should "return error status" in {
    mockKoulutusKoodiUritForEPerusteFailure(66)
    koodiClient.getKoulutusKoodiUritForEPerusteFromCache(66L).left.get.getMessage should
      equal("Failed to get ePerusteet with id 66 after retry, got response 500, " +
        "Failure in eperuste-service for ePerusteId 66")
  }

  "When tutkinnonosa query failed" should "return error status" in {
    mockTutkinnonOsatByEPeruste(123, Seq((125, 1235)))
    mockTutkinnonOsatFailure(66)
    koodiClient.getTutkinnonosatForEPerusteetFromCache(Seq(123, 66)).left.get.getMessage should
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