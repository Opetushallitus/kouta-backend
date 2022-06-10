package fi.oph.kouta.client

import fi.oph.kouta.Templates
import fi.oph.kouta.TestSetups.{CONFIG_PROFILE_TEMPLATE, SYSTEM_PROPERTY_NAME_CONFIG_PROFILE, SYSTEM_PROPERTY_NAME_TEMPLATE}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.mocks.KoodistoServiceMock
import fi.oph.kouta.validation.{ammatillisetKoulutustyypit, lukioKoulutusKoodiUrit, yoKoulutustyypit}
import org.scalatra.test.scalatest.ScalatraFlatSpec

import java.lang.Thread.sleep
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
    KoodistoUtils.koodiUriExistsInList("some_1111#12",
      Seq(KoodiUri("some_1111", 12), KoodiUri("some_2222", 1))) should equal(true)
    KoodistoUtils.koodiUriExistsInList("some_1111#10",
      Seq(KoodiUri("some_1111", 12), KoodiUri("some_2222", 1))) should equal(true)
    KoodistoUtils.koodiUriExistsInList("some_1111",
      Seq(KoodiUri("some_1111", 1), KoodiUri("some_2222", 1))) should equal(true)
    KoodistoUtils.koodiUriExistsInList("some_1111#2",
      Seq(KoodiUri("some_1111", 1), KoodiUri("some_2222", 1))) should equal(false)
    KoodistoUtils.koodiUriExistsInList("some_3333#1",
      Seq(KoodiUri("some_1111", 1), KoodiUri("some_2222", 1))) should equal(false)
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
    koodiClient.koulutusKoodiUriOfKoulutustyypitExists(ammatillisetKoulutustyypit, "koulutus_371101#12") should equal(true)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExists(ammatillisetKoulutustyypit, "koulutus_371103#1") should equal(false)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExists(ammatillisetKoulutustyypit, "koulutus_371104#1") should equal(false)
    clearServiceMocks()
    mockKoulutustyyppiResponse(ammatillisetKoulutustyypit.head, Seq(("koulutus_371103", 12, None)), ammatillisetKoulutustyypit.tail)
    // Should still use values from cache
    koodiClient.koulutusKoodiUriOfKoulutustyypitExists(ammatillisetKoulutustyypit, "koulutus_371101#12") should equal(true)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExists(ammatillisetKoulutustyypit, "koulutus_371103#1") should equal(false)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExists(ammatillisetKoulutustyypit, "koulutus_371104#1") should equal(false)
  }

  "Finding koulutuskoodiUri by filter" should "return true when koodiUri exists with accepted version" in {
    val filter = lukioKoulutusKoodiUrit :+ "koulutus_111111"
    mockKoodiUriResponse("koulutus", Seq(("koulutus_309902", 12, None), ("koulutus_301102", 3, None),
      ("koulutus_301101", 1, None), ("koulutus_111111", 1, Some(dayInPast))))
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_309902") should equal(true)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_301101") should equal(true)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_301102") should equal(true)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_111111") should equal(false)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_222222") should equal(false)
    clearServiceMocks()
    mockKoodiUriResponse("koulutus", Seq(("koulutus_222222", 12, None), ("koulutus_111111", 1, None)))
    // Should still use values from cache
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_309902") should equal(true)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_301101") should equal(true)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_301102") should equal(true)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_111111") should equal(false)
    koodiClient.koulutusKoodiUriExists(filter, "koulutus_222222") should equal(false)
  }

  "Finding lisatiedotOtsikkoKoodiUri" should "return true when koodiUri exists" in {
    mockKoodiUriResponse("koulutuksenlisatiedot",
      Seq(("koulutuksenlisatiedot_03", 1, None), ("koulutuksenlisatiedot_04", 2, Some(dayInPast))))
    koodiClient.lisatiedotOtsikkoKoodiUriExists("koulutuksenlisatiedot_03") should equal(true)
    koodiClient.lisatiedotOtsikkoKoodiUriExists("koulutuksenlisatiedot_04") should equal(false)
    koodiClient.lisatiedotOtsikkoKoodiUriExists("koulutuksenlisatiedot_03#2") should equal(false)
    koodiClient.lisatiedotOtsikkoKoodiUriExists("koulutuksenlisatiedot_05") should equal(false)
  }

  "Finding koulutusalaKoodiUri" should "return true when koodiUri exists" in {
    mockKoodiUriResponse("kansallinenkoulutusluokitus2016koulutusalataso2",
      Seq(("kansallinenkoulutusluokitus2016koulutusalataso2_020", 1, None),
        ("kansallinenkoulutusluokitus2016koulutusalataso2_080", 2, Some(dayInPast))))
    koodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso2_020") should equal(true)
    koodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso2_080") should equal(false)
    koodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso2_020#2") should equal(false)
    koodiClient.koulutusalaKoodiUriExists("kansallinenkoulutusluokitus2016koulutusalataso2_050") should equal(false)
  }

  "Finding opintojenLaajuusKoodiUri" should "return true when koodiUri exists" in {
    mockKoodiUriResponse("opintojenlaajuus",
      Seq(("opintojenlaajuus_40", 1, None), ("opintojenlaajuus_v53", 2, Some(dayInPast))))
    koodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_40") should equal(true)
    koodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_v53") should equal(false)
    koodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_40#2") should equal(false)
    koodiClient.opintojenLaajuusKoodiUriExists("opintojenlaajuus_60") should equal(false)
  }

  "Finding opintojenLaajuusyksikkoKoodiUri" should "return true when koodiUri exists" in {
    mockKoodiUriResponse("opintojenlaajuusyksikko",
      Seq(("opintojenlaajuusyksikko_6", 1, None), ("opintojenlaajuusyksikko_10", 2, Some(dayInPast))))
    koodiClient.opintojenLaajuusyksikkoKoodiUriExists("opintojenlaajuusyksikko_6") should equal(true)
    koodiClient.opintojenLaajuusyksikkoKoodiUriExists("opintojenlaajuusyksikko_10") should equal(false)
    koodiClient.opintojenLaajuusyksikkoKoodiUriExists("opintojenlaajuusyksikko_6#2") should equal(false)
    koodiClient.opintojenLaajuusyksikkoKoodiUriExists("opintojenlaajuusyksikko_20") should equal(false)
  }

  "After cache TTL is expired" should "data fetched to cache again" in {
    mockLatestKoodiUriResponse("kansallinenkoulutusluokitus2016koulutusalataso1_01", 2)
    mockKoulutustyyppiResponse(yoKoulutustyypit.last,
      Seq(("koulutus_201000", 12, None),("koulutus_371101", 12, None)), yoKoulutustyypit.init)
    // Tutkintonimikkeiden tarkistus testataan tässä yhteydessä (ei omassa casessaan), tällöin sitä ei löydy cachesta
    // etukäteen
    mockKoodiUriResponse("tutkintonimikekk",
      Seq(("tutkintonimikekk_110", 1, None), ("tutkintonimikekk_111", 2, Some(dayInPast))))

    koodiClient.getKoodiUriWithLatestVersion(
      "kansallinenkoulutusluokitus2016koulutusalataso1_01") should equal(
      "kansallinenkoulutusluokitus2016koulutusalataso1_01#2")
    koodiClient.koulutusKoodiUriOfKoulutustyypitExists(yoKoulutustyypit, "koulutus_201000#12") should equal(true)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExists(yoKoulutustyypit, "koulutus_111111#1") should equal(false)
    koodiClient.tutkintoNimikeKoodiUriExists("tutkintonimikekk_110") should equal(true)
    koodiClient.tutkintoNimikeKoodiUriExists("tutkintonimikekk_111") should equal(false)
    koodiClient.tutkintoNimikeKoodiUriExists("tutkintonimikekk_110#2") should equal(false)
    koodiClient.tutkintoNimikeKoodiUriExists("tutkintonimikekk_120") should equal(false)

    // Odotetaan että cache TTL expiroituu
    sleep(5000)
    clearServiceMocks()
    mockLatestKoodiUriResponse("kansallinenkoulutusluokitus2016koulutusalataso1_01", 10)
    mockKoulutustyyppiResponse(yoKoulutustyypit.last, Seq(("koulutus_111111", 12, None)), yoKoulutustyypit.init)
    mockKoodiUriResponse("tutkintonimikekk", Seq(("tutkintonimikekk_120", 1, None)))

    koodiClient.getKoodiUriWithLatestVersion(
      "kansallinenkoulutusluokitus2016koulutusalataso1_01") should equal(
      "kansallinenkoulutusluokitus2016koulutusalataso1_01#10")
    koodiClient.koulutusKoodiUriOfKoulutustyypitExists(yoKoulutustyypit, "koulutus_201000#12") should equal(false)
    koodiClient.koulutusKoodiUriOfKoulutustyypitExists(yoKoulutustyypit, "koulutus_111111#1") should equal(true)
    koodiClient.tutkintoNimikeKoodiUriExists("tutkintonimikekk_110") should equal(false)
    koodiClient.tutkintoNimikeKoodiUriExists("tutkintonimikekk_120") should equal(true)

  }
}
