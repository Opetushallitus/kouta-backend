package fi.oph.kouta.validation

import fi.oph.kouta.TestData.{MinYoValintaperuste, YoValintaperuste}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.UserOid
import fi.oph.kouta.validation.Validations._

class ValintaperusteValidationSpec extends BaseValidationSpec[Valintaperuste] {

  val max = YoValintaperuste
  val min = MinYoValintaperuste

  it should "fail if perustiedot is invalid" in {
    failsValidation(max.copy(kielivalinta = Seq()), MissingKielivalinta)
    failsValidation(max.copy(nimi = Map(Fi -> "nimi")), invalidKielistetty("nimi", Seq(Sv)))
    failsValidation(max.copy(nimi = Map(Fi -> "nimi", Sv -> "")), invalidKielistetty("nimi", Seq(Sv)))
    failsValidation(max.copy(muokkaaja = UserOid("moikka")), validationMsg("moikka"))
  }

  it should "pass imcomplete valintaperuste if not julkaistu" in {
    passesValidation(min)
  }

  it should "fail if julkaistu valintaperuste is invalid" in {
    failsValidation(max.copy(hakutapaKoodiUri = Some("korppi")), validationMsg("korppi"))
    failsValidation(max.copy(kohdejoukkoKoodiUri = Some("kerttu")), validationMsg("kerttu"))
    failsValidation(max.copy(kohdejoukonTarkenneKoodiUri = Some("tonttu")), validationMsg("tonttu"))
  }

  it should "pass valid julkaistu valintaperuste" in {
    passesValidation(max)
  }

  it should "return multiple error messages" in {
    failsValidation(max.copy(hakutapaKoodiUri = None, kohdejoukkoKoodiUri = None),
      missingMsg("hakutapaKoodiUri"), missingMsg("kohdejoukkoKoodiUri"))
  }
}

