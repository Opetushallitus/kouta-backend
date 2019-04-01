package fi.oph.kouta.validation

import fi.oph.kouta.TestData.{MinYoValintaperuste, YoValintaperuste}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.UserOid

class ValintaperusteValidationSpec  extends BaseValidationSpec[Valintaperuste] with Validations {

  val max = YoValintaperuste
  val min = MinYoValintaperuste

  it should "fail if perustiedot is invalid" in {
    assertLeft(max.copy(kielivalinta = Seq()), MissingKielivalinta)
    assertLeft(max.copy(nimi = Map(Fi -> "nimi")), invalidKielistetty("nimi", Seq(Sv)))
    assertLeft(max.copy(nimi = Map(Fi -> "nimi", Sv -> "")), invalidKielistetty("nimi", Seq(Sv)))
    assertLeft(max.copy(muokkaaja = UserOid("moikka")), validationMsg("moikka"))
  }

  it should "pass imcomplete valintaperuste if not julkaistu" in {
    assertRight(min)
  }

  it should "fail if julkaistu valintaperuste is invalid" in {
    assertLeft(max.copy(hakutapaKoodiUri = Some("korppi")), validationMsg("korppi"))
    assertLeft(max.copy(kohdejoukkoKoodiUri = Some("kerttu")), validationMsg("kerttu"))
    assertLeft(max.copy(kohdejoukonTarkenneKoodiUri = Some("tonttu")), validationMsg("tonttu"))
  }

  it should "pass valid julkaistu valintaperuste" in {
    assertRight(max)
  }

  it should "return multiple error messages" in {
    assertLeft(max.copy(hakutapaKoodiUri = None, kohdejoukkoKoodiUri = None),
      List(missingMsg("hakutapaKoodiUri"), missingMsg("kohdejoukkoKoodiUri")))
  }
}

