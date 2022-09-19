package fi.oph.kouta.service

import fi.oph.kouta.client.{HakuKoodiClient, KoulutusKoodiClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.ToteutusOid
import fi.oph.kouta.repository.{HakukohdeDAO, KoulutusDAO, SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.util.ToteutusServiceUtil
import fi.oph.kouta.validation
import fi.oph.kouta.validation.CrudOperations.{create, update}
import fi.oph.kouta.validation.ExternalQueryResults.ExternalQueryResult
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{
  IsValid,
  NoErrors,
  ToteutusDiffResolver,
  ValidationContext,
  ValidationError,
  ammatillinenPerustutkintoKoulutustyyppiKoodiUri
}

import java.util.regex.Pattern

object ToteutusServiceValidation
    extends ToteutusServiceValidation(
      KoulutusKoodiClient,
      OrganisaatioServiceImpl,
      HakuKoodiClient,
      KoulutusDAO,
      HakukohdeDAO,
      SorakuvausDAO,
      ToteutusDAO
    )

class ToteutusServiceValidation(
    val koulutusKoodiClient: KoulutusKoodiClient,
    val organisaatioService: OrganisaatioService,
    hakuKoodiClient: HakuKoodiClient,
    koulutusDAO: KoulutusDAO,
    hakukohdeDAO: HakukohdeDAO,
    val sorakuvausDAO: SorakuvausDAO,
    toteutusDAO: ToteutusDAO
) extends KoulutusToteutusValidatingService[Toteutus] {
  override def validateEntity(toteutus: Toteutus, oldToteutus: Option[Toteutus]): IsValid = {
    val commonErrors = and(
      toteutus.validate(),
      validateIfTrueOrElse(
        oldToteutus.isDefined,
        assertNotOptional(toteutus.oid, "oid"),
        assertNotDefined(toteutus.oid, "oid")
      ),
      validateTarjoajat(toteutus.tarjoajat, oldToteutus.map(_.tarjoajat).getOrElse(List())),
      validateIfDefined[String](toteutus.teemakuva, assertValidUrl(_, "teemakuva")),
      validateIfSuccessful(
        assertValid(toteutus.koulutusOid, "koulutusOid"),
        validateKoulutusIntegrity(toteutus)
      )
    )

    val vCtx                 = ValidationContext(toteutus.tila, toteutus.kielivalinta, if (oldToteutus.isDefined) update else create)
    val toteutusDiffResolver = ToteutusDiffResolver(toteutus, oldToteutus)

    val koulutustyyppiSpecificErrors = toteutus.metadata match {
      case Some(metadata) =>
        val koulutustyypitWithMandatoryKuvaus: Set[Koulutustyyppi] =
          Set(AmmMuu, Tuva, Telma, VapaaSivistystyoOpistovuosi, VapaaSivistystyoMuu, KkOpintojakso)

        and(
          assertFalse(
            toteutusDiffResolver.koulutustyyppiChanged(),
            "metadata.tyyppi",
            notModifiableMsg("koulutustyyppiä", "toteutukselle")
          ),
          validateIfSuccessful(
            validateIfFalse(metadata.allowSorakuvaus, assertNotDefined(toteutus.sorakuvausId, "sorakuvausId")),
            validateSorakuvausIntegrity(toteutus.sorakuvausId, toteutus.tila, metadata.tyyppi, "metadata.tyyppi")
          ),
          validateIfDefined[Opetus](
            metadata.opetus,
            opetus => validateOpetus(vCtx, toteutusDiffResolver, opetus)
          ),
          validateIfNonEmpty[Yhteyshenkilo](
            metadata.yhteyshenkilot,
            "metadata.yhteyshenkilot",
            _.validate(vCtx.tila, vCtx.kielivalinta, _)
          ),
          validateIfJulkaistu(
            vCtx.tila,
            and(
              validateIfTrueOrElse(
                koulutustyypitWithMandatoryKuvaus.contains(metadata.tyyppi),
                validateKielistetty(vCtx.kielivalinta, metadata.kuvaus, "metadata.kuvaus"),
                validateOptionalKielistetty(vCtx.kielivalinta, metadata.kuvaus, "metadata.kuvaus")
              ),
              assertNotOptional(metadata.opetus, "metadata.opetus")
            )
          ),
          metadata match {
            case ammMetadata: AmmatillinenToteutusMetadata =>
              and(
                validateIfNonEmptySeq[AmmatillinenOsaamisala](
                  ammMetadata.osaamisalat,
                  toteutusDiffResolver.newAmmatillisetOsaamisalat(),
                  "metadata.osaamisalat",
                  validateOsaamisala(_, _, _, vCtx)
                ),
                validateIfTrue(
                  ammMetadata.ammatillinenPerustutkintoErityisopetuksena.contains(true),
                  validateAmmatillinenPerustutkintoErityisopetuksena(toteutus, "koulutuksetKoodiUri", vCtx)
                )
              )
            case tutkintoonJohtamatonToteutusMetadata: TutkintoonJohtamatonToteutusMetadata =>
              tutkintoonJohtamatonToteutusMetadata match {
                case m: KkOpintojaksoToteutusMetadata =>
                  and(
                    validateTutkintoonJohtamatonMetadata(vCtx.tila, vCtx.kielivalinta, m),
                    // Opintojaksolla ei ole ammattinimikkeitä
                    assertEmpty(m.ammattinimikkeet, "metadata.ammattinimikkeet")
                  )
                case m: KkOpintokokonaisuusToteutusMetadata =>
                  and(
                    validateTutkintoonJohtamatonMetadata(vCtx.tila, vCtx.kielivalinta, m),
                    // Opintokokonaisuudella ei ole ammattinimikkeitä
                    assertEmpty(m.ammattinimikkeet, "metadata.ammattinimikkeet"),
                    validateIfNonEmpty[ToteutusOid](
                      m.liitetytOpintojaksot,
                      "metadata.liitetytOpintojaksot",
                      (oid, path) =>
                        validateOpintojaksotIntegrity(vCtx, m)
                    )
                  )
                case _ =>
                  validateTutkintoonJohtamatonMetadata(
                    vCtx.tila,
                    vCtx.kielivalinta,
                    tutkintoonJohtamatonToteutusMetadata
                  )
              }
            case lkMetadata: LukioToteutusMetadata =>
              validateLukioMetadata(vCtx, toteutusDiffResolver, lkMetadata)
            case _ => NoErrors
          }
        )
      case _ => if (toteutus.tila == Julkaistu) error("metadata", missingMsg) else NoErrors
    }
    Seq(commonErrors, koulutustyyppiSpecificErrors).flatten.distinct
  }

  private def validateOpetusKoodiUriListItem(
      koodiUrit: Seq[String],
      koodiUriPattern: Pattern,
      relativePath: String,
      validationContext: ValidationContext,
      koodistoFunc: String => ExternalQueryResult,
      errorFunc: String => validation.ErrorMessage
  ): IsValid =
    validateIfNonEmpty[String](
      koodiUrit,
      s"metadata.opetus.$relativePath",
      (koodiUri, path) =>
        validateIfSuccessful(
          assertMatch(koodiUri, koodiUriPattern, path),
          assertKoodistoQueryResult(koodiUri, koodistoFunc, path, validationContext, errorFunc(koodiUri))
        )
    )

  private def validateOpetus(
      vCtx: ValidationContext,
      toteutusDiffResolver: ToteutusDiffResolver,
      opetus: Opetus
  ): IsValid = {
    val path = "metadata.opetus"
    and(
      validateOpetusKoodiUriListItem(
        toteutusDiffResolver.newOpetuskieliKoodiUrit(),
        OpetuskieliKoodiPattern,
        "opetuskieliKoodiUrit",
        vCtx,
        koulutusKoodiClient.opetusKieliKoodiUriExists,
        invalidOpetusKieliKoodiUri
      ),
      validateOpetusKoodiUriListItem(
        toteutusDiffResolver.newOpetusaikaKoodiUrit(),
        OpetusaikaKoodiPattern,
        "opetusaikaKoodiUrit",
        vCtx,
        koulutusKoodiClient.opetusAikaKoodiUriExists,
        invalidOpetusAikaKoodiUri
      ),
      validateOpetusKoodiUriListItem(
        toteutusDiffResolver.newOpetustapaKoodiUrit(),
        OpetustapaKoodiPattern,
        "opetustapaKoodiUrit",
        vCtx,
        koulutusKoodiClient.opetusTapaKoodiUriExists,
        invalidOpetusTapaKoodiUri
      ),
      validateIfDefined[KoulutuksenAlkamiskausi](
        opetus.koulutuksenAlkamiskausi,
        _.validate(
          "metadata.opetus.koulutuksenAlkamiskausi",
          toteutusDiffResolver.koulutuksenAlkamiskausiWithNewValues(),
          vCtx,
          hakuKoodiClient.kausiKoodiUriExists
        )
      ),
      validateIfDefined[Apuraha](opetus.apuraha, apuraha => validateApuraha(vCtx.tila, vCtx.kielivalinta, apuraha)),
      validateIfNonEmpty[Lisatieto](
        toteutusDiffResolver.newLisatiedot(),
        s"$path.lisatiedot",
        (lisatieto, path) =>
          validateIfSuccessful(
            lisatieto.validate(vCtx.tila, vCtx.kielivalinta, path),
            assertKoodistoQueryResult(
              lisatieto.otsikkoKoodiUri,
              koulutusKoodiClient.lisatiedotOtsikkoKoodiUriExists,
              path = s"$path.otsikkoKoodiUri",
              vCtx,
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
        vCtx.tila,
        and(
          assertNotEmpty(opetus.opetuskieliKoodiUrit, s"$path.opetuskieliKoodiUrit"),
          assertNotEmpty(opetus.opetusaikaKoodiUrit, s"$path.opetusaikaKoodiUrit"),
          assertNotEmpty(opetus.opetustapaKoodiUrit, s"$path.opetustapaKoodiUrit"),
          validateIfTrue(opetus.onkoApuraha, assertNotOptional(opetus.apuraha, s"$path.apuraha")),
          validateOptionalKielistetty(vCtx.kielivalinta, opetus.opetuskieletKuvaus, s"$path.opetuskieletKuvaus"),
          validateOptionalKielistetty(vCtx.kielivalinta, opetus.opetusaikaKuvaus, s"$path.opetusaikaKuvaus"),
          validateOptionalKielistetty(vCtx.kielivalinta, opetus.opetustapaKuvaus, s"$path.opetustapaKuvaus"),
          assertNotOptional(opetus.maksullisuustyyppi, s"$path.maksullisuustyyppi"),
          validateOptionalKielistetty(vCtx.kielivalinta, opetus.maksullisuusKuvaus, s"$path.maksullisuusKuvaus"),
          validateIfTrue(
            opetus.maksullisuustyyppi.contains(Maksullinen) || opetus.maksullisuustyyppi.contains(Lukuvuosimaksu),
            assertNotOptional(opetus.maksunMaara, s"$path.maksunMaara")
          ),
          validateOptionalKielistetty(vCtx.kielivalinta, opetus.suunniteltuKestoKuvaus, s"$path.suunniteltuKestoKuvaus")
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
      newOsaamisala: Option[AmmatillinenOsaamisala],
      path: String,
      vCtx: ValidationContext
  ): IsValid = {
    and(
      validateIfNonEmpty(osaamisala.linkki, s"$path.linkki", assertValidUrl _),
      validateIfJulkaistu(
        vCtx.tila,
        and(
          validateOptionalKielistetty(vCtx.kielivalinta, osaamisala.linkki, s"$path.linkki"),
          validateOptionalKielistetty(vCtx.kielivalinta, osaamisala.otsikko, s"$path.otsikko")
        )
      ),
      validateIfDefined[String](
        newOsaamisala.map(_.koodiUri),
        koodiUri =>
          validateIfSuccessful(
            assertMatch(koodiUri, OsaamisalaKoodiPattern, s"$path.koodiUri"),
            assertKoodistoQueryResult(
              koodiUri,
              koulutusKoodiClient.osaamisalaKoodiUriExists,
              s"$path.koodiUri",
              vCtx,
              invalidOsaamisalaKoodiUri(koodiUri)
            )
          )
      )
    )
  }

  private def validateAmmatillinenPerustutkintoErityisopetuksena(
      toteutus: Toteutus,
      path: String,
      validationContext: ValidationContext
  ): IsValid = {
    koulutusDAO.get(toteutus.koulutusOid) match {
      case Some(koulutus) =>
        validateIfNonEmpty[String](
          koulutus.koulutuksetKoodiUri,
          path,
          (koodiUri, path) =>
            assertKoulutustyyppiQueryResult(
              koodiUri,
              Seq(ammatillinenPerustutkintoKoulutustyyppiKoodiUri),
              koulutusKoodiClient,
              path,
              validationContext,
              invalidKoulutustyyppiKoodiForAmmatillinenPerustutkintoErityisopetuksena(koodiUri)
            )
        )
      case None =>
        error("koulutusOid", nonExistent("Koulutusta", toteutus.koulutusOid))
    }
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

  def validateOpintojaksotIntegrity(context: ValidationContext, metadata: KkOpintokokonaisuusToteutusMetadata): IsValid = {
    var errors: List[ValidationError] = List()
    var errorMap: Map[String, List[Option[ToteutusOid]]] = Map()

    val addErrorOid = (errorKey: String, toteutusOid: Option[ToteutusOid]) => {
      errorMap += (errorKey -> (errorMap.getOrElse(errorKey, List()) ++ List(toteutusOid)))
    }

    val liitetytOpintojaksot = metadata.liitetytOpintojaksot
    val toteutukset = toteutusDAO.get(liitetytOpintojaksot.toList)

    toteutukset.foreach(toteutus => {
      val liitettavanToteutuksenTyyppi = toteutus.metadata.get.tyyppi

      if (liitettavanToteutuksenTyyppi != KkOpintojakso) {
        addErrorOid("metadata.liitetytOpintojaksot.koulutustyyppi", toteutus.oid)
      }
    })

    // Jos opintokokonaisuus on julkaistu, täytyy siihen liitettyjen opintojaksojen olla myös julkaistuja
    if (context.tila == Julkaistu) {
      toteutukset.foreach(toteutus => {
        if (toteutus.tila != Julkaistu) {
          addErrorOid("metadata.liitetytOpintojaksot.julkaisutila", toteutus.oid)
        }
      })
    }

    errors = errorMap.toList.map(value => {
      val errorKey    = value._1
      val toteutukset = value._2.flatten
      ValidationError(
        errorKey,
        errorKey match {
          case "metadata.liitetytOpintojaksot.koulutustyyppi" =>
            invalidKoulutustyyppiForLiitettyOpintojakso(toteutukset)
          case "metadata.liitetytOpintojaksot.julkaisutila" =>
            invalidTilaForLiitettyOpintojaksoOnJulkaisu(toteutukset)
        }
      )
    })

    if (errors.isEmpty) NoErrors else errors
  }

  private def validateKieliKoodit(
      koodiUrit: Seq[String],
      relativePath: String,
      validationContext: ValidationContext
  ): IsValid =
    validateIfNonEmpty[String](
      koodiUrit,
      s"metadata.kielivalikoima.$relativePath",
      (koodiUri, path) =>
        validateIfSuccessful(
          assertMatch(koodiUri, KieliKoodiPattern, path),
          assertKoodistoQueryResult(
            koodiUri,
            hakuKoodiClient.kieliKoodiUriExists,
            path,
            validationContext,
            invalidKieliKoodiUri(relativePath, koodiUri)
          )
        )
    )

  private def validateLukioLinjat(
      linjat: Seq[LukiolinjaTieto],
      newLinjat: Seq[LukiolinjaTieto],
      relativePath: String,
      koodiUriPattern: Pattern,
      koodistoFunc: String => ExternalQueryResult,
      vCtx: ValidationContext
  ): IsValid =
    validateIfNonEmptySeq[LukiolinjaTieto](
      linjat,
      newLinjat,
      s"metadata.$relativePath",
      (linja, newLinjaOption, path) =>
        validateIfSuccessful(
          and(
            validateIfJulkaistu(
              vCtx.tila,
              validateOptionalKielistetty(vCtx.kielivalinta, linja.kuvaus, s"$path.kuvaus")
            ),
            assertMatch(linja.koodiUri, koodiUriPattern, s"$path.koodiUri")
          ),
          validateIfDefined[LukiolinjaTieto](
            newLinjaOption,
            newLinja =>
              assertKoodistoQueryResult(
                newLinja.koodiUri,
                koodistoFunc,
                s"$path.koodiUri",
                vCtx,
                invalidLukioLinjaKoodiUri(relativePath, newLinja.koodiUri)
              )
          )
        )
    )

  private def validateLukioMetadata(
      vCtx: ValidationContext,
      toteutusDiffResolver: ToteutusDiffResolver,
      lkMetadata: LukioToteutusMetadata
  ): IsValid = {
    and(
      validateIfDefined[Kielivalikoima](
        lkMetadata.kielivalikoima,
        kv =>
          and(
            validateKieliKoodit(toteutusDiffResolver.newA1Kielet(), "A1Kielet", vCtx),
            validateKieliKoodit(toteutusDiffResolver.newA2Kielet(), "A2Kielet", vCtx),
            validateKieliKoodit(toteutusDiffResolver.newB1Kielet(), "B1Kielet", vCtx),
            validateKieliKoodit(toteutusDiffResolver.newB2Kielet(), "B2Kielet", vCtx),
            validateKieliKoodit(toteutusDiffResolver.newB3Kielet(), "B3Kielet", vCtx),
            validateKieliKoodit(toteutusDiffResolver.newAidinkielet(), "aidinkielet", vCtx),
            validateKieliKoodit(toteutusDiffResolver.newMuutkielet(), "muutKielet", vCtx)
          )
      ),
      // Yleislinja täytyy olla true, jos painotukset ja erityisetKoulutustehtavat tyhjiä.
      validateIfTrue(
        lkMetadata.painotukset.isEmpty && lkMetadata.erityisetKoulutustehtavat.isEmpty,
        assertTrue(lkMetadata.yleislinja, "metadata.yleislinja", withoutLukiolinja)
      ),
      validateLukioLinjat(
        lkMetadata.painotukset,
        toteutusDiffResolver.newLukioPainotukset(),
        "painotukset",
        LukioPainotusKoodiPattern,
        koulutusKoodiClient.lukioPainotusKoodiUriExists,
        vCtx
      ),
      validateLukioLinjat(
        lkMetadata.erityisetKoulutustehtavat,
        toteutusDiffResolver.newLukioErityisetKoulutustehtavat(),
        "erityisetKoulutustehtavat",
        LukioErityinenKoulutustehtavaKoodiPattern,
        koulutusKoodiClient.lukioErityinenKoulutustehtavaKoodiUriExists,
        vCtx
      ),
      validateIfNonEmptySeq[LukiodiplomiTieto](
        lkMetadata.diplomit,
        toteutusDiffResolver.newLukioDiplomit(),
        "metadata.diplomit",
        (diplomi, newDiplomiOpt, path) =>
          validateIfSuccessful(
            and(
              assertMatch(diplomi.koodiUri, LukioDiplomiKoodiPattern, s"$path.koodiUri"),
              validateIfNonEmpty(diplomi.linkki, s"$path.linkki", assertValidUrl _),
              validateIfJulkaistu(
                vCtx.tila,
                and(
                  validateOptionalKielistetty(vCtx.kielivalinta, diplomi.linkki, s"$path.linkki"),
                  validateOptionalKielistetty(vCtx.kielivalinta, diplomi.linkinAltTeksti, s"$path.linkinAltTeksti")
                )
              )
            ),
            validateIfDefined[LukiodiplomiTieto](
              newDiplomiOpt,
              newDiplomi =>
                assertKoodistoQueryResult(
                  newDiplomi.koodiUri,
                  koulutusKoodiClient.lukioDiplomiKoodiUriExists,
                  s"$path.koodiUri",
                  vCtx,
                  invalidLukioDiplomiKoodiUri(newDiplomi.koodiUri)
                )
            )
          )
      )
    )
  }

  private def validateKoulutusIntegrity(toteutus: Toteutus): IsValid = {
    val koulutus = koulutusDAO.get(toteutus.koulutusOid)
    val (
      koulutusTila,
      koulutusTyyppi,
      koulutusOpintojenlaajuusMin,
      koulutusOpintojenlaajuusMax,
      koulutusOpintojenLaajuusyksikko
    ) = koulutus match {
      case Some(k: Koulutus) =>
        k.metadata match {
          case Some(metadata: KkOpintokokonaisuusKoulutusMetadata) =>
            (
              Some(k.tila),
              Some(k.koulutustyyppi),
              metadata.opintojenLaajuusNumeroMin,
              metadata.opintojenLaajuusNumeroMax,
              metadata.opintojenLaajuusyksikkoKoodiUri
            )
          case _ => (Some(k.tila), Some(k.koulutustyyppi), None, None, None)
        }
      case None => (None, None, None, None, None)
    }

    val kkOpintokokonaisuusToteutusMetadata = toteutus.metadata match {
      case Some(metadata: KkOpintokokonaisuusToteutusMetadata) => Some(metadata)
      case _                                                   => None
    }

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
      ),
      validateIfDefined[KkOpintokokonaisuusToteutusMetadata](
        kkOpintokokonaisuusToteutusMetadata,
        kkOpintokokonaisuusToteutusMetadata =>
          and(
            assertTrue(
              ToteutusServiceUtil.isValidOpintojenlaajuus(
                koulutusOpintojenlaajuusMin,
                koulutusOpintojenlaajuusMax,
                kkOpintokokonaisuusToteutusMetadata.opintojenLaajuusNumero
              ),
              "metadata.opintojenLaajuusNumero",
              notInTheRangeMsg(
                koulutusOpintojenlaajuusMin,
                koulutusOpintojenlaajuusMax,
                kkOpintokokonaisuusToteutusMetadata.opintojenLaajuusNumero
              )
            ),
            assertTrue(
              ToteutusServiceUtil.isValidOpintojenLaajuusyksikko(
                koulutusOpintojenLaajuusyksikko,
                kkOpintokokonaisuusToteutusMetadata.opintojenLaajuusyksikkoKoodiUri
              ),
              "metadata.opintojenLaajuusyksikkoKoodiUri",
              invalidToteutusOpintojenLaajuusyksikkoIntegrity(
                koulutusOpintojenLaajuusyksikko,
                kkOpintokokonaisuusToteutusMetadata.opintojenLaajuusyksikkoKoodiUri
              )
            )
          )
      )
    )
  }

  override def validateEntityOnJulkaisu(toteutus: Toteutus): IsValid = {
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
