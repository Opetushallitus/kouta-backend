package fi.oph.kouta.service.validation

import fi.oph.kouta.client.KoodistoUtils.{koodiUriFromString, koodiUriWithEqualOrHigherVersioNbrInList, koodiUrisEqual}
import fi.oph.kouta.client.{CachedKoodistoClient, EPerusteKoodiClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.service.{KoodistoValidator, ValidatingSubService}
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, KoulutusDiffResolver, NoErrors, ValidationContext}

object AmmatillinenKoulutusServiceValidation
    extends AmmatillinenKoulutusServiceValidation(CachedKoodistoClient, EPerusteKoodiClient)

class AmmatillinenKoulutusServiceValidation(
    val koodistoClient: CachedKoodistoClient,
    ePerusteKoodiClient: EPerusteKoodiClient
) extends KoodistoValidator
    with ValidatingSubService[Koulutus] {
  def validate(koulutus: Koulutus, oldKoulutus: Option[Koulutus], vCtx: ValidationContext): IsValid = {
    val koulutusDiffResolver = KoulutusDiffResolver(koulutus, oldKoulutus)

    val upperLevelErrors = koulutus.koulutustyyppi match {
      case Amm => {
        if (koulutus.isPelastusalanKoulutus) {
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
                koodistoClient.getKoodiUriVersionOrLatestFromCache(koulutus.koulutuksetKoodiUri.head) match {
                  case Left(_) => error("koulutuksetKoodiUri", koodistoServiceFailureMsg)
                  case Right(uri) =>
                    assertNimiMatchExternal(
                      koulutus.nimi,
                      uri.nimi,
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
          case m: AmmatillinenKoulutusMetadata => {
            if (koulutus.isPelastusalanKoulutus) {
              and(
                validateOpintojenLaajuusNumero(m.opintojenLaajuusNumero, vCtx),
                validateOpintojenLaajuusYksikko(
                  m.opintojenLaajuusyksikkoKoodiUri,
                  koulutusDiffResolver.hasLaajuusyksikkoChanged(),
                  vCtx
                ),
                assertTutkintonimikeKoodiUrit(koulutusDiffResolver.newTutkintonimikeKoodiUrit(), vCtx, TutkintonimikkeetKoodisto),
                assertKoulutusalaKoodiUrit(koulutusDiffResolver.newKoulutusalaKoodiUrit(), vCtx)
              )
            } else {
              NoErrors
            }
          }
          case m: AmmatillinenTutkinnonOsaKoulutusMetadata =>
            validateAmmTutkinnonosaMetadata(
              vCtx,
              m.tutkinnonOsat,
              koulutusDiffResolver.newTutkinnonosat().nonEmpty
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
                        koodiUriWithEqualOrHigherVersioNbrInList(koulutusKoodiUri, koodiUritForEperuste, false),
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
      tutkinnonOsat: Seq[TutkinnonOsa],
      newTutkinnonOsat: Boolean
  ): IsValid = {
    val path = "metadata.tutkinnonOsat"
    and(
      validateIfJulkaistu(vCtx.tila, assertNotEmpty(tutkinnonOsat, path)),
      validateIfTrue(
        newTutkinnonOsat,
        validateIfSuccessful(
          validateIfTrue(
            newTutkinnonOsat,
            validateIfNonEmpty[TutkinnonOsa](
              tutkinnonOsat,
              path,
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
              case Left(_) => error(s"$path.ePerusteId", ePerusteServiceFailureMsg)
              case Right(tutkinnonOsaIdsByEPerusteId) =>
                validateIfNonEmpty[TutkinnonOsa](
                  tutkinnonOsat,
                  path,
                  _.validate(vCtx, _, tutkinnonOsaIdsByEPerusteId)
                )
              case _ =>
                validateIfNonEmpty[TutkinnonOsa](
                  tutkinnonOsat,
                  path,
                  _.validate(vCtx, _, Map())
                )
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
