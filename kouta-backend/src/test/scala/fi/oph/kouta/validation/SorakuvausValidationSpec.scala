package fi.oph.kouta.validation

import fi.oph.kouta.TestData.{MinSorakuvaus, YoSorakuvaus}
import fi.oph.kouta.domain.{Fi, Sorakuvaus, Sv, Tallennettu}
import fi.oph.kouta.validation.Validations._

class SorakuvausValidationSpec extends BaseValidationSpec[Sorakuvaus] {

  val max: Sorakuvaus = YoSorakuvaus
  val min: Sorakuvaus = MinSorakuvaus

  it should "fail if perustiedot is invalid" in {
    failsValidation(min.copy(kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(min.copy(nimi = Map(Fi -> "nimi")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(max.copy(nimi = Map(Fi -> "nimi", Sv -> "")), "nimi", invalidKielistetty(Seq(Sv)))
  }

  it should "pass incomplete sorakuvaus if not julkaistu" in {
    passesValidation(min)
  }

  it should "pass valid julkaistu sorakuvaus" in {
    passesValidation(max)
  }

  it should "validate kuvaus from metadata" in {
    val metadata = Some(max.metadata.get.copy(kuvaus = Map(Fi -> "kuvaus")))
    failsValidation(max.copy(metadata = metadata), "metadata.kuvaus", invalidKielistetty(Seq(Sv)))
  }

  it should "fail if metadata is missing from a julkaistu sorakuvaus" in {
    passesValidation(max.copy(metadata = None, tila = Tallennettu))
    failsValidation(max.copy(metadata = None), "metadata", missingMsg)
  }

  it should "fail if koulutusala or koulutus is invalid" in {
    failsValidation(max.copy(metadata = Some(max.metadata.get.copy(koulutusalaKoodiUri = Some("mummo")))), "metadata.koulutusalaKoodiUri", validationMsg("mummo"))
    failsValidation(max.copy(metadata = Some(max.metadata.get.copy(koulutusKoodiUrit = Seq("mummo")))), "metadata.koulutusKoodiUrit[0]", validationMsg("mummo"))
  }
}
