package fi.oph.kouta.client

import fi.oph.kouta.Templates
import fi.oph.kouta.TestSetups.{CONFIG_PROFILE_TEMPLATE, SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, SYSTEM_PROPERTY_NAME_TEMPLATE}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.mocks.KoodistoServiceMock
import org.scalatra.test.scalatest.ScalatraFlatSpec

import java.lang.Thread.sleep
import scala.concurrent.duration.DurationInt

class EPerusteKoodiClientSpec extends ScalatraFlatSpec with KoodistoServiceMock {
  var koodiClient: EPerusteKoodiClient = _

  override def beforeAll() = {
    System.setProperty(SYSTEM_PROPERTY_NAME_TEMPLATE, Templates.DEFAULT_TEMPLATE_FILE_PATH)
    System.setProperty(SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, CONFIG_PROFILE_TEMPLATE)
    super.startServiceMocking()
    urlProperties = Some(KoutaConfigurationFactory.configuration.urlProperties.addOverride("host.virkailija", s"localhost:$mockPort"))
    koodiClient = new EPerusteKoodiClient(urlProperties.get, 3.seconds)
  }

  "Querying koulutusKoodiUrit for ePeruste" should "return koodiurit if ePeruste was valid" in {
    mockKoulutusKoodiUritForEPerusteResponse(11L, None, Seq("koulutus_371101", "koulutus_371102"))
    mockKoulutusKoodiUritForEPerusteResponse(123L, Some(System.currentTimeMillis() - (5 * 60 * 1000)), Seq("koulutus_371105", "koulutus_371106"))
    koodiClient.getKoulutusKoodiUritForEPeruste(11L) should equal(Seq(KoodiUri("koulutus_371101", 1), KoodiUri("koulutus_371102", 1)))
    koodiClient.getKoulutusKoodiUritForEPeruste(123L) should equal(Seq[KoodiUri]())
    koodiClient.getKoulutusKoodiUritForEPeruste(100L) should equal(Seq[KoodiUri]())
    clearServiceMocks()
    mockKoulutusKoodiUritForEPerusteResponse(11L, None, Seq("koulutus_371107", "koulutus_371108"))
    // Should still use values from cache
    koodiClient.getKoulutusKoodiUritForEPeruste(11L) should equal(Seq(KoodiUri("koulutus_371101", 1), KoodiUri("koulutus_371102", 1)))
  }

  "Querying tutkinnonosa viitteet and id:t for ePeruste" should "return viitteet and id:t if ePeruste was existing" in {
    mockTutkinnonOsatByEPeruste(123L, Seq((122L, 1234L)))
    koodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(123L) should equal(Seq((122L, 1234L)))
    koodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(11L) should equal(Seq[(Long, Long)]())
    clearServiceMocks()
    mockTutkinnonOsatByEPeruste(123L, Seq((125L, 1235L)))
    // Should still use values from cache
    koodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(123L) should equal(Seq((122L, 1234L)))
  }

  "Querying osaamisalaKoodiurit for ePeruste" should "return koodiurit if ePeruste was existing" in {
    mockOsaamisalaKoodiUritByEPeruste(11L, Seq("osaamisala_01", "osaamisala_02"))
    koodiClient.getOsaamisalaKoodiuritForEPeruste(11L) should equal(Seq(KoodiUri("osaamisala_01", 1), KoodiUri("osaamisala_02", 1)))
    koodiClient.getOsaamisalaKoodiuritForEPeruste(123L) should equal(Seq[KoodiUri]())
    clearServiceMocks()
    mockOsaamisalaKoodiUritByEPeruste(11L, Seq("osaamisala_03", "osaamisala_04"))
    // Should still use values from cache
    koodiClient.getOsaamisalaKoodiuritForEPeruste(11L) should equal(Seq(KoodiUri("osaamisala_01", 1), KoodiUri("osaamisala_02", 1)))
  }

  "After cache TTL is expired" should "data fetched to cache again" in {
    mockKoulutusKoodiUritForEPerusteResponse(11L, None, Seq("koulutus_371101", "koulutus_371102"))
    mockTutkinnonOsatByEPeruste(123L, Seq((122L, 1234L)))
    mockOsaamisalaKoodiUritByEPeruste(11L, Seq("osaamisala_01", "osaamisala_02"))
    koodiClient.getKoulutusKoodiUritForEPeruste(11L) should equal(Seq(KoodiUri("koulutus_371101", 1), KoodiUri("koulutus_371102", 1)))
    koodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(123L) should equal(Seq((122L, 1234L)))
    koodiClient.getOsaamisalaKoodiuritForEPeruste(11L) should equal(Seq(KoodiUri("osaamisala_01", 1), KoodiUri("osaamisala_02", 1)))

    // Odotetaan että cache TTL expiroituu
    sleep(5000)

    clearServiceMocks()
    mockKoulutusKoodiUritForEPerusteResponse(11L, None, Seq("koulutus_371107", "koulutus_371108"))
    mockTutkinnonOsatByEPeruste(123L, Seq((125L, 1235L)))
    mockOsaamisalaKoodiUritByEPeruste(11L, Seq("osaamisala_03", "osaamisala_04"))
    koodiClient.getKoulutusKoodiUritForEPeruste(11L) should equal(Seq(KoodiUri("koulutus_371107", 1), KoodiUri("koulutus_371108", 1)))
    koodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(123L) should equal(Seq((125L, 1235L)))
    koodiClient.getOsaamisalaKoodiuritForEPeruste(11L) should equal(Seq(KoodiUri("osaamisala_03", 1), KoodiUri("osaamisala_04", 1)))
  }
}