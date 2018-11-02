package fi.oph.kouta.validation

import fi.oph.kouta.TestData.{JulkaistuValintaperuste, MinValintaperuste}
import fi.oph.kouta.domain._

class ValintaperusteValidationSpec  extends BaseValidationSpec[Valintaperuste] with ValidationMessages {

  val max = JulkaistuValintaperuste
  val min = MinValintaperuste

  it should "fail if perustiedot is invalid" in {
    assertLeft(max.copy(kielivalinta = Seq()), MissingKielivalinta)
    assertLeft(max.copy(nimi = Map(Fi -> "nimi")), invalidKielistetty("nimi", Seq(Sv)))
    assertLeft(max.copy(nimi = Map(Fi -> "nimi", Sv -> "")), invalidKielistetty("nimi", Seq(Sv)))
    assertLeft(max.copy(muokkaaja = "moikka"), invalidOidMsg("moikka"))
  }

  it should "pass imcomplete valintaperuste if not julkaistu" in {
    assertRight(min)
  }

  it should "fail if julkaistu valintaperuste is invalid" in {
    assertLeft(max.copy(hakutapaKoodiUri = Some("korppi")), invalidHakutapaKoodi("korppi"))
    assertLeft(max.copy(kohdejoukkoKoodiUri = Some("kerttu")), invalidKohdejoukkoKoodi("kerttu"))
    assertLeft(max.copy(kohdejoukonTarkenneKoodiUri = Some("tonttu")), invalidKohdejoukonTarkenneKoodi("tonttu"))
  }

  it should "pass valid julkaistu valintaperuste" in {
    assertRight(max)
  }
}

