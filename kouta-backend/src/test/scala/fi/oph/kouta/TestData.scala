package fi.oph.kouta

import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.oid._

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util.UUID

object TestData {
  def now(): LocalDateTime                   = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
  def inFuture(s: Long = 500): LocalDateTime = LocalDateTime.now().plusSeconds(s).truncatedTo(ChronoUnit.MINUTES)
  def inPast(s: Long = 500): LocalDateTime   = LocalDateTime.now().minusSeconds(s).truncatedTo(ChronoUnit.MINUTES)
  def inFourMonths(): LocalDateTime = LocalDateTime.now().plusMonths(4).truncatedTo(ChronoUnit.MINUTES)
  val startTime1: LocalDateTime =
    LocalDate.now().plusDays(1).atTime(LocalTime.parse("09:49")).truncatedTo(ChronoUnit.MINUTES)
  val endTime1: LocalDateTime =
    LocalDate.now().plusDays(1).atTime(LocalTime.parse("09:58")).truncatedTo(ChronoUnit.MINUTES)
  val muokkaajanNimi: String = "Testi Muokkaaja"

  def kieliMap(text: String): Kielistetty = Map(Fi -> s"$text fi", Sv -> s"$text sv")

  def getInvalidHakuajat = List(Ajanjakso(TestData.inFuture(9000), Some(TestData.inFuture(3000))))

  val Osoite1: Osoite =
    Osoite(osoite = Map(Fi -> "Kivatie 1", Sv -> "kivavägen 1"), postinumeroKoodiUri = Some("posti_04230#2"))
  val Osoite2: Osoite =
    Osoite(osoite = Map(Fi -> "Hauskatie 1", Sv -> "Hauskavägen 1"), postinumeroKoodiUri = Some("posti_61100#2"))
  val LiitteenToimitusosoite1 = LiitteenToimitusosoite(
    osoite = Osoite1,
    sahkoposti = Some("foo@bar.fi"),
    verkkosivu = Some("https://wwww.foobar.fi")
  )

  val Yhteystieto1: Yhteyshenkilo = Yhteyshenkilo(
    nimi = Map(Fi -> "Aku Ankka", Sv -> "Aku Ankka"),
    puhelinnumero = Map(Fi -> "123", Sv -> "123"),
    sahkoposti = Map(Fi -> "aku.ankka@ankkalinnankoulu.fi", Sv -> "aku.ankka@ankkalinnankoulu.fi"),
    titteli = Map(Fi -> "titteli", Sv -> "titteli sv"),
    wwwSivu = Map(Fi -> "http://opintopolku.fi", Sv -> "http://studieinfo.fi")
  )

  val Liite1: Liite = Liite(
    id = None,
    tyyppiKoodiUri = Some("liitetyypitamm_2#1"),
    nimi = Map(Fi -> "liite 1 Fi", Sv -> "liite 1 Sv"),
    kuvaus = Map(Fi -> "kuvaus Fi", Sv -> "kuvaus Sv"),
    toimitusaika = Some(inFuture()),
    toimitustapa = Some(Hakijapalvelu),
    toimitusosoite = None
  )

  val Liite2: Liite = Liite(
    id = None,
    tyyppiKoodiUri = Some("liitetyypitamm_1#1"),
    nimi = Map(Fi -> "liite 2 Fi", Sv -> "liite 2 Sv"),
    kuvaus = Map(Fi -> "kuvaus Fi", Sv -> "kuvaus Sv"),
    toimitusaika = None,
    toimitustapa = Some(MuuOsoite),
    toimitusosoite = Some(LiitteenToimitusosoite1)
  )

  val ValintakokeenLisatilaisuudet1: ValintakokeenLisatilaisuudet = ValintakokeenLisatilaisuudet(
    id = None,
    tilaisuudet = List(
      Valintakoetilaisuus(
        jarjestamispaikka = Map(Fi -> "Lisä järjestämispaikka fi", Sv -> "Lisä järjestämispaikka sv"),
        osoite = Some(Osoite1),
        aika = Some(Ajanjakso(alkaa = startTime1, paattyy = Some(endTime1))),
        lisatietoja = Map(Fi -> "lisätieto fi", Sv -> "lisätieto sv")
      )
    )
  )

  val Valintakoe1: Valintakoe = Valintakoe(
    id = None,
    tyyppiKoodiUri = Some("valintakokeentyyppi_1#1"),
    nimi = Map(Fi -> "valintakokeen nimi fi", Sv -> "valintakokeen nimi sv"),
    metadata = Some(
      ValintakoeMetadata(
        tietoja = Map(Fi -> "tietoa valintakokeesta fi", Sv -> "tietoa valintakokeesta sv"),
        vahimmaispisteet = Some(182.1),
        liittyyEnnakkovalmistautumista = Some(true),
        ohjeetEnnakkovalmistautumiseen = Map(Fi -> "Ennakko-ohjeet fi", Sv -> "Ennakko-ohjeet sv"),
        erityisjarjestelytMahdollisia = Some(true),
        ohjeetErityisjarjestelyihin = Map(Fi -> "Erityisvalmistelut fi", Sv -> "Erityisvalmistelut sv")
      )
    ),
    tilaisuudet = List(
      Valintakoetilaisuus(
        jarjestamispaikka = Map(Fi -> "Järjestämispaikka fi", Sv -> "Järjestämispaikka sv"),
        osoite = Some(Osoite1),
        aika = Some(Ajanjakso(alkaa = now(), paattyy = Some(inFuture().plusYears(200)))),
        lisatietoja = Map(Fi -> "lisätieto fi", Sv -> "lisätieto sv")
      )
    )
  )

  val AmmKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = true,
    esikatselu = true,
    koulutustyyppi = Amm,
    koulutuksetKoodiUri = Seq("koulutus_371101#1"),
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(
      AmmatillinenKoulutusMetadata(
        kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
        lisatiedot = Seq(
          Lisatieto(
            otsikkoKoodiUri = "koulutuksenlisatiedot_03#1",
            teksti = Map(Fi -> "Opintojen lisätieto ", Sv -> "Opintojen lisätieto sv")
          )
        ),
        isMuokkaajaOphVirkailija = Some(true)
      )
    ),
    tarjoajat = List(GrandChildOid, EvilGrandChildOid, EvilCousin),
    muokkaaja = OphUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/ammkuva"),
    ePerusteId = Some(11L),
    modified = None,
    _enrichedData = Some(
      KoulutusEnrichedData(
        muokkaajanNimi = Some(muokkaajanNimi)
      )
    )
  )

  val Lisatieto1: Lisatieto = Lisatieto(
    otsikkoKoodiUri = "koulutuksenlisatiedot_03#1",
    teksti = Map(Fi -> "Opintojen lisätieto ", Sv -> "Opintojen lisätieto sv")
  )

  val YoKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = true,
    koulutustyyppi = Yo,
    koulutuksetKoodiUri = Seq("koulutus_371101#1", "koulutus_201000#1"),
    esikatselu = true,
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(
      YliopistoKoulutusMetadata(
        tutkintonimikeKoodiUrit = Seq("tutkintonimikekk_110#2"),
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2#1"),
        kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
        lisatiedot = Seq(Lisatieto1),
        isMuokkaajaOphVirkailija = Some(true)
      )
    ),
    tarjoajat = List(YoOid, HkiYoOid),
    muokkaaja = OphUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/yokuva"),
    modified = None,
    _enrichedData = Some(
      KoulutusEnrichedData(
        muokkaajanNimi = Some(muokkaajanNimi)
      )
    )
  )

  val AmkKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = true,
    koulutustyyppi = Amk,
    koulutuksetKoodiUri = Seq("koulutus_371101#1", "koulutus_201000#1"),
    esikatselu = true,
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(
      AmmattikorkeakouluKoulutusMetadata(
        tutkintonimikeKoodiUrit = Seq("tutkintonimikekk_110#2"),
        koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso2_020#1"),
        kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
        lisatiedot = Seq(Lisatieto1),
        isMuokkaajaOphVirkailija = Some(true)
      )
    ),
    tarjoajat = List(AmkOid),
    muokkaaja = OphUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/yokuva"),
    modified = None,
    _enrichedData = Some(
      KoulutusEnrichedData(
        muokkaajanNimi = Some(muokkaajanNimi)
      )
    )
  )

  val AmmOpettajaKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = false,
    koulutustyyppi = AmmOpeErityisopeJaOpo,
    koulutuksetKoodiUri = Seq("koulutus_000002#12"),
    esikatselu = true,
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(
      AmmOpeErityisopeJaOpoKoulutusMetadata(
        tutkintonimikeKoodiUrit = Seq(),
        koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_01"),
        opintojenLaajuusNumero = None,
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2#1"),
        kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
        lisatiedot = Seq(Lisatieto1),
        isMuokkaajaOphVirkailija = Some(true)
      )
    ),
    tarjoajat = List(AmkOid),
    muokkaaja = OphUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/yokuva"),
    modified = None,
    _enrichedData = Some(
      KoulutusEnrichedData(
        muokkaajanNimi = Some(muokkaajanNimi)
      )
    )
  )

  val YoOpettajaKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = false,
    koulutustyyppi = OpePedagOpinnot,
    koulutuksetKoodiUri = Seq("koulutus_919999#12"),
    esikatselu = true,
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(
      OpePedagOpinnotKoulutusMetadata(
        tutkintonimikeKoodiUrit = Seq(),
        koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_01"),
        opintojenLaajuusNumero = None,
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2#1"),
        kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
        lisatiedot = Seq(Lisatieto1),
        isMuokkaajaOphVirkailija = Some(true)
      )
    ),
    tarjoajat = List(HkiYoOid),
    muokkaaja = OphUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/yokuva"),
    modified = None,
    _enrichedData = Some(
      KoulutusEnrichedData(
        muokkaajanNimi = Some(muokkaajanNimi)
      )
    )
  )

  val KkOpintojaksoKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = false,
    koulutustyyppi = KkOpintojakso,
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(
      KkOpintojaksoKoulutusMetadata(
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_6#1"),
        opintojenLaajuusNumero = Some(10),
        kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
        koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_001#1"),
        isMuokkaajaOphVirkailija = Some(false),
        avoinKorkeakoulutus = Some(true),
        tunniste = Some("XZY-123"),
      )
    ),
    tarjoajat = List(YoOid, HkiYoOid),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/amm-kuva"),
    modified = None,
    _enrichedData = Some(
      KoulutusEnrichedData(
        muokkaajanNimi = Some(muokkaajanNimi)
      )
    )
  )

  val ErikoislaakariKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = false,
    koulutustyyppi = Erikoislaakari,
    koulutuksetKoodiUri = List("koulutus_775101#1"),
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(
      ErikoislaakariKoulutusMetadata(
        koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso2_091#1"),
        kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
        isMuokkaajaOphVirkailija = Some(true)
      )
    ),
    tarjoajat = List(HkiYoOid),
    muokkaaja = OphUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/erikoislaakari-kuva"),
    modified = None,
    _enrichedData = Some(
      KoulutusEnrichedData(
        muokkaajanNimi = Some(muokkaajanNimi)
      )
    )
  )

  val KkOpintokokonaisuusKoulutuksenMetatieto: KkOpintokokonaisuusKoulutusMetadata = KkOpintokokonaisuusKoulutusMetadata(
    opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_6#1"),
    opintojenLaajuusNumeroMin = Some(5),
    opintojenLaajuusNumeroMax = Some(15),
    kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
    koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_001#1"),
    isMuokkaajaOphVirkailija = Some(false)
  )

  val KkOpintokokonaisuusKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = false,
    koulutustyyppi = KkOpintokokonaisuus,
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(KkOpintokokonaisuusKoulutuksenMetatieto),
    tarjoajat = List(HkiYoOid, YoOid),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/amm-kuva"),
    modified = None,
    _enrichedData = Some(
      KoulutusEnrichedData(
        muokkaajanNimi = Some(muokkaajanNimi)
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
    metadata = Some(
      AmmatillinenTutkinnonOsaKoulutusMetadata(
        tyyppi = AmmTutkinnonOsa,
        kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
        lisatiedot = Seq(Lisatieto1),
        tutkinnonOsat = Seq(TutkinnonOsa(Some(123L), Some("koulutus_371101#1"), Some(1234L), Some(122L))),
        isMuokkaajaOphVirkailija = Some(false)
      )
    ),
    tarjoajat = List(GrandChildOid, EvilGrandChildOid, EvilCousin),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/amm-kuva"),
    ePerusteId = None,
    modified = None,
    _enrichedData = Some(
      KoulutusEnrichedData(
        muokkaajanNimi = Some(muokkaajanNimi)
      )
    )
  )

  val AmmOsaamisalaKoulutus: Koulutus = AmmTutkinnonOsaKoulutus.copy(
    koulutustyyppi = AmmOsaamisala,
    koulutuksetKoodiUri = Seq("koulutus_371101#1"),
    ePerusteId = Some(11L),
    metadata = Some(
      AmmatillinenOsaamisalaKoulutusMetadata(
        tyyppi = AmmOsaamisala,
        kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
        lisatiedot = Seq(Lisatieto1),
        osaamisalaKoodiUri = Some("osaamisala_01"),
        isMuokkaajaOphVirkailija = Some(false)
      )
    )
  )

  val AmmMuuKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = false,
    koulutustyyppi = AmmMuu,
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(
      AmmatillinenMuuKoulutusMetadata(
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_6#1"),
        opintojenLaajuusNumero = Some(10),
        kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
        koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso2_080#1"),
        isMuokkaajaOphVirkailija = Some(false)
      )
    ),
    tarjoajat = List(GrandChildOid, EvilGrandChildOid, EvilCousin),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/amm-kuva"),
    modified = None,
    _enrichedData = Some(
      KoulutusEnrichedData(
        muokkaajanNimi = Some(muokkaajanNimi)
      )
    )
  )

  val LukiokoulutuksenMetatieto = LukioKoulutusMetadata(
    opintojenLaajuusNumero = Some(40),
    opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2#1"),
    koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_00#1"),
    kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
    lisatiedot = Seq(Lisatieto1),
    isMuokkaajaOphVirkailija = Some(true)
  )

  val LukioKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = true,
    koulutustyyppi = Lk,
    koulutuksetKoodiUri = Seq("koulutus_301101#1"),
    esikatselu = true,
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(LukiokoulutuksenMetatieto),
    tarjoajat = List(GrandChildOid, EvilGrandChildOid, EvilCousin),
    muokkaaja = OphUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/lkkuva"),
    modified = None,
    _enrichedData = Some(
      KoulutusEnrichedData(
        muokkaajanNimi = Some(muokkaajanNimi)
      )
    )
  )

  val TuvaKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = false,
    koulutustyyppi = Tuva,
    esikatselu = false,
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(
      TuvaKoulutusMetadata(
        kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
        linkkiEPerusteisiin = Map(Fi -> "http://testilinkki.fi", Sv -> "http://testlink.sv"),
        isMuokkaajaOphVirkailija = Some(false),
        opintojenLaajuusNumero = None,
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_8#1")
      )
    ),
    tarjoajat = List(GrandChildOid, EvilGrandChildOid, EvilCousin),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/lkkuva"),
    ePerusteId = None,
    modified = None
  )

  val TelmaKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = false,
    koulutustyyppi = Telma,
    esikatselu = false,
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(
      TelmaKoulutusMetadata(
        kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
        linkkiEPerusteisiin = Map(Fi -> "http://testilinkki.fi", Sv -> "http://testlink.sv"),
        isMuokkaajaOphVirkailija = Some(true),
        opintojenLaajuusNumero = None,
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_6#1"),
      )
    ),
    tarjoajat = List(GrandChildOid, EvilGrandChildOid, EvilCousin),
    muokkaaja = OphUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/lkkuva"),
    ePerusteId = None,
    modified = None
  )

  val VapaaSivistystyoOpistovuosiKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = false,
    koulutustyyppi = VapaaSivistystyoOpistovuosi,
    esikatselu = false,
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(
      VapaaSivistystyoOpistovuosiKoulutusMetadata(
        kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
        linkkiEPerusteisiin = Map(Fi -> "http://testilinkki.fi", Sv -> "http://testlink.sv"),
        koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_001#1"),
        opintojenLaajuusNumero = None,
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_2#1"),
        isMuokkaajaOphVirkailija = Some(false)
      )
    ),
    tarjoajat = List(GrandChildOid, EvilGrandChildOid, EvilCousin),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/lkkuva"),
    ePerusteId = None,
    modified = None
  )

  val VapaaSivistystyoMuuKoulutus: Koulutus = VapaaSivistystyoOpistovuosiKoulutus.copy(
    koulutustyyppi = VapaaSivistystyoMuu,
    metadata = Some(
      VapaaSivistystyoMuuKoulutusMetadata(
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_6#1"),
        opintojenLaajuusNumero = Some(12),
        kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
        linkkiEPerusteisiin = Map(Fi -> "http://testilinkki.fi", Sv -> "http://testlink.sv"),
        koulutusalaKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusalataso1_001#1"),
        isMuokkaajaOphVirkailija = Some(false)
      )
    )
  )

  val AikuistenPerusopetusKoulutus: Koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = false,
    koulutustyyppi = AikuistenPerusopetus,
    koulutuksetKoodiUri = Seq("koulutus_201101#12"),
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(
      AikuistenPerusopetusKoulutusMetadata(
        kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
        opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_6#1"),
        opintojenLaajuusNumero = Some(10),
        linkkiEPerusteisiin = Map(Fi -> "http://testilinkki.fi", Sv -> "http://testlink.sv"),
        isMuokkaajaOphVirkailija = Some(false)
      )
    ),
    tarjoajat = List(GrandChildOid, EvilGrandChildOid, EvilCousin),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/lkkuva"),
    modified = None,
    _enrichedData = Some(
      KoulutusEnrichedData(
        muokkaajanNimi = Some(muokkaajanNimi)
      )
    )
  )

  val MinKoulutus: Koulutus = Koulutus(
    koulutustyyppi = Amm,
    johtaaTutkintoon = false,
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    nimi = kieliMap("Minimi koulutus"),
    modified = None
  )

  val JulkaistuHaku: Haku = Haku(
    nimi = Map(Fi -> "Haku fi", Sv -> "Haku sv"),
    tila = Julkaistu,
    hakutapaKoodiUri = Some("hakutapa_03#1"),
    hakukohteenLiittamisenTakaraja = Some(inFuture()),
    hakukohteenMuokkaamisenTakaraja = Some(inFuture()),
    ajastettuJulkaisu = Some(inFuture()),
    ajastettuHaunJaHakukohteidenArkistointi = Some(inFourMonths()),
    ajastettuHaunJaHakukohteidenArkistointiAjettu = None,
    kohdejoukkoKoodiUri = Some("haunkohdejoukko_17#1"),
    kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_1#1"),
    hakulomaketyyppi = Some(MuuHakulomake),
    hakulomakeAtaruId = None,
    hakulomakeKuvaus = Map(),
    hakulomakeLinkki = Map(Fi -> "https://www.koulu.test.fi/hakemusinfo-fi", Sv -> "https://www.koulu.test.fi/hakemusinfo-sv"),
    metadata = Some(
      HakuMetadata(
        yhteyshenkilot = Seq(Yhteystieto1),
        tulevaisuudenAikataulu = Seq(Ajanjakso(alkaa = now(), paattyy = Some(inFuture().plusYears(200)))),
        koulutuksenAlkamiskausi = Some(
          KoulutuksenAlkamiskausi(
            alkamiskausityyppi = Some(AlkamiskausiJaVuosi),
            henkilokohtaisenSuunnitelmanLisatiedot = Map(Fi -> "Jotakin lisätietoa", Sv -> "Jotakin lisätietoa sv"),
            koulutuksenAlkamispaivamaara = None,
            koulutuksenPaattymispaivamaara = None,
            koulutuksenAlkamiskausiKoodiUri = Some("kausi_k#1"),
            koulutuksenAlkamisvuosi = Some(LocalDate.now().getYear.toString)
          )
        ),
        isMuokkaajaOphVirkailija = Some(false)
      )
    ),
    hakuajat = List(Ajanjakso(alkaa = now(), paattyy = Some(inFuture().plusYears(200)))),
    organisaatioOid = ChildOid,
    muokkaaja = TestUserOid,
    kielivalinta = Seq(Fi, Sv),
    modified = None,
    _enrichedData = Some(
      HakuEnrichedData(
        muokkaajanNimi = Some(muokkaajanNimi)
      )
    )
  )

  val MinHaku: Haku = Haku(
    muokkaaja = TestUserOid,
    organisaatioOid = LonelyOid,
    kielivalinta = Seq(Fi, Sv),
    nimi = kieliMap("Minimi haku"),
    modified = None,
    hakutapaKoodiUri = Some("hakutapa_03#1"),
    kohdejoukkoKoodiUri = Some("haunkohdejoukko_17#1")
  )

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
    //hakulomakeAtaruId = Some(UUID.randomUUID()),
    hakulomakeKuvaus = Map(
      Fi -> "Hakulomake tulostetaan ja toimitetaan postitse",
      Sv -> "Hakulomake tulostetaan ja toimitetaan postitse sv"
    ),
    //hakulomakeLinkki =
    //  Map(Fi -> "https://koulu.test/kohteen-hakemusinfo-fi", Sv -> "https://koulu.test/kohteen-hakemusinfo-sv"),
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
    metadata = Some(
      HakukohdeMetadata(
        valintakokeidenYleiskuvaus = Map(Fi -> "yleiskuvaus fi", Sv -> "yleiskuvaus sv"),
        valintaperusteenValintakokeidenLisatilaisuudet = List(ValintakokeenLisatilaisuudet1),
        kynnysehto = Map(Fi -> "Kynnysehto fi", Sv -> "Kynnysehto sv"),
        kaytetaanHaunAlkamiskautta = Some(false),
        koulutuksenAlkamiskausi = Some(
          KoulutuksenAlkamiskausi(
            alkamiskausityyppi = Some(TarkkaAlkamisajankohta),
            koulutuksenAlkamisvuosi = None,
            koulutuksenAlkamiskausiKoodiUri = None,
            koulutuksenAlkamispaivamaara = Some(inFuture(20000)),
            koulutuksenPaattymispaivamaara = Some(inFuture(30000))
          )
        ),
        aloituspaikat = Some(
          Aloituspaikat(
            lukumaara = Some(100),
            ensikertalaisille = Some(50),
            kuvaus = Map(Fi -> "aloituspaikkojen kuvaus fi", Sv -> "aloituspaikkojen kuvaus sv")
          )
        ),
        isMuokkaajaOphVirkailija = Some(false)
      )
    ),
    valintakokeet = List(Valintakoe1),
    hakuajat = List(Ajanjakso(alkaa = now(), paattyy = Some(inFuture().plusYears(200)))),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    modified = None,
    _enrichedData = Some(
      HakukohdeEnrichedData(
        esitysnimi = Map(Fi -> "Hakukohde fi", Sv -> "Hakukohde sv"),
        muokkaajanNimi = Some(muokkaajanNimi)
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
    modified = None
  )

  val MinHakukohdeListItem: HakukohdeListItem = HakukohdeListItem(
    oid = HakukohdeOid("1.2.246.562.20.123"),
    valintaperusteId = None,
    jarjestyspaikkaOid = None,
    muokkaaja = TestUserOid,
    tila = Tallennettu,
    organisaatioOid = ChildOid,
    nimi = kieliMap("Minimi hakukohde"),
    toteutusOid = ToteutusOid("1.2.246.562.17.123"),
    hakuOid = HakuOid("1.2.246.562.29.123"),
    modified = Modified(LocalDateTime.now().minusDays(1))
  )

  val LukioHakukohteenLinja = HakukohteenLinja(
    alinHyvaksyttyKeskiarvo = Some(8.2),
    lisatietoa = Map(Fi -> "Linjan lisatieto fi", Sv -> "Linjan lisatieto sv"),
    painotetutArvosanat = Seq(
      PainotettuOppiaine(
        Some(OppiaineKoodiUrit(Some("painotettavatoppiaineetlukiossa_b3pt"), Some("kieli_FI"))),
        Some(2.5)
      ),
      PainotettuOppiaine(
        Some(OppiaineKoodiUrit(Some("painotettavatoppiaineetlukiossa_b1lt"), Some("kieli_SV"))),
        Some(2.8)
      )
    )
  )
  val Taulukko1: Taulukko = Taulukko(
    id = None,
    nimi = Map(Fi -> "Taulukko 1", Sv -> "Taulukko 1 sv"),
    rows = Seq(
      Row(
        index = 0,
        isHeader = true,
        columns = Seq(
          Column(index = 0, text = Map(Fi -> "Otsikko", Sv -> "Otsikko sv")),
          Column(index = 1, text = Map(Fi -> "Otsikko 2", Sv -> "Otsikko 2 sv"))
        )
      ),
      Row(
        index = 1,
        isHeader = false,
        columns = Seq(
          Column(index = 0, text = Map(Fi -> "Tekstiä", Sv -> "Tekstiä sv")),
          Column(index = 1, text = Map(Fi -> "Tekstiä 2", Sv -> "Tekstiä 2 sv"))
        )
      )
    )
  )

  val Taulukko2: Taulukko = Taulukko(
    id = None,
    nimi = Map(Fi -> "Taulukko 2", Sv -> "Taulukko 2 sv"),
    rows = Seq(
      Row(
        index = 0,
        isHeader = true,
        columns = Seq(
          Column(index = 0, text = Map(Fi -> "Otsikko", Sv -> "Otsikko sv")),
          Column(index = 1, text = Map(Fi -> "Otsikko 2", Sv -> "Otsikko 2 sv"))
        )
      ),
      Row(
        index = 1,
        isHeader = false,
        columns = Seq(
          Column(index = 0, text = Map(Fi -> "Tekstiä", Sv -> "Tekstiä sv")),
          Column(index = 1, text = Map(Fi -> "Tekstiä 2", Sv -> "Tekstiä 2 sv"))
        )
      )
    )
  )

  val Valintatapa1: Valintatapa = Valintatapa(
    nimi = kieliMap("Valintatapa1"),
    valintatapaKoodiUri = Some("valintatapajono_av#1"),
    sisalto = Seq(SisaltoTeksti(kieliMap("Sisaltoteksti")), Taulukko1, Taulukko2),
    kaytaMuuntotaulukkoa = false,
    kynnysehto = kieliMap("kynnysehto"),
    enimmaispisteet = Some(201.15),
    vahimmaispisteet = Some(182.1)
  )

  val Valintatapa2: Valintatapa = Valintatapa(
    nimi = kieliMap("Valintatapa2"),
    valintatapaKoodiUri = Some("valintatapajono_tv#1"),
    sisalto = Seq(SisaltoTeksti(kieliMap("Sisaltoteksti")), Taulukko2),
    kaytaMuuntotaulukkoa = true,
    kynnysehto = kieliMap("kynnysehto"),
    enimmaispisteet = Some(18.1),
    vahimmaispisteet = Some(10.1)
  )

  val LkValintaperusteMetadata = GenericValintaperusteMetadata(
    tyyppi = Lk,
    valintatavat = Seq(Valintatapa1, Valintatapa2),
    valintakokeidenYleiskuvaus = Map(Fi -> "yleiskuvaus fi", Sv -> "yleiskuvaus sv"),
    kuvaus = kieliMap("kuvaus"),
    lisatiedot = Map(Fi -> "lisatiedot", Sv -> "lisatiedot sv"),
    hakukelpoisuus = Map(Fi -> "hakukelpoisuus", Sv -> "hakukelpoisuus sv"),
    sisalto = Seq(SisaltoTeksti(kieliMap("Sisaltoteksti")), Taulukko1, Taulukko2),
    isMuokkaajaOphVirkailija = Some(false)
  )

  val YoValintaperusteMetadata = LkValintaperusteMetadata.copy(tyyppi = Yo)

  val AmmValintaperusteMetadata = LkValintaperusteMetadata.copy(tyyppi = Amm)

  val AmmTutkinnonOsaValintaperusteMetadata = LkValintaperusteMetadata.copy(tyyppi = AmmTutkinnonOsa)

  val AmmOsaamisalaValintaperusteMetadata = LkValintaperusteMetadata.copy(tyyppi = AmmOsaamisala)

  val AmmMuuValintaperusteMetadata = LkValintaperusteMetadata.copy(tyyppi = AmmMuu)

  val TuvaValintaperusteMetadata = LkValintaperusteMetadata.copy(tyyppi = Tuva)

  val MuuTyyppiValintaperusteMetadata = LkValintaperusteMetadata.copy(tyyppi = Muu)

  val AmmValintaperuste: Valintaperuste = Valintaperuste(
    koulutustyyppi = Amm,
    id = None,
    tila = Julkaistu,
    hakutapaKoodiUri = Some("hakutapa_02#1"),
    kohdejoukkoKoodiUri = Some("haunkohdejoukko_17#1"),
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    esikatselu = true,
    valintakokeet = List(Valintakoe1),
    metadata = Some(AmmValintaperusteMetadata),
    organisaatioOid = ChildOid,
    muokkaaja = TestUserOid,
    kielivalinta = List(Fi, Sv),
    modified = None,
    _enrichedData = Some(ValintaperusteEnrichedData(muokkaajanNimi = Some(muokkaajanNimi)))
  )

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
    modified = None,
    _enrichedData = Some(ValintaperusteEnrichedData(muokkaajanNimi = Some(muokkaajanNimi)))
  )

  val MinYoValintaperuste: Valintaperuste = Valintaperuste(
    koulutustyyppi = Yo,
    kielivalinta = Seq(Fi, Sv),
    nimi = kieliMap("Minimi valintaperuste"),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    modified = None,
    _enrichedData = Some(ValintaperusteEnrichedData(muokkaajanNimi = Some(muokkaajanNimi)))
  )

  val AmmMuuValintaperuste: Valintaperuste = Valintaperuste(
    koulutustyyppi = AmmMuu,
    id = None,
    tila = Julkaistu,
    hakutapaKoodiUri = Some("hakutapa_02#1"),
    kohdejoukkoKoodiUri = Some("haunkohdejoukko_17#1"),
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    esikatselu = true,
    julkinen = false,
    valintakokeet = List(Valintakoe1),
    metadata = Some(AmmMuuValintaperusteMetadata),
    organisaatioOid = ChildOid,
    muokkaaja = TestUserOid,
    kielivalinta = List(Fi, Sv),
    modified = None,
    _enrichedData = Some(ValintaperusteEnrichedData(muokkaajanNimi = Some(muokkaajanNimi)))
  )

  val YoSorakuvaus: Sorakuvaus = Sorakuvaus(
    id = None,
    tila = Julkaistu,
    nimi = kieliMap("nimi"),
    koulutustyyppi = Yo,
    kielivalinta = List(Fi, Sv),
    metadata = Some(
      SorakuvausMetadata(
        kuvaus = kieliMap("kuvaus"),
        koulutusKoodiUrit = Seq("koulutus_371101#1"),
        koulutusalaKoodiUri = Some("kansallinenkoulutusluokitus2016koulutusalataso2_054#1"),
        isMuokkaajaOphVirkailija = Some(true)
      )
    ),
    organisaatioOid = OphOid,
    muokkaaja = OphUserOid,
    modified = None,
    _enrichedData = Some(SorakuvausEnrichedData(muokkaajanNimi = Some(muokkaajanNimi)))
  )

  val AmmSorakuvaus: Sorakuvaus = YoSorakuvaus.copy(koulutustyyppi = Amm)

  val MinSorakuvaus: Sorakuvaus = Sorakuvaus(
    koulutustyyppi = Yo,
    kielivalinta = Seq(Fi, Sv),
    nimi = kieliMap("Minimi sorakuvaus"),
    muokkaaja = OphUserOid,
    organisaatioOid = OphOid,
    modified = None
  )

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
    koulutuksenAlkamiskausi = Some(
      KoulutuksenAlkamiskausi(
        alkamiskausityyppi = Some(AlkamiskausiJaVuosi),
        henkilokohtaisenSuunnitelmanLisatiedot = Map(Fi -> "Jotakin lisätietoa", Sv -> "Jotakin lisätietoa sv"),
        koulutuksenAlkamispaivamaara = None,
        koulutuksenPaattymispaivamaara = None,
        koulutuksenAlkamiskausiKoodiUri = Some("kausi_k#1"),
        koulutuksenAlkamisvuosi = Some(LocalDate.now().getYear.toString)
      )
    ),
    lisatiedot = Seq(
      Lisatieto(
        otsikkoKoodiUri = "koulutuksenlisatiedot_03#1",
        teksti = Map(Fi -> "Opintojen rakenteen kuvaus", Sv -> "Rakenne kuvaus sv")
      ),
      Lisatieto(
        otsikkoKoodiUri = "koulutuksenlisatiedot_03#1",
        teksti = Map(Fi -> "Sisältö kuvaus", Sv -> "Sisältö kuvaus sv")
      )
    ),
    onkoApuraha = true,
    apuraha = Some(
      Apuraha(
        min = Some(100),
        max = Some(200),
        yksikko = Some(Euro),
        kuvaus = Map(Fi -> "apurahakuvaus fi", Sv -> "apurahakuvaus sv")
      )
    ),
    suunniteltuKestoVuodet = Some(3),
    suunniteltuKestoKuukaudet = Some(10),
    suunniteltuKestoKuvaus = Map(Fi -> "Keston kuvaus fi", Sv -> "Keston kuvaus sv")
  )

  val AmmToteutuksenMetatieto: AmmatillinenToteutusMetadata = AmmatillinenToteutusMetadata(
    kuvaus = Map(),
    osaamisalat = List(
      AmmatillinenOsaamisala(
        "osaamisala_0001#1",
        linkki = Map(Fi -> "http://osaamisala.fi/linkki/fi", Sv -> "http://osaamisala.fi/linkki/sv"),
        otsikko =
          Map(Fi -> "Katso osaamisalan tarkempi kuvaus tästä", Sv -> "Katso osaamisalan tarkempi kuvaus tästä sv")
      )
    ),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    ammattinimikkeet = List(Keyword(Fi, "insinööri"), Keyword(Fi, "koneinsinööri")),
    yhteyshenkilot = Seq(Yhteystieto1),
    isMuokkaajaOphVirkailija = Some(false),
    hasJotpaRahoitus = Some(false)
  )

  val LukioToteutuksenMetatieto: LukioToteutusMetadata = LukioToteutusMetadata(
    kuvaus = Map(),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "tutkija")),
    ammattinimikkeet = List(),
    yhteyshenkilot = Seq(Yhteystieto1),
    kielivalikoima = Some(
      Kielivalikoima(
        A1Kielet = Seq("kieli_EN#1"),
        A2Kielet = Seq("kieli_DE#1"),
        B1Kielet = Seq("kieli_SV#1"),
        B2Kielet = Seq("kieli_FR#1"),
        B3Kielet = Seq("kieli_ES#1"),
        aidinkielet = Seq("kieli_FI#1"),
        muutKielet = Seq("kieli_ET#1")
      )
    ),
    yleislinja = true,
    painotukset = List(
      LukiolinjaTieto(koodiUri = "lukiopainotukset_1#1", Map(Fi -> "painotus 1 kuvaus", Sv -> "painotus 1 kuvaus sv"))
    ),
    erityisetKoulutustehtavat = List(
      LukiolinjaTieto(
        koodiUri = "lukiolinjaterityinenkoulutustehtava_1#1",
        Map(Fi -> "erityinen koulutustehtava 1 kuvaus", Sv -> "erityinen koulutustehtava 1 kuvaus sv")
      )
    ),
    diplomit = List(
      LukiodiplomiTieto(
        koodiUri = "moduulikoodistolops2021_kald3#1",
        linkki = Map(Fi -> "http://linkki.fi", Sv -> "http://link.se"),
        linkinAltTeksti = Map(Fi -> "Suomeksi", Sv -> "På svenska")
      )
    ),
    isMuokkaajaOphVirkailija = Some(false),
    hasJotpaRahoitus = Some(false)
  )

  val YoToteutuksenMetatieto: YliopistoToteutusMetadata = YliopistoToteutusMetadata(
    kuvaus = Map(),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    ammattinimikkeet = List(Keyword(Fi, "insinööri"), Keyword(Fi, "koneinsinööri")),
    yhteyshenkilot = Seq(Yhteystieto1),
    isMuokkaajaOphVirkailija = Some(false),
    hasJotpaRahoitus = Some(false))

  val KkOpintojaksoToteutuksenMetatieto: KkOpintojaksoToteutusMetadata = KkOpintojaksoToteutusMetadata(
    kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    hakutermi = Some(Hakeutuminen),
    hakulomaketyyppi = Some(MuuHakulomake),
    hakulomakeLinkki = Map(Fi -> "http://www.linkki.fi", Sv -> "http://www.linkki.se"),
    lisatietoaHakeutumisesta = Map(Fi -> "Lisätieto", Sv -> "Lisätieto sv"),
    lisatietoaValintaperusteista = Map(Fi -> "Lisätieto", Sv -> "Lisätieto sv"),
    hakuaika = Some(Ajanjakso(alkaa = now(), paattyy = Some(inFuture().plusYears(200)))),
    yhteyshenkilot = Seq(Yhteystieto1),
    aloituspaikat = None,
    isMuokkaajaOphVirkailija = Some(false),
    hasJotpaRahoitus = Some(false),
    avoinKorkeakoulutus = Some(true),
  )

  val KkOpintokokonaisuusToteutuksenMetatieto: KkOpintokokonaisuusToteutusMetadata = KkOpintokokonaisuusToteutusMetadata(
    kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"),
    opintojenLaajuusNumero = Some(10),
    opintojenLaajuusyksikkoKoodiUri = Some("opintojenlaajuusyksikko_6#1"),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    hakutermi = Some(Hakeutuminen),
    hakulomaketyyppi = Some(MuuHakulomake),
    hakulomakeLinkki = Map(Fi -> "http://www.linkki.fi", Sv -> "http://www.linkki.se"),
    lisatietoaHakeutumisesta = Map(Fi -> "Lisätieto", Sv -> "Lisätieto sv"),
    lisatietoaValintaperusteista = Map(Fi -> "Lisätieto", Sv -> "Lisätieto sv"),
    hakuaika = Some(Ajanjakso(alkaa = now(), paattyy = Some(inFuture().plusYears(200)))),
    yhteyshenkilot = Seq(Yhteystieto1),
    aloituspaikat = None,
    isMuokkaajaOphVirkailija = Some(false),
    hasJotpaRahoitus = Some(false))

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
    koulutuksetKoodiUri = Seq("koulutus_371101#1"),
    _enrichedData = Some(
      ToteutusEnrichedData(
        esitysnimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
        muokkaajanNimi = Some(muokkaajanNimi)
      )
    )
  )

  val JulkaistuYoToteutus: Toteutus = JulkaistuAmmToteutus.copy(metadata = Some(YoToteutuksenMetatieto), tarjoajat = List(YoOid, HkiYoOid))

  val JulkaistuKkOpintojaksoToteutus: Toteutus =
    JulkaistuAmmToteutus.copy(metadata = Some(KkOpintojaksoToteutuksenMetatieto), tarjoajat = List(YoOid, HkiYoOid))
  val JulkaistuKkOpintokokonaisuusToteutus: Toteutus =
    JulkaistuAmmToteutus.copy(metadata = Some(KkOpintokokonaisuusToteutuksenMetatieto), tarjoajat = List(YoOid, HkiYoOid))
  val JulkaistuAmkToteutus: Toteutus =
    JulkaistuAmmToteutus.copy(metadata = Some(YoToteutuksenMetatieto.copy(tyyppi = Amk)), tarjoajat = List(AmkOid))

  val AmmOpettajaToteutuksenMetatieto = AmmOpeErityisopeJaOpoToteutusMetadata(
    tyyppi = AmmOpeErityisopeJaOpo,
    kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    yhteyshenkilot = Seq(Yhteystieto1),
    aloituspaikat = Some(23),
    isMuokkaajaOphVirkailija = Some(false),
    hasJotpaRahoitus = Some(false)
  )

  val JulkaistuAmmOpettajaToteutus =
    JulkaistuAmmToteutus.copy(metadata = Some(AmmOpettajaToteutuksenMetatieto), tarjoajat = List(AmkOid))
  val JulkaistuYoOpettajaToteutus =
    JulkaistuAmmOpettajaToteutus.copy(metadata = Some(AmmOpettajaToteutuksenMetatieto.copy(tyyppi = OpePedagOpinnot)), tarjoajat = List(HkiYoOid))

  val TuvaToteutuksenMetatieto: TuvaToteutusMetadata = TuvaToteutusMetadata(
    tyyppi = Tuva,
    kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    jarjestetaanErityisopetuksena = true,
    yhteyshenkilot = Seq(Yhteystieto1),
    aloituspaikat = Some(23),
    isMuokkaajaOphVirkailija = Some(false),
    hasJotpaRahoitus = Some(false)
  )

  val TuvaToteutus: Toteutus = JulkaistuAmmToteutus.copy(metadata = Some(TuvaToteutuksenMetatieto))

  val TelmaToteutuksenMetatieto: TelmaToteutusMetadata = TelmaToteutusMetadata(
    tyyppi = Telma,
    kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    yhteyshenkilot = Seq(Yhteystieto1),
    aloituspaikat = Some(23),
    isMuokkaajaOphVirkailija = Some(false),
    hasJotpaRahoitus = Some(false)
  )

  val TelmaToteutus: Toteutus = JulkaistuAmmToteutus.copy(metadata = Some(TelmaToteutuksenMetatieto))

  val VapaaSivistystyoOpistovuosiToteutusMetatieto: VapaaSivistystyoOpistovuosiToteutusMetadata =
    VapaaSivistystyoOpistovuosiToteutusMetadata(
      tyyppi = VapaaSivistystyoOpistovuosi,
      kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"),
      opetus = Some(ToteutuksenOpetus),
      asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
      yhteyshenkilot = Seq(Yhteystieto1),
      isMuokkaajaOphVirkailija = Some(false),
      hasJotpaRahoitus = Some(false))

  val VapaaSivistystyoOpistovuosiToteutus: Toteutus =
    JulkaistuAmmToteutus.copy(metadata = Some(VapaaSivistystyoOpistovuosiToteutusMetatieto))

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
    hakuaika = Some(Ajanjakso(alkaa = now(), paattyy = Some(inFuture().plusYears(200)))),
    yhteyshenkilot = Seq(Yhteystieto1),
    aloituspaikat = None,
    hasJotpaRahoitus = Some(false))

  val VapaaSivistystyoMuuToteutusHakemuspalveluMetatieto: VapaaSivistystyoMuuToteutusMetadata =
    VapaaSivistystyoMuuToteutusMetatieto.copy(
      hakutermi = Some(Hakeutuminen),
      hakulomaketyyppi = Some(Ataru),
      hakulomakeLinkki = Map(),
      lisatietoaHakeutumisesta = Map(),
      lisatietoaValintaperusteista = Map(),
      hakuaika = None
    )

  val VapaaSivistystyoMuuToteutus: Toteutus =
    JulkaistuAmmToteutus.copy(metadata = Some(VapaaSivistystyoMuuToteutusMetatieto))

  val AikuistenPerusopetusToteutusMetatieto: AikuistenPerusopetusToteutusMetadata =
    AikuistenPerusopetusToteutusMetadata(
      tyyppi = AikuistenPerusopetus,
      kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"),
      opetus = Some(ToteutuksenOpetus),
      asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
      yhteyshenkilot = Seq(Yhteystieto1),
      hakutermi = Some(Hakeutuminen),
      hakulomaketyyppi = Some(MuuHakulomake),
      hakulomakeLinkki = Map(Fi -> "http://www.linkki.fi", Sv -> "http://www.linkki.se"),
      lisatietoaHakeutumisesta = Map(Fi -> "Lisätieto", Sv -> "Lisätieto sv"),
      lisatietoaValintaperusteista = Map(Fi -> "Lisätieto", Sv -> "Lisätieto sv"),
      hakuaika = Some(Ajanjakso(alkaa = now(), paattyy = Some(inFuture().plusYears(200)))),
      aloituspaikat = Some(100),
      isMuokkaajaOphVirkailija = Some(false),
      hasJotpaRahoitus = Some(false)
    )

  val AikuistenPerusopetusToteutus: Toteutus =
    JulkaistuAmmToteutus.copy(metadata = Some(AikuistenPerusopetusToteutusMetatieto))

  val MinToteutus: Toteutus = Toteutus(
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    koulutusOid = KoulutusOid("1.2.246.562.13.123"),
    kielivalinta = Seq(Fi, Sv),
    nimi = kieliMap("Minimi toteutus"),
    modified = None,
    _enrichedData = Some(
      ToteutusEnrichedData(
        esitysnimi = kieliMap("Minimi toteutus"),
        muokkaajanNimi = Some(muokkaajanNimi)
      )
    )
  )

  val AmmOsaamisalaToteutus: Toteutus = JulkaistuAmmToteutus.copy(
    metadata = Some(
      AmmatillinenOsaamisalaToteutusMetadata(
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
        hakuaika = Some(Ajanjakso(alkaa = now(), paattyy = Some(inFuture().plusYears(200)))),
        aloituspaikat = Some(23),
        isMuokkaajaOphVirkailija = Some(false),
        hasJotpaRahoitus = Some(false)
      )
    )
  )

  val LukioToteutus: Toteutus = Toteutus(
    oid = None,
    koulutusOid = KoulutusOid("1.2.246.562.13.123"),
    tila = Julkaistu,
    esikatselu = true,
    tarjoajat = List(OtherOid, LukioOid),
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(LukioToteutuksenMetatieto),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    modified = None,
    _enrichedData = Some(
      ToteutusEnrichedData(
        esitysnimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
        muokkaajanNimi = Some(muokkaajanNimi)
      )
    )
  )

  val AmmOsaamisalaToteutusMetadataHakemuspalvelu: AmmatillinenOsaamisalaToteutusMetadata =
    AmmatillinenOsaamisalaToteutusMetadata(
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
      aloituspaikat = Some(23),
      hasJotpaRahoitus = Some(false))

  val AmmOsaamisalaToteutusMetadataEiSahkoista: AmmatillinenOsaamisalaToteutusMetadata =
    AmmatillinenOsaamisalaToteutusMetadata(
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
      aloituspaikat = Some(23),
      hasJotpaRahoitus = Some(false))

  val AmmTutkinnonOsaToteutus: Toteutus = JulkaistuAmmToteutus.copy(
    metadata = Some(
      AmmatillinenTutkinnonOsaToteutusMetadata(
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
        hakuaika = Some(Ajanjakso(alkaa = now(), paattyy = Some(inFuture().plusYears(200)))),
        aloituspaikat = Some(22),
        isMuokkaajaOphVirkailija = Some(false),
        hasJotpaRahoitus = Some(false)
      )
    )
  )

  val AmmTutkinnonOsaToteutusMetadataHakemuspalvelu: AmmatillinenTutkinnonOsaToteutusMetadata =
    AmmatillinenTutkinnonOsaToteutusMetadata(
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
      aloituspaikat = Some(23),
      hasJotpaRahoitus = Some(false)
    )

  val AmmTutkinnonOsaToteutusMetadataEiSahkoista: AmmatillinenTutkinnonOsaToteutusMetadata =
    AmmatillinenTutkinnonOsaToteutusMetadata(
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
      aloituspaikat = Some(23),
      hasJotpaRahoitus = Some(false)
    )

  val AmmMuuToteutusMetatieto: AmmatillinenMuuToteutusMetadata =
      AmmatillinenMuuToteutusMetadata(
        kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"),
        opetus = Some(ToteutuksenOpetus),
        asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
        hakutermi = Some(Hakeutuminen),
        hakulomaketyyppi = Some(MuuHakulomake),
        hakulomakeLinkki = Map(Fi -> "http://www.linkki.fi", Sv -> "http://www.linkki.se"),
        lisatietoaHakeutumisesta = Map(Fi -> "Lisätieto", Sv -> "Lisätieto sv"),
        lisatietoaValintaperusteista = Map(Fi -> "Lisätieto", Sv -> "Lisätieto sv"),
        hakuaika = Some(Ajanjakso(alkaa = now(), paattyy = Some(inFuture().plusYears(200)))),
        yhteyshenkilot = Seq(Yhteystieto1),
        isMuokkaajaOphVirkailija = Some(false),
        hasJotpaRahoitus = Some(false)
      )

  val AmmMuuToteutus: Toteutus = JulkaistuAmmToteutus.copy(metadata =
    Some(AmmMuuToteutusMetatieto))

  val JulkaistuOppilaitos: Oppilaitos = Oppilaitos(
    oid = ChildOid,
    tila = Julkaistu,
    esikatselu = true,
    metadata = Some(
      OppilaitosMetadata(
        wwwSivu = Some(
          NimettyLinkki(
            url = Map(Fi -> "http://www.oppilaitos.fi", Sv -> "http://www.oppilaitos.sv"),
            nimi = Map(Fi -> "Linkki suomeksi", Sv -> "Linkki ruotsiksi")
          )
        ),
        hakijapalveluidenYhteystiedot = Some(
          Yhteystieto(
            nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
            postiosoite = Some(Osoite1),
            kayntiosoite = Some(Osoite2),
            puhelinnumero = Map(Fi -> "123", Sv -> "123"),
            sahkoposti = Map(Fi -> "aku.ankka@ankkalinnankoulu.fi", Sv -> "aku.ankka@ankkalinnankoulu.fi")
          )
        ),
        tietoaOpiskelusta = Seq(
          TietoaOpiskelusta(
            otsikkoKoodiUri = "organisaationkuvaustiedot_03#1",
            teksti = Map(Fi -> "Tietoa opiskelusta ", Sv -> "Tietoa opiskelusta sv")
          )
        ),
        esittely = Map(Fi -> "Esittely", Sv -> "Esittely sv"),
        opiskelijoita = Some(100),
        korkeakouluja = Some(1),
        tiedekuntia = Some(2),
        kampuksia = Some(2),
        yksikoita = None,
        toimipisteita = Some(2),
        akatemioita = Some(1),
        isMuokkaajaOphVirkailija = Some(false)
      )
    ),
    kielivalinta = Seq(Fi, Sv),
    organisaatioOid = ChildOid,
    muokkaaja = TestUserOid,
    modified = None,
    _enrichedData = Some(OppilaitosEnrichedData(muokkaajanNimi = Some(muokkaajanNimi)))
  )

  val MinOppilaitos: Oppilaitos = Oppilaitos(
    oid = ChildOid,
    tila = Tallennettu,
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    muokkaaja = TestUserOid
  )

  val JulkaistuOppilaitoksenOsa: OppilaitoksenOsa = OppilaitoksenOsa(
    oid = GrandChildOid,
    oppilaitosOid = ChildOid,
    tila = Julkaistu,
    esikatselu = true,
    metadata = Some(
      OppilaitoksenOsaMetadata(
        hakijapalveluidenYhteystiedot = Some(
          Yhteystieto(
            nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
            postiosoite = Some(Osoite1),
            kayntiosoite = Some(Osoite2),
            puhelinnumero = Map(Fi -> "123", Sv -> "123"),
            sahkoposti = Map(Fi -> "aku.ankka@ankkalinnankoulu.fi", Sv -> "aku.ankka@ankkalinnankoulu.fi")
          )),
        wwwSivu = Some(
          NimettyLinkki(
            url = Map(Fi -> "http://www.oppilaitos.fi", Sv -> "http://www.oppilaitos.sv"),
            nimi = Map(Fi -> "Linkki suomeksi", Sv -> "Linkki ruotsiksi")
          )
        ),
        esittely = Map(Fi -> "Esittely", Sv -> "Esittely sv"),
        opiskelijoita = Some(100),
        kampus = Map(Fi -> "Kampus fi", Sv -> "Kampus sv"),
        isMuokkaajaOphVirkailija = Some(false)
      )
    ),
    kielivalinta = Seq(Fi, Sv),
    organisaatioOid = ChildOid,
    muokkaaja = TestUserOid,
    modified = None,
    _enrichedData = Some(OppilaitosEnrichedData(muokkaajanNimi = Some(muokkaajanNimi)))
  )

  val MinOppilaitoksenOsa: OppilaitoksenOsa = OppilaitoksenOsa(
    oid = GrandChildOid,
    oppilaitosOid = ChildOid,
    tila = Tallennettu,
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    muokkaaja = TestUserOid
  )

  val orgServiceOrganisaatio: Organisaatio = Organisaatio(
    oid = ChildOid.toString,
    parentOidPath = s"${ChildOid.toString}/1.2.246.562.10.97036773279/1.2.246.562.10.00000000001",
    oppilaitostyyppi = Some("oppilaitostyyppi_21#1"),
    nimi = Map(Fi -> "Organisaatio", Sv -> "Organisation", En -> "Organization"),
    kotipaikkaUri = Some("kunta_123"),
    children = List(
      OrganisaationOsa(
        oid = GrandChildOid.toString,
        parentOidPath =
          s"${GrandChildOid.toString}/${ChildOid.toString}/1.2.246.562.10.97036773279/1.2.246.562.10.00000000001",
        nimi = Map(Fi -> "Organisation osa", Sv -> "Barn Organisation", En -> "Child Organization"),
        kotipaikkaUri = Some("kunta_123"),
        children = List(),
        organisaatiotyypit = List("organisaatiotyyppi_1")
      )
    )
  )

  val inPastAikaleima             = inPast(1000)
  val inPastJakso                 = Ajanjakso(inPast(2000), Some(inPastAikaleima))
  val inPastValintakoeTilaisuudet = List(Valintakoe1.tilaisuudet.head.copy(aika = Some(inPastJakso)))
  val inPastValintakokeet         = List(Valintakoe1.copy(tilaisuudet = inPastValintakoeTilaisuudet))
  def inPastLisaTilaisuudet(id: Option[UUID]): List[ValintakokeenLisatilaisuudet] =
    List(ValintakokeenLisatilaisuudet1.copy(id = id, tilaisuudet = inPastValintakoeTilaisuudet))
  val inPastKoulutuksenAlkamiskausi = KoulutuksenAlkamiskausi(
    alkamiskausityyppi = Some(TarkkaAlkamisajankohta),
    koulutuksenAlkamispaivamaara = Some(inFuture(1000)),
    koulutuksenPaattymispaivamaara = Some(inFuture(2000)),
    koulutuksenAlkamisvuosi = Some("2020")
  )
}
