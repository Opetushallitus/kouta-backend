package fi.oph.kouta.validation

import java.time.Instant

import fi.oph.kouta.TestData._
import fi.oph.kouta.domain.{Fi, Haku, Ajanjakso, Sv}

class HakuValidationSpec extends BaseValidationSpec[Haku] with ValidationMessages {

  val max = JulkaistuHaku
  val min = MinHaku

  it should "fail if perustiedot is invalid" in {
    assertLeft(max.copy(oid = Some("moikka")), invalidOidMsg("moikka"))
    assertLeft(max.copy(kielivalinta = Seq()), MissingKielivalinta)
    assertLeft(max.copy(nimi = Map(Fi -> "nimi")), invalidKielistetty("nimi", Seq(Sv)))
    assertLeft(max.copy(nimi = Map(Fi -> "nimi", Sv -> "")), invalidKielistetty("nimi", Seq(Sv)))
    assertLeft(max.copy(muokkaaja = "moikka"), invalidOidMsg("moikka"))
  }

  it should "pass imcomplete haku if not julkaistu" in {
    assertRight(min)
  }

  it should "fail if haku oid is invalid" in {
    assertLeft(min.copy(oid = Some("1.2.3")), invalidHakuOidMsg("1.2.3"))
  }

  it should "fail if julkaistu haku is invalid" in {
    assertLeft(max.copy(hakutapaKoodiUri = Some("korppi")), invalidHakutapaKoodi("korppi"))
    assertLeft(max.copy(alkamiskausiKoodiUri = Some("tintti")), invalidKausiKoodi("tintti"))
    assertLeft(max.copy(alkamisvuosi = Some("20180")),invalidAlkamisvuosi("20180"))
    assertLeft(max.copy(alkamisvuosi = Some("2017")),invalidAlkamisvuosi("2017"))
    assertLeft(max.copy(kohdejoukkoKoodiUri = Some("kerttu")), invalidKohdejoukkoKoodi("kerttu"))
    assertLeft(max.copy(kohdejoukonTarkenneKoodiUri = Some("tonttu")), invalidKohdejoukonTarkenneKoodi("tonttu"))
    assertLeft(max.copy(hakulomaketyyppi = None), MissingHakulomaketyyppi)
    assertLeft(max.copy(hakuajat = List(Ajanjakso(alkaa = Instant.now().plusSeconds(90000), paattyy = Instant.now.plusSeconds(9000)))), InvalidHakuaika)

  }

  it should "pass valid julkaistu haku" in {
    assertRight(max)
  }
}
