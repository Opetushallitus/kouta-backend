package fi.oph.kouta.validation

import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.validation.Validations._
import org.scalatest.Assertion
import org.scalatra.test.scalatest.ScalatraFlatSpec

class ValintakoeValidationSpec extends ScalatraFlatSpec {

  val DefaultKielivalinta = Seq(Fi, Sv)

  def failsValidation(tila: Julkaisutila, e: Valintakoe, expected: String): Assertion = e.validate(tila, DefaultKielivalinta) match {
    case NoErrors => fail("Expecting validation failure, but it succeeded")
    case errors => errors should contain theSameElementsAs Seq(expected)
  }

  def passesValidation(tila: Julkaisutila, e: Valintakoe) = e.validate(tila, DefaultKielivalinta) should equal(NoErrors)

  "Valintakoe validation" should "accept proper valintakoe" in {
    passesValidation(Julkaistu, Valintakoe1)
  }

  it should "fail if the tyyppi koodi is invalid" in {
    failsValidation(Tallennettu, Valintakoe1.copy(tyyppiKoodiUri = Some("mummo")), validationMsg("mummo"))
  }

  it should "fail if valintakoetilaisuudet are invalid" in {
    failsValidation(Tallennettu, Valintakoe1.copy(tilaisuudet = Valintakoe1.tilaisuudet.map(_.copy(osoite = Some(Osoite1.copy(postinumeroKoodiUri = Some("mummo")))))), validationMsg("mummo"))
  }

  def failsValidation(tila: Julkaisutila, e: Valintakoetilaisuus, expected: String): Assertion = e.validate(tila, DefaultKielivalinta) match {
    case NoErrors => fail("Expecting validation failure, but it succeeded")
    case errors => errors should contain theSameElementsAs Seq(expected)
  }

  def passesValidation(tila: Julkaisutila, e: Valintakoetilaisuus) = e.validate(tila, DefaultKielivalinta) should equal(NoErrors)

  val Valintakoetilaisuus1 = Valintakoe1.tilaisuudet.head

  "Valintakoetilaisuus validation" should "accept proper valintakoetilaisuus" in {
    passesValidation(Julkaistu, Valintakoetilaisuus1)
  }

  it should "validate osoite" in {
    failsValidation(Tallennettu, Valintakoetilaisuus1.copy(osoite = Some(Osoite1.copy(postinumeroKoodiUri = Some("false")))), validationMsg("false"))
  }

  it should "validate ajanjakso" in {
    val ajanjakso = Ajanjakso(alkaa = inFuture(2000), paattyy = inFuture(100))
    failsValidation(Tallennettu, Valintakoetilaisuus1.copy(aika = Some(ajanjakso)), invalidAjanjakso(ajanjakso, "aika"))
  }

  it should "fail if osoite is missing when julkaistu" in {
    passesValidation(Tallennettu, Valintakoetilaisuus1.copy(osoite = None))
    failsValidation(Julkaistu, Valintakoetilaisuus1.copy(osoite = None), missingMsg("osoite"))
  }

  it should "fail if aika ends in the past when julkaistu" in {
    val ajanjakso = Ajanjakso(alkaa = inPast(4000), paattyy = inPast(2000))
    passesValidation(Tallennettu, Valintakoetilaisuus1.copy(aika = Some(ajanjakso)))
    failsValidation(Julkaistu, Valintakoetilaisuus1.copy(aika = Some(ajanjakso)), pastAjanjaksoMsg(ajanjakso, "Aika"))
  }

  it should "fail if aika is missing when julkaistu" in {
    passesValidation(Tallennettu, Valintakoetilaisuus1.copy(aika = None))
    failsValidation(Julkaistu, Valintakoetilaisuus1.copy(aika = None), missingMsg("aika"))
  }

  def failsValidation(tila: Julkaisutila, e: Osoite, expected: String): Assertion = e.validate(tila, DefaultKielivalinta) match {
    case NoErrors => fail("Expecting validation failure, but it succeeded")
    case errors => errors should contain theSameElementsAs Seq(expected)
  }

  def passesValidation(tila: Julkaisutila, e: Osoite) = e.validate(tila, DefaultKielivalinta) should equal(NoErrors)

  "Osoite validation" should "accept a proper osoite" in {
    passesValidation(Julkaistu, Osoite1)
  }

  it should "fail if the postinumeroKoodiUri is invalid" in {
    failsValidation(Tallennettu, Osoite1.copy(postinumeroKoodiUri = Some("false")), validationMsg("false"))
  }

  it should "fail if osoite is missing languages when julkaistu" in {
    passesValidation(Tallennettu, Osoite1.copy(osoite = Map(Fi -> "osoite")))
    failsValidation(Julkaistu, Osoite1.copy(osoite = Map(Fi -> "osoite")), invalidKielistetty("osoite", Seq(Sv)))
  }
  it should "fail if postinumeroKoodiUri is missing when julkaistu" in {
    passesValidation(Tallennettu, Osoite1.copy(postinumeroKoodiUri = None))
    failsValidation(Julkaistu, Osoite1.copy(postinumeroKoodiUri = None), missingMsg("postinumeroKoodiUri"))
  }
}
