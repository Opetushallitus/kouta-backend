package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.{Fi, Kielistetty, Sv}
import fi.oph.kouta.mocks.{KoodistoServiceMock, SpecWithMocks}
import fi.oph.kouta.validation.ExternalQueryResults.{itemFound, itemNotFound}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HakuKoodiClientSpec extends SpecWithMocks with KoodistoServiceMock {
  var koodiClient: HakuKoodiClient = _
  val defaultNimi: Kielistetty         = Map(Fi -> "nimi", Sv -> "nimi sv")

  val dayInPast = LocalDate.now().minusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

  KoutaConfigurationFactory.setupWithDefaultTemplateFile()
  setUrlProperties(KoutaConfigurationFactory.configuration.urlProperties)

  override def beforeAll = {
    super.beforeAll()
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
    mockKoodistoResponse(
      "hakukohteeterammatillinenerityisopetus",
      Seq(
        ("hakukohteeterammatillinenerityisopetus_01", 1, None),
        ("hakukohteeterammatillinenerityisopetus_02", 3, None),
        ("hakukohteeterammatillinenerityisopetus_XX", 2, Some(dayInPast))
      )
    )
    koodiClient.hakukohdeKoodiUriPoJalkYhteishakuExists("hakukohteetperusopetuksenjalkeinenyhteishaku_01") should equal(itemFound)
    koodiClient.hakukohdeKoodiUriPoJalkYhteishakuExists("hakukohteetperusopetuksenjalkeinenyhteishaku_02#2") should equal(itemFound)
    koodiClient.hakukohdeKoodiUriPoJalkYhteishakuExists("hakukohteetperusopetuksenjalkeinenyhteishaku_XX") should equal(itemNotFound)
    koodiClient.hakukohdeKoodiUriPoJalkYhteishakuExists("hakukohteetperusopetuksenjalkeinenyhteishaku_YY") should equal(itemNotFound)
    koodiClient.hakukohdeKoodiUriPoJalkYhteishakuExists("hakukohteetperusopetuksenjalkeinenerillishaku_01") should equal(itemNotFound)

    koodiClient.hakukohdeKoodiUriAmmErityisopetusExists("hakukohteeterammatillinenerityisopetus_01") should equal(itemFound)
    koodiClient.hakukohdeKoodiUriAmmErityisopetusExists("hakukohteeterammatillinenerityisopetus_02#2") should equal(itemFound)
    koodiClient.hakukohdeKoodiUriAmmErityisopetusExists("hakukohteeterammatillinenerityisopetus_XX") should equal(itemNotFound)
    koodiClient.hakukohdeKoodiUriAmmErityisopetusExists("hakukohteeterammatillinenerityisopetus_YY") should equal(itemNotFound)
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
    koodiClient.pohjakoulutusVaatimusKoodiUriExists("pohjakoulutusvaatimuskouta_pk") should equal(itemFound)
    koodiClient.pohjakoulutusVaatimusKoodiUriExists("pohjakoulutusvaatimuskouta_yo#2") should equal(itemFound)
    koodiClient.pohjakoulutusVaatimusKoodiUriExists("pohjakoulutusvaatimuskouta_XX") should equal(itemNotFound)
    koodiClient.pohjakoulutusVaatimusKoodiUriExists("pohjakoulutusvaatimuskouta_YY") should equal(itemNotFound)
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
    koodiClient.liiteTyyppiKoodiUriExists("liitetyypitamm_1") should equal(itemFound)
    koodiClient.liiteTyyppiKoodiUriExists("liitetyypitamm_2#2") should equal(itemFound)
    koodiClient.liiteTyyppiKoodiUriExists("liitetyypitamm_XX") should equal(itemNotFound)
    koodiClient.liiteTyyppiKoodiUriExists("liitetyypitamm_YY") should equal(itemNotFound)
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
    koodiClient.valintakoeTyyppiKoodiUriExists("valintakokeentyyppi_1") should equal(itemFound)
    koodiClient.valintakoeTyyppiKoodiUriExists("valintakokeentyyppi_2#2") should equal(itemFound)
    koodiClient.valintakoeTyyppiKoodiUriExists("valintakokeentyyppi_XX") should equal(itemNotFound)
    koodiClient.valintakoeTyyppiKoodiUriExists("valintakokeentyyppi_YY") should equal(itemNotFound)
  }

  "Finding kausiKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("kausi", Seq(("kausi_s", 1, None), ("kausi_k", 3, None), ("kausi_X", 2, Some(dayInPast))))
    koodiClient.kausiKoodiUriExists("kausi_s") should equal(itemFound)
    koodiClient.kausiKoodiUriExists("kausi_k#2") should equal(itemFound)
    koodiClient.kausiKoodiUriExists("kausi_X") should equal(itemNotFound)
    koodiClient.kausiKoodiUriExists("kausi_Y") should equal(itemNotFound)
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
    koodiClient.oppiaineKoodiUriExists("painotettavatoppiaineetlukiossa_b3pt") should equal(itemFound)
    koodiClient.oppiaineKoodiUriExists("painotettavatoppiaineetlukiossa_b1lt#2") should equal(itemFound)
    koodiClient.oppiaineKoodiUriExists("painotettavatoppiaineetlukiossa_XX") should equal(itemNotFound)
    koodiClient.oppiaineKoodiUriExists("painotettavatoppiaineetlukiossa_YY") should equal(itemNotFound)
  }

  "Finding custom painotettavat oppiaineKoodiUrit" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "painotettavatoppiaineetlukiossa",
      Seq(
        ("painotettavatoppiaineetlukiossa_b3pt", 1, None),
        ("painotettavatoppiaineetlukiossa_b1lt", 3, None),
        ("painotettavatoppiaineetlukiossa_XX", 2, Some(dayInPast))
      )
    )
    koodiClient.oppiaineKoodiUriExists("painotettavatoppiaineetlukiossa_a1") should equal(itemFound)
    koodiClient.oppiaineKoodiUriExists("painotettavatoppiaineetlukiossa_b3") should equal(itemFound)
    koodiClient.oppiaineKoodiUriExists("painotettavatoppiaineetlukiossa_X1") should equal(itemNotFound)
    koodiClient.oppiaineKoodiUriExists("painotettavatoppiaineetpäiväkodissa_X1") should equal(itemNotFound)
  }

  "Finding kieliKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("kieli", Seq(("kieli_fi", 1, None), ("kieli_sv", 3, None), ("kieli_su", 2, Some(dayInPast))))
    koodiClient.kieliKoodiUriExists("kieli_fi") should equal(itemFound)
    koodiClient.kieliKoodiUriExists("kieli_sv#2") should equal(itemFound)
    koodiClient.kieliKoodiUriExists("kieli_su") should equal(itemNotFound)
    koodiClient.kieliKoodiUriExists("kieli_xx") should equal(itemNotFound)
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
    koodiClient.postiosoitekoodiExists("posti_37150") should equal(itemFound)
    koodiClient.postiosoitekoodiExists("posti_90810#2") should equal(itemFound)
    koodiClient.postiosoitekoodiExists("posti_XXXXX") should equal(itemNotFound)
    koodiClient.postiosoitekoodiExists("posti_YYYYY") should equal(itemNotFound)
  }

  "Finding hakutapaKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "hakutapa",
      Seq(
        ("hakutapa_01", 1, None),
        ("hakutapa_02", 3, None),
        ("hakutapa_66", 2, Some(dayInPast))
      )
    )
    koodiClient.hakutapaKoodiUriExists("hakutapa_01") should equal(itemFound)
    koodiClient.hakutapaKoodiUriExists("hakutapa_02#2") should equal(itemFound)
    koodiClient.hakutapaKoodiUriExists("hakutapa_66") should equal(itemNotFound)
    koodiClient.hakutapaKoodiUriExists("hakutapa_99") should equal(itemNotFound)
  }

  "Finding haunkohdejoukkoKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "haunkohdejoukko",
      Seq(
        ("haunkohdejoukko_17", 1, None),
        ("haunkohdejoukko_18", 3, None),
        ("haunkohdejoukko_66", 2, Some(dayInPast))
      )
    )
    koodiClient.haunkohdejoukkoKoodiUriExists("haunkohdejoukko_17") should equal(itemFound)
    koodiClient.haunkohdejoukkoKoodiUriExists("haunkohdejoukko_18#2") should equal(itemFound)
    koodiClient.haunkohdejoukkoKoodiUriExists("haunkohdejoukko_66") should equal(itemNotFound)
    koodiClient.haunkohdejoukkoKoodiUriExists("haunkohdejoukko_99") should equal(itemNotFound)
  }

  "Finding haunkohdejoukonTarkenneKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "haunkohdejoukontarkenne",
      Seq(
        ("haunkohdejoukontarkenne_1", 1, None),
        ("haunkohdejoukontarkenne_2", 3, None),
        ("haunkohdejoukontarkenne_6", 2, Some(dayInPast))
      )
    )
    koodiClient.haunkohdejoukonTarkenneKoodiUriExists("haunkohdejoukontarkenne_1") should equal(itemFound)
    koodiClient.haunkohdejoukonTarkenneKoodiUriExists("haunkohdejoukontarkenne_2#2") should equal(itemFound)
    koodiClient.haunkohdejoukonTarkenneKoodiUriExists("haunkohdejoukontarkenne_6") should equal(itemNotFound)
    koodiClient.haunkohdejoukonTarkenneKoodiUriExists("haunkohdejoukontarkenne_9") should equal(itemNotFound)
  }

  "Finding valintatapaKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "valintatapajono",
      Seq(
        ("valintatapajono_av", 1, None),
        ("valintatapajono_tv", 3, None),
        ("valintatapajono_km", 2, Some(dayInPast))
      )
    )
    koodiClient.valintatapaKoodiUriExists("valintatapajono_av") should equal(itemFound)
    koodiClient.valintatapaKoodiUriExists("valintatapajono_tv#2") should equal(itemFound)
    koodiClient.valintatapaKoodiUriExists("valintatapajono_km") should equal(itemNotFound)
    koodiClient.valintatapaKoodiUriExists("valintatapajono_xx") should equal(itemNotFound)
  }

  "Getting latest version of koodiUri" should "return version from cache" in {
    mockLatestKoodiUriResponse("hakukohteeterammatillinenerityisopetus_1753", 2)
    koodiClient.getKoodiUriVersionOrLatestFromCache("hakukohteeterammatillinenerityisopetus_1753") should equal(
      Right(KoodiUri("hakukohteeterammatillinenerityisopetus_1753", 2, defaultNimi))
    )
    clearServiceMocks()
    mockLatestKoodiUriResponse("hakukohteeterammatillinenerityisopetus_1753", 1)
    // Should still use value from cache
    koodiClient.getKoodiUriVersionOrLatestFromCache("hakukohteeterammatillinenerityisopetus_1753") should equal(
      Right(KoodiUri("hakukohteeterammatillinenerityisopetus_1753", 2, defaultNimi))
    )
  }

  "Getting certain version of koodiUri" should "return version from cache" in {
    mockKoodiUriVersionResponse("hakukohteetperusopetuksenjalkeinenyhteishaku_122", 3)
    koodiClient.getKoodiUriVersionOrLatestFromCache("hakukohteetperusopetuksenjalkeinenyhteishaku_122#3") should equal(
      Right(KoodiUri("hakukohteetperusopetuksenjalkeinenyhteishaku_122", 3, defaultNimi))
    )
  }

}
