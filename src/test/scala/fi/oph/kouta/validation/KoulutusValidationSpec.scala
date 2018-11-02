package fi.oph.kouta.validation

import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._

class KoulutusValidationSpec extends BaseValidationSpec[Koulutus] with ValidationMessages {

  val amm = AmmKoulutus
  val min = MinKoulutus

  it should "fail if perustiedot is invalid" in {
    assertLeft(amm.copy(oid = Some("moikka")), invalidOidMsg("moikka"))
    assertLeft(amm.copy(kielivalinta = Seq()), MissingKielivalinta)
    assertLeft(amm.copy(nimi = Map(Fi -> "nimi")), invalidKielistetty("nimi", Seq(Sv)))
    assertLeft(amm.copy(nimi = Map(Fi -> "nimi", Sv -> "")), invalidKielistetty("nimi", Seq(Sv)))
    assertLeft(amm.copy(muokkaaja = "moikka"), invalidOidMsg("moikka"))
  }

  it should "pass imcomplete koulutus if not julkaistu" in {
    assertRight(min)
  }

  it should "fail if koulutus oid is invalid" in {
    assertLeft(min.copy(oid = Some("1.2.3")), invalidKoulutusOidMsg("1.2.3"))
  }

  it should "fail if julkaistu koulutus is invalid" in {
    assertLeft(amm.copy(koulutustyyppi = None), MissingKoulutustyyppi)
    assertLeft(amm.copy(johtaaTutkintoon = false), invalidTutkintoonjohtavuus("amm"))
    assertLeft(amm.copy(koulutusKoodiUri = None), MissingKoulutuskoodi)
    assertLeft(amm.copy(koulutusKoodiUri = Some("mummo")), invalidKoulutuskoodi("mummo"))
    assertLeft(amm.copy(tarjoajat = List("mummo", "varis", "1.2.3")), invalidTarjoajaOids(List("mummo", "varis")))
  }

  it should "pass valid ammatillinen koulutus" in {
    assertRight(amm)
  }
}
