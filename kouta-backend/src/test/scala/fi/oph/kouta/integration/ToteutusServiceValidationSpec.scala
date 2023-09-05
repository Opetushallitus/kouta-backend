package fi.oph.kouta.integration

import fi.oph.kouta.TestData._
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.mocks.TestKoodistoElement
import fi.oph.kouta.repository.{HakukohdeDAO, KoulutusDAO, SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.security.{Authority, CasSession, ServiceTicket}
import fi.oph.kouta.service.{KoodistoService, KoutaValidationException, OrganisaatioService, OrganizationAuthorizationFailedException, ToteutusServiceValidation}
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.validation.ExternalQueryResults.{itemFound, itemNotFound}
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{BaseServiceValidationSpec, ValidationError}
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers.contain
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper

import java.net.InetAddress
import java.time.LocalDateTime
import java.util.UUID
import scala.util.{Failure, Try}

class ToteutusServiceValidationSpec extends BaseServiceValidationSpec[Toteutus] {
  val koodistoService     = mock[KoodistoService]
  val organisaatioService = mock[OrganisaatioService]
  val koulutusDao         = mock[KoulutusDAO]
  val hakukohdeDao        = mock[HakukohdeDAO]
  val sorakuvausDao       = mock[SorakuvausDAO]
  val toteutusDao         = mock[ToteutusDAO]
  val lukioToteutus = LukioToteutus.copy(koulutusOid = KoulutusOid("1.2.246.562.13.125"), nimi = Map())
  val lukioDIAKoulutus =
    LukioKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.122")), koulutuksetKoodiUri = Seq("koulutus_301103"))
  val lukioDIAToteutus =
    LukioToteutus.copy(koulutusOid = lukioDIAKoulutus.oid.get, nimi = Map(Fi -> "DIA", Sv -> "DIA sv"))
  val ammTutkinnonOsaToteutus = AmmTutkinnonOsaToteutus.copy(koulutusOid = KoulutusOid("1.2.246.562.13.124"))
  val ammMuuToteutus          = AmmMuuToteutus.copy(koulutusOid = KoulutusOid("1.2.246.562.13.130"))
  val yoToteutus              = JulkaistuYoToteutus.copy(koulutusOid = KoulutusOid("1.2.246.562.13.131"))
  val kkOpintojaksoToteutus   = JulkaistuKkOpintojaksoToteutus.copy(koulutusOid = KoulutusOid("1.2.246.562.13.132"))
  val kkOpintokokonaisuusToteutus =
    JulkaistuKkOpintokokonaisuusToteutus.copy(koulutusOid = KoulutusOid("1.2.246.562.13.133"))
  val kkOpintokokonaisuusKoulutus = KkOpintokokonaisuusKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.133")))
  val kkOpintojaksoKoulutus       = KkOpintojaksoKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.143")))
  val ammOpettajaKoulutus         = AmmOpettajaKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.134")))
  val ammOpettajaToteutus         = JulkaistuAmmOpettajaToteutus.copy(koulutusOid = ammOpettajaKoulutus.oid.get)
  val yoOpettajaKoulutus          = YoOpettajaKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.135")))
  val yoOpettajaToteutus          = JulkaistuYoOpettajaToteutus.copy(koulutusOid = yoOpettajaKoulutus.oid.get)
  val tuvaKoulutus                = TuvaKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.136")))
  val tuvaToteutus                = TuvaToteutus.copy(koulutusOid = tuvaKoulutus.oid.get)
  val telmaKoulutus               = TelmaKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.137")))
  val telmaToteutus               = TelmaToteutus.copy(koulutusOid = telmaKoulutus.oid.get)
  val ammOsaamisalaKoulutus       = AmmOsaamisalaKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.138")))
  val ammOsaamisalaToteutus       = AmmOsaamisalaToteutus.copy(koulutusOid = ammOsaamisalaKoulutus.oid.get)
  val vstOpistovuosiKoulutus      = VapaaSivistystyoOpistovuosiKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.139")))
  val vstOpistovuosiToteutus      = VapaaSivistystyoOpistovuosiToteutus.copy(koulutusOid = vstOpistovuosiKoulutus.oid.get)
  val tpoKoulutus                 = TaiteenPerusopetusKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.140")))
  val tpoToteutus                 = TaiteenPerusopetusToteutus.copy(koulutusOid = tpoKoulutus.oid.get)
  val muuKoulutus                 = MuuKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.141")))
  val muuToteutus                 = JulkaistuMuuToteutus.copy(koulutusOid = muuKoulutus.oid.get)
  val pelastusalanAmmKoulutus     = PelastusalanAmmKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.142")))
  val yoTohtoriKoulutus           = YoKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.143")))
  val pelastusalanAmmToteutus = JulkaistuAmmToteutus.copy(
    koulutusOid = pelastusalanAmmKoulutus.oid.get,
    metadata = Some(AmmToteutuksenMetatieto.copy(osaamisalat = List()))
  )
  val sorakuvausId  = UUID.randomUUID()
  val sorakuvausId2 = UUID.randomUUID()
  val sorakuvausId3 = UUID.randomUUID()
  val sorakuvausId4 = UUID.randomUUID()
  val sorakuvausId5 = UUID.randomUUID()
  val sorakuvausId6 = UUID.randomUUID()
  val toteutusOid  = ToteutusOid("1.2.246.562.17.00000000000000000123")
  val toteutusOid2 = ToteutusOid("1.2.246.562.17.00000000000000000124")
  val toteutusOid3 = ToteutusOid("1.2.246.562.17.00000000000000000125")
  val toteutusOid4 = ToteutusOid("1.2.246.562.17.00000000000000000126")
  val existingToteutus   = JulkaistuAmmToteutus.copy(oid = Some(toteutusOid))
  val koulutusOid1       = KoulutusOid("1.2.246.562.13.00000000000000000997")
  val koulutusOid2       = KoulutusOid("1.2.246.562.13.00000000000000000998")
  val invalidKoulutusOid = KoulutusOid("1.2.246.562.13.00000000000000000999")
  val organisaatioOidCausingFailure = OrganisaatioOid("1.2.246.562.10.66666666666")
  val invalidKoulutuksetKoodiUri = "koulutus_XXX#1"
  val validKoulutuksetKoodiUri   = "koulutus_371101#1"
  val authenticatedNonPaakayttaja = Authenticated(
    UUID.randomUUID().toString,
    CasSession(
      ServiceTicket("ST-123"),
      "1.2.3.1234",
      Set("APP_KOUTA", "APP_KOUTA_TOTEUTUS_READ", s"APP_KOUTA_TOTEUTUS_READ_${ChildOid}").map(Authority(_))
    ),
    "testAgent",
    InetAddress.getByName("127.0.0.1")
  )
  val nimiNotMatchingDefault = Map(Fi -> "eri nimi", Sv -> "eri nimi sv")

  private def ammToteutusWithOpetusParameters(
      opetuskieliKoodiUrit: Seq[String] = Seq("oppilaitoksenopetuskieli_1#1"),
      opetusaikaKoodiUrit: Seq[String] = Seq("opetusaikakk_1#1"),
      opetustapaKoodiUrit: Seq[String] = Seq("opetuspaikkakk_1#1", "opetuspaikkakk_2#1"),
      apuraha: Option[Apuraha] = None,
      lisatiedot: Seq[Lisatieto] = Seq(Lisatieto1),
      maksullisuustyyppi: Option[Maksullisuustyyppi] = Some(Maksullinen)
  ): Toteutus = {
    val opetus: Opetus = ToteutuksenOpetus.copy(
      opetuskieliKoodiUrit = opetuskieliKoodiUrit,
      opetusaikaKoodiUrit = opetusaikaKoodiUrit,
      opetustapaKoodiUrit = opetustapaKoodiUrit,
      apuraha = apuraha,
      lisatiedot = lisatiedot,
      maksullisuustyyppi = maksullisuustyyppi
    )
    JulkaistuAmmToteutus.copy(metadata = Some(AmmToteutuksenMetatieto.copy(opetus = Some(opetus))))
  }

  private def yoToteutusWithOpetusParameters(
      opetuskieliKoodiUrit: Seq[String] = Seq("oppilaitoksenopetuskieli_4#1"),
      opetusaikaKoodiUrit: Seq[String] = Seq("opetusaikakk_1#1"),
      opetustapaKoodiUrit: Seq[String] = Seq("opetuspaikkakk_1#1", "opetuspaikkakk_2#1"),
      onkoApuraha: Boolean = true,
      apuraha: Option[Apuraha] = Some(
        Apuraha(Some(100), Some(200), Some(Euro), Map(Fi -> "apurahakuvaus fi", Sv -> "apurahakuvaus sv"))
      ),
      lisatiedot: Seq[Lisatieto] = Seq(Lisatieto1),
      maksullisuustyyppi: Option[Maksullisuustyyppi] = Some(Lukuvuosimaksu),
      koulutusOid: Option[KoulutusOid] = Some(KoulutusOid("1.2.246.562.13.131"))
  ): Toteutus = {
    val opetus: Opetus = ToteutuksenOpetus.copy(
      opetuskieliKoodiUrit = opetuskieliKoodiUrit,
      opetusaikaKoodiUrit = opetusaikaKoodiUrit,
      opetustapaKoodiUrit = opetustapaKoodiUrit,
      onkoApuraha = onkoApuraha,
      apuraha = apuraha,
      lisatiedot = lisatiedot,
      maksullisuustyyppi = maksullisuustyyppi
    )
    JulkaistuYoToteutus.copy(
      koulutusOid = koulutusOid.get,
      metadata = Some(YoToteutuksenMetatieto.copy(opetus = Some(opetus)))
    )
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
      koodistoService,
      organisaatioService,
      koulutusDao,
      hakukohdeDao,
      sorakuvausDao,
      toteutusDao
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    // yleiset
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(AmmOid)).thenAnswer(
      Seq(AmmOid),
      Seq(Amm, AmmTutkinnonOsa, AmmOsaamisala, AmmMuu, Tuva, Telma, VapaaSivistystyoOpistovuosi, TaiteenPerusopetus)
    )
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(LukioOid)).thenAnswer(Seq(LukioOid), Seq(Lk))
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(YoOid))
      .thenAnswer(Seq(YoOid), Seq(Yo, KkOpintojakso, KkOpintokokonaisuus))
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(HkiYoOid))
      .thenAnswer(Seq(HkiYoOid), Seq(Yo, KkOpintojakso, KkOpintokokonaisuus, Erikoislaakari, OpePedagOpinnot))
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(AmkOid))
      .thenAnswer(Seq(AmkOid), Seq(Amk, AmmOpeErityisopeJaOpo))
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(OtherOid))
      .thenAnswer(Seq(OtherOid), Koulutustyyppi.values)
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(LonelyOid)).thenAnswer(Seq(LonelyOid), Seq())
    when(organisaatioService.getAllChildOidsFlat(ChildOid)).thenAnswer(Seq(ChildOid, GrandChildOid, GrandGrandChildOid))
    when(koodistoService.koodiUriExistsInKoodisto(OpetuskieliKoodisto, "oppilaitoksenopetuskieli_1#1"))
      .thenAnswer(itemFound)
    when(koodistoService.koodiUriExistsInKoodisto(OpetuskieliKoodisto, "oppilaitoksenopetuskieli_4#1"))
      .thenAnswer(itemFound)
    when(koodistoService.koodiUriExistsInKoodisto(OpetusaikaKoodisto, "opetusaikakk_1#1")).thenAnswer(itemFound)
    when(koodistoService.koodiUriExistsInKoodisto(OpetustapaKoodisto, "opetuspaikkakk_1#1")).thenAnswer(itemFound)
    when(koodistoService.koodiUriExistsInKoodisto(OpetustapaKoodisto, "opetuspaikkakk_2#1")).thenAnswer(itemFound)
    when(koodistoService.koodiUriExistsInKoodisto(KoulutuksenLisatiedotKoodisto, "koulutuksenlisatiedot_03#1"))
      .thenAnswer(itemFound)
    when(koodistoService.koodiUriExistsInKoodisto(KausiKoodisto, "kausi_k#1")).thenAnswer(itemFound)

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
      .thenAnswer(Some(KkOpintojaksoKoulutus.copy(oid = Some(KoulutusOid("1.2.246.562.13.132")), tila = Julkaistu)))
    when(koulutusDao.get(kkOpintokokonaisuusKoulutus.oid.get)).thenAnswer(Some(kkOpintokokonaisuusKoulutus))
    // Avoin kk related default mocks
    when(koulutusDao.listTarjoajaOids(KoulutusOid("1.2.246.562.13.133"))).thenReturn(Seq(YoOid, HkiYoOid))
    when(koulutusDao.listTarjoajaOids(KoulutusOid("1.2.246.562.13.132"))).thenReturn(Seq(YoOid, HkiYoOid))
    when(organisaatioService.getAllChildAndParentOidsWithKoulutustyypitFlat(YoOid)).thenAnswer(Seq(YoOid), Seq(Yo))
    when(organisaatioService.getAllChildAndParentOidsWithKoulutustyypitFlat(HkiYoOid))
      .thenAnswer(Seq(HkiYoOid), Seq(Yo))

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
    when(koulutusDao.get(ammOsaamisalaKoulutus.oid.get)).thenAnswer(Some(ammOsaamisalaKoulutus))
    when(koulutusDao.get(vstOpistovuosiKoulutus.oid.get)).thenAnswer(Some(vstOpistovuosiKoulutus))
    when(koulutusDao.get(invalidKoulutusOid)).thenAnswer(None)
    when(koulutusDao.get(lukioDIAKoulutus.oid.get)).thenAnswer(Some(lukioDIAKoulutus))
    when(koulutusDao.get(tpoKoulutus.oid.get)).thenAnswer(Some(tpoKoulutus))
    when(koulutusDao.get(muuKoulutus.oid.get)).thenAnswer(Some(muuKoulutus))
    when(koulutusDao.get(pelastusalanAmmKoulutus.oid.get)).thenAnswer(Some(pelastusalanAmmKoulutus))
    when(koulutusDao.get(yoTohtoriKoulutus.oid.get)).thenAnswer(Some(
      YoKoulutus.copy(tila = Julkaistu, koulutuksetKoodiUri = Seq("koulutus_855101#12"))))

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
    when(koodistoService.koodiUriExistsInKoodisto(OsaamisalaKoodisto, "osaamisala_0001#1")).thenAnswer(itemFound)
    // lukio
    when(koodistoService.koodiUriExistsInKoodisto(LukioPainotuksetKoodisto, "lukiopainotukset_1#1"))
      .thenAnswer(itemFound)
    when(
      koodistoService.koodiUriExistsInKoodisto(
        LukioErityinenKoulutustehtavaKoodisto,
        "lukiolinjaterityinenkoulutustehtava_1#1"
      )
    ).thenAnswer(itemFound)
    when(koodistoService.koodiUriExistsInKoodisto(LukioDiplomiKoodisto, "moduulikoodistolops2021_kald3#1"))
      .thenAnswer(itemFound)
    when(koodistoService.koodiUriExistsInKoodisto(LukioDiplomiKoodisto, "moduulikoodistolops2021_kald3#1"))
      .thenAnswer(itemFound)
    when(
      koodistoService.isInLisattavatKoulutukset(
        AmmatillisetPerustutkintoKoodit.koulutusTyypit,
        validKoulutuksetKoodiUri
      )
    ).thenAnswer(itemFound)
    when(
      koodistoService.isInLisattavatKoulutukset(
        AmmatillisetPerustutkintoKoodit.koulutusTyypit,
        invalidKoulutuksetKoodiUri
      )
    ).thenAnswer(itemNotFound)
    when(koodistoService.koodiUriExistsInKoodisto(KieliKoodisto, "kieli_EN#1")).thenAnswer(itemFound)
    when(koodistoService.koodiUriExistsInKoodisto(KieliKoodisto, "kieli_DE#1")).thenAnswer(itemFound)
    when(koodistoService.koodiUriExistsInKoodisto(KieliKoodisto, "kieli_SV#1")).thenAnswer(itemFound)
    when(koodistoService.koodiUriExistsInKoodisto(KieliKoodisto, "kieli_FR#1")).thenAnswer(itemFound)
    when(koodistoService.koodiUriExistsInKoodisto(KieliKoodisto, "kieli_ES#1")).thenAnswer(itemFound)
    when(koodistoService.koodiUriExistsInKoodisto(KieliKoodisto, "kieli_FI#1")).thenAnswer(itemFound)
    when(koodistoService.koodiUriExistsInKoodisto(KieliKoodisto, "kieli_ET#1")).thenAnswer(itemFound)

    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(ChildOid)).thenAnswer((Seq(ChildOid), Seq(Amk)))
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(PohjoiskalotinKoulutussaatio))
      .thenAnswer((Seq(PohjoiskalotinKoulutussaatio), Seq(Muu)))
    when(organisaatioService.withoutOppilaitostyypit(anySeq[OrganisaatioOid], anySeq[String])).thenReturn(Seq())

    when(koodistoService.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto, "opintojenlaajuusyksikko_6#1"))
      .thenAnswer(itemFound)
    when(koodistoService.koodiUriExistsInKoodisto(TaiteenalaKoodisto, "taiteenperusopetustaiteenala_kuvataide"))
      .thenAnswer(itemFound)
    when(koodistoService.getKoulutuksetByTutkintotyyppi("tutkintotyyppi_16"))
      .thenReturn(Right(Seq(TestKoodistoElement("koulutus_655101", 2, defaultName), TestKoodistoElement("koulutus_755101", 2, defaultName), TestKoodistoElement("koulutus_855101", 2, defaultName))))
  }

  private def failSorakuvausValidation(toteutus: Toteutus): Assertion = {
    val koulutusOid = KoulutusOid("1.2.246.562.13.600")
    when(koulutusDao.get(koulutusOid))
      .thenAnswer(Some(AmmKoulutus.copy(tila = Julkaistu, koulutustyyppi = toteutus.metadata.get.tyyppi)))
    val testedToteutus =
      toteutus.copy(koulutusOid = koulutusOid, tarjoajat = List(OtherOid), sorakuvausId = Some(sorakuvausId))
    failsValidation(testedToteutus, "sorakuvausId", notMissingMsg(Some(sorakuvausId)))
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

  it should "succeed when new valid DIA -lukiototeutus" in {
    passesValidation(lukioDIAToteutus)
  }

  it should "succeed when new valid kk-opintojakso toteutus" in {
    passesValidation(kkOpintojaksoToteutus)
  }

  it should "succeed when new valid TaiteenPerusopetus toteutus" in {
    passesValidation(tpoToteutus)
  }

  it should "succeed when new valid Muu toteutus" in {
    passesValidation(muuToteutus)
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
    failsValidation(MinToteutus.copy(kielivalinta = Seq()), "kielivalinta", missingMsg)
    failsValidation(yoToteutus.copy(nimi = Map(Fi -> "nimi")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(yoToteutus.copy(nimi = Map(Fi -> "nimi", Sv -> "")), "nimi", invalidKielistetty(Seq(Sv)))
    failsValidation(
      lukioToteutus.copy(organisaatioOid = OrganisaatioOid("1.2.3")),
      "organisaatioOid",
      validationMsg("1.2.3")
    )
    failsValidation(lukioToteutus.copy(organisaatioOid = OrganisaatioOid("")), "organisaatioOid", validationMsg(""))
    failsValidation(lukioToteutus.copy(koulutusOid = KoulutusOid("puppu")), "koulutusOid", validationMsg("puppu"))
    failsValidation(lukioToteutus.copy(teemakuva = Some("puppu")), "teemakuva", invalidUrl("puppu"))
    failsValidation(lukioDIAToteutus.copy(nimi = Map()), "nimi", invalidKielistetty(Seq(Fi, Sv)))
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
        ValidationError("nimi", invalidKielistetty(Seq(Fi, Sv))),
        ValidationError("metadata.tyyppi", notModifiableMsg("koulutustyyppiä", "toteutukselle"))
      )
    )
  }

  it should "fail if oid not given in modify operation" in {
    failsModifyValidation(lukioToteutus, lukioToteutus, Seq(ValidationError("oid", missingMsg)))
  }

  it should "fail if metadata missing from julkaistu toteutus" in {
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
        ValidationError("tarjoajat[1]", tarjoajaOidWoRequiredKoulutustyyppi(LonelyOid, Lk))
      )
    )
  }

  it should "fail if organisaatio-service not working when checking tarjoajat" in {
    failsValidation(
      lukioToteutus.copy(tarjoajat = List(organisaatioOidCausingFailure)),
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

  it should "fail if sorakuvaus given for non supported koulutustyyppi" in {
    failSorakuvausValidation(JulkaistuAmmToteutus)
    failSorakuvausValidation(ammMuuToteutus)
    failSorakuvausValidation(yoToteutus)
    failSorakuvausValidation(JulkaistuAmkToteutus)
    failSorakuvausValidation(JulkaistuAmmOpettajaToteutus)
    failSorakuvausValidation(JulkaistuYoOpettajaToteutus)
    failSorakuvausValidation(lukioToteutus)
    failSorakuvausValidation(tuvaToteutus)
    failSorakuvausValidation(telmaToteutus)
    failSorakuvausValidation(vstOpistovuosiToteutus)
    failSorakuvausValidation(VapaaSivistystyoMuuToteutus)
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

  it should "fail if tarjoajat missing from julkaistu toteutus" in {
    failsValidation(lukioToteutus.copy(tarjoajat = List()), "tarjoajat", missingMsg)
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

  "AmmOsaamisalaToteutus validation" should "fail if nimi not matching koulutusnimi" in {
    failsValidation(
      ammOsaamisalaToteutus.copy(nimi = nimiNotMatchingDefault),
      Seq(
        ValidationError("nimi.fi", illegalNameForFixedlyNamedEntityMsg("nimi", "koulutuksessa")),
        ValidationError("nimi.sv", illegalNameForFixedlyNamedEntityMsg("nimi sv", "koulutuksessa"))
      )
    )
  }

  "AmmTutkinnonosaToteutus validation" should "pass if nimi not matching koulutusnimi" in {
    passesValidation(ammTutkinnonOsaToteutus.copy(nimi = nimiNotMatchingDefault))
  }

  it should "fail if nimi not matching koulutusnimi" in {
    failsValidation(
      tuvaToteutus.copy(nimi = nimiNotMatchingDefault),
      Seq(
        ValidationError("nimi.fi", illegalNameForFixedlyNamedEntityMsg("nimi", "koulutuksessa")),
        ValidationError("nimi.sv", illegalNameForFixedlyNamedEntityMsg("nimi sv", "koulutuksessa"))
      )
    )
  }

  it should "fail if nimi not matching koulutusnimi" in {
    failsValidation(
      telmaToteutus.copy(nimi = nimiNotMatchingDefault),
      Seq(
        ValidationError("nimi.fi", illegalNameForFixedlyNamedEntityMsg("nimi", "koulutuksessa")),
        ValidationError("nimi.sv", illegalNameForFixedlyNamedEntityMsg("nimi sv", "koulutuksessa"))
      )
    )
  }

  "Vapaa sivistystyö opistovuosi" should "fail if nimi not matching koulutusnimi" in {
    failsValidation(
      vstOpistovuosiToteutus.copy(nimi = nimiNotMatchingDefault),
      Seq(
        ValidationError("nimi.fi", illegalNameForFixedlyNamedEntityMsg("nimi", "koulutuksessa")),
        ValidationError("nimi.sv", illegalNameForFixedlyNamedEntityMsg("nimi sv", "koulutuksessa"))
      )
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
            aloituspaikat = Some(-1),
            aloituspaikkakuvaus = Map(Fi -> "aloituspaikkakuvaus")
          )
        )
      ),
      Seq(
        ValidationError("metadata.hakulomakeLinkki.fi", invalidUrl("puppu fi")),
        ValidationError("metadata.hakulomakeLinkki.sv", invalidUrl("puppu sv")),
        ValidationError("metadata.hakuaika", invalidAjanjaksoMsg(ajanjakso)),
        ValidationError("metadata.aloituspaikat", notNegativeMsg),
        ValidationError("metadata.aloituspaikkakuvaus", invalidKielistetty(Seq(Sv)))
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

  "validateApuraha" should "fail if invalid apuraha" in {
    val validKuvaus = Map(Fi -> "kuvaus fi", Sv -> "kuvaus sv")
    failsValidation(
      yoToteutusWithOpetusParameters(apuraha = Some(Apuraha(Some(-100), Some(-200), Some(Euro), validKuvaus))),
      Seq(
        ValidationError("metadata.opetus.apuraha.min", minmaxMsg(-100, -200)),
        ValidationError("metadata.opetus.apuraha.min", notNegativeMsg),
        ValidationError("metadata.opetus.apuraha.max", notNegativeMsg)
      )
    )
    failsValidation(
      yoToteutusWithOpetusParameters(apuraha = Some(Apuraha(Some(50), Some(200), Some(Prosentti), validKuvaus))),
      "metadata.opetus.apuraha.max",
      lessOrEqualMsg(200, 100)
    )
    failsValidation(
      yoToteutusWithOpetusParameters(apuraha = Some(Apuraha(None, None, None, Map(Fi -> "vain suomeksi")))),
      Seq(
        ValidationError("metadata.opetus.apuraha.kuvaus", invalidKielistetty(Seq(Sv))),
        ValidationError("metadata.opetus.apuraha.min", missingMsg),
        ValidationError("metadata.opetus.apuraha.max", missingMsg)
      )
    )
  }

  it should "fail if maksullisuustyyppi is not lukuvuosimaksu and apuraha is defined" in {
    failsValidation(
      yoToteutusWithOpetusParameters(
        opetuskieliKoodiUrit = Seq("oppilaitoksenopetuskieli_4#1"),
        maksullisuustyyppi = Some(Maksullinen)
      ),
      Seq(
        ValidationError("metadata.opetus.apuraha", invalidMaksullisuustyyppiWithApuraha)
      )
    )
  }

  it should "fail if onkoApuraha is true for opetus with no apuraha" in {
    failsValidation(
      yoToteutusWithOpetusParameters(
        opetuskieliKoodiUrit = Seq("oppilaitoksenopetuskieli_4#1"),
        onkoApuraha = true,
        apuraha = None
      ),
      Seq(
        ValidationError("metadata.opetus.apuraha", missingMsg)
      )
    )
  }

  "validateMaksullisuus" should "fail if lukuvuosimaksu is selected and koulutustyyppi is not yo or amk" in {
    failsValidation(
      ammToteutusWithOpetusParameters(
        opetuskieliKoodiUrit = Seq("oppilaitoksenopetuskieli_4#1"),
        maksullisuustyyppi = Some(Lukuvuosimaksu)
      ),
      Seq(
        ValidationError("metadata.opetus.maksullisuustyyppi", invalidKoulutustyyppiWithLukuvuosimaksuMsg(Amm))
      )
    )
  }

  it should "fail if lukuvuosimaksu is selected and koulutus is tohtorikoulutus" in {
    failsValidation(
      yoToteutusWithOpetusParameters(
        maksullisuustyyppi = Some(Lukuvuosimaksu),
        koulutusOid = Some(yoTohtoriKoulutus.oid.get)
      ),
      Seq(
        ValidationError("metadata.opetus.maksullisuustyyppi", invalidKoulutusWithLukuvuosimaksu(Seq("koulutus_855101")))
      )
    )
  }

  it should "fail with error message if koodistoservice is down" in {
    when(koodistoService.getKoulutuksetByTutkintotyyppi("tutkintotyyppi_16"))
      .thenReturn(Left(new RuntimeException()))
    failsValidation(
      yoToteutusWithOpetusParameters(
        maksullisuustyyppi = Some(Lukuvuosimaksu),
        koulutusOid = Some(yoTohtoriKoulutus.oid.get)
      ),
      Seq(
        ValidationError("metadata.opetus.maksullisuustyyppi", koodistoServiceFailureMsg)
      )
    )
  }

  it should "fail if lukuvuosimaksu is selected and English is not set as opetuskieli" in {
    failsValidation(
      yoToteutusWithOpetusParameters(opetuskieliKoodiUrit = Seq("oppilaitoksenopetuskieli_1#1")),
      Seq(
        ValidationError("metadata.opetus.maksullisuustyyppi", invalidOpetuskieliWithLukuvuosimaksu)
      )
    )
  }


  it should "allow aloituspaikat for Taiteen perusopetus" in {
    passesValidation(tpoToteutus.copy(metadata =
      Some(
        TaiteenPerusopetusToteutusMetatieto.copy(
          aloituspaikat = Some(10),
          aloituspaikkakuvaus = Map(Fi -> "aloituspaikkakuvaus", Sv -> "aloituspaikkakuvaus sv")
        ))))
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

  it should "fail if opintojen laajuus not in the range given for koulutus" in {
    val toteutuksenLaajuus = Some(25.0)
    failsValidation(
      kkOpintojaksoToteutus.copy(metadata =
        Some(KkOpintojaksoToteutuksenMetatieto.copy(opintojenLaajuusNumero = toteutuksenLaajuus))
      ),
      "metadata.opintojenLaajuusNumero",
      notInTheRangeMsg(
        KkOpintojaksoKoulutuksenMetatieto.opintojenLaajuusNumeroMin,
        KkOpintojaksoKoulutuksenMetatieto.opintojenLaajuusNumeroMax,
        toteutuksenLaajuus
      )
    )
  }

  it should "fail if opintojen laajuusyksikko is different in koulutus" in {
    val koulutusLaajuusyksikko = kkOpintojaksoKoulutus.metadata.get
      .asInstanceOf[KkOpintojaksoKoulutusMetadata]
      .opintojenLaajuusyksikkoKoodiUri
    val toteutusLaajuusyksikko = Some("opintojenlaajuusyksikko_5#1")
    failsValidation(
      kkOpintojaksoToteutus.copy(metadata =
        Some(KkOpintojaksoToteutuksenMetatieto.copy(opintojenLaajuusyksikkoKoodiUri = toteutusLaajuusyksikko))
      ),
      "metadata.opintojenLaajuusyksikkoKoodiUri",
      invalidToteutusOpintojenLaajuusyksikkoIntegrity(
        koulutusLaajuusyksikko,
        toteutusLaajuusyksikko
      )
    )
  }

  it should "pass with any opintojenLaajuusyksikko when related koulutus opintojenlaajuusyksikko is not defined" in {
    val opintojaksoKoulutusOid = KoulutusOid("1.2.246.562.13.134")
    when(koulutusDao.get(opintojaksoKoulutusOid))
      .thenAnswer(
        Some(
          kkOpintojaksoKoulutus
            .copy(metadata = Some(KkOpintojaksoKoulutuksenMetatieto.copy(opintojenLaajuusyksikkoKoodiUri = None)))
        )
      )
    passesValidation(kkOpintojaksoToteutus.copy(koulutusOid = opintojaksoKoulutusOid))
  }

  it should "pass with any otherwise valid opintojenLaajuusNumero when related koulutus opintojenlaajuus range is not defined" in {
    val opintojaksoKoulutusOid = KoulutusOid("1.2.246.562.13.134")
    when(koulutusDao.get(opintojaksoKoulutusOid))
      .thenAnswer(
        Some(
          kkOpintojaksoKoulutus
            .copy(metadata =
              Some(
                KkOpintojaksoKoulutuksenMetatieto.copy(
                  opintojenLaajuusNumeroMin = None,
                  opintojenLaajuusNumeroMax = None
                )
              )
            )
        )
      )
    passesValidation(kkOpintojaksoToteutus.copy(koulutusOid = opintojaksoKoulutusOid))
  }

  it should "pass when opintojenLaajuusNumero within related koulutus partial range (only min or max defined)" in {
    val opintojaksoKoulutusOid = KoulutusOid("1.2.246.562.13.134")
    when(koulutusDao.get(opintojaksoKoulutusOid))
      .thenAnswer(
        Some(
          kkOpintojaksoKoulutus
            .copy(metadata =
              Some(
                KkOpintojaksoKoulutuksenMetatieto.copy(
                  opintojenLaajuusNumeroMin = None
                )
              )
            )
        )
      )

    passesValidation(kkOpintojaksoToteutus.copy(koulutusOid = opintojaksoKoulutusOid))

    when(koulutusDao.get(opintojaksoKoulutusOid))
      .thenAnswer(
        Some(
          kkOpintojaksoKoulutus
            .copy(metadata =
              Some(
                KkOpintojaksoKoulutuksenMetatieto.copy(
                  opintojenLaajuusNumeroMax = None
                )
              )
            )
        )
      )

    passesValidation(kkOpintojaksoToteutus.copy(koulutusOid = opintojaksoKoulutusOid))
  }

  it should "pass when koulutus has isAvoinKorkeakoulutus = false" in {
    when(koulutusDao.get(kkOpintojaksoToteutus.koulutusOid))
      .thenReturn(
        Some(
          KkOpintojaksoKoulutus.copy(
            oid = Some(kkOpintojaksoToteutus.koulutusOid),
            metadata = Some(KkOpintojaksoKoulutuksenMetatieto.copy(isAvoinKorkeakoulutus = Some(false)))
          )
        )
      )

    passesValidation(
      kkOpintojaksoToteutus.copy(metadata =
        Some(KkOpintojaksoToteutuksenMetatieto.copy(isAvoinKorkeakoulutus = Some(false)))
      )
    )

    passesValidation(
      kkOpintojaksoToteutus.copy(metadata =
        Some(KkOpintojaksoToteutuksenMetatieto.copy(isAvoinKorkeakoulutus = Some(true)))
      )
    )
  }

  it should "fail with isAvoinKorkeakoulutus = false when corresponding koulutus value is true" in {
    val opintojaksoToteutusWithOid = kkOpintojaksoToteutus.copy(
      tarjoajat = List(YoOid),
      metadata = Some(KkOpintojaksoToteutuksenMetatieto.copy(isAvoinKorkeakoulutus = Some(false)))
    )
    when(koulutusDao.get(opintojaksoToteutusWithOid.koulutusOid))
      .thenReturn(
        Some(
          KkOpintojaksoKoulutus.copy(
            oid = Some(opintojaksoToteutusWithOid.koulutusOid),
            tarjoajat = List(YoOid),
            metadata = Some(KkOpintojaksoKoulutuksenMetatieto.copy(isAvoinKorkeakoulutus = Some(true)))
          )
        )
      )
    when(koulutusDao.listTarjoajaOids(opintojaksoToteutusWithOid.koulutusOid)).thenReturn(Seq(YoOid))
    when(organisaatioService.getAllChildAndParentOidsWithKoulutustyypitFlat(YoOid)).thenReturn((Seq(YoOid), Seq(Yo)))

    failsValidation(
      opintojaksoToteutusWithOid,
      "metadata.isAvoinKorkeakoulutus",
      invalidIsAvoinKorkeakoulutusIntegrity
    )
  }

  it should "fail validation if avoin kk -toteutus has different jarjestaja than what is allowed in koulutus" in {
    val opintojaksoToteutusWithOid = kkOpintojaksoToteutus.copy(
      tarjoajat = List(AmkOid, YoOid),
      metadata = Some(KkOpintojaksoToteutuksenMetatieto.copy(isAvoinKorkeakoulutus = Some(true)))
    )
    when(koulutusDao.get(opintojaksoToteutusWithOid.koulutusOid))
      .thenReturn(
        Some(
          KkOpintojaksoKoulutus.copy(
            oid = Some(opintojaksoToteutusWithOid.koulutusOid),
            tarjoajat = List(YoOid),
            metadata = Some(KkOpintojaksoKoulutuksenMetatieto.copy(isAvoinKorkeakoulutus = Some(true)))
          )
        )
      )
    when(koulutusDao.listTarjoajaOids(opintojaksoToteutusWithOid.koulutusOid)).thenReturn(Seq(YoOid))
    when(organisaatioService.getAllChildAndParentOidsWithKoulutustyypitFlat(YoOid)).thenAnswer(Seq(YoOid), Seq(Yo))
    when(organisaatioService.getAllChildAndParentOidsWithKoulutustyypitFlat(AmkOid)).thenAnswer(Seq(AmkOid), Seq(Amk))

    failsValidation(
      opintojaksoToteutusWithOid,
      "tarjoajat",
      invalidJarjestajaForAvoinKorkeakoulutus(List(AmkOid))
    )
  }

  it should "fail validation if avoin kk -toteutus does not have any allowed tarjoajat" in {
    val opintojaksoToteutusWithOid = kkOpintojaksoToteutus.copy(
      tarjoajat = List(AmkOid, YoOid),
      metadata = Some(KkOpintojaksoToteutuksenMetatieto.copy(isAvoinKorkeakoulutus = Some(true)))
    )
    when(koulutusDao.get(opintojaksoToteutusWithOid.koulutusOid))
      .thenReturn(
        Some(
          KkOpintojaksoKoulutus.copy(
            oid = Some(opintojaksoToteutusWithOid.koulutusOid),
            tarjoajat = List(),
            metadata = Some(KkOpintojaksoKoulutuksenMetatieto.copy(isAvoinKorkeakoulutus = Some(true)))
          )
        )
      )
    when(koulutusDao.listTarjoajaOids(opintojaksoToteutusWithOid.koulutusOid)).thenReturn(Seq())
    when(organisaatioService.getAllChildAndParentOidsWithKoulutustyypitFlat(YoOid)).thenAnswer(Seq(YoOid), Seq(Yo))

    failsValidation(
      opintojaksoToteutusWithOid,
      "tarjoajat",
      invalidJarjestajaForAvoinKorkeakoulutus(List(AmkOid, YoOid))
    )
  }

  it should "pass when saving avoin kk-opintojakso with kansalaisopisto tarjoaja" in {
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(KuopionKansalaisopistoOid))
      .thenAnswer(
        Seq(KuopionKansalaisopistoOid),
        Seq(
          Amm,
          Lk,
          Muu,
          VapaaSivistystyoOpistovuosi,
          VapaaSivistystyoMuu,
          AikuistenPerusopetus
        )
      )

    when(
      organisaatioService.withoutOppilaitostyypit(
        Seq(KuopionKansalaisopistoOid),
        oppilaitostyypitForAvoinKorkeakoulutus
      )
    ).thenReturn(Seq())

    when(koulutusDao.get(kkOpintojaksoToteutus.koulutusOid))
      .thenAnswer(
        Some(
          KkOpintojaksoKoulutus.copy(
            oid = Some(kkOpintojaksoToteutus.koulutusOid),
            tarjoajat = List(YoOid, KuopionKansalaisopistoOid),
            metadata = Some(KkOpintojaksoKoulutuksenMetatieto.copy(isAvoinKorkeakoulutus = Some(true)))
          )
        )
      )

    when(koulutusDao.listTarjoajaOids(kkOpintojaksoToteutus.koulutusOid))
      .thenReturn(Seq(YoOid, KuopionKansalaisopistoOid))
    when(organisaatioService.getAllChildAndParentOidsWithKoulutustyypitFlat(YoOid)).thenAnswer(Seq(YoOid), Seq(Yo))
    when(organisaatioService.getAllChildAndParentOidsWithKoulutustyypitFlat(KuopionKansalaisopistoOid))
      .thenAnswer(Seq(KuopionKansalaisopistoOid), Seq(Muu))

    val oldOpintojaksoToteutus = kkOpintojaksoToteutus.copy(oid = Some(ToteutusOid("1.2.246.562.17.123")))
    val newOpintojaksoToteutus = oldOpintojaksoToteutus.copy(
      tarjoajat = List(KuopionKansalaisopistoOid),
      metadata = Some(KkOpintojaksoToteutuksenMetatieto.copy(isAvoinKorkeakoulutus = Some(true)))
    )

    passesValidation(newOpintojaksoToteutus, oldOpintojaksoToteutus)
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
    val koulutusLaajuusyksikko = KkOpintokokonaisuusKoulutuksenMetatieto.opintojenLaajuusyksikkoKoodiUri
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
    val opintokokonaisuusKoulutusOid = kkOpintokokonaisuusKoulutus.oid.get
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
    val opintokokonaisuusKoulutusOid = kkOpintokokonaisuusKoulutus.oid.get
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
    val opintokokonaisuusKoulutusOid = kkOpintokokonaisuusKoulutus.oid.get
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

  it should "pass if attached toteutus is opintojakso-koulutustyyppi" in {
    val opintojaksoToteutusWithOid = kkOpintojaksoToteutus.copy(oid = Some(toteutusOid2))
    when(toteutusDao.get(List(toteutusOid2)))
      .thenAnswer(
        Seq(opintojaksoToteutusWithOid)
      )

    passesValidation(
      kkOpintokokonaisuusToteutus.copy(metadata =
        Some(KkOpintokokonaisuusToteutuksenMetatieto.copy(liitetytOpintojaksot = Seq(toteutusOid2)))
      )
    )
  }

  def failsOpintojaksotValidation(
      toteutus: Toteutus,
      oldToteutus: Toteutus,
      expected: Seq[ValidationError]
  ): Assertion =
    Try(validator.withValidation(toteutus, Some(oldToteutus), authenticatedNonPaakayttaja)(t => t)) match {
      case Failure(exp: KoutaValidationException) => exp.errorMessages should contain theSameElementsAs expected
      case _                                      => fail("Expecting validation failure, but it succeeded")
    }

  it should "fail if attached toteutus is not opintojakso" in {
    val lukioOppilaitosOid   = ChildOid
    val lukioToteutusOid     = toteutusOid2
    val lukioToteutusWithOid = lukioToteutus.copy(oid = Some(lukioToteutusOid), organisaatioOid = lukioOppilaitosOid)
    when(toteutusDao.get(List(lukioToteutusOid)))
      .thenAnswer(
        Seq(lukioToteutusWithOid)
      )

    val opintokokonaisuusToteutus = kkOpintokokonaisuusToteutus.copy(oid = Some(toteutusOid))
    failsOpintojaksotValidation(
      opintokokonaisuusToteutus.copy(metadata =
        Some(KkOpintokokonaisuusToteutuksenMetatieto.copy(liitetytOpintojaksot = Seq(lukioToteutusOid)))
      ),
      opintokokonaisuusToteutus,
      Seq(
        ValidationError(
          "metadata.liitetytOpintojaksot.koulutustyyppi",
          invalidKoulutustyyppiForLiitettyOpintojakso(Seq(lukioToteutusOid))
        )
      )
    )
  }

  it should "fail if one of the attached toteutus is not julkaistu when opintokokonaisuus is julkaistu" in {
    val opintojaksoToteutus1 = kkOpintojaksoToteutus.copy(oid = Some(toteutusOid))
    val opintojaksoToteutus2 = kkOpintojaksoToteutus.copy(oid = Some(toteutusOid2), tila = Tallennettu)
    when(toteutusDao.get(List(toteutusOid2, toteutusOid)))
      .thenAnswer(
        Seq(opintojaksoToteutus1, opintojaksoToteutus2)
      )

    val opintokokonaisuusToteutus = kkOpintokokonaisuusToteutus.copy(oid = Some(toteutusOid3))
    failsOpintojaksotValidation(
      opintokokonaisuusToteutus.copy(metadata =
        Some(KkOpintokokonaisuusToteutuksenMetatieto.copy(liitetytOpintojaksot = Seq(toteutusOid2, toteutusOid)))
      ),
      opintokokonaisuusToteutus,
      Seq(
        ValidationError(
          "metadata.liitetytOpintojaksot.julkaisutila",
          invalidTilaForLiitettyOpintojaksoOnJulkaisu(Seq(toteutusOid2))
        )
      )
    )
  }

  it should "fail if attached toteutus is Arkistoitu or Poistettu" in {
    val organisaatioOid      = ChildOid
    val opintojaksoToteutus1 = kkOpintojaksoToteutus.copy(oid = Some(toteutusOid), organisaatioOid = organisaatioOid)
    val opintojaksoToteutus2 =
      kkOpintojaksoToteutus.copy(oid = Some(toteutusOid2), organisaatioOid = organisaatioOid, tila = Arkistoitu)
    val opintojaksoToteutus3 =
      kkOpintojaksoToteutus.copy(oid = Some(toteutusOid3), organisaatioOid = organisaatioOid, tila = Poistettu)
    when(toteutusDao.get(List(toteutusOid2, toteutusOid, toteutusOid3)))
      .thenAnswer(
        Seq(opintojaksoToteutus1, opintojaksoToteutus2, opintojaksoToteutus3)
      )

    val opintokokonaisuusToteutus = kkOpintokokonaisuusToteutus.copy(oid = Some(toteutusOid4), tila = Tallennettu)
    failsOpintojaksotValidation(
      opintokokonaisuusToteutus.copy(metadata =
        Some(
          KkOpintokokonaisuusToteutuksenMetatieto.copy(liitetytOpintojaksot =
            Seq(toteutusOid2, toteutusOid, toteutusOid3)
          )
        )
      ),
      opintokokonaisuusToteutus,
      Seq(
        ValidationError(
          "metadata.liitetytOpintojaksot.tila",
          invalidTilaForLiitettyOpintojakso(Seq(toteutusOid2, toteutusOid3))
        )
      )
    )
  }

  it should "pass if attached opintojakso is Tallennettu or Julkaistu when opintokokonaisuus is Tallennettu" in {
    val opintojaksoToteutus1 = kkOpintojaksoToteutus.copy(oid = Some(toteutusOid))
    val opintojaksoToteutus2 = kkOpintojaksoToteutus.copy(oid = Some(toteutusOid2), tila = Tallennettu)
    when(toteutusDao.get(List(toteutusOid2, toteutusOid)))
      .thenAnswer(
        Seq(opintojaksoToteutus1, opintojaksoToteutus2)
      )

    passesValidation(
      kkOpintokokonaisuusToteutus.copy(
        tila = Tallennettu,
        metadata =
          Some(KkOpintokokonaisuusToteutuksenMetatieto.copy(liitetytOpintojaksot = Seq(toteutusOid2, toteutusOid)))
      )
    )
  }

  it should "fail if the attached opintojakso does not exist" in {
    val opintojaksoToteutus1 = kkOpintojaksoToteutus.copy(oid = Some(toteutusOid))
    when(toteutusDao.get(List(toteutusOid, toteutusOid2)))
      .thenAnswer(
        Seq(opintojaksoToteutus1)
      )

    val opintokokonaisuusToteutus = kkOpintokokonaisuusToteutus.copy(oid = Some(toteutusOid4))
    failsOpintojaksotValidation(
      opintokokonaisuusToteutus.copy(metadata =
        Some(KkOpintokokonaisuusToteutuksenMetatieto.copy(liitetytOpintojaksot = Seq(toteutusOid, toteutusOid2)))
      ),
      opintokokonaisuusToteutus,
      Seq(
        ValidationError("metadata.liitetytOpintojaksot.notFound", unknownOpintojakso(Seq(toteutusOid2)))
      )
    )
  }

  it should "fail if one of the attached opintojaksot does not belong to the same organization as opintokokonaisuus" in {
    val opintojaksoOppilaitosOid = GrandChildOid
    val opintojaksoToteutusWithOid =
      kkOpintojaksoToteutus.copy(oid = Some(toteutusOid), organisaatioOid = opintojaksoOppilaitosOid)
    val lukioOid             = ChildOid
    val lukioToteutusWithOid = lukioToteutus.copy(oid = Some(toteutusOid2), organisaatioOid = lukioOid)

    when(toteutusDao.get(List(toteutusOid2, toteutusOid)))
      .thenAnswer(
        Seq(lukioToteutusWithOid, opintojaksoToteutusWithOid)
      )
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(opintojaksoOppilaitosOid))
      .thenAnswer(
        (Seq(opintojaksoOppilaitosOid), Seq(Amk))
      )
    when(organisaatioService.getAllChildOidsAndKoulutustyypitFlat(lukioOid))
      .thenAnswer(
        (Seq(lukioOid), Seq(Lk))
      )

    val opintokokonaisuusToteutus = kkOpintokokonaisuusToteutus.copy(oid = Some(toteutusOid3))
    assertThrows[OrganizationAuthorizationFailedException] {
      validator.withValidation(
        opintokokonaisuusToteutus.copy(metadata =
          Some(KkOpintokokonaisuusToteutuksenMetatieto.copy(liitetytOpintojaksot = Seq(toteutusOid2, toteutusOid)))
        ),
        Some(opintokokonaisuusToteutus),
        authenticatedNonPaakayttaja
      )(t => t)
    }
  }

  "Muu validation" should "fail if ammattinimikkeet given" in {
    failsValidation(
      muuToteutus.copy(metadata = Some(MuuToteutuksenMetatieto.copy(ammattinimikkeet = List(Keyword(Fi, "nimike"))))),
      "metadata.ammattinimikkeet",
      notEmptyMsg
    )
  }

  it should "fail if opintojen laajuus range extends outside the range given in koulutus" in {
    failsValidation(
      muuToteutus.copy(metadata =
        Some(MuuToteutuksenMetatieto.copy(opintojenLaajuusNumeroMin = Some(5), opintojenLaajuusNumeroMax = Some(50)))
      ),
      Seq(
        ValidationError(
          "metadata.opintojenLaajuusNumeroMin",
          invalidToteutusOpintojenLaajuusMin(
            MuuKoulutuksenMetatieto.opintojenLaajuusNumeroMin,
            Some(5)
          )
        ),
        ValidationError(
          "metadata.opintojenLaajuusNumeroMax",
          invalidToteutusOpintojenLaajuusMax(
            MuuKoulutuksenMetatieto.opintojenLaajuusNumeroMax,
            Some(50)
          )
        )
      )
    )
  }

  it should "fail if opintojen laajuusyksikko is different in koulutus" in {
    val koulutusLaajuusyksikko = MuuToteutuksenMetatieto.opintojenLaajuusyksikkoKoodiUri
    val toteutusLaajuusyksikko = Some("opintojenlaajuusyksikko_5#1")
    failsValidation(
      muuToteutus.copy(metadata =
        Some(MuuToteutuksenMetatieto.copy(opintojenLaajuusyksikkoKoodiUri = toteutusLaajuusyksikko))
      ),
      "metadata.opintojenLaajuusyksikkoKoodiUri",
      invalidToteutusOpintojenLaajuusyksikkoIntegrity(
        koulutusLaajuusyksikko,
        toteutusLaajuusyksikko
      )
    )
  }

  it should "pass with any opintojenLaajuusyksikko when related koulutus opintojenlaajuusyksikko is not defined" in {
    val muuKoulutusOid = muuKoulutus.oid.get
    when(koulutusDao.get(muuKoulutusOid))
      .thenAnswer(
        Some(
          muuKoulutus
            .copy(metadata = Some(MuuKoulutuksenMetatieto.copy(opintojenLaajuusyksikkoKoodiUri = None)))
        )
      )
    passesValidation(muuToteutus.copy(koulutusOid = muuKoulutusOid))
  }

  it should "pass with any otherwise valid opintojenLaajuus range when related koulutus opintojenlaajuus range is not defined" in {
    val muuKoulutusOid = muuKoulutus.oid.get
    when(koulutusDao.get(muuKoulutusOid))
      .thenAnswer(
        Some(
          muuKoulutus
            .copy(metadata =
              Some(
                MuuKoulutuksenMetatieto.copy(
                  opintojenLaajuusNumeroMin = None,
                  opintojenLaajuusNumeroMax = None
                )
              )
            )
        )
      )
    passesValidation(muuToteutus.copy(koulutusOid = muuKoulutusOid))
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

  "TaiteenPerusopetusToteutus validation" should "fail if invalid opintojenLaajuus" in {
    failsValidation(
      tpoToteutus.copy(metadata =
        Some(
          TaiteenPerusopetusToteutusMetatieto.copy(
            opintojenLaajuusyksikkoKoodiUri = Some("puppu"),
            opintojenLaajuusNumeroMin = Some(-1.0),
            opintojenLaajuusNumeroMax = Some(-5.0)
          )
        )
      ),
      Seq(
        ValidationError("metadata.opintojenLaajuusyksikkoKoodiUri", invalidOpintojenLaajuusyksikkoKoodiuri("puppu")),
        ValidationError("metadata.opintojenLaajuusNumeroMin", notNegativeMsg),
        ValidationError("metadata.opintojenLaajuusNumeroMax", notNegativeMsg),
        ValidationError("metadata.opintojenLaajuusNumeroMin", minmaxMsg(-1.0, -5.0))
      )
    )
  }

  it should "fail if invalid taiteenalaKoodiUrit" in {
    failsValidation(
      tpoToteutus.copy(metadata = Some(TaiteenPerusopetusToteutusMetatieto.copy(taiteenalaKoodiUrit = Seq("puppu")))),
      "metadata.taiteenalaKoodiUrit[0]",
      invalidTaiteenPerusopetusTaiteenalaKoodiuri("puppu")
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
    val startDate = yearAgo.minusDays(1)
    val endDate   = yearAgo.plusDays(1)
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
    val startDate = yearAgo.minusDays(1)
    val endDate   = yearAgo.plusDays(1)
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

  it should "pass validation of pelastusalan amm koulutus with empty osaamisalat" in {
    passesValidation(pelastusalanAmmToteutus)
  }
}
