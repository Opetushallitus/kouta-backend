package fi.oph.kouta.integration

import fi.oph.kouta.TestData._
import fi.oph.kouta.TestOids.{AmmOid, LonelyOid, LukioOid, OtherOid, UnknownOid}
import fi.oph.kouta.client.{HakuKoodiClient, KoulutusKoodiClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.repository.{HakukohdeDAO, KoulutusDAO, SorakuvausDAO}
import fi.oph.kouta.service.{OrganisaatioService, ToteutusServiceValidation}
import fi.oph.kouta.validation.ExternalQueryResults.{itemFound, itemNotFound}
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{
  BaseServiceValidationSpec,
  ValidationError,
  ammatillinenPerustutkintoKoulutustyyppiKoodiUri
}
import org.scalatest.Assertion

import java.time.LocalDateTime
import java.util.UUID

class ToteutusServiceValidationSpec extends BaseServiceValidationSpec[Toteutus] {
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
  val ammOpettajaKoulutus         = AmmOpettajaKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.134")))
  val ammOpettajaToteutus         = JulkaistuAmmOpettajaToteutus.copy(koulutusOid = ammOpettajaKoulutus.oid.get)
  val yoOpettajaKoulutus          = YoOpettajaKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.135")))
  val yoOpettajaToteutus          = JulkaistuYoOpettajaToteutus.copy(koulutusOid = yoOpettajaKoulutus.oid.get)
  val tuvaKoulutus = TuvaKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.136")))
  val tuvaToteutus = TuvaToteutus.copy(koulutusOid = tuvaKoulutus.oid.get)
  val telmaKoulutus = TelmaKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.137")))
  val telmaToteutus = TelmaToteutus.copy(koulutusOid = telmaKoulutus.oid.get)

  val sorakuvausId  = UUID.randomUUID()
  val sorakuvausId2 = UUID.randomUUID()
  val sorakuvausId3 = UUID.randomUUID()
  val sorakuvausId4 = UUID.randomUUID()
  val sorakuvausId5 = UUID.randomUUID()
  val sorakuvausId6 = UUID.randomUUID()

  val toteutusOid  = ToteutusOid("1.2.246.562.17.00000000000000000123")
  val toteutusOid2 = ToteutusOid("1.2.246.562.17.00000000000000000124")

  val existingToteutus   = JulkaistuAmmToteutus.copy(oid = Some(toteutusOid))
  val koulutusOid1       = KoulutusOid("1.2.246.562.13.00000000000000000997")
  val koulutusOid2       = KoulutusOid("1.2.246.562.13.00000000000000000998")
  val invalidKoulutusOid = KoulutusOid("1.2.246.562.13.00000000000000000999")

  val invalidKoulutuksetKoodiUri = "koulutus_XXX#1"
  val validKoulutuksetKoodiUri   = "koulutus_371101#1"

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
      .thenAnswer(Right(Set[OrganisaatioOid]()))
    when(organisaatioService.findUnknownOrganisaatioOidsFromHierarkia(Set(LonelyOid, LukioOid)))
      .thenAnswer(Right(Set[OrganisaatioOid](LonelyOid)))
    when(organisaatioService.findUnknownOrganisaatioOidsFromHierarkia(Set(LonelyOid, UnknownOid)))
      .thenAnswer(Left(new RuntimeException()))
    when(koulutusKoodiClient.opetusKieliKoodiUriExists("oppilaitoksenopetuskieli_1#1")).thenAnswer(itemFound)
    when(koulutusKoodiClient.opetusAikaKoodiUriExists("opetusaikakk_1#1")).thenAnswer(itemFound)
    when(koulutusKoodiClient.opetusTapaKoodiUriExists("opetuspaikkakk_1#1")).thenAnswer(itemFound)
    when(koulutusKoodiClient.opetusTapaKoodiUriExists("opetuspaikkakk_2#1")).thenAnswer(itemFound)
    when(koulutusKoodiClient.lisatiedotOtsikkoKoodiUriExists("koulutuksenlisatiedot_03#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.kausiKoodiUriExists("kausi_k#1")).thenAnswer(itemFound)

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
    when(koulutusDao.get(koulutusOid1))
      .thenAnswer(
        Some(AmmKoulutus.copy(oid = Some(koulutusOid1), koulutuksetKoodiUri = Seq(invalidKoulutuksetKoodiUri)))
      )
    when(koulutusDao.get(koulutusOid2))
      .thenAnswer(Some(AmmKoulutus.copy(oid = Some(koulutusOid2), koulutuksetKoodiUri = Seq(validKoulutuksetKoodiUri))))
    when(koulutusDao.get(ammOpettajaKoulutus.oid.get)).thenAnswer(Some(ammOpettajaKoulutus))
    when(koulutusDao.get(yoOpettajaKoulutus.oid.get)).thenAnswer(Some(yoOpettajaKoulutus))
    when(koulutusDao.get(tuvaKoulutus.oid.get)).thenAnswer(Some(tuvaKoulutus))
    when(koulutusDao.get(telmaKoulutus.oid.get)).thenAnswer(Some(telmaKoulutus))
    when(koulutusDao.get(invalidKoulutusOid)).thenAnswer(None)

    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId))
      .thenAnswer(Some(Julkaistu), Some(Amm), Some(Seq(validKoulutuksetKoodiUri)))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId2)).thenAnswer((None, None, None))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId3))
      .thenAnswer((Some(Tallennettu), Some(Amm), Some(Seq(validKoulutuksetKoodiUri))))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId4))
      .thenAnswer((Some(Poistettu), Some(Amm), Some(Seq(validKoulutuksetKoodiUri))))
    when(sorakuvausDao.getTilaTyyppiAndKoulutusKoodit(sorakuvausId5)).thenAnswer((Some(Julkaistu), Some(Yo), None))
    when(hakukohdeDao.listByToteutusOid(toteutusOid, TilaFilter.onlyOlemassaolevat()))
      .thenAnswer(Seq[HakukohdeListItem]())
    when(hakukohdeDao.listByToteutusOid(toteutusOid2, TilaFilter.onlyOlemassaolevat()))
      .thenAnswer(Seq(MinHakukohdeListItem.copy(toteutusOid = toteutusOid2)))

    // ammatillinen
    when(koulutusKoodiClient.osaamisalaKoodiUriExists("osaamisala_0001#1")).thenAnswer(itemFound)
    // lukio
    when(koulutusKoodiClient.lukioPainotusKoodiUriExists("lukiopainotukset_1#1")).thenAnswer(itemFound)
    when(
      koulutusKoodiClient.lukioErityinenKoulutustehtavaKoodiUriExists("lukiolinjaterityinenkoulutustehtava_1#1")
    ).thenAnswer(itemFound)
    when(koulutusKoodiClient.lukioDiplomiKoodiUriExists("moduulikoodistolops2021_kald3#1")).thenAnswer(itemFound)
    when(koulutusKoodiClient.lukioDiplomiKoodiUriExists("moduulikoodistolops2021_kald3#1")).thenAnswer(itemFound)
    when(
      koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExist(
        Seq(ammatillinenPerustutkintoKoulutustyyppiKoodiUri),
        validKoulutuksetKoodiUri
      )
    ).thenAnswer(itemFound)
    when(
      koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExist(
        Seq(ammatillinenPerustutkintoKoulutustyyppiKoodiUri),
        invalidKoulutuksetKoodiUri
      )
    ).thenAnswer(itemNotFound)
    when(hakuKoodiClient.kieliKoodiUriExists("kieli_EN#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.kieliKoodiUriExists("kieli_DE#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.kieliKoodiUriExists("kieli_SV#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.kieliKoodiUriExists("kieli_FR#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.kieliKoodiUriExists("kieli_ES#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.kieliKoodiUriExists("kieli_FI#1")).thenAnswer(itemFound)
    when(hakuKoodiClient.kieliKoodiUriExists("kieli_ET#1")).thenAnswer(itemFound)
  }

  "Validation" should "succeed when new valid toteutus" in {
    passesValidation(JulkaistuAmmToteutus)
  }

  it should "succeed when new incomplete luonnos" in {
    passesValidation(MinToteutus)
  }

  it should "succeed when sorakuvausId given for suitable type of toteutus" in {
    passesValidation(ammTutkinnonOsaToteutus.copy(sorakuvausId = Some(sorakuvausId)))
  }

  it should "succeed when new valid AmmOpettaja-toteutus" in {
    passesValidation(ammOpettajaToteutus)
  }

  it should "succeed when new valid Opettajien pedagogiset opinnot -toteutus" in {
    passesValidation(yoOpettajaToteutus)
  }

  it should "succeed when new valid tutkintoon johtamaton toteutus" in {
    passesValidation(ammMuuToteutus)
  }

  it should "succeed when new valid korkeakoulu toteutus" in {
    passesValidation(yoToteutus)
  }

  it should "succeed when new valid lukio-toteutus" in {
    passesValidation(lukioToteutus)
  }

  it should "succeed when new valid kk-opintojakso toteutus" in {
    passesValidation(kkOpintojaksoToteutus)
  }

  it should "succeed when tarjoajat not changed in modify operation, eventhough unknown organisaatiot" in {
    val toteutus = existingToteutus.copy(tarjoajat = List(LonelyOid, LukioOid))
    passesValidation(toteutus, toteutus)
  }

  it should "succeed when opetus not changed in modify operation, eventhough unknown koodiUrit" in {
    val toteutus1 = ammToteutusWithOpetusParameters(
      Seq("oppilaitoksenopetuskieli_9#1"),
      Seq("opetusaikakk_9#1"),
      Seq("opetuspaikkakk_9#1"),
      ToteutuksenOpetus.apuraha,
      Seq(Lisatieto1.copy(otsikkoKoodiUri = "koulutuksenlisatiedot_99#1"))
    ).copy(oid = Some(toteutusOid))
    passesValidation(toteutus1, toteutus1)

    val toteutus2 =
      ammToteutusWithKoulutuksenAlkamiskausi(startDate = None, endDate = None, koodiUri = Some("kausi_k#99"))
        .copy(tila = Tallennettu, oid = Some(toteutusOid))
    passesValidation(toteutus2, toteutus2)
  }

  it should "succeed when ammatilliset osaamisalat not changed in modify operation, eventhough unknown osaamisalaKoodiUri" in {
    val toteutus = existingToteutus.copy(
      tila = Tallennettu,
      metadata = Some(
        AmmToteutuksenMetatieto.copy(osaamisalat =
          List(
            AmmatillinenOsaamisala(
              koodiUri = "osaamisala_0002"
            )
          )
        )
      )
    )
    passesValidation(toteutus, toteutus)
  }

  it should "succeed when lukiototeutus not change in modify operation, eventhough unknown koodiUrit" in {
    val unknown = Seq("kieli_XX1")
    val toteutus1 = lukioToteutusWithKieliParameters(unknown, unknown, unknown, unknown, unknown, unknown, unknown)
      .copy(oid = Some(toteutusOid))
    passesValidation(toteutus1, toteutus1)
    val toteutus2 = lukioToteutusWithSpecificParameters(
      painotukset = List(
        LukiolinjaTieto(koodiUri = "lukiopainotukset_9#1", Map(Fi -> "painotus 1 kuvaus", Sv -> "painotus 1 kuvaus sv"))
      ),
      erityisetKoulutustehtavat = List(
        LukiolinjaTieto(
          koodiUri = "lukiolinjaterityinenkoulutustehtava_9#1",
          Map(Fi -> "erityinen koulutustehtava 1 kuvaus", Sv -> "erityinen koulutustehtava 1 kuvaus sv")
        )
      ),
      diplomit = List(
        LukiodiplomiTieto(
          koodiUri = "moduulikoodistolops2021_kald9#1",
          linkki = Map(Fi -> "http://linkki.fi", Sv -> "http://link.se"),
          linkinAltTeksti = Map(Fi -> "Suomeksi", Sv -> "På svenska")
        )
      )
    ).copy(oid = Some(toteutusOid))
    passesValidation(toteutus2, toteutus2)
  }

  it should "fail if perustiedot is invalid" in {
    failsValidation(
      lukioToteutus.copy(oid = Some(ToteutusOid("1.2.3"))),
      Seq(
        ValidationError("oid", validationMsg("1.2.3")),
        ValidationError("oid", notMissingMsg(Some(ToteutusOid("1.2.3"))))
      )
    )
    failsValidation(lukioToteutus.copy(kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(lukioToteutus.copy(nimi = Map(Fi -> "nimi")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(lukioToteutus.copy(nimi = Map(Fi -> "nimi", Sv -> "")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(
      lukioToteutus.copy(organisaatioOid = OrganisaatioOid("1.2.3")),
      "organisaatioOid",
      validationMsg("1.2.3")
    )
    failsValidation(lukioToteutus.copy(organisaatioOid = OrganisaatioOid("")), "organisaatioOid", validationMsg(""))
    failsValidation(lukioToteutus.copy(koulutusOid = KoulutusOid("puppu")), "koulutusOid", validationMsg("puppu"))
    failsValidation(lukioToteutus.copy(teemakuva = Some("puppu")), "teemakuva", invalidUrl("puppu"))
  }

  it should "fail if koulutustyyppi changed in modify operation" in {
    failsModifyValidation(
      lukioToteutus.copy(
        oid = Some(toteutusOid),
        metadata = Some(lukioToteutus.metadata.get.asInstanceOf[LukioToteutusMetadata].copy(tyyppi = Amm))
      ),
      lukioToteutus,
      Seq(
        ValidationError("metadata.tyyppi", tyyppiMismatch("koulutuksen", "1.2.246.562.13.125")),
        ValidationError("metadata.tyyppi", notModifiableMsg("koulutustyyppiä", "toteutukselle"))
      )
    )
  }

  it should "fail if oid not given in modify operation" in {
    failsModifyValidation(lukioToteutus, lukioToteutus, Seq(ValidationError("oid", missingMsg)))
  }

  it should "fail if metadata missing from julkaistu totetutus" in {
    failsValidation(
      JulkaistuAmmToteutus.copy(metadata = None),
      "metadata",
      missingMsg
    )
  }

  it should "fail if invalid tarjoaja OIDs" in {
    failsValidation(
      lukioToteutus.copy(tarjoajat = List(OrganisaatioOid("puppu"), LonelyOid, LukioOid)),
      Seq(
        ValidationError("tarjoajat[0]", validationMsg("puppu")),
        ValidationError("tarjoajat[1]", unknownTarjoajaOid(LonelyOid))
      )
    )
  }

  it should "fail if organisaatio-service not working when checking tarjoajat" in {
    failsValidation(
      lukioToteutus.copy(tarjoajat = List(LonelyOid, UnknownOid)),
      "tarjoajat",
      organisaatioServiceFailureMsg
    )
  }

  it should "fail if koulutus doesn't exist" in {
    val oid = KoulutusOid("1.2.246.562.13.126")
    failsValidation(JulkaistuAmmToteutus.copy(koulutusOid = oid), "koulutusOid", nonExistent("Koulutusta", oid))
  }

  it should "fail to store julkaistu toteutus if koulutus is not yet julkaistu" in {
    val oid = KoulutusOid("1.2.246.562.13.127")
    failsValidation(
      JulkaistuAmmToteutus.copy(koulutusOid = oid),
      "tila",
      notYetJulkaistu("Koulutusta", oid)
    )
  }

  it should "fail to store julkaistu toteutus if koulutus poistettu" in {
    val oid = KoulutusOid("1.2.246.562.13.128")
    failsValidation(JulkaistuAmmToteutus.copy(koulutusOid = oid), "koulutusOid", nonExistent("Koulutusta", oid))
  }

  it should "fail to store toteutus if koulutustyyppi doesn't match koulutus koulutustyyppi" in {
    val oid = KoulutusOid("1.2.246.562.13.125")
    failsValidation(JulkaistuAmmToteutus.copy(koulutusOid = oid), "metadata.tyyppi", tyyppiMismatch("koulutuksen", oid))
  }

  private def failSorakuvausValidation(toteutus: Toteutus, koulutusOidStr: String): Assertion = {
    val koulutusOid = KoulutusOid(koulutusOidStr)
    when(koulutusDao.get(koulutusOid))
      .thenAnswer(Some(AmmKoulutus.copy(tila = Julkaistu, koulutustyyppi = toteutus.metadata.get.tyyppi)))
    val testedToteutus = toteutus.copy(koulutusOid = koulutusOid, sorakuvausId = Some(sorakuvausId))
    failsValidation(testedToteutus, "sorakuvausId", notMissingMsg(Some(sorakuvausId)))
  }

  it should "fail if sorakuvaus given for non supported koulutustyyppi" in {
    failSorakuvausValidation(JulkaistuAmmToteutus, "1.2.246.562.13.600")
    failSorakuvausValidation(AmmMuuToteutus, "1.2.246.562.13.601")
    failSorakuvausValidation(JulkaistuYoToteutus, "1.2.246.562.13.602")
    failSorakuvausValidation(JulkaistuAmkToteutus, "1.2.246.562.13.603")
    failSorakuvausValidation(JulkaistuAmmOpettajaToteutus, "1.2.246.562.13.604")
    failSorakuvausValidation(JulkaistuYoOpettajaToteutus, "1.2.246.562.13.605")
    failSorakuvausValidation(LukioToteutus, "1.2.246.562.13.606")
    failSorakuvausValidation(TuvaToteutus, "1.2.246.562.13.607")
    failSorakuvausValidation(TelmaToteutus, "1.2.246.562.13.608")
    failSorakuvausValidation(VapaaSivistystyoOpistovuosiToteutus, "1.2.246.562.13.609")
    failSorakuvausValidation(VapaaSivistystyoMuuToteutus, "1.2.246.562.13.610")
  }

  it should "fail if sorakuvaus doesn't exist" in {
    failsValidation(
      ammTutkinnonOsaToteutus.copy(sorakuvausId = Some(sorakuvausId2)),
      "sorakuvausId",
      nonExistent("Sorakuvausta", sorakuvausId2)
    )
  }

  it should "fail to store julkaistu toteutus if sorakuvaus is not yet julkaistu" in {
    failsValidation(
      ammTutkinnonOsaToteutus.copy(sorakuvausId = Some(sorakuvausId3)),
      "tila",
      notYetJulkaistu("Sorakuvausta", sorakuvausId3)
    )
  }

  it should "fail to store julkaistu koulutus if sorakuvaus poistettu" in {
    failsValidation(
      ammTutkinnonOsaToteutus.copy(sorakuvausId = Some(sorakuvausId4)),
      "sorakuvausId",
      nonExistent("Sorakuvausta", sorakuvausId4)
    )
  }

  it should "fail to store koulutus if koulutustyyppi doesn't match sorakuvaus koulutustyyppi" in {
    failsValidation(
      ammTutkinnonOsaToteutus.copy(sorakuvausId = Some(sorakuvausId5)),
      "metadata.tyyppi",
      tyyppiMismatch("sorakuvauksen", sorakuvausId5)
    )
  }

  it should "fail if opetus missing from julkaistu totetutus" in {
    failsValidation(
      JulkaistuAmmToteutus.copy(metadata = Some(AmmToteutuksenMetatieto.copy(opetus = None))),
      "metadata.opetus",
      missingMsg
    )
  }

  it should "fail if invalid opetus parameters" in {
    failsValidation(
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
    failsValidation(
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
    failsValidation(
      ammToteutusWithOpetusParameters(opetuskieliKoodiUrit = Seq("puppu", "oppilaitoksenopetuskieli_2#1")),
      Seq(
        ValidationError("metadata.opetus.opetuskieliKoodiUrit[0]", invalidOpetusKieliKoodiUri("puppu")),
        ValidationError(
          "metadata.opetus.opetuskieliKoodiUrit[1]",
          invalidOpetusKieliKoodiUri("oppilaitoksenopetuskieli_2#1")
        )
      )
    )
  }

  it should "fail if invalid opetusaikaKoodiUri" in {
    failsValidation(
      ammToteutusWithOpetusParameters(opetusaikaKoodiUrit = Seq("puppu", "opetusaikakk_2#1")),
      Seq(
        ValidationError("metadata.opetus.opetusaikaKoodiUrit[0]", invalidOpetusAikaKoodiUri("puppu")),
        ValidationError("metadata.opetus.opetusaikaKoodiUrit[1]", invalidOpetusAikaKoodiUri("opetusaikakk_2#1"))
      )
    )
  }

  it should "fail if invalid opetustapaKoodiUri" in {
    failsValidation(
      ammToteutusWithOpetusParameters(opetustapaKoodiUrit = Seq("puppu", "opetuspaikkakk_3#1")),
      Seq(
        ValidationError("metadata.opetus.opetustapaKoodiUrit[0]", invalidOpetusTapaKoodiUri("puppu")),
        ValidationError("metadata.opetus.opetustapaKoodiUrit[1]", invalidOpetusTapaKoodiUri("opetuspaikkakk_3#1"))
      )
    )
  }

  it should "fail if invalid opetus-lisatieto" in {
    val validLisatieto = Map(Fi -> "lisatieto fi", Sv -> "lisatieto sv")
    failsValidation(
      ammToteutusWithOpetusParameters(lisatiedot =
        Seq(
          Lisatieto("koulutuksenlisatiedot_03#1", Map(Fi -> "vain suomeksi")),
          Lisatieto("koulutuksenlisatiedot_04#1", validLisatieto)
        )
      ),
      Seq(
        ValidationError("metadata.opetus.lisatiedot[0].teksti", invalidKielistetty(Seq(Sv))),
        ValidationError(
          "metadata.opetus.lisatiedot[1].otsikkoKoodiUri",
          invalidLisatietoOtsikkoKoodiuri("koulutuksenlisatiedot_04#1")
        )
      )
    )
  }

  it should "fail if invalid apuraha" in {
    val validKuvaus = Map(Fi -> "kuvaus fi", Sv -> "kuvaus sv")
    failsValidation(
      ammToteutusWithOpetusParameters(apuraha = Some(Apuraha(Some(-100), Some(-200), Some(Euro), validKuvaus))),
      Seq(
        ValidationError("metadata.opetus.apuraha.min", minmaxMsg(-100, -200)),
        ValidationError("metadata.opetus.apuraha.min", notNegativeMsg),
        ValidationError("metadata.opetus.apuraha.max", notNegativeMsg)
      )
    )
    failsValidation(
      ammToteutusWithOpetusParameters(apuraha = Some(Apuraha(Some(50), Some(200), Some(Prosentti), validKuvaus))),
      "metadata.opetus.apuraha.max",
      lessOrEqualMsg(200, 100)
    )
    failsValidation(
      ammToteutusWithOpetusParameters(apuraha = Some(Apuraha(None, None, None, Map(Fi -> "vain suomeksi")))),
      Seq(
        ValidationError("metadata.opetus.apuraha.kuvaus", invalidKielistetty(Seq(Sv))),
        ValidationError("metadata.opetus.apuraha.min", missingMsg),
        ValidationError("metadata.opetus.apuraha.max", missingMsg)
      )
    )
  }

  it should "fail if invalid koulutuksenAlkamiskausi" in {
    failsValidation(
      ammToteutusWithKoulutuksenAlkamiskausi(
        Some(inFuture(2000)),
        Some(inFuture(1000))
      ).copy(tila = Tallennettu),
      "metadata.opetus.koulutuksenAlkamiskausi.koulutuksenAlkamispaivamaara",
      InvalidKoulutuspaivamaarat
    )
  }

  it should "fail if metadata kuvaus missing for koulutustyypit requiring kuvaus" in {
    failsValidation(
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
    passesValidation(
      lukioToteutus.copy(metadata =
        Some(
          lukioToteutus.metadata.get.asInstanceOf[LukioToteutusMetadata].copy(kuvaus = Map(Fi -> "", Sv -> ""))
        )
      )
    )
    failsValidation(
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
    failsValidation(
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
        ValidationError("metadata.osaamisalat[0].koodiUri", invalidOsaamisalaKoodiUri("puppu")),
        ValidationError("metadata.osaamisalat[1].koodiUri", invalidOsaamisalaKoodiUri("osaamisala_0002")),
        ValidationError("metadata.osaamisalat[2].linkki.fi", invalidUrl("puppu fi")),
        ValidationError("metadata.osaamisalat[2].linkki.sv", invalidUrl("puppu sv"))
      )
    )
  }

  it should "fail if invalid osaamisala for julkaistu toteutus" in {
    failsValidation(
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

  "AmmOpettajaToteutus validation" should "fail if negative aloituspaikat" in {
    failsValidation(
      ammOpettajaToteutus.copy(metadata = Some(AmmOpettajaToteutuksenMetatieto.copy(aloituspaikat = Some(-10)))),
      "metadata.aloituspaikat",
      notNegativeMsg
    )
  }

  "YoOpettajaToteutus validation" should "fail if negative aloituspaikat" in {
    failsValidation(
      yoOpettajaToteutus.copy(metadata =
        Some(AmmOpettajaToteutuksenMetatieto.copy(tyyppi = OpePedagOpinnot, aloituspaikat = Some(-10)))
      ),
      "metadata.aloituspaikat",
      notNegativeMsg
    )
  }

  "TuvaToteutus validation" should "fail if negative aloituspaikat" in {
    failsValidation(
      tuvaToteutus.copy(metadata = Some(TuvaToteutuksenMetatieto.copy(aloituspaikat = Some(-10)))),
      "metadata.aloituspaikat",
      notNegativeMsg
    )
  }

  "TelmaToteutus validation" should "fail if negative aloituspaikat" in {
    failsValidation(
      telmaToteutus.copy(metadata = Some(TelmaToteutuksenMetatieto.copy(aloituspaikat = Some(-10)))),
      "metadata.aloituspaikat",
      notNegativeMsg
    )
  }

  "Tutkintoon johtamaton toteutus validation" should "fail if invalid values for luonnos" in {
    val ajanjakso = Ajanjakso(alkaa = LocalDateTime.now(), paattyy = Some(LocalDateTime.now().minusDays(1)))
    failsValidation(
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

  it should "fail if missing values for julkaistu totetutus" in {
    val metadataBase = ammMuuToteutus.metadata.get.asInstanceOf[AmmatillinenMuuToteutusMetadata]
    failsValidation(
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
    failsValidation(
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
    failsValidation(
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
    failsValidation(
      kkOpintojaksoToteutus.copy(metadata =
        Some(KkOpintojaksoToteutuksenMetatieto.copy(ammattinimikkeet = List(Keyword(Fi, "nimike"))))
      ),
      "metadata.ammattinimikkeet",
      notEmptyMsg
    )
  }

  "Kk-opintokokonaisuus validation" should "fail if ammattinimikkeet given" in {
    failsValidation(
      kkOpintokokonaisuusToteutus.copy(metadata =
        Some(KkOpintokokonaisuusToteutuksenMetatieto.copy(ammattinimikkeet = List(Keyword(Fi, "nimike"))))
      ),
      "metadata.ammattinimikkeet",
      notEmptyMsg
    )
  }

  it should "succeed when new valid kk-opintokokonaisuustoteutus" in {
    passesValidation(kkOpintokokonaisuusToteutus)
  }

  it should "fail if opintojen laajuus not in the range given for koulutus" in {
    val toteutuksenLaajuus = Some(25.0)
    failsValidation(
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
    failsValidation(
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
    passesValidation(kkOpintokokonaisuusToteutus.copy(koulutusOid = opintokokonaisuusKoulutusOid))
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
    passesValidation(kkOpintokokonaisuusToteutus.copy(koulutusOid = opintokokonaisuusKoulutusOid))
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
                  opintojenLaajuusNumeroMin = None
                )
              )
            )
        )
      )

    passesValidation(kkOpintokokonaisuusToteutus.copy(koulutusOid = opintokokonaisuusKoulutusOid))

    when(koulutusDao.get(opintokokonaisuusKoulutusOid))
      .thenAnswer(
        Some(
          kkOpintokokonaisuusKoulutus
            .copy(metadata =
              Some(
                KkOpintokokonaisuusKoulutuksenMetatieto.copy(
                  opintojenLaajuusNumeroMax = None
                )
              )
            )
        )
      )

    passesValidation(kkOpintokokonaisuusToteutus.copy(koulutusOid = opintokokonaisuusKoulutusOid))

  }

  "Lukiototeutus validation" should "fail if invalid painotukset" in {
    val kuvaus = Map(Fi -> "kuvaus fi", Sv -> "kuvaus sv")
    failsValidation(
      lukioToteutusWithSpecificParameters(painotukset =
        Seq(
          LukiolinjaTieto("lukiopainotukset_2", kuvaus),
          LukiolinjaTieto("lukiopainotukset_1#1", Map(Fi -> "vain suomeksi"))
        )
      ),
      Seq(
        ValidationError(
          "metadata.painotukset[0].koodiUri",
          invalidLukioLinjaKoodiUri("painotukset", "lukiopainotukset_2")
        ),
        ValidationError("metadata.painotukset[1].kuvaus", invalidKielistetty(Seq(Sv)))
      )
    )
  }

  it should "fail if invalid erityisetKoulutustehtavat" in {
    val kuvaus = Map(Fi -> "kuvaus fi", Sv -> "kuvaus sv")
    failsValidation(
      lukioToteutusWithSpecificParameters(erityisetKoulutustehtavat =
        Seq(
          LukiolinjaTieto("lukiolinjaterityinenkoulutustehtava_2", kuvaus),
          LukiolinjaTieto("lukiolinjaterityinenkoulutustehtava_1#1", Map(Fi -> "vain suomeksi"))
        )
      ),
      Seq(
        ValidationError(
          "metadata.erityisetKoulutustehtavat[0].koodiUri",
          invalidLukioLinjaKoodiUri("erityisetKoulutustehtavat", "lukiolinjaterityinenkoulutustehtava_2")
        ),
        ValidationError("metadata.erityisetKoulutustehtavat[1].kuvaus", invalidKielistetty(Seq(Sv)))
      )
    )
  }

  it should "fail if no painotukset nor erityinenkoulutustehtavat and yleislinja = false" in {
    val metadataBase =
      lukioToteutusWithSpecificParameters(painotukset = Seq(), erityisetKoulutustehtavat = Seq()).metadata.get
        .asInstanceOf[LukioToteutusMetadata]
    failsValidation(
      lukioToteutus.copy(metadata = Some(metadataBase.copy(yleislinja = false))),
      "metadata.yleislinja",
      withoutLukiolinja
    )
  }

  it should "fail if invalid diplomi" in {
    val linkki = Map(Fi -> "http://linkki.fi", Sv -> "http://link.se")
    val alt    = Map(Fi -> "Suomeksi", Sv -> "På svenska")
    failsValidation(
      lukioToteutusWithSpecificParameters(diplomit =
        Seq(
          LukiodiplomiTieto("moduulikoodistolops2021_kald4", linkki, Map(Fi -> "vain suomeksi")),
          LukiodiplomiTieto("moduulikoodistolops2021_kald3#1", Map(Fi -> "puppu fi", Sv -> "puppu sv"), alt)
        )
      ).copy(tila = Tallennettu),
      Seq(
        ValidationError("metadata.diplomit[0].koodiUri", invalidLukioDiplomiKoodiUri("moduulikoodistolops2021_kald4")),
        ValidationError("metadata.diplomit[1].linkki.fi", invalidUrl("puppu fi")),
        ValidationError("metadata.diplomit[1].linkki.sv", invalidUrl("puppu sv"))
      )
    )
    failsValidation(
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
    failsValidation(
      lukioToteutusWithKieliParameters(A1Kielet = Seq("kieli_XX#1")),
      "metadata.kielivalikoima.A1Kielet[0]",
      invalidKieliKoodiUri("A1Kielet", "kieli_XX#1")
    )
  }

  it should "fail if invalid A2Kielet" in {
    failsValidation(
      lukioToteutusWithKieliParameters(A2Kielet = Seq("kieli_YY#1")),
      "metadata.kielivalikoima.A2Kielet[0]",
      invalidKieliKoodiUri("A2Kielet", "kieli_YY#1")
    )
  }

  it should "fail if invalid B1Kielet" in {
    failsValidation(
      lukioToteutusWithKieliParameters(B1Kielet = Seq("kieli_ZZ#1")),
      "metadata.kielivalikoima.B1Kielet[0]",
      invalidKieliKoodiUri("B1Kielet", "kieli_ZZ#1")
    )
  }

  it should "fail if invalid B2Kielet" in {
    failsValidation(
      lukioToteutusWithKieliParameters(B2Kielet = Seq("kieli_XY#1")),
      "metadata.kielivalikoima.B2Kielet[0]",
      invalidKieliKoodiUri("B2Kielet", "kieli_XY#1")
    )
  }

  it should "fail if invalid B3Kielet" in {
    failsValidation(
      lukioToteutusWithKieliParameters(B3Kielet = Seq("kieli_XZ#1")),
      "metadata.kielivalikoima.B3Kielet[0]",
      invalidKieliKoodiUri("B3Kielet", "kieli_XZ#1")
    )
  }

  it should "fail if invalid Äidinkielet" in {
    failsValidation(
      lukioToteutusWithKieliParameters(aidinkielet = Seq("kieli_ZX#1")),
      "metadata.kielivalikoima.aidinkielet[0]",
      invalidKieliKoodiUri("aidinkielet", "kieli_ZX#1")
    )
  }

  it should "fail if invalid muut kielet" in {
    failsValidation(
      lukioToteutusWithKieliParameters(muutKielet = Seq("kieli_ZY#1")),
      "metadata.kielivalikoima.muutKielet[0]",
      invalidKieliKoodiUri("muutKielet", "kieli_ZY#1")
    )
  }

  val ammWithId = JulkaistuAmmToteutus.copy(oid = Some(ToteutusOid("1.2.246.562.17.00000000000000000123")))
  "State change" should "succeed from tallennettu to julkaistu" in {
    passesValidation(ammWithId, JulkaistuAmmToteutus.copy(tila = Tallennettu))
  }

  it should "succeed from julkaistu to arkistoitu" in {
    passesValidation(ammWithId.copy(tila = Arkistoitu), JulkaistuAmmToteutus)
  }

  it should "succeed from arkistoitu to julkaistu" in {
    passesValidation(ammWithId, JulkaistuAmmToteutus.copy(tila = Arkistoitu))
  }

  it should "succeed from julkaistu to tallennettu" in {
    passesValidation(ammWithId.copy(tila = Tallennettu), JulkaistuAmmToteutus)
  }

  it should "succeed from tallennettu to poistettu when no existing hakukohteet for toteutus" in {
    passesValidation(
      ammWithId.copy(tila = Poistettu, oid = Some(toteutusOid)),
      JulkaistuAmmToteutus.copy(tila = Tallennettu)
    )
  }

  it should "fail from tallennettu to arkistoitu" in {
    failsStageChangeValidation(
      ammWithId.copy(tila = Arkistoitu),
      JulkaistuAmmToteutus.copy(tila = Tallennettu),
      illegalStateChange("toteutukselle", Tallennettu, Arkistoitu)
    )
  }

  it should "fail from arkistoitu to tallennettu" in {
    failsStageChangeValidation(
      ammWithId.copy(tila = Tallennettu),
      JulkaistuAmmToteutus.copy(tila = Arkistoitu),
      illegalStateChange("toteutukselle", Arkistoitu, Tallennettu)
    )
  }

  it should "fail from julkaistu to poistettu" in {
    failsStageChangeValidation(
      ammWithId.copy(tila = Poistettu),
      JulkaistuAmmToteutus,
      illegalStateChange("toteutukselle", Julkaistu, Poistettu)
    )
  }

  it should "fail from arkistoitu to poistettu" in {
    failsStageChangeValidation(
      ammWithId.copy(tila = Poistettu),
      JulkaistuAmmToteutus.copy(tila = Arkistoitu),
      illegalStateChange("toteutukselle", Arkistoitu, Poistettu)
    )
  }

  it should "fail from poistettu to tallennettu" in {
    failsStageChangeValidation(
      ammWithId.copy(tila = Tallennettu),
      JulkaistuAmmToteutus.copy(tila = Poistettu),
      illegalStateChange("toteutukselle", Poistettu, Tallennettu)
    )
  }

  it should "fail from tallennettu to poistettu when existing hakukohteet for toteutus" in {
    failsStageChangeValidation(
      ammWithId.copy(tila = Poistettu, oid = Some(toteutusOid2)),
      JulkaistuAmmToteutus.copy(tila = Tallennettu),
      integrityViolationMsg("Toteutusta", "hakukohteita")
    )
  }

  "Julkaisu" should "fail when changing state to julkaistu and invalid koulutuksen alkamiskausi" in {
    val yearAgo   = LocalDateTime.now().minusYears(1)
    val startDate = yearAgo.minusDays(3)
    val endDate   = yearAgo.minusDays(1)
    failsStageChangeValidation(
      ammToteutusWithKoulutuksenAlkamiskausi(Some(startDate), Some(endDate))
        .copy(oid = Some(ToteutusOid("1.2.246.562.17.00000000000000000123"))),
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
    failsValidation(
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
    passesValidation(
      ammToteutusWithKoulutuksenAlkamiskausi(None, None, startYear = Some(LocalDateTime.now().getYear.toString))
        .copy(oid = Some(ToteutusOid("1.2.246.562.17.00000000000000000123"))),
      JulkaistuAmmToteutus.copy(tila = Tallennettu)
    )
  }

  it should "succeed when modifying already julkaistu toteutus" in {
    passesValidation(
      ammToteutusWithKoulutuksenAlkamiskausi(Some(inPast(2000)), Some(inPast(1000)))
        .copy(oid = Some(ToteutusOid("1.2.246.562.17.00000000000000000123"))),
      JulkaistuAmmToteutus
    )
  }

  it should "succeed when state changes from arkistoitu to julkaistu, eventhough timestamps not in future" in {
    passesValidation(
      ammToteutusWithKoulutuksenAlkamiskausi(Some(inPast(2000)), Some(inPast(1000)))
        .copy(oid = Some(ToteutusOid("1.2.246.562.17.00000000000000000123"))),
      JulkaistuAmmToteutus.copy(tila = Arkistoitu)
    )
  }

  it should "fail if ammatillinenPerustutkintoErityisopetuksena is true and koulutustyyppi does not have relation to koulutustyyppi_1" in {
    failsValidation(
      JulkaistuAmmToteutus.copy(
        koulutusOid = koulutusOid1,
        koulutuksetKoodiUri = Seq(invalidKoulutuksetKoodiUri),
        metadata = Some(AmmToteutuksenMetatieto.copy(ammatillinenPerustutkintoErityisopetuksena = Some(true)))
      ),
      Seq(
        ValidationError(
          "koulutuksetKoodiUri[0]",
          invalidKoulutustyyppiKoodiForAmmatillinenPerustutkintoErityisopetuksena(invalidKoulutuksetKoodiUri)
        )
      )
    )
  }

  it should "succeed if ammatillinenPerustutkintoErityisopetuksena is true and koulutustyyppi has valid relation to koulutustyyppi_1" in {
    passesValidation(
      JulkaistuAmmToteutus.copy(
        koulutusOid = koulutusOid2,
        metadata = Some(AmmToteutuksenMetatieto.copy(ammatillinenPerustutkintoErityisopetuksena = Some(true)))
      )
    )
  }

  it should "succeed if no ammatillinenPerustutkintoErityisopetuksena defined and koulutustyyppi has valid relation to koulutustyyppi_1" in {
    passesValidation(
      JulkaistuAmmToteutus.copy(koulutusOid = koulutusOid2, metadata = Some(AmmToteutuksenMetatieto))
    )
  }

  it should "fail if ammatillinenPerustutkintoErityisopetuksena is true and koulutus is not found" in {
    failsValidation(
      JulkaistuAmmToteutus.copy(koulutusOid = invalidKoulutusOid, metadata = Some(AmmToteutuksenMetatieto)),
      "koulutusOid",
      nonExistent("Koulutusta", invalidKoulutusOid)
    )
    failsValidation(
      JulkaistuAmmToteutus.copy(
        koulutusOid = invalidKoulutusOid,
        metadata = Some(AmmToteutuksenMetatieto.copy(ammatillinenPerustutkintoErityisopetuksena = Some(true)))
      ),
      "koulutusOid",
      nonExistent("Koulutusta", invalidKoulutusOid)
    )
  }
}
