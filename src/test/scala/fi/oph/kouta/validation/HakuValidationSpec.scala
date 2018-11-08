package fi.oph.kouta.validation

import java.time.Instant

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData._
import fi.oph.kouta.domain.{Ajanjakso, Fi, Haku, Sv}

class HakuValidationSpec extends BaseValidationSpec[Haku] with Validations {

  val max = JulkaistuHaku
  val min = MinHaku

  it should "fail if perustiedot is invalid" in {
    assertLeft(max.copy(oid = Some("moikka")), validationMsg("moikka"))
    assertLeft(max.copy(kielivalinta = Seq()), MissingKielivalinta)
    assertLeft(max.copy(nimi = Map(Fi -> "nimi")), invalidKielistetty("nimi", Seq(Sv)))
    assertLeft(max.copy(nimi = Map(Fi -> "nimi", Sv -> "")), invalidKielistetty("nimi", Seq(Sv)))
    assertLeft(max.copy(muokkaaja = "moikka"), validationMsg("moikka"))
  }

  it should "pass imcomplete haku if not julkaistu" in {
    assertRight(min)
  }

  it should "fail if haku oid is invalid" in {
    assertLeft(min.copy(oid = Some("1.2.3")), validationMsg("1.2.3"))
  }

  it should "fail if julkaistu haku is invalid" in {
    assertLeft(max.copy(hakutapaKoodiUri = None), missingMsg("hakutapaKoodiUri"))
    assertLeft(max.copy(hakutapaKoodiUri = Some("korppi")), validationMsg("korppi"))
    assertLeft(max.copy(alkamiskausiKoodiUri = Some("tintti")), validationMsg("tintti"))
    assertLeft(max.copy(alkamisvuosi = Some("20180")), validationMsg("20180"))
    assertLeft(max.copy(alkamisvuosi = Some("2017")), validationMsg("2017"))
    assertLeft(max.copy(kohdejoukkoKoodiUri = Some("kerttu")), validationMsg("kerttu"))
    assertLeft(max.copy(kohdejoukonTarkenneKoodiUri = Some("tonttu")), validationMsg("tonttu"))
    assertLeft(max.copy(hakulomaketyyppi = None), missingMsg("hakulomaketyyppi"))
    assertLeft(max.copy(hakuajat = List(Ajanjakso(alkaa = TestData.inFuture(90000), paattyy = TestData.inFuture(9000)))), InvalidHakuaika)

  }

  it should "pass valid julkaistu haku" in {
    assertRight(max)
  }

  it should "return multiple error messages" in {
    assertLeft(min.copy(hakutapaKoodiUri = Some("korppi"), alkamisvuosi = Some("2017")),
      List(validationMsg("korppi"), validationMsg("2017")))
  }
}
