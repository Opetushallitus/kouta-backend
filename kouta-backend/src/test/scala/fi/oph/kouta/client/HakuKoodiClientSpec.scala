package fi.oph.kouta.client

import fi.oph.kouta.Templates
import fi.oph.kouta.TestSetups.{CONFIG_PROFILE_TEMPLATE, SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, SYSTEM_PROPERTY_NAME_TEMPLATE}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.mocks.KoodistoServiceMock
import org.scalatra.test.scalatest.ScalatraFlatSpec

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HakuKoodiClientSpec extends ScalatraFlatSpec with KoodistoServiceMock {
  var koodiClient: HakuKoodiClient = _

  val dayInPast = LocalDate.now().minusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

  override def beforeAll() = {
    System.setProperty(SYSTEM_PROPERTY_NAME_TEMPLATE, Templates.DEFAULT_TEMPLATE_FILE_PATH)
    System.setProperty(SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, CONFIG_PROFILE_TEMPLATE)
    super.startServiceMocking()
    urlProperties = Some(KoutaConfigurationFactory.configuration.urlProperties.addOverride("host.virkailija", s"localhost:$mockPort"))
    koodiClient = new HakuKoodiClient(urlProperties.get)
  }

  "Finding kausiKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("kausi", Seq(("kausi_s", 1, None), ("kausi_k", 3, None), ("kausi_X", 2, Some(dayInPast))))
    koodiClient.kausiKoodiUriExists("kausi_s") should equal(true)
    koodiClient.kausiKoodiUriExists("kausi_k#2") should equal(true)
    koodiClient.kausiKoodiUriExists("kausi_X") should equal(false)
    koodiClient.kausiKoodiUriExists("kausi_Y") should equal(false)
  }

  "Finding kieliKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("kieli",
      Seq(("kieli_fi", 1, None), ("kieli_sv", 3, None),
        ("kieli_su", 2, Some(dayInPast))))
    koodiClient.kieliKoodiUriExists("kieli_fi") should equal(true)
    koodiClient.kieliKoodiUriExists("kieli_sv#2") should equal(true)
    koodiClient.kieliKoodiUriExists("kieli_su") should equal(false)
    koodiClient.kieliKoodiUriExists("kieli_xx") should equal(false)
  }
}
