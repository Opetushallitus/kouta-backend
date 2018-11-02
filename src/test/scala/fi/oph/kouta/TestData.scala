package fi.oph.kouta

import java.time.Instant

import fi.oph.kouta.domain._

object TestData {

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
    hakukohteenLiittamisenTakaraja = Some(Instant.now()),
    hakukohteenMuokkaamisenTakaraja = Some(Instant.now()),
    alkamiskausiKoodiUri = Some("kausi_k#1"),
    alkamisvuosi = Some("2019"),
    kohdejoukkoKoodiUri = Some("kohdejoukko_05#11"),
    kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_1#11"),
    hakulomaketyyppi = Some(EiSähköistä),
    hakulomake = Some("Hakulomake tulostetaan ja toimitetaan postitse"),
    metadata = Some(HakuMetadata(yhteystieto = Some(new Yhteystieto(nimi = "Iines Ankka")))),
    hakuajat = List(Hakuaika(alkaa = Instant.now(), paattyy = Instant.now.plusSeconds(9000))),
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
    metadata = None,
    hakuajat = List(Hakuaika(alkaa = Instant.now(), paattyy = Instant.now.plusSeconds(9000))),
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
    yhteystieto = Some(Yhteystieto(nimi = "Aku Ankka", puhelinnumero = Some("123"))))

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
