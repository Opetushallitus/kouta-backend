package fi.oph.kouta

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.keyword.Keyword

object TestData {

  def now() = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
  def inFuture(s:Long = 500) = LocalDateTime.now().plusSeconds(s).truncatedTo(ChronoUnit.MINUTES)

  val Osoite1 = Osoite(
    osoite = Map(Fi -> "Kivatie 1", Sv -> "kivavägen 1"),
    postinumero = Some("12345"),
    postitoimipaikka = Map(Fi -> "Kaupunki", Sv -> "SV kaupunki"))

  val Yhteystieto1 = Yhteystieto(
    nimi = Map(Fi -> "Aku Ankka", Sv -> "Aku Ankka"),
    puhelinnumero = Map(Fi -> "123", Sv -> "123"))

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
    tyyppi = Some("heippa"),
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
    metadata = Some(new KoulutusMetadata()),
    tarjoajat = List("1.2", "2.2", "3.2"),
    muokkaaja = "5.5.5",
    organisaatioOid = "1.2",
    kielivalinta = List(Fi, Sv))

  val MinKoulutus = Koulutus(johtaaTutkintoon = false, muokkaaja = "1.2.3", organisaatioOid = "1.2")

  val JulkaistuHaku = new Haku(
    nimi = Map(Fi -> "Haku fi", Sv -> "Haku sv"),
    tila = Julkaistu,
    hakutapaKoodiUri = Some("hakutapa_03#1"),
    hakukohteenLiittamisenTakaraja = Some(inFuture()),
    hakukohteenMuokkaamisenTakaraja = Some(inFuture()),
    alkamiskausiKoodiUri = Some("kausi_k#1"),
    alkamisvuosi = Some("2019"),
    kohdejoukkoKoodiUri = Some("kohdejoukko_05#11"),
    kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_1#11"),
    hakulomaketyyppi = Some(EiSähköistä),
    hakulomake = Some("Hakulomake tulostetaan ja toimitetaan postitse"),
    metadata = Some(HakuMetadata(Some(Yhteystieto1))),
    hakuajat = List(Ajanjakso(alkaa = now(), paattyy = inFuture())),
    organisaatioOid = "1.2.3.4",
    muokkaaja = "5.4.3.2.1",
    kielivalinta = Seq(Fi, Sv))

  val MinHaku = new Haku(muokkaaja = "9.9.9.9.9", organisaatioOid = "5.5.5")

  val JulkaistuHakukohde = new Hakukohde(
    oid = None,
    toteutusOid = "1.2.246.562.17.123",
    hakuOid = "1.2.246.562.29.123",
    tila = Julkaistu,
    nimi = Map(Fi -> "Haku fi", Sv -> "Haku sv"),
    alkamiskausiKoodiUri = Some("kausi_k#1"),
    alkamisvuosi = Some("2019"),
    hakulomaketyyppi = Some(EiSähköistä),
    hakulomake = Some("Hakulomake tulostetaan ja toimitetaan postitse"),
    aloituspaikat = Some(2),
    ensikertalaisenAloituspaikat = Some(1),
    pohjakoulutusvaatimusKoodiUri = Some("pohjakoulutusvaatimus_01#2"),
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
    muokkaaja = "1.2.3.2.1",
    organisaatioOid = "1.2",
    kielivalinta = Seq(Fi, Sv))

  val MinHakukohde = new Hakukohde(
    muokkaaja = "7.7.7.7",
    organisaatioOid = "1.2",
    toteutusOid = "1.2.246.562.17.123",
    hakuOid = "1.2.246.562.29.123")

  val JulkaistuValintaperuste = new Valintaperuste(
    id = None,
    tila = Julkaistu,
    hakutapaKoodiUri = Some("hakutapa_01#1"),
    kohdejoukkoKoodiUri = Some("kohdejoukko_02#2"),
    kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_1#11"),
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = None,
    organisaatioOid = "1.2.3.4",
    muokkaaja = "2.1.2.1.2",
    kielivalinta = List(Fi, Sv))

  val MinValintaperuste = new Valintaperuste(muokkaaja = "7.7.7.7.7", organisaatioOid = "1.2.1.2")

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
    osaamisalat = List(Osaamisala("osaamisala_koodi_uri#1")),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List(Keyword(Fi, "robotiikka"), Keyword(Fi, "robottiautomatiikka")),
    ammattinimikkeet = List(Keyword(Fi, "insinööri"), Keyword(Fi, "koneinsinööri")),
    yhteystieto = Some(Yhteystieto1))

  val JulkaistuAmmToteutus = Toteutus(
    oid = None,
    koulutusOid = "1.2.246.562.13.123",
    tila = Julkaistu,
    tarjoajat = List("1.2.3.3", "1.2.3.4"),
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(AmmToteutuksenMetatieto),
    muokkaaja = "1.2.3.4",
    organisaatioOid = "1.2",
    kielivalinta = Seq(Fi, Sv))

  val MinToteutus = new Toteutus(
    muokkaaja = "5.4.3.2",
    organisaatioOid = "1.2",
    koulutusOid = "1.2.246.562.13.123")
}
