package fi.oph.kouta.validation

import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.validation.Validations._

class ValintakoeValidationSpec extends SubEntityValidationSpec[Valintakoe] {

  "Valintakoe validation" should "accept proper valintakoe" in {
    passesValidation(Julkaistu, Valintakoe1)
  }

  it should "fail if the tyyppi koodi is invalid" in {
    failsValidation(Tallennettu, Valintakoe1.copy(tyyppiKoodiUri = Some("mummo")), "tyyppiKoodiUri", validationMsg("mummo"))
  }

  it should "fail if valintakoetilaisuudet are invalid" in {
    failsValidation(
      Tallennettu,
      Valintakoe1.copy(tilaisuudet = Valintakoe1.tilaisuudet.map(_.copy(osoite = Some(Osoite1.copy(postinumeroKoodiUri = Some("mummo")))))),
      "tilaisuudet[0].osoite.postinumeroKoodiUri",
      validationMsg("mummo"))
  }

  "Valintakoe on julkaisu validation" should "pass a valid valintakoe" in {
    passesOnJulkaisuValidation(Valintakoe1)
  }

  it should "validate tilaisuudet" in {
    val ajanjakso = Ajanjakso(alkaa = inPast(4000), paattyy = inPast(2000))
    val tilaisuus = Valintakoe1.tilaisuudet.head.copy(aika = Some(ajanjakso))
    val koe = Valintakoe1.copy(tilaisuudet = List(tilaisuus))
    passesValidation(Julkaistu, koe)
    failsOnJulkaisuValidation(koe, "tilaisuudet[0].aika.paattyy", pastDateMsg(ajanjakso.paattyy))
  }
}

class ValintakoetilaisuusValidationSpec extends SubEntityValidationSpec[Valintakoetilaisuus] {

  val Valintakoetilaisuus1 = Valintakoe1.tilaisuudet.head

  "Valintakoetilaisuus validation" should "accept proper valintakoetilaisuus" in {
    passesValidation(Julkaistu, Valintakoetilaisuus1)
  }

  it should "validate osoite" in {
    failsValidation(
      Tallennettu,
      Valintakoetilaisuus1.copy(osoite = Some(Osoite1.copy(postinumeroKoodiUri = Some("false")))),
      "osoite.postinumeroKoodiUri",
      validationMsg("false"))
  }

  it should "validate ajanjakso" in {
    val ajanjakso = Ajanjakso(alkaa = inFuture(2000), paattyy = inFuture(100))
    failsValidation(Tallennettu, Valintakoetilaisuus1.copy(aika = Some(ajanjakso)), "aika", invalidAjanjaksoMsg(ajanjakso))
  }

  it should "fail if osoite is missing when julkaistu" in {
    passesValidation(Tallennettu, Valintakoetilaisuus1.copy(osoite = None))
    failsValidation(Julkaistu, Valintakoetilaisuus1.copy(osoite = None), "osoite", missingMsg)
  }

  it should "fail if aika is missing when julkaistu" in {
    passesValidation(Tallennettu, Valintakoetilaisuus1.copy(aika = None))
    failsValidation(Julkaistu, Valintakoetilaisuus1.copy(aika = None), "aika", missingMsg)
  }

  it should "fail if lisatietoja is missing languages when julkaistu" in {
    passesValidation(Tallennettu, Valintakoetilaisuus1.copy(lisatietoja = Map(Fi -> "lisatietoja fi")))
    failsValidation(Julkaistu, Valintakoetilaisuus1.copy(lisatietoja = Map(Fi -> "lisatietoja fi")), "lisatietoja", invalidKielistetty(Seq(Sv)))
  }

  "Valintakoetilaisuus on julkaisu validation" should "pass a valid valintakoetilaisuus" in {
    passesOnJulkaisuValidation(Valintakoetilaisuus1)
  }

  it should "fail if aika ends in the past" in {
    val ajanjakso = Ajanjakso(alkaa = inPast(4000), paattyy = inPast(2000))
    passesValidation(Julkaistu, Valintakoetilaisuus1.copy(aika = Some(ajanjakso)))
    failsOnJulkaisuValidation(Valintakoetilaisuus1.copy(aika = Some(ajanjakso)), "aika.paattyy", pastDateMsg(ajanjakso.paattyy))
  }
}

class OsoiteValidationSpec extends SubEntityValidationSpec[Osoite] {

  "Osoite validation" should "accept a proper osoite" in {
    passesValidation(Julkaistu, Osoite1)
  }

  it should "fail if the postinumeroKoodiUri is invalid" in {
    failsValidation(Tallennettu, Osoite1.copy(postinumeroKoodiUri = Some("false")), "postinumeroKoodiUri", validationMsg("false"))
  }

  it should "fail if osoite is missing languages when julkaistu" in {
    passesValidation(Tallennettu, Osoite1.copy(osoite = Map(Fi -> "osoite")))
    failsValidation(Julkaistu, Osoite1.copy(osoite = Map(Fi -> "osoite")), "osoite", invalidKielistetty(Seq(Sv)))
  }
  it should "fail if postinumeroKoodiUri is missing when julkaistu" in {
    passesValidation(Tallennettu, Osoite1.copy(postinumeroKoodiUri = None))
    failsValidation(Julkaistu, Osoite1.copy(postinumeroKoodiUri = None), "postinumeroKoodiUri", missingMsg)
  }
}

class LisatietoValidationSpec extends SubEntityValidationSpec[Lisatieto] {
  val max = Lisatieto(
    otsikkoKoodiUri = "koulutuksenlisatiedot_03#1",
    teksti = Map(Fi -> "Opintojen lisätieto ", Sv -> "Opintojen lisätieto sv")
  )

  "Lisatieto validator" should "pass a valid lisatieto" in {
    passesValidation(Julkaistu, max)
  }

  it should "fail if otsikkoKoodiUri is invalid" in {
    failsValidation(Tallennettu, max.copy(otsikkoKoodiUri = "mummo"), "otsikkoKoodiUri", validationMsg("mummo"))
  }

  it should "fail if teksti has missing languages in a julkaistu lisatieto" in {
    passesValidation(Tallennettu, max.copy(teksti = Map(Fi -> "teksti")))
    failsValidation(Julkaistu, max.copy(teksti = Map(Fi -> "teksti")), "teksti", invalidKielistetty(Seq(Sv)))
  }
}
