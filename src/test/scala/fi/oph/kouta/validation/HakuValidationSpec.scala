package fi.oph.kouta.validation

import java.util.UUID

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.validation.Validations._

class HakuValidationSpec extends BaseValidationSpec[Haku] {

  val max = JulkaistuHaku
  val min = MinHaku

  it should "fail if perustiedot is invalid" in {
    failsValidation(max.copy(oid = Some(HakuOid("moikka"))), validationMsg("moikka"))
    failsValidation(max.copy(kielivalinta = Seq()), MissingKielivalinta)
    failsValidation(max.copy(nimi = Map()), invalidKielistetty("nimi", Seq(Fi, Sv)))
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
    failsValidation(max.copy(alkamiskausiKoodiUri = None, hakutapaKoodiUri = Some("hakutapa_01#1")), missingMsg("alkamiskausiKoodiUri"))
    failsValidation(max.copy(alkamisvuosi = None, hakutapaKoodiUri = Some("hakutapa_01#1")), missingMsg("alkamisvuosi"))
    failsValidation(max.copy(alkamisvuosi = Some("20180")), validationMsg("20180"))
    failsValidation(max.copy(alkamisvuosi = Some("2017")), validationMsg("2017"))
    failsValidation(max.copy(kohdejoukkoKoodiUri = None), missingMsg("kohdejoukkoKoodiUri"))
    failsValidation(max.copy(kohdejoukkoKoodiUri = Some("kerttu")), validationMsg("kerttu"))
    failsValidation(max.copy(kohdejoukonTarkenneKoodiUri = Some("tonttu")), validationMsg("tonttu"))
    failsValidation(max.copy(hakulomaketyyppi = None), missingMsg("hakulomaketyyppi"))
    val ajanjakso = Ajanjakso(alkaa = TestData.inFuture(90000), paattyy = TestData.inFuture(9000))
    failsValidation(max.copy(hakuajat = List(ajanjakso)), invalidAjanjakso(ajanjakso, "Hakuaika"))
  }

  it should "fail if metadata is invalid" in {
    val ajanjakso = Ajanjakso(alkaa = TestData.inFuture(90000), paattyy = TestData.inFuture(9000))
    val metadata = max.metadata.get.copy(tulevaisuudenAikataulu = List(ajanjakso))
    failsValidation(max.copy(metadata = Some(metadata)), invalidAjanjakso(ajanjakso, "tulevaisuudenAikataulu"))
  }

  it should "fail if yhteyshenkilot has other info, but no name" in {
    val metadata = max.metadata.get.copy(yhteyshenkilot = Seq(Yhteyshenkilo(sahkoposti = Map(Fi -> "sahkoposti", Sv -> "email"))))
    failsValidation(max.copy(metadata = Some(metadata)), invalidKielistetty("nimi", Seq(Fi, Sv)))
  }


    it should "fail if hakulomake is invalid" in {
    failsValidation(max.copy(hakulomaketyyppi = Some(Ataru), hakulomakeAtaruId = None), missingMsg("hakulomakeAtaruId"))
    failsValidation(max.copy(hakulomaketyyppi = Some(MuuHakulomake), hakulomakeLinkki = Map(Fi -> "http://url.fi")), invalidKielistetty("hakulomakeLinkki", Seq(Sv)))
    failsValidation(max.copy(hakulomaketyyppi = Some(MuuHakulomake), hakulomakeLinkki = Map()), invalidKielistetty("hakulomakeLinkki", Seq(Fi, Sv)))
    failsValidation(max.copy(hakulomaketyyppi = Some(MuuHakulomake), hakulomakeLinkki = Map(Fi -> "http://url.fi", Sv -> "virhe")), invalidUrl("virhe"))
    failsValidation(max.copy(hakulomaketyyppi = Some(EiSähköistä), hakulomakeKuvaus = Map(Fi -> "kuvaus")), invalidKielistetty("hakulomakeKuvaus", Seq(Sv)))
  }

  it should "pass valid hakulomake" in {
    passesValidation(max.copy(hakulomaketyyppi = Some(Ataru), hakulomakeAtaruId = Some(UUID.randomUUID())))
    passesValidation(max.copy(hakulomaketyyppi = Some(MuuHakulomake), hakulomakeLinkki = Map(Fi -> "http://url.fi", Sv -> "http://url.se")))
    passesValidation(max.copy(hakulomaketyyppi = Some(EiSähköistä), hakulomakeKuvaus = Map()))
  }

  it should "pass valid julkaistu haku" in {
    passesValidation(max)
  }

  it should "return multiple error messages" in {
    failsValidation(min.copy(hakutapaKoodiUri = Some("korppi"), alkamisvuosi = Some("2017")),
      validationMsg("korppi"), validationMsg("2017"))
  }
}
