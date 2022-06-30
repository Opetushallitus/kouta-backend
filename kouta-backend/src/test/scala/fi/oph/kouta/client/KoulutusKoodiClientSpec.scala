package fi.oph.kouta.client

import fi.oph.kouta.Templates
import fi.oph.kouta.TestSetups.{CONFIG_PROFILE_TEMPLATE, SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, SYSTEM_PROPERTY_NAME_TEMPLATE}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.mocks.KoodistoServiceMock
import fi.oph.kouta.validation.{ammatillisetKoulutustyypit, lukioKoulutusKoodiUrit, yoKoulutustyypit}
import org.scalatra.test.scalatest.ScalatraFlatSpec
import scalacache.modes.sync.mode

import java.time.format.DateTimeFormatter
import java.time.LocalDate
import scala.concurrent.duration.DurationInt

class KoulutusKoodiClientSpec extends ScalatraFlatSpec with KoodistoServiceMock {
  var koodiClient: KoulutusKoodiClient = _

  val dayInPast = LocalDate.now().minusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))


  override def beforeAll() = {
    System.setProperty(SYSTEM_PROPERTY_NAME_TEMPLATE, Templates.DEFAULT_TEMPLATE_FILE_PATH)
    System.setProperty(SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, CONFIG_PROFILE_TEMPLATE)
    super.startServiceMocking()
    urlProperties = Some(KoutaConfigurationFactory.configuration.urlProperties.addOverride("host.virkailija", s"localhost:$mockPort"))
    koodiClient = new KoulutusKoodiClient(urlProperties.get, 3.seconds)
  }

  "Creating KoodiUri object from string" should "work according to validity rules" in {
    KoodistoUtils.koodiUriFromString("some_1111#12") should equal(KoodiUri("some_1111", 12))
    KoodistoUtils.koodiUriFromString("some_1111") should equal(KoodiUri("some_1111", 1))
    KoodistoUtils.koodiUriFromString("some_1111#puppua") should equal(KoodiUri("some_1111#puppua", 1))
  }

  "Searching KoodiUri from list" should "find match if versio is equal or lower" in {
    KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList("some_1111#12",
      Seq(KoodiUri("some_1111", 12), KoodiUri("some_2222", 1))) should equal(true)
    KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList("some_1111#10",
      Seq(KoodiUri("some_1111", 12), KoodiUri("some_2222", 1))) should equal(true)
    KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList("some_1111",
      Seq(KoodiUri("some_1111", 1), KoodiUri("some_2222", 1))) should equal(true)
    KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList("some_1111#2",
      Seq(KoodiUri("some_1111", 1), KoodiUri("some_2222", 1))) should equal(false)
    KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList("some_3333#1",
      Seq(KoodiUri("some_1111", 1), KoodiUri("some_2222", 1))) should equal(false)
  }

  it should "find match regardless of the version if requested" in {
    KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList("some_1111#12",
      Seq(KoodiUri("some_1111", 1), KoodiUri("some_2222", 1)), false) should equal(true)
  }

  "Getting latest version of koodiUri" should "return version from cache" in {
    mockLatestKoodiUriResponse("koulutus_201101", 12)
    koodiClient.getKoodiUriWithLatestVersion("koulutus_201101") should equal("koulutus_201101#12")
    clearServiceMocks()
    mockLatestKoodiUriResponse("koulutus_201101", 10)
    // Should still use value from cache
    koodiClient.getKoodiUriWithLatestVersion("koulutus_201101") should equal("koulutus_201101#12")
  }

  "Finding koulutuskoodiUri by koulutustyyppi" should "returns true when koodiuri exists for any of koulutustyypit" in {
    mockKoulutustyyppiResponse(ammatillisetKoulutustyypit.last, Seq(("koulutus_371101", 12, None),
      ("koulutus_371102", 10, None), ("koulutus_371103", 1, Some(dayInPast))), ammatillisetKoulutustyypit.init)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExist(ammatillisetKoulutustyypit, "koulutus_371101#12") should equal(true)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExist(ammatillisetKoulutustyypit, "koulutus_371102#9") should equal(true)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExist(ammatillisetKoulutustyypit, "koulutus_371103") should equal(false)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExist(ammatillisetKoulutustyypit, "koulutus_371102#11") should equal(false)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExist(ammatillisetKoulutustyypit, "koulutus_371104#1") should equal(false)
    clearServiceMocks()
    mockKoulutustyyppiResponse(ammatillisetKoulutustyypit.head, Seq(("koulutus_371103", 12, None)), ammatillisetKoulutustyypit.tail)
    // Should still use values from cache
    koodiClient.koulutusKoodiUriOfKoulutustyypitExist(ammatillisetKoulutustyypit, "koulutus_371101#12") should equal(true)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExist(ammatillisetKoulutustyypit, "koulutus_371102#9") should equal(true)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExist(ammatillisetKoulutustyypit, "koulutus_371103") should equal(false)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExist(ammatillisetKoulutustyypit, "koulutus_371102#11") should equal(false)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExist(ammatillisetKoulutustyypit, "koulutus_371104#1") should equal(false)
  }

  "Finding koulutuskoodiUri by filter" should "return true when koodiUri exists with accepted version" in {
    val filter = lukioKoulutusKoodiUrit :+ "koulutus_111111"
    mockKoodistoResponse("koulutus", Seq(("koulutus_309902", 12, None), ("koulutus_301102", 3, None),
      ("koulutus_301101", 1, None), ("koulutus_111111", 1, Some(dayInPast))))
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_309902") should equal(true)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_301102#2") should equal(true)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_111111") should equal(false)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_222222") should equal(false)
    clearServiceMocks()
    mockKoodistoResponse("koulutus", Seq(("koulutus_222222", 12, None), ("koulutus_111111", 1, None)))
    // Should still use values from cache
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_309902") should equal(true)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_301102#2") should equal(true)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_111111") should equal(false)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_222222") should equal(false)
  }

  "Finding lisatiedotOtsikkoKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("koulutuksenlisatiedot",
      Seq(("koulutuksenlisatiedot_02", 1, None), ("koulutuksenlisatiedot_03", 2, None),
        ("koulutuksenlisatiedot_04", 2, Some(dayInPast))))
    koodiClient.lisatiedotOtsikkoKoodiUriExists("koulutuksenlisatiedot_02") should equal(true)
    koodiClient.lisatiedotOtsikkoKoodiUriExists("koulutuksenlisatiedot_03#1") should equal(true)
    koodiClient.lisatiedotOtsikkoKoodiUriExists("koulutuksenlisatiedot_04") should equal(false)
    koodiClient.lisatiedotOtsikkoKoodiUriExists("koulutuksenlisatiedot_03#3") should equal(false)
    koodiClient.lisatiedotOtsikkoKoodiUriExists("koulutuksenlisatiedot_05") should equal(false)
  }

  "Finding koulutusalaKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("kansallinenkoulutusluokitus2016koulutusalataso2",
      Seq(("kansallinenkoulutusluokitus2016koulutusalataso2_010", 1, None),
        ("kansallinenkoulutusluokitus2016koulutusalataso2_020", 2, None),
        ("kansallinenkoulutusluokitus2016koulutusalataso2_080", 2, Some(dayInPast))))
    koodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso2_010") should equal(true)
    koodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso2_020#1") should equal(true)
    koodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso2_080") should equal(false)
    koodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso2_020#3") should equal(false)
    koodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso2_050") should equal(false)
  }

  "Finding opintojenLaajuusKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("opintojenlaajuus",
      Seq(("opintojenlaajuus_40", 1, None), ("opintojenlaajuus_v53", 2, Some(dayInPast))))
    koodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_40") should equal(true)
    koodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_v53") should equal(false)
    koodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_40#2") should equal(false)
    koodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_60") should equal(false)
  }

  "Finding opintojenLaajuusyksikkoKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("opintojenlaajuusyksikko",
      Seq(("opintojenlaajuusyksikko_6", 1, None), ("opintojenlaajuusyksikko_10", 2, Some(dayInPast))))
    koodiClient.opintojenLaajuusyksikkoKoodiUriExists("opintojenlaajuusyksikko_6") should equal(true)
    koodiClient.opintojenLaajuusyksikkoKoodiUriExists("opintojenlaajuusyksikko_10") should equal(false)
    koodiClient.opintojenLaajuusyksikkoKoodiUriExists("opintojenlaajuusyksikko_6#2") should equal(false)
    koodiClient.opintojenLaajuusyksikkoKoodiUriExists("opintojenlaajuusyksikko_20") should equal(false)
  }

  "Finding opetusKieliKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("oppilaitoksenopetuskieli",
      Seq(("oppilaitoksenopetuskieli_2", 1, None), ("oppilaitoksenopetuskieli_3", 3, None),
        ("oppilaitoksenopetuskieli_4", 2, Some(dayInPast))))
    koodiClient.opetusKieliKoodiUriExists("oppilaitoksenopetuskieli_2") should equal(true)
    koodiClient.opetusKieliKoodiUriExists("oppilaitoksenopetuskieli_3#2") should equal(true)
    koodiClient.opetusKieliKoodiUriExists("opintojenlaajuusyksikko_4") should equal(false)
    koodiClient.opetusKieliKoodiUriExists("opintojenlaajuusyksikko_5") should equal(false)
  }

  "Finding opetusAikaKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("opetusaikakk",
      Seq(("opetusaikakk_3", 1, None), ("opetusaikakk_4", 3, None),
        ("opetusaikakk_5", 2, Some(dayInPast))))
    koodiClient.opetusAikaKoodiUriExists("opetusaikakk_3") should equal(true)
    koodiClient.opetusAikaKoodiUriExists("opetusaikakk_4#2") should equal(true)
    koodiClient.opetusAikaKoodiUriExists("opetusaikakk_5") should equal(false)
    koodiClient.opetusAikaKoodiUriExists("opetusaikakk_6") should equal(false)
  }

  "Finding opetusTapaKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("opetuspaikkakk",
      Seq(("opetuspaikkakk_1", 1, None), ("opetuspaikkakk_2", 3, None),
        ("opetuspaikkakk_3", 2, Some(dayInPast))))
    koodiClient.opetusTapaKoodiUriExists("opetuspaikkakk_1") should equal(true)
    koodiClient.opetusTapaKoodiUriExists("opetuspaikkakk_2#2") should equal(true)
    koodiClient.opetusTapaKoodiUriExists("opetuspaikkakk_3") should equal(false)
    koodiClient.opetusTapaKoodiUriExists("opetuspaikkakk_4") should equal(false)
  }

  "Finding osaamisalaKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("osaamisala",
      Seq(("osaamisala_1578", 1, None), ("osaamisala_2353", 3, None),
        ("osaamisala_3024", 2, Some(dayInPast))))
    koodiClient.osaamisalaKoodiUriExists("osaamisala_1578") should equal(true)
    koodiClient.osaamisalaKoodiUriExists("osaamisala_2353#2") should equal(true)
    koodiClient.osaamisalaKoodiUriExists("osaamisala_3024") should equal(false)
    koodiClient.osaamisalaKoodiUriExists("osaamisala_2248") should equal(false)
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

  "Finding lukioPainotusKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("lukiopainotukset",
      Seq(("lukiopainotukset_503", 1, None), ("lukiopainotukset_543", 3, None),
        ("lukiopainotukset_817", 2, Some(dayInPast))))
    koodiClient.lukioPainotusKoodiUriExists("lukiopainotukset_503") should equal(true)
    koodiClient.lukioPainotusKoodiUriExists("lukiopainotukset_543#2") should equal(true)
    koodiClient.lukioPainotusKoodiUriExists("lukiopainotukset_817") should equal(false)
    koodiClient.lukioPainotusKoodiUriExists("lukiopainotukset_0108") should equal(false)
  }

  "Finding lukioErityinenKoulutustehtavaKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("lukiolinjaterityinenkoulutustehtava",
      Seq(("lukiolinjaterityinenkoulutustehtava_0109", 1, None), ("lukiolinjaterityinenkoulutustehtava_0111", 3, None),
        ("lukiolinjaterityinenkoulutustehtava_0119", 2, Some(dayInPast))))
    koodiClient.lukioErityinenKoulutustehtavaKoodiUriExists(
      "lukiolinjaterityinenkoulutustehtava_0109") should equal(true)
    koodiClient.lukioErityinenKoulutustehtavaKoodiUriExists("lukiolinjaterityinenkoulutustehtava_0111#2") should equal(true)
    koodiClient.lukioErityinenKoulutustehtavaKoodiUriExists("lukiolinjaterityinenkoulutustehtava_0119") should equal(false)
    koodiClient.lukioErityinenKoulutustehtavaKoodiUriExists("lukiolinjaterityinenkoulutustehtava_0126") should equal(false)
  }

  "Finding lukioDiplomiKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("moduulikoodistolops2021",
      Seq(("moduulikoodistolops2021_vka4", 1, None), ("moduulikoodistolops2021_vka5", 3, None),
        ("moduulikoodistolops2021_vka6", 2, Some(dayInPast))))
    koodiClient.lukioDiplomiKoodiUriExists("moduulikoodistolops2021_vka4") should equal(true)
    koodiClient.lukioDiplomiKoodiUriExists("moduulikoodistolops2021_vka5#2") should equal(true)
    koodiClient.lukioDiplomiKoodiUriExists("moduulikoodistolops2021_vka6") should equal(false)
    koodiClient.lukioDiplomiKoodiUriExists("moduulikoodistolops2021_uo6") should equal(false)
  }

  "Finding kausiKoodiUri" should "return true when koodiUri exists" in {
    mockKoodistoResponse("kausi", Seq(("kausi_s", 1, None), ("kausi_k", 3, None), ("kausi_X", 2, Some(dayInPast))))
    koodiClient.kausiKoodiUriExists("kausi_s") should equal(true)
    koodiClient.kausiKoodiUriExists("kausi_k#2") should equal(true)
    koodiClient.kausiKoodiUriExists("kausi_X") should equal(false)
    koodiClient.kausiKoodiUriExists("kausi_Y") should equal(false)
  }

  "When cache data is expired or removed" should "data fetched to cache again" in {
    mockLatestKoodiUriResponse("kansallinenkoulutusluokitus2016koulutusalataso1_01", 2)
    mockKoulutustyyppiResponse(yoKoulutustyypit.last,
      Seq(("koulutus_201000", 12, None),("koulutus_371101", 12, None)), yoKoulutustyypit.init)
    // Tutkintonimikkeiden tarkistus testataan tässä yhteydessä (ei omassa casessaan), tällöin sitä ei löydy cachesta
    // etukäteen
    mockKoodistoResponse("tutkintonimikekk",
      Seq(("tutkintonimikekk_110", 1, None), ("tutkintonimikekk_111", 3, None), ("tutkintonimikekk_112", 2, Some(dayInPast))))

    koodiClient.getKoodiUriWithLatestVersion(
      "kansallinenkoulutusluokitus2016koulutusalataso1_01") should equal(
      "kansallinenkoulutusluokitus2016koulutusalataso1_01#2")
    koodiClient.koulutusKoodiUriOfKoulutustyypitExist(yoKoulutustyypit, "koulutus_201000#12") should equal(true)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExist(yoKoulutustyypit, "koulutus_111111#1") should equal(false)
    koodiClient.tutkintoNimikeKoodiUriExists("tutkintonimikekk_110") should equal(true)
    koodiClient.tutkintoNimikeKoodiUriExists("tutkintonimikekk_111#2") should equal(true)
    koodiClient.tutkintoNimikeKoodiUriExists("tutkintonimikekk_112") should equal(false)
    koodiClient.tutkintoNimikeKoodiUriExists("tutkintonimikekk_111#4") should equal(false)
    koodiClient.tutkintoNimikeKoodiUriExists("tutkintonimikekk_120") should equal(false)

    koodiClient.koodiuriVersionCache.removeAll()
    koodiClient.koulutusKoodiUriCache.removeAll()
    koodiClient.commonKoodiUriCache.removeAll()
    clearServiceMocks()
    mockLatestKoodiUriResponse("kansallinenkoulutusluokitus2016koulutusalataso1_01", 10)
    mockKoulutustyyppiResponse(yoKoulutustyypit.last, Seq(("koulutus_111111", 12, None)), yoKoulutustyypit.init)
    mockKoodistoResponse("tutkintonimikekk", Seq(("tutkintonimikekk_120", 1, None)))

    koodiClient.getKoodiUriWithLatestVersion(
      "kansallinenkoulutusluokitus2016koulutusalataso1_01") should equal(
      "kansallinenkoulutusluokitus2016koulutusalataso1_01#10")
    koodiClient.koulutusKoodiUriOfKoulutustyypitExist(yoKoulutustyypit, "koulutus_201000#12") should equal(false)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExist(yoKoulutustyypit, "koulutus_111111#1") should equal(true)
    koodiClient.tutkintoNimikeKoodiUriExists("tutkintonimikekk_110") should equal(false)
    koodiClient.tutkintoNimikeKoodiUriExists("tutkintonimikekk_120") should equal(true)

  }
}
