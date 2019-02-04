package fi.oph.kouta

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.oid._

object TestData {

  def now() = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
  def inFuture(s:Long = 500) = LocalDateTime.now().plusSeconds(s).truncatedTo(ChronoUnit.MINUTES)

  val Osoite1 = Osoite(
    osoite = Map(Fi -> "Kivatie 1", Sv -> "kivavägen 1"),
    postinumero = Some("12345"),
    postitoimipaikka = Map(Fi -> "Kaupunki", Sv -> "SV kaupunki"))

  val Yhteystieto1 = Yhteystieto(
    nimi = Map(Fi -> "Aku Ankka", Sv -> "Aku Ankka"),
    puhelinnumero = Map(Fi -> "123", Sv -> "123"),
    sahkoposti = Map(Fi -> "aku.ankka@ankkalinnankoulu.fi", Sv -> "aku.ankka@ankkalinnankoulu.fi"),
    titteli = Map(Fi -> "titteli", Sv -> "titteli sv"))

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
    koulutusKoodiUri = Some("koulutus_123456#1"),
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(new KoulutusMetadata(Map(Fi -> "kuvaus", Sv -> "kuvaus sv"))),
    tarjoajat = List("1.2", "2.2", "3.2").map(OrganisaatioOid),
    muokkaaja = UserOid("5.5.5"),
    organisaatioOid = OrganisaatioOid("1.2"),
    kielivalinta = List(Fi, Sv))

  val MinKoulutus = Koulutus(johtaaTutkintoon = false, muokkaaja = UserOid("1.2.3"), organisaatioOid = OrganisaatioOid("1.2"))

  val JulkaistuHaku = new Haku(
    nimi = Map(Fi -> "Haku fi", Sv -> "Haku sv"),
    tila = Julkaistu,
    hakutapaKoodiUri = Some("hakutapa_03#1"),
    hakukohteenLiittamisenTakaraja = Some(inFuture()),
    hakukohteenMuokkaamisenTakaraja = Some(inFuture()),
    alkamiskausiKoodiUri = Some("kausi_k#1"),
    alkamisvuosi = Some("2019"),
    kohdejoukkoKoodiUri = Some("kohdejoukko_02#2"),
    kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_1#11"),
    hakulomaketyyppi = Some(EiSähköistä),
    hakulomake = Some("Hakulomake tulostetaan ja toimitetaan postitse"),
    metadata = Some(HakuMetadata(Some(Yhteystieto1))),
    hakuajat = List(Ajanjakso(alkaa = now(), paattyy = inFuture())),
    organisaatioOid = OrganisaatioOid("1.2.3.4"),
    muokkaaja = UserOid("5.4.3.2.1"),
    kielivalinta = Seq(Fi, Sv))

  val MinHaku = new Haku(muokkaaja = UserOid("9.9.9.9.9"), organisaatioOid = OrganisaatioOid("5.5.5"))

  val JulkaistuHakukohde = new Hakukohde(
    oid = None,
    toteutusOid = ToteutusOid("1.2.246.562.17.123"),
    hakuOid = HakuOid("1.2.246.562.29.123"),
    tila = Julkaistu,
    nimi = Map(Fi -> "Hakukohde fi", Sv -> "Hakukohde sv"),
    alkamiskausiKoodiUri = Some("kausi_k#1"),
    alkamisvuosi = Some("2019"),
    hakulomaketyyppi = Some(EiSähköistä),
    hakulomake = Some("Hakulomake tulostetaan ja toimitetaan postitse"),
    aloituspaikat = Some(2),
    ensikertalaisenAloituspaikat = Some(1),
    pohjakoulutusvaatimusKoodiUri = Some("pohjakoulutusvaatimustoinenaste_01#2"),
    muuPohjakoulutusvaatimus = Map(),
    toinenAsteOnkoKaksoistutkinto = None,
    kaytetaanHaunAikataulua = Some(false),
    valintaperuste = None,
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
    kielivalinta = Seq(Fi, Sv))

  val MinHakukohde = new Hakukohde(
    muokkaaja = UserOid("7.7.7.7"),
    organisaatioOid = OrganisaatioOid("1.2"),
    toteutusOid = ToteutusOid("1.2.246.562.17.123"),
    hakuOid = HakuOid("1.2.246.562.29.123"))

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

  val Valintatapa1 = new Valintatapa(valintatapaKoodiUri = Some("valintatapajono_av#1"),
                                     kuvaus = Map(Fi -> "kuvaus fi", Sv -> "kuvaus sv"),
                                     taulukot = Seq(Taulukko1, Taulukko2),
                                     kaytaMuuntotaulukkoa = false,
                                     kynnysehto = Map(Fi -> "kynnysehto fi", Sv -> "kynnysehto sv"),
                                     enimmaispisteet = Some(201.15),
                                     vahimmaispisteet = Some(182.1))

  val Valintatapa2 = new Valintatapa(valintatapaKoodiUri = Some("valintatapajono_tv#1"),
                                     kuvaus = Map(Fi -> "kuvaus 2 fi", Sv -> "kuvaus 2 sv"),
                                     taulukot = Seq(Taulukko2),
                                     kaytaMuuntotaulukkoa = true,
                                     kynnysehto = Map(Fi -> "kynnysehto fi", Sv -> "kynnysehto sv"),
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

  val valintaperusteMetadata = new ValintaperusteMetadata(
    valintatavat = Seq(Valintatapa1, Valintatapa2),
    kielitaitovaatimukset = Seq(Kielitaitovaatimus1))

  val JulkaistuValintaperuste = new Valintaperuste(
    id = None,
    tila = Julkaistu,
    hakutapaKoodiUri = Some("hakutapa_01#1"),
    kohdejoukkoKoodiUri = Some("kohdejoukko_02#2"),
    kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_1#11"),
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(valintaperusteMetadata),
    organisaatioOid = OrganisaatioOid("1.2.3.4"),
    muokkaaja = UserOid("2.1.2.1.2"),
    kielivalinta = List(Fi, Sv))

  val MinValintaperuste = new Valintaperuste(muokkaaja = UserOid("7.7.7.7.7"), organisaatioOid = OrganisaatioOid("1.2.1.2"))

  val ToteutuksenOpetus = Opetus(
    opetuskielet = List("kieli_fi#1"),
    opetusaikaKoodiUri = Some("opetusaikakk_1#1"),
    opetustapaKoodiUri = Some("opetuspaikkakk_1#1"),
    onkoMaksullinen = Some(true),
    maksunMaara = Map(Fi -> "200,50 euroa", Sv -> "200,50 euro"),
    kuvaus = Map(Fi -> "Kuvaus fi", Sv -> "Kuvaus sv"),
    osiot = List(
      Osio(otsikko = Map(Fi -> "Opintojen rakenne", Sv -> "Rakenne sv"),
           teksti = Map(Fi -> "Opintojen rakenteen kuvaus", Sv -> "Rakenne kuvaus sv")),
      Osio(otsikko = Map(Fi -> "Sisältö", Sv -> "Sisältö sv"),
           teksti = Map(Fi -> "Sisältö kuvaus", Sv -> "Sisältö kuvaus sv"))))

  val AmmToteutuksenMetatieto = ToteutusMetadata(
    kuvaus = Map(),
    osaamisalat = List(Osaamisala("osaamisala_koodi_uri#1",
      linkki = Map(Fi -> "http://osaamisala/linkki/fi", Sv -> "http://osaamisala/linkki/sv"),
      otsikko = Map(Fi -> "Katso osaamisalan tarkempi kuvaus tästä", Sv -> "Katso osaamisalan tarkempi kuvaus tästä sv"))),
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
    organisaatioOid = OrganisaatioOid("1.2"),
    kielivalinta = Seq(Fi, Sv))

  val MinToteutus = new Toteutus(
    muokkaaja = UserOid("5.4.3.2"),
    organisaatioOid = OrganisaatioOid("1.2"),
    koulutusOid = KoulutusOid("1.2.246.562.13.123"))
}