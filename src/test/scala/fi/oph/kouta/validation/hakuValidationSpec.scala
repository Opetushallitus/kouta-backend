package fi.oph.kouta.validation

import java.util.UUID

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.validation.Validations._

class HakuValidationSpec extends BaseValidationSpec[Haku] {

  val max = JulkaistuHaku
  val min = MinHaku
  val pastAjanjakso = Ajanjakso(alkaa = TestData.inPast(2000), paattyy = TestData.inPast(100))

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

  it should "pass non-julkaistu haku even if alkamisvuosi is invalid" in {
    passesValidation(min.copy(alkamisvuosi = Some("2017")))
    passesValidation(min.copy(alkamisvuosi = Some("20180")))
  }

  it should "fail if julkaistu haku is invalid" in {
    failsValidation(max.copy(hakutapaKoodiUri = None), "hakutapaKoodiUri", missingMsg)
    failsValidation(max.copy(hakutapaKoodiUri = Some("korppi")), "hakutapaKoodiUri", validationMsg("korppi"))

    failsValidation(max.copy(alkamiskausiKoodiUri = Some("tintti")), "alkamiskausiKoodiUri", validationMsg("tintti"))
    failsValidation(max.copy(alkamiskausiKoodiUri = None, hakutapaKoodiUri = Some("hakutapa_01#1")), "alkamiskausiKoodiUri", missingMsg)

    failsValidation(max.copy(alkamisvuosi = None, hakutapaKoodiUri = Some("hakutapa_01#1")), "alkamisvuosi", missingMsg)
    failsValidation(max.copy(alkamisvuosi = Some("20180")), "alkamisvuosi", validationMsg("20180"))

    failsValidation(max.copy(kohdejoukkoKoodiUri = None), "kohdejoukkoKoodiUri", missingMsg)
    failsValidation(max.copy(kohdejoukkoKoodiUri = Some("kerttu")), "kohdejoukkoKoodiUri", validationMsg("kerttu"))
    failsValidation(max.copy(kohdejoukonTarkenneKoodiUri = Some("tonttu")), "kohdejoukonTarkenneKoodiUri", validationMsg("tonttu"))
    failsValidation(max.copy(hakulomaketyyppi = None), "hakulomaketyyppi", missingMsg)

    val invalidAjanjakso = Ajanjakso(alkaa = TestData.inFuture(90000), paattyy = TestData.inFuture(9000))
    failsValidation(max.copy(hakuajat = List(invalidAjanjakso)), "hakuajat[0]", invalidAjanjaksoMsg(invalidAjanjakso))
  }

  it should "fail if metadata is invalid" in {
    val ajanjakso = Ajanjakso(alkaa = TestData.inFuture(90000), paattyy = TestData.inFuture(9000))
    val metadata = max.metadata.get.copy(tulevaisuudenAikataulu = List(ajanjakso))
    failsValidation(max.copy(metadata = Some(metadata)), "metadata.tulevaisuudenAikataulu[0]", invalidAjanjaksoMsg(ajanjakso))
  }

  it should "fail if yhteyshenkilot has other info, but no name" in {
    val metadata = max.metadata.get.copy(yhteyshenkilot = Seq(Yhteyshenkilo(sahkoposti = Map(Fi -> "sahkoposti", Sv -> "email"))))
    failsValidation(max.copy(metadata = Some(metadata)), "metadata.yhteyshenkilot[0].nimi", invalidKielistetty(Seq(Fi, Sv)))
  }

  it should "fail if hakulomake is invalid" in {
    failsValidation(max.copy(hakulomaketyyppi = Some(Ataru), hakulomakeAtaruId = None), "hakulomakeAtaruId", missingMsg)
    failsValidation(max.copy(hakulomaketyyppi = Some(MuuHakulomake), hakulomakeLinkki = Map(Fi -> "http://url.fi")), "hakulomakeLinkki", invalidKielistetty(Seq(Sv)))
    failsValidation(max.copy(hakulomaketyyppi = Some(MuuHakulomake), hakulomakeLinkki = Map()), "hakulomakeLinkki", invalidKielistetty(Seq(Fi, Sv)))
    failsValidation(max.copy(hakulomaketyyppi = Some(MuuHakulomake), hakulomakeLinkki = Map(Fi -> "http://url.fi", Sv -> "virhe")), "hakulomakeLinkki", invalidUrl("virhe"))
    failsValidation(max.copy(hakulomaketyyppi = Some(EiSähköistä), hakulomakeKuvaus = Map(Fi -> "kuvaus")), "hakulomakeKuvaus", invalidKielistetty(Seq(Sv)))
  }

  it should "pass valid hakulomake" in {
    passesValidation(max.copy(hakulomaketyyppi = Some(Ataru), hakulomakeAtaruId = Some(UUID.randomUUID())))
    passesValidation(max.copy(hakulomaketyyppi = Some(MuuHakulomake), hakulomakeLinkki = Map(Fi -> "http://url.fi", Sv -> "http://url.se")))
    passesValidation(max.copy(hakulomaketyyppi = Some(EiSähköistä), hakulomakeKuvaus = Map()))
  }

  it should "pass valid julkaistu haku" in {
    passesValidation(max)
  }

  it should "return multiple error messages" in {
    failsValidation(max.copy(hakutapaKoodiUri = Some("korppi"), alkamisvuosi = Some("02017")),
      ("hakutapaKoodiUri", validationMsg("korppi")), ("alkamisvuosi", validationMsg("02017")))
  }

  "Haku on julkaisu validation" should "pass a valid haku" in {
    passesOnJulkaisuValidation(max)
  }

  it should "fail if alkamisvuosi is in the past" in {
    passesValidation(max.copy(tila = Julkaistu, alkamisvuosi = Some("2017")))
    failsOnJulkaisuValidation(max.copy(alkamisvuosi = Some("2017")), "alkamisvuosi", pastDateMsg("2017"))
  }

  it should "fail if hakuajat are in the past" in {
    passesValidation(max.copy(tila = Julkaistu, hakuajat = List(pastAjanjakso)))
    failsOnJulkaisuValidation(max.copy(hakuajat = List(pastAjanjakso)), "hakuajat[0].paattyy", pastDateMsg(pastAjanjakso.paattyy))
  }

  it should "validate metadata" in {
    val metadata = max.metadata.get.copy(tulevaisuudenAikataulu = Seq(pastAjanjakso))
    passesValidation(max.copy(tila = Julkaistu, metadata = Some(metadata)))
    failsOnJulkaisuValidation(max.copy(metadata = Some(metadata)), "metadata.tulevaisuudenAikataulu[0].paattyy", pastDateMsg(pastAjanjakso.paattyy))
  }
}

class HakuMetadataValidatorSpec extends SubEntityValidationSpec[HakuMetadata] {
  val metadata = JulkaistuHaku.metadata.get
  val pastAjanjakso = Ajanjakso(alkaa = TestData.inPast(2000), paattyy = TestData.inPast(100))

  "HakuMetadata validation" should "pass a valid haku metadata" in {
    passesValidation(Julkaistu, metadata)
  }

  it should "validate yhteyshenkilot" in {
    val m = metadata.copy(yhteyshenkilot = Seq(Yhteyshenkilo(sahkoposti = Map(Fi -> "sahkoposti", Sv -> "email"))))
    failsValidation(Julkaistu, m, "yhteyshenkilot[0].nimi", invalidKielistetty(Seq(Fi, Sv)))
  }

  it should "validate tulevaisuudenAikataulu" in {
    val ajanjakso = Ajanjakso(alkaa = TestData.inFuture(90000), paattyy = TestData.inFuture(9000))
    failsValidation(Tallennettu, metadata.copy(tulevaisuudenAikataulu = List(ajanjakso)), "tulevaisuudenAikataulu[0]", invalidAjanjaksoMsg(ajanjakso))
  }

  "HakuMetadata on julkaisu validation" should "pass a valid haku metadata" in {
    passesOnJulkaisuValidation(metadata)
  }

  it should "validate tulevaisuudenAikataulu" in {
    passesValidation(Julkaistu, metadata.copy(tulevaisuudenAikataulu = List(pastAjanjakso)))
    failsOnJulkaisuValidation(metadata.copy(tulevaisuudenAikataulu = List(pastAjanjakso)), "tulevaisuudenAikataulu[0].paattyy", pastDateMsg(pastAjanjakso.paattyy))
  }
}
