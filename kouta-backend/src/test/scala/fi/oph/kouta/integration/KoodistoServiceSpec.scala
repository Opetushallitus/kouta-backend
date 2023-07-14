package fi.oph.kouta.integration

import fi.oph.kouta.client.{KoodiUri, KoodiUriUtils, KoodistoClient}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain._
import fi.oph.kouta.mocks.{KoodistoServiceMock, SpecWithMocks}
import fi.oph.kouta.service.KoodistoService
import fi.oph.kouta.validation.ExternalQueryResults.{itemFound, itemNotFound, queryFailed}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class KoodistoServiceSpec extends SpecWithMocks with KoodistoServiceMock {
  KoutaConfigurationFactory.setupWithDefaultTemplateFile()
  setUrlProperties(KoutaConfigurationFactory.configuration.urlProperties)
  var koodistoService: KoodistoService = _
  val defaultNimi: Kielistetty         = Map(Fi -> "nimi", Sv -> "nimi sv")

  val dayInPast = LocalDate.now().minusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

  override def beforeAll() = {
    super.beforeAll()
    koodistoService = new KoodistoService(new KoodistoClient(urlProperties.get))
  }

  override def afterEach() = {
    koodistoService.invalidateCaches()
    clearServiceMocks()
  }

  "Creating KoodiUri object from string" should "work according to validity rules" in {
    KoodiUriUtils.koodiUriFromString("some_1111#12") should equal(KoodiUri("some_1111", 12))
    KoodiUriUtils.koodiUriFromString("some_1111") should equal(KoodiUri("some_1111", 1))
    KoodiUriUtils.koodiUriFromString("some_1111#puppua") should equal(KoodiUri("some_1111#puppua", 1))
  }

  "Searching KoodiUri from list" should "find match if versio is equal or lower" in {
    KoodiUriUtils.koodiUriWithEqualOrHigherVersioNbrInList(
      "some_1111#12",
      Seq(KoodiUri("some_1111", 12), KoodiUri("some_2222", 1))
    ) should equal(true)
    KoodiUriUtils.koodiUriWithEqualOrHigherVersioNbrInList(
      "some_1111#10",
      Seq(KoodiUri("some_1111", 12), KoodiUri("some_2222", 1))
    ) should equal(true)
    KoodiUriUtils.koodiUriWithEqualOrHigherVersioNbrInList(
      "some_1111",
      Seq(KoodiUri("some_1111", 1), KoodiUri("some_2222", 1))
    ) should equal(true)
    KoodiUriUtils.koodiUriWithEqualOrHigherVersioNbrInList(
      "some_1111#2",
      Seq(KoodiUri("some_1111", 1), KoodiUri("some_2222", 1))
    ) should equal(false)
    KoodiUriUtils.koodiUriWithEqualOrHigherVersioNbrInList(
      "some_3333#1",
      Seq(KoodiUri("some_1111", 1), KoodiUri("some_2222", 1))
    ) should equal(false)
  }

  it should "find match regardless of the version if requested" in {
    KoodiUriUtils.koodiUriWithEqualOrHigherVersioNbrInList(
      "some_1111#12",
      Seq(KoodiUri("some_1111", 1), KoodiUri("some_2222", 1)),
      false
    ) should equal(true)
  }

  "Getting latest version of koodiUri" should "return version from cache" in {
    mockLatestKoodiUriResponse("koulutus_201101", 12)
    koodistoService.getKaannokset("koulutus_201101") should equal(Right(defaultNimi))
    clearServiceMocks()
    mockLatestKoodiUriResponse("koulutus_201101", 10)
    // Should still use value from cache
    koodistoService.getKaannokset("koulutus_201101") should equal(Right(defaultNimi))
  }

  "Getting certain version of koodiUri" should "return version from cache" in {
    mockKoodiUriVersionResponse("koulutus_201102", 11)
    koodistoService.getKaannokset("koulutus_201102#11") should equal(
      Right(defaultNimi)
    )
  }

  "Finding koulutuskoodiUri by koulutustyyppi" should "return true when koodiuri exists for any of koulutustyypit" in {
    mockKoulutustyyppiResponse(
      AmmatillisetKoulutusKoodit.koulutusTyypit.last,
      Seq(("koulutus_371101", 12, None), ("koulutus_371102", 10, None), ("koulutus_371103", 1, Some(dayInPast))),
      AmmatillisetKoulutusKoodit.koulutusTyypit.init
    )
    koodistoService.isInLisattavatKoulutukset(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371101#12") should equal(
      itemFound
    )
    koodistoService.isInLisattavatKoulutukset(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371102#9") should equal(
      itemFound
    )
    koodistoService.isInLisattavatKoulutukset(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371103") should equal(
      itemNotFound
    )
    koodistoService.isInLisattavatKoulutukset(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371102#11") should equal(
      itemNotFound
    )
    koodistoService.isInLisattavatKoulutukset(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371104#1") should equal(
      itemNotFound
    )
    clearServiceMocks()
    mockKoulutustyyppiResponse(
      AmmatillisetKoulutusKoodit.koulutusTyypit.head,
      Seq(("koulutus_371103", 12, None)),
      AmmatillisetKoulutusKoodit.koulutusTyypit.tail
    )
    // Should still use values from cache
    koodistoService.isInLisattavatKoulutukset(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371101#12") should equal(
      itemFound
    )
    koodistoService.isInLisattavatKoulutukset(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371102#9") should equal(
      itemFound
    )
    koodistoService.isInLisattavatKoulutukset(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371103") should equal(
      itemNotFound
    )
    koodistoService.isInLisattavatKoulutukset(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371102#11") should equal(
      itemNotFound
    )
    koodistoService.isInLisattavatKoulutukset(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371104#1") should equal(
      itemNotFound
    )
  }

  "Finding koulutuskoodiUri by filter" should "return true when koodiUri exists with accepted version" in {
    val filter = LukioKoulutusKoodit.koulutusKoodiUrit :+ "koulutus_111111"
    mockKoodistoResponse(
      "koulutus",
      Seq(
        ("koulutus_309902", 12, None),
        ("koulutus_301102", 3, None),
        ("koulutus_301101", 1, None),
        ("koulutus_111111", 1, Some(dayInPast))
      )
    )
    koodistoService.isLisattavaKoulutus(filter, "koulutus_309902") should equal(itemFound)
    koodistoService.isLisattavaKoulutus(filter, "koulutus_301102#2") should equal(itemFound)
    koodistoService.isLisattavaKoulutus(filter, "koulutus_111111") should equal(itemNotFound)
    koodistoService.isLisattavaKoulutus(filter, "koulutus_222222") should equal(itemNotFound)
    clearServiceMocks()
    mockKoodistoResponse("koulutus", Seq(("koulutus_222222", 12, None), ("koulutus_111111", 1, None)))
    // Should still use values from cache
    koodistoService.isLisattavaKoulutus(filter, "koulutus_309902") should equal(itemFound)
    koodistoService.isLisattavaKoulutus(filter, "koulutus_301102#2") should equal(itemFound)
    koodistoService.isLisattavaKoulutus(filter, "koulutus_111111") should equal(itemNotFound)
    koodistoService.isLisattavaKoulutus(filter, "koulutus_222222") should equal(itemNotFound)
  }

  "Finding lisatiedotOtsikkoKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "koulutuksenlisatiedot",
      Seq(
        ("koulutuksenlisatiedot_02", 1, None),
        ("koulutuksenlisatiedot_03", 2, None),
        ("koulutuksenlisatiedot_04", 2, Some(dayInPast))
      )
    )
    koodistoService.koodiUriExistsInKoodisto(KoulutuksenLisatiedotKoodisto, "koulutuksenlisatiedot_02") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(KoulutuksenLisatiedotKoodisto, "koulutuksenlisatiedot_03#1") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(KoulutuksenLisatiedotKoodisto, "koulutuksenlisatiedot_04") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(KoulutuksenLisatiedotKoodisto, "koulutuksenlisatiedot_03#3") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(KoulutuksenLisatiedotKoodisto, "koulutuksenlisatiedot_05") should equal(itemNotFound)
  }

  "Finding koulutusalaKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "kansallinenkoulutusluokitus2016koulutusalataso2",
      Seq(
        ("kansallinenkoulutusluokitus2016koulutusalataso2_010", 1, None),
        ("kansallinenkoulutusluokitus2016koulutusalataso2_020", 2, None),
        ("kansallinenkoulutusluokitus2016koulutusalataso2_080", 2, Some(dayInPast))
      )
    )
    koodistoService.koodiUriExistsInKoodisto(KoulutusalaKoodisto, "kansallinenkoulutusluokitus2016koulutusalataso2_010") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(KoulutusalaKoodisto, "kansallinenkoulutusluokitus2016koulutusalataso2_020#1") should equal(
      itemFound
    )
    koodistoService.koodiUriExistsInKoodisto(KoulutusalaKoodisto, "kansallinenkoulutusluokitus2016koulutusalataso2_080") should equal(
      itemNotFound
    )
    koodistoService.koodiUriExistsInKoodisto(KoulutusalaKoodisto,"kansallinenkoulutusluokitus2016koulutusalataso2_020#3") should equal(
      itemNotFound
    )
    koodistoService.koodiUriExistsInKoodisto(KoulutusalaKoodisto, "kansallinenkoulutusluokitus2016koulutusalataso2_050") should equal(
      itemNotFound
    )
  }

  "Finding opintojenLaajuusyksikkoKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "opintojenlaajuusyksikko",
      Seq(("opintojenlaajuusyksikko_6", 1, None), ("opintojenlaajuusyksikko_10", 2, Some(dayInPast)))
    )
    koodistoService.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto, "opintojenlaajuusyksikko_6") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto, "opintojenlaajuusyksikko_10") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto, "opintojenlaajuusyksikko_6#2") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto, "opintojenlaajuusyksikko_20") should equal(itemNotFound)
  }

  "Finding opetusKieliKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "oppilaitoksenopetuskieli",
      Seq(
        ("oppilaitoksenopetuskieli_2", 1, None),
        ("oppilaitoksenopetuskieli_3", 3, None),
        ("oppilaitoksenopetuskieli_4", 2, Some(dayInPast))
      )
    )
    koodistoService.koodiUriExistsInKoodisto(OpetuskieliKoodisto, "oppilaitoksenopetuskieli_2") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(OpetuskieliKoodisto, "oppilaitoksenopetuskieli_3#2") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(OpetuskieliKoodisto, "opintojenlaajuusyksikko_4") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(OpetuskieliKoodisto, "opintojenlaajuusyksikko_5") should equal(itemNotFound)
  }

  "Finding opetusAikaKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "opetusaikakk",
      Seq(("opetusaikakk_3", 1, None), ("opetusaikakk_4", 3, None), ("opetusaikakk_5", 2, Some(dayInPast)))
    )
    koodistoService.koodiUriExistsInKoodisto(OpetusaikaKoodisto, "opetusaikakk_3") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(OpetusaikaKoodisto,"opetusaikakk_4#2") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(OpetusaikaKoodisto,"opetusaikakk_5") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(OpetusaikaKoodisto,"opetusaikakk_6") should equal(itemNotFound)
  }

  "Finding opetusTapaKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "opetuspaikkakk",
      Seq(("opetuspaikkakk_1", 1, None), ("opetuspaikkakk_2", 3, None), ("opetuspaikkakk_3", 2, Some(dayInPast)))
    )
    koodistoService.koodiUriExistsInKoodisto(OpetustapaKoodisto, "opetuspaikkakk_1") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(OpetustapaKoodisto, "opetuspaikkakk_2#2") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(OpetustapaKoodisto, "opetuspaikkakk_3") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(OpetustapaKoodisto, "opetuspaikkakk_4") should equal(itemNotFound)
  }

  "Finding osaamisalaKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "osaamisala",
      Seq(("osaamisala_1578", 1, None), ("osaamisala_2353", 3, None), ("osaamisala_3024", 2, Some(dayInPast)))
    )
    koodistoService.koodiUriExistsInKoodisto(OsaamisalaKoodisto, "osaamisala_1578") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(OsaamisalaKoodisto, "osaamisala_2353#2") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(OsaamisalaKoodisto, "osaamisala_3024") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(OsaamisalaKoodisto, "osaamisala_2248") should equal(itemNotFound)
  }

  "Finding lukioPainotusKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "lukiopainotukset",
      Seq(
        ("lukiopainotukset_503", 1, None),
        ("lukiopainotukset_543", 3, None),
        ("lukiopainotukset_817", 2, Some(dayInPast))
      )
    )
    koodistoService.koodiUriExistsInKoodisto(LukioPainotuksetKoodisto, "lukiopainotukset_503") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(LukioPainotuksetKoodisto, "lukiopainotukset_543#2") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(LukioPainotuksetKoodisto, "lukiopainotukset_817") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(LukioPainotuksetKoodisto, "lukiopainotukset_0108") should equal(itemNotFound)
  }

  "Finding lukioErityinenKoulutustehtavaKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "lukiolinjaterityinenkoulutustehtava",
      Seq(
        ("lukiolinjaterityinenkoulutustehtava_0109", 1, None),
        ("lukiolinjaterityinenkoulutustehtava_0111", 3, None),
        ("lukiolinjaterityinenkoulutustehtava_0119", 2, Some(dayInPast))
      )
    )
    koodistoService.koodiUriExistsInKoodisto(LukioErityinenKoulutustehtavaKoodisto, "lukiolinjaterityinenkoulutustehtava_0109") should equal(
      itemFound
    )
    koodistoService.koodiUriExistsInKoodisto(LukioErityinenKoulutustehtavaKoodisto, "lukiolinjaterityinenkoulutustehtava_0111#2") should equal(
      itemFound
    )
    koodistoService.koodiUriExistsInKoodisto(LukioErityinenKoulutustehtavaKoodisto, "lukiolinjaterityinenkoulutustehtava_0119") should equal(
      itemNotFound
    )
    koodistoService.koodiUriExistsInKoodisto(LukioErityinenKoulutustehtavaKoodisto, "lukiolinjaterityinenkoulutustehtava_0126") should equal(
      itemNotFound
    )
  }

  "Finding lukioDiplomiKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "moduulikoodistolops2021",
      Seq(
        ("moduulikoodistolops2021_vka4", 1, None),
        ("moduulikoodistolops2021_vka5", 3, None),
        ("moduulikoodistolops2021_vka6", 2, Some(dayInPast))
      )
    )
    koodistoService.koodiUriExistsInKoodisto(LukioDiplomiKoodisto, "moduulikoodistolops2021_vka4") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(LukioDiplomiKoodisto, "moduulikoodistolops2021_vka5#2") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(LukioDiplomiKoodisto, "moduulikoodistolops2021_vka6") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(LukioDiplomiKoodisto, "moduulikoodistolops2021_uo6") should equal(itemNotFound)
  }

  "When cache data is expired or removed" should "data fetched to cache again" in {
    mockLatestKoodiUriResponse("kansallinenkoulutusluokitus2016koulutusalataso1_01", 2)
    mockKoulutustyyppiResponse(
      YoKoulutusKoodit.koulutusTyypit.last,
      Seq(("koulutus_201001", 12, None), ("koulutus_371101", 12, None)),
      YoKoulutusKoodit.koulutusTyypit.init
    )
    // Tutkintonimikkeiden tarkistus testataan tässä yhteydessä (ei omassa casessaan), tällöin sitä ei löydy cachesta
    // etukäteen
    mockKoodistoResponse(
      "tutkintonimikekk",
      Seq(
        ("tutkintonimikekk_110", 1, None),
        ("tutkintonimikekk_111", 3, None),
        ("tutkintonimikekk_112", 2, Some(dayInPast))
      )
    )

    koodistoService.getKaannokset("kansallinenkoulutusluokitus2016koulutusalataso1_01") should equal(
      Right(defaultNimi)
    )
    koodistoService.isInLisattavatKoulutukset(YoKoulutusKoodit.koulutusTyypit, "koulutus_201001#12") should equal(itemFound)
    koodistoService.isInLisattavatKoulutukset(YoKoulutusKoodit.koulutusTyypit, "koulutus_111111#1") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(TutkintonimikeKorkeakoulutusKoodisto, "tutkintonimikekk_110") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(TutkintonimikeKorkeakoulutusKoodisto,"tutkintonimikekk_111#2") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(TutkintonimikeKorkeakoulutusKoodisto,"tutkintonimikekk_112") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(TutkintonimikeKorkeakoulutusKoodisto,"tutkintonimikekk_111#4") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(TutkintonimikeKorkeakoulutusKoodisto,"tutkintonimikekk_120") should equal(itemNotFound)

    koodistoService.invalidateCaches()
    clearServiceMocks()
    mockLatestKoodiUriResponse("kansallinenkoulutusluokitus2016koulutusalataso1_01", 10)
    mockKoulutustyyppiResponse(
      YoKoulutusKoodit.koulutusTyypit.last,
      Seq(("koulutus_111111", 12, None)),
      YoKoulutusKoodit.koulutusTyypit.init
    )
    mockKoodistoResponse("tutkintonimikekk", Seq(("tutkintonimikekk_120", 1, None)))

    koodistoService.getKaannokset("kansallinenkoulutusluokitus2016koulutusalataso1_01") should equal(
      Right(defaultNimi)
    )
    koodistoService.isInLisattavatKoulutukset(YoKoulutusKoodit.koulutusTyypit, "koulutus_201001#12") should equal(itemNotFound)
    koodistoService.isInLisattavatKoulutukset(YoKoulutusKoodit.koulutusTyypit, "koulutus_111111#1") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(TutkintonimikeKorkeakoulutusKoodisto,"tutkintonimikekk_110") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(TutkintonimikeKorkeakoulutusKoodisto,"tutkintonimikekk_120") should equal(itemFound)
  }

  "When koodisto-query failed" should "return error status" in {
    mockKoodistoFailure("opintojenlaajuusyksikko")
    koodistoService.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto, "opintojenlaajuusyksikko_2") should equal(queryFailed)
    clearServiceMocks()
    mockKoodistoNotFound("opintojenlaajuusyksikko")
    koodistoService.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto, "opintojenlaajuusyksikko_2") should equal(itemNotFound)
    clearServiceMocks()
    mockKoodistoResponse("opintojenlaajuusyksikko", Seq(("opintojenlaajuusyksikko_2", 1, None)))
    koodistoService.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto,"opintojenlaajuusyksikko_2") should equal(itemFound)
  }

  "When getting latest version of koodiUri" should "throw error" in {
    mockLatestKoodiUriFailure("koulutus_201101")
    koodistoService.getKaannokset("koulutus_201101").left.get.getMessage should equal(
      "Failure in koodisto-service for koodiuri-base koulutus_201101"
    )
  }

  "When koodisto-query failed in koulutuskoodiUri by koulutustyyppi " should "return error status" in {
    mockKoulutustyyppiFailure(AmmatillisetKoulutusKoodit.koulutusTyypit.last)
    koodistoService.isInLisattavatKoulutukset(
      Seq(AmmatillisetKoulutusKoodit.koulutusTyypit.last),
      "koulutus_371101#12"
    ) should equal(queryFailed)
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
    koodistoService.koodiUriExistsInKoodisto(
      HakukohdePoJalkYhteishakuKoodisto,
      "hakukohteetperusopetuksenjalkeinenyhteishaku_01"
    ) should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(
      HakukohdePoJalkYhteishakuKoodisto,
      "hakukohteetperusopetuksenjalkeinenyhteishaku_02#2"
    ) should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(
      HakukohdePoJalkYhteishakuKoodisto,
      "hakukohteetperusopetuksenjalkeinenyhteishaku_XX"
    ) should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(
      HakukohdePoJalkYhteishakuKoodisto,
      "hakukohteetperusopetuksenjalkeinenyhteishaku_YY"
    ) should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(
      HakukohdePoJalkYhteishakuKoodisto,
      "hakukohteetperusopetuksenjalkeinenerillishaku_01"
    ) should equal(itemNotFound)

    koodistoService.koodiUriExistsInKoodisto(
      HakukohdeAmmErityisopetusKoodisto,
      "hakukohteeterammatillinenerityisopetus_01"
    ) should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(
      HakukohdeAmmErityisopetusKoodisto,
      "hakukohteeterammatillinenerityisopetus_02#2"
    ) should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(
      HakukohdeAmmErityisopetusKoodisto,
      "hakukohteeterammatillinenerityisopetus_XX"
    ) should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(
      HakukohdeAmmErityisopetusKoodisto,
      "hakukohteeterammatillinenerityisopetus_YY"
    ) should equal(itemNotFound)
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
    koodistoService.koodiUriExistsInKoodisto(PohjakoulutusvaatimusKoodisto, "pohjakoulutusvaatimuskouta_pk") should equal(
      itemFound
    )
    koodistoService.koodiUriExistsInKoodisto(PohjakoulutusvaatimusKoodisto, "pohjakoulutusvaatimuskouta_yo#2") should equal(
      itemFound
    )
    koodistoService.koodiUriExistsInKoodisto(PohjakoulutusvaatimusKoodisto, "pohjakoulutusvaatimuskouta_XX") should equal(
      itemNotFound
    )
    koodistoService.koodiUriExistsInKoodisto(PohjakoulutusvaatimusKoodisto, "pohjakoulutusvaatimuskouta_YY") should equal(
      itemNotFound
    )
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
    koodistoService.koodiUriExistsInKoodisto(LiiteTyyppiKoodisto, "liitetyypitamm_1") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(LiiteTyyppiKoodisto, "liitetyypitamm_2#2") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(LiiteTyyppiKoodisto, "liitetyypitamm_XX") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(LiiteTyyppiKoodisto, "liitetyypitamm_YY") should equal(itemNotFound)
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
    koodistoService.koodiUriExistsInKoodisto(ValintakoeTyyppiKoodisto, "valintakokeentyyppi_1") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(ValintakoeTyyppiKoodisto, "valintakokeentyyppi_2#2") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(ValintakoeTyyppiKoodisto, "valintakokeentyyppi_XX") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(ValintakoeTyyppiKoodisto, "valintakokeentyyppi_YY") should equal(itemNotFound)
  }

  "Finding kausiKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("kausi", Seq(("kausi_s", 1, None), ("kausi_k", 3, None), ("kausi_X", 2, Some(dayInPast))))
    koodistoService.koodiUriExistsInKoodisto(KausiKoodisto, "kausi_s") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(KausiKoodisto, "kausi_k#2") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(KausiKoodisto, "kausi_X") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(KausiKoodisto, "kausi_Y") should equal(itemNotFound)
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
    koodistoService.oppiaineArvoExists("painotettavatoppiaineetlukiossa_b3pt") should equal(itemFound)
    koodistoService.oppiaineArvoExists("painotettavatoppiaineetlukiossa_b1lt#2") should equal(itemFound)
    koodistoService.oppiaineArvoExists("painotettavatoppiaineetlukiossa_XX") should equal(itemNotFound)
    koodistoService.oppiaineArvoExists("painotettavatoppiaineetlukiossa_YY") should equal(itemNotFound)
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
    koodistoService.oppiaineArvoExists("painotettavatoppiaineetlukiossa_a1") should equal(itemFound)
    koodistoService.oppiaineArvoExists("painotettavatoppiaineetlukiossa_b3") should equal(itemFound)
    koodistoService.oppiaineArvoExists("painotettavatoppiaineetlukiossa_X1") should equal(itemNotFound)
    koodistoService.oppiaineArvoExists("painotettavatoppiaineetpäiväkodissa_X1") should equal(itemNotFound)
  }

  "Finding kieliKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("kieli", Seq(("kieli_fi", 1, None), ("kieli_sv", 3, None), ("kieli_su", 2, Some(dayInPast))))
    koodistoService.koodiUriExistsInKoodisto(KieliKoodisto, "kieli_fi") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(KieliKoodisto, "kieli_sv#2") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(KieliKoodisto, "kieli_su") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(KieliKoodisto, "kieli_xx") should equal(itemNotFound)
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
    koodistoService.koodiUriExistsInKoodisto(PostiosoiteKoodisto, "posti_37150") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(PostiosoiteKoodisto, "posti_90810#2") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(PostiosoiteKoodisto, "posti_XXXXX") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(PostiosoiteKoodisto, "posti_YYYYY") should equal(itemNotFound)
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
    koodistoService.koodiUriExistsInKoodisto(HakutapaKoodisto, "hakutapa_01") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(HakutapaKoodisto, "hakutapa_02#2") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(HakutapaKoodisto, "hakutapa_66") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(HakutapaKoodisto, "hakutapa_99") should equal(itemNotFound)
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
    koodistoService.koodiUriExistsInKoodisto(HaunKohdejoukkoKoodisto, "haunkohdejoukko_17") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(HaunKohdejoukkoKoodisto, "haunkohdejoukko_18#2") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(HaunKohdejoukkoKoodisto, "haunkohdejoukko_66") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(HaunKohdejoukkoKoodisto, "haunkohdejoukko_99") should equal(itemNotFound)
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
    koodistoService.koodiUriExistsInKoodisto(HaunKohdejoukonTarkenneKoodisto, "haunkohdejoukontarkenne_1") should equal(
      itemFound
    )
    koodistoService.koodiUriExistsInKoodisto(HaunKohdejoukonTarkenneKoodisto, "haunkohdejoukontarkenne_2#2") should equal(
      itemFound
    )
    koodistoService.koodiUriExistsInKoodisto(HaunKohdejoukonTarkenneKoodisto, "haunkohdejoukontarkenne_6") should equal(
      itemNotFound
    )
    koodistoService.koodiUriExistsInKoodisto(HaunKohdejoukonTarkenneKoodisto, "haunkohdejoukontarkenne_9") should equal(
      itemNotFound
    )
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
    koodistoService.koodiUriExistsInKoodisto(ValintatapaKoodisto, "valintatapajono_av") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(ValintatapaKoodisto, "valintatapajono_tv#2") should equal(itemFound)
    koodistoService.koodiUriExistsInKoodisto(ValintatapaKoodisto, "valintatapajono_km") should equal(itemNotFound)
    koodistoService.koodiUriExistsInKoodisto(ValintatapaKoodisto, "valintatapajono_xx") should equal(itemNotFound)
  }

  "When tutkintotyyppi-koodisto-query failed" should "return error status" in {
    mockKoulutusByTutkintotyyppiFailure("tutkintotyyppi_16")
    koodistoService.getKoulutuksetByTutkintotyyppi("tutkintotyyppi_16").left.get.getMessage should equal(
      s"Failed to get koulutusKoodiUris for koulutustyyppi tutkintotyyppi_16 from koodisto."
    )
  }

  "Finding lisättävät koulutukset by koulutustyyppi" should "return valid koulutukset" in {
    mockKoulutusByTutkintotyyppiResponse("tutkintotyyppi_12345",
      Seq(("koulutus_371101", 12, None))
    )
    koodistoService.getLisattavatKoulutukset("tutkintotyyppi_12345").right.get.map(element => (element.koodiUri, element.versio)) should equal(
      Seq(("koulutus_371101", 12)),
    )
  }

  "Finding lisättävät koulutukset by koulutustyyppi" should "filter out koulutukset that are invalid" in {
    mockKoulutusByTutkintotyyppiResponse("tutkintotyyppi_12345",
      Seq(("koulutus_371101", 12, None), ("koulutus_371102", 1, Some(dayInPast)))
    )
    koodistoService.getLisattavatKoulutukset("tutkintotyyppi_12345").right.get.map(element => (element.koodiUri, element.versio)) should equal(
      Seq(("koulutus_371101", 12)),
    )
  }

  "Finding lisättävät koulutukset by koulutustyyppi" should "filter out koulutukset that are väliotsikko" in {
    mockKoulutusByTutkintotyyppiResponse("tutkintotyyppi_12345",
      Seq(("koulutus_371100", 10, None), // valiotsikko as ends with "00"
        ("koulutus_371101", 12, None))
    )
    koodistoService.getLisattavatKoulutukset("tutkintotyyppi_12345").right.get.map(element => (element.koodiUri, element.versio)) should equal(
      Seq(("koulutus_371101", 12)),
    )
  }

  "Fetching valintakoetyypit " should "return list of koodis" in {
    mockKoodistoResponse(
      "valintakokeentyyppi",
      Seq(
        ("valintakokeentyyppi_1", 1, None),
        ("valintakokeentyyppi_2", 1, None),
        ("valintakokeentyyppi_3", 1, None)
      )
    )
    koodistoService.getValintakokeenTyypit(Seq.empty, None, None, Seq.empty).right.get.map(e => e.koodiUri) should equal(Seq("valintakokeentyyppi_1", "valintakokeentyyppi_2", "valintakokeentyyppi_3"))
  }

  "Fetching valintakoetyypit " should "returns none if they all contain some specific ylakoodisto" in {
    mockValintakoeKoodit()
    koodistoService.getValintakokeenTyypit(Seq.empty, None, None, Seq.empty).right.get.map(e => e.koodiUri) should equal(Seq.empty)
  }

  "Fetching valintakoetyypit " should "return koodi with relation to specific koulutus" in {
    mockValintakoeKoodit()
    koodistoService.getValintakokeenTyypit(Seq("koulutus_11"), None, None, Seq.empty).right.get.map(e => e.koodiUri) should equal(Seq("valintakokeentyyppi_1"))
  }

  "Fetching valintakoetyypit " should "return koodi with relation to specific hakutapa" in {
    mockValintakoeKoodit()
    koodistoService.getValintakokeenTyypit(Seq.empty, Some("hakutapa_01"), None, Seq.empty).right.get.map(e => e.koodiUri) should equal(Seq("valintakokeentyyppi_2"))
  }

  "Fetching valintakoetyypit " should "return koodi with relation to specific haun kohdejoukko" in {
    mockValintakoeKoodit()
    koodistoService.getValintakokeenTyypit(Seq.empty, None, Some("haunkohdejoukko_12"), Seq.empty).right.get.map(e => e.koodiUri) should equal(Seq("valintakokeentyyppi_3"))
  }

  "Fetching valintakoetyypit " should "return koodi with relation to specific osaamisala" in {
    mockValintakoeKoodit()
    koodistoService.getValintakokeenTyypit(Seq.empty, None, None, Seq("osaamisala_1791")).right.get.map(e => e.koodiUri) should equal(Seq("valintakokeentyyppi_4"))
  }
}
