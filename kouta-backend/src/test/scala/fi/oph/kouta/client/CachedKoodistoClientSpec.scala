package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.mocks.{KoodistoServiceMock, SpecWithMocks}
import fi.oph.kouta.domain.{AmmatillisetKoulutusKoodit, Fi, Kielistetty, KoulutuksenLisatiedotKoodisto, KoulutusalaKoodisto, LukioDiplomiKoodisto, LukioErityinenKoulutustehtavaKoodisto, LukioKoulutusKoodit, LukioPainotuksetKoodisto, OpetusaikaKoodisto, OpetuskieliKoodisto, OpetustapaKoodisto, OpintojenLaajuusyksikkoKoodisto, OsaamisalaKoodisto, Sv, TutkintonimikeKoodisto, YoKoulutusKoodit}
import fi.oph.kouta.validation.ExternalQueryResults.{itemFound, itemNotFound, queryFailed}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CachedKoodistoClientSpec extends SpecWithMocks with KoodistoServiceMock {
  KoutaConfigurationFactory.setupWithDefaultTemplateFile()
  setUrlProperties(KoutaConfigurationFactory.configuration.urlProperties)
  var koodiClient: CachedKoodistoClient = _
  val defaultNimi: Kielistetty         = Map(Fi -> "nimi", Sv -> "nimi sv")

  val dayInPast = LocalDate.now().minusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

  override def beforeAll() = {
    super.beforeAll()
    koodiClient = new CachedKoodistoClient(urlProperties.get)
  }

  "Creating KoodiUri object from string" should "work according to validity rules" in {
    KoodistoUtils.koodiUriFromString("some_1111#12") should equal(KoodiUri("some_1111", 12))
    KoodistoUtils.koodiUriFromString("some_1111") should equal(KoodiUri("some_1111", 1))
    KoodistoUtils.koodiUriFromString("some_1111#puppua") should equal(KoodiUri("some_1111#puppua", 1))
  }

  "Searching KoodiUri from list" should "find match if versio is equal or lower" in {
    KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList(
      "some_1111#12",
      Seq(KoodiUri("some_1111", 12), KoodiUri("some_2222", 1))
    ) should equal(true)
    KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList(
      "some_1111#10",
      Seq(KoodiUri("some_1111", 12), KoodiUri("some_2222", 1))
    ) should equal(true)
    KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList(
      "some_1111",
      Seq(KoodiUri("some_1111", 1), KoodiUri("some_2222", 1))
    ) should equal(true)
    KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList(
      "some_1111#2",
      Seq(KoodiUri("some_1111", 1), KoodiUri("some_2222", 1))
    ) should equal(false)
    KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList(
      "some_3333#1",
      Seq(KoodiUri("some_1111", 1), KoodiUri("some_2222", 1))
    ) should equal(false)
  }

  it should "find match regardless of the version if requested" in {
    KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList(
      "some_1111#12",
      Seq(KoodiUri("some_1111", 1), KoodiUri("some_2222", 1)),
      false
    ) should equal(true)
  }

  "Getting latest version of koodiUri" should "return version from cache" in {
    mockLatestKoodiUriResponse("koulutus_201101", 12)
    koodiClient.getKoodiUriVersionOrLatestFromCache("koulutus_201101") should equal(Right(KoodiUri("koulutus_201101", 12, defaultNimi)))
    clearServiceMocks()
    mockLatestKoodiUriResponse("koulutus_201101", 10)
    // Should still use value from cache
    koodiClient.getKoodiUriVersionOrLatestFromCache("koulutus_201101") should equal(Right(KoodiUri("koulutus_201101", 12, defaultNimi)))
  }

  "Getting certain version of koodiUri" should "return version from cache" in {
    mockKoodiUriVersionResponse("koulutus_201102", 11)
    koodiClient.getKoodiUriVersionOrLatestFromCache("koulutus_201102#11") should equal(
      Right(KoodiUri("koulutus_201102", 11, defaultNimi))
    )
  }

  "Finding koulutuskoodiUri by koulutustyyppi" should "return true when koodiuri exists for any of koulutustyypit" in {
    mockKoulutustyyppiResponse(
      AmmatillisetKoulutusKoodit.koulutusTyypit.last,
      Seq(("koulutus_371101", 12, None), ("koulutus_371102", 10, None), ("koulutus_371103", 1, Some(dayInPast))),
      AmmatillisetKoulutusKoodit.koulutusTyypit.init
    )
    koodiClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371101#12") should equal(
      itemFound
    )
    koodiClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371102#9") should equal(
      itemFound
    )
    koodiClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371103") should equal(
      itemNotFound
    )
    koodiClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371102#11") should equal(
      itemNotFound
    )
    koodiClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371104#1") should equal(
      itemNotFound
    )
    clearServiceMocks()
    mockKoulutustyyppiResponse(
      AmmatillisetKoulutusKoodit.koulutusTyypit.head,
      Seq(("koulutus_371103", 12, None)),
      AmmatillisetKoulutusKoodit.koulutusTyypit.tail
    )
    // Should still use values from cache
    koodiClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371101#12") should equal(
      itemFound
    )
    koodiClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371102#9") should equal(
      itemFound
    )
    koodiClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371103") should equal(
      itemNotFound
    )
    koodiClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371102#11") should equal(
      itemNotFound
    )
    koodiClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(AmmatillisetKoulutusKoodit.koulutusTyypit, "koulutus_371104#1") should equal(
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
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_309902") should equal(itemFound)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_301102#2") should equal(itemFound)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_111111") should equal(itemNotFound)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_222222") should equal(itemNotFound)
    clearServiceMocks()
    mockKoodistoResponse("koulutus", Seq(("koulutus_222222", 12, None), ("koulutus_111111", 1, None)))
    // Should still use values from cache
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_309902") should equal(itemFound)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_301102#2") should equal(itemFound)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_111111") should equal(itemNotFound)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_222222") should equal(itemNotFound)
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
    koodiClient.koodiUriExistsInKoodisto(KoulutuksenLisatiedotKoodisto, "koulutuksenlisatiedot_02") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(KoulutuksenLisatiedotKoodisto, "koulutuksenlisatiedot_03#1") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(KoulutuksenLisatiedotKoodisto, "koulutuksenlisatiedot_04") should equal(itemNotFound)
    koodiClient.koodiUriExistsInKoodisto(KoulutuksenLisatiedotKoodisto, "koulutuksenlisatiedot_03#3") should equal(itemNotFound)
    koodiClient.koodiUriExistsInKoodisto(KoulutuksenLisatiedotKoodisto, "koulutuksenlisatiedot_05") should equal(itemNotFound)
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
    koodiClient.koodiUriExistsInKoodisto(KoulutusalaKoodisto, "kansallinenkoulutusluokitus2016koulutusalataso2_010") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(KoulutusalaKoodisto, "kansallinenkoulutusluokitus2016koulutusalataso2_020#1") should equal(
      itemFound
    )
    koodiClient.koodiUriExistsInKoodisto(KoulutusalaKoodisto, "kansallinenkoulutusluokitus2016koulutusalataso2_080") should equal(
      itemNotFound
    )
    koodiClient.koodiUriExistsInKoodisto(KoulutusalaKoodisto,"kansallinenkoulutusluokitus2016koulutusalataso2_020#3") should equal(
      itemNotFound
    )
    koodiClient.koodiUriExistsInKoodisto(KoulutusalaKoodisto, "kansallinenkoulutusluokitus2016koulutusalataso2_050") should equal(
      itemNotFound
    )
  }

  "Finding opintojenLaajuusyksikkoKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "opintojenlaajuusyksikko",
      Seq(("opintojenlaajuusyksikko_6", 1, None), ("opintojenlaajuusyksikko_10", 2, Some(dayInPast)))
    )
    koodiClient.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto, "opintojenlaajuusyksikko_6") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto, "opintojenlaajuusyksikko_10") should equal(itemNotFound)
    koodiClient.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto, "opintojenlaajuusyksikko_6#2") should equal(itemNotFound)
    koodiClient.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto, "opintojenlaajuusyksikko_20") should equal(itemNotFound)
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
    koodiClient.koodiUriExistsInKoodisto(OpetuskieliKoodisto, "oppilaitoksenopetuskieli_2") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(OpetuskieliKoodisto, "oppilaitoksenopetuskieli_3#2") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(OpetuskieliKoodisto, "opintojenlaajuusyksikko_4") should equal(itemNotFound)
    koodiClient.koodiUriExistsInKoodisto(OpetuskieliKoodisto, "opintojenlaajuusyksikko_5") should equal(itemNotFound)
  }

  "Finding opetusAikaKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "opetusaikakk",
      Seq(("opetusaikakk_3", 1, None), ("opetusaikakk_4", 3, None), ("opetusaikakk_5", 2, Some(dayInPast)))
    )
    koodiClient.koodiUriExistsInKoodisto(OpetusaikaKoodisto, "opetusaikakk_3") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(OpetusaikaKoodisto,"opetusaikakk_4#2") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(OpetusaikaKoodisto,"opetusaikakk_5") should equal(itemNotFound)
    koodiClient.koodiUriExistsInKoodisto(OpetusaikaKoodisto,"opetusaikakk_6") should equal(itemNotFound)
  }

  "Finding opetusTapaKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "opetuspaikkakk",
      Seq(("opetuspaikkakk_1", 1, None), ("opetuspaikkakk_2", 3, None), ("opetuspaikkakk_3", 2, Some(dayInPast)))
    )
    koodiClient.koodiUriExistsInKoodisto(OpetustapaKoodisto, "opetuspaikkakk_1") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(OpetustapaKoodisto, "opetuspaikkakk_2#2") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(OpetustapaKoodisto, "opetuspaikkakk_3") should equal(itemNotFound)
    koodiClient.koodiUriExistsInKoodisto(OpetustapaKoodisto, "opetuspaikkakk_4") should equal(itemNotFound)
  }

  "Finding osaamisalaKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse(
      "osaamisala",
      Seq(("osaamisala_1578", 1, None), ("osaamisala_2353", 3, None), ("osaamisala_3024", 2, Some(dayInPast)))
    )
    koodiClient.koodiUriExistsInKoodisto(OsaamisalaKoodisto, "osaamisala_1578") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(OsaamisalaKoodisto, "osaamisala_2353#2") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(OsaamisalaKoodisto, "osaamisala_3024") should equal(itemNotFound)
    koodiClient.koodiUriExistsInKoodisto(OsaamisalaKoodisto, "osaamisala_2248") should equal(itemNotFound)
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
    koodiClient.koodiUriExistsInKoodisto(LukioPainotuksetKoodisto, "lukiopainotukset_503") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(LukioPainotuksetKoodisto, "lukiopainotukset_543#2") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(LukioPainotuksetKoodisto, "lukiopainotukset_817") should equal(itemNotFound)
    koodiClient.koodiUriExistsInKoodisto(LukioPainotuksetKoodisto, "lukiopainotukset_0108") should equal(itemNotFound)
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
    koodiClient.koodiUriExistsInKoodisto(LukioErityinenKoulutustehtavaKoodisto, "lukiolinjaterityinenkoulutustehtava_0109") should equal(
      itemFound
    )
    koodiClient.koodiUriExistsInKoodisto(LukioErityinenKoulutustehtavaKoodisto, "lukiolinjaterityinenkoulutustehtava_0111#2") should equal(
      itemFound
    )
    koodiClient.koodiUriExistsInKoodisto(LukioErityinenKoulutustehtavaKoodisto, "lukiolinjaterityinenkoulutustehtava_0119") should equal(
      itemNotFound
    )
    koodiClient.koodiUriExistsInKoodisto(LukioErityinenKoulutustehtavaKoodisto, "lukiolinjaterityinenkoulutustehtava_0126") should equal(
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
    koodiClient.koodiUriExistsInKoodisto(LukioDiplomiKoodisto, "moduulikoodistolops2021_vka4") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(LukioDiplomiKoodisto, "moduulikoodistolops2021_vka5#2") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(LukioDiplomiKoodisto, "moduulikoodistolops2021_vka6") should equal(itemNotFound)
    koodiClient.koodiUriExistsInKoodisto(LukioDiplomiKoodisto, "moduulikoodistolops2021_uo6") should equal(itemNotFound)
  }

  "When cache data is expired or removed" should "data fetched to cache again" in {
    mockLatestKoodiUriResponse("kansallinenkoulutusluokitus2016koulutusalataso1_01", 2)
    mockKoulutustyyppiResponse(
      YoKoulutusKoodit.koulutusTyypit.last,
      Seq(("koulutus_201000", 12, None), ("koulutus_371101", 12, None)),
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

    koodiClient.getKoodiUriVersionOrLatestFromCache("kansallinenkoulutusluokitus2016koulutusalataso1_01") should equal(
      Right(KoodiUri("kansallinenkoulutusluokitus2016koulutusalataso1_01", 2, defaultNimi))
    )
    koodiClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(YoKoulutusKoodit.koulutusTyypit, "koulutus_201000#12") should equal(itemFound)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(YoKoulutusKoodit.koulutusTyypit, "koulutus_111111#1") should equal(itemNotFound)
    koodiClient.koodiUriExistsInKoodisto(TutkintonimikeKoodisto, "tutkintonimikekk_110") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(TutkintonimikeKoodisto,"tutkintonimikekk_111#2") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(TutkintonimikeKoodisto,"tutkintonimikekk_112") should equal(itemNotFound)
    koodiClient.koodiUriExistsInKoodisto(TutkintonimikeKoodisto,"tutkintonimikekk_111#4") should equal(itemNotFound)
    koodiClient.koodiUriExistsInKoodisto(TutkintonimikeKoodisto,"tutkintonimikekk_120") should equal(itemNotFound)

    koodiClient.koodiuriVersionCache.invalidateAll()
    koodiClient.koodiUriCache.invalidateAll()
    clearServiceMocks()
    mockLatestKoodiUriResponse("kansallinenkoulutusluokitus2016koulutusalataso1_01", 10)
    mockKoulutustyyppiResponse(
      YoKoulutusKoodit.koulutusTyypit.last,
      Seq(("koulutus_111111", 12, None)),
      YoKoulutusKoodit.koulutusTyypit.init
    )
    mockKoodistoResponse("tutkintonimikekk", Seq(("tutkintonimikekk_120", 1, None)))

    koodiClient.getKoodiUriVersionOrLatestFromCache("kansallinenkoulutusluokitus2016koulutusalataso1_01") should equal(
      Right(KoodiUri("kansallinenkoulutusluokitus2016koulutusalataso1_01", 10, defaultNimi))
    )
    koodiClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(YoKoulutusKoodit.koulutusTyypit, "koulutus_201000#12") should equal(itemNotFound)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(YoKoulutusKoodit.koulutusTyypit, "koulutus_111111#1") should equal(itemFound)
    koodiClient.koodiUriExistsInKoodisto(TutkintonimikeKoodisto,"tutkintonimikekk_110") should equal(itemNotFound)
    koodiClient.koodiUriExistsInKoodisto(TutkintonimikeKoodisto,"tutkintonimikekk_120") should equal(itemFound)
  }

  "When koodisto-query failed" should "return error status" in {
    mockKoodistoFailure("opintojenlaajuusyksikko")
    koodiClient.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto, "opintojenlaajuusyksikko_2") should equal(queryFailed)
    clearServiceMocks()
    mockKoodistoNotFound("opintojenlaajuusyksikko")
    koodiClient.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto, "opintojenlaajuusyksikko_2") should equal(itemNotFound)
    clearServiceMocks()
    mockKoodistoResponse("opintojenlaajuusyksikko", Seq(("opintojenlaajuusyksikko_2", 1, None)))
    koodiClient.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto,"opintojenlaajuusyksikko_2") should equal(itemFound)
  }

  "When getting latest version of koodiUri" should "throw error" in {
    mockLatestKoodiUriFailure("koulutus_201101")
    koodiClient.getKoodiUriVersionOrLatestFromCache("koulutus_201101").left.get.getMessage should equal(
      "Failed to get koodiuri-version from koodisto for koulutus_201101 after retry, got response 500, Failure in koodisto-service for koodiuri-base koulutus_201101"
    )
  }

  "When koodisto-query failed in koulutuskoodiUri by koulutustyyppi " should "return error status" in {
    mockKoulutustyyppiFailure(AmmatillisetKoulutusKoodit.koulutusTyypit.last)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExistFromCache(
      Seq(AmmatillisetKoulutusKoodit.koulutusTyypit.last),
      "koulutus_371101#12"
    ) should equal(queryFailed)
  }
}