package fi.oph.kouta.service.yos

import fi.oph.kouta.service.yos.YosKoulutusAsteLuokka.{ALEMMAT_ASTEET, EI_YOS_KOULUTUSASTETTA, YLEMMAT_JA_ALEMMAT_ASTEET}
import fi.oph.kouta.util.UnitSpec

import java.time.LocalDateTime

class YosPredicateSpec extends UnitSpec {

  // arvot, joilla YOS on voimassa sekä hakuajan että koulutuksen alkamisvuoden perusteella
  private val VoimassaHaunAlkamisaika         = Some(LocalDateTime.parse("2026-08-01T00:00:00"))
  private val VoimassaKoulutuksenAlkamisvuosi = Some("2027")
  private val OrganisaatioOid                 = "123.23.123"

  private val yosHakutoive = YosHakutoive(
    korkeakoulutus = true,
    tutkintoonJohtava = true,
    jatkoTutkinto = false,
    kaksoisTutkinto = false,
    organisaatioJaVanhemmat = List(OrganisaatioOid),
    koulutusAste = ALEMMAT_ASTEET,
    haunAlkamisaika = VoimassaHaunAlkamisaika,
    koulutuksenAlkamisvuosi = VoimassaKoulutuksenAlkamisvuosi
  )

  "kuuluukoHakutoiveYosinPiiriin" should "return true for tutkintoon johtava korkeakoulutus with alemmat asteet" in {
    YosPredicate.kuuluukoHakutoiveYosinPiiriin(yosHakutoive) shouldBe true
  }

  it should "return true for tutkintoon johtava korkeakoulutus with ylemmat ja alemmat asteet" in {
    YosPredicate.kuuluukoHakutoiveYosinPiiriin(yosHakutoive.copy(koulutusAste = YLEMMAT_JA_ALEMMAT_ASTEET)) shouldBe true
  }

  it should "return false when hakutoive is not korkeakoulutus" in {
    YosPredicate.kuuluukoHakutoiveYosinPiiriin(yosHakutoive.copy(korkeakoulutus = false)) shouldBe false
  }

  it should "return false when hakutoive is not tutkintoon johtava" in {
    YosPredicate.kuuluukoHakutoiveYosinPiiriin(yosHakutoive.copy(tutkintoonJohtava = false)) shouldBe false
  }

  it should "return false for jatkotutkinto" in {
    YosPredicate.kuuluukoHakutoiveYosinPiiriin(yosHakutoive.copy(jatkoTutkinto = true)) shouldBe false
  }

  it should "return false for kaksoistutkinto" in {
    YosPredicate.kuuluukoHakutoiveYosinPiiriin(yosHakutoive.copy(kaksoisTutkinto = true)) shouldBe false
  }

  it should "return false when koulutusaste is not a YOS koulutusaste" in {
    YosPredicate.kuuluukoHakutoiveYosinPiiriin(yosHakutoive.copy(koulutusAste = EI_YOS_KOULUTUSASTETTA)) shouldBe false
  }

  it should "return false for Poliisiammattikorkeakoulu" in {
    YosPredicate.kuuluukoHakutoiveYosinPiiriin(
      yosHakutoive.copy(organisaatioJaVanhemmat = List(YosConstants.POLIISI_AMK_OID))
    ) shouldBe false
  }

  it should "return false for Maanpuolustuskorkeakoulu" in {
    YosPredicate.kuuluukoHakutoiveYosinPiiriin(
      yosHakutoive.copy(organisaatioJaVanhemmat = List(OrganisaatioOid, YosConstants.MAANPUOLUSTUS_KK_OID))
    ) shouldBe false
  }

  it should "return false for Högskolan på Åland" in {
    YosPredicate.kuuluukoHakutoiveYosinPiiriin(
      yosHakutoive.copy(organisaatioJaVanhemmat = List(YosConstants.AHVENANMAAN_KK_OID, OrganisaatioOid))
    ) shouldBe false
  }

  it should "return false when haun alkamisaika is before leikkuripäivä" in {
    YosPredicate.kuuluukoHakutoiveYosinPiiriin(
      yosHakutoive.copy(haunAlkamisaika = Some(LocalDateTime.parse("2026-07-31T23:59:59")))
    ) shouldBe false
  }

  it should "return false when haun alkamisaika is not known" in {
    YosPredicate.kuuluukoHakutoiveYosinPiiriin(yosHakutoive.copy(haunAlkamisaika = None)) shouldBe false
  }

  it should "return true when haun alkamisaika is exactly at leikkurihetki" in {
    YosPredicate.kuuluukoHakutoiveYosinPiiriin(
      yosHakutoive.copy(haunAlkamisaika = Some(LocalDateTime.parse("2026-08-01T00:00:00")))
    ) shouldBe true
  }

  it should "return false when koulutuksen alkamisvuosi is before leikkurivuosi" in {
    YosPredicate.kuuluukoHakutoiveYosinPiiriin(yosHakutoive.copy(koulutuksenAlkamisvuosi = Some("2026"))) shouldBe false
  }

  it should "return false when koulutuksen alkamisvuosi is not known" in {
    YosPredicate.kuuluukoHakutoiveYosinPiiriin(yosHakutoive.copy(koulutuksenAlkamisvuosi = None)) shouldBe false
  }

  it should "return true when koulutuksen alkamisvuosi is exactly leikkurivuosi" in {
    YosPredicate.kuuluukoHakutoiveYosinPiiriin(yosHakutoive.copy(koulutuksenAlkamisvuosi = Some("2027"))) shouldBe true
  }

  it should "return true when haun alkamisaika is before leikkuripäivä but voimassaolo is not checked" in {
    YosPredicate.kuuluukoHakutoiveYosinPiiriin(
      yosHakutoive.copy(haunAlkamisaika = Some(LocalDateTime.parse("2026-07-31T23:59:59"))),
      tarkistaVoimassaOlo = false
    ) shouldBe true
  }
}
