package fi.oph.kouta.validation

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.validation.Validations._

import java.time.LocalDate
import java.util.UUID

class HakuValidationSpec extends BaseValidationSpec[Haku] {

  val max: Haku = JulkaistuHaku
  val min: Haku = MinHaku
  val pastAjanjakso: Ajanjakso = Ajanjakso(alkaa = TestData.inPast(2000), paattyy = Some(TestData.inPast(100)))
  val onlyAlkaaAjanjakso: Ajanjakso = Ajanjakso(alkaa = TestData.inPast(2000), paattyy = None)

  it should "fail if perustiedot is invalid" in {
    failsValidation(max.copy(oid = Some(HakuOid("moikka"))), "oid", validationMsg("moikka"))
    failsValidation(min.copy(kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(min.copy(nimi = Map()), "nimi", invalidKielistetty(Seq(Fi, Sv)))
    failsValidation(max.copy(nimi = Map(Fi -> "nimi")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(max.copy(nimi = Map(Fi -> "nimi", Sv -> "")), "nimi", invalidKielistetty(Seq(Sv)))
  }

  it should "pass incomplete haku if not julkaistu" in {
    passesValidation(min)
  }

  it should "fail if haku oid is invalid" in {
    failsValidation(min.copy(oid = Some(HakuOid("1.2.3"))), "oid", validationMsg("1.2.3"))
  }

  it should "fail if organisaatio oid is invalid" in {
    failsValidation(min.copy(organisaatioOid = OrganisaatioOid("1.2.3")), "organisaatioOid", validationMsg("1.2.3"))
  }

  it should "fail if organisaatio oid is empty" in {
    failsValidation(min.copy(organisaatioOid = OrganisaatioOid("")), "organisaatioOid", validationMsg(""))
  }

  it should "fail if julkaistu haku is invalid" in {
    failsValidation(max.copy(hakutapaKoodiUri = None), "hakutapaKoodiUri", missingMsg)
    failsValidation(max.copy(hakutapaKoodiUri = Some("korppi")), "hakutapaKoodiUri", validationMsg("korppi"))

    failsValidation(max.copy(kohdejoukkoKoodiUri = None), "kohdejoukkoKoodiUri", missingMsg)
    failsValidation(max.copy(kohdejoukkoKoodiUri = Some("kerttu")), "kohdejoukkoKoodiUri", validationMsg("kerttu"))
    failsValidation(max.copy(kohdejoukonTarkenneKoodiUri = Some("tonttu")), "kohdejoukonTarkenneKoodiUri", validationMsg("tonttu"))
    failsValidation(max.copy(hakulomaketyyppi = None, hakulomakeLinkki = Map()), "hakulomaketyyppi", missingMsg)

    val invalidAjanjakso = Ajanjakso(alkaa = TestData.inFuture(90000), paattyy = Some(TestData.inFuture(9000)))
    failsValidation(max.copy(hakuajat = List(invalidAjanjakso)), "hakuajat[0]", invalidAjanjaksoMsg(invalidAjanjakso))
  }

  it should "fail if metadata is invalid" in {
    val ajanjakso = Ajanjakso(alkaa = TestData.inFuture(90000), paattyy = Some(TestData.inFuture(9000)))
    val metadata = max.metadata.get.copy(tulevaisuudenAikataulu = List(ajanjakso))
    failsValidation(max.copy(metadata = Some(metadata)), "metadata.tulevaisuudenAikataulu[0]", invalidAjanjaksoMsg(ajanjakso))
  }

  it should "fail if yhteyshenkilot has other info, but no name" in {
    val metadata = max.metadata.get.copy(yhteyshenkilot = Seq(Yhteyshenkilo(sahkoposti = Map(Fi -> "sahkoposti", Sv -> "email"))))
    failsValidation(max.copy(metadata = Some(metadata)), "metadata.yhteyshenkilot[0].nimi", invalidKielistetty(Seq(Fi, Sv)))
  }

  it should "fail if hakulomake is invalid" in {
    failsValidation(max.copy(hakulomaketyyppi = Some(Ataru), hakulomakeAtaruId = None, hakulomakeLinkki = Map()), "hakulomakeAtaruId", missingMsg)
    failsValidation(max.copy(hakulomaketyyppi = Some(MuuHakulomake), hakulomakeLinkki = Map(Fi -> "http://url.fi")), "hakulomakeLinkki", invalidKielistetty(Seq(Sv)))
    failsValidation(max.copy(hakulomaketyyppi = Some(MuuHakulomake), hakulomakeLinkki = Map()), "hakulomakeLinkki", invalidKielistetty(Seq(Fi, Sv)))
    failsValidation(max.copy(hakulomaketyyppi = Some(MuuHakulomake), hakulomakeLinkki = Map(Fi -> "http://url.fi", Sv -> "virhe")), "hakulomakeLinkki", invalidUrl("virhe"))
    failsValidation(max.copy(hakulomaketyyppi = Some(EiSähköistä), hakulomakeKuvaus = Map(Fi -> "vain suomeksi"), hakulomakeLinkki = Map()), "hakulomakeKuvaus", invalidKielistetty(Seq(Sv)))
  }

  it should "pass valid hakulomake" in {
    passesValidation(max.copy(hakulomaketyyppi = Some(Ataru), hakulomakeAtaruId = Some(UUID.randomUUID()), hakulomakeLinkki = Map()))
    passesValidation(max.copy(hakulomaketyyppi = Some(MuuHakulomake), hakulomakeLinkki = Map(Fi -> "http://url.fi", Sv -> "http://url.se")))
    passesValidation(max.copy(hakulomaketyyppi = Some(EiSähköistä), hakulomakeLinkki = Map(), hakulomakeKuvaus = Map()))
  }

  it should "pass valid julkaistu haku" in {
    passesValidation(max)
  }

  "Haku on julkaisu validation" should "pass a valid haku" in {
    passesOnJulkaisuValidation(max)
  }

  it should "fail if hakuajat are in the past unless jatkuva haku" in {
    passesValidation(max.copy(tila = Julkaistu, hakuajat = List(pastAjanjakso)))
    failsOnJulkaisuValidation(max.copy(hakutapaKoodiUri = Some("hakutapa_01#1"), hakuajat = List(pastAjanjakso)), "hakuajat[0].paattyy", pastDateMsg(pastAjanjakso.paattyy.get))
    failsOnJulkaisuValidation(max.copy(hakutapaKoodiUri = Some("hakutapa_01#1"), hakuajat = List(onlyAlkaaAjanjakso)), "hakuajat[0].paattyy", missingMsg)
    passesOnJulkaisuValidation(max.copy(hakutapaKoodiUri = Some("hakutapa_03#1"), hakuajat = List(onlyAlkaaAjanjakso)))
  }

  it should "validate metadata" in {
    val metadata = max.metadata.get.copy(tulevaisuudenAikataulu = Seq(pastAjanjakso))
    passesValidation(max.copy(tila = Julkaistu, metadata = Some(metadata)))
    failsOnJulkaisuValidation(max.copy(metadata = Some(metadata)), "metadata.tulevaisuudenAikataulu[0].paattyy", pastDateMsg(pastAjanjakso.paattyy.get))
  }

  it should "fail if ajastettu arkistointi is in the past." in {
    val haku = max.copy(tila = Julkaistu, hakuajat = List(Ajanjakso(alkaa = now(), paattyy = Some(now().plusMonths(1)))), ajastettuHaunJaHakukohteidenArkistointi = Some(now().plusMinutes(5)))
    failsValidation(haku, "ajastettuHaunJaHakukohteidenArkistointi", invalidArkistointiDate(3))
  }

  it should "fail if ajastettu arkistointi is in the past with multiple hakuajat." in {
    val haku = max.copy(tila = Julkaistu, hakuajat = List(Ajanjakso(alkaa = now().minusYears(1), paattyy = Some(now().minusMonths(11))), Ajanjakso(alkaa = now(), paattyy = Some(now().plusMonths(1)))), ajastettuHaunJaHakukohteidenArkistointi = Some(now().plusMinutes(5)))
    failsValidation(haku, "ajastettuHaunJaHakukohteidenArkistointi", invalidArkistointiDate(3))
  }

  it should "pass if no hakuaika päättyy" in {
    val haku = max.copy(tila = Julkaistu, hakuajat = List(onlyAlkaaAjanjakso), ajastettuHaunJaHakukohteidenArkistointi = Some(now().minusDays(1)))
    passesValidation(haku)
  }

  it should "pass if no automaattinen arkistointi value" in {
    val haku = max.copy(tila = Julkaistu, ajastettuHaunJaHakukohteidenArkistointi = None)
    passesValidation(haku)
  }


}

class HakuMetadataValidatorSpec extends SubEntityValidationSpec[HakuMetadata] {
  val metadata: HakuMetadata = JulkaistuHaku.metadata.get
  val pastAjanjakso: Ajanjakso = Ajanjakso(alkaa = TestData.inPast(2000), paattyy = Some(TestData.inPast(100)))

  "HakuMetadata validation" should "pass a valid haku metadata" in {
    passesValidation(Julkaistu, metadata)
  }

  it should "validate yhteyshenkilot" in {
    val m = metadata.copy(yhteyshenkilot = Seq(Yhteyshenkilo(sahkoposti = Map(Fi -> "sahkoposti", Sv -> "email"))))
    failsValidation(Julkaistu, m, "yhteyshenkilot[0].nimi", invalidKielistetty(Seq(Fi, Sv)))
  }

  it should "validate tulevaisuudenAikataulu" in {
    val ajanjakso = Ajanjakso(alkaa = TestData.inFuture(90000), paattyy = Some(TestData.inFuture(9000)))
    failsValidation(Tallennettu, metadata.copy(tulevaisuudenAikataulu = List(ajanjakso)), "tulevaisuudenAikataulu[0]", invalidAjanjaksoMsg(ajanjakso))
  }

  val alkamiskausiJaVuosi: KoulutuksenAlkamiskausi = KoulutuksenAlkamiskausi(
    alkamiskausityyppi = Some(AlkamiskausiJaVuosi),
    koulutuksenAlkamispaivamaara = None,
    koulutuksenPaattymispaivamaara = None,
    koulutuksenAlkamiskausiKoodiUri = Some("kausi_k#1"),
    koulutuksenAlkamisvuosi = Some(LocalDate.now().getYear.toString),
    henkilokohtaisenSuunnitelmanLisatiedot = Map()
  )

  it should "validate koulutuksen alkamiskausi" in {
    passesValidation(Julkaistu, metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiJaVuosi)))
    passesValidation(Tallennettu, metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiJaVuosi.copy(koulutuksenAlkamiskausiKoodiUri = None, koulutuksenAlkamisvuosi = None))))
    failsValidation(Julkaistu, metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiJaVuosi.copy(koulutuksenAlkamiskausiKoodiUri = None))), "koulutuksenAlkamiskausi.koulutuksenAlkamiskausiKoodiUri", missingMsg)
    failsValidation(Julkaistu, metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiJaVuosi.copy(koulutuksenAlkamisvuosi = None))), "koulutuksenAlkamiskausi.koulutuksenAlkamisvuosi", missingMsg)
    failsValidation(Julkaistu, metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiJaVuosi.copy(koulutuksenAlkamiskausiKoodiUri = Some("mummo")))), "koulutuksenAlkamiskausi.koulutuksenAlkamiskausiKoodiUri", validationMsg("mummo"))
    failsValidation(Julkaistu, metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiJaVuosi.copy(koulutuksenAlkamisvuosi = Some("100")))), "koulutuksenAlkamiskausi.koulutuksenAlkamisvuosi", validationMsg("100"))
    failsValidation(Julkaistu, metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiJaVuosi.copy(henkilokohtaisenSuunnitelmanLisatiedot = Map(Fi -> "Lisätiedot")))), "koulutuksenAlkamiskausi.henkilokohtaisenSuunnitelmanLisatiedot", invalidKielistetty(Seq(Sv)))
  }

  it should "fail if alkamisvuosi or alkamiskausi is invalid even when haku is not julkaistu" in {
    failsValidation(Tallennettu, metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiJaVuosi.copy(koulutuksenAlkamisvuosi = Some("20180")))), "koulutuksenAlkamiskausi.koulutuksenAlkamisvuosi", validationMsg("20180"))
    failsValidation(Tallennettu, metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiJaVuosi.copy(koulutuksenAlkamiskausiKoodiUri = Some("tintti_123")))), "koulutuksenAlkamiskausi.koulutuksenAlkamiskausiKoodiUri", validationMsg("tintti_123"))
  }

  it should "return multiple error messages" in {
    failsValidation(
      Tallennettu,
      metadata.copy( koulutuksenAlkamiskausi = Some(alkamiskausiJaVuosi.copy(koulutuksenAlkamisvuosi = Some("20180"), koulutuksenAlkamiskausiKoodiUri = Some("tintti_123")))),
      ("koulutuksenAlkamiskausi.koulutuksenAlkamisvuosi", validationMsg("20180")),
      ("koulutuksenAlkamiskausi.koulutuksenAlkamiskausiKoodiUri", validationMsg("tintti_123")))
  }

  it should "fail if alkamisvuosi is in the past on julkaisu" in {
    passesValidation(Julkaistu, metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiJaVuosi.copy(koulutuksenAlkamisvuosi = Some("2017")))))
    failsOnJulkaisuValidation(metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiJaVuosi.copy(koulutuksenAlkamisvuosi = Some("2017")))), "koulutuksenAlkamiskausi.koulutuksenAlkamisvuosi", pastDateMsg("2017"))
  }

  val alkamiskausiTarkka: KoulutuksenAlkamiskausi = KoulutuksenAlkamiskausi(
    alkamiskausityyppi = Some(TarkkaAlkamisajankohta),
    koulutuksenAlkamispaivamaara = Some(TestData.now()),
    koulutuksenPaattymispaivamaara = Some(TestData.inFuture(300)),
    koulutuksenAlkamiskausiKoodiUri = None,
    koulutuksenAlkamisvuosi = None)

  it should "validate koulutuksen tarkka alkamisaika" in {
    passesValidation(Julkaistu, metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiTarkka)))
    passesValidation(Julkaistu, metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiTarkka.copy(koulutuksenPaattymispaivamaara = None))))
    passesValidation(Tallennettu, metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiTarkka.copy(koulutuksenAlkamispaivamaara = None, koulutuksenPaattymispaivamaara = None))))
    failsValidation(Julkaistu, metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiTarkka.copy(koulutuksenAlkamispaivamaara = None))), "koulutuksenAlkamiskausi.koulutuksenAlkamispaivamaara", missingMsg)
    failsValidation(Julkaistu, metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiTarkka.copy(koulutuksenAlkamispaivamaara = Some(TestData.inFuture(5000))))), "koulutuksenAlkamiskausi.koulutuksenAlkamispaivamaara", InvalidKoulutuspaivamaarat)
  }

  it should "fail if tarkka alkamisaika is invalid even when haku is not julkaistu" in {
    failsValidation(
      Tallennettu,
      metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiTarkka.copy(koulutuksenAlkamispaivamaara = Some(TestData.inFuture(5000)), koulutuksenPaattymispaivamaara = Some( TestData.inPast(300))))),
      "koulutuksenAlkamiskausi.koulutuksenAlkamispaivamaara",
      InvalidKoulutuspaivamaarat)
  }

  it should "fail if alkamisaika is in the past on julkaisu" in {
    val pastDate = TestData.inPast(300)
    passesValidation(Julkaistu, metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiTarkka.copy(koulutuksenAlkamispaivamaara = Some(pastDate)))))
    failsOnJulkaisuValidation(metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausiTarkka.copy(koulutuksenAlkamispaivamaara = Some(pastDate)))), "koulutuksenAlkamiskausi.koulutuksenAlkamispaivamaara", pastDateMsg(pastDate))
  }

  it should "fail if paattymispaiva is in the past on julkaisu" in {
    val (pastDate, evenMorePast) = (TestData.inPast(300), TestData.inPast(700))
    val alkaminenAndPaattyminenInThePast = Some(alkamiskausiTarkka.copy(koulutuksenPaattymispaivamaara = Some(pastDate), koulutuksenAlkamispaivamaara = Some(evenMorePast)))

    passesValidation(Julkaistu, metadata.copy(koulutuksenAlkamiskausi = alkaminenAndPaattyminenInThePast))
    failsOnJulkaisuValidation(
      metadata.copy(koulutuksenAlkamiskausi = alkaminenAndPaattyminenInThePast),
      ("koulutuksenAlkamiskausi.koulutuksenAlkamispaivamaara", pastDateMsg(evenMorePast)),
      ("koulutuksenAlkamiskausi.koulutuksenPaattymispaivamaara", pastDateMsg(pastDate)))
  }

  it should "validate henkilökohtainen suunnitelma" in {
    val alkamiskausi = KoulutuksenAlkamiskausi(
      alkamiskausityyppi = Some(HenkilökohtainenSuunnitelma),
      koulutuksenAlkamispaivamaara = None,
      koulutuksenPaattymispaivamaara = None,
      koulutuksenAlkamiskausiKoodiUri = None,
      koulutuksenAlkamisvuosi = None)

    passesValidation(Julkaistu, metadata.copy(koulutuksenAlkamiskausi = Some(alkamiskausi)))
  }

  "HakuMetadata on julkaisu validation" should "pass a valid haku metadata" in {
    passesOnJulkaisuValidation(metadata)
  }

  it should "validate tulevaisuudenAikataulu" in {
    passesValidation(Julkaistu, metadata.copy(tulevaisuudenAikataulu = List(pastAjanjakso)))
    failsOnJulkaisuValidation(metadata.copy(tulevaisuudenAikataulu = List(pastAjanjakso)), "tulevaisuudenAikataulu[0].paattyy", pastDateMsg(pastAjanjakso.paattyy.get))
  }
}
