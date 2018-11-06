package fi.oph.kouta

import java.time.{Instant, LocalTime}
import java.time.temporal.{ChronoUnit, TemporalUnit}

import fi.oph.kouta.domain._
import org.joda.time.Seconds

object TestData {

  def now() = Instant.now().truncatedTo(ChronoUnit.MINUTES)
  def inFuture(s:Long = 500) = Instant.now.plusSeconds(s).truncatedTo(ChronoUnit.MINUTES)

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
    palautusaika = Some(inFuture()),
    toimitustapa = Some(Hakijapalvelu),
    toimitusosoite = None)

  val Liite2 = Liite(
    id = None,
    tyyppi = Some("terve"),
    nimi = Map(Fi -> "liite 2 Fi", Sv -> "liite 2 Sv"),
    kuvaus = Map(Fi -> "kuvaus Fi", Sv -> "kuvaus Sv"),
    palautusaika = None,
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
    kielivalinta = List(Fi, Sv))

  val MinKoulutus = Koulutus(johtaaTutkintoon = false, muokkaaja = "1.2.3")

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
    organisaatio = "1.2.3.4",
    muokkaaja = "5.4.3.2.1",
    kielivalinta = Seq(Fi, Sv))

  val MinHaku = new Haku(muokkaaja = "9.9.9.9.9", organisaatio = "5.5.5")

  val JulkaistuHakukohde = new Hakukohde(
    oid = None,
    koulutusOid = "1.2.246.562.13.123",
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
    liitteidenPalautusaika = Some(inFuture()),
    liitteidenToimitustapa = None,
    liitteidenToimitusosoite = None,
    liitteet = List(Liite1, Liite2),
    valintakokeet = List(Valintakoe1),
    hakuajat = List(Ajanjakso(alkaa = now(), paattyy = inFuture())),
    muokkaaja = "1.2.3.2.1",
    kielivalinta = Seq(Fi, Sv))

  val MinHakukohde = new Hakukohde(muokkaaja = "7.7.7.7", koulutusOid = "1.2.246.562.13.123", hakuOid = "1.2.246.562.29.123")

  val JulkaistuValintaperuste = new Valintaperuste(
    id = None,
    tila = Julkaistu,
    hakutapaKoodiUri = Some("hakutapa_01#1"),
    kohdejoukkoKoodiUri = Some("kohdejoukko_02#2"),
    kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_1#11"),
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = None,
    organisaatio = "1.2.3.4",
    muokkaaja = "2.1.2.1.2",
    kielivalinta = List(Fi, Sv))

  val MinValintaperuste = new Valintaperuste(muokkaaja = "7.7.7.7.7", organisaatio = "1.2.1.2")

  val ToteutuksenOpetus = Opetus(
    opetuskielet = List("fi"),
    lahiopetus = Some(true),
    opetusajat = List("opetusaikakk_1#1", "opetusaikakk_2#1"),
    maksullisuus = None,
    kuvaus = Map())

  val AmmToteutuksenMetatieto = ToteutusMetadata(
    kuvaus = Map(),
    osaamisalat = List(Osaamisala("osaamisala_koodi_uri#1")),
    opetus = Some(ToteutuksenOpetus),
    asiasanat = List("robotiikka"),
    ammattinimikkeet = List("koneinsinööri"),
    yhteystieto = Some(Yhteystieto1))

  val JulkaistuAmmToteutus = Toteutus(
    oid = None,
    koulutusOid = "1.2.246.562.13.123",
    tila = Julkaistu,
    tarjoajat = List("1.2.3.3", "1.2.3.4"),
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(AmmToteutuksenMetatieto),
    muokkaaja = "1.2.3.4",
    kielivalinta = Seq(Fi, Sv))

  val MinToteutus = new Toteutus(muokkaaja = "5.4.3.2", koulutusOid = "1.2.246.562.13.123")
}
