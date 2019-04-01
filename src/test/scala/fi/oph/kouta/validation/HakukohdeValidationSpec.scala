package fi.oph.kouta.validation

import java.time.Instant

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._

class HakukohdeValidationSpec extends BaseValidationSpec[Hakukohde] with Validations {

  val max = JulkaistuHakukohde
  val min = MinHakukohde

  it should "fail if perustiedot is invalid" in {
    assertLeft(max.copy(oid = Some(HakukohdeOid("moikka"))), validationMsg("moikka"))
    assertLeft(max.copy(kielivalinta = Seq()), MissingKielivalinta)
    assertLeft(max.copy(nimi = Map(Fi -> "nimi")), invalidKielistetty("nimi", Seq(Sv)))
    assertLeft(max.copy(nimi = Map(Fi -> "nimi", Sv -> "")), invalidKielistetty("nimi", Seq(Sv)))
    assertLeft(max.copy(muokkaaja = UserOid("moikka")), validationMsg("moikka"))
  }

  it should "pass imcomplete hakukohde if not julkaistu" in {
    assertRight(min)
  }

  it should "fail if hakukohde oid is invalid" in {
    assertLeft(min.copy(oid = Some(HakukohdeOid("1.2.3"))), validationMsg("1.2.3"))
  }

  it should "fail if julkaistu hakukohde is invalid" in {
    assertLeft(max.copy(alkamiskausiKoodiUri = Some("tintti")), validationMsg("tintti"))
    assertLeft(max.copy(alkamisvuosi = Some("20180")), validationMsg("20180"))
    assertLeft(max.copy(alkamisvuosi = Some("2017")), validationMsg("2017"))
    assertLeft(max.copy(pohjakoulutusvaatimusKoodiUrit = Seq("hessu")), validationMsg("hessu"))
    assertLeft(max.copy(hakuajat = List(Ajanjakso(alkaa = TestData.inFuture(90000), paattyy = TestData.inFuture(9000)))), InvalidHakuaika)
  }

  it should "pass valid julkaistu hakukohde" in {
    assertRight(max)
  }

  it should "return multiple error messages" in {
    assertLeft(max.copy(aloituspaikat = Some(-1), liitteetOnkoSamaToimitusaika = Some(true), liitteidenToimitusaika = None),
      List(notNegativeMsg("aloituspaikat"), missingMsg("liitteiden toimitusaika")))
  }
}
