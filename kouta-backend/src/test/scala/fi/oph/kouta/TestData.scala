package fi.oph.kouta

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util.UUID

import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.oid._

object TestData {

  def now(): LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
  def inFuture(s:Long = 500): LocalDateTime = LocalDateTime.now().plusSeconds(s).truncatedTo(ChronoUnit.MINUTES)
  def inPast(s:Long = 500): LocalDateTime = LocalDateTime.now().minusSeconds(s).truncatedTo(ChronoUnit.MINUTES)
  val startTime1: LocalDateTime = LocalDate.now().plusDays(1).atTime(LocalTime.parse("09:49")).truncatedTo(ChronoUnit.MINUTES)
  val endTime1: LocalDateTime = LocalDate.now().plusDays(1).atTime(LocalTime.parse("09:58")).truncatedTo(ChronoUnit.MINUTES)
  val muokkaajanNimi: String = "Testi Muokkaaja"

  def kieliMap(text: String): Kielistetty = Map(Fi -> s"$text fi", Sv -> s"$text sv")

  def getInvalidHakuajat = List(Ajanjakso(TestData.inFuture(9000), Some(TestData.inFuture(3000))))

  val Osoite1: Osoite = Osoite(
    osoite = Map(Fi -> "Kivatie 1", Sv -> "kivavägen 1"),
    postinumeroKoodiUri = Some("posti_04230#2"))

  val Yhteystieto1: Yhteyshenkilo = Yhteyshenkilo(
    nimi = Map(Fi -> "Aku Ankka", Sv -> "Aku Ankka"),
    puhelinnumero = Map(Fi -> "123", Sv -> "123"),
    sahkoposti = Map(Fi -> "aku.ankka@ankkalinnankoulu.fi", Sv -> "aku.ankka@ankkalinnankoulu.fi"),
    titteli = Map(Fi -> "titteli", Sv -> "titteli sv"),
    wwwSivu = Map(Fi -> "http://opintopolku.fi", Sv -> "http://studieinfo.fi"))

  val Liite1: Liite = Liite(
    id = None,
    tyyppiKoodiUri = Some("liitetyypitamm_2#1"),
    nimi = Map(Fi -> "liite 1 Fi", Sv -> "liite 1 Sv"),
    kuvaus = Map(Fi -> "kuvaus Fi", Sv -> "kuvaus Sv"),
    toimitusaika = Some(inFuture()),
    toimitustapa = Some(Hakijapalvelu),
    toimitusosoite = None)

  val Liite2: Liite = Liite(
    id = None,
    tyyppiKoodiUri = Some("liitetyypitamm_1#1"),
    nimi = Map(Fi -> "liite 2 Fi", Sv -> "liite 2 Sv"),
    kuvaus = Map(Fi -> "kuvaus Fi", Sv -> "kuvaus Sv"),
    toimitusaika = None,
    toimitustapa = Some(MuuOsoite),
    toimitusosoite = Some(LiitteenToimitusosoite(osoite = Osoite1, sahkoposti = Some("foo@bar.fi"))))

  val ValintakokeenLisatilaisuudet1: ValintakokeenLisatilaisuudet = ValintakokeenLisatilaisuudet(
    id = None,
    tilaisuudet = List(Valintakoetilaisuus(
      jarjestamispaikka = Map(Fi -> "Lisä järjestämispaikka fi", Sv -> "Lisä järjestämispaikka sv"),
      osoite = Some(Osoite1),
      aika = Some(Ajanjakso(alkaa = startTime1, paattyy = Some(endTime1))),
      lisatietoja = Map(Fi -> "lisätieto fi", Sv -> "lisätieto sv"))))

  val Valintakoe1: Valintakoe = Valintakoe(
    id = None,
    tyyppiKoodiUri = Some("valintakokeentyyppi_1#1"),
    nimi = Map(Fi -> "valintakokeen nimi fi", Sv -> "valintakokeen nimi sv"),
    metadata = Some(ValintakoeMetadata(
      tietoja = Map(Fi -> "tietoa valintakokeesta fi", Sv -> "tietoa valintakokeesta sv"),
      vahimmaispisteet = Some(182.1),
      liittyyEnnakkovalmistautumista = Some(true),
      ohjeetEnnakkovalmistautumiseen = Map(Fi -> "Ennakko-ohjeet fi", Sv -> "Ennakko-ohjeet sv"),
      erityisjarjestelytMahdollisia = Some(true),
      ohjeetErityisjarjestelyihin = Map(Fi -> "Erityisvalmistelut fi", Sv -> "Erityisvalmistelut sv"))),
    tilaisuudet = List(Valintakoetilaisuus(
      jarjestamispaikka = Map(Fi -> "Järjestämispaikka fi", Sv -> "Järjestämispaikka sv"),
      osoite = Some(Osoite1),
      aika = Some(Ajanjakso(alkaa = now(), paattyy = Some(inFuture()))),
      lisatietoja = Map(Fi -> "lisätieto fi", Sv -> "lisätieto sv"))))

  val AmmKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = true,
    esikatselu = true,
    koulutustyyppi = Amm,
    koulutuksetKoodiUri = Seq("koulutus_371101#1"),
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(AmmatillinenKoulutusMetadata(
      kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
      lisatiedot = Seq(
        Lisatieto(otsikkoKoodiUri = "koulutuksenlisatiedot_03#1",
          teksti = Map(Fi -> "Opintojen lisätieto ", Sv -> "Opintojen lisätieto sv"))))),
    tarjoajat = List(GrandChildOid, EvilGrandChildOid, EvilCousin),
    muokkaaja = OphUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/ammkuva"),
    ePerusteId = Some(11L),
    modified = None,
    _enrichedData = Some(
      KoulutusEnrichedData(
        muokkaajanNimi = muokkaajanNimi
      )
    ))

  val Lisatieto1: Lisatieto = Lisatieto(otsikkoKoodiUri = "koulutuksenlisatiedot_03#1",
    teksti = Map(Fi -> "Opintojen lisätieto ", Sv -> "Opintojen lisätieto sv"))

  val YoKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = true,
    koulutustyyppi = Yo,
    koulutuksetKoodiUri = Seq("koulutus_371101#1", "koulutus_201000#1"),
    esikatselu = true,
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(YliopistoKoulutusMetadata(
      tutkintonimikeKoodiUrit = Seq("tutkintonimikekk_110#2"),
      opintojenLaajuusKoodiUri = Some("opintojenlaajuus_40#1"),
      kuvauksenNimi = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
      kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
      lisatiedot = Seq(Lisatieto1))),
    tarjoajat = List(GrandChildOid, EvilGrandChildOid, EvilCousin),
    muokkaaja = OphUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/yokuva"),
    ePerusteId = Some(12L),
    modified = None,
    _enrichedData = Some(
      KoulutusEnrichedData(
        muokkaajanNimi = muokkaajanNimi
      )
    )
  )

  val AmmTutkinnonOsaKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = false,
    koulutustyyppi = AmmTutkinnonOsa,
    esikatselu = true,
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(AmmatillinenTutkinnonOsaKoulutusMetadata(
      tyyppi = AmmTutkinnonOsa,
      kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
      lisatiedot = Seq(Lisatieto1),
      tutkinnonOsat = Seq(TutkinnonOsa(Some(123L), Some("koulutus_371101#1"), Some(1234L), Some(122L))))),
    tarjoajat = List(GrandChildOid, EvilGrandChildOid, EvilCousin),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/amm-kuva"),
    ePerusteId = None,
    modified = None,
    _enrichedData = Some(
      KoulutusEnrichedData(
        muokkaajanNimi = muokkaajanNimi
      )
    )
  )

  val AmmOsaamisalaKoulutus: Koulutus = AmmTutkinnonOsaKoulutus.copy(
    koulutustyyppi = AmmOsaamisala,
    koulutuksetKoodiUri = Seq("koulutus_371101#1"),
    ePerusteId = Some(11L),
    metadata = Some(AmmatillinenOsaamisalaKoulutusMetadata(
      tyyppi = AmmOsaamisala,
      kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
      lisatiedot = Seq(Lisatieto1),
      osaamisalaKoodiUri = Some("osaamisala_01"))))

  val LukioKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = true,
    koulutustyyppi = Lk,
    koulutuksetKoodiUri = Seq("koulutus_301101#1"),
    esikatselu = true,
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(LukioKoulutusMetadata(
      opintojenLaajuusKoodiUri = Some("opintojenlaajuus_40#1"),
      kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
      koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_001#1"),
      lisatiedot = Seq(Lisatieto1))),
    tarjoajat = List(GrandChildOid, EvilGrandChildOid, EvilCousin),
    muokkaaja = OphUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/lkkuva"),
    ePerusteId = Some(12L),
    modified = None,
    _enrichedData = Some(
      KoulutusEnrichedData(
        muokkaajanNimi = muokkaajanNimi
      )
    )
  )

  val TuvaKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = false,
    koulutustyyppi = Tuva,
    koulutuksetKoodiUri = Seq("koulutus_301101#1"),
    esikatselu = false,
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(TuvaKoulutusMetadata(
      opintojenLaajuusKoodiUri = Some("opintojenlaajuus_v53#1"),
      kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
      linkkiEPerusteisiin = Map(Fi -> "http://testilinkki.fi", Sv -> "http://testlink.sv")
    )),
    tarjoajat = List(GrandChildOid, EvilGrandChildOid, EvilCousin),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/lkkuva"),
    ePerusteId = None,
    modified = None)

  val TelmaKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = false,
    koulutustyyppi = Telma,
    koulutuksetKoodiUri = Seq("koulutus_301101#1"),
    esikatselu = false,
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(TelmaKoulutusMetadata(
      opintojenLaajuusKoodiUri = Some("opintojenlaajuus_60#1"),
      kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
      linkkiEPerusteisiin = Map(Fi -> "http://testilinkki.fi", Sv -> "http://testlink.sv")
    )),
    tarjoajat = List(GrandChildOid, EvilGrandChildOid, EvilCousin),
    muokkaaja = OphUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/lkkuva"),
    ePerusteId = None,
    modified = None)

  val VapaaSivistystyoOpistovuosiKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = false,
    koulutustyyppi = VapaaSivistystyoOpistovuosi,
    esikatselu = false,
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(VapaaSivistystyoOpistovuosiKoulutusMetadata(
      opintojenLaajuusKoodiUri = Some("opintojenlaajuus_v53#1"),
      kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
      linkkiEPerusteisiin = Map(Fi -> "http://testilinkki.fi", Sv -> "http://testlink.sv"),
      koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_001#1"),
    )),
    tarjoajat = List(GrandChildOid, EvilGrandChildOid, EvilCousin),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/lkkuva"),
    ePerusteId = None,
    modified = None)

  val VapaaSivistystyoMuuKoulutus: Koulutus = VapaaSivistystyoOpistovuosiKoulutus.copy(
    koulutustyyppi = VapaaSivistystyoMuu,
    metadata = Some(VapaaSivistystyoMuuKoulutusMetadata(
      opintojenLaajuusKoodiUri = Some("opintojenlaajuus_v53#1"),
      kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
      linkkiEPerusteisiin = Map(Fi -> "http://testilinkki.fi", Sv -> "http://testlink.sv"),
      koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_001#1"),
    )))

  val MinKoulutus: Koulutus = Koulutus(
    koulutustyyppi = Amm,
    johtaaTutkintoon = false,
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    nimi = kieliMap("Minimi koulutus"),
    modified = None)

  val JulkaistuHaku: Haku = Haku(
    nimi = Map(Fi -> "Haku fi", Sv -> "Haku sv"),
    tila = Julkaistu,
    hakutapaKoodiUri = Some("hakutapa_03#1"),
    hakukohteenLiittamisenTakaraja = Some(inFuture()),
    hakukohteenMuokkaamisenTakaraja = Some(inFuture()),
    ajastettuJulkaisu = Some(inFuture()),
    kohdejoukkoKoodiUri = Some("haunkohdejoukko_17#1"),
    kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_1#1"),
    hakulomaketyyppi = Some(EiSähköistä),
    hakulomakeAtaruId = Some(UUID.randomUUID()),
    hakulomakeKuvaus = Map( Fi -> "Hakulomake tulostetaan ja toimitetaan postitse", Sv -> "Hakulomake tulostetaan ja toimitetaan postitse sv"),
    hakulomakeLinkki = Map( Fi -> "https://koulu.test/hakemusinfo-fi", Sv -> "https://koulu.test/hakemusinfo-sv"),
    metadata = Some(HakuMetadata(
      yhteyshenkilot = Seq(Yhteystieto1),
      tulevaisuudenAikataulu = Seq(Ajanjakso(alkaa = now(), paattyy = Some(inFuture()))),
      koulutuksenAlkamiskausi = Some(KoulutuksenAlkamiskausi(
        alkamiskausityyppi = Some(AlkamiskausiJaVuosi),
        henkilokohtaisenSuunnitelmanLisatiedot = Map(Fi -> "Jotakin lisätietoa", Sv -> "Jotakin lisätietoa sv"),
        koulutuksenAlkamispaivamaara = None,
        koulutuksenPaattymispaivamaara = None,
        koulutuksenAlkamiskausiKoodiUri = Some("kausi_k#1"),
        koulutuksenAlkamisvuosi = Some(LocalDate.now().getYear.toString))))),
    hakuajat = List(Ajanjakso(alkaa = now(), paattyy = Some(inFuture()))),
    organisaatioOid = ChildOid,
    muokkaaja = TestUserOid,
    kielivalinta = Seq(Fi, Sv),
    modified = None)

  val MinHaku: Haku = Haku(
    muokkaaja = TestUserOid,
    organisaatioOid = LonelyOid,
    kielivalinta = Seq(Fi, Sv),
    nimi = kieliMap("Minimi haku"),
    modified = None)

  val JulkaistuHakukohde: Hakukohde = Hakukohde(
    oid = None,
    toteutusOid = ToteutusOid("1.2.246.562.17.123"),
    hakuOid = HakuOid("1.2.246.562.29.123"),
    tila = Julkaistu,
    nimi = Map(Fi -> "Hakukohde fi", Sv -> "Hakukohde sv"),
    hakukohdeKoodiUri = None,
    jarjestyspaikkaOid = Some(OtherOid),
    esikatselu = true,
    hakulomaketyyppi = Some(EiSähköistä),
    hakulomakeAtaruId = Some(UUID.randomUUID()),
    hakulomakeKuvaus = Map( Fi -> "Hakulomake tulostetaan ja toimitetaan postitse", Sv -> "Hakulomake tulostetaan ja toimitetaan postitse sv"),
    hakulomakeLinkki = Map( Fi -> "https://koulu.test/kohteen-hakemusinfo-fi", Sv -> "https://koulu.test/kohteen-hakemusinfo-sv"),
    kaytetaanHaunHakulomaketta = Some(false),
    pohjakoulutusvaatimusKoodiUrit = Seq("pohjakoulutusvaatimuskouta_pk#1", "pohjakoulutusvaatimuskouta_yo#1"),
    pohjakoulutusvaatimusTarkenne = kieliMap("Pohjakoulutusvaatimuksen tarkenne"),
    muuPohjakoulutusvaatimus = Map(),
    toinenAsteOnkoKaksoistutkinto = None,
    kaytetaanHaunAikataulua = Some(false),
    valintaperusteId = None,
    liitteetOnkoSamaToimitusaika = Some(true),
    liitteetOnkoSamaToimitusosoite = Some(false),
    liitteidenToimitusaika = Some(inFuture()),
    liitteidenToimitustapa = None,
    liitteidenToimitusosoite = None,
    liitteet = List(Liite1, Liite2),
    metadata = Some(HakukohdeMetadata(
      valintakokeidenYleiskuvaus = Map(Fi -> "yleiskuvaus fi", Sv -> "yleiskuvaus sv"),
      valintaperusteenValintakokeidenLisatilaisuudet = List(ValintakokeenLisatilaisuudet1),
      kynnysehto = Map(Fi -> "Kynnysehto fi", Sv -> "Kynnysehto sv"),
      kaytetaanHaunAlkamiskautta = Some(false),
      koulutuksenAlkamiskausi = Some(KoulutuksenAlkamiskausi(
        alkamiskausityyppi = Some(TarkkaAlkamisajankohta),
        koulutuksenAlkamisvuosi = None,
        koulutuksenAlkamiskausiKoodiUri = None,
        koulutuksenAlkamispaivamaara = Some(inFuture(20000)),
        koulutuksenPaattymispaivamaara = Some(inFuture(30000)))),
      aloituspaikat = Some(Aloituspaikat(
        lukumaara = Some(100), ensikertalaisille = Some(50), kuvaus = Map(Fi -> "aloituspaikkojen kuvaus fi", Sv -> "aloituspaikkojen kuvaus sv"))))),
    valintakokeet = List(Valintakoe1),
    hakuajat = List(Ajanjakso(alkaa = now(), paattyy = Some(inFuture()))),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    modified = None,
    _enrichedData = Some(
      EnrichedData(
        esitysnimi = Map(Fi -> "Hakukohde fi", Sv -> "Hakukohde sv")
      )
    )
  )

  val MinHakukohde: Hakukohde = Hakukohde(
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    nimi = kieliMap("Minimi hakukohde"),
    hakukohdeKoodiUri = None,
    toteutusOid = ToteutusOid("1.2.246.562.17.123"),
    hakuOid = HakuOid("1.2.246.562.29.123"),
    modified = None)

  val Taulukko1: Taulukko = Taulukko(
    id = None,
    nimi = Map(Fi -> "Taulukko 1", Sv -> "Taulukko 1 sv"),
    rows = Seq(
      Row(index = 0, isHeader = true, columns = Seq(
        Column(index = 0, text = Map(Fi -> "Otsikko", Sv -> "Otsikko sv")),
        Column(index = 1, text = Map(Fi -> "Otsikko 2", Sv -> "Otsikko 2 sv")))),
      Row(index = 1, isHeader = false, columns = Seq(
        Column(index = 0, text = Map(Fi -> "Tekstiä", Sv -> "Tekstiä sv")),
        Column(index = 1, text = Map(Fi -> "Tekstiä 2", Sv -> "Tekstiä 2 sv"))))))

  val Taulukko2: Taulukko = Taulukko(
    id = None,
    nimi = Map(Fi -> "Taulukko 2", Sv -> "Taulukko 2 sv"),
    rows = Seq(
      Row(index = 0, isHeader = true, columns = Seq(
        Column(index = 0, text = Map(Fi -> "Otsikko", Sv -> "Otsikko sv")),
        Column(index = 1, text = Map(Fi -> "Otsikko 2", Sv -> "Otsikko 2 sv")))),
      Row(index = 1, isHeader = false, columns = Seq(
        Column(index = 0, text = Map(Fi -> "Tekstiä", Sv -> "Tekstiä sv")),
        Column(index = 1, text = Map(Fi -> "Tekstiä 2", Sv -> "Tekstiä 2 sv"))))))

  val Valintatapa1: Valintatapa = Valintatapa(
    nimi = kieliMap("Valintatapa1"),
    valintatapaKoodiUri = Some("valintatapajono_av#1"),
    kuvaus = kieliMap("kuvaus"),
    sisalto = Seq(SisaltoTeksti(kieliMap("Sisaltoteksti")), Taulukko1, Taulukko2),
    kaytaMuuntotaulukkoa = false,
    kynnysehto = kieliMap("kynnysehto"),
    enimmaispisteet = Some(201.15),
    vahimmaispisteet = Some(182.1))

  val Valintatapa2: Valintatapa = Valintatapa(
    nimi = kieliMap("Valintatapa2"),
    valintatapaKoodiUri = Some("valintatapajono_tv#1"),
    kuvaus = kieliMap("kuvaus 2"),
    sisalto = Seq(SisaltoTeksti(kieliMap("Sisaltoteksti")), Taulukko2),
    kaytaMuuntotaulukkoa = true,
    kynnysehto = kieliMap("kynnysehto"),
    enimmaispisteet = Some(18.1),
    vahimmaispisteet = Some(10.1))

  val LkValintaperusteMetadata: LukioValintaperusteMetadata = LukioValintaperusteMetadata(
    valintatavat = Seq(Valintatapa1, Valintatapa2),
    valintakokeidenYleiskuvaus = Map(Fi -> "yleiskuvaus fi", Sv -> "yleiskuvaus sv"),
    kuvaus = kieliMap("kuvaus"),
    lisatiedot = Map(Fi -> "lisatiedot", Sv -> "lisatiedot sv"),
    hakukelpoisuus = Map(Fi -> "hakukelpoisuus", Sv -> "hakukelpoisuus sv"),
    sisalto = Seq(SisaltoTeksti(kieliMap("Sisaltoteksti")), Taulukko1, Taulukko2))

  val YoValintaperusteMetadata: YliopistoValintaperusteMetadata = YliopistoValintaperusteMetadata(
    valintatavat = Seq(Valintatapa1, Valintatapa2),
    valintakokeidenYleiskuvaus = Map(Fi -> "yleiskuvaus fi", Sv -> "yleiskuvaus sv"),
    kuvaus = kieliMap("kuvaus"),
    lisatiedot = Map(Fi -> "lisatiedot", Sv -> "lisatiedot sv"),
    hakukelpoisuus = Map(Fi -> "hakukelpoisuus", Sv -> "hakukelpoisuus sv"),
    sisalto = Seq(SisaltoTeksti(kieliMap("Sisaltoteksti")), Taulukko1, Taulukko2))

  val AmmValintaperusteMetadata: AmmatillinenValintaperusteMetadata = AmmatillinenValintaperusteMetadata(
    valintatavat = Seq(Valintatapa1, Valintatapa2),
    valintakokeidenYleiskuvaus = Map(Fi -> "yleiskuvaus fi", Sv -> "yleiskuvaus sv"),
    kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
    lisatiedot = Map(Fi -> "lisatiedot", Sv -> "lisatiedot sv"),
    hakukelpoisuus = Map(Fi -> "hakukelpoisuus", Sv -> "hakukelpoisuus sv"),
    sisalto = Seq(SisaltoTeksti(kieliMap("Sisaltoteksti")), Taulukko1, Taulukko2))

  val AmmTutkinnonOsaValintaperusteMetadata: AmmatillinenTutkinnonOsaValintaperusteMetadata = AmmatillinenTutkinnonOsaValintaperusteMetadata(
    valintatavat = Seq(Valintatapa1, Valintatapa2),
    valintakokeidenYleiskuvaus = Map(Fi -> "yleiskuvaus fi", Sv -> "yleiskuvaus sv"),
    kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
    lisatiedot = Map(Fi -> "lisatiedot", Sv -> "lisatiedot sv"),
    hakukelpoisuus = Map(Fi -> "hakukelpoisuus", Sv -> "hakukelpoisuus sv"),
    sisalto = Seq(SisaltoTeksti(kieliMap("Sisaltoteksti")), Taulukko1, Taulukko2))

  val AmmOsaamisalaValintaperusteMetadata: AmmatillinenOsaamisalaValintaperusteMetadata = AmmatillinenOsaamisalaValintaperusteMetadata(
    valintatavat = Seq(Valintatapa1, Valintatapa2),
    valintakokeidenYleiskuvaus = Map(Fi -> "yleiskuvaus fi", Sv -> "yleiskuvaus sv"),
    kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
    lisatiedot = Map(Fi -> "lisatiedot", Sv -> "lisatiedot sv"),
    hakukelpoisuus = Map(Fi -> "hakukelpoisuus", Sv -> "hakukelpoisuus sv"),
    sisalto = Seq(SisaltoTeksti(kieliMap("Sisaltoteksti")), Taulukko1, Taulukko2))


  val TuvaValintaperusteMetadata: TutkintokoulutukseenValmentavaValintaperusteMetadata = TutkintokoulutukseenValmentavaValintaperusteMetadata(
    valintatavat = Seq(Valintatapa1, Valintatapa2),
    valintakokeidenYleiskuvaus = Map(Fi -> "yleiskuvaus fi", Sv -> "yleiskuvaus sv"),
    kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
    lisatiedot = Map(Fi -> "lisatiedot", Sv -> "lisatiedot sv"),
    hakukelpoisuus = Map(Fi -> "hakukelpoisuus", Sv -> "hakukelpoisuus sv"),
    sisalto = Seq(SisaltoTeksti(kieliMap("Sisaltoteksti")), Taulukko1, Taulukko2))

  val MuuTyyppiValintaperusteMetadata: MuuValintaperusteMetadata = MuuValintaperusteMetadata(
    valintatavat = Seq(Valintatapa1, Valintatapa2),
    valintakokeidenYleiskuvaus = Map(Fi -> "yleiskuvaus fi", Sv -> "yleiskuvaus sv"),
    kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
    lisatiedot = Map(Fi -> "lisatiedot", Sv -> "lisatiedot sv"),
    hakukelpoisuus = Map(Fi -> "hakukelpoisuus", Sv -> "hakukelpoisuus sv"),
    sisalto = Seq(SisaltoTeksti(kieliMap("Sisaltoteksti")), Taulukko1, Taulukko2))

  val AmmValintaperuste: Valintaperuste = Valintaperuste(
    koulutustyyppi = Amm,
    id = None,
    tila = Julkaistu,
    hakutapaKoodiUri = Some("hakutapa_02#1"),
    kohdejoukkoKoodiUri = Some("haunkohdejoukko_17#1"),
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    esikatselu = true,
    julkinen = false,
    valintakokeet = List(Valintakoe1),
    metadata = Some(AmmValintaperusteMetadata),
    organisaatioOid = ChildOid,
    muokkaaja = TestUserOid,
    kielivalinta = List(Fi, Sv),
    modified = None)

  val YoValintaperuste: Valintaperuste = Valintaperuste(
    koulutustyyppi = Yo,
    id = None,
    tila = Julkaistu,
    hakutapaKoodiUri = Some("hakutapa_03#1"),
    kohdejoukkoKoodiUri = Some("haunkohdejoukko_15#1"),
    nimi = kieliMap("nimi"),
    esikatselu = true,
    julkinen = true,
    valintakokeet = List(Valintakoe1),
    metadata = Some(YoValintaperusteMetadata),
    organisaatioOid = ChildOid,
    muokkaaja = TestUserOid,
    kielivalinta = List(Fi, Sv),
    modified = None)

  val MinYoValintaperuste: Valintaperuste = Valintaperuste(
    koulutustyyppi = Yo,
    kielivalinta = Seq(Fi, Sv),
    nimi = kieliMap("Minimi valintaperuste"),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    modified = None)

  val YoSorakuvaus: Sorakuvaus = Sorakuvaus(
    id = None,
    tila = Julkaistu,
    nimi = kieliMap("nimi"),
    koulutustyyppi = Yo,
    kielivalinta = List(Fi, Sv),
    metadata = Some(SorakuvausMetadata(
      kuvaus = kieliMap("kuvaus"),
      koulutusKoodiUrit = Seq("koulutus_371101#1"),
      koulutusalaKoodiUri = Some("kansallinenkoulutusluokitus2016koulutusalataso2_054#1"))),
    organisaatioOid = OphOid,
    muokkaaja = OphUserOid,
    modified = None)

  val AmmSorakuvaus: Sorakuvaus = YoSorakuvaus.copy(koulutustyyppi = Amm)

  val MinSorakuvaus: Sorakuvaus = Sorakuvaus(
    koulutustyyppi = Yo,
    kielivalinta = Seq(Fi, Sv),
    nimi = kieliMap("Minimi sorakuvaus"),
    muokkaaja = OphUserOid,
    organisaatioOid = OphOid,
    modified = None)

  val ToteutuksenOpetus: Opetus = Opetus(
    opetuskieliKoodiUrit = Seq("oppilaitoksenopetuskieli_1#1"),
    opetuskieletKuvaus = Map(Fi -> "Kielikuvaus fi", Sv -> "Kielikuvaus sv"),
    opetusaikaKoodiUrit = Seq("opetusaikakk_1#1"),
    opetusaikaKuvaus = Map(Fi -> "Opetusaikakuvaus fi", Sv -> "Opetusaikakuvaus sv"),
    opetustapaKoodiUrit = Seq("opetuspaikkakk_1#1", "opetuspaikkakk_2#1"),
    opetustapaKuvaus = Map(Fi -> "Opetustapakuvaus fi", Sv -> "Opetustapakuvaus sv"),
    maksullisuustyyppi = Some(Maksullinen),
    maksullisuusKuvaus = Map(Fi -> "Maksullisuuskuvaus fi", Sv -> "Maksullisuuskuvaus sv"),
    maksunMaara = Some(200.5),
    koulutuksenAlkamiskausi = Some(KoulutuksenAlkamiskausi(
      alkamiskausityyppi = Some(AlkamiskausiJaVuosi),
      henkilokohtaisenSuunnitelmanLisatiedot = Map(Fi -> "Jotakin lisätietoa", Sv -> "Jotakin lisätietoa sv"),
      koulutuksenAlkamispaivamaara = None,
      koulutuksenPaattymispaivamaara = None,
      koulutuksenAlkamiskausiKoodiUri = Some("kausi_k#1"),
      koulutuksenAlkamisvuosi = Some(LocalDate.now().getYear.toString))),
    lisatiedot = Seq(
      Lisatieto(otsikkoKoodiUri = "koulutuksenlisatiedot_03#1",
           teksti = Map(Fi -> "Opintojen rakenteen kuvaus", Sv -> "Rakenne kuvaus sv")),
      Lisatieto(otsikkoKoodiUri = "koulutuksenlisatiedot_03#1",
           teksti = Map(Fi -> "Sisältö kuvaus", Sv -> "Sisältö kuvaus sv"))),
    onkoApuraha = true,
    apuraha = Some(Apuraha(
      min = Some(100),
      max = Some(200),
      yksikko = Some(Euro),
      kuvaus = Map(Fi -> "apurahakuvaus fi", Sv -> "apurahakuvaus sv"),
    )),
    suunniteltuKestoVuodet = Some(3),
    suunniteltuKestoKuukaudet = Some(10),
    suunniteltuKestoKuvaus = Map(Fi -> "Keston kuvaus fi", Sv -> "Keston kuvaus sv")
  )

  val AmmToteutuksenMetatieto: AmmatillinenToteutusMetadata = AmmatillinenToteutusMetadata(
    kuvaus = Map(),
    osaamisalat = List(AmmatillinenOsaamisala("osaamisala_0001#1",
      linkki = Map(Fi -> "http://osaamisala.fi/linkki/fi", Sv -> "http://osaamisala.fi/linkki/sv"),
      otsikko = Map(Fi -> "Katso osaamisalan tarkempi kuvaus tästä", Sv -> "Katso osaamisalan tarkempi kuvaus tästä sv"))),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    ammattinimikkeet = List(Keyword(Fi, "insinööri"), Keyword(Fi, "koneinsinööri")),
    yhteyshenkilot = Seq(Yhteystieto1))

  val LukioToteutuksenMetatieto: LukioToteutusMetadata = LukioToteutusMetadata(
    kuvaus = Map(),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "tutkija")),
    ammattinimikkeet = List(),
    yhteyshenkilot = Seq(Yhteystieto1),
    kielivalikoima = Some(Kielivalikoima(
      A1Kielet = Seq("kieli_EN#1"),
      A2Kielet = Seq("kieli_DE#1"),
      B1Kielet = Seq("kieli_SV#1"),
      B2Kielet = Seq("kieli_FR#1"),
      B3Kielet = Seq("kieli_ES#1"),
      aidinkielet = Seq("kieli_FI#1"),
      muutKielet = Seq("kieli_ET#1"))),
    yleislinja = true,
    painotukset = List(LukiolinjaTieto(koodiUri = "lukiopainotukset_1#1", Map(Fi -> "painotus 1 kuvaus", Sv -> "painotus 1 kuvaus sv"))),
    erityisetKoulutustehtavat = List(LukiolinjaTieto(koodiUri = "lukiolinjaterityinenkoulutustehtava_1#1", Map(Fi -> "erityinen koulutustehtava 1 kuvaus", Sv -> "erityinen koulutustehtava 1 kuvaus sv"))),
    diplomit = List(LukiodiplomiTieto(koodiUri = "moduulikoodistolops2021_kald3#1", linkki = Map(Fi -> "http://linkki.fi", Sv -> "http://link.se"), linkinAltTeksti = Map(Fi -> "Suomeksi", Sv -> "På svenska")))
  )

  val YoToteutuksenMetatieto: YliopistoToteutusMetadata = YliopistoToteutusMetadata(
    kuvaus = Map(),
    alemmanKorkeakoulututkinnonOsaamisalat = Seq(KorkeakouluOsaamisala(
      linkki = Map(Fi -> "http://osaamisala.fi/linkki/fi", Sv -> "http://osaamisala.fi/linkki/sv"),
      otsikko = Map(Fi -> "Katso osaamisalan tarkempi kuvaus tästä", Sv -> "Katso osaamisalan tarkempi kuvaus tästä sv"),
      nimi = Map(Fi -> "Nimi", Sv -> "Namn"),
      kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"))),
    ylemmanKorkeakoulututkinnonOsaamisalat = Seq(KorkeakouluOsaamisala(
      linkki = Map(Fi -> "http://osaamisala.fi/linkki/fi", Sv -> "http://osaamisala.fi/linkki/sv"),
      otsikko = Map(Fi -> "Katso osaamisalan tarkempi kuvaus tästä", Sv -> "Katso osaamisalan tarkempi kuvaus tästä sv"),
      nimi = Map(Fi -> "Nimi", Sv -> "Namn"),
      kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"))),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    ammattinimikkeet = List(Keyword(Fi, "insinööri"), Keyword(Fi, "koneinsinööri")),
    yhteyshenkilot = Seq(Yhteystieto1))

  val JulkaistuAmmToteutus: Toteutus = Toteutus(
    oid = None,
    koulutusOid = KoulutusOid("1.2.246.562.13.123"),
    tila = Julkaistu,
    esikatselu = true,
    tarjoajat = List(OtherOid, AmmOid),
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(AmmToteutuksenMetatieto),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    modified = None,
    _enrichedData = Some(
      ToteutusEnrichedData(
        esitysnimi = Map(Fi -> "nimi", Sv -> "nimi sv")
      )
    )
  )

  val JulkaistuYoToteutus: Toteutus = JulkaistuAmmToteutus.copy(metadata = Some(YoToteutuksenMetatieto))

  val TuvaToteutuksenMetatieto: TuvaToteutusMetadata = TuvaToteutusMetadata(
    tyyppi = Tuva,
    kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    jarjestetaanErityisopetuksena = true,
    yhteyshenkilot = Seq(Yhteystieto1),
    aloituspaikat = Some(23))

  val TuvaToteutus: Toteutus = JulkaistuAmmToteutus.copy(metadata = Some(TuvaToteutuksenMetatieto))

  val TelmaToteutuksenMetatieto: TelmaToteutusMetadata = TelmaToteutusMetadata(
    tyyppi = Telma,
    kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    yhteyshenkilot = Seq(Yhteystieto1),
    aloituspaikat = Some(23))

  val TelmaToteutus: Toteutus = JulkaistuAmmToteutus.copy(metadata = Some(TelmaToteutuksenMetatieto))

  val VapaaSivistystyoOpistovuosiToteutusMetatieto: VapaaSivistystyoOpistovuosiToteutusMetadata = VapaaSivistystyoOpistovuosiToteutusMetadata(
    tyyppi = VapaaSivistystyoOpistovuosi,
    kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    yhteyshenkilot = Seq(Yhteystieto1))

  val VapaaSivistystyoOpistovuosiToteutus: Toteutus = JulkaistuAmmToteutus.copy(metadata = Some(VapaaSivistystyoOpistovuosiToteutusMetatieto))

  val VapaaSivistystyoMuuToteutusMetatieto: VapaaSivistystyoMuuToteutusMetadata = VapaaSivistystyoMuuToteutusMetadata(
    tyyppi = VapaaSivistystyoMuu,
    kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    hakutermi = Some(Hakeutuminen),
    hakulomaketyyppi = Some(MuuHakulomake),
    hakulomakeLinkki = Map(Fi -> "http://www.linkki.fi", Sv -> "http://www.linkki.se"),
    lisatietoaHakeutumisesta = Map(Fi -> "Lisätieto", Sv -> "Lisätieto sv"),
    lisatietoaValintaperusteista = Map(Fi -> "Lisätieto", Sv -> "Lisätieto sv"),
    hakuaika = Some(Ajanjakso(alkaa = now(), paattyy = Some(inFuture()))),
    yhteyshenkilot = Seq(Yhteystieto1),
    aloituspaikat = None)

  val VapaaSivistystyoMuuToteutusHakemuspalveluMetatieto: VapaaSivistystyoMuuToteutusMetadata = VapaaSivistystyoMuuToteutusMetatieto.copy(
    hakutermi = Some(Hakeutuminen),
    hakulomaketyyppi = Some(Ataru),
    hakulomakeLinkki = Map(),
    lisatietoaHakeutumisesta = Map(),
    lisatietoaValintaperusteista = Map(),
    hakuaika = None)

  val VapaaSivistystyoMuuToteutus: Toteutus = JulkaistuAmmToteutus.copy(metadata = Some(VapaaSivistystyoMuuToteutusMetatieto))

  val MinToteutus: Toteutus = Toteutus(
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    koulutusOid = KoulutusOid("1.2.246.562.13.123"),
    kielivalinta = Seq(Fi, Sv),
    nimi = kieliMap("Minimi toteutus"),
    modified = None,
    _enrichedData = Some(
      ToteutusEnrichedData(
        esitysnimi = kieliMap("Minimi toteutus")
      )
    )
  )

  val AmmOsaamisalaToteutus: Toteutus = JulkaistuAmmToteutus.copy(
    metadata = Some(AmmatillinenOsaamisalaToteutusMetadata(
      tyyppi = AmmOsaamisala,
      kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"),
      opetus = Some(ToteutuksenOpetus),
      asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
      ammattinimikkeet = List(Keyword(Fi, "insinööri"), Keyword(Fi, "koneinsinööri")),
      yhteyshenkilot = Seq(Yhteystieto1),
      hakutermi = Some(Hakeutuminen),
      hakulomaketyyppi = Some(MuuHakulomake),
      hakulomakeLinkki = Map(Fi -> "http://www.linkki.fi", Sv -> "http://www.linkki.se"),
      lisatietoaHakeutumisesta = Map(Fi -> "Lisätieto", Sv -> "Lisätieto sv"),
      lisatietoaValintaperusteista = Map(Fi -> "Lisätieto", Sv -> "Lisätieto sv"),
      hakuaika = Some(Ajanjakso(alkaa = now(), paattyy = Some(inFuture()))),
      aloituspaikat = Some(23))))

  val LukioToteutus: Toteutus = Toteutus(
    oid = None,
    koulutusOid = KoulutusOid("1.2.246.562.13.123"),
    tila = Julkaistu,
    esikatselu = true,
    tarjoajat = List(OtherOid, AmmOid),
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(LukioToteutuksenMetatieto),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    modified = None,
    _enrichedData = Some(
      ToteutusEnrichedData(
        esitysnimi = Map(Fi -> "nimi", Sv -> "nimi sv")
      )
    )
  )

  val AmmOsaamisalaToteutusMetadataHakemuspalvelu: AmmatillinenOsaamisalaToteutusMetadata = AmmatillinenOsaamisalaToteutusMetadata(
    tyyppi = AmmOsaamisala,
    kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    ammattinimikkeet = List(Keyword(Fi, "insinööri"), Keyword(Fi, "koneinsinööri")),
    yhteyshenkilot = Seq(Yhteystieto1),
    hakutermi = Some(Hakeutuminen),
    hakulomaketyyppi = Some(Ataru),
    hakulomakeLinkki = Map(),
    lisatietoaHakeutumisesta = Map(),
    lisatietoaValintaperusteista = Map(),
    hakuaika = None,
    aloituspaikat = Some(23))

  val AmmOsaamisalaToteutusMetadataEiSahkoista: AmmatillinenOsaamisalaToteutusMetadata = AmmatillinenOsaamisalaToteutusMetadata(
    tyyppi = AmmOsaamisala,
    kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    ammattinimikkeet = List(Keyword(Fi, "insinööri"), Keyword(Fi, "koneinsinööri")),
    yhteyshenkilot = Seq(Yhteystieto1),
    hakutermi = Some(Hakeutuminen),
    hakulomaketyyppi = Some(EiSähköistä),
    hakulomakeLinkki = Map(),
    lisatietoaHakeutumisesta = Map(Fi -> "Lisätieto", Sv -> "Lisätieto sv"),
    lisatietoaValintaperusteista = Map(),
    hakuaika = None,
    aloituspaikat = Some(23))

  val AmmTutkinnonOsaToteutus: Toteutus = JulkaistuAmmToteutus.copy(
    metadata = Some(AmmatillinenTutkinnonOsaToteutusMetadata(
      tyyppi = AmmTutkinnonOsa,
      kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"),
      opetus = Some(ToteutuksenOpetus),
      asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
      ammattinimikkeet = List(Keyword(Fi, "insinööri"), Keyword(Fi, "koneinsinööri")),
      yhteyshenkilot = Seq(Yhteystieto1),
      hakutermi = Some(Hakeutuminen),
      hakulomaketyyppi = Some(MuuHakulomake),
      hakulomakeLinkki = Map(Fi -> "http://www.linkki.fi", Sv -> "http://www.linkki.se"),
      lisatietoaHakeutumisesta = Map(Fi -> "Lisätieto", Sv -> "Lisätieto sv"),
      lisatietoaValintaperusteista = Map(Fi -> "Lisätieto", Sv -> "Lisätieto sv"),
      hakuaika = Some(Ajanjakso(alkaa = now(), paattyy = Some(inFuture()))),
      aloituspaikat = Some(22))))

  val AmmTutkinnonOsaToteutusMetadataHakemuspalvelu: AmmatillinenTutkinnonOsaToteutusMetadata = AmmatillinenTutkinnonOsaToteutusMetadata(
    tyyppi = AmmTutkinnonOsa,
    kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    ammattinimikkeet = List(Keyword(Fi, "insinööri"), Keyword(Fi, "koneinsinööri")),
    yhteyshenkilot = Seq(Yhteystieto1),
    hakutermi = Some(Hakeutuminen),
    hakulomaketyyppi = Some(Ataru),
    hakulomakeLinkki = Map(),
    lisatietoaHakeutumisesta = Map(),
    lisatietoaValintaperusteista = Map(),
    hakuaika = None,
    aloituspaikat = Some(23))

  val AmmTutkinnonOsaToteutusMetadataEiSahkoista: AmmatillinenTutkinnonOsaToteutusMetadata = AmmatillinenTutkinnonOsaToteutusMetadata(
    tyyppi = AmmTutkinnonOsa,
    kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    ammattinimikkeet = List(Keyword(Fi, "insinööri"), Keyword(Fi, "koneinsinööri")),
    yhteyshenkilot = Seq(Yhteystieto1),
    hakutermi = Some(Hakeutuminen),
    hakulomaketyyppi = Some(EiSähköistä),
    hakulomakeLinkki = Map(),
    lisatietoaHakeutumisesta = Map(Fi -> "Lisätieto", Sv -> "Lisätieto sv"),
    lisatietoaValintaperusteista = Map(),
    hakuaika = None,
    aloituspaikat = Some(23))

  val JulkaistuOppilaitos: Oppilaitos = Oppilaitos(
    oid = ChildOid,
    tila = Julkaistu,
    esikatselu = true,
    metadata = Some(OppilaitosMetadata(
      yhteystiedot = Seq(Yhteystieto(
        nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
        postiosoite = Some(Osoite1),
        kayntiosoite = Some(Osoite1),
        puhelinnumero = Map(Fi -> "123", Sv -> "123"),
        sahkoposti = Map(Fi -> "aku.ankka@ankkalinnankoulu.fi", Sv -> "aku.ankka@ankkalinnankoulu.fi"))),
      wwwSivu = Some(NimettyLinkki(
        url = Map(Fi -> "http://www.oppilaitos.fi", Sv -> "http://www.oppilaitos.sv"),
        nimi = Map(Fi -> "Linkki suomeksi", Sv -> "Linkki ruotsiksi"))),
      hakijapalveluidenYhteystiedot = Some(Yhteystieto(
        nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
        postiosoite = Some(Osoite1),
        puhelinnumero = Map(Fi -> "123", Sv -> "123"),
        sahkoposti = Map(Fi -> "aku.ankka@ankkalinnankoulu.fi", Sv -> "aku.ankka@ankkalinnankoulu.fi"))),
      tietoaOpiskelusta = Seq(
        TietoaOpiskelusta(otsikkoKoodiUri = "organisaationkuvaustiedot_03#1",
          teksti = Map(Fi -> "Tietoa opiskelusta ", Sv -> "Tietoa opiskelusta sv"))),
      esittely = Map(Fi -> "Esittely", Sv -> "Esittely sv"),
      opiskelijoita = Some(100),
      korkeakouluja = Some(1),
      tiedekuntia = Some(2),
      kampuksia = Some(2),
      yksikoita = None,
      toimipisteita = Some(2),
      akatemioita = Some(1))),
    kielivalinta = Seq(Fi, Sv),
    organisaatioOid = ChildOid,
    muokkaaja = TestUserOid,
    modified = None)

  val MinOppilaitos: Oppilaitos = Oppilaitos(
    oid = ChildOid,
    tila = Tallennettu,
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    muokkaaja = TestUserOid)

  val JulkaistuOppilaitoksenOsa: OppilaitoksenOsa = OppilaitoksenOsa(
    oid = GrandChildOid,
    oppilaitosOid = ChildOid,
    tila = Julkaistu,
    esikatselu = true,
    metadata = Some(OppilaitoksenOsaMetadata(
      yhteystiedot = Seq(Yhteystieto(
        nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
        postiosoite = Some(Osoite1),
        puhelinnumero = Map(Fi -> "123", Sv -> "123"),
        sahkoposti = Map(Fi -> "aku.ankka@ankkalinnankoulu.fi", Sv -> "aku.ankka@ankkalinnankoulu.fi"))),
      wwwSivu = Some(NimettyLinkki(
        url = Map(Fi -> "http://www.oppilaitos.fi", Sv -> "http://www.oppilaitos.sv"),
        nimi = Map(Fi -> "Linkki suomeksi", Sv -> "Linkki ruotsiksi"))),
      esittely = Map(Fi -> "Esittely", Sv -> "Esittely sv"),
      opiskelijoita = Some(100),
      kampus = Map(Fi -> "Kampus fi", Sv -> "Kampus sv"),
      jarjestaaUrheilijanAmmKoulutusta = false)),
    kielivalinta = Seq(Fi, Sv),
    organisaatioOid = ChildOid,
    muokkaaja = TestUserOid,
    modified = None)

  val MinOppilaitoksenOsa: OppilaitoksenOsa = OppilaitoksenOsa(
    oid = GrandChildOid,
    oppilaitosOid = ChildOid,
    tila = Tallennettu,
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    muokkaaja = TestUserOid)
}
