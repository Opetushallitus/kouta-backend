package fi.oph.kouta.validation

import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._

class KoulutusValidationSpec extends BaseValidationSpec[Koulutus] with Validations {

  val amm = AmmKoulutus
  val min = MinKoulutus

  it should "fail if perustiedot is invalid" in {
    assertLeft(amm.copy(oid = Some(KoulutusOid("moikka"))), validationMsg("moikka"))
    assertLeft(amm.copy(kielivalinta = Seq()), MissingKielivalinta)
    assertLeft(amm.copy(nimi = Map(Fi -> "nimi")), invalidKielistetty("nimi", Seq(Sv)))
    assertLeft(amm.copy(nimi = Map(Fi -> "nimi", Sv -> "")), invalidKielistetty("nimi", Seq(Sv)))
    assertLeft(amm.copy(muokkaaja = UserOid("moikka")), validationMsg("moikka"))
  }

  it should "pass imcomplete koulutus if not julkaistu" in {
    assertRight(min)
  }

  it should "fail if koulutus oid is invalid" in {
    assertLeft(min.copy(oid = Some(KoulutusOid("1.2.3"))), validationMsg("1.2.3"))
  }

  it should "fail if julkaistu koulutus is invalid" in {
    assertLeft(amm.copy(koulutustyyppi = None), missingMsg("koulutustyyppi"))
    assertLeft(amm.copy(johtaaTutkintoon = false), invalidTutkintoonjohtavuus("amm"))
    assertLeft(amm.copy(koulutusKoodiUri = None), missingMsg("koulutusKoodiUri"))
    assertLeft(amm.copy(koulutusKoodiUri = Some("mummo")), validationMsg("mummo"))
    assertLeft(amm.copy(tarjoajat = List("mummo", "varis", "1.2.3").map(OrganisaatioOid)), invalidOidsMsg(List("mummo", "varis").map(OrganisaatioOid)))
  }

  it should "pass valid ammatillinen koulutus" in {
    assertRight(amm)
  }

  it should "return multiple error messages" in {
    assertLeft(min.copy(koulutusKoodiUri = Some("ankka"), oid = Some(KoulutusOid("2017"))),
      List(validationMsg("ankka"), validationMsg("2017")))
  }
}
