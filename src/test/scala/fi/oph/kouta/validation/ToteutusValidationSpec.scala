package fi.oph.kouta.validation

import fi.oph.kouta.TestData.{JulkaistuAmmToteutus, MinToteutus}
import fi.oph.kouta.domain.{Fi, Sv, Toteutus}
import fi.oph.kouta.domain.oid._

class ToteutusValidationSpec extends BaseValidationSpec[Toteutus] with Validations {

  val amm = JulkaistuAmmToteutus
  val min = MinToteutus

  it should "fail if perustiedot is invalid" in {
    failsValidation(amm.copy(oid = Some(ToteutusOid("moikka"))), validationMsg("moikka"))
    failsValidation(amm.copy(kielivalinta = Seq()), MissingKielivalinta)
    failsValidation(amm.copy(nimi = Map(Fi -> "nimi")), invalidKielistetty("nimi", Seq(Sv)))
    failsValidation(amm.copy(nimi = Map(Fi -> "nimi", Sv -> "")), invalidKielistetty("nimi", Seq(Sv)))
    failsValidation(amm.copy(muokkaaja = UserOid("moikka")), validationMsg("moikka"))
  }

  it should "pass imcomplete toteutus if not julkaistu" in {
    passesValidation(min)
  }

  it should "fail if toteutus oid is invalid" in {
    failsValidation(min.copy(oid = Some(ToteutusOid("1.2.3"))), validationMsg("1.2.3"))
  }

  it should "fail if julkaistu toteutus is invalid" in {
    failsValidation(amm.copy(tarjoajat = List("mummo", "varis", "1.2.3").map(OrganisaatioOid)), invalidOidsMsg(List("mummo", "varis").map(OrganisaatioOid)))
  }

  it should "pass valid ammatillinen toteutus" in {
    passesValidation(amm)
  }

  it should "return multiple error messages" in {
    failsValidation(min.copy(oid = Some(ToteutusOid("kurppa")), muokkaaja = UserOid("Hannu Hanhi")),
      List(validationMsg("kurppa"), validationMsg("Hannu Hanhi")))
  }
}
