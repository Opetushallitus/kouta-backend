package fi.oph.kouta.integration

import fi.oph.kouta.TestData
import fi.oph.kouta.TestData.{JulkaistuHaku, JulkaistuHakukohde}
import fi.oph.kouta.domain.Julkaistu
import fi.oph.kouta.domain.oid.{HakuOid, ToteutusOid}
import fi.oph.kouta.service.HakukohdeServiceValidation
import org.scalatest.flatspec.AnyFlatSpec

class HakukohdeServiceValidationSpec extends AnyFlatSpec {
  val anyHakukohde = JulkaistuHakukohde
  val anyHaku = JulkaistuHaku
  val anyDep = (
    Julkaistu,
    None,
    None,
    Seq()
  )

  "Hakukohde validation" should "pass for Oph-virkailija even though hakukohteen liittämisen takaraja has expired" in {
    val hakukohde = anyHakukohde.copy(
      toteutusOid = ToteutusOid("1.2.246.562.17.1111111111"),
      hakuOid = HakuOid("1.2.246.562.29.2222222222"),
    )
    val haku = Some(anyHaku.copy(hakukohteenLiittamisenTakaraja = Some(TestData.inPast(100))))
    val isOphPaakayttaja = true
    val deps = Map(
      "1.2.246.562.17.1111111111" -> anyDep,
      "1.2.246.562.29.2222222222" -> anyDep
    )

    val errors = HakukohdeServiceValidation.validate(
      hakukohde,
      haku,
      isOphPaakayttaja,
      deps,
      "put"
    )
    assert(errors.isEmpty)
  }

  it should "pass validation for Oph-virkailija even though hakukohteen muokkaamisen takaraja has expired" in {
    val hakukohde = anyHakukohde.copy(
      toteutusOid = ToteutusOid("1.2.246.562.17.1111111111"),
      hakuOid = HakuOid("1.2.246.562.29.2222222222"),
    )
    val haku = Some(anyHaku.copy(hakukohteenMuokkaamisenTakaraja = Some(TestData.inPast(100))))
    val isOphPaakayttaja = true
    val deps = Map(
      "1.2.246.562.17.1111111111" -> anyDep,
      "1.2.246.562.29.2222222222" -> anyDep
    )

    val errors = HakukohdeServiceValidation.validate(
      hakukohde,
      haku,
      isOphPaakayttaja,
      deps,
      "update"
    )
    assert(errors.isEmpty)
  }

  it should "not pass validation for oppilaitosvirkailija when hakukohteen liittämisen takaraja has expired" in {
    val hakukohde = anyHakukohde.copy(
      toteutusOid = ToteutusOid("1.2.246.562.17.1111111111"),
      hakuOid = HakuOid("1.2.246.562.29.2222222222"),
    )
    val haku = Some(anyHaku.copy(hakukohteenLiittamisenTakaraja = Some(TestData.inPast(100))))
    val isOphPaakayttaja = false
    val deps = Map(
      "1.2.246.562.17.1111111111" -> anyDep,
      "1.2.246.562.29.2222222222" -> anyDep
    )

    val errors = HakukohdeServiceValidation.validate(
      hakukohde,
      haku,
      isOphPaakayttaja,
      deps,
      "put"
    )
    assert(!errors.isEmpty)
  }

  it should "not pass validation for oppilaitosvirkailija when hakukohteen muokkaamisen takaraja has expired" in {
    val hakukohde = anyHakukohde.copy(
      toteutusOid = ToteutusOid("1.2.246.562.17.1111111111"),
      hakuOid = HakuOid("1.2.246.562.29.2222222222"),
    )
    val haku = Some(anyHaku.copy(hakukohteenMuokkaamisenTakaraja = Some(TestData.inPast(100))))
    val isOphPaakayttaja = false
    val deps = Map(
      "1.2.246.562.17.1111111111" -> anyDep,
      "1.2.246.562.29.2222222222" -> anyDep
    )

    val errors = HakukohdeServiceValidation.validate(
      hakukohde,
      haku,
      isOphPaakayttaja,
      deps,
      "update"
    )
    assert(!errors.isEmpty)
  }

  it should "have one error for expired hakukohteen muokkaamisen takaraja when updating" in {
    val hakukohde = anyHakukohde.copy(
      toteutusOid = ToteutusOid("1.2.246.562.17.1111111111"),
      hakuOid = HakuOid("1.2.246.562.29.2222222222"),
    )
    val haku = Some(anyHaku.copy(hakukohteenMuokkaamisenTakaraja = Some(TestData.inPast(100)), hakukohteenLiittamisenTakaraja = Some(TestData.inPast(100))))
    val isOphPaakayttaja = false
    val deps = Map(
      "1.2.246.562.17.1111111111" -> anyDep,
      "1.2.246.562.29.2222222222" -> anyDep
    )

    val errors = HakukohdeServiceValidation.validate(
      hakukohde,
      haku,
      isOphPaakayttaja,
      deps,
      "update"
    )
    assert(errors.length == 1 && errors.head.path == "hakukohteenMuokkaamisenTakaraja" && errors.head.errorType == "pastDateMsg")
  }

  it should "pass validation for oppilaitosvirkailija when updating if hakukohteen liittamisen takaraja has expired" in {
    val hakukohde = anyHakukohde.copy(
      toteutusOid = ToteutusOid("1.2.246.562.17.1111111111"),
      hakuOid = HakuOid("1.2.246.562.29.2222222222"),
    )
    val haku = Some(anyHaku.copy( hakukohteenLiittamisenTakaraja = Some(TestData.inPast(100))))
    val isOphPaakayttaja = false
    val deps = Map(
      "1.2.246.562.17.1111111111" -> anyDep,
      "1.2.246.562.29.2222222222" -> anyDep
    )

    val errors = HakukohdeServiceValidation.validate(
      hakukohde,
      haku,
      isOphPaakayttaja,
      deps,
      "update"
    )
    assert(errors.isEmpty)
  }
}