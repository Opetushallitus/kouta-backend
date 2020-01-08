package fi.oph.kouta.validation

import java.time.Instant

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData._
import fi.oph.kouta.domain.{Ajanjakso, Fi, Haku, Sv}
import fi.oph.kouta.domain.oid._

class HakuValidationSpec extends BaseValidationSpec[Haku] with Validations {

  val max = JulkaistuHaku
  val min = MinHaku

  it should "fail if perustiedot is invalid" in {
    failsValidation(max.copy(oid = Some(HakuOid("moikka"))), validationMsg("moikka"))
    failsValidation(max.copy(kielivalinta = Seq()), MissingKielivalinta)
    failsValidation(max.copy(nimi = Map(Fi -> "nimi")), invalidKielistetty("nimi", Seq(Sv)))
    failsValidation(max.copy(nimi = Map(Fi -> "nimi", Sv -> "")), invalidKielistetty("nimi", Seq(Sv)))
    failsValidation(max.copy(muokkaaja = UserOid("moikka")), validationMsg("moikka"))
  }

  it should "pass imcomplete haku if not julkaistu" in {
    passesValidation(min)
  }

  it should "fail if haku oid is invalid" in {
    failsValidation(min.copy(oid = Some(HakuOid("1.2.3"))), validationMsg("1.2.3"))
  }

  it should "fail if julkaistu haku is invalid" in {
    failsValidation(max.copy(hakutapaKoodiUri = None), missingMsg("hakutapaKoodiUri"))
    failsValidation(max.copy(hakutapaKoodiUri = Some("korppi")), validationMsg("korppi"))
    failsValidation(max.copy(alkamiskausiKoodiUri = Some("tintti")), validationMsg("tintti"))
    failsValidation(max.copy(alkamisvuosi = Some("20180")), validationMsg("20180"))
    failsValidation(max.copy(alkamisvuosi = Some("2017")), validationMsg("2017"))
    failsValidation(max.copy(kohdejoukkoKoodiUri = Some("kerttu")), validationMsg("kerttu"))
    failsValidation(max.copy(kohdejoukonTarkenneKoodiUri = Some("tonttu")), validationMsg("tonttu"))
    failsValidation(max.copy(hakulomaketyyppi = None), missingMsg("hakulomaketyyppi"))
    failsValidation(max.copy(hakuajat = List(Ajanjakso(alkaa = TestData.inFuture(90000), paattyy = TestData.inFuture(9000)))), InvalidHakuaika)

  }

  it should "pass valid julkaistu haku" in {
    passesValidation(max)
  }

  it should "return multiple error messages" in {
    failsValidation(min.copy(hakutapaKoodiUri = Some("korppi"), alkamisvuosi = Some("2017")),
      List(validationMsg("korppi"), validationMsg("2017")))
  }
}
