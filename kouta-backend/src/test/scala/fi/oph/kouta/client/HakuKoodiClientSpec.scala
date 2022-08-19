package fi.oph.kouta.client

import fi.oph.kouta.Templates
import fi.oph.kouta.TestSetups.{
  CONFIG_PROFILE_TEMPLATE,
  SYSTEM_PROPERTY_NAME_CONFIG_PROFILE,
  SYSTEM_PROPERTY_NAME_TEMPLATE
}
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
    urlProperties = Some(
      KoutaConfigurationFactory.configuration.urlProperties.addOverride("host.virkailija", s"localhost:$mockPort")
    )
    koodiClient = new HakuKoodiClient(urlProperties.get)
  }

  "Finding hakukohdeKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "hakukohteetperusopetuksenjalkeinenyhteishaku",
      Seq(
        ("hakukohteetperusopetuksenjalkeinenyhteishaku_01", 1, None),
        ("hakukohteetperusopetuksenjalkeinenyhteishaku_02", 3, None),
        ("hakukohteetperusopetuksenjalkeinenyhteishaku_XX", 2, Some(dayInPast))
      )
    )
    koodiClient.hakukohdeKoodiUriExists("hakukohteetperusopetuksenjalkeinenyhteishaku_01") should equal(true)
    koodiClient.hakukohdeKoodiUriExists("hakukohteetperusopetuksenjalkeinenyhteishaku_02#2") should equal(true)
    koodiClient.hakukohdeKoodiUriExists("hakukohteetperusopetuksenjalkeinenyhteishaku_XX") should equal(false)
    koodiClient.hakukohdeKoodiUriExists("hakukohteetperusopetuksenjalkeinenyhteishaku_YY") should equal(false)
    koodiClient.hakukohdeKoodiUriExists("hakukohteetperusopetuksenjalkeinenerillishaku_01") should equal(false)
  }

  "Finding pohjakoulutusVaatimusKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "pohjakoulutusvaatimuskouta",
      Seq(
        ("pohjakoulutusvaatimuskouta_pk", 1, None),
        ("pohjakoulutusvaatimuskouta_yo", 3, None),
        ("pohjakoulutusvaatimuskouta_XX", 2, Some(dayInPast))
      )
    )
    koodiClient.pohjakoulutusVaatimusKoodiUriExists("pohjakoulutusvaatimuskouta_pk") should equal(true)
    koodiClient.pohjakoulutusVaatimusKoodiUriExists("pohjakoulutusvaatimuskouta_yo#2") should equal(true)
    koodiClient.pohjakoulutusVaatimusKoodiUriExists("pohjakoulutusvaatimuskouta_XX") should equal(false)
    koodiClient.pohjakoulutusVaatimusKoodiUriExists("pohjakoulutusvaatimuskouta_YY") should equal(false)
  }

  "Finding liiteTyyppiKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "liitetyypitamm",
      Seq(
        ("liitetyypitamm_1", 1, None),
        ("liitetyypitamm_2", 3, None),
        ("liitetyypitamm_XX", 2, Some(dayInPast))
      )
    )
    koodiClient.liiteTyyppiKoodiUriExists("liitetyypitamm_1") should equal(true)
    koodiClient.liiteTyyppiKoodiUriExists("liitetyypitamm_2#2") should equal(true)
    koodiClient.liiteTyyppiKoodiUriExists("liitetyypitamm_XX") should equal(false)
    koodiClient.liiteTyyppiKoodiUriExists("liitetyypitamm_YY") should equal(false)
  }

  "Finding valintakoeTyyppiKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "valintakokeentyyppi",
      Seq(
        ("valintakokeentyyppi_1", 1, None),
        ("valintakokeentyyppi_2", 3, None),
        ("valintakokeentyyppi_XX", 2, Some(dayInPast))
      )
    )
    koodiClient.valintakoeTyyppiKoodiUriExists("valintakokeentyyppi_1") should equal(true)
    koodiClient.valintakoeTyyppiKoodiUriExists("valintakokeentyyppi_2#2") should equal(true)
    koodiClient.valintakoeTyyppiKoodiUriExists("valintakokeentyyppi_XX") should equal(false)
    koodiClient.valintakoeTyyppiKoodiUriExists("valintakokeentyyppi_YY") should equal(false)
  }

  "Finding kausiKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("kausi", Seq(("kausi_s", 1, None), ("kausi_k", 3, None), ("kausi_X", 2, Some(dayInPast))))
    koodiClient.kausiKoodiUriExists("kausi_s") should equal(true)
    koodiClient.kausiKoodiUriExists("kausi_k#2") should equal(true)
    koodiClient.kausiKoodiUriExists("kausi_X") should equal(false)
    koodiClient.kausiKoodiUriExists("kausi_Y") should equal(false)
  }

  "Finding oppiaineKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "painotettavatoppiaineetlukiossa",
      Seq(
        ("painotettavatoppiaineetlukiossa_b3pt", 1, None),
        ("painotettavatoppiaineetlukiossa_b1lt", 3, None),
        ("painotettavatoppiaineetlukiossa_XX", 2, Some(dayInPast))
      )
    )
    koodiClient.oppiaineKoodiUriExists("painotettavatoppiaineetlukiossa_b3pt") should equal(true)
    koodiClient.oppiaineKoodiUriExists("painotettavatoppiaineetlukiossa_b1lt#2") should equal(true)
    koodiClient.oppiaineKoodiUriExists("painotettavatoppiaineetlukiossa_XX") should equal(false)
    koodiClient.oppiaineKoodiUriExists("painotettavatoppiaineetlukiossa_YY") should equal(false)
  }

  "Finding kieliKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("kieli", Seq(("kieli_fi", 1, None), ("kieli_sv", 3, None), ("kieli_su", 2, Some(dayInPast))))
    koodiClient.kieliKoodiUriExists("kieli_fi") should equal(true)
    koodiClient.kieliKoodiUriExists("kieli_sv#2") should equal(true)
    koodiClient.kieliKoodiUriExists("kieli_su") should equal(false)
    koodiClient.kieliKoodiUriExists("kieli_xx") should equal(false)
  }

  "Finding postiosoitekoodi" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "posti",
      Seq(
        ("posti_37150", 1, None),
        ("posti_90810", 3, None),
        ("posti_XXXXX", 2, Some(dayInPast))
      )
    )
    koodiClient.postiosoitekoodiExists("posti_37150") should equal(true)
    koodiClient.postiosoitekoodiExists("posti_90810#2") should equal(true)
    koodiClient.postiosoitekoodiExists("posti_XXXXX") should equal(false)
    koodiClient.postiosoitekoodiExists("posti_YYYYY") should equal(false)
  }
}
