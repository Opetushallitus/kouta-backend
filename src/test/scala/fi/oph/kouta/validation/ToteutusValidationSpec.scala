package fi.oph.kouta.validation

import fi.oph.kouta.TestData.{JulkaistuAmmToteutus, MinToteutus}
import fi.oph.kouta.domain.{Fi, Sv, Toteutus}

class ToteutusValidationSpec extends BaseValidationSpec[Toteutus] with ValidationMessages {

  val amm = JulkaistuAmmToteutus
  val min = MinToteutus

  it should "fail if perustiedot is invalid" in {
    assertLeft(amm.copy(oid = Some("moikka")), invalidOidMsg("moikka"))
    assertLeft(amm.copy(kielivalinta = Seq()), MissingKielivalinta)
    assertLeft(amm.copy(nimi = Map(Fi -> "nimi")), invalidKielistetty("nimi", Seq(Sv)))
    assertLeft(amm.copy(nimi = Map(Fi -> "nimi", Sv -> "")), invalidKielistetty("nimi", Seq(Sv)))
    assertLeft(amm.copy(muokkaaja = "moikka"), invalidOidMsg("moikka"))
  }

  it should "pass imcomplete toteutus if not julkaistu" in {
    assertRight(min)
  }

  it should "fail if toteutus oid is invalid" in {
    assertLeft(min.copy(oid = Some("1.2.3")), invalidToteutusOidMsg("1.2.3"))
  }

  it should "fail if julkaistu toteutus is invalid" in {
    assertLeft(amm.copy(tarjoajat = List("mummo", "varis", "1.2.3")), invalidTarjoajaOids(List("mummo", "varis")))
  }

  it should "pass valid ammatillinen toteutus" in {
    assertRight(amm)
  }
}
