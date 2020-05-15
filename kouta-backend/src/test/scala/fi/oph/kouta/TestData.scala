package fi.oph.kouta

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.oid._

object TestData {

  def now() = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
  def inFuture(s:Long = 500) = LocalDateTime.now().plusSeconds(s).truncatedTo(ChronoUnit.MINUTES)
  def inPast(s:Long = 500) = LocalDateTime.now().minusSeconds(s).truncatedTo(ChronoUnit.MINUTES)

  def kieliMap(text: String): Kielistetty = Map(Fi -> s"$text fi", Sv -> s"$text sv")

  def getInvalidHakuajat = List(Ajanjakso(TestData.inFuture(9000), Some(TestData.inFuture(3000))))

  val Osoite1 = Osoite(
    osoite = Map(Fi -> "Kivatie 1", Sv -> "kivavägen 1"),
    postinumeroKoodiUri = Some("posti_04230#2"))

  val Yhteystieto1 = Yhteyshenkilo(
    nimi = Map(Fi -> "Aku Ankka", Sv -> "Aku Ankka"),
    puhelinnumero = Map(Fi -> "123", Sv -> "123"),
    sahkoposti = Map(Fi -> "aku.ankka@ankkalinnankoulu.fi", Sv -> "aku.ankka@ankkalinnankoulu.fi"),
    titteli = Map(Fi -> "titteli", Sv -> "titteli sv"),
    wwwSivu = Map(Fi -> "http://opintopolku.fi", Sv -> "http://studieinfo.fi"))

  val Liite1 = Liite(
    id = None,
    tyyppiKoodiUri = Some("liitetyypitamm_2#1"),
    nimi = Map(Fi -> "liite 1 Fi", Sv -> "liite 1 Sv"),
    kuvaus = Map(Fi -> "kuvaus Fi", Sv -> "kuvaus Sv"),
    toimitusaika = Some(inFuture()),
    toimitustapa = Some(Hakijapalvelu),
    toimitusosoite = None)

  val Liite2 = Liite(
    id = None,
    tyyppiKoodiUri = Some("liitetyypitamm_1#1"),
    nimi = Map(Fi -> "liite 2 Fi", Sv -> "liite 2 Sv"),
    kuvaus = Map(Fi -> "kuvaus Fi", Sv -> "kuvaus Sv"),
    toimitusaika = None,
    toimitustapa = Some(MuuOsoite),
    toimitusosoite = Some(LiitteenToimitusosoite(osoite = Osoite1, sahkoposti = Some("foo@bar.fi"))))

  val Valintakoe1 = Valintakoe(
    id = None,
    tyyppiKoodiUri = Some("valintakokeentyyppi_1#1"),
    tilaisuudet = List(Valintakoetilaisuus(
      osoite = Some(Osoite1),
      aika = Some(Ajanjakso(alkaa = now(), paattyy = Some(inFuture()))),
      lisatietoja = Map(Fi -> "lisätietieto fi", Sv -> "lisätieto sv"))))

  val AmmKoulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = true,
    koulutustyyppi = Amm,
    koulutusKoodiUri = Some("koulutus_371101#1"),
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(AmmatillinenKoulutusMetadata(
      kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
      lisatiedot = Seq(
        Lisatieto(otsikkoKoodiUri = "koulutuksenlisatiedot_03#1",
          teksti = Map(Fi -> "Opintojen lisätieto ", Sv -> "Opintojen lisätieto sv"))))),
    tarjoajat = List(GrandChildOid, EvilGrandChildOid, EvilCousin),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/ammkuva"),
    ePerusteId = Some(11L),
    modified = None)

  val YoKoulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = true,
    koulutustyyppi = Yo,
    koulutusKoodiUri = Some("koulutus_371101#1"),
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(YliopistoKoulutusMetadata(
      tutkintonimikeKoodiUrit = Seq("tutkintonimikekk_110#2"),
      opintojenLaajuusKoodiUri = Some("opintojenlaajuus_40#1"),
      kuvauksenNimi = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
      kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
      lisatiedot = Seq(
        Lisatieto(otsikkoKoodiUri = "koulutuksenlisatiedot_03#1",
          teksti = Map(Fi -> "Opintojen lisätieto ", Sv -> "Opintojen lisätieto sv"))))),
    tarjoajat = List(GrandChildOid, EvilGrandChildOid, EvilCousin),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    teemakuva = Some("http://kuva.fi/yokuva"),
    ePerusteId = Some(12L),
    modified = None)

  val MinKoulutus = Koulutus(
    koulutustyyppi = Amm,
    johtaaTutkintoon = false,
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    nimi = kieliMap("Minimi koulutus"),
    modified = None)

  val JulkaistuHaku = Haku(
    nimi = Map(Fi -> "Haku fi", Sv -> "Haku sv"),
    tila = Julkaistu,
    hakutapaKoodiUri = Some("hakutapa_03#1"),
    hakukohteenLiittamisenTakaraja = Some(inFuture()),
    hakukohteenMuokkaamisenTakaraja = Some(inFuture()),
    ajastettuJulkaisu = Some(inFuture()),
    alkamiskausiKoodiUri = Some("kausi_k#1"),
    alkamisvuosi = Some(LocalDate.now().getYear.toString),
    kohdejoukkoKoodiUri = Some("haunkohdejoukko_17#1"),
    kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_1#1"),
    hakulomaketyyppi = Some(EiSähköistä),
    hakulomakeAtaruId = Some(UUID.randomUUID()),
    hakulomakeKuvaus = Map( Fi -> "Hakulomake tulostetaan ja toimitetaan postitse", Sv -> "Hakulomake tulostetaan ja toimitetaan postitse sv"),
    hakulomakeLinkki = Map( Fi -> "https://koulu.test/hakemusinfo-fi", Sv -> "https://koulu.test/hakemusinfo-sv"),
    metadata = Some(HakuMetadata(Seq(Yhteystieto1), Seq(Ajanjakso(alkaa = now(), paattyy = Some(inFuture()))))),
    hakuajat = List(Ajanjakso(alkaa = now(), paattyy = Some(inFuture()))),
    organisaatioOid = ChildOid,
    muokkaaja = TestUserOid,
    kielivalinta = Seq(Fi, Sv),
    modified = None)

  val MinHaku = Haku(
    muokkaaja = TestUserOid,
    organisaatioOid = LonelyOid,
    kielivalinta = Seq(Fi, Sv),
    nimi = kieliMap("Minimi haku"),
    modified = None)

  val JulkaistuHakukohde = Hakukohde(
    oid = None,
    toteutusOid = ToteutusOid("1.2.246.562.17.123"),
    hakuOid = HakuOid("1.2.246.562.29.123"),
    tila = Julkaistu,
    nimi = Map(Fi -> "Hakukohde fi", Sv -> "Hakukohde sv"),
    alkamiskausiKoodiUri = Some("kausi_k#1"),
    alkamisvuosi = Some(LocalDate.now().getYear.toString),
    kaytetaanHaunAlkamiskautta = Some(false),
    hakulomaketyyppi = Some(EiSähköistä),
    hakulomakeAtaruId = Some(UUID.randomUUID()),
    hakulomakeKuvaus = Map( Fi -> "Hakulomake tulostetaan ja toimitetaan postitse", Sv -> "Hakulomake tulostetaan ja toimitetaan postitse sv"),
    hakulomakeLinkki = Map( Fi -> "https://koulu.test/kohteen-hakemusinfo-fi", Sv -> "https://koulu.test/kohteen-hakemusinfo-sv"),
    kaytetaanHaunHakulomaketta = Some(false),
    aloituspaikat = Some(2),
    ensikertalaisenAloituspaikat = Some(1),
    pohjakoulutusvaatimusKoodiUrit = Seq("pohjakoulutusvaatimustoinenaste_pk#1", "pohjakoulutusvaatimustoinenaste_yo#1"),
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
    valintakokeet = List(Valintakoe1),
    hakuajat = List(Ajanjakso(alkaa = now(), paattyy = Some(inFuture()))),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    modified = None)

  val MinHakukohde = Hakukohde(
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    nimi = kieliMap("Minimi hakukohde"),
    toteutusOid = ToteutusOid("1.2.246.562.17.123"),
    hakuOid = HakuOid("1.2.246.562.29.123"),
    modified = None)

  val Taulukko1 = Taulukko(
    id = None,
    nimi = Map(Fi -> "Taulukko 1", Sv -> "Taulukko 1 sv"),
    rows = Seq(
      Row(index = 0, isHeader = true, columns = Seq(
        Column(index = 0, text = Map(Fi -> "Otsikko", Sv -> "Otsikko sv")),
        Column(index = 1, text = Map(Fi -> "Otsikko 2", Sv -> "Otsikko 2 sv")))),
      Row(index = 1, isHeader = false, columns = Seq(
        Column(index = 0, text = Map(Fi -> "Tekstiä", Sv -> "Tekstiä sv")),
        Column(index = 1, text = Map(Fi -> "Tekstiä 2", Sv -> "Tekstiä 2 sv"))))))

  val Taulukko2 = Taulukko(
    id = None,
    nimi = Map(Fi -> "Taulukko 2", Sv -> "Taulukko 2 sv"),
    rows = Seq(
      Row(index = 0, isHeader = true, columns = Seq(
        Column(index = 0, text = Map(Fi -> "Otsikko", Sv -> "Otsikko sv")),
        Column(index = 1, text = Map(Fi -> "Otsikko 2", Sv -> "Otsikko 2 sv")))),
      Row(index = 1, isHeader = false, columns = Seq(
        Column(index = 0, text = Map(Fi -> "Tekstiä", Sv -> "Tekstiä sv")),
        Column(index = 1, text = Map(Fi -> "Tekstiä 2", Sv -> "Tekstiä 2 sv"))))))

  val Valintatapa1 = Valintatapa(
    nimi = kieliMap("Valintatapa1"),
    valintatapaKoodiUri = Some("valintatapajono_av#1"),
    kuvaus = kieliMap("kuvaus"),
    sisalto = Seq(ValintatapaSisaltoTeksti(kieliMap("Sisaltoteksti")), Taulukko1, Taulukko2),
    kaytaMuuntotaulukkoa = false,
    kynnysehto = kieliMap("kynnysehto"),
    enimmaispisteet = Some(201.15),
    vahimmaispisteet = Some(182.1))

  val Valintatapa2 = Valintatapa(
    nimi = kieliMap("Valintatapa2"),
    valintatapaKoodiUri = Some("valintatapajono_tv#1"),
    kuvaus = kieliMap("kuvaus 2"),
    sisalto = Seq(ValintatapaSisaltoTeksti(kieliMap("Sisaltoteksti")), Taulukko2),
    kaytaMuuntotaulukkoa = true,
    kynnysehto = kieliMap("kynnysehto"),
    enimmaispisteet = Some(18.1),
    vahimmaispisteet = Some(10.1))

  val Kielitaitovaatimus1 = ValintaperusteKielitaitovaatimus(
    kieliKoodiUri = Some("kieli_en#1"),
    kielitaidonVoiOsoittaa = Seq(
      Kielitaito(kielitaitoKoodiUri = Some("kielitaidonosoittaminen_01#1") ),
      Kielitaito(kielitaitoKoodiUri = Some("kielitaidonosoittaminen_03#1"), lisatieto = Map( Fi -> "muu", Sv -> "muu sv"))),
    vaatimukset = Seq(
      Kielitaitovaatimus(
        kielitaitovaatimusKoodiUri = Some("kielitaitovaatimustyypit_01#1"),
        kielitaitovaatimusKuvaukset = Seq(
          KielitaitovaatimusKuvaus(
            kielitaitovaatimusKuvausKoodiUri = Some("kielitaitovaatimustyypitkuvaus_01#1"),
            kielitaitovaatimusTaso = Some("1")))),
      Kielitaitovaatimus(
        kielitaitovaatimusKoodiUri = Some("kielitaitovaatimustyypit_02#1"),
        kielitaitovaatimusKuvaukset = Seq(
          KielitaitovaatimusKuvaus(
            kielitaitovaatimusKuvausKoodiUri = Some("kielitaitovaatimustyypitkuvaus_01#1"),
            kielitaitovaatimusTaso = Some("A")),
          KielitaitovaatimusKuvaus(
            kielitaitovaatimusKuvausKoodiUri = Some("kielitaitovaatimustyypitkuvaus_02#1"),
            kielitaitovaatimusTaso = Some("A"))))))

  val YoValintaperusteMetadata = YliopistoValintaperusteMetadata(
    valintatavat = Seq(Valintatapa1, Valintatapa2),
    kielitaitovaatimukset = Seq(Kielitaitovaatimus1),
    osaamistaustaKoodiUrit = Seq("osaamistausta_001#1"),
    kuvaus = kieliMap("kuvaus"))

  val AmmValintaperusteMetadata = AmmatillinenValintaperusteMetadata(
    valintatavat = Seq(Valintatapa1, Valintatapa2),
    kielitaitovaatimukset = Seq(Kielitaitovaatimus1),
    kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"))

  val AmmValintaperuste = Valintaperuste(
    koulutustyyppi = Amm,
    id = None,
    tila = Julkaistu,
    hakutapaKoodiUri = Some("hakutapa_02#1"),
    kohdejoukkoKoodiUri = Some("haunkohdejoukko_17#1"),
    kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_1#1"),
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    julkinen = false,
    valintakokeet = List(Valintakoe1),
    metadata = Some(AmmValintaperusteMetadata),
    sorakuvausId = Some(UUID.randomUUID()),
    organisaatioOid = ChildOid,
    muokkaaja = TestUserOid,
    kielivalinta = List(Fi, Sv),
    modified = None)

  val YoValintaperuste = Valintaperuste(
    koulutustyyppi = Yo,
    id = None,
    tila = Julkaistu,
    hakutapaKoodiUri = Some("hakutapa_03#1"),
    kohdejoukkoKoodiUri = Some("haunkohdejoukko_15#1"),
    kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_2#1"),
    nimi = kieliMap("nimi"),
    julkinen = true,
    valintakokeet = List(Valintakoe1),
    metadata = Some(YoValintaperusteMetadata),
    sorakuvausId = Some(UUID.randomUUID()),
    organisaatioOid = ChildOid,
    muokkaaja = TestUserOid,
    kielivalinta = List(Fi, Sv),
    modified = None)

  val MinYoValintaperuste = Valintaperuste(
    koulutustyyppi = Yo,
    kielivalinta = Seq(Fi, Sv),
    nimi = kieliMap("Minimi valintaperuste"),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    modified = None)

  val YoSorakuvaus = Sorakuvaus(
    id = None,
    tila = Julkaistu,
    nimi = kieliMap("nimi"),
    koulutustyyppi = Yo,
    julkinen = false,
    kielivalinta = List(Fi, Sv),
    metadata = Some(SorakuvausMetadata(kuvaus = kieliMap("kuvaus"))),
    organisaatioOid = ChildOid,
    muokkaaja = TestUserOid,
    modified = None)

  val AmmSorakuvaus = YoSorakuvaus.copy(koulutustyyppi = Amm)

  val MinSorakuvaus = Sorakuvaus(
    koulutustyyppi = Yo,
    kielivalinta = Seq(Fi, Sv),
    nimi = kieliMap("Minimi sorakuvaus"),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    modified = None)

  val ToteutuksenOpetus = Opetus(
    opetuskieliKoodiUrit = Seq("oppilaitoksenopetuskieli_1#1"),
    opetuskieletKuvaus = Map(Fi -> "Kielikuvaus fi", Sv -> "Kielikuvaus sv"),
    opetusaikaKoodiUrit = Seq("opetusaikakk_1#1"),
    opetusaikaKuvaus = Map(Fi -> "Opetusaikakuvaus fi", Sv -> "Opetusaikakuvaus sv"),
    opetustapaKoodiUrit = Seq("opetuspaikkakk_1#1", "opetuspaikkakk_2#1"),
    opetustapaKuvaus = Map(Fi -> "Opetustapakuvaus fi", Sv -> "Opetustapakuvaus sv"),
    onkoMaksullinen = Some(true),
    maksullisuusKuvaus = Map(Fi -> "Maksullisuuskuvaus fi", Sv -> "Maksullisuuskuvaus sv"),
    maksunMaara = Some(200.5),
    koulutuksenTarkkaAlkamisaika = true,
    koulutuksenAlkamispaivamaara = Some(inFuture(20000)),
    koulutuksenPaattymispaivamaara = Some(inFuture(30000)),
    lisatiedot = Seq(
      Lisatieto(otsikkoKoodiUri = "koulutuksenlisatiedot_03#1",
           teksti = Map(Fi -> "Opintojen rakenteen kuvaus", Sv -> "Rakenne kuvaus sv")),
      Lisatieto(otsikkoKoodiUri = "koulutuksenlisatiedot_03#1",
           teksti = Map(Fi -> "Sisältö kuvaus", Sv -> "Sisältö kuvaus sv"))),
    onkoStipendia = Some(false),
    stipendinMaara = Some(100.0),
  )

  val AmmToteutuksenMetatieto = AmmatillinenToteutusMetadata(
    kuvaus = Map(),
    osaamisalat = List(AmmatillinenOsaamisala("osaamisala_0001#1",
      linkki = Map(Fi -> "http://osaamisala.fi/linkki/fi", Sv -> "http://osaamisala.fi/linkki/sv"),
      otsikko = Map(Fi -> "Katso osaamisalan tarkempi kuvaus tästä", Sv -> "Katso osaamisalan tarkempi kuvaus tästä sv"))),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    ammattinimikkeet = List(Keyword(Fi, "insinööri"), Keyword(Fi, "koneinsinööri")),
    yhteyshenkilot = Seq(Yhteystieto1))

  val YoToteutuksenMetatieto = YliopistoToteutusMetadata(
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

  val JulkaistuAmmToteutus = Toteutus(
    oid = None,
    koulutusOid = KoulutusOid("1.2.246.562.13.123"),
    tila = Julkaistu,
    tarjoajat = List(OtherOid, AmmOid),
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(AmmToteutuksenMetatieto),
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    modified = None)

  val JulkaistuYoToteutus = JulkaistuAmmToteutus.copy(metadata = Some(YoToteutuksenMetatieto))

  val MinToteutus = Toteutus(
    muokkaaja = TestUserOid,
    organisaatioOid = ChildOid,
    koulutusOid = KoulutusOid("1.2.246.562.13.123"),
    kielivalinta = Seq(Fi, Sv),
    nimi = kieliMap("Minimi toteutus"),
    modified = None)

  val JulkaistuOppilaitos = Oppilaitos(
    oid = ChildOid,
    tila = Julkaistu,
    metadata = Some(OppilaitosMetadata(
      yhteystiedot = Some(Yhteystieto(
        osoite = Some(Osoite1),
        wwwSivu = Map(Fi -> "http://www.oppilaitos.fi", Sv -> "http://www.oppilaitos.sv"),
        puhelinnumero = Map(Fi -> "123", Sv -> "123"),
        sahkoposti = Map(Fi -> "aku.ankka@ankkalinnankoulu.fi", Sv -> "aku.ankka@ankkalinnankoulu.fi"))),
      tietoaOpiskelusta = Seq(
        Lisatieto(otsikkoKoodiUri = "koulutuksenlisatiedot_03#1",
          teksti = Map(Fi -> "Opintojen lisätieto ", Sv -> "Opintojen lisätieto sv"))),
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

  val MinOppilaitos = Oppilaitos(
    oid = ChildOid,
    tila = Tallennettu,
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    muokkaaja = TestUserOid)

  val JulkaistuOppilaitoksenOsa = OppilaitoksenOsa(
    oid = GrandChildOid,
    oppilaitosOid = ChildOid,
    tila = Julkaistu,
    metadata = Some(OppilaitoksenOsaMetadata(
      yhteystiedot = Some(Yhteystieto(
        osoite = Some(Osoite1),
        wwwSivu = Map(Fi -> "https://www.tiedekunta.fi", Sv -> "https://www.tiedekunta.sv"),
        puhelinnumero = Map(Fi -> "123", Sv -> "123"),
        sahkoposti = Map(Fi -> "aku.ankka@ankkalinnankoulu.fi", Sv -> "aku.ankka@ankkalinnankoulu.fi"))),
      esittely = Map(Fi -> "Esittely", Sv -> "Esittely sv"),
      opiskelijoita = Some(100),
      kampus = Map(Fi -> "Kampus fi", Sv -> "Kampus sv"))),
    kielivalinta = Seq(Fi, Sv),
    organisaatioOid = ChildOid,
    muokkaaja = TestUserOid,
    modified = None)

  val MinOppilaitoksenOsa = OppilaitoksenOsa(
    oid = GrandChildOid,
    oppilaitosOid = ChildOid,
    tila = Tallennettu,
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    muokkaaja = TestUserOid)
}
