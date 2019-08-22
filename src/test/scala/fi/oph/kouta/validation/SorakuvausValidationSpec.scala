package fi.oph.kouta.validation

import fi.oph.kouta.domain.{Fi, Sorakuvaus, Sv}
import fi.oph.kouta.TestData.{MinSorakuvaus, YoSorakuvaus}
import fi.oph.kouta.domain.oid.UserOid

class SorakuvausValidationSpec extends BaseValidationSpec[Sorakuvaus] with Validations {

  val max = YoSorakuvaus
  val min = MinSorakuvaus

  it should "fail if perustiedot is invalid" in {
    assertLeft(max.copy(kielivalinta = Seq()), MissingKielivalinta)
    assertLeft(max.copy(nimi = Map(Fi -> "nimi")), invalidKielistetty("nimi", Seq(Sv)))
    assertLeft(max.copy(nimi = Map(Fi -> "nimi", Sv -> "")), invalidKielistetty("nimi", Seq(Sv)))
    assertLeft(max.copy(muokkaaja = UserOid("moikka")), validationMsg("moikka"))
  }

  it should "pass imcomplete sorakuvaus if not julkaistu" in {
    assertRight(min)
  }

  it should "pass valid julkaistu sorakuvaus" in {
    assertRight(max)
  }
}
