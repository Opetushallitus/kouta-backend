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

  it should "fail if aika ends in the past when julkaistu" in {
    val ajanjakso = Ajanjakso(alkaa = inPast(4000), paattyy = inPast(2000))
    passesValidation(Tallennettu, Valintakoetilaisuus1.copy(aika = Some(ajanjakso)))
    failsValidation(Julkaistu, Valintakoetilaisuus1.copy(aika = Some(ajanjakso)), "aika", pastAjanjaksoMsg(ajanjakso))
  }

  it should "fail if aika is missing when julkaistu" in {
    passesValidation(Tallennettu, Valintakoetilaisuus1.copy(aika = None))
    failsValidation(Julkaistu, Valintakoetilaisuus1.copy(aika = None), "aika", missingMsg)
  }

  it should "fail if lisatietoja is missing languages when julkaistu" in {
    passesValidation(Tallennettu, Valintakoetilaisuus1.copy(lisatietoja = Map(Fi -> "lisatietoja fi")))
    failsValidation(Julkaistu, Valintakoetilaisuus1.copy(lisatietoja = Map(Fi -> "lisatietoja fi")), "lisatietoja", invalidKielistetty(Seq(Sv)))
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
