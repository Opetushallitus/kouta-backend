package fi.oph.kouta.integration

import fi.oph.kouta.TestData._
import fi.oph.kouta.TestOids.{AmmOid, LonelyOid, LukioOid, OtherOid}
import fi.oph.kouta.client.{HakuKoodiClient, KoulutusKoodiClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.repository.{HakukohdeDAO, KoulutusDAO, SorakuvausDAO}
import fi.oph.kouta.service.{OrganisaatioService, ToteutusServiceValidation}
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{BaseValidationSpec, ErrorMessage, ValidationError}
import org.scalatest.Assertion

import java.time.{Instant, LocalDateTime}
import java.util.UUID

class ToteutusServiceValidationSpec extends BaseValidationSpec[Toteutus] {
  val koulutusKoodiClient = mock[KoulutusKoodiClient]
  val organisaatioService = mock[OrganisaatioService]
  val hakuKoodiClient     = mock[HakuKoodiClient]
  val koulutusDao         = mock[KoulutusDAO]
  val hakukohdeDao        = mock[HakukohdeDAO]
  val sorakuvausDao       = mock[SorakuvausDAO]

  val lukioToteutus           = LukioToteutus.copy(koulutusOid = KoulutusOid("1.2.246.562.13.125"))
  val ammTutkinnonOsaToteutus = AmmTutkinnonOsaToteutus.copy(koulutusOid = KoulutusOid("1.2.246.562.13.124"))
  val ammMuuToteutus          = AmmMuuToteutus.copy(koulutusOid = KoulutusOid("1.2.246.562.13.130"))
  val yoToteutus              = JulkaistuYoToteutus.copy(koulutusOid = KoulutusOid("1.2.246.562.13.131"))
  val kkOpintojaksoToteutus   = JulkaistuKkOpintojaksoToteutus.copy(koulutusOid = KoulutusOid("1.2.246.562.13.132"))
  val kkOpintokokonaisuusToteutus =
    JulkaistuKkOpintokokonaisuusToteutus.copy(koulutusOid = KoulutusOid("1.2.246.562.13.133"))
  val kkOpintokokonaisuusKoulutus = KkOpintokokonaisuusKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.133")))

  val sorakuvausId  = UUID.randomUUID()
  val sorakuvausId2 = UUID.randomUUID()
  val sorakuvausId3 = UUID.randomUUID()
  val sorakuvausId4 = UUID.randomUUID()
  val sorakuvausId5 = UUID.randomUUID()
  val sorakuvausId6 = UUID.randomUUID()

  val toteutusOid  = ToteutusOid("1.2.246.562.17.00000000000000000123")
  val toteutusOid2 = ToteutusOid("1.2.246.562.17.00000000000000000124")

  val koulutusOid1 = KoulutusOid("1.2.246.562.13.00000000000000000997")
  val koulutusOid2 = KoulutusOid("1.2.246.562.13.00000000000000000998")
  val invalidKoulutusOid = KoulutusOid("1.2.246.562.13.00000000000000000999")

  val invalidKoulutuksetKoodiUri = Seq("koulutus_XXX#1")
  val validKoulutuksetKoodiUri = Seq("koulutus_371101#1")

  private def ammToteutusWithOpetusParameters(
      opetuskieliKoodiUrit: Seq[String] = Seq("oppilaitoksenopetuskieli_1#1"),
      opetusaikaKoodiUrit: Seq[String] = Seq("opetusaikakk_1#1"),
      opetustapaKoodiUrit: Seq[String] = Seq("opetuspaikkakk_1#1", "opetuspaikkakk_2#1"),
      apuraha: Option[Apuraha] = Some(
        Apuraha(Some(100), Some(200), Some(Euro), Map(Fi -> "apurahakuvaus fi", Sv -> "apurahakuvaus sv"))
      ),
      lisatiedot: Seq[Lisatieto] = Seq(Lisatieto1)
  ): Toteutus = {
    val opetus: Opetus = ToteutuksenOpetus.copy(
      opetuskieliKoodiUrit = opetuskieliKoodiUrit,
      opetusaikaKoodiUrit = opetusaikaKoodiUrit,
      opetustapaKoodiUrit = opetustapaKoodiUrit,
      apuraha = apuraha,
      lisatiedot = lisatiedot
    )
    JulkaistuAmmToteutus.copy(metadata = Some(AmmToteutuksenMetatieto.copy(opetus = Some(opetus))))
  }

  private def ammToteutusWithKoulutuksenAlkamiskausi(
      startDate: Option[LocalDateTime],
      endDate: Option[LocalDateTime],
      tyyppi: Option[Alkamiskausityyppi] = Some(AlkamiskausiJaVuosi),
      lisatiedot: Kielistetty = Map(Fi -> "Jotakin lisätietoa", Sv -> "Jotakin lisätietoa sv"),
      koodiUri: Option[String] = Some("kausi_k#1"),
      startYear: Option[String] = None
  ): Toteutus = {
    val metadataBase = JulkaistuAmmToteutus.metadata.get.asInstanceOf[AmmatillinenToteutusMetadata]
    val opetusBase   = metadataBase.opetus.get
    val alkamisvuosi: Option[String] = startYear match {
      case Some(startYear) => Some(startYear)
      case None            => if (startDate.isDefined) Some(startDate.get.getYear.toString) else None
      case _               => None
    }
    JulkaistuAmmToteutus.copy(
      metadata = Some(
        metadataBase.copy(
          opetus = Some(
            opetusBase.copy(
              koulutuksenAlkamiskausi = Some(
                KoulutuksenAlkamiskausi(
                  tyyppi,
                  lisatiedot,
                  startDate,
                  endDate,
                  koodiUri,
                  alkamisvuosi
                )
              )
            )
          )
        )
      )
    )
  }

  private def lukioToteutusWithSpecificParameters(
      painotukset: Seq[LukiolinjaTieto] = List(
        LukiolinjaTieto(koodiUri = "lukiopainotukset_1#1", Map(Fi -> "painotus 1 kuvaus", Sv -> "painotus 1 kuvaus sv"))
      ),
      erityisetKoulutustehtavat: Seq[LukiolinjaTieto] = List(
        LukiolinjaTieto(
          koodiUri = "lukiolinjaterityinenkoulutustehtava_1#1",
          Map(Fi -> "erityinen koulutustehtava 1 kuvaus", Sv -> "erityinen koulutustehtava 1 kuvaus sv")
        )
      ),
      diplomit: Seq[LukiodiplomiTieto] = List(
        LukiodiplomiTieto(
          koodiUri = "moduulikoodistolops2021_kald3#1",
          linkki = Map(Fi -> "http://linkki.fi", Sv -> "http://link.se"),
          linkinAltTeksti = Map(Fi -> "Suomeksi", Sv -> "På svenska")
        )
      )
  ): Toteutus = {
    val metadata = LukioToteutuksenMetatieto.copy(
      painotukset = painotukset,
      erityisetKoulutustehtavat = erityisetKoulutustehtavat,
      diplomit = diplomit
    )
    lukioToteutus.copy(metadata = Some(metadata))
  }

  private def lukioToteutusWithKieliParameters(
      A1Kielet: Seq[String] = Seq("kieli_EN#1"),
      A2Kielet: Seq[String] = Seq("kieli_DE#1"),
      B1Kielet: Seq[String] = Seq("kieli_SV#1"),
      B2Kielet: Seq[String] = Seq("kieli_FR#1"),
      B3Kielet: Seq[String] = Seq("kieli_ES#1"),
      aidinkielet: Seq[String] = Seq("kieli_FI#1"),
      muutKielet: Seq[String] = Seq("kieli_ET#1")
  ): Toteutus = {
    val kielivalikoima = LukioToteutuksenMetatieto.kielivalikoima.get.copy(
      A1Kielet,
      A2Kielet,
      B1Kielet,
      B2Kielet,
      B3Kielet,
      aidinkielet,
      muutKielet
    )
    lukioToteutus.copy(metadata = Some(LukioToteutuksenMetatieto.copy(kielivalikoima = Some(kielivalikoima))))
  }

  override val validator =
    new ToteutusServiceValidation(
      koulutusKoodiClient,
      organisaatioService,
      hakuKoodiClient,
      koulutusDao,
      hakukohdeDao,
      sorakuvausDao
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    // yleiset
    when(organisaatioService.findUnknownOrganisaatioOidsFromHierarkia(Set(OtherOid, AmmOid)))
      .thenAnswer(Set[OrganisaatioOid]())
    when(organisaatioService.findUnknownOrganisaatioOidsFromHierarkia(Set(LonelyOid, LukioOid)))
      .thenAnswer(Set(LonelyOid))
    when(koulutusKoodiClient.opetusKieliKoodiUriExists("oppilaitoksenopetuskieli_1#1")).thenAnswer(true)
    when(koulutusKoodiClient.opetusAikaKoodiUriExists("opetusaikakk_1#1")).thenAnswer(true)
    when(koulutusKoodiClient.opetusTapaKoodiUriExists("opetuspaikkakk_1#1")).thenAnswer(true)
    when(koulutusKoodiClient.opetusTapaKoodiUriExists("opetuspaikkakk_2#1")).thenAnswer(true)
    when(koulutusKoodiClient.lisatiedotOtsikkoKoodiUriExists("koulutuksenlisatiedot_03#1")).thenAnswer(true)
    when(hakuKoodiClient.kausiKoodiUriExists("kausi_k#1")).thenAnswer(true)

    // tietokantakyselyt
    when(koulutusDao.get(KoulutusOid("1.2.246.562.13.123"))).thenAnswer(Some(AmmKoulutus.copy(tila = Julkaistu)))
    when(koulutusDao.get(KoulutusOid("1.2.246.562.13.124")))
      .thenAnswer(Some(AmmTutkinnonOsaKoulutus.copy(tila = Julkaistu)))
    when(koulutusDao.get(KoulutusOid("1.2.246.562.13.125"))).thenAnswer(Some(LukioKoulutus.copy(tila = Julkaistu)))
    when(koulutusDao.get(KoulutusOid("1.2.246.562.13.126"))).thenAnswer(None)
    when(koulutusDao.get(KoulutusOid("1.2.246.562.13.127"))).thenAnswer(Some(AmmKoulutus.copy(tila = Tallennettu)))
    when(koulutusDao.get(KoulutusOid("1.2.246.562.13.128"))).thenAnswer(Some(AmmKoulutus.copy(tila = Poistettu)))
    when(koulutusDao.get(KoulutusOid("1.2.246.562.13.130"))).thenAnswer(Some(AmmMuuKoulutus.copy(tila = Julkaistu)))
    when(koulutusDao.get(KoulutusOid("1.2.246.562.13.131"))).thenAnswer(Some(YoKoulutus.copy(tila = Julkaistu)))
    when(koulutusDao.get(KoulutusOid("1.2.246.562.13.132")))
      .thenAnswer(Some(KkOpintojaksoKoulutus.copy(tila = Julkaistu)))
    when(koulutusDao.get(kkOpintokokonaisuusKoulutus.oid.get)).thenAnswer(Some(kkOpintokokonaisuusKoulutus))
    when(koulutusDao.get(koulutusOid1, TilaFilter.all()))
      .thenAnswer(Some((AmmKoulutus.copy(oid = Some(koulutusOid1),
        koulutuksetKoodiUri = invalidKoulutuksetKoodiUri),
        Instant.now())))
    when(koulutusDao.get(koulutusOid2, TilaFilter.all()))
      .thenAnswer(Some((AmmKoulutus.copy(oid = Some(koulutusOid2),
        koulutuksetKoodiUri = validKoulutuksetKoodiUri),
        Instant.now())))
    when(koulutusDao.get(invalidKoulutusOid, TilaFilter.all())).thenAnswer(None)
    when(koulutusDao.get(koulutusOid1)).thenAnswer((Some(Julkaistu), Some(Amm)))
    when(koulutusDao.get(koulutusOid2)).thenAnswer((Some(Julkaistu), Some(Amm)))
    when(koulutusDao.get(invalidKoulutusOid)).thenAnswer((None, None))

    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId))
      .thenAnswer(Some(Julkaistu), Some(Amm), Some(Seq("koulutus_371101#1")))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId2)).thenAnswer((None, None, None))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId3))
      .thenAnswer((Some(Tallennettu), Some(Amm), Some(Seq("koulutus_371101#1"))))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId4))
      .thenAnswer((Some(Poistettu), Some(Amm), Some(Seq("koulutus_371101#1"))))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId5)).thenAnswer((Some(Julkaistu), Some(Yo), None))
    when(hakukohdeDao.listByToteutusOid(toteutusOid, TilaFilter.onlyOlemassaolevat()))
      .thenAnswer(Seq[HakukohdeListItem]())
    when(hakukohdeDao.listByToteutusOid(toteutusOid2, TilaFilter.onlyOlemassaolevat()))
      .thenAnswer(Seq(MinHakukohdeListItem.copy(toteutusOid = toteutusOid2)))

    // ammatillinen
    when(koulutusKoodiClient.osaamisalaKoodiUriExists("osaamisala_0001#1")).thenAnswer(true)
    // lukio
    when(koulutusKoodiClient.lukioPainotusKoodiUriExists("lukiopainotukset_1#1")).thenAnswer(true)
    when(
      koulutusKoodiClient.lukioErityinenKoulutustehtavaKoodiUriExists("lukiolinjaterityinenkoulutustehtava_1#1")
    ).thenAnswer(true)
    when(koulutusKoodiClient.lukioDiplomiKoodiUriExists("moduulikoodistolops2021_kald3#1")).thenAnswer(true)
    when(koulutusKoodiClient.isKoulutusAmmatillinenPerustutkinto(invalidKoulutuksetKoodiUri)).thenAnswer(false)
    when(koulutusKoodiClient.isKoulutusAmmatillinenPerustutkinto(validKoulutuksetKoodiUri)).thenAnswer(true)

    when(hakuKoodiClient.kieliKoodiUriExists("kieli_EN#1")).thenAnswer(true)
    when(hakuKoodiClient.kieliKoodiUriExists("kieli_DE#1")).thenAnswer(true)
    when(hakuKoodiClient.kieliKoodiUriExists("kieli_SV#1")).thenAnswer(true)
    when(hakuKoodiClient.kieliKoodiUriExists("kieli_FR#1")).thenAnswer(true)
    when(hakuKoodiClient.kieliKoodiUriExists("kieli_ES#1")).thenAnswer(true)
    when(hakuKoodiClient.kieliKoodiUriExists("kieli_FI#1")).thenAnswer(true)
    when(hakuKoodiClient.kieliKoodiUriExists("kieli_ET#1")).thenAnswer(true)
  }

  "Validation" should "succeed when new valid toteutus" in {
    passValidation(JulkaistuAmmToteutus)
  }

  it should "succeed when new incomplete luonnos" in {
    passValidation(MinToteutus)
  }

  it should "succeed when sorakuvausId given for suitable type of toteutus" in {
    passValidation(ammTutkinnonOsaToteutus.copy(sorakuvausId = Some(sorakuvausId)))
  }

  it should "succeed when new valid tutkintoon johtamaton toteutus" in {
    passValidation(ammMuuToteutus)
  }

  it should "succeed when new valid korkeakoulu toteutus" in {
    passValidation(yoToteutus)
  }

  it should "succeed when new valid lukio-toteutus" in {
    passValidation(lukioToteutus)
  }

  it should "succeed when new valid kk-opintojakso toteutus" in {
    passValidation(kkOpintojaksoToteutus)
  }

  it should "fail if perustiedot is invalid" in {
    failValidation(lukioToteutus.copy(oid = Some(ToteutusOid("1.2.3"))), "oid", validationMsg("1.2.3"))
    failsValidation(lukioToteutus.copy(kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(lukioToteutus.copy(nimi = Map(Fi -> "nimi")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(lukioToteutus.copy(nimi = Map(Fi -> "nimi", Sv -> "")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(
      lukioToteutus.copy(organisaatioOid = OrganisaatioOid("1.2.3")),
      "organisaatioOid",
      validationMsg("1.2.3")
    )
    failsValidation(lukioToteutus.copy(organisaatioOid = OrganisaatioOid("")), "organisaatioOid", validationMsg(""))
    failValidation(lukioToteutus.copy(koulutusOid = KoulutusOid("puppu")), "koulutusOid", validationMsg("puppu"))
    failValidation(lukioToteutus.copy(teemakuva = Some("puppu")), "teemakuva", invalidUrl("puppu"))
  }

  it should "fail if metadata missing from julkaistu totetutus" in {
    failValidation(
      JulkaistuAmmToteutus.copy(metadata = None),
      "metadata",
      missingMsg
    )
  }

  it should "fail if invalid tarjoaja OIDs" in {
    failValidation(
      lukioToteutus.copy(tarjoajat = List(OrganisaatioOid("puppu"), LonelyOid, LukioOid)),
      Seq(
        ValidationError("tarjoajat[0]", validationMsg("puppu")),
        ValidationError("tarjoajat[1]", unknownTarjoajaOid(LonelyOid))
      )
    )
  }

  it should "fail if koulutus doesn't exist" in {
    val oid = KoulutusOid("1.2.246.562.13.126")
    failValidation(JulkaistuAmmToteutus.copy(koulutusOid = oid), "koulutusOid", nonExistent("Koulutusta", oid))
  }

  it should "fail to store julkaistu toteutus if koulutus is not yet julkaistu" in {
    val oid = KoulutusOid("1.2.246.562.13.127")
    failValidation(
      JulkaistuAmmToteutus.copy(koulutusOid = oid),
      "tila",
      notYetJulkaistu("Koulutusta", oid)
    )
  }

  it should "fail to store julkaistu toteutus if koulutus poistettu" in {
    val oid = KoulutusOid("1.2.246.562.13.128")
    failValidation(JulkaistuAmmToteutus.copy(koulutusOid = oid), "koulutusOid", nonExistent("Koulutusta", oid))
  }

  it should "fail to store toteutus if koulutustyyppi doesn't match koulutus koulutustyyppi" in {
    val oid = KoulutusOid("1.2.246.562.13.125")
    failValidation(JulkaistuAmmToteutus.copy(koulutusOid = oid), "metadata.tyyppi", tyyppiMismatch("koulutuksen", oid))
  }

  private def failSorakuvausValidation(toteutus: Toteutus, koulutusOidStr: String): Assertion = {
    val koulutusOid = KoulutusOid(koulutusOidStr)
    when(koulutusDao.get(koulutusOid))
      .thenAnswer(Some(AmmKoulutus.copy(tila = Julkaistu, koulutustyyppi = toteutus.metadata.get.tyyppi)))
    val testedToteutus = toteutus.copy(koulutusOid = koulutusOid, sorakuvausId = Some(sorakuvausId))
    failValidation(testedToteutus, "sorakuvausId", notMissingMsg(Some(sorakuvausId)))
  }

  it should "fail if sorakuvaus given for non supported koulutustyyppi" in {
    failSorakuvausValidation(JulkaistuAmmToteutus, "1.2.246.562.13.600")
    failSorakuvausValidation(AmmMuuToteutus, "1.2.246.562.13.601")
    failSorakuvausValidation(JulkaistuYoToteutus, "1.2.246.562.13.602")
    failSorakuvausValidation(JulkaistuAmkToteutus, "1.2.246.562.13.603")
    failSorakuvausValidation(AmmOpettajaToteutus, "1.2.246.562.13.604")
    failSorakuvausValidation(LukioToteutus, "1.2.246.562.13.605")
    failSorakuvausValidation(TuvaToteutus, "1.2.246.562.13.606")
    failSorakuvausValidation(TelmaToteutus, "1.2.246.562.13.607")
    failSorakuvausValidation(VapaaSivistystyoOpistovuosiToteutus, "1.2.246.562.13.608")
    failSorakuvausValidation(VapaaSivistystyoMuuToteutus, "1.2.246.562.13.609")
  }

  it should "fail if sorakuvaus doesn't exist" in {
    failValidation(
      ammTutkinnonOsaToteutus.copy(sorakuvausId = Some(sorakuvausId2)),
      "sorakuvausId",
      nonExistent("Sorakuvausta", sorakuvausId2)
    )
  }

  it should "fail to store julkaistu toteutus if sorakuvaus is not yet julkaistu" in {
    failValidation(
      ammTutkinnonOsaToteutus.copy(sorakuvausId = Some(sorakuvausId3)),
      "tila",
      notYetJulkaistu("Sorakuvausta", sorakuvausId3)
    )
  }

  it should "fail to store julkaistu koulutus if sorakuvaus poistettu" in {
    failValidation(
      ammTutkinnonOsaToteutus.copy(sorakuvausId = Some(sorakuvausId4)),
      "sorakuvausId",
      nonExistent("Sorakuvausta", sorakuvausId4)
    )
  }

  it should "fail to store koulutus if koulutustyyppi doesn't match sorakuvaus koulutustyyppi" in {
    failValidation(
      ammTutkinnonOsaToteutus.copy(sorakuvausId = Some(sorakuvausId5)),
      "metadata.tyyppi",
      tyyppiMismatch("sorakuvauksen", sorakuvausId5)
    )
  }

  it should "fail if opetus missing from julkaistu totetutus" in {
    failValidation(
      JulkaistuAmmToteutus.copy(metadata = Some(AmmToteutuksenMetatieto.copy(opetus = None))),
      "metadata.opetus",
      missingMsg
    )
  }

  it should "fail if invalid opetus parameters" in {
    failValidation(
      JulkaistuAmmToteutus.copy(metadata =
        Some(
          AmmatillinenToteutusMetadata(opetus =
            Some(
              ToteutuksenOpetus.copy(
                maksunMaara = Some(-1),
                suunniteltuKestoVuodet = Some(-1),
                suunniteltuKestoKuukaudet = Some(-1),
                maksullisuustyyppi = None
              )
            )
          )
        )
      ),
      Seq(
        ValidationError("metadata.opetus.maksunMaara", notNegativeMsg),
        ValidationError("metadata.opetus.suunniteltuKestoVuodet", notNegativeMsg),
        ValidationError("metadata.opetus.suunniteltuKestoKuukaudet", notNegativeMsg),
        ValidationError("metadata.opetus.maksullisuustyyppi", missingMsg)
      )
    )
  }

  it should "fail if missing opetus parameters" in {
    val incompleteKielistetty = Map(Fi -> "vain suomeksi", Sv -> "")
    failValidation(
      JulkaistuAmmToteutus.copy(metadata =
        Some(
          AmmatillinenToteutusMetadata(opetus =
            Some(
              Opetus(
                maksullisuustyyppi = Some(Maksullinen),
                onkoApuraha = true,
                opetuskieletKuvaus = incompleteKielistetty,
                opetusaikaKuvaus = incompleteKielistetty,
                opetustapaKuvaus = incompleteKielistetty,
                maksullisuusKuvaus = incompleteKielistetty,
                suunniteltuKestoKuvaus = incompleteKielistetty
              )
            )
          )
        )
      ),
      Seq(
        ValidationError("metadata.opetus.opetuskieliKoodiUrit", missingMsg),
        ValidationError("metadata.opetus.opetusaikaKoodiUrit", missingMsg),
        ValidationError("metadata.opetus.opetustapaKoodiUrit", missingMsg),
        ValidationError("metadata.opetus.apuraha", missingMsg),
        ValidationError("metadata.opetus.opetuskieletKuvaus", invalidKielistetty(Seq(Sv))),
        ValidationError("metadata.opetus.opetusaikaKuvaus", invalidKielistetty(Seq(Sv))),
        ValidationError("metadata.opetus.opetustapaKuvaus", invalidKielistetty(Seq(Sv))),
        ValidationError("metadata.opetus.maksullisuusKuvaus", invalidKielistetty(Seq(Sv))),
        ValidationError("metadata.opetus.maksunMaara", missingMsg),
        ValidationError("metadata.opetus.suunniteltuKestoKuvaus", invalidKielistetty(Seq(Sv)))
      )
    )
  }

  it should "fail if invalid opetuskieliKoodiUri" in {
    failValidation(
      ammToteutusWithOpetusParameters(opetuskieliKoodiUrit = Seq("puppu", "oppilaitoksenopetuskieli_2#1")),
      Seq(
        ValidationError("metadata.opetus.opetuskieliKoodiUrit[0]", validationMsg("puppu")),
        ValidationError(
          "metadata.opetus.opetuskieliKoodiUrit[1]",
          invalidOpetusKieliKoodiUri("oppilaitoksenopetuskieli_2#1")
        )
      )
    )
  }

  it should "fail if invalid opetusaikaKoodiUri" in {
    failValidation(
      ammToteutusWithOpetusParameters(opetusaikaKoodiUrit = Seq("puppu", "opetusaikakk_2#1")),
      Seq(
        ValidationError("metadata.opetus.opetusaikaKoodiUrit[0]", validationMsg("puppu")),
        ValidationError("metadata.opetus.opetusaikaKoodiUrit[1]", invalidOpetusAikaKoodiUri("opetusaikakk_2#1"))
      )
    )
  }

  it should "fail if invalid opetustapaKoodiUri" in {
    failValidation(
      ammToteutusWithOpetusParameters(opetustapaKoodiUrit = Seq("puppu", "opetuspaikkakk_3#1")),
      Seq(
        ValidationError("metadata.opetus.opetustapaKoodiUrit[0]", validationMsg("puppu")),
        ValidationError("metadata.opetus.opetustapaKoodiUrit[1]", invalidOpetusTapaKoodiUri("opetuspaikkakk_3#1"))
      )
    )
  }

  it should "fail if invalid opetus-lisatieto" in {
    val validLisatieto = Map(Fi -> "lisatieto fi", Sv -> "lisatieto sv")
    failValidation(
      ammToteutusWithOpetusParameters(lisatiedot =
        Seq(
          Lisatieto("koulutuksenlisatiedot_03#1", Map(Fi -> "vain suomeksi")),
          Lisatieto("puppu", validLisatieto),
          Lisatieto("koulutuksenlisatiedot_04#1", validLisatieto)
        )
      ),
      Seq(
        ValidationError("metadata.opetus.lisatiedot[0].teksti", invalidKielistetty(Seq(Sv))),
        ValidationError("metadata.opetus.lisatiedot[1].otsikkoKoodiUri", validationMsg("puppu")),
        ValidationError(
          "metadata.opetus.lisatiedot[2].otsikkoKoodiUri",
          invalidOpetusLisatietoOtsikkoKoodiuri("koulutuksenlisatiedot_04#1")
        )
      )
    )
  }

  it should "fail if invalid apuraha" in {
    val validKuvaus = Map(Fi -> "kuvaus fi", Sv -> "kuvaus sv")
    failValidation(
      ammToteutusWithOpetusParameters(apuraha = Some(Apuraha(Some(-100), Some(-200), Some(Euro), validKuvaus))),
      Seq(
        ValidationError("metadata.opetus.apuraha.min", minmaxMsg(-100, -200)),
        ValidationError("metadata.opetus.apuraha.min", notNegativeMsg),
        ValidationError("metadata.opetus.apuraha.max", notNegativeMsg)
      )
    )
    failValidation(
      ammToteutusWithOpetusParameters(apuraha = Some(Apuraha(Some(50), Some(200), Some(Prosentti), validKuvaus))),
      "metadata.opetus.apuraha.max",
      lessOrEqualMsg(200, 100)
    )
    failValidation(
      ammToteutusWithOpetusParameters(apuraha = Some(Apuraha(None, None, None, Map(Fi -> "vain suomeksi")))),
      Seq(
        ValidationError("metadata.opetus.apuraha.kuvaus", invalidKielistetty(Seq(Sv))),
        ValidationError("metadata.opetus.apuraha.min", missingMsg),
        ValidationError("metadata.opetus.apuraha.max", missingMsg)
      )
    )
  }

  it should "fail if invalid koulutuksenAlkamiskausi" in {
    failValidation(
      ammToteutusWithKoulutuksenAlkamiskausi(
        Some(inFuture(2000)),
        Some(inFuture(1000))
      ).copy(tila = Tallennettu),
      "metadata.opetus.koulutuksenAlkamiskausi.koulutuksenAlkamispaivamaara",
      InvalidKoulutuspaivamaarat
    )
  }

  it should "fail if metadata kuvaus missing for koulutustyypit requiring kuvaus" in {
    failValidation(
      ammMuuToteutus.copy(metadata =
        Some(
          ammMuuToteutus.metadata.get
            .asInstanceOf[AmmatillinenMuuToteutusMetadata]
            .copy(kuvaus = Map(Fi -> "", Sv -> ""))
        )
      ),
      "metadata.kuvaus",
      invalidKielistetty(Seq(Fi, Sv))
    )
  }

  it should "fail if metadata kuvaus missing for koulutustyypit not requiring kuvaus" in {
    passValidation(
      lukioToteutus.copy(metadata =
        Some(
          lukioToteutus.metadata.get.asInstanceOf[LukioToteutusMetadata].copy(kuvaus = Map(Fi -> "", Sv -> ""))
        )
      )
    )
    failValidation(
      lukioToteutus.copy(metadata =
        Some(
          lukioToteutus.metadata.get
            .asInstanceOf[LukioToteutusMetadata]
            .copy(kuvaus = Map(Fi -> "vain suomeksi", Sv -> ""))
        )
      ),
      "metadata.kuvaus",
      invalidKielistetty(Seq(Sv))
    )
  }

  "Ammatillinen toteutus validation" should "fail if invalid osaamisala for luonnos" in {
    failValidation(
      JulkaistuAmmToteutus.copy(
        tila = Tallennettu,
        metadata = Some(
          AmmatillinenToteutusMetadata(osaamisalat =
            List(
              AmmatillinenOsaamisala(
                koodiUri = "puppu",
                linkki = Map(Fi -> "http://www.linkki.fi", Sv -> "http://www.linkki.sv")
              ),
              AmmatillinenOsaamisala(
                koodiUri = "osaamisala_0002",
                linkki = Map(Fi -> "http://www.linkki.fi", Sv -> "http://www.linkki.sv")
              ),
              AmmatillinenOsaamisala(
                koodiUri = "osaamisala_0001#1",
                linkki = Map(Fi -> "puppu fi", Sv -> "puppu sv")
              )
            )
          )
        )
      ),
      Seq(
        ValidationError("metadata.osaamisalat[0].koodiUri", validationMsg("puppu")),
        ValidationError("metadata.osaamisalat[1].koodiUri", invalidOsaamisalaKoodiUri("osaamisala_0002")),
        ValidationError("metadata.osaamisalat[2].linkki.fi", invalidUrl("puppu fi")),
        ValidationError("metadata.osaamisalat[2].linkki.sv", invalidUrl("puppu sv"))
      )
    )
  }

  "Ammatillinen toteutus validation" should "fail if invalid osaamisala for julkaistu toteutus" in {
    failValidation(
      JulkaistuAmmToteutus.copy(
        metadata = Some(
          AmmToteutuksenMetatieto.copy(osaamisalat =
            List(
              AmmatillinenOsaamisala(
                "osaamisala_0001#1",
                Map(Fi -> "http://www.linkki.fi"),
                Map(Fi -> "vain suomeksi", Sv -> "")
              )
            )
          )
        )
      ),
      Seq(
        ValidationError("metadata.osaamisalat[0].linkki", invalidKielistetty(Seq(Sv))),
        ValidationError("metadata.osaamisalat[0].otsikko", invalidKielistetty(Seq(Sv)))
      )
    )
  }

  "Tutkintoon johtamaton toteutus validation" should "fail if invalid values for luonnos" in {
    val ajanjakso = Ajanjakso(alkaa = LocalDateTime.now(), paattyy = Some(LocalDateTime.now().minusDays(1)))
    failValidation(
      ammMuuToteutus.copy(
        tila = Tallennettu,
        metadata = Some(
          AmmatillinenMuuToteutusMetadata(
            hakulomakeLinkki = Map(Fi -> "puppu fi", Sv -> "puppu sv"),
            hakuaika = Some(ajanjakso),
            aloituspaikat = Some(-1)
          )
        )
      ),
      Seq(
        ValidationError("metadata.hakulomakeLinkki.fi", invalidUrl("puppu fi")),
        ValidationError("metadata.hakulomakeLinkki.sv", invalidUrl("puppu sv")),
        ValidationError("metadata.hakuaika", invalidAjanjaksoMsg(ajanjakso)),
        ValidationError("metadata.aloituspaikat", notNegativeMsg)
      )
    )
  }

  "Tutkintoon johtamaton toteutus validation" should "fail if missing values for julkaistu totetutus" in {
    val metadataBase = ammMuuToteutus.metadata.get.asInstanceOf[AmmatillinenMuuToteutusMetadata]
    failValidation(
      ammMuuToteutus.copy(metadata =
        Some(
          metadataBase.copy(
            hakutermi = None,
            hakulomaketyyppi = Some(MuuHakulomake),
            lisatietoaHakeutumisesta = Map(Fi -> "", Sv -> ""),
            hakulomakeLinkki = Map(),
            lisatietoaValintaperusteista = Map(Fi -> "vain suomeksi", Sv -> ""),
            hakuaika = None
          )
        )
      ),
      Seq(
        ValidationError("metadata.hakutermi", missingMsg),
        ValidationError("metadata.lisatietoaHakeutumisesta", invalidKielistetty(Seq(Fi, Sv))),
        ValidationError("metadata.hakulomakeLinkki", invalidKielistetty(Seq(Fi, Sv))),
        ValidationError("metadata.lisatietoaValintaperusteista", invalidKielistetty(Seq(Sv))),
        ValidationError("metadata.hakuaika", missingMsg)
      )
    )
    failValidation(
      ammMuuToteutus.copy(metadata =
        Some(
          metadataBase.copy(
            hakulomaketyyppi = Some(EiSähköistä),
            lisatietoaHakeutumisesta = Map(Fi -> "", Sv -> "")
          )
        )
      ),
      "metadata.lisatietoaHakeutumisesta",
      invalidKielistetty(Seq(Fi, Sv))
    )
    failValidation(
      ammMuuToteutus.copy(metadata =
        Some(
          metadataBase.copy(
            hakulomaketyyppi = None
          )
        )
      ),
      "metadata.hakulomaketyyppi",
      missingMsg
    )
  }

  "Kk-opintojakso validation" should "fail if ammattinimikkeet given" in {
    failValidation(
      kkOpintojaksoToteutus.copy(metadata =
        Some(KkOpintojaksoToteutuksenMetatieto.copy(ammattinimikkeet = List(Keyword(Fi, "nimike"))))
      ),
      "metadata.ammattinimikkeet",
      notEmptyMsg
    )
  }

  "Kk-opintokokonaisuus validation" should "fail if ammattinimikkeet given" in {
    failValidation(
      kkOpintokokonaisuusToteutus.copy(metadata =
        Some(KkOpintokokonaisuusToteutuksenMetatieto.copy(ammattinimikkeet = List(Keyword(Fi, "nimike"))))
      ),
      "metadata.ammattinimikkeet",
      notEmptyMsg
    )
  }

  it should "succeed when new valid kk-opintokokonaisuustoteutus" in {
    passValidation(kkOpintokokonaisuusToteutus)
  }

  it should "fail if opintojen laajuus not in the range given for koulutus" in {
    val toteutuksenLaajuus = Some(25.0)
    failValidation(
      kkOpintokokonaisuusToteutus.copy(metadata =
        Some(KkOpintokokonaisuusToteutuksenMetatieto.copy(opintojenLaajuusNumero = toteutuksenLaajuus))
      ),
      "metadata.opintojenLaajuusNumero",
      notInTheRangeMsg(
        KkOpintokokonaisuusKoulutuksenMetatieto.opintojenLaajuusNumeroMin,
        KkOpintokokonaisuusKoulutuksenMetatieto.opintojenLaajuusNumeroMax,
        toteutuksenLaajuus
      )
    )
  }

  it should "fail if opintojen laajuusyksikko is different in koulutus" in {
    val koulutusLaajuusyksikko = kkOpintokokonaisuusKoulutus.metadata.get
      .asInstanceOf[KkOpintokokonaisuusKoulutusMetadata]
      .opintojenLaajuusyksikkoKoodiUri
    val toteutusLaajuusyksikko = Some("opintojenlaajuusyksikko_5#1")
    failValidation(
      kkOpintokokonaisuusToteutus.copy(metadata =
        Some(KkOpintokokonaisuusToteutuksenMetatieto.copy(opintojenLaajuusyksikkoKoodiUri = toteutusLaajuusyksikko))
      ),
      "metadata.opintojenLaajuusyksikkoKoodiUri",
      invalidToteutusOpintojenLaajuusyksikkoIntegrity(
        koulutusLaajuusyksikko,
        toteutusLaajuusyksikko
      )
    )
  }

  it should "pass with any opintojenLaajuusyksikko when related koulutus opintojenlaajuusyksikko is not defined" in {
    val opintokokonaisuusKoulutusOid = KoulutusOid("1.2.246.562.13.134")
    when(koulutusDao.get(opintokokonaisuusKoulutusOid))
      .thenAnswer(
        Some(
          kkOpintokokonaisuusKoulutus
            .copy(metadata = Some(KkOpintokokonaisuusKoulutuksenMetatieto.copy(opintojenLaajuusyksikkoKoodiUri = None)))
        )
      )
    passValidation(kkOpintokokonaisuusToteutus.copy(koulutusOid = opintokokonaisuusKoulutusOid))
  }

  it should "pass with any otherwise valid opintojenLaajuusNumero when related koulutus opintojenlaajuus range is not defined" in {
    val opintokokonaisuusKoulutusOid = KoulutusOid("1.2.246.562.13.134")
    when(koulutusDao.get(opintokokonaisuusKoulutusOid))
      .thenAnswer(
        Some(
          kkOpintokokonaisuusKoulutus
            .copy(metadata =
              Some(
                KkOpintokokonaisuusKoulutuksenMetatieto.copy(
                  opintojenLaajuusNumeroMin = None,
                  opintojenLaajuusNumeroMax = None
                )
              )
            )
        )
      )
    passValidation(kkOpintokokonaisuusToteutus.copy(koulutusOid = opintokokonaisuusKoulutusOid))
  }

  it should "pass when opintojenLaajuusNumero within related koulutus partial range (only min or max defined)" in {
    val opintokokonaisuusKoulutusOid = KoulutusOid("1.2.246.562.13.134")
    when(koulutusDao.get(opintokokonaisuusKoulutusOid))
      .thenAnswer(
        Some(
          kkOpintokokonaisuusKoulutus
            .copy(metadata =
              Some(
                KkOpintokokonaisuusKoulutuksenMetatieto.copy(
                  opintojenLaajuusNumeroMin = None,
                )
              )
            )
        )
      )

    passValidation(kkOpintokokonaisuusToteutus.copy(koulutusOid = opintokokonaisuusKoulutusOid))

    when(koulutusDao.get(opintokokonaisuusKoulutusOid))
      .thenAnswer(
        Some(
          kkOpintokokonaisuusKoulutus
            .copy(metadata =
              Some(
                KkOpintokokonaisuusKoulutuksenMetatieto.copy(
                  opintojenLaajuusNumeroMax = None,
                )
              )
            )
        )
      )

    passValidation(kkOpintokokonaisuusToteutus.copy(koulutusOid = opintokokonaisuusKoulutusOid))

  }

  "Lukiototeutus validation" should "fail if invalid painotukset" in {
    val kuvaus = Map(Fi -> "kuvaus fi", Sv -> "kuvaus sv")
    failValidation(
      lukioToteutusWithSpecificParameters(painotukset =
        Seq(
          LukiolinjaTieto("puppu", kuvaus),
          LukiolinjaTieto("lukiopainotukset_2", kuvaus),
          LukiolinjaTieto("lukiopainotukset_1#1", Map(Fi -> "vain suomeksi"))
        )
      ),
      Seq(
        ValidationError("metadata.painotukset[0].koodiUri", validationMsg("puppu")),
        ValidationError(
          "metadata.painotukset[1].koodiUri",
          invalidLukioLinjaKoodiUri("painotukset", "lukiopainotukset_2")
        ),
        ValidationError("metadata.painotukset[2].kuvaus", invalidKielistetty(Seq(Sv)))
      )
    )
  }

  it should "fail if invalid erityisetKoulutustehtavat" in {
    val kuvaus = Map(Fi -> "kuvaus fi", Sv -> "kuvaus sv")
    failValidation(
      lukioToteutusWithSpecificParameters(erityisetKoulutustehtavat =
        Seq(
          LukiolinjaTieto("puppu", kuvaus),
          LukiolinjaTieto("lukiolinjaterityinenkoulutustehtava_2", kuvaus),
          LukiolinjaTieto("lukiolinjaterityinenkoulutustehtava_1#1", Map(Fi -> "vain suomeksi"))
        )
      ),
      Seq(
        ValidationError("metadata.erityisetKoulutustehtavat[0].koodiUri", validationMsg("puppu")),
        ValidationError(
          "metadata.erityisetKoulutustehtavat[1].koodiUri",
          invalidLukioLinjaKoodiUri("erityisetKoulutustehtavat", "lukiolinjaterityinenkoulutustehtava_2")
        ),
        ValidationError("metadata.erityisetKoulutustehtavat[2].kuvaus", invalidKielistetty(Seq(Sv)))
      )
    )
  }

  it should "fail if no painotukset nor erityinenkoulutustehtavat and yleislinja = false" in {
    val metadataBase =
      lukioToteutusWithSpecificParameters(painotukset = Seq(), erityisetKoulutustehtavat = Seq()).metadata.get
        .asInstanceOf[LukioToteutusMetadata]
    failValidation(
      lukioToteutus.copy(metadata = Some(metadataBase.copy(yleislinja = false))),
      "metadata.yleislinja",
      withoutLukiolinja
    )
  }

  it should "fail if invalid diplomi" in {
    val linkki = Map(Fi -> "http://linkki.fi", Sv -> "http://link.se")
    val alt    = Map(Fi -> "Suomeksi", Sv -> "På svenska")
    failValidation(
      lukioToteutusWithSpecificParameters(diplomit =
        Seq(
          LukiodiplomiTieto("puppu", linkki, Map(Fi -> "vain suomeksi")),
          LukiodiplomiTieto("moduulikoodistolops2021_kald4", linkki, Map(Fi -> "vain suomeksi")),
          LukiodiplomiTieto("moduulikoodistolops2021_kald3#1", Map(Fi -> "puppu fi", Sv -> "puppu sv"), alt)
        )
      ).copy(tila = Tallennettu),
      Seq(
        ValidationError("metadata.diplomit[0].koodiUri", validationMsg("puppu")),
        ValidationError("metadata.diplomit[1].koodiUri", invalidLukioDiplomiKoodiUri("moduulikoodistolops2021_kald4")),
        ValidationError("metadata.diplomit[2].linkki.fi", invalidUrl("puppu fi")),
        ValidationError("metadata.diplomit[2].linkki.sv", invalidUrl("puppu sv"))
      )
    )
    failValidation(
      lukioToteutusWithSpecificParameters(diplomit =
        Seq(
          LukiodiplomiTieto("moduulikoodistolops2021_kald3#1", Map(Fi -> "http://linkki.fi"), alt),
          LukiodiplomiTieto("moduulikoodistolops2021_kald3#1", linkki, Map(Fi -> "vain suomeksi"))
        )
      ),
      Seq(
        ValidationError("metadata.diplomit[0].linkki", invalidKielistetty(Seq(Sv))),
        ValidationError("metadata.diplomit[1].linkinAltTeksti", invalidKielistetty(Seq(Sv)))
      )
    )
  }

  it should "fail if invalid A1Kielet" in {
    failValidation(
      lukioToteutusWithKieliParameters(A1Kielet = Seq("puppu", "kieli_XX#1")),
      Seq(
        ValidationError("metadata.kielivalikoima.A1Kielet[0]", validationMsg("puppu")),
        ValidationError("metadata.kielivalikoima.A1Kielet[1]", invalidKieliKoodiUri("A1Kielet", "kieli_XX#1"))
      )
    )
  }

  it should "fail if invalid A2Kielet" in {
    failValidation(
      lukioToteutusWithKieliParameters(A2Kielet = Seq("puppu", "kieli_YY#1")),
      Seq(
        ValidationError("metadata.kielivalikoima.A2Kielet[0]", validationMsg("puppu")),
        ValidationError("metadata.kielivalikoima.A2Kielet[1]", invalidKieliKoodiUri("A2Kielet", "kieli_YY#1"))
      )
    )
  }

  it should "fail if invalid B1Kielet" in {
    failValidation(
      lukioToteutusWithKieliParameters(B1Kielet = Seq("puppu", "kieli_ZZ#1")),
      Seq(
        ValidationError("metadata.kielivalikoima.B1Kielet[0]", validationMsg("puppu")),
        ValidationError("metadata.kielivalikoima.B1Kielet[1]", invalidKieliKoodiUri("B1Kielet", "kieli_ZZ#1"))
      )
    )
  }

  it should "fail if invalid B2Kielet" in {
    failValidation(
      lukioToteutusWithKieliParameters(B2Kielet = Seq("puppu", "kieli_XY#1")),
      Seq(
        ValidationError("metadata.kielivalikoima.B2Kielet[0]", validationMsg("puppu")),
        ValidationError("metadata.kielivalikoima.B2Kielet[1]", invalidKieliKoodiUri("B2Kielet", "kieli_XY#1"))
      )
    )
  }

  it should "fail if invalid B3Kielet" in {
    failValidation(
      lukioToteutusWithKieliParameters(B3Kielet = Seq("puppu", "kieli_XZ#1")),
      Seq(
        ValidationError("metadata.kielivalikoima.B3Kielet[0]", validationMsg("puppu")),
        ValidationError("metadata.kielivalikoima.B3Kielet[1]", invalidKieliKoodiUri("B3Kielet", "kieli_XZ#1"))
      )
    )
  }

  it should "fail if invalid Äidinkielet" in {
    failValidation(
      lukioToteutusWithKieliParameters(aidinkielet = Seq("puppu", "kieli_ZX#1")),
      Seq(
        ValidationError("metadata.kielivalikoima.aidinkielet[0]", validationMsg("puppu")),
        ValidationError("metadata.kielivalikoima.aidinkielet[1]", invalidKieliKoodiUri("aidinkielet", "kieli_ZX#1"))
      )
    )
  }

  it should "fail if invalid muut kielet" in {
    failValidation(
      lukioToteutusWithKieliParameters(muutKielet = Seq("puppu", "kieli_ZY#1")),
      Seq(
        ValidationError("metadata.kielivalikoima.muutKielet[0]", validationMsg("puppu")),
        ValidationError("metadata.kielivalikoima.muutKielet[1]", invalidKieliKoodiUri("muutKielet", "kieli_ZY#1"))
      )
    )
  }

  "State change" should "succeed from tallennettu to julkaistu" in {
    passValidation(JulkaistuAmmToteutus, JulkaistuAmmToteutus.copy(tila = Tallennettu))
  }

  it should "succeed from julkaistu to arkistoitu" in {
    passValidation(JulkaistuAmmToteutus.copy(tila = Arkistoitu), JulkaistuAmmToteutus)
  }

  it should "succeed from arkistoitu to julkaistu" in {
    passValidation(JulkaistuAmmToteutus, JulkaistuAmmToteutus.copy(tila = Arkistoitu))
  }

  it should "succeed from julkaistu to tallennettu" in {
    passValidation(JulkaistuAmmToteutus.copy(tila = Tallennettu), JulkaistuAmmToteutus)
  }

  it should "succeed from tallennettu to poistettu when no existing hakukohteet for toteutus" in {
    passValidation(
      JulkaistuAmmToteutus.copy(tila = Poistettu, oid = Some(toteutusOid)),
      JulkaistuAmmToteutus.copy(tila = Tallennettu)
    )
  }

  it should "fail from tallennettu to arkistoitu" in {
    failStageChangeValidation(
      JulkaistuAmmToteutus.copy(tila = Arkistoitu),
      JulkaistuAmmToteutus.copy(tila = Tallennettu),
      illegalStateChange("toteutukselle", Tallennettu, Arkistoitu)
    )
  }

  it should "fail from arkistoitu to tallennettu" in {
    failStageChangeValidation(
      JulkaistuAmmToteutus.copy(tila = Tallennettu),
      JulkaistuAmmToteutus.copy(tila = Arkistoitu),
      illegalStateChange("toteutukselle", Arkistoitu, Tallennettu)
    )
  }

  it should "fail from julkaistu to poistettu" in {
    failStageChangeValidation(
      JulkaistuAmmToteutus.copy(tila = Poistettu),
      JulkaistuAmmToteutus,
      illegalStateChange("toteutukselle", Julkaistu, Poistettu)
    )
  }

  it should "fail from arkistoitu to poistettu" in {
    failStageChangeValidation(
      JulkaistuAmmToteutus.copy(tila = Poistettu),
      JulkaistuAmmToteutus.copy(tila = Arkistoitu),
      illegalStateChange("toteutukselle", Arkistoitu, Poistettu)
    )
  }

  it should "fail from poistettu to tallennettu" in {
    failStageChangeValidation(
      JulkaistuAmmToteutus.copy(tila = Tallennettu),
      JulkaistuAmmToteutus.copy(tila = Poistettu),
      illegalStateChange("toteutukselle", Poistettu, Tallennettu)
    )
  }

  it should "fail from tallennettu to poistettu when existing hakukohteet for toteutus" in {
    failStageChangeValidation(
      JulkaistuAmmToteutus.copy(tila = Poistettu, oid = Some(toteutusOid2)),
      JulkaistuAmmToteutus.copy(tila = Tallennettu),
      integrityViolationMsg("Toteutusta", "hakukohteita")
    )
  }

  "Julkaisu" should "fail when changing state to julkaistu and invalid koulutuksen alkamiskausi" in {
    val yearAgo   = LocalDateTime.now().minusYears(1)
    val startDate = yearAgo.minusDays(3)
    val endDate   = yearAgo.minusDays(1)
    failStageChangeValidation(
      ammToteutusWithKoulutuksenAlkamiskausi(Some(startDate), Some(endDate)),
      JulkaistuAmmToteutus.copy(tila = Tallennettu),
      Seq(
        ValidationError(
          "metadata.opetus.koulutuksenAlkamiskausi.koulutuksenAlkamisvuosi",
          pastDateMsg(yearAgo.getYear.toString)
        ),
        ValidationError("metadata.opetus.koulutuksenAlkamiskausi.koulutuksenAlkamispaivamaara", pastDateMsg(startDate)),
        ValidationError("metadata.opetus.koulutuksenAlkamiskausi.koulutuksenPaattymispaivamaara", pastDateMsg(endDate))
      )
    )
  }

  it should "fail when new julkaistu toteutus with invalid koulutuksen alkamiskausi" in {
    val yearAgo   = LocalDateTime.now().minusYears(1)
    val startDate = yearAgo.minusDays(3)
    val endDate   = yearAgo.minusDays(1)
    failValidation(
      ammToteutusWithKoulutuksenAlkamiskausi(Some(startDate), Some(endDate)),
      Seq(
        ValidationError(
          "metadata.opetus.koulutuksenAlkamiskausi.koulutuksenAlkamisvuosi",
          pastDateMsg(yearAgo.getYear.toString)
        ),
        ValidationError("metadata.opetus.koulutuksenAlkamiskausi.koulutuksenAlkamispaivamaara", pastDateMsg(startDate)),
        ValidationError("metadata.opetus.koulutuksenAlkamiskausi.koulutuksenPaattymispaivamaara", pastDateMsg(endDate))
      )
    )
  }

  it should "succeed when alkamiskausi not given" in {
    passValidation(
      ammToteutusWithKoulutuksenAlkamiskausi(None, None, startYear = Some(LocalDateTime.now().getYear.toString)),
      JulkaistuAmmToteutus.copy(tila = Tallennettu)
    )
  }

  it should "succeed when modifying already julkaistu toteutus" in {
    val yearAgo   = LocalDateTime.now().minusYears(1)
    val startDate = yearAgo.minusDays(3)
    val endDate   = yearAgo.minusDays(1)
    passValidation(ammToteutusWithKoulutuksenAlkamiskausi(Some(startDate), Some(endDate)), JulkaistuAmmToteutus)
  }

  it should "fail if ammatillinenPerustutkintoErityisopetuksena is true and koulutustyyppi does not have relation to koulutustyyppi_1" in {
    failValidation(
      JulkaistuAmmToteutus.copy(koulutusOid = koulutusOid1, metadata = Some(AmmToteutuksenMetatieto.copy(ammatillinenPerustutkintoErityisopetuksena = true))),
      "metadata.ammatillinenPerustutkintoErityisopetuksena",
      ErrorMessage(
        msg = s"Koulutuksen koulutustyyppi List(koulutus_XXX#1) on virheellinen, koulutustyyppillä täytyy olla koodistorelaatio koulutustyyppi_1:een että se voidaan järjestää erityisopetuksena",
        id = "invalidKoulutustyyppiKoodiForAmmatillinenPerustutkintoErityisopetuksena"
      )
    )
  }

  it should "succeed if ammatillinenPerustutkintoErityisopetuksena is true and koulutustyyppi has valid relation to koulutustyyppi_1" in {
    passValidation(
      JulkaistuAmmToteutus.copy(koulutusOid = koulutusOid2, metadata = Some(AmmToteutuksenMetatieto.copy(ammatillinenPerustutkintoErityisopetuksena = true)))
    )
  }

  it should "fail if ammatillinenPerustutkintoErityisopetuksena is true and koulutus is not found" in {
    failValidation(JulkaistuAmmToteutus.copy(koulutusOid = invalidKoulutusOid, metadata = Some(AmmToteutuksenMetatieto)), "koulutusOid", nonExistent("Koulutusta", invalidKoulutusOid))
    failValidation(JulkaistuAmmToteutus.copy(koulutusOid = invalidKoulutusOid, metadata = Some(AmmToteutuksenMetatieto.copy(ammatillinenPerustutkintoErityisopetuksena = true))), "koulutusOid", nonExistent("Koulutusta", invalidKoulutusOid))
  }
}
