package fi.oph.kouta.validation

import fi.oph.kouta.TestData.{MinSorakuvaus, YoSorakuvaus}
import fi.oph.kouta.domain.oid.UserOid
import fi.oph.kouta.domain.{Fi, Sorakuvaus, Sv}
import fi.oph.kouta.validation.Validations._

class SorakuvausValidationSpec extends BaseValidationSpec[Sorakuvaus] {

  val max = YoSorakuvaus
  val min = MinSorakuvaus

  it should "fail if perustiedot is invalid" in {
    failsValidation(max.copy(kielivalinta = Seq()), MissingKielivalinta)
    failsValidation(max.copy(nimi = Map(Fi -> "nimi")), invalidKielistetty("nimi", Seq(Sv)))
    failsValidation(max.copy(nimi = Map(Fi -> "nimi", Sv -> "")), invalidKielistetty("nimi", Seq(Sv)))
    failsValidation(max.copy(muokkaaja = UserOid("moikka")), validationMsg("moikka"))
  }

  it should "pass imcomplete sorakuvaus if not julkaistu" in {
    passesValidation(min)
  }

  it should "pass valid julkaistu sorakuvaus" in {
    passesValidation(max)
  }
}
