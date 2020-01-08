package fi.oph.kouta.validation

import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._

class KoulutusValidationSpec extends BaseValidationSpec[Koulutus] with Validations {

  val amm = AmmKoulutus
  val min = MinKoulutus

  it should "fail if perustiedot is invalid" in {
    failsValidation(amm.copy(oid = Some(KoulutusOid("moikka"))), validationMsg("moikka"))
    failsValidation(amm.copy(kielivalinta = Seq()), MissingKielivalinta)
    failsValidation(amm.copy(nimi = Map(Fi -> "nimi")), invalidKielistetty("nimi", Seq(Sv)))
    failsValidation(amm.copy(nimi = Map(Fi -> "nimi", Sv -> "")), invalidKielistetty("nimi", Seq(Sv)))
    failsValidation(amm.copy(muokkaaja = UserOid("moikka")), validationMsg("moikka"))
  }

  it should "pass imcomplete koulutus if not julkaistu" in {
    passesValidation(min)
  }

  it should "fail if koulutus oid is invalid" in {
    failsValidation(min.copy(oid = Some(KoulutusOid("1.2.3"))), validationMsg("1.2.3"))
  }

  it should "fail if julkaistu koulutus is invalid" in {
    failsValidation(amm.copy(johtaaTutkintoon = false), invalidTutkintoonjohtavuus("amm"))
    failsValidation(amm.copy(koulutusKoodiUri = None), missingMsg("koulutusKoodiUri"))
    failsValidation(amm.copy(koulutusKoodiUri = Some("mummo")), validationMsg("mummo"))
    failsValidation(amm.copy(tarjoajat = List("mummo", "varis", "1.2.3").map(OrganisaatioOid)), invalidOidsMsg(List("mummo", "varis").map(OrganisaatioOid)))
  }

  it should "pass valid ammatillinen koulutus" in {
    passesValidation(amm)
  }

  it should "return multiple error messages" in {
    failsValidation(min.copy(koulutusKoodiUri = Some("ankka"), oid = Some(KoulutusOid("2017"))),
      List(validationMsg("ankka"), validationMsg("2017")))
  }
}
