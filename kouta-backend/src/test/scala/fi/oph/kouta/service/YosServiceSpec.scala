package fi.oph.kouta.service

import fi.oph.kouta.TestData.{JulkaistuHaku, JulkaistuHakukohde}
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid, OrganisaatioOid}
import fi.oph.kouta.domain.{
  Ajanjakso,
  AlkamiskausiJaVuosi,
  Fi,
  Haku,
  Hakukohde,
  OrganisaatioServiceOrg,
  PaateltyAlkamiskausi,
  Tallennettu
}
import fi.oph.kouta.service.yos.YosConstants
import org.mockito.scalatest.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.LocalDateTime

class YosServiceSpec extends AnyFlatSpec with Matchers with BeforeAndAfterEach with MockitoSugar {
  private val organisaatioService = mock[OrganisaatioServiceImpl]
  private val koodistoService     = mock[KoodistoService]
  private val yosService          = new YosService(organisaatioService, koodistoService)

  private val HakuOidStr         = "1.2.246.562.29.00000000000000074021"
  private val HakukohdeOidStr    = "1.2.246.562.20.00000000000000078520"
  private val OrganisaatioOidStr = "1.2.246.562.10.2014040310315946122056"
  private val ParentOidStr       = "1.2.246.562.10.00000000001"

  private val HakuJokaKuuluuYosPiiriin: Haku = JulkaistuHaku.copy(
    oid = Some(HakuOid(HakuOidStr)),
    kohdejoukkoKoodiUri = Some("haunkohdejoukko_12#1"),
    kohdejoukonTarkenneKoodiUri = None,
    hakuajat = List(
      Ajanjakso(LocalDateTime.parse("2026-08-01T08:00:00"), Some(LocalDateTime.parse("2026-08-30T15:00:00")))
    )
  )

  private val HakutoiveJokaKuuluuYosPiiriin: Hakukohde = JulkaistuHakukohde.copy(
    oid = Some(HakukohdeOid(HakukohdeOidStr)),
    hakuOid = HakuOid(HakuOidStr),
    jarjestyspaikkaOid = Some(OrganisaatioOid(OrganisaatioOidStr))
  )

  private val AlkamiskausiJokaKuuluuYosPiiriin =
    Some(PaateltyAlkamiskausi(AlkamiskausiJaVuosi, "kausi_k#1", "2027", HakukohdeOidStr))
  private val YlempiKoulutusaste = Seq("kansallinenkoulutusluokitus2016koulutusastetaso2_72")

  private def organisaatio(parentOids: String*): OrganisaatioServiceOrg =
    OrganisaatioServiceOrg(
      oid = OrganisaatioOidStr,
      parentOidPath = parentOids.mkString("|", "|", "|"),
      nimi = Map(Fi -> "Tinasepän kuparipaja"),
      status = "AKTIIVINEN"
    )

  private def mockOrganisaatio(parentOids: String*): Unit =
    when(organisaatioService.getOrganisaatio(OrganisaatioOid(OrganisaatioOidStr)))
      .thenReturn(Right(organisaatio(parentOids: _*)))

  private def kuuluukoYosPiiriin(
      haku: Option[Haku] = Some(HakuJokaKuuluuYosPiiriin),
      hakukohde: Hakukohde = HakutoiveJokaKuuluuYosPiiriin,
      paateltyAlkamiskausi: Option[PaateltyAlkamiskausi] = AlkamiskausiJokaKuuluuYosPiiriin,
      johtaaTutkintoon: Option[Boolean] = Some(true),
      koulutusasteKoodiUrit: Seq[String] = YlempiKoulutusaste
  ): Boolean =
    yosService.kuuluukoHakutoiveYossinpiiriin(
      haku,
      hakukohde,
      paateltyAlkamiskausi,
      johtaaTutkintoon,
      koulutusasteKoodiUrit
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(organisaatioService)
    mockOrganisaatio(ParentOidStr)
  }

  "kuuluukoHakutoiveYossinpiiriin" should "return true for hakutoive that kuuluu YOS piiriin" in {
    kuuluukoYosPiiriin() shouldBe true
  }

  it should "return true for alempi korkeakoulututkinto" in {
    kuuluukoYosPiiriin(koulutusasteKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusastetaso2_62")) shouldBe true
  }

  it should "return false when haku is not found" in {
    kuuluukoYosPiiriin(haku = None) shouldBe false
  }

  it should "return false when hakukohde has no järjestyspaikka" in {
    kuuluukoYosPiiriin(hakukohde = HakutoiveJokaKuuluuYosPiiriin.copy(jarjestyspaikkaOid = None)) shouldBe false
  }

  it should "return false when organisaatio is not found" in {
    when(organisaatioService.getOrganisaatio(OrganisaatioOid(OrganisaatioOidStr)))
      .thenReturn(Left(new RuntimeException("not found")))
    kuuluukoYosPiiriin() shouldBe false
  }

  it should "return false when haku is luonnos" in {
    kuuluukoYosPiiriin(haku = Some(HakuJokaKuuluuYosPiiriin.copy(tila = Tallennettu))) shouldBe false
    verify(organisaatioService, never).getOrganisaatio(any[OrganisaatioOid])
  }

  it should "return false when hakukohde is luonnos" in {
    kuuluukoYosPiiriin(hakukohde = HakutoiveJokaKuuluuYosPiiriin.copy(tila = Tallennettu)) shouldBe false
    verify(organisaatioService, never).getOrganisaatio(any[OrganisaatioOid])
  }

  // Yksityiskohtainen leikkuripäivä/-vuosirajojen testaus tehdään YosPredicateSpec:ssä. Tässä varmistetaan
  // vain että YosService kytkee haku- ja hakukohdetiedot oikein YosHakutoiveelle.
  it should "return false when YOS is not voimassa by haun hakuaika" in {
    kuuluukoYosPiiriin(haku =
      Some(
        HakuJokaKuuluuYosPiiriin.copy(hakuajat =
          List(
            Ajanjakso(LocalDateTime.parse("2026-07-31T23:59:59"), Some(LocalDateTime.parse("2026-08-30T15:00:00")))
          )
        )
      )
    ) shouldBe false
  }

  it should "use the earliest hakuaika as haun alkamisaika" in {
    // aikaisin hakuajoista (2026-07-01) alittaa leikkurin, vaikka toinen hakuajoista täyttäisikin sen
    kuuluukoYosPiiriin(haku =
      Some(
        HakuJokaKuuluuYosPiiriin.copy(hakuajat =
          List(
            Ajanjakso(LocalDateTime.parse("2026-08-01T00:00:00")),
            Ajanjakso(LocalDateTime.parse("2026-07-01T00:00:00"))
          )
        )
      )
    ) shouldBe false
  }

  it should "return false when haku has no hakuajat" in {
    kuuluukoYosPiiriin(haku = Some(HakuJokaKuuluuYosPiiriin.copy(hakuajat = List()))) shouldBe false
  }

  it should "return false when YOS is not voimassa by koulutuksen alkamisvuosi" in {
    kuuluukoYosPiiriin(paateltyAlkamiskausi =
      Some(PaateltyAlkamiskausi(AlkamiskausiJaVuosi, "kausi_k#1", "2026", HakukohdeOidStr))
    ) shouldBe false
  }

  it should "return false when hakukohde has no päätelty alkamiskausi" in {
    kuuluukoYosPiiriin(paateltyAlkamiskausi = None) shouldBe false
  }

  it should "return false for Poliisiammattikorkeakoulu" in {
    mockOrganisaatio(YosConstants.POLIISI_AMK_OID)
    kuuluukoYosPiiriin() shouldBe false
  }

  it should "return false for Högskolan på Åland" in {
    mockOrganisaatio(YosConstants.AHVENANMAAN_KK_OID)
    kuuluukoYosPiiriin() shouldBe false
  }

  it should "return false for Maanpuolustuskorkeakoulu" in {
    mockOrganisaatio(YosConstants.MAANPUOLUSTUS_KK_OID)
    kuuluukoYosPiiriin() shouldBe false
  }

  it should "return false when haku is not korkeakouluhaku" in {
    kuuluukoYosPiiriin(haku = Some(HakuJokaKuuluuYosPiiriin.copy(kohdejoukkoKoodiUri = Some("haunkohdejoukko_11#1")))) shouldBe false
  }

  it should "return false for Erasmus Mundus tai kaksoistutkinto haku" in {
    kuuluukoYosPiiriin(haku =
      Some(HakuJokaKuuluuYosPiiriin.copy(kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_010#1")))
    ) shouldBe false
  }

  it should "return false for hakemusmaksullinen kaksoistutkinto haku" in {
    kuuluukoYosPiiriin(haku =
      Some(HakuJokaKuuluuYosPiiriin.copy(kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_11#1")))
    ) shouldBe false
  }

  it should "return false for jatkotutkinto haku" in {
    kuuluukoYosPiiriin(haku =
      Some(HakuJokaKuuluuYosPiiriin.copy(kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_3#1")))
    ) shouldBe false
  }

  it should "return false when koulutusaste does not kuulu YOS piiriin" in {
    kuuluukoYosPiiriin(koulutusasteKoodiUrit = Seq("kansallinenkoulutusluokitus2016koulutusastetaso2_82")) shouldBe false
  }

  it should "return false when koulutusaste is not known" in {
    kuuluukoYosPiiriin(koulutusasteKoodiUrit = Seq()) shouldBe false
  }

  it should "return false when koulutus does not johda tutkintoon" in {
    kuuluukoYosPiiriin(johtaaTutkintoon = Some(false)) shouldBe false
  }

  it should "return false when tutkintoon johtavuus is not known" in {
    kuuluukoYosPiiriin(johtaaTutkintoon = None) shouldBe false
  }
}
