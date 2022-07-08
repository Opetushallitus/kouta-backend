package fi.oph.kouta.validation

import java.util.UUID

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.validation.Validations._

class HakukohdeValidationSpec extends BaseValidationSpec[Hakukohde] {

  val max: Hakukohde = JulkaistuHakukohde
  val min: Hakukohde = MinHakukohde

  it should "fail if perustiedot is invalid" in {
    failsValidation(max.copy(oid = Some(HakukohdeOid("1.2.3"))), "oid", validationMsg("1.2.3"))
    failsValidation(min.copy(kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(min.copy(nimi = Map(Fi -> "nimi")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(max.copy(nimi = Map(Fi -> "nimi", Sv -> "")), "nimi", invalidKielistetty(Seq(Sv)))
  }

  it should "pass incomplete hakukohde if not julkaistu" in {
    passesValidation(min)
  }

  it should "fail if haku oid is invalid" in {
    failsValidation(min.copy(hakuOid = HakuOid("2.3.4")), "hakuOid", validationMsg("2.3.4"))
  }

  it should "fail if toteutus oid is invalid" in {
    failsValidation(min.copy(toteutusOid = ToteutusOid("3.4.5")), "toteutusOid", validationMsg("3.4.5"))
  }

  it should "fail if organisaatio oid is invalid" in {
    failsValidation(min.copy(organisaatioOid = OrganisaatioOid("1.2.3")), "organisaatioOid", validationMsg("1.2.3"))
  }

  it should "fail if organisaatio oid is empty" in {
    failsValidation(min.copy(organisaatioOid = OrganisaatioOid("")), "organisaatioOid", validationMsg(""))
  }

  it should "fail if tallennettu hakukohde has invalid information" in {
    val invalidHakuajat = TestData.getInvalidHakuajat
    failsValidation(min.copy(hakuajat = invalidHakuajat), "hakuajat[0]", invalidAjanjaksoMsg(invalidHakuajat.head))

    failsValidation(min.copy(pohjakoulutusvaatimusKoodiUrit = Seq("tintti", "huuhkaja")),
      ValidationError("pohjakoulutusvaatimusKoodiUrit[0]", validationMsg("tintti")),
      ValidationError("pohjakoulutusvaatimusKoodiUrit[1]", validationMsg("huuhkaja")))
  }

  it should "fail if julkaistu hakukohde is invalid" in {
    passesValidation(max.copy(tila = Julkaistu, liitteetOnkoSamaToimitusaika = Some(false), liitteidenToimitusaika = None))
    passesValidation(max.copy(tila = Tallennettu, liitteetOnkoSamaToimitusaika = Some(true), liitteidenToimitusaika = None))
    failsValidation(max.copy(liitteetOnkoSamaToimitusaika = Some(true), liitteidenToimitusaika = None), "liitteidenToimitusaika", missingMsg)

    passesValidation(max.copy(tila = Julkaistu, liitteetOnkoSamaToimitusosoite = Some(false), liitteidenToimitusosoite = None))
    passesValidation(max.copy(tila = Tallennettu, liitteetOnkoSamaToimitusosoite = Some(true), liitteidenToimitusosoite = None))
    failsValidation(max.copy(liitteetOnkoSamaToimitusosoite = Some(true), liitteidenToimitustapa = None), "liitteidenToimitustapa", missingMsg)
    failsValidation(max.copy(liitteetOnkoSamaToimitusosoite = Some(true), liitteidenToimitustapa = Some(MuuOsoite), liitteidenToimitusosoite = None), "liitteidenToimitusosoite", missingMsg)

    passesValidation(max.copy(tila = Tallennettu, pohjakoulutusvaatimusKoodiUrit = Seq()))
    failsValidation(max.copy(pohjakoulutusvaatimusKoodiUrit = Seq()), "pohjakoulutusvaatimusKoodiUrit", missingMsg)

    passesValidation(max.copy(tila = Tallennettu, pohjakoulutusvaatimusTarkenne = Map(Fi -> "tarkenne")))
    failsValidation(max.copy(pohjakoulutusvaatimusTarkenne = Map(Fi -> "tarkenne")), "pohjakoulutusvaatimusTarkenne", invalidKielistetty(Seq(Sv)))

    passesValidation(max.copy(tila = Tallennettu, muuPohjakoulutusvaatimus = Map(Fi -> "tarkenne")))
    failsValidation(max.copy(muuPohjakoulutusvaatimus = Map(Fi -> "tarkenne")), "muuPohjakoulutusvaatimus", invalidKielistetty(Seq(Sv)))

    passesValidation(max.copy(tila = Julkaistu, kaytetaanHaunAikataulua = Some(true), hakuajat = List()))
    passesValidation(max.copy(tila = Tallennettu, kaytetaanHaunAikataulua = Some(false), hakuajat = List()))
    failsValidation(max.copy(kaytetaanHaunAikataulua = Some(false), hakuajat = List()), "hakuajat", missingMsg)
    failsValidation(max.copy(kaytetaanHaunAikataulua = None), "kaytetaanHaunAikataulua", missingMsg)
  }

  it should "validate hakulomake information of a julkaistu hakukohde" in {
    val hakulomakeLinkki: Kielistetty = Map(Fi -> "http://lomake.fi/fi", Sv -> "http://lomake.fi/sv")
    passesValidation(max.copy(hakulomaketyyppi = Some(MuuHakulomake), hakulomakeLinkki = hakulomakeLinkki))
    failsValidation(max.copy(hakulomaketyyppi = Some(MuuHakulomake), hakulomakeLinkki = hakulomakeLinkki - Fi), "hakulomakeLinkki", invalidKielistetty(Seq(Fi)))
    failsValidation(max.copy(hakulomaketyyppi = Some(MuuHakulomake), hakulomakeLinkki = hakulomakeLinkki + (Fi -> "linkki")), "hakulomakeLinkki", invalidUrl("linkki"))

    passesValidation(max.copy(hakulomaketyyppi = Some(Ataru), hakulomakeAtaruId = Some(UUID.randomUUID)))
    failsValidation(max.copy(hakulomaketyyppi = Some(Ataru), hakulomakeAtaruId = None), "hakulomakeAtaruId", missingMsg)

    passesValidation(max.copy(hakulomaketyyppi = Some(EiSähköistä), hakulomakeKuvaus = Map(Fi -> "kuvaus", Sv -> "kuvaus sv")))
    failsValidation(max.copy(hakulomaketyyppi = Some(EiSähköistä), hakulomakeKuvaus = Map(Fi -> "kuvaus", Sv -> "")), "hakulomakeKuvaus", invalidKielistetty(Seq(Sv)))

    failsValidation(max.copy(kaytetaanHaunHakulomaketta = Some(false), hakulomaketyyppi = None), "hakulomaketyyppi", missingMsg)
    failsValidation(max.copy(kaytetaanHaunHakulomaketta = None), "kaytetaanHaunHakulomaketta", missingMsg)

    passesValidation(max.copy(kaytetaanHaunHakulomaketta = Some(true), hakulomaketyyppi = None))
  }

  it should "validate liitteet" in {
    failsValidation(min.copy(liitteet = List(Liite1.copy(tyyppiKoodiUri = Some("2.3.4")))), "liitteet[0].tyyppiKoodiUri", validationMsg("2.3.4"))
  }

  it should "validate valintakokeet" in {
    failsValidation(min.copy(valintakokeet = List(Valintakoe1.copy(tyyppiKoodiUri = Some("koodi")))), "valintakokeet[0].tyyppiKoodiUri", validationMsg("koodi"))
  }

  it should "pass valid julkaistu hakukohde" in {
    passesValidation(max)
  }

  it should "return multiple error messages" in {
    failsValidation(max.copy(pohjakoulutusvaatimusKoodiUrit = Seq("vaara uri"), liitteetOnkoSamaToimitusaika = Some(true), liitteidenToimitusaika = None),
      ValidationError("pohjakoulutusvaatimusKoodiUrit[0]", validationMsg("vaara uri")),
      ValidationError("liitteidenToimitusaika", missingMsg))
  }

  "Hakukohde on julkaisu validation" should "pass a valid hakukohde" in {
    passesOnJulkaisuValidation(max)
  }

  it should "fail if liitteidenToimitusaika is in the past" in {
    val past = inPast()
    passesValidation(max.copy(tila = Julkaistu, liitteidenToimitusaika = Some(past)))
    failsOnJulkaisuValidation(max.copy(liitteidenToimitusaika = Some(past)), "liitteidenToimitusaika", pastDateMsg(past))
  }

  it should "fail if hakuajat are in the past" in {
    val pastHakuaika = Ajanjakso(inPast(2000), Some(inPast(1000)))
    passesValidation(max.copy(tila = Julkaistu, hakuajat = List(pastHakuaika)))
    failsOnJulkaisuValidation(max.copy(hakuajat = List(pastHakuaika)), "hakuajat[0].paattyy", pastDateMsg(pastHakuaika.paattyy.get))
  }

  it should "validate liitteet" in {
    val past = inPast(9001)
    failsOnJulkaisuValidation(max.copy(liitteet = List(Liite1.copy(toimitusaika = Some(past)))), "liitteet[0].toimitusaika", pastDateMsg(past))
  }

  it should "validate valintakokeet" in {
    val ajanjakso = Ajanjakso(alkaa = inPast(4000), paattyy = Some(inPast(2000)))
    val tilaisuus = Valintakoe1.tilaisuudet.head.copy(aika = Some(ajanjakso))
    val hakukohde = max.copy(valintakokeet = List(Valintakoe1.copy(tilaisuudet = List(tilaisuus))))
    passesValidation(hakukohde)
    failsOnJulkaisuValidation(hakukohde, "valintakokeet[0].tilaisuudet[0].aika.paattyy", pastDateMsg(ajanjakso.paattyy.get))
  }
}

/*
class HakukohdeMetadaValidationSpec extends SubEntityValidationSpec[HakukohdeMetadata]{

  "Hakukohde metadata validation" should "validate koulutuksenAlkamiskausi" in {
    val metadataWithInvalidAlkamisvuosi = HakukohdeMetadata(
      kaytetaanHaunAlkamiskautta = Some(false),
      koulutuksenAlkamiskausi =
        Some(KoulutuksenAlkamiskausi(
          koulutuksenAlkamisvuosi = Some("200007"),
          koulutuksenAlkamiskausiKoodiUri = Some("kausi_k#1"))),
          aloituspaikat = Some(Aloituspaikat(None, None)),
      isMuokkaajaOphVirkailija = None)

    failsValidation(Tallennettu, metadataWithInvalidAlkamisvuosi, "koulutuksenAlkamiskausi.koulutuksenAlkamisvuosi", validationMsg("200007"))
  }


  it should "validate valintaperusteenValintakokeidenLisatilaisuudet" in {
    val ajanjakso = Ajanjakso(alkaa = inPast(4000), paattyy = Some(inPast(2000)))
    val tilaisuus = ValintakokeenLisatilaisuudet1.tilaisuudet.head.copy(aika = Some(ajanjakso))
    val metadataWithPastTilaisuus = HakukohdeMetadata(
      kaytetaanHaunAlkamiskautta = Some(false),
      koulutuksenAlkamiskausi =
        Some(KoulutuksenAlkamiskausi(
          alkamiskausityyppi = Some(AlkamiskausiJaVuosi),
          koulutuksenAlkamisvuosi = Some(now().getYear.toString()),
          koulutuksenAlkamiskausiKoodiUri = Some("kausi_k#1"))),
          aloituspaikat = Some(Aloituspaikat(Some(0), None)),
      valintaperusteenValintakokeidenLisatilaisuudet = List(ValintakokeenLisatilaisuudet(id = None, tilaisuudet = List(tilaisuus))),
      isMuokkaajaOphVirkailija = None
    )

    failsOnJulkaisuValidation(metadataWithPastTilaisuus, "valintaperusteenValintakokeidenLisatilaisuudet[0].tilaisuudet[0].aika.paattyy", pastDateMsg(ajanjakso.paattyy.get))
  }

  it should "validate koulutuksenAlkamiskausi of a julkaistu hakukohde" in {
    val metadataWithoutAlkamiskausityyppi = HakukohdeMetadata(
      kaytetaanHaunAlkamiskautta = Some(false),
      koulutuksenAlkamiskausi =
        Some(KoulutuksenAlkamiskausi(
          alkamiskausityyppi = None,
          koulutuksenAlkamisvuosi = Some("2007"),
          koulutuksenAlkamiskausiKoodiUri = Some("kausi_k#1"))),
          aloituspaikat = Some(Aloituspaikat(Some(0), None)),
      isMuokkaajaOphVirkailija = Some(false))

    failsValidation(Julkaistu, metadataWithoutAlkamiskausityyppi, "koulutuksenAlkamiskausi.alkamiskausityyppi", missingMsg)
  }

  it should "validate koulutuksenAlkamiskausi on julkaisu of hakukohde" in {
    val metadataWithAlkamisvuosiInThePast = HakukohdeMetadata(
      kaytetaanHaunAlkamiskautta = Some(false),
      koulutuksenAlkamiskausi =
        Some(KoulutuksenAlkamiskausi(
          koulutuksenAlkamisvuosi = Some("2007"),
          koulutuksenAlkamiskausiKoodiUri = Some("kausi_k#1"))),
          aloituspaikat = None,
      isMuokkaajaOphVirkailija = Some(false))

    failsOnJulkaisuValidation(metadataWithAlkamisvuosiInThePast, "koulutuksenAlkamiskausi.koulutuksenAlkamisvuosi", pastDateMsg("2007"))
  }

  it should "validate kaytetaanHaunAlkamiskautta is mandatory" in {
    val metadataWithoutKaytetaanHaunAlkamiskauttaFlag = HakukohdeMetadata(
      koulutuksenAlkamiskausi =
        Some(KoulutuksenAlkamiskausi(
          koulutuksenAlkamisvuosi = Some("2007"),
          koulutuksenAlkamiskausiKoodiUri = Some("kausi_k#1"))),
          aloituspaikat = None,
      isMuokkaajaOphVirkailija = Some(false))

    failsValidation(Tallennettu, metadataWithoutKaytetaanHaunAlkamiskauttaFlag, "kaytetaanHaunAlkamiskautta", missingMsg)
  }

  it should "validate koulutuksenAlkamiskausi is given if not using haun alkamiskausi" in {
    val metadataWithoutKaytetaanHaunAlkamiskauttaFlag = HakukohdeMetadata(
      kaytetaanHaunAlkamiskautta = Some(false),
      koulutuksenAlkamiskausi = None,
      aloituspaikat = None,
      isMuokkaajaOphVirkailija = Some(false))

    failsValidation(Tallennettu, metadataWithoutKaytetaanHaunAlkamiskauttaFlag, "koulutuksenAlkamiskausi", missingMsg)
  }

  it should "validate aloituspaikat is mandatory when Julkaistu" in {
    val invalidAloituspaikatMetadata = HakukohdeMetadata(
      kaytetaanHaunAlkamiskautta = Some(true),
      koulutuksenAlkamiskausi = None,
      aloituspaikat = None,
      isMuokkaajaOphVirkailija = Some(false))

    passesValidation(Tallennettu, invalidAloituspaikatMetadata)
    failsValidation(
      Julkaistu,
      invalidAloituspaikatMetadata,
      "aloituspaikat", missingMsg);
  }

  it should "validate aloituspaikat" in {
    val invalidAloituspaikatMetadata = HakukohdeMetadata(
      kaytetaanHaunAlkamiskautta = Some(true),
      koulutuksenAlkamiskausi = None,
      aloituspaikat = Some(Aloituspaikat(lukumaara = Some(-10), ensikertalaisille = Some(-5), kuvaus = Map(Fi -> "kuvaus", Sv -> ""))),
      isMuokkaajaOphVirkailija = Some(false))

    failsValidation(
      Julkaistu,
      invalidAloituspaikatMetadata,
      ("aloituspaikat.lukumaara", notNegativeMsg),
      ("aloituspaikat.ensikertalaisille", notNegativeMsg),
      ("aloituspaikat.kuvaus", invalidKielistetty(Seq(Sv))))
  }

  it should "validate hakukohteenLinja" in {
    val metadataWithoutAllTranslations = HakukohdeMetadata(
      kaytetaanHaunAlkamiskautta = Some(true),
      koulutuksenAlkamiskausi = None,
      aloituspaikat = Some(Aloituspaikat(Some(0), None)),
      hakukohteenLinja = Some(HakukohteenLinja(linja = None, alinHyvaksyttyKeskiarvo = Some(7.5), lisatietoa = Map(Fi -> "lisatietoa", Sv -> ""))),
      isMuokkaajaOphVirkailija = Some(false))

    passesValidation(Tallennettu, metadataWithoutAllTranslations)
    failsValidation(Julkaistu, metadataWithoutAllTranslations, "hakukohteenLinja.lisatietoa", invalidKielistetty(Seq(Sv)))
  }

  it should "fail validation for painotetutArvosanat when oppiaine koodiUri is invalid" in {
    val hakukohdeMetadata = HakukohdeMetadata(
      kaytetaanHaunAlkamiskautta = Some(true),
      koulutuksenAlkamiskausi = None,
      aloituspaikat = Some(Aloituspaikat(Some(0), None)),
      hakukohteenLinja = Some(HakukohteenLinja(
        linja = None,
        alinHyvaksyttyKeskiarvo = Some(7.5),
        lisatietoa = Map(Fi -> "lisatietoa", Sv -> "lisatietoa sv"),
        painotetutArvosanat = Seq(
          PainotettuOppiaine(koodiUrit = Some(OppiaineKoodiUrit(oppiaine = Some("painotettavatoppiaineetlukiossa"), kieli = None)), painokerroin = Some(1.5))))),
      isMuokkaajaOphVirkailija = Some(false))

    failsValidation(Julkaistu, hakukohdeMetadata, "hakukohteenLinja.painotetutArvosanat[0].koodiUrit.oppiaine", validationMsg("painotettavatoppiaineetlukiossa"))
  }

  it should "fail validation for painotetutArvosanat when oppiaine koodiUri is missing" in {
    val hakukohdeMetadata = HakukohdeMetadata(
      kaytetaanHaunAlkamiskautta = Some(true),
      koulutuksenAlkamiskausi = None,
      aloituspaikat = Some(Aloituspaikat(Some(0), None)),
      hakukohteenLinja = Some(HakukohteenLinja(
        linja = None,
        alinHyvaksyttyKeskiarvo = Some(7.5),
        lisatietoa = Map(Fi -> "lisatietoa", Sv -> "lisatietoa sv"),
        painotetutArvosanat = Seq(
          PainotettuOppiaine(koodiUrit = Some(OppiaineKoodiUrit(oppiaine = None, kieli = None)), painokerroin = Some(1.5))))),
      isMuokkaajaOphVirkailija = Some(false))

    failsValidation(Julkaistu, hakukohdeMetadata, "hakukohteenLinja.painotetutArvosanat[0].koodiUrit.oppiaine", missingMsg)
  }

  it should "fail validation for painotetutArvosanat when koodiUrit is missing" in {
    val hakukohdeMetadata = HakukohdeMetadata(
      kaytetaanHaunAlkamiskautta = Some(true),
      koulutuksenAlkamiskausi = None,
      aloituspaikat = Some(Aloituspaikat(Some(0), None)),
      hakukohteenLinja = Some(HakukohteenLinja(
        linja = None,
        alinHyvaksyttyKeskiarvo = Some(7.5),
        lisatietoa = Map(Fi -> "lisatietoa", Sv -> "lisatietoa sv"),
        painotetutArvosanat = Seq(
          PainotettuOppiaine(koodiUrit = None, painokerroin = Some(1.5))))),
      isMuokkaajaOphVirkailija = Some(false))

    failsValidation(Julkaistu, hakukohdeMetadata, "hakukohteenLinja.painotetutArvosanat[0].koodiUrit", missingMsg)
  }

  it should "fail validation for painotetutArvosanat when painokerroin is missing" in {
    val hakukohdeMetadata = HakukohdeMetadata(
      kaytetaanHaunAlkamiskautta = Some(true),
      koulutuksenAlkamiskausi = None,
      aloituspaikat = Some(Aloituspaikat(Some(0), None)),
      hakukohteenLinja = Some(HakukohteenLinja(
        linja = None,
        alinHyvaksyttyKeskiarvo = Some(7.5),
        lisatietoa = Map(Fi -> "lisatietoa", Sv -> "lisatietoa sv"),
        painotetutArvosanat = Seq(
          PainotettuOppiaine(koodiUrit = Some(OppiaineKoodiUrit(oppiaine = Some("painotettavatoppiaineetlukiossa_a1"), kieli = Some("kieli_en#1"))), painokerroin = None)))),
      isMuokkaajaOphVirkailija = Some(false))

    failsValidation(Julkaistu, hakukohdeMetadata, "hakukohteenLinja.painotetutArvosanat[0].painokerroin", missingMsg)
  }

  it should "fail validation for painotetutArvosanat when painokerroin is negative" in {
    val hakukohdeMetadata = HakukohdeMetadata(
      kaytetaanHaunAlkamiskautta = Some(true),
      koulutuksenAlkamiskausi = None,
      aloituspaikat = Some(Aloituspaikat(Some(0), None)),
      hakukohteenLinja = Some(HakukohteenLinja(
        linja = None,
        alinHyvaksyttyKeskiarvo = Some(7.5),
        lisatietoa = Map(Fi -> "lisatietoa", Sv -> "lisatietoa sv"),
        painotetutArvosanat = Seq(
          PainotettuOppiaine(koodiUrit = Some(OppiaineKoodiUrit(oppiaine = Some("painotettavatoppiaineetlukiossa_ai"), kieli = None)), painokerroin = Some(-1.5))))),
      isMuokkaajaOphVirkailija = Some(false))

    failsValidation(Julkaistu, hakukohdeMetadata, "hakukohteenLinja.painotetutArvosanat[0].painokerroin", notNegativeMsg)
  }

  it should "pass validation for hakukohdemetadata with painotetutArvosanat" in {
     val hakukohdeMetadata = HakukohdeMetadata(
       kaytetaanHaunAlkamiskautta = Some(true),
       koulutuksenAlkamiskausi = None,
       aloituspaikat = Some(Aloituspaikat(Some(0), None)),
       hakukohteenLinja = Some(HakukohteenLinja(
         linja = None,
         alinHyvaksyttyKeskiarvo = Some(7.5),
         lisatietoa = Map(Fi -> "lisatietoa", Sv -> "lisatietoa sv"),
         painotetutArvosanat = Seq(
           PainotettuOppiaine(koodiUrit = Some(OppiaineKoodiUrit(oppiaine = Some("painotettavatoppiaineetlukiossa_a1en"), kieli = None)), painokerroin = Some(1.5)),
           PainotettuOppiaine(koodiUrit = Some(OppiaineKoodiUrit(oppiaine = Some("painotettavatoppiaineetlukiossa_mu"), kieli = None)), painokerroin = Some(1.7))))),
       isMuokkaajaOphVirkailija = Some(false))

     passesValidation(Julkaistu, hakukohdeMetadata)
  }
}

class LiiteValidationSpec extends SubEntityValidationSpec[Liite] {

  "Liite validation" should "pass with a valid liite" in {
    passesValidation(Julkaistu, Liite1)
  }

  it should "validate LiitteenToimitusosoite" in {
    val liite = Liite1.copy(toimitusosoite = Some(LiitteenToimitusosoite(osoite = Osoite1, sahkoposti = Some("invalid"))))
    failsValidation(Tallennettu, liite, "toimitusosoite.sahkoposti", invalidEmail("invalid"))
  }

  it should "fail if tyyppiKoodiUri is invalid" in {
    failsValidation(Tallennettu, Liite1.copy(tyyppiKoodiUri = Some("invalid")), "tyyppiKoodiUri", validationMsg("invalid"))
  }

  it should "fail if nimi is missing languages when julkaistu" in {
    passesValidation(Tallennettu, Liite1.copy(nimi = Map(Fi -> "nimi")))
    failsValidation(Julkaistu, Liite1.copy(nimi = Map(Fi -> "nimi")), "nimi", invalidKielistetty(Seq(Sv)))
  }


  it should "fail if kuvaus is missing languages when julkaistu" in {
    passesValidation(Tallennettu, Liite1.copy(kuvaus = Map(Fi -> "kuvaus")))
    failsValidation(Julkaistu, Liite1.copy(kuvaus = Map(Fi -> "kuvaus")), "kuvaus", invalidKielistetty(Seq(Sv)))
  }

  it should "fail if toimitustapa is MuuOsoite, but no toimitusosoite is specified when julkaistu" in {
    val liite = Liite1.copy(toimitustapa = Some(MuuOsoite), toimitusosoite = None)

    passesValidation(Tallennettu, liite)
    passesValidation(Julkaistu, liite.copy(toimitustapa = Some(Lomake)))
    failsValidation(Julkaistu, liite, "toimitusosoite", missingMsg)
  }

  "Liite on julkaisu validation" should "pass with a valid liite" in {
    passesOnJulkaisuValidation(Liite1)
  }

  it should "fail if toimitusaika is in the past" in {
    val past = inPast(9001)
    failsOnJulkaisuValidation(Liite1.copy(toimitusaika = Some(past)), "toimitusaika", pastDateMsg(past))
  }

}

class LiitteenToimitusosoiteValidationSpec extends SubEntityValidationSpec[LiitteenToimitusosoite] {
  val toimitusOsoite: LiitteenToimitusosoite = LiitteenToimitusosoite(osoite = Osoite1, sahkoposti = Some("foo@bar.fi"))

  "LiitteenToimitusosoite validation" should "pass a valid LiitteenToimitusosoite" in {
    passesValidation(Julkaistu, toimitusOsoite)
  }

  it should "fail if sähköposti is invalid" in {
    failsValidation(Tallennettu, toimitusOsoite.copy(sahkoposti = Some("foo@bar")), "sahkoposti", invalidEmail("foo@bar"))
  }

  it should "validate osoite" in {
    failsValidation(Tallennettu, toimitusOsoite.copy(osoite = Osoite1.copy(postinumeroKoodiUri = Some("invalid"))), "osoite.postinumeroKoodiUri", validationMsg("invalid"))
  }
}

 */
