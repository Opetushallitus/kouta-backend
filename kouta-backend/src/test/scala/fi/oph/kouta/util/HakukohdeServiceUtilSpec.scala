package fi.oph.kouta.util

import fi.oph.kouta.TestData.{
  AmmKoulutus,
  AmmToteutuksenMetatieto,
  AmmTutkinnonOsaKoulutus,
  JulkaistuAmmToteutus,
  JulkaistuHaku,
  JulkaistuHakukohde,
  TelmaToteutuksenMetatieto,
  ToteutuksenOpetus,
  TuvaToteutuksenMetatieto,
  YoKoulutus
}
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid, ToteutusOid}
import fi.oph.kouta.domain.{
  AlkamiskausiJaVuosi,
  Haku,
  Hakukohde,
  HenkilökohtainenSuunnitelma,
  KoulutuksenAlkamiskausi,
  PaateltyAlkamisajankohta,
  PaateltyAlkamiskausi,
  Tallennettu,
  TarkkaAlkamisajankohta,
  Toteutus,
  TuvaToteutusMetadata,
  YhdenPaikanSaanto
}

import java.time.LocalDateTime

class HakukohdeServiceUtilSpec extends UnitSpec {
  val tuvaToteutuksenMetadata: TuvaToteutusMetadata = TuvaToteutuksenMetatieto

  "getJarjestetaanErityisopetuksena" should "return true for TUVA without erityisopetus" in {
    assert(HakukohdeServiceUtil.getJarjestetaanErityisopetuksena(tuvaToteutuksenMetadata) == true)
  }

  it should "return false for TUVA without erityisopetus" in {
    val tuvaToMetadataWithoutErityisopetus                   = tuvaToteutuksenMetadata.copy(jarjestetaanErityisopetuksena = false)
    assert(HakukohdeServiceUtil.getJarjestetaanErityisopetuksena(tuvaToMetadataWithoutErityisopetus) == false)
  }

  it should "return false for TELMA toteutus" in {
    val telma = TelmaToteutuksenMetatieto
    assert(HakukohdeServiceUtil.getJarjestetaanErityisopetuksena(telma) == false)
  }

  val hakukohdeOid = "1.2.246.562.20.00000000000000000001"
  val hakuOid      = "1.2.246.562.29.00000000000000000001"
  val toteutusOid  = "1.2.246.562.17.00000000000000000001"

  def kausiJaVuosi(kausiUri: String, vuosi: String): KoulutuksenAlkamiskausi =
    KoulutuksenAlkamiskausi(
      alkamiskausityyppi = Some(AlkamiskausiJaVuosi),
      koulutuksenAlkamiskausiKoodiUri = Some(kausiUri),
      koulutuksenAlkamisvuosi = Some(vuosi)
    )

  def tarkkaAjankohta(pvm: LocalDateTime): KoulutuksenAlkamiskausi =
    KoulutuksenAlkamiskausi(
      alkamiskausityyppi = Some(TarkkaAlkamisajankohta),
      koulutuksenAlkamispaivamaara = Some(pvm)
    )

  def hakukohde(alkamiskausi: Option[KoulutuksenAlkamiskausi]): Hakukohde =
    JulkaistuHakukohde.copy(
      oid = Some(HakukohdeOid(hakukohdeOid)),
      metadata = JulkaistuHakukohde.metadata.map(_.copy(koulutuksenAlkamiskausi = alkamiskausi))
    )

  def haku(alkamiskausi: Option[KoulutuksenAlkamiskausi]): Haku =
    JulkaistuHaku.copy(
      oid = Some(HakuOid(hakuOid)),
      metadata = JulkaistuHaku.metadata.map(_.copy(koulutuksenAlkamiskausi = alkamiskausi))
    )

  def toteutus(alkamiskausi: Option[KoulutuksenAlkamiskausi]): Toteutus =
    JulkaistuAmmToteutus.copy(
      oid = Some(ToteutusOid(toteutusOid)),
      metadata = Some(
        AmmToteutuksenMetatieto.copy(opetus = Some(ToteutuksenOpetus.copy(koulutuksenAlkamiskausi = alkamiskausi)))
      )
    )

  "paatteleAlkamiskausi" should "use hakukohde's alkamiskausi when it is defined" in {
    val result = HakukohdeServiceUtil.paatteleAlkamiskausi(
      hakukohde(Some(kausiJaVuosi("kausi_s#1", "2027"))),
      Some(haku(Some(kausiJaVuosi("kausi_k#1", "2028")))),
      Some(toteutus(Some(kausiJaVuosi("kausi_k#1", "2029"))))
    )
    result shouldBe Some(PaateltyAlkamiskausi(AlkamiskausiJaVuosi, "kausi_s#1", "2027", hakukohdeOid))
  }

  it should "fall back to haku's alkamiskausi when hakukohde has none" in {
    val result = HakukohdeServiceUtil.paatteleAlkamiskausi(
      hakukohde(None),
      Some(haku(Some(kausiJaVuosi("kausi_k#1", "2028")))),
      Some(toteutus(Some(kausiJaVuosi("kausi_k#1", "2029"))))
    )
    result shouldBe Some(PaateltyAlkamiskausi(AlkamiskausiJaVuosi, "kausi_k#1", "2028", hakuOid))
  }

  it should "fall back to toteutus' alkamiskausi when hakukohde and haku have none" in {
    val result = HakukohdeServiceUtil.paatteleAlkamiskausi(
      hakukohde(None),
      Some(haku(None)),
      Some(toteutus(Some(kausiJaVuosi("kausi_k#1", "2029"))))
    )
    result shouldBe Some(PaateltyAlkamiskausi(AlkamiskausiJaVuosi, "kausi_k#1", "2029", toteutusOid))
  }

  it should "use toteutus' alkamiskausi when haku is missing" in {
    val result = HakukohdeServiceUtil.paatteleAlkamiskausi(
      hakukohde(None),
      None,
      Some(toteutus(Some(kausiJaVuosi("kausi_s#1", "2029"))))
    )
    result shouldBe Some(PaateltyAlkamiskausi(AlkamiskausiJaVuosi, "kausi_s#1", "2029", toteutusOid))
  }

  it should "infer kevät from tarkka alkamisajankohta before August" in {
    val result = HakukohdeServiceUtil.paatteleAlkamiskausi(
      hakukohde(Some(tarkkaAjankohta(LocalDateTime.of(2027, 7, 31, 23, 59)))),
      None,
      None
    )
    result shouldBe Some(PaateltyAlkamiskausi(TarkkaAlkamisajankohta, "kausi_k#1", "2027", hakukohdeOid))
  }

  it should "infer syksy from tarkka alkamisajankohta in August or later" in {
    val result = HakukohdeServiceUtil.paatteleAlkamiskausi(
      hakukohde(Some(tarkkaAjankohta(LocalDateTime.of(2027, 8, 1, 0, 0)))),
      None,
      None
    )
    result shouldBe Some(PaateltyAlkamiskausi(TarkkaAlkamisajankohta, "kausi_s#1", "2027", hakukohdeOid))
  }

  it should "skip henkilökohtainen suunnitelma and fall back to the next source" in {
    val result = HakukohdeServiceUtil.paatteleAlkamiskausi(
      hakukohde(Some(KoulutuksenAlkamiskausi(alkamiskausityyppi = Some(HenkilökohtainenSuunnitelma)))),
      Some(haku(Some(kausiJaVuosi("kausi_k#1", "2028")))),
      Some(toteutus(Some(kausiJaVuosi("kausi_s#1", "2029"))))
    )
    result shouldBe Some(PaateltyAlkamiskausi(AlkamiskausiJaVuosi, "kausi_k#1", "2028", hakuOid))
  }

  it should "skip incomplete alkamiskausi and fall back to the next source" in {
    val incomplete = KoulutuksenAlkamiskausi(
      alkamiskausityyppi = Some(AlkamiskausiJaVuosi),
      koulutuksenAlkamiskausiKoodiUri = Some("kausi_k#1"),
      koulutuksenAlkamisvuosi = None
    )
    val result = HakukohdeServiceUtil.paatteleAlkamiskausi(
      hakukohde(Some(incomplete)),
      Some(haku(None)),
      Some(toteutus(Some(tarkkaAjankohta(LocalDateTime.of(2029, 9, 1, 12, 0)))))
    )
    result shouldBe Some(PaateltyAlkamiskausi(TarkkaAlkamisajankohta, "kausi_s#1", "2029", toteutusOid))
  }

  it should "return None when no source has a usable alkamiskausi" in {
    val result = HakukohdeServiceUtil.paatteleAlkamiskausi(hakukohde(None), Some(haku(None)), Some(toteutus(None)))
    result shouldBe None
  }

  "paatteleAlkamisajankohta" should "use hakukohde's tarkka alkamisajankohta as such" in {
    val result = HakukohdeServiceUtil.paatteleAlkamisajankohta(
      hakukohde(Some(tarkkaAjankohta(LocalDateTime.of(2027, 3, 24, 9, 49)))),
      Some(haku(Some(kausiJaVuosi("kausi_k#1", "2028")))),
      None
    )
    result shouldBe Some(PaateltyAlkamisajankohta(Some(TarkkaAlkamisajankohta), Some("2027-03-24T09:49"), hakukohdeOid, false))
  }

  it should "use the first day of kevät from alkamiskausi ja -vuosi" in {
    val result = HakukohdeServiceUtil.paatteleAlkamisajankohta(hakukohde(Some(kausiJaVuosi("kausi_k#1", "2027"))), None, None)
    result shouldBe Some(PaateltyAlkamisajankohta(Some(AlkamiskausiJaVuosi), Some("2027-01-01"), hakukohdeOid, false))
  }

  it should "use the first day of syksy from alkamiskausi ja -vuosi" in {
    val result = HakukohdeServiceUtil.paatteleAlkamisajankohta(hakukohde(Some(kausiJaVuosi("kausi_s#1", "2027"))), None, None)
    result shouldBe Some(PaateltyAlkamisajankohta(Some(AlkamiskausiJaVuosi), Some("2027-08-01"), hakukohdeOid, false))
  }

  it should "use haku's alkamisajankohta before toteutus' when hakukohde has henkilökohtainen suunnitelma" in {
    val result = HakukohdeServiceUtil.paatteleAlkamisajankohta(
      hakukohde(Some(KoulutuksenAlkamiskausi(alkamiskausityyppi = Some(HenkilökohtainenSuunnitelma)))),
      Some(haku(Some(kausiJaVuosi("kausi_s#1", "2028")))),
      Some(toteutus(Some(kausiJaVuosi("kausi_k#1", "2029"))))
    )
    result shouldBe Some(PaateltyAlkamisajankohta(Some(AlkamiskausiJaVuosi), Some("2028-08-01"), hakuOid, true))
  }

  it should "fall back to toteutus' alkamisajankohta when hakukohde and haku have none" in {
    val result = HakukohdeServiceUtil.paatteleAlkamisajankohta(
      hakukohde(None),
      Some(haku(None)),
      Some(toteutus(Some(tarkkaAjankohta(LocalDateTime.of(2029, 9, 1, 12, 0)))))
    )
    result shouldBe Some(PaateltyAlkamisajankohta(Some(TarkkaAlkamisajankohta), Some("2029-09-01T12:00"), toteutusOid, false))
  }

  it should "take henkilökohtainen suunnitelma only from hakukohde" in {
    val result = HakukohdeServiceUtil.paatteleAlkamisajankohta(
      hakukohde(None),
      Some(haku(Some(KoulutuksenAlkamiskausi(alkamiskausityyppi = Some(HenkilökohtainenSuunnitelma))))),
      None
    )
    result shouldBe Some(PaateltyAlkamisajankohta(None, None, hakukohdeOid, false))
  }

  it should "return henkilökohtainen suunnitelma without pvm when no source has a usable alkamisajankohta" in {
    val result = HakukohdeServiceUtil.paatteleAlkamisajankohta(
      hakukohde(Some(KoulutuksenAlkamiskausi(alkamiskausityyppi = Some(HenkilökohtainenSuunnitelma)))),
      Some(haku(None)),
      Some(toteutus(None))
    )
    result shouldBe Some(PaateltyAlkamisajankohta(None, None, hakukohdeOid, true))
  }

  "paatteleJohtaaTutkintoon" should "return true when koulutus johtaa tutkintoon" in {
    HakukohdeServiceUtil.paatteleJohtaaTutkintoon(Some(AmmKoulutus)) shouldBe Some(true)
  }

  it should "return false when koulutus does not johtaa tutkintoon" in {
    HakukohdeServiceUtil.paatteleJohtaaTutkintoon(Some(AmmTutkinnonOsaKoulutus)) shouldBe Some(false)
  }

  it should "return None when koulutus is missing" in {
    HakukohdeServiceUtil.paatteleJohtaaTutkintoon(None) shouldBe None
  }

  def ypsHaku(kohdejoukonTarkenne: Option[String]): Haku =
    haku(None).copy(kohdejoukonTarkenneKoodiUri = kohdejoukonTarkenne)

  def alkamiskausi(kausiUri: String, vuosi: String): Option[PaateltyAlkamiskausi] =
    Some(PaateltyAlkamiskausi(AlkamiskausiJaVuosi, kausiUri, vuosi, hakukohdeOid))

  def paatteleYps(
      ypsHakukohde: Hakukohde = hakukohde(None),
      ypsHakuOpt: Option[Haku] = Some(ypsHaku(None)),
      koulutus: Option[fi.oph.kouta.domain.Koulutus] = Some(YoKoulutus),
      paateltyAlkamiskausi: Option[PaateltyAlkamiskausi] = alkamiskausi("kausi_s#1", "2027")
  ): YhdenPaikanSaanto =
    HakukohdeServiceUtil.paatteleYhdenPaikanSaanto(ypsHakukohde, ypsHakuOpt, koulutus, paateltyAlkamiskausi)

  def eiYps(syy: String): YhdenPaikanSaanto = YhdenPaikanSaanto(voimassa = false, syy = syy)

  "paatteleYhdenPaikanSaanto" should "be voimassa for julkaistu tutkintoon johtava korkeakoulutus without kohdejoukon tarkenne" in {
    paatteleYps() shouldBe HakukohdeServiceUtil.YpsVoimassa
  }

  it should "be voimassa when haun kohdejoukon tarkenne is blank" in {
    paatteleYps(ypsHakuOpt = Some(ypsHaku(Some(" ")))) shouldBe HakukohdeServiceUtil.YpsVoimassa
  }

  it should "be voimassa when alkamiskausi is missing" in {
    paatteleYps(paateltyAlkamiskausi = None) shouldBe HakukohdeServiceUtil.YpsVoimassa
  }

  it should "not be voimassa when haku is missing" in {
    paatteleYps(ypsHakuOpt = None) shouldBe eiYps("Hakukohteen hakua ei löytynyt")
  }

  it should "not be voimassa when haku is luonnos" in {
    paatteleYps(ypsHakuOpt = Some(ypsHaku(None).copy(tila = Tallennettu))) shouldBe eiYps("Haku on luonnos tilassa")
  }

  it should "not be voimassa when hakukohde is luonnos" in {
    paatteleYps(ypsHakukohde = hakukohde(None).copy(tila = Tallennettu)) shouldBe eiYps("Hakukohde on luonnos tilassa")
  }

  it should "not be voimassa when koulutus is not korkeakoulutus" in {
    paatteleYps(koulutus = Some(AmmKoulutus)) shouldBe eiYps("Ei korkeakoulutus koulutusta")
  }

  it should "not be voimassa when koulutus is missing" in {
    paatteleYps(koulutus = None) shouldBe eiYps("Ei korkeakoulutus koulutusta")
  }

  it should "not be voimassa when koulutus does not johtaa tutkintoon" in {
    paatteleYps(koulutus = Some(YoKoulutus.copy(johtaaTutkintoon = false))) shouldBe
      eiYps("Ei tutkintoon johtavaa koulutusta")
  }

  it should "not be voimassa when alkamiskausi is before autumn 2016" in {
    paatteleYps(paateltyAlkamiskausi = alkamiskausi("kausi_s#1", "2015")) shouldBe
      eiYps("Koulutuksen alkamiskausi on ennen syksyä 2016")
    paatteleYps(paateltyAlkamiskausi = alkamiskausi("kausi_k#1", "2016")) shouldBe
      eiYps("Koulutuksen alkamiskausi on ennen syksyä 2016")
  }

  it should "be voimassa when alkamiskausi is autumn 2016" in {
    paatteleYps(paateltyAlkamiskausi = alkamiskausi("kausi_s#1", "2016")) shouldBe HakukohdeServiceUtil.YpsVoimassa
  }

  it should "be voimassa for jatkotutkintohaku, Erasmus Mundus and hakemusmaksullinen kaksoistutkinto" in {
    for (tarkenne <- Seq("haunkohdejoukontarkenne_3#1", "haunkohdejoukontarkenne_010#1", "haunkohdejoukontarkenne_11#1")) {
      withClue(tarkenne) {
        paatteleYps(ypsHakuOpt = Some(ypsHaku(Some(tarkenne)))) shouldBe HakukohdeServiceUtil.YpsVoimassa
      }
    }
  }

  it should "not be voimassa for other kohdejoukon tarkenne, e.g. Högskolan på Åland" in {
    paatteleYps(ypsHakuOpt = Some(ypsHaku(Some("haunkohdejoukontarkenne_12#1")))) shouldBe
      eiYps("Haun kohdejoukon tarkenne on haunkohdejoukontarkenne_12#1")
    paatteleYps(ypsHakuOpt = Some(ypsHaku(Some("haunkohdejoukontarkenne_1#1")))) shouldBe
      eiYps("Haun kohdejoukon tarkenne on haunkohdejoukontarkenne_1#1")
  }
}
