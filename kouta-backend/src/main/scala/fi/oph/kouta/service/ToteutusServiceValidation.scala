package fi.oph.kouta.service

import fi.oph.kouta.client.{EPerusteKoodiClient, KoodistoElement}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.Oid
import fi.oph.kouta.repository.{HakukohdeDAO, KoulutusDAO, SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.service.validation.LiitettyEntityValidation
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.LaajuusValidationUtil
import fi.oph.kouta.util.MiscUtils.{isDIAlukiokoulutus, isEBlukiokoulutus, withoutKoodiVersion}
import fi.oph.kouta.validation.CrudOperations.{create, update}
import fi.oph.kouta.validation.ExternalQueryResults.ExternalQueryResult
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation._

import java.time.Instant

object ToteutusServiceValidation
    extends ToteutusServiceValidation(
      KoodistoService,
      OrganisaatioServiceImpl,
      KoulutusDAO,
      HakukohdeDAO,
      SorakuvausDAO,
      ToteutusDAO,
      EPerusteKoodiClient
    )

class ToteutusServiceValidation(
    val koodistoService: KoodistoService,
    val organisaatioService: OrganisaatioService,
    koulutusDAO: KoulutusDAO,
    hakukohdeDAO: HakukohdeDAO,
    val sorakuvausDAO: SorakuvausDAO,
    toteutusDAO: ToteutusDAO,
    ePerusteKoodiClient: EPerusteKoodiClient
) extends KoulutusToteutusValidatingService[Toteutus]
    with RoleEntityAuthorizationService[Toteutus] {

  protected val roleEntity: RoleEntity = Role.Toteutus

  def withValidation[R](toteutus: Toteutus, oldToteutus: Option[Toteutus], authenticated: Authenticated)(
      f: Toteutus => R
  ): R = {
    var errors = super.validate(toteutus, oldToteutus)
    if (errors.isEmpty) {
      toteutus.metadata match {
        case Some(_: KkOpintojaksoToteutusMetadata) =>
          errors = validateLiitettyEntityIntegrity(toteutus)
        case Some(metadata) =>
          errors = if (toteutus.tila == Julkaistu || toteutus.tila == Tallennettu) validateLiitetytEntitiesIntegrity(toteutus.tila, metadata, ePerusteKoodiClient, authenticated) else NoErrors
        case None =>
      }
    }

    errors match {
      case NoErrors => f(toteutus)
      case errors   => throw KoutaValidationException(errors)
    }
  }

  override def validateEntity(toteutus: Toteutus, oldToteutus: Option[Toteutus]): IsValid = {
    val koulutusOidValid = assertValid(toteutus.koulutusOid, "koulutusOid")
    val koulutus         = if (koulutusOidValid.isEmpty) koulutusDAO.get(toteutus.koulutusOid) else None
    val commonErrors = and(
      toteutus.validate(),
      validateIfTrueOrElse(
        oldToteutus.isDefined,
        assertNotOptional(toteutus.oid, "oid"),
        assertNotDefined(toteutus.oid, "oid")
      ),
      validateImageUrlWithConfig(toteutus.teemakuva, "teemakuva"),
      validateIfSuccessful(
        koulutusOidValid,
        validateKoulutusIntegrity(toteutus, koulutus)
      )
    )

    val vCtx                 = ValidationContext(toteutus.tila, toteutus.kielivalinta, if (oldToteutus.isDefined) update else create)
    val toteutusDiffResolver = ToteutusDiffResolver(toteutus, oldToteutus)
    val koulutustyyppiSpecificErrors = toteutus.metadata match {
      case Some(metadata) =>
        val koulutustyypitWithMandatoryKuvaus: Set[Koulutustyyppi] =
          Set(
            AmmMuu,
            Tuva,
            Telma,
            VapaaSivistystyoOpistovuosi,
            VapaaSivistystyoMuu,
            VapaaSivistystyoOsaamismerkki,
            KkOpintojakso
          )
        val koulutusTyyppi    = metadata.tyyppi
        val koulutusKoodiUrit = koulutus.map(_.koulutuksetKoodiUri).getOrElse(Seq())
        and(
          validateIfTrueOrElse(
            koulutusTyyppi == Lk && !isEBlukiokoulutus(koulutusKoodiUrit) && !isDIAlukiokoulutus(koulutusKoodiUrit),
            assertEmptyKielistetty(toteutus.nimi, "nimi"),
            validateKielistetty(toteutus.kielivalinta, toteutus.nimi, "nimi")
          ),
          assertFalse(
            toteutusDiffResolver.koulutustyyppiChanged(),
            "metadata.tyyppi",
            notModifiableMsg("koulutustyyppiä", "toteutukselle")
          ),
          validateIfSuccessful(
            validateIfFalse(metadata.allowSorakuvaus, assertNotDefined(toteutus.sorakuvausId, "sorakuvausId")),
            validateSorakuvausIntegrity(toteutus.sorakuvausId, toteutus.tila, metadata.tyyppi, "metadata.tyyppi")
          ),
          validateTarjoajat(
            koulutusTyyppi,
            toteutus.tarjoajat,
            oldToteutus.map(_.tarjoajat).getOrElse(List()),
            if (toteutus.isAvoinKorkeakoulutus()) oppilaitostyypitForAvoinKorkeakoulutus else Seq()
          ),
          validateIfJulkaistu(vCtx.tila, assertNotEmpty(toteutus.tarjoajat, "tarjoajat")),
          validateIfDefined[Opetus](
            metadata.opetus,
            opetus => validateOpetus(vCtx, toteutusDiffResolver, opetus, koulutusTyyppi, koulutusKoodiUrit)
          ),
          validateIfNonEmpty[Yhteyshenkilo](
            metadata.yhteyshenkilot,
            "metadata.yhteyshenkilot",
            _.validate(vCtx, _)
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
                validateIfFalse(
                  koulutus.map(_.isAmmTutkintoWithoutEPeruste).getOrElse(false),
                  validateIfNonEmptySeq[AmmatillinenOsaamisala](
                    ammMetadata.osaamisalat,
                    toteutusDiffResolver.newAmmatillisetOsaamisalat(),
                    "metadata.osaamisalat",
                    validateOsaamisala(_, _, _, vCtx)
                  )
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
                    validateTutkintoonJohtamatonMetadata(vCtx, m),
                    // Opintojaksolla ei ole ammattinimikkeitä
                    assertEmpty(m.ammattinimikkeet, "metadata.ammattinimikkeet"),
                    validateAvoinKorkeakoulutusIntegrity(koulutus, toteutus)
                  )
                case m: KkOpintokokonaisuusToteutusMetadata =>
                  and(
                    validateTutkintoonJohtamatonMetadata(vCtx, m),
                    // Opintokokonaisuudella ei ole ammattinimikkeitä
                    assertEmpty(m.ammattinimikkeet, "metadata.ammattinimikkeet"),
                    validateAvoinKorkeakoulutusIntegrity(koulutus, toteutus)
                  )
                case m: TaiteenPerusopetusToteutusMetadata =>
                  validateTaiteenPerusopetusMetadata(m, vCtx, toteutusDiffResolver)
                case m: MuuToteutusMetadata =>
                  and(
                    validateTutkintoonJohtamatonMetadata(vCtx, m),
                    assertEmpty(m.ammattinimikkeet, "metadata.ammattinimikkeet")
                  )
                case m: VapaaSivistystyoOsaamismerkkiToteutusMetadata =>
                  and(
                    assertFalse(
                      m.isHakukohteetKaytossa.get,
                      "metadata.isHakukohteetKaytossa",
                      hakukohteenLiittaminenNotAllowed(koulutusTyyppi)
                    ),
                    validateTutkintoonJohtamatonMetadata(vCtx, m),
                    assertEmpty(m.ammattinimikkeet, "metadata.ammattinimikkeet")
                  )
                case _ =>
                  validateTutkintoonJohtamatonMetadata(
                    vCtx,
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

  private def validateAvoinKorkeakoulutusIntegrity(koulutus: Option[Koulutus], toteutus: Toteutus) = {
    val isToteutusAvoinKorkeakoulutus = toteutus.isAvoinKorkeakoulutus()
    val (tarjoajat, jarjestajat) = if (isToteutusAvoinKorkeakoulutus) {
      (koulutusDAO.listTarjoajaOids(koulutus.get.oid.get), toteutus.tarjoajat)
    } else {
      (Seq(), List())
    }

    val invalidJarjestajat = if (tarjoajat.isEmpty) {
      jarjestajat
    } else {
      val allTarjoajaParentAndChildOids = tarjoajat.flatMap(tarjoaja => {
        organisaatioService.getAllChildAndParentOidsWithKoulutustyypitFlat(tarjoaja)._1
      })

      jarjestajat.filter(jarjestaja => !allTarjoajaParentAndChildOids.contains(jarjestaja))
    }

    and(
      validateIfTrue(
        koulutus.map(_.isAvoinKorkeakoulutus).getOrElse(false), {
          assertTrue(
            isToteutusAvoinKorkeakoulutus == koulutus.get.isAvoinKorkeakoulutus(),
            "metadata.isAvoinKorkeakoulutus",
            invalidIsAvoinKorkeakoulutusIntegrity
          )
        }
      ),
      assertTrue(
        invalidJarjestajat.isEmpty,
        "tarjoajat",
        invalidJarjestajaForAvoinKorkeakoulutus(invalidJarjestajat)
      )
    )
  }

  private def validateOpetusKoodiUriListItem(
      koodiUrit: Seq[String],
      relativePath: String,
      validationContext: ValidationContext,
      koodistoFunc: String => ExternalQueryResult,
      errorFunc: String => ErrorMessage
  ): IsValid =
    validateIfNonEmpty[String](
      koodiUrit,
      s"metadata.opetus.$relativePath",
      (koodiUri, path) =>
        assertKoodistoQueryResult(koodiUri, koodistoFunc, path, validationContext, errorFunc(koodiUri))
    )

  private def validateOpetus(
      vCtx: ValidationContext,
      toteutusDiffResolver: ToteutusDiffResolver,
      opetus: Opetus,
      koulutustyyppi: Koulutustyyppi,
      koulutuskoodiurit: Seq[String]
  ): IsValid = {
    val path = "metadata.opetus"
    and(
      validateOpetusKoodiUriListItem(
        toteutusDiffResolver.newOpetuskieliKoodiUrit(),
        "opetuskieliKoodiUrit",
        vCtx,
        koodistoService.koodiUriExistsInKoodisto(OpetuskieliKoodisto, _),
        invalidOpetusKieliKoodiUri
      ),
      validateOpetusKoodiUriListItem(
        toteutusDiffResolver.newOpetusaikaKoodiUrit(),
        "opetusaikaKoodiUrit",
        vCtx,
        koodistoService.koodiUriExistsInKoodisto(OpetusaikaKoodisto, _),
        invalidOpetusAikaKoodiUri
      ),
      validateOpetusKoodiUriListItem(
        toteutusDiffResolver.newOpetustapaKoodiUrit(),
        "opetustapaKoodiUrit",
        vCtx,
        koodistoService.koodiUriExistsInKoodisto(OpetustapaKoodisto, _),
        invalidOpetusTapaKoodiUri
      ),
      validateIfDefined[KoulutuksenAlkamiskausi](
        opetus.koulutuksenAlkamiskausi,
        _.validate(
          "metadata.opetus.koulutuksenAlkamiskausi",
          toteutusDiffResolver.koulutuksenAlkamiskausiWithNewValues(),
          vCtx,
          koodistoService.koodiUriExistsInKoodisto(KausiKoodisto, _)
        )
      ),
      validateMaksullisuus(opetus, koulutustyyppi, koulutuskoodiurit, path),
      validateIfDefined[Apuraha](
        opetus.apuraha,
        apuraha => validateApuraha(vCtx.tila, vCtx.kielivalinta, apuraha, opetus)
      ),
      validateIfNonEmptySeq[Lisatieto](
        opetus.lisatiedot,
        toteutusDiffResolver.newLisatiedot(),
        s"$path.lisatiedot",
        (lisatieto, newLisatieto, path) =>
          lisatieto.validate(
            path,
            newLisatieto,
            vCtx,
            koodistoService.koodiUriExistsInKoodisto(KoulutuksenLisatiedotKoodisto, _)
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

  private def validateApuraha(
      tila: Julkaisutila,
      kielivalinta: Seq[Kieli],
      apuraha: Apuraha,
      opetus: Opetus
  ): IsValid = {
    val path = "metadata.opetus.apuraha"
    val min  = apuraha.min
    val max  = apuraha.max
    and(
      assertTrue(opetus.maksullisuustyyppi == Some(Lukuvuosimaksu), s"$path", invalidMaksullisuustyyppiWithApuraha),
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

  private def validateMaksullisuus(
      opetus: Opetus,
      koulutustyyppi: Koulutustyyppi,
      koulutuskoodiurit: Seq[String],
      path: String
  ): IsValid = {
    val isTutkintoonJohtavaKorkeakoulutus =
      Koulutustyyppi.isTutkintoonJohtava(koulutustyyppi) && Koulutustyyppi.isKorkeakoulu(koulutustyyppi)
    val English         = "oppilaitoksenopetuskieli_4"
    val Tohtorikoulutus = "tutkintotyyppi_16"

    and(
      validateIfTrue(
        opetus.maksullisuustyyppi.contains(Lukuvuosimaksu),
        and(
          assertTrue(
            opetus.opetuskieliKoodiUrit.map(koodiuri => withoutKoodiVersion(koodiuri)).contains(English),
            s"$path.maksullisuustyyppi",
            invalidOpetuskieliWithLukuvuosimaksu
          ),
          assertTrue(
            isTutkintoonJohtavaKorkeakoulutus,
            s"$path.maksullisuustyyppi",
            invalidKoulutustyyppiWithLukuvuosimaksuMsg(koulutustyyppi)
          ),
          validateIfTrue(
            isTutkintoonJohtavaKorkeakoulutus,
            koodistoService.getKoulutuksetByTutkintotyyppi(Tohtorikoulutus) match {
              case Right(tohtorikoulutuskoodiurit: Seq[KoodistoElement]) =>
                val koulutuskoodiuritWithoutVersion = koulutuskoodiurit.flatMap(_.split("#"))
                val tohtorikoulutukset =
                  tohtorikoulutuskoodiurit.map(_.koodiUri).intersect(koulutuskoodiuritWithoutVersion)
                assertEmpty(
                  tohtorikoulutukset,
                  s"$path.maksullisuustyyppi",
                  invalidKoulutusWithLukuvuosimaksu(tohtorikoulutukset)
                )
              case _ => error(s"$path.maksullisuustyyppi", koodistoServiceFailureMsg)
            }
          )
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
          assertKoodistoQueryResult(
            koodiUri,
            koodistoService.koodiUriExistsInKoodisto(OsaamisalaKoodisto, _),
            s"$path.koodiUri",
            vCtx,
            invalidOsaamisalaKoodiUri(koodiUri)
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
            assertKoulutuskoodiQueryResult(
              koodiUri,
              AmmatillisetPerustutkintoKoodit,
              koodistoService,
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
      vCtx: ValidationContext,
      m: TutkintoonJohtamatonToteutusMetadata
  ) = {
    and(
      validateIfNonEmpty(m.hakulomakeLinkki, "metadata.hakulomakeLinkki", assertValidUrl _),
      validateIfDefined[Ajanjakso](m.hakuaika, _.validate(vCtx, "metadata.hakuaika")),
      validateIfDefined[Int](m.aloituspaikat, assertNotNegative(_, "metadata.aloituspaikat")),
      validateOptionalKielistetty(vCtx.kielivalinta, m.aloituspaikkakuvaus, "metadata.aloituspaikkakuvaus"),
      validateIfTrue(
        vCtx.tila == Julkaistu,
        and(
          validateIfTrue(
            vCtx.crudOperation == create,
            assertFalse(
              m.hakulomaketyyppi.contains(Ataru) || m.hakulomaketyyppi.contains(HakuApp),
              "metadata.hakulomaketyyppi",
              notAllowedHakulomaketyyppi(m.hakulomaketyyppi)
            )
          ),
          validateIfTrue(
            !m.isHakukohteetKaytossa.contains(true),
            and(
              assertNotOptional(m.hakutermi, "metadata.hakutermi"),
              assertNotOptional(m.hakulomaketyyppi, "metadata.hakulomaketyyppi"),
              validateIfTrue(
                m.hakulomaketyyppi.contains(MuuHakulomake),
                and(
                  validateKielistetty(
                    vCtx.kielivalinta,
                    m.lisatietoaHakeutumisesta,
                    "metadata.lisatietoaHakeutumisesta"
                  ),
                  validateKielistetty(vCtx.kielivalinta, m.hakulomakeLinkki, "metadata.hakulomakeLinkki"),
                  validateOptionalKielistetty(
                    vCtx.kielivalinta,
                    m.lisatietoaValintaperusteista,
                    "metadata.lisatietoaValintaperusteista"
                  ),
                  assertNotOptional(m.hakuaika, "metadata.hakuaika")
                )
              ),
              validateIfTrue(
                m.hakulomaketyyppi.contains(EiSähköistä),
                validateKielistetty(vCtx.kielivalinta, m.lisatietoaHakeutumisesta, "metadata.lisatietoaHakeutumisesta")
              )
            )
          )
        )
      )
    )
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
        assertKoodistoQueryResult(
          koodiUri,
          koodistoService.koodiUriExistsInKoodisto(KieliKoodisto, _),
          path,
          validationContext,
          invalidKieliKoodiUri(relativePath, koodiUri)
        )
    )

  private def validateLukioLinjat(
      linjat: Seq[LukiolinjaTieto],
      newLinjat: Seq[LukiolinjaTieto],
      relativePath: String,
      koodistoFunc: String => ExternalQueryResult,
      vCtx: ValidationContext
  ): IsValid =
    validateIfNonEmptySeq[LukiolinjaTieto](
      linjat,
      newLinjat,
      s"metadata.$relativePath",
      (linja, newLinjaOption, path) =>
        validateIfSuccessful(
          validateIfJulkaistu(
            vCtx.tila,
            validateOptionalKielistetty(vCtx.kielivalinta, linja.kuvaus, s"$path.kuvaus")
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
        koodistoService.koodiUriExistsInKoodisto(LukioPainotuksetKoodisto, _),
        vCtx
      ),
      validateLukioLinjat(
        lkMetadata.erityisetKoulutustehtavat,
        toteutusDiffResolver.newLukioErityisetKoulutustehtavat(),
        "erityisetKoulutustehtavat",
        koodistoService.koodiUriExistsInKoodisto(LukioErityinenKoulutustehtavaKoodisto, _),
        vCtx
      ),
      validateIfNonEmptySeq[LukiodiplomiTieto](
        lkMetadata.diplomit,
        toteutusDiffResolver.newLukioDiplomit(),
        "metadata.diplomit",
        (diplomi, newDiplomiOpt, path) =>
          validateIfSuccessful(
            and(
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
                  koodistoService.koodiUriExistsInKoodisto(LukioDiplomiKoodisto, _),
                  s"$path.koodiUri",
                  vCtx,
                  invalidLukioDiplomiKoodiUri(newDiplomi.koodiUri)
                )
            )
          )
      )
    )
  }

  private def validateKoulutusIntegrity(toteutus: Toteutus, koulutus: Option[Koulutus]): IsValid = {
    val koulutusTila   = koulutus.map(_.tila)
    val koulutusTyyppi = koulutus.map(_.koulutustyyppi)

    val (
      koulutusLaajuusMin,
      koulutusLaajuusMax,
      koulutusLaajuusyksikko
    ) = koulutus match {
      case Some(k: Koulutus) =>
        k.metadata match {
          case Some(metadata: LaajuusMinMax) =>
            (
              metadata.opintojenLaajuusNumeroMin,
              metadata.opintojenLaajuusNumeroMax,
              metadata.opintojenLaajuusyksikkoKoodiUri
            )
          case _ => (None, None, None)
        }
      case None => (None, None, None)
    }

    val koulutustyypitRequiringToteutusAndKoulutusNamesEqual: Set[Koulutustyyppi] =
      Set(AmmOsaamisala, Telma, Tuva, VapaaSivistystyoOpistovuosi)

    and(
      validateDependency(toteutus.tila, koulutusTila, toteutus.koulutusOid, "Koulutusta", "koulutusOid"),
      validateIfDefined[Koulutustyyppi](
        koulutusTyyppi,
        koulutusTyyppi =>
          and(
            validateIfTrue(
              koulutustyypitRequiringToteutusAndKoulutusNamesEqual.contains(koulutusTyyppi),
              assertNimiMatchExternal(toteutus.nimi, koulutus.get.nimi, "nimi", "koulutuksessa")
            ),
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
      toteutus.metadata match {
        case Some(metadataWithLaajuusNumero: LaajuusSingle) => {
          and(
            assertTrue(
              LaajuusValidationUtil.isValidOpintojenLaajuus(
                koulutusLaajuusMin,
                koulutusLaajuusMax,
                metadataWithLaajuusNumero.opintojenLaajuusNumero
              ),
              "metadata.opintojenLaajuusNumero",
              notInTheRangeMsg(
                koulutusLaajuusMin,
                koulutusLaajuusMax,
                metadataWithLaajuusNumero.opintojenLaajuusNumero
              )
            ),
            assertTrue(
              LaajuusValidationUtil.isValidOpintojenLaajuusyksikko(
                koulutusLaajuusyksikko,
                metadataWithLaajuusNumero.opintojenLaajuusyksikkoKoodiUri
              ),
              "metadata.opintojenLaajuusyksikkoKoodiUri",
              invalidToteutusOpintojenLaajuusyksikkoIntegrity(
                koulutusLaajuusyksikko,
                metadataWithLaajuusNumero.opintojenLaajuusyksikkoKoodiUri
              )
            )
          )
        }
        case Some(metadataWithLaajuusRange: LaajuusMinMax) => {
          and(
            assertTrue(
              LaajuusValidationUtil.isAtLeast(
                metadataWithLaajuusRange.opintojenLaajuusNumeroMin,
                koulutusLaajuusMin
              ),
              "metadata.opintojenLaajuusNumeroMin",
              invalidToteutusOpintojenLaajuusMin(
                koulutusLaajuusMin,
                metadataWithLaajuusRange.opintojenLaajuusNumeroMin
              )
            ),
            assertTrue(
              LaajuusValidationUtil.isAtMost(
                metadataWithLaajuusRange.opintojenLaajuusNumeroMax,
                koulutusLaajuusMax
              ),
              "metadata.opintojenLaajuusNumeroMax",
              invalidToteutusOpintojenLaajuusMax(
                koulutusLaajuusMax,
                metadataWithLaajuusRange.opintojenLaajuusNumeroMax
              )
            ),
            assertTrue(
              LaajuusValidationUtil.isValidOpintojenLaajuusyksikko(
                koulutusLaajuusyksikko,
                metadataWithLaajuusRange.opintojenLaajuusyksikkoKoodiUri
              ),
              "metadata.opintojenLaajuusyksikkoKoodiUri",
              invalidToteutusOpintojenLaajuusyksikkoIntegrity(
                koulutusLaajuusyksikko,
                metadataWithLaajuusRange.opintojenLaajuusyksikkoKoodiUri
              )
            )
          )
        }
        case _ => NoErrors
      }
    )
  }

  private def validateTaiteenPerusopetusMetadata(
      m: TaiteenPerusopetusToteutusMetadata,
      vCtx: ValidationContext,
      toteutusDiffResolver: ToteutusDiffResolver
  ): IsValid =
    and(
      assertOpintojenLaajuusyksikkoKoodiUri(toteutusDiffResolver.newOpintojenLaajuusyksikkoKoodiUri(), vCtx),
      validateLaajuusMinMax(m.opintojenLaajuusNumeroMin, m.opintojenLaajuusNumeroMax),
      validateIfNonEmpty[String](
        toteutusDiffResolver.newTaiteenalaKoodiUrit(),
        "metadata.taiteenalaKoodiUrit",
        (koodiUri, path) =>
          assertKoodistoQueryResult(
            koodiUri,
            koodistoService.koodiUriExistsInKoodisto(TaiteenalaKoodisto, _),
            path,
            vCtx,
            invalidTaiteenPerusopetusTaiteenalaKoodiuri(koodiUri)
          )
      ),
      validateTutkintoonJohtamatonMetadata(vCtx, m)
    )

  override def validateEntityOnJulkaisu(toteutus: Toteutus): IsValid =
    toteutus.metadata
      .flatMap(_.opetus)
      .flatMap(_.koulutuksenAlkamiskausi)
      .map(_.validateOnJulkaisu("metadata.opetus.koulutuksenAlkamiskausi"))
      .getOrElse(NoErrors)

  override def validateInternalDependenciesWhenDeletingEntity(toteutus: Toteutus): IsValid = assertTrue(
    hakukohdeDAO.listByToteutusOid(toteutus.oid.get, TilaFilter.onlyOlemassaolevat()).isEmpty,
    "tila",
    integrityViolationMsg("Toteutusta", "hakukohteita")
  )

  def validateLiitettyEntityIntegrity(toteutus: Toteutus): IsValid = {
    LiitettyEntityValidation.validateLiitettyEntityIntegrity(toteutus, toteutus.oid match {
      case Some(oid) => toteutusDAO.get(oid)
      case None => Vector()
    })
  }

  def validateLiitetytEntitiesIntegrity(
      tila: Julkaisutila,
      metadata: ToteutusMetadata,
      ePerusteKoodiClient: EPerusteKoodiClient,
      authenticated: Authenticated
  ): IsValid = {
    var errors: List[ValidationError]    = List()
    var errorMap: Map[String, List[Oid]] = Map()

    val addErrorOid = (errorKey: String, koulutusOid: Oid) => {
      errorMap += (errorKey -> (errorMap.getOrElse(errorKey, List()) ++ List(koulutusOid)))
    }

    val (liitetytOids, entities, liitettyjenKoulutustyyppi) = metadata match {
      case m: VapaaSivistystyoToteutusMetadata =>
        val liitetytOids = m.liitetytOsaamismerkit
        val koulutukset  = if (liitetytOids.isEmpty) List() else koulutusDAO.get(liitetytOids.toList)
        (liitetytOids, koulutukset, Some(VapaaSivistystyoOsaamismerkki))
      case m: KkOpintokokonaisuusToteutusMetadata =>
        val liitetytOids = m.liitetytOpintojaksot
        val toteutukset  = if (liitetytOids.isEmpty) List() else toteutusDAO.get(liitetytOids.toList)
        (liitetytOids, toteutukset, Some(KkOpintojakso))
      case _ =>
        (List(), List(), None)
    }

    entities.foreach(entity => {
      val liitettavanEntiteetinTyyppi = entity.koulutustyyppi
      val liitettavanEntiteetinTila   = entity.tila
      entity match {
        case t: ToteutusLiitettyListItem => authorizeGetWithType[ToteutusLiitettyListItem](t)(authenticated)
        case t: KoulutusLiitettyListItem =>
          authorizeGetWithType[KoulutusLiitettyListItem](t)(authenticated)
          if (liitettavanEntiteetinTyyppi == VapaaSivistystyoOsaamismerkki) {
            t.osaamismerkkiKoodiUri match {
              case Some(koodiUri) =>
                ePerusteKoodiClient.getOsaamismerkkiFromEPerusteCache(withoutKoodiVersion(koodiUri)) match {
                  case Right(osaamismerkki) =>
                    osaamismerkki.voimassaoloLoppuu match {
                      case Some(voimassaoloLoppuu) =>
                        if (voimassaoloLoppuu < Instant.now().toEpochMilli) {
                          addErrorOid("metadata.liitetytEntiteetit.deprecatedOsaamismerkki", entity.oid)
                        }
                      case _ =>
                    }
                  case Left(exp) => throw exp
                }
              case _ =>
            }
          }
      }

      if (liitettavanEntiteetinTyyppi != liitettyjenKoulutustyyppi.get) {
        addErrorOid("metadata.liitetytEntiteetit.koulutustyyppi", entity.oid)
      }

      if (!TilaFilter.onlyOlemassaolevatAndArkistoimattomat().contains(liitettavanEntiteetinTila)) {
        addErrorOid("metadata.liitetytEntiteetit.tila", entity.oid)
      }

      // Jos toteutus on julkaistu, täytyy siihen liitettyjen entiteettien olla myös julkaistuja
      if (tila == Julkaistu) {
        if (liitettavanEntiteetinTila != Julkaistu) {
          addErrorOid(s"metadata.liitetytEntiteetit.julkaisutila", entity.oid)
        }
      }
    })

    liitetytOids.foreach(oid => {
      if (!entities.exists(entity => entity.oid == oid)) {
        addErrorOid("metadata.liitetytEntiteetit.notFound", oid)
      }
    })

    errors = errorMap.toList.map(value => {
      val errorKey = value._1
      val entities = value._2
      ValidationError(
        errorKey,
        errorKey match {
          case "metadata.liitetytEntiteetit.koulutustyyppi" =>
            invalidKoulutustyyppiForLiitetty(entities, liitettyjenKoulutustyyppi)
          case "metadata.liitetytEntiteetit.julkaisutila" =>
            invalidTilaForLiitettyOnJulkaisu(entities, liitettyjenKoulutustyyppi)
          case "metadata.liitetytEntiteetit.tila" =>
            invalidTilaForLiitetty(entities, liitettyjenKoulutustyyppi)
          case "metadata.liitetytEntiteetit.notFound" =>
            unknownEntity(entities, liitettyjenKoulutustyyppi)
          case "metadata.liitetytEntiteetit.deprecatedOsaamismerkki" =>
            deprecatedOsaamismerkki(entities)

        }
      )
    })

    if (errors.isEmpty) NoErrors else errors
  }
}
