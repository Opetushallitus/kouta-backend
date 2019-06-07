package fi.oph.kouta

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.OrganisaatioServiceMock.{ChildOid, EvilGrandChildOid, GrandChildOid, EvilCousin}

object TestData {

  def now() = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
  def inFuture(s:Long = 500) = LocalDateTime.now().plusSeconds(s).truncatedTo(ChronoUnit.MINUTES)

  def kieliMap(text: String): Kielistetty = Map(Fi -> s"$text fi", Sv -> s"$text sv")

  val Osoite1 = Osoite(
    osoite = Map(Fi -> "Kivatie 1", Sv -> "kivavägen 1"),
    postinumero = Some("12345"),
    postitoimipaikka = Map(Fi -> "Kaupunki", Sv -> "SV kaupunki"))

  val Yhteystieto1 = Yhteystieto(
    nimi = Map(Fi -> "Aku Ankka", Sv -> "Aku Ankka"),
    puhelinnumero = Map(Fi -> "123", Sv -> "123"),
    sahkoposti = Map(Fi -> "aku.ankka@ankkalinnankoulu.fi", Sv -> "aku.ankka@ankkalinnankoulu.fi"),
    titteli = Map(Fi -> "titteli", Sv -> "titteli sv"),
    wwwSivu = Map(Fi -> "http://opintopolku.fi", Sv -> "http://studieinfo.fi"))

  val Liite1 = Liite(
    id = None,
    tyyppi = Some("moi"),
    nimi = Map(Fi -> "liite 1 Fi", Sv -> "liite 1 Sv"),
    kuvaus = Map(Fi -> "kuvaus Fi", Sv -> "kuvaus Sv"),
    toimitusaika = Some(inFuture()),
    toimitustapa = Some(Hakijapalvelu),
    toimitusosoite = None)

  val Liite2 = Liite(
    id = None,
    tyyppi = Some("terve"),
    nimi = Map(Fi -> "liite 2 Fi", Sv -> "liite 2 Sv"),
    kuvaus = Map(Fi -> "kuvaus Fi", Sv -> "kuvaus Sv"),
    toimitusaika = None,
    toimitustapa = Some(MuuOsoite),
    toimitusosoite = Some(LiitteenToimitusosoite(osoite = Osoite1, sahkoposti = Some("foo@bar.fi"))))

  val Valintakoe1 = Valintakoe(
    id = None,
    tyyppi = Some("valintakokeentyyppi_1#1"),
    tilaisuudet = List(Valintakoetilaisuus(
      osoite = Some(Osoite1),
      aika = Some(Ajanjakso(alkaa = now(), paattyy = inFuture())),
      lisatietoja = Map(Fi -> "lisätietieto fi", Sv -> "lisätieto sv"))))

  val AmmKoulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = true,
    koulutustyyppi = Some(Amm),
    koulutusKoodiUri = Some("koulutus_371101#1"),
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(new AmmatillinenKoulutusMetadata(
      kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
      lisatiedot = Seq(
        Lisatieto(otsikkoKoodiUri = "koulutuksenjarjestamisenlisaosiot_3#1",
          teksti = Map(Fi -> "Opintojen lisätieto ", Sv -> "Opintojen lisätieto sv"))))),
    tarjoajat = List(GrandChildOid, EvilGrandChildOid, EvilCousin),
    muokkaaja = UserOid("5.5.5"),
    organisaatioOid = ChildOid,
    kielivalinta = List(Fi, Sv),
    modified = None)

  val YoKoulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = true,
    koulutustyyppi = Some(Yo),
    koulutusKoodiUri = Some("koulutus_371101#1"),
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(new YliopistoKoulutusMetadata(
      tutkintonimikeKoodiUrit = Seq("tutkintonimike_1#1"),
      opintojenLaajuusKoodiUri = Some("opintojenlaajuus_1#1"),
      kuvauksenNimi = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
      kuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv"),
      lisatiedot = Seq(
        Lisatieto(otsikkoKoodiUri = "koulutuksenjarjestamisenlisaosiot_3#1",
          teksti = Map(Fi -> "Opintojen lisätieto ", Sv -> "Opintojen lisätieto sv"))))),
    tarjoajat = List("1.2", "2.2", "3.2").map(OrganisaatioOid),
    muokkaaja = UserOid("5.5.5"),
    organisaatioOid = OrganisaatioOid("1.2"),
    kielivalinta = List(Fi, Sv),
    modified = None)

  val MinKoulutus = Koulutus(johtaaTutkintoon = false, muokkaaja = UserOid("1.2.3"), organisaatioOid = OrganisaatioOid("1.2"), modified = None)

  val JulkaistuHaku = new Haku(
    nimi = Map(Fi -> "Haku fi", Sv -> "Haku sv"),
    tila = Julkaistu,
    hakutapaKoodiUri = Some("hakutapa_03#1"),
    hakukohteenLiittamisenTakaraja = Some(inFuture()),
    hakukohteenMuokkaamisenTakaraja = Some(inFuture()),
    ajastettuJulkaisu = Some(inFuture()),
    alkamiskausiKoodiUri = Some("kausi_k#1"),
    alkamisvuosi = Some("2019"),
    kohdejoukkoKoodiUri = Some("haunkohdejoukko_02#2"),
    kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_1#11"),
    hakulomaketyyppi = Some(EiSähköistä),
    hakulomakeAtaruId = Some(UUID.randomUUID()),
    hakulomakeKuvaus = Map( Fi -> "Hakulomake tulostetaan ja toimitetaan postitse", Sv -> "Hakulomake tulostetaan ja toimitetaan postitse sv"),
    hakulomakeLinkki = Map( Fi -> "https://koulu.test/hakemusinfo-fi", Sv -> "https://koulu.test/hakemusinfo-sv"),
    metadata = Some(HakuMetadata(Some(Yhteystieto1), Seq(Ajanjakso(alkaa = now(), paattyy = inFuture())))),
    hakuajat = List(Ajanjakso(alkaa = now(), paattyy = inFuture())),
    organisaatioOid = OrganisaatioOid("1.2.3.4"),
    muokkaaja = UserOid("5.4.3.2.1"),
    kielivalinta = Seq(Fi, Sv),
    modified = None)

  val MinHaku = new Haku(muokkaaja = UserOid("9.9.9.9.9"), organisaatioOid = OrganisaatioOid("5.5.5"), modified = None)

  val JulkaistuHakukohde = new Hakukohde(
    oid = None,
    toteutusOid = ToteutusOid("1.2.246.562.17.123"),
    hakuOid = HakuOid("1.2.246.562.29.123"),
    tila = Julkaistu,
    nimi = Map(Fi -> "Hakukohde fi", Sv -> "Hakukohde sv"),
    alkamiskausiKoodiUri = Some("kausi_k#1"),
    alkamisvuosi = Some("2019"),
    hakulomaketyyppi = Some(EiSähköistä),
    hakulomakeAtaruId = Some(UUID.randomUUID()),
    hakulomakeKuvaus = Map( Fi -> "Hakulomake tulostetaan ja toimitetaan postitse", Sv -> "Hakulomake tulostetaan ja toimitetaan postitse sv"),
    hakulomakeLinkki = Map( Fi -> "https://koulu.test/kohteen-hakemusinfo-fi", Sv -> "https://koulu.test/kohteen-hakemusinfo-sv"),
    eriHakulomakeKuinHaulla = Some(true),
    aloituspaikat = Some(2),
    ensikertalaisenAloituspaikat = Some(1),
    pohjakoulutusvaatimusKoodiUrit = Seq("pohjakoulutusvaatimustoinenaste_01#2", "pohjakoulutusvaatimustoinenaste_01#3"),
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
    hakuajat = List(Ajanjakso(alkaa = now(), paattyy = inFuture())),
    muokkaaja = UserOid("1.2.3.2.1"),
    organisaatioOid = OrganisaatioOid("1.2"),
    kielivalinta = Seq(Fi, Sv),
    modified = None)

  val MinHakukohde = new Hakukohde(
    muokkaaja = UserOid("7.7.7.7"),
    organisaatioOid = OrganisaatioOid("1.2"),
    toteutusOid = ToteutusOid("1.2.246.562.17.123"),
    hakuOid = HakuOid("1.2.246.562.29.123"),
    modified = None)

  val Taulukko1 = new Taulukko(
    id = None,
    nimi = Map(Fi -> "Taulukko 1", Sv -> "Taulukko 1 sv"),
    rows = Seq(
      new Row(index = 0, isHeader = true, columns = Seq(
        Column(index = 0, text = Map(Fi -> "Otsikko", Sv -> "Otsikko sv")),
        Column(index = 1, text = Map(Fi -> "Otsikko 2", Sv -> "Otsikko 2 sv")))),
      new Row(index = 1, isHeader = false, columns = Seq(
        Column(index = 0, text = Map(Fi -> "Tekstiä", Sv -> "Tekstiä sv")),
        Column(index = 1, text = Map(Fi -> "Tekstiä 2", Sv -> "Tekstiä 2 sv"))))))

  val Taulukko2 = new Taulukko(
    id = None,
    nimi = Map(Fi -> "Taulukko 2", Sv -> "Taulukko 2 sv"),
    rows = Seq(
      new Row(index = 0, isHeader = true, columns = Seq(
        Column(index = 0, text = Map(Fi -> "Otsikko", Sv -> "Otsikko sv")),
        Column(index = 1, text = Map(Fi -> "Otsikko 2", Sv -> "Otsikko 2 sv")))),
      new Row(index = 1, isHeader = false, columns = Seq(
        Column(index = 0, text = Map(Fi -> "Tekstiä", Sv -> "Tekstiä sv")),
        Column(index = 1, text = Map(Fi -> "Tekstiä 2", Sv -> "Tekstiä 2 sv"))))))

  val AmmValintatapa1 = AmmatillinenValintatapa(
    valintatapaKoodiUri = Some("valintatapajono_av#1"),
    kuvaus = kieliMap("kuvaus"),
    sisalto = Seq(ValintatapaSisaltoTeksti(kieliMap("Sisaltoteksti")), Taulukko1, Taulukko2),
    kaytaMuuntotaulukkoa = false,
    kynnysehto = kieliMap("kynnysehto"),
    enimmaispisteet = Some(201.15),
    vahimmaispisteet = Some(182.1))

  val AmmValintatapa2 = AmmatillinenValintatapa(
    valintatapaKoodiUri = Some("valintatapajono_tv#1"),
    kuvaus = kieliMap("kuvaus 2"),
    sisalto = Seq(ValintatapaSisaltoTeksti(kieliMap("Sisaltoteksti")), Taulukko2),
    kaytaMuuntotaulukkoa = true,
    kynnysehto = kieliMap("kynnysehto"),
    enimmaispisteet = Some(18.1),
    vahimmaispisteet = Some(10.1))

  val YoValintatapa1 = YliopistoValintatapa(
    nimi = kieliMap("Valintatapa1"),
    valintatapaKoodiUri = Some("valintatapajono_av#1"),
    kuvaus = kieliMap("kuvaus"),
    sisalto = Seq(ValintatapaSisaltoTeksti(kieliMap("Sisaltoteksti")), Taulukko1, Taulukko2),
    kaytaMuuntotaulukkoa = false,
    kynnysehto = kieliMap("kynnysehto"),
    enimmaispisteet = Some(201.15),
    vahimmaispisteet = Some(182.1))

  val YoValintatapa2 = YliopistoValintatapa(
    valintatapaKoodiUri = Some("valintatapajono_tv#1"),
    kuvaus = kieliMap("kuvaus 2"),
    sisalto = Seq(ValintatapaSisaltoTeksti(kieliMap("Sisaltoteksti")), Taulukko2),
    kaytaMuuntotaulukkoa = true,
    kynnysehto = kieliMap("kynnysehto"),
    enimmaispisteet = Some(18.1),
    vahimmaispisteet = Some(10.1))

  val Kielitaitovaatimus1 = new ValintaperusteKielitaitovaatimus(
    kieliKoodiUri = Some("kieli_en#1"),
    kielitaidonVoiOsoittaa = Seq(
      new Kielitaito(kielitaitoKoodiUri = Some("jokukoodiuri_kk#1") ),
      new Kielitaito(kielitaitoKoodiUri = Some("jokukoodiuri_muu#1"), lisatieto = Map( Fi -> "muu", Sv -> "muu sv"))),
    vaatimukset = Seq(
      new Kielitaitovaatimus(
        kielitaitovaatimusKoodiUri = Some("koodiuri_yl#1"),
        kielitaitovaatimusKuvaukset = Seq(new KielitaitovaatimusKuvaus(
          kielitaitovaatimusKuvausKoodiUri = Some("koodiUri_read#1"),
          kielitaitovaatimusTaso = Some("1")))),
      new Kielitaitovaatimus(
        kielitaitovaatimusKoodiUri = Some("koodiuri_toefl#1"),
        kielitaitovaatimusKuvaukset = Seq(
          new KielitaitovaatimusKuvaus(
            kielitaitovaatimusKuvausKoodiUri = Some("koodiUri_read#1"),
            kielitaitovaatimusTaso = Some("A")),
          new KielitaitovaatimusKuvaus(
            kielitaitovaatimusKuvausKoodiUri = Some("koodiUri_write#1"),
            kielitaitovaatimusTaso = Some("A"))))))

  val yoValintaperusteMetadata = YliopistoValintaperusteMetadata(
    valintatavat = Seq(YoValintatapa1, YoValintatapa2),
    kielitaitovaatimukset = Seq(Kielitaitovaatimus1),
    osaamistaustaKoodiUrit = Seq("osaamistausta_03"),
    kuvaus = kieliMap("kuvaus"))

  val ammValintaperusteMetadata = AmmatillinenValintaperusteMetadata(
    valintatavat = Seq(AmmValintatapa1, AmmValintatapa2),
    kielitaitovaatimukset = Seq(Kielitaitovaatimus1))

  val AmmValintaperuste = Valintaperuste(
    koulutustyyppi = Amm,
    id = None,
    tila = Julkaistu,
    hakutapaKoodiUri = Some("hakutapa_01#1"),
    kohdejoukkoKoodiUri = Some("haunkohdejoukko_02#2"),
    kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_1#11"),
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    onkoJulkinen = true,
    metadata = Some(ammValintaperusteMetadata),
    organisaatioOid = OrganisaatioOid("1.2.3.4"),
    muokkaaja = UserOid("2.1.2.1.2"),
    kielivalinta = List(Fi, Sv),
    modified = None)

  val YoValintaperuste = Valintaperuste(
    koulutustyyppi = Yo,
    id = None,
    tila = Julkaistu,
    hakutapaKoodiUri = Some("hakutapa_01#1"),
    kohdejoukkoKoodiUri = Some("haunkohdejoukko_02#2"),
    kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_1#11"),
    nimi = kieliMap("nimi"),
    onkoJulkinen = true,
    metadata = Some(yoValintaperusteMetadata),
    organisaatioOid = OrganisaatioOid("1.2.3.4"),
    muokkaaja = UserOid("2.1.2.1.2"),
    kielivalinta = List(Fi, Sv),
    modified = None)

  val MinYoValintaperuste = Valintaperuste(koulutustyyppi = Yo, muokkaaja = UserOid("7.7.7.7.7"), organisaatioOid = OrganisaatioOid("1.2.1.2"), modified = None)

  val ToteutuksenOpetus = Opetus(
    opetuskieliKoodiUrit = Seq("kieli_fi#1"),
    opetuskieletKuvaus = Map(Fi -> "Kielikuvaus fi", Sv -> "Kielikuvaus sv"),
    opetusaikaKoodiUri = Some("opetusaikakk_1#1"),
    opetusaikaKuvaus = Map(Fi -> "Opetusaikakuvaus fi", Sv -> "Opetusaikakuvaus sv"),
    opetustapaKoodiUrit = Seq("opetuspaikkakk_1#1", "opetuspaikkakk_2#1"),
    opetustapaKuvaus = Map(Fi -> "Opetustapakuvaus fi", Sv -> "Opetustapakuvaus sv"),
    onkoMaksullinen = Some(true),
    maksullisuusKuvaus = Map(Fi -> "Maksullisuuskuvaus fi", Sv -> "Maksullisuuskuvaus sv"),
    maksunMaara = Map(Fi -> "200,50 euroa", Sv -> "200,50 euro"),
    alkamiskausiKoodiUri = Some("kausi_k#1"),
    alkamisvuosi = Some("2020"),
    alkamisaikaKuvaus = Map(Fi -> "Aikakuvaus fi", Sv -> "Aikakuvaus sv"),
    lisatiedot = Seq(
      Lisatieto(otsikkoKoodiUri = "koulutuksenjarjestamisenlisaosiot_2#1",
           teksti = Map(Fi -> "Opintojen rakenteen kuvaus", Sv -> "Rakenne kuvaus sv")),
      Lisatieto(otsikkoKoodiUri = "koulutuksenjarjestamisenlisaosiot_1#1",
           teksti = Map(Fi -> "Sisältö kuvaus", Sv -> "Sisältö kuvaus sv"))),
    onkoStipendia = Some(false),
    onkoLukuvuosimaksua = Some(false),
    stipendinMaara = Map(Fi -> "100 euroa", Sv -> "100 euro"),
    lukuvuosimaksu = Map(Fi -> "200 euroa", Sv -> "200 euro")
  )

  val AmmToteutuksenMetatieto = AmmatillinenToteutusMetadata(
    kuvaus = Map(),
    osaamisalat = List(AmmatillinenOsaamisala("osaamisala_koodi_uri#1",
      linkki = Map(Fi -> "http://osaamisala/linkki/fi", Sv -> "http://osaamisala/linkki/sv"),
      otsikko = Map(Fi -> "Katso osaamisalan tarkempi kuvaus tästä", Sv -> "Katso osaamisalan tarkempi kuvaus tästä sv"))),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    ammattinimikkeet = List(Keyword(Fi, "insinööri"), Keyword(Fi, "koneinsinööri")),
    yhteystieto = Some(Yhteystieto1))

  val YoToteutuksenMetaTieto = YliopistoToteutusMetadata(
    kuvaus = Map(),
    alemmanKorkeakoulututkinnonOsaamisalat = Seq(KorkeakouluOsaamisala(
      linkki = Map(Fi -> "http://osaamisala/linkki/fi", Sv -> "http://osaamisala/linkki/sv"),
      otsikko = Map(Fi -> "Katso osaamisalan tarkempi kuvaus tästä", Sv -> "Katso osaamisalan tarkempi kuvaus tästä sv"),
      nimi = Map(Fi -> "Nimi", Sv -> "Namn"),
      kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"))),
    ylemmanKorkeakoulututkinnonOsaamisalat = Seq(KorkeakouluOsaamisala(
      linkki = Map(Fi -> "http://osaamisala/linkki/fi", Sv -> "http://osaamisala/linkki/sv"),
      otsikko = Map(Fi -> "Katso osaamisalan tarkempi kuvaus tästä", Sv -> "Katso osaamisalan tarkempi kuvaus tästä sv"),
      nimi = Map(Fi -> "Nimi", Sv -> "Namn"),
      kuvaus = Map(Fi -> "Kuvaus", Sv -> "Kuvaus sv"))),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    ammattinimikkeet = List(Keyword(Fi, "insinööri"), Keyword(Fi, "koneinsinööri")),
    yhteystieto = Some(Yhteystieto1))

  val JulkaistuAmmToteutus = Toteutus(
    oid = None,
    koulutusOid = KoulutusOid("1.2.246.562.13.123"),
    tila = Julkaistu,
    tarjoajat = List("1.2.3.3", "1.2.3.4").map(OrganisaatioOid),
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(AmmToteutuksenMetatieto),
    muokkaaja = UserOid("1.2.3.4"),
    organisaatioOid = ChildOid,
    kielivalinta = Seq(Fi, Sv),
    modified = None)

  val JulkaistuYoToteutus = JulkaistuAmmToteutus.copy(metadata = Some(YoToteutuksenMetaTieto))

  val MinToteutus = new Toteutus(
    muokkaaja = UserOid("5.4.3.2"),
    organisaatioOid = OrganisaatioOid("1.2"),
    koulutusOid = KoulutusOid("1.2.246.562.13.123"),
    modified = None)
}
