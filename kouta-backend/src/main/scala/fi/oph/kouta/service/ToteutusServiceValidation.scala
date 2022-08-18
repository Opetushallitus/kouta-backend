package fi.oph.kouta.service

import fi.oph.kouta.client.KoulutusKoodiClient
import fi.oph.kouta.client.HakuKoodiClient
import fi.oph.kouta.domain._
import fi.oph.kouta.repository.{HakukohdeDAO, KoulutusDAO, SorakuvausDAO}
import fi.oph.kouta.validation
import fi.oph.kouta.validation.CrudOperations.CrudOperation
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, NoErrors}

import java.util.regex.Pattern

object ToteutusServiceValidation
    extends ToteutusServiceValidation(
      KoulutusKoodiClient,
      OrganisaatioServiceImpl,
      HakuKoodiClient,
      KoulutusDAO,
      HakukohdeDAO,
      SorakuvausDAO
    )

class ToteutusServiceValidation(
    val koulutusKoodiClient: KoulutusKoodiClient,
    val organisaatioService: OrganisaatioService,
    hakuKoodiClient: HakuKoodiClient,
    koulutusDAO: KoulutusDAO,
    hakukohdeDAO: HakukohdeDAO,
    val sorakuvausDAO: SorakuvausDAO
) extends KoulutusToteutusValidatingService[Toteutus] {
  override def validateEntity(toteutus: Toteutus, crudOperation: CrudOperation): IsValid = {
    val tila         = toteutus.tila
    val kielivalinta = toteutus.kielivalinta
    val commonErrors = and(
      toteutus.validate(),
      validateTarjoajat(toteutus.tarjoajat),
      validateIfDefined[String](toteutus.teemakuva, assertValidUrl(_, "teemakuva")),
      validateIfSuccessful(
        assertValid(toteutus.koulutusOid, "koulutusOid"),
        validateKoulutusIntegrity(toteutus)
      )
    )

    val koulutustyyppiSpecificErrors = toteutus.metadata match {
      case Some(metadata) =>
        val koulutustyypitWithMandatoryKuvaus: Set[Koulutustyyppi] =
          Set(AmmMuu, Tuva, Telma, VapaaSivistystyoOpistovuosi, VapaaSivistystyoMuu, KkOpintojakso)

        and(
          validateIfSuccessful(
            validateIfFalse(metadata.allowSorakuvaus, assertNotDefined(toteutus.sorakuvausId, "sorakuvausId")),
            validateSorakuvausIntegrity(toteutus.sorakuvausId, toteutus.tila, metadata.tyyppi, "metadata.tyyppi")
          ),
          validateIfDefined[Opetus](metadata.opetus, opetus => validateOpetus(tila, kielivalinta, opetus)),
          validateIfNonEmpty[Yhteyshenkilo](
            metadata.yhteyshenkilot,
            "metadata.yhteyshenkilot",
            _.validate(tila, kielivalinta, _)
          ),
          validateIfJulkaistu(
            tila,
            and(
              validateIfTrueOrElse(
                koulutustyypitWithMandatoryKuvaus.contains(metadata.tyyppi),
                validateKielistetty(kielivalinta, metadata.kuvaus, "metadata.kuvaus"),
                validateOptionalKielistetty(kielivalinta, metadata.kuvaus, "metadata.kuvaus")
              ),
              assertNotOptional(metadata.opetus, "metadata.opetus")
            )
          ),
          metadata match {
            case ammMetadata: AmmatillinenToteutusMetadata =>
              validateIfNonEmpty[AmmatillinenOsaamisala](
                ammMetadata.osaamisalat,
                "metadata.osaamisalat",
                validateOsaamisala(_, _, tila, kielivalinta)
              )
            case tutkintoonJohtamatonToteutusMetadata: TutkintoonJohtamatonToteutusMetadata =>
              tutkintoonJohtamatonToteutusMetadata match {
                case m: KkOpintojaksoToteutusMetadata =>
                  and(
                    validateTutkintoonJohtamatonMetadata(tila, kielivalinta, m),
                    // Opintojaksolla ei ole ammattinimikkeitä
                    assertEmpty(m.ammattinimikkeet, "metadata.ammattinimikkeet")
                  )
                case _ => validateTutkintoonJohtamatonMetadata(tila, kielivalinta, tutkintoonJohtamatonToteutusMetadata)
              }
            case lkMetadata: LukioToteutusMetadata => validateLukioMetadata(tila, kielivalinta, lkMetadata)
            case _                                 => NoErrors
          }
        )
      case _ => if (toteutus.tila == Julkaistu) error("metadata", missingMsg) else NoErrors
    }

    Seq(commonErrors, koulutustyyppiSpecificErrors).flatten
  }

  private def validateOpetusKoodiUriListItem(
      koodiUrit: Seq[String],
      koodiUriPattern: Pattern,
      relativePath: String,
      koodistoFunc: String => Boolean,
      errorFunc: String => validation.ErrorMessage
  ): IsValid =
    validateIfNonEmpty[String](
      koodiUrit,
      s"metadata.opetus.$relativePath",
      (koodiUri, path) =>
        validateIfSuccessful(
          assertMatch(koodiUri, koodiUriPattern, path),
          assertTrue(koodistoFunc(koodiUri), path, errorFunc(koodiUri))
        )
    )

  private def validateOpetus(tila: Julkaisutila, kielivalinta: Seq[Kieli], opetus: Opetus): IsValid = {
    val path = "metadata.opetus"
    and(
      validateOpetusKoodiUriListItem(
        opetus.opetuskieliKoodiUrit,
        OpetuskieliKoodiPattern,
        "opetuskieliKoodiUrit",
        koulutusKoodiClient.opetusKieliKoodiUriExists,
        invalidOpetusKieliKoodiUri
      ),
      validateOpetusKoodiUriListItem(
        opetus.opetusaikaKoodiUrit,
        OpetusaikaKoodiPattern,
        "opetusaikaKoodiUrit",
        koulutusKoodiClient.opetusAikaKoodiUriExists,
        invalidOpetusAikaKoodiUri
      ),
      validateOpetusKoodiUriListItem(
        opetus.opetustapaKoodiUrit,
        OpetustapaKoodiPattern,
        "opetustapaKoodiUrit",
        koulutusKoodiClient.opetusTapaKoodiUriExists,
        invalidOpetusTapaKoodiUri
      ),
      validateIfDefined[KoulutuksenAlkamiskausi](
        opetus.koulutuksenAlkamiskausi,
        _.validate(tila, kielivalinta, "metadata.opetus.koulutuksenAlkamiskausi", hakuKoodiClient.kausiKoodiUriExists)
      ),
      validateIfDefined[Apuraha](opetus.apuraha, apuraha => validateApuraha(tila, kielivalinta, apuraha)),
      validateIfNonEmpty[Lisatieto](
        opetus.lisatiedot,
        s"$path.lisatiedot",
        (lisatieto, path) =>
          validateIfSuccessful(
            lisatieto.validate(tila, kielivalinta, path),
            assertTrue(
              koulutusKoodiClient.lisatiedotOtsikkoKoodiUriExists(lisatieto.otsikkoKoodiUri),
              path = s"$path.otsikkoKoodiUri",
              invalidOpetusLisatietoOtsikkoKoodiuri(lisatieto.otsikkoKoodiUri)
            )
          )
      ),
      validateIfDefined[Double](opetus.maksunMaara, assertNotNegative(_, s"$path.maksunMaara")),
      validateIfDefined[Int](opetus.suunniteltuKestoVuodet, assertNotNegative(_, s"$path.suunniteltuKestoVuodet")),
      validateIfDefined[Int](
        opetus.suunniteltuKestoKuukaudet,
        assertNotNegative(_, s"$path.suunniteltuKestoKuukaudet")
      ),
      validateIfJulkaistu(
        tila,
        and(
          assertNotEmpty(opetus.opetuskieliKoodiUrit, s"$path.opetuskieliKoodiUrit"),
          assertNotEmpty(opetus.opetusaikaKoodiUrit, s"$path.opetusaikaKoodiUrit"),
          assertNotEmpty(opetus.opetustapaKoodiUrit, s"$path.opetustapaKoodiUrit"),
          validateIfTrue(opetus.onkoApuraha, assertNotOptional(opetus.apuraha, s"$path.apuraha")),
          validateOptionalKielistetty(kielivalinta, opetus.opetuskieletKuvaus, s"$path.opetuskieletKuvaus"),
          validateOptionalKielistetty(kielivalinta, opetus.opetusaikaKuvaus, s"$path.opetusaikaKuvaus"),
          validateOptionalKielistetty(kielivalinta, opetus.opetustapaKuvaus, s"$path.opetustapaKuvaus"),
          assertNotOptional(opetus.maksullisuustyyppi, s"$path.maksullisuustyyppi"),
          validateOptionalKielistetty(kielivalinta, opetus.maksullisuusKuvaus, s"$path.maksullisuusKuvaus"),
          validateIfTrue(
            opetus.maksullisuustyyppi.contains(Maksullinen) || opetus.maksullisuustyyppi.contains(Lukuvuosimaksu),
            assertNotOptional(opetus.maksunMaara, s"$path.maksunMaara")
          ),
          validateOptionalKielistetty(kielivalinta, opetus.suunniteltuKestoKuvaus, s"$path.suunniteltuKestoKuvaus")
        )
      )
    )
  }

  private def validateApuraha(tila: Julkaisutila, kielivalinta: Seq[Kieli], apuraha: Apuraha): IsValid = {
    val path = "metadata.opetus.apuraha"
    val min  = apuraha.min
    val max  = apuraha.max
    and(
      validateMinMax(min, max, s"$path.min"),
      validateIfDefined[Int](min, assertNotNegative(_, s"$path.min")),
      validateIfDefined[Int](max, assertNotNegative(_, s"$path.max")),
      validateIfTrue(
        apuraha.yksikko.orNull == Prosentti,
        validateIfDefined(max, assertLessOrEqual(_, 100, s"$path.max"))
      ),
      validateIfJulkaistu(
        tila,
        and(
          validateOptionalKielistetty(kielivalinta, apuraha.kuvaus, s"$path.kuvaus"),
          assertNotOptional(min, s"$path.min"),
          assertNotOptional(max, s"$path.max")
        )
      )
    )
  }

  private def validateOsaamisala(
      osaamisala: AmmatillinenOsaamisala,
      path: String,
      tila: Julkaisutila,
      kielivalinta: Seq[Kieli]
  ): IsValid = {
    and(
      validateIfNonEmpty(osaamisala.linkki, s"$path.linkki", assertValidUrl _),
      validateIfJulkaistu(
        tila,
        and(
          validateOptionalKielistetty(kielivalinta, osaamisala.linkki, s"$path.linkki"),
          validateOptionalKielistetty(kielivalinta, osaamisala.otsikko, s"$path.otsikko")
        )
      ),
      validateIfSuccessful(
        assertMatch(osaamisala.koodiUri, OsaamisalaKoodiPattern, s"$path.koodiUri"),
        assertTrue(
          koulutusKoodiClient.osaamisalaKoodiUriExists(osaamisala.koodiUri),
          s"$path.koodiUri",
          invalidOsaamisalaKoodiUri(osaamisala.koodiUri)
        )
      )
    )
  }

  private def validateTutkintoonJohtamatonMetadata(
      tila: Julkaisutila,
      kielivalinta: Seq[Kieli],
      m: TutkintoonJohtamatonToteutusMetadata
  ) =
    and(
      validateIfNonEmpty(m.hakulomakeLinkki, "metadata.hakulomakeLinkki", assertValidUrl _),
      validateIfDefined[Ajanjakso](m.hakuaika, _.validate(tila, kielivalinta, "metadata.hakuaika")),
      validateIfDefined[Int](m.aloituspaikat, assertNotNegative(_, "metadata.aloituspaikat")),
      validateIfJulkaistu(
        tila,
        and(
          assertNotOptional(m.hakutermi, "metadata.hakutermi"),
          assertNotOptional(m.hakulomaketyyppi, "metadata.hakulomaketyyppi"),
          validateIfTrue(
            m.hakulomaketyyppi.contains(MuuHakulomake),
            and(
              validateKielistetty(kielivalinta, m.lisatietoaHakeutumisesta, "metadata.lisatietoaHakeutumisesta"),
              validateKielistetty(kielivalinta, m.hakulomakeLinkki, "metadata.hakulomakeLinkki"),
              validateOptionalKielistetty(
                kielivalinta,
                m.lisatietoaValintaperusteista,
                "metadata.lisatietoaValintaperusteista"
              ),
              assertNotOptional(m.hakuaika, "metadata.hakuaika")
            )
          ),
          validateIfTrue(
            m.hakulomaketyyppi.contains(EiSähköistä),
            validateKielistetty(kielivalinta, m.lisatietoaHakeutumisesta, "metadata.lisatietoaHakeutumisesta")
          )
        )
      )
    )

  private def validateKieliKoodit(koodiUrit: Seq[String], relativePath: String): IsValid =
    validateIfNonEmpty[String](
      koodiUrit,
      s"metadata.kielivalikoima.$relativePath",
      (koodiUri, path) =>
        validateIfSuccessful(
          assertMatch(koodiUri, KieliKoodiPattern, path),
          assertTrue(
            hakuKoodiClient.kieliKoodiUriExists(koodiUri),
            path,
            invalidKieliKoodiUri(relativePath, koodiUri)
          )
        )
    )

  private def validateLukioLinjat(
      tila: Julkaisutila,
      kielivalinta: Seq[Kieli],
      linjat: Seq[LukiolinjaTieto],
      relativePath: String,
      koodiUriPattern: Pattern,
      koodistoFunc: String => Boolean
  ): IsValid =
    validateIfNonEmpty[LukiolinjaTieto](
      linjat,
      s"metadata.$relativePath",
      (linja, path) =>
        validateIfSuccessful(
          and(
            validateIfJulkaistu(tila, validateOptionalKielistetty(kielivalinta, linja.kuvaus, s"$path.kuvaus")),
            assertMatch(linja.koodiUri, koodiUriPattern, s"$path.koodiUri")
          ),
          assertTrue(
            koodistoFunc(linja.koodiUri),
            s"$path.koodiUri",
            invalidLukioLinjaKoodiUri(relativePath, linja.koodiUri)
          )
        )
    )

  private def validateLukioMetadata(
      tila: Julkaisutila,
      kielivalinta: Seq[Kieli],
      lkMetadata: LukioToteutusMetadata
  ): IsValid =
    and(
      validateIfDefined[Kielivalikoima](
        lkMetadata.kielivalikoima,
        kv =>
          and(
            validateKieliKoodit(kv.A1Kielet, "A1Kielet"),
            validateKieliKoodit(kv.A2Kielet, "A2Kielet"),
            validateKieliKoodit(kv.B1Kielet, "B1Kielet"),
            validateKieliKoodit(kv.B2Kielet, "B2Kielet"),
            validateKieliKoodit(kv.B3Kielet, "B3Kielet"),
            validateKieliKoodit(kv.aidinkielet, "aidinkielet"),
            validateKieliKoodit(kv.muutKielet, "muutKielet")
          )
      ),
      // Yleislinja täytyy olla true, jos painotukset ja erityisetKoulutustehtavat tyhjiä.
      validateIfTrue(
        lkMetadata.painotukset.isEmpty && lkMetadata.erityisetKoulutustehtavat.isEmpty,
        assertTrue(lkMetadata.yleislinja, "metadata.yleislinja", withoutLukiolinja)
      ),
      validateLukioLinjat(
        tila,
        kielivalinta,
        lkMetadata.painotukset,
        "painotukset",
        LukioPainotusKoodiPattern,
        koulutusKoodiClient.lukioPainotusKoodiUriExists
      ),
      validateLukioLinjat(
        tila,
        kielivalinta,
        lkMetadata.erityisetKoulutustehtavat,
        "erityisetKoulutustehtavat",
        LukioErityinenKoulutustehtavaKoodiPattern,
        koulutusKoodiClient.lukioErityinenKoulutustehtavaKoodiUriExists
      ),
      validateIfNonEmpty[LukiodiplomiTieto](
        lkMetadata.diplomit,
        "metadata.diplomit",
        (diplomi, path) =>
          validateIfSuccessful(
            and(
              assertMatch(diplomi.koodiUri, LukioDiplomiKoodiPattern, s"$path.koodiUri"),
              validateIfNonEmpty(diplomi.linkki, s"$path.linkki", assertValidUrl _),
              validateIfJulkaistu(
                tila,
                and(
                  validateOptionalKielistetty(kielivalinta, diplomi.linkki, s"$path.linkki"),
                  validateOptionalKielistetty(kielivalinta, diplomi.linkinAltTeksti, s"$path.linkinAltTeksti")
                )
              )
            ),
            assertTrue(
              koulutusKoodiClient.lukioDiplomiKoodiUriExists(diplomi.koodiUri),
              s"$path.koodiUri",
              invalidLukioDiplomiKoodiUri(diplomi.koodiUri)
            )
          )
      )
    )

  private def validateKoulutusIntegrity(toteutus: Toteutus): IsValid = {
    val (koulutusTila, koulutusTyyppi) = koulutusDAO.getTilaAndTyyppi(toteutus.koulutusOid)
    and(
      validateDependency(toteutus.tila, koulutusTila, toteutus.koulutusOid, "Koulutusta", "koulutusOid"),
      validateIfDefined[Koulutustyyppi](
        koulutusTyyppi,
        koulutusTyyppi =>
          and(
            validateIfTrue(koulutusTyyppi != Lk, validateKielistetty(toteutus.kielivalinta, toteutus.nimi, "nimi")),
            validateIfDefined[ToteutusMetadata](
              toteutus.metadata,
              toteutusMetadata =>
                assertTrue(
                  koulutusTyyppi == toteutusMetadata.tyyppi,
                  "metadata.tyyppi",
                  tyyppiMismatch("koulutuksen", toteutus.koulutusOid)
                )
            )
          )
      )
    )
  }
  override def validateEntityOnJulkaisu(toteutus: Toteutus, crudOperation: CrudOperation): IsValid = {
    toteutus.metadata match {
      case Some(metadata) =>
        metadata.opetus match {
          case Some(opetus) =>
            opetus.koulutuksenAlkamiskausi match {
              case Some(koulutuksenAlkamiskausi) =>
                koulutuksenAlkamiskausi.validateOnJulkaisu("metadata.opetus.koulutuksenAlkamiskausi")
              case _ => NoErrors
            }
          case _ => NoErrors
        }
      case _ => NoErrors
    }
  }

  override def validateInternalDependenciesWhenDeletingEntity(toteutus: Toteutus): IsValid = assertTrue(
    hakukohdeDAO.listByToteutusOid(toteutus.oid.get, TilaFilter.onlyOlemassaolevat()).isEmpty,
    "tila",
    integrityViolationMsg("Toteutusta", "hakukohteita")
  )
}
