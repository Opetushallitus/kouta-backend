package fi.oph.kouta.service.validation

import fi.oph.kouta.client.{EPerusteAmosaaClient, EPerusteAmosaaQueryException, EPerusteKoodiClient}
import fi.oph.kouta.client.KoodiUriUtils.{koodiUriFromString, koodiUriWithEqualOrHigherVersioNbrInList, koodiUrisEqual}
import fi.oph.kouta.domain._
import fi.oph.kouta.service.{KoodistoService, KoodistoValidator, ValidatingSubService}
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, KoulutusDiffResolver, NoErrors, ValidationContext}

import scala.util.{Failure, Success, Try}

object AmmatillinenKoulutusServiceValidation
    extends AmmatillinenKoulutusServiceValidation(KoodistoService, EPerusteKoodiClient, EPerusteAmosaaClient)

class AmmatillinenKoulutusServiceValidation(
    val koodistoService: KoodistoService,
    ePerusteKoodiClient: EPerusteKoodiClient,
    ePerusteAmosaaClient: EPerusteAmosaaClient
) extends KoodistoValidator
    with ValidatingSubService[Koulutus] {
  def validate(koulutus: Koulutus, oldKoulutus: Option[Koulutus], vCtx: ValidationContext): IsValid = {
    val koulutusDiffResolver = KoulutusDiffResolver(koulutus, oldKoulutus)

    val upperLevelErrors = koulutus.koulutustyyppi match {
      case Amm =>
        if (koulutus.isAmmTutkintoWithoutEPeruste()) {
          and(
            assertNotDefined(koulutus.ePerusteId, "ePerusteId"),
            validateKoulutusKoodiUrit(
              AmmatillisetKoulutusKoodit,
              koulutus.koulutuksetKoodiUri,
              koulutusDiffResolver.newKoulutusKoodiUrit(),
              Some(1),
              vCtx
            )
          )
        } else {
          and(
            validateIfSuccessful(
              validateKoulutusKoodiUrit(
                AmmatillisetKoulutusKoodit,
                koulutus.koulutuksetKoodiUri,
                koulutusDiffResolver.newKoulutusKoodiUrit(),
                Some(1),
                vCtx
              ),
              validateIfTrue(
                ammKoulutusNimiShouldBeValidated(koulutus, koulutusDiffResolver),
                koodistoService.getKaannokset(koulutus.koulutuksetKoodiUri.head) match {
                  case Left(_) => error("koulutuksetKoodiUri", koodistoServiceFailureMsg)
                  case Right(kaannokset) =>
                    assertNimiMatchExternal(
                      koulutus.nimi,
                      kaannokset,
                      "nimi",
                      s"koulutuksessa ${koulutus.koulutuksetKoodiUri.head}"
                    )
                  case _ => error("koulutuksetKoodiUri", invalidKoulutuskoodiuri(koulutus.koulutuksetKoodiUri.head))
                }
              )
            ),
            validateIfJulkaistu(koulutus.tila, assertNotOptional(koulutus.ePerusteId, "ePerusteId")),
            validateEPeruste(
              koulutusDiffResolver.newEPerusteId(),
              "ePerusteId",
              koulutusDiffResolver.newKoulutusKoodiUrit()
            )
          )
        }
      case AmmOsaamisala =>
        and(
          validateKoulutusKoodiUrit(
            AmmatillisetKoulutusKoodit,
            koulutus.koulutuksetKoodiUri,
            koulutusDiffResolver.newKoulutusKoodiUrit(),
            Some(1),
            vCtx
          ),
          validateIfJulkaistu(koulutus.tila, assertNotOptional(koulutus.ePerusteId, "ePerusteId")),
          validateEPeruste(
            koulutusDiffResolver.newEPerusteId(),
            "ePerusteId",
            koulutusDiffResolver.newKoulutusKoodiUrit()
          )
        )

      case AmmMuu =>
        and(
          assertNotDefined(koulutus.sorakuvausId, "sorakuvausId"),
          assertEmpty(koulutus.koulutuksetKoodiUri, "koulutuksetKoodiUri"),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )

      case _ =>
        and(
          assertEmpty(koulutus.koulutuksetKoodiUri, "koulutuksetKoodiUri"),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
    }

    val metadataErrors = koulutus.metadata match {
      case Some(metadata) =>
        metadata match {
          case m: AmmatillinenKoulutusMetadata =>
            if (koulutus.isAmmTutkintoWithoutEPeruste()) {
              and(
                validateOpintojenLaajuusNumero(m.opintojenLaajuusNumero, vCtx),
                validateOpintojenLaajuusYksikko(
                  m.opintojenLaajuusyksikkoKoodiUri,
                  koulutusDiffResolver.hasLaajuusyksikkoChanged(),
                  vCtx
                ),
                assertTutkintonimikeKoodiUrit(
                  koulutusDiffResolver.newTutkintonimikeKoodiUrit(),
                  vCtx,
                  TutkintonimikkeetKoodisto
                ),
                assertKoulutusalaKoodiUrit(koulutusDiffResolver.newKoulutusalaKoodiUrit(), vCtx)
              )
            } else {
              and(
                assertEmptyKielistetty(m.kuvaus, "metadata.kuvaus"),
                assertEmptyKielistetty(m.osaamistavoitteet, "metadata.osaamistavoitteet")
              )
            }
          case m: AmmatillinenTutkinnonOsaKoulutusMetadata =>
            validateAmmTutkinnonosaMetadata(
              vCtx,
              m,
              koulutusDiffResolver.newTutkinnonosat().nonEmpty,
              koulutusDiffResolver.newPaikallisetTutkinnonosat().nonEmpty
            )
          case m: AmmatillinenOsaamisalaKoulutusMetadata =>
            validateAmmOsaamisalaKoulutusMetadata(
              vCtx.tila,
              m,
              koulutus.ePerusteId,
              koulutusDiffResolver
            )
          case m: AmmatillinenMuuKoulutusMetadata =>
            and(
              assertKoulutusalaKoodiUrit(koulutusDiffResolver.newKoulutusalaKoodiUrit(), vCtx),
              validateOpintojenLaajuusYksikko(
                m.opintojenLaajuusyksikkoKoodiUri,
                koulutusDiffResolver.hasLaajuusyksikkoChanged(),
                vCtx
              ),
              validateOpintojenLaajuusNumero(
                m.opintojenLaajuusNumero,
                vCtx
              )
            )
          case _ => NoErrors
        }
      case _ => NoErrors
    }

    Seq(upperLevelErrors, metadataErrors).flatten.distinct
  }

  private def ammKoulutusNimiShouldBeValidated(
      ammKoulutus: Koulutus,
      koulutusDiffResolver: KoulutusDiffResolver
  ): Boolean =
    (koulutusDiffResolver
      .newKoulutusKoodiUrit()
      .nonEmpty || koulutusDiffResolver.newNimi().isDefined) && ammKoulutus.koulutuksetKoodiUri.nonEmpty

  private def validateEPeruste(ePerusteId: Option[Long], path: String, koulutusKoodiUrit: Seq[String]): IsValid =
    // Nykyisellään (6/2022) millään koulutustyypillä ei määritellä ePerusteID:tä + useita koulutusKoodiUreja
    validateIfTrue(
      koulutusKoodiUrit.size < 2,
      validateIfDefined[Long](
        ePerusteId,
        ePerusteId =>
          validateIfSuccessful(
            assertNotNegative(ePerusteId, path),
            ePerusteKoodiClient.getKoulutusKoodiUritForEPerusteFromCache(ePerusteId) match {
              case Right(koodiUritForEperuste) =>
                validateIfSuccessful(
                  // ePeruste oletetaan tuntemattomaksi, ellei sille löydy yhtään koulutusKoodiUria
                  assertTrue(koodiUritForEperuste.nonEmpty, path, invalidEPerusteId(ePerusteId)),
                  validateIfNonEmpty[String](
                    koulutusKoodiUrit,
                    "notUsed",
                    (koulutusKoodiUri, notUsedPath) =>
                      assertTrue(
                        koodiUriWithEqualOrHigherVersioNbrInList(
                          koulutusKoodiUri,
                          koodiUritForEperuste,
                          checkVersio = false
                        ),
                        path,
                        invalidEPerusteIdForKoulutusKoodiUri(ePerusteId, koulutusKoodiUri)
                      )
                  )
                )
              case _ => error(path, ePerusteServiceFailureMsg)
            }
          )
      )
    )

  private def validateAmmTutkinnonosaMetadata(
      vCtx: ValidationContext,
      metadata: AmmatillinenTutkinnonOsaKoulutusMetadata,
      newTutkinnonOsat: Boolean,
      newPaikallisetTutkinnonOsat: Boolean
  ): IsValid = {
    val tutkinnonOsatPath   = "metadata.tutkinnonOsat"
    val paikallisetPath     = "metadata.paikallisetTutkinnonOsat"
    val tutkinnonOsat       = metadata.tutkinnonOsat
    val paikalliset         = metadata.paikallisetTutkinnonOsat

    val paikallisetConsistencyCheck = assertTrue(
      paikalliset.map(_.opetussuunnitelmaId).distinct.size <= 1,
      paikallisetPath,
      inconsistentPaikallinenTutkinnonOsaOpetussuunnitelmaId
    )

    and(
      validateIfJulkaistu(
        vCtx.tila,
        assertTrue(
          tutkinnonOsat.nonEmpty || paikalliset.nonEmpty,
          tutkinnonOsatPath,
          missingTutkinnonOsatOrPaikallisetTutkinnonOsatMsg
        )
      ),
      assertEmptyKielistetty(metadata.kuvaus, "metadata.kuvaus"),
      paikallisetConsistencyCheck,
      validateIfTrue(
        newTutkinnonOsat,
        validateIfSuccessful(
          validateIfTrue(
            newTutkinnonOsat,
            validateIfNonEmpty[TutkinnonOsa](
              tutkinnonOsat,
              tutkinnonOsatPath,
              (osa, path) =>
                validateEPeruste(
                  osa.ePerusteId,
                  s"$path.ePerusteId",
                  Seq(osa.koulutusKoodiUri.getOrElse("")).filter(_.nonEmpty)
                )
            )
          ), {
            ePerusteKoodiClient.getTutkinnonosatForEPerusteetFromCache(
              tutkinnonOsat.filter(_.ePerusteId.isDefined).map(_.ePerusteId.get)
            ) match {
              case Left(_) => error(s"$tutkinnonOsatPath.ePerusteId", ePerusteServiceFailureMsg)
              case Right(tutkinnonOsaIdsByEPerusteId) =>
                validateIfNonEmpty[TutkinnonOsa](
                  tutkinnonOsat,
                  tutkinnonOsatPath,
                  _.validate(vCtx, _, tutkinnonOsaIdsByEPerusteId)
                )
              case _ =>
                validateIfNonEmpty[TutkinnonOsa](
                  tutkinnonOsat,
                  tutkinnonOsatPath,
                  _.validate(vCtx, _, Map())
                )
            }
          }
        )
      ),
      validateIfTrue(
        newPaikallisetTutkinnonOsat && paikalliset.nonEmpty,
        validateIfSuccessful(
          paikallisetConsistencyCheck,
          {
            val opetussuunnitelmaId = paikalliset.head.opetussuunnitelmaId
            Try(opetussuunnitelmaId.toLong) match {
              case Failure(_) =>
                error(s"$paikallisetPath.opetussuunnitelmaId", invalidPaikallinenTutkinnonOsaOpetussuunnitelmaId(opetussuunnitelmaId))
              case Success(opetussuunnitelmaIdLong) =>
                Try(ePerusteAmosaaClient.getPaikallisetTutkinnonosat(opetussuunnitelmaIdLong)) match {
                  case Failure(e: EPerusteAmosaaQueryException) if e.status == 404 =>
                    error(s"$paikallisetPath.opetussuunnitelmaId", invalidPaikallinenTutkinnonOsaOpetussuunnitelmaId(opetussuunnitelmaId))
                  case Failure(_) =>
                    error(s"$paikallisetPath.opetussuunnitelmaId", amosaaServiceFailureMsg)
                  case Success(amosaaOsat) =>
                    val validIds = amosaaOsat.map(_.id.toString).toSet
                    validateIfNonEmpty[PaikallinenTutkinnonOsa](
                      paikalliset,
                      paikallisetPath,
                      (osa, path) =>
                        assertTrue(
                          validIds.contains(osa.tutkinnonosaId),
                          s"$path.tutkinnonosaId",
                          invalidPaikallinenTutkinnonOsaId(osa.tutkinnonosaId)
                        )
                    )
                }
            }
          }
        )
      )
    )
  }

  private def validateAmmOsaamisalaKoulutusMetadata(
      tila: Julkaisutila,
      osaamisalaMetadata: AmmatillinenOsaamisalaKoulutusMetadata,
      ePerusteId: Option[Long],
      koulutusDiffResolver: KoulutusDiffResolver
  ): IsValid =
    and(
      validateIfJulkaistu(
        tila,
        assertNotOptional(osaamisalaMetadata.osaamisalaKoodiUri, "metadata.osaamisalaKoodiUri")
      ),
      assertEmptyKielistetty(osaamisalaMetadata.kuvaus, "metadata.kuvaus"),
      assertEmptyKielistetty(osaamisalaMetadata.osaamistavoitteet, "metadata.osaamistavoitteet"),
      validateIfTrue(
        koulutusDiffResolver
          .newOsaamisalaKoodiUri()
          .isDefined || koulutusDiffResolver.newEPerusteId().isDefined || koulutusDiffResolver.newNimi().isDefined,
        validateIfDefined[String](
          osaamisalaMetadata.osaamisalaKoodiUri,
          koodiUri =>
            validateIfDefined[Long](
              ePerusteId,
              ePerusteId =>
                validateIfTrue(
                  ePerusteId > 0,
                  ePerusteKoodiClient
                    .getOsaamisalaKoodiuritForEPerusteFromCache(ePerusteId) match {
                    case Right(osaamisalaKoodiuritForEPeruste) =>
                      val checkedKoodiUri = koodiUriFromString(koodiUri)
                      val newNimi         = koulutusDiffResolver.newNimi()
                      val matchingKoodiUri =
                        osaamisalaKoodiuritForEPeruste.find(uri => koodiUrisEqual(uri, checkedKoodiUri))
                      validateIfSuccessful(
                        assertTrue(
                          matchingKoodiUri.isDefined,
                          "metadata.osaamisalaKoodiUri",
                          invalidOsaamisalaForEPeruste(ePerusteId, koodiUri)
                        ),
                        validateIfTrue(
                          newNimi.isDefined,
                          assertNimiMatchExternal(
                            newNimi.get,
                            matchingKoodiUri.get.nimi,
                            "nimi",
                            s"osaamisalassa $koodiUri"
                          )
                        )
                      )

                    case _ => error("ePerusteId", ePerusteServiceFailureMsg)
                  }
                )
            )
        )
      )
    )

}
