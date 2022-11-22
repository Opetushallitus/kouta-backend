package fi.oph.kouta.service

import fi.oph.kouta.client.KoodistoUtils.{koodiUriFromString, koodiUriWithEqualOrHigherVersioNbrInList, koodiUrisEqual}
import fi.oph.kouta.client.{EPerusteKoodiClient, KoulutusKoodiClient, TutkinnonOsaServiceItem}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.ToteutusOid
import fi.oph.kouta.repository.{SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.util.MiscUtils.withoutKoodiVersion
import fi.oph.kouta.validation.CrudOperations.{create, update}
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation._

object KoulutusServiceValidation
    extends KoulutusServiceValidation(
      KoulutusKoodiClient,
      EPerusteKoodiClient,
      OrganisaatioServiceImpl,
      ToteutusDAO,
      SorakuvausDAO
    )

class KoulutusServiceValidation(
    val koulutusKoodiClient: KoulutusKoodiClient,
    ePerusteKoodiClient: EPerusteKoodiClient,
    val organisaatioService: OrganisaatioService,
    toteutusDAO: ToteutusDAO,
    val sorakuvausDAO: SorakuvausDAO
) extends KoulutusToteutusValidatingService[Koulutus] {

  override def validateEntity(koulutus: Koulutus, oldKoulutus: Option[Koulutus]): IsValid = {
    val validationContext =
      ValidationContext(koulutus.tila, koulutus.kielivalinta, if (oldKoulutus.isDefined) update else create)
    val koulutusDiffResolver = KoulutusDiffResolver(koulutus, oldKoulutus)
    val commonErrors         = validateCommonParameters(koulutus, oldKoulutus)
    val koulutusLevelErrors =
      validateKoulutustyyppiSpecificParameters(koulutus, validationContext, koulutusDiffResolver)
    val metadataErrors = validateMetadata(koulutus, validationContext, koulutusDiffResolver)

    Seq(commonErrors, koulutusLevelErrors, metadataErrors).flatten.distinct
  }

  private def validateMetadata(
      koulutus: Koulutus,
      validationContext: ValidationContext,
      koulutusDiffResolver: KoulutusDiffResolver
  ): IsValid = {
    koulutus.metadata match {
      case Some(metadata) =>
        and(
          validateCommonMetadataParameters(
            koulutus.koulutustyyppi,
            metadata,
            validationContext,
            koulutusDiffResolver
          ),
          validateMetadataSpecificParameters(
            koulutus,
            validationContext,
            koulutusDiffResolver
          )
        )
      case _ => if (koulutus.tila == Julkaistu) error("metadata", missingMsg) else NoErrors
    }
  }

  private def validateCommonParameters(koulutus: Koulutus, oldKoulutus: Option[Koulutus]): IsValid = {
    val tila   = koulutus.tila
    val tyyppi = koulutus.koulutustyyppi
    and(
      koulutus.validate(),
      validateIfTrueOrElse(
        oldKoulutus.isDefined,
        and(
          assertTrue(
            tyyppi == oldKoulutus.get.koulutustyyppi,
            "koulutustyyppi",
            notModifiableMsg("koulutustyyppiä", "koulutukselle")
          ),
          assertNotOptional(koulutus.oid, "oid")
        ),
        assertNotDefined(koulutus.oid, "oid")
      ),
      validateTarjoajat(tyyppi, koulutus.tarjoajat, oldKoulutus.map(_.tarjoajat).getOrElse(List())),
      validateIfJulkaistu(
        tila,
        and(
          assertTrue(
            koulutus.johtaaTutkintoon == Koulutustyyppi.isTutkintoonJohtava(tyyppi),
            "johtaaTutkintoon",
            invalidTutkintoonjohtavuus(tyyppi.toString)
          ),
          validateIfDefined[String](koulutus.teemakuva, assertValidUrl(_, "teemakuva"))
        )
      )
    )
  }

  private def validateKoulutustyyppiSpecificParameters(
      koulutus: Koulutus,
      validationContext: ValidationContext,
      koulutusDiffResolver: KoulutusDiffResolver
  ): IsValid = {
    koulutus.koulutustyyppi match {
      case Amm | AmmOsaamisala =>
        and(
          validateSorakuvaus(koulutus),
          validateAmmatillinenKoulutus(koulutus, koulutusDiffResolver, validationContext)
        )
      case Yo =>
        and(
          validateSorakuvaus(koulutus),
          validateKoulutusKoodiUrit(
            YoKoulutusKoodit,
            koulutus.koulutuksetKoodiUri,
            koulutusDiffResolver.newKoulutusKoodiUrit(),
            None,
            validationContext
          ),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      case Amk =>
        and(
          validateSorakuvaus(koulutus),
          validateKoulutusKoodiUrit(
            AmkKoulutusKoodit,
            koulutus.koulutuksetKoodiUri,
            koulutusDiffResolver.newKoulutusKoodiUrit(),
            None,
            validationContext
          ),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      case AmmOpeErityisopeJaOpo =>
        and(
          validateSorakuvaus(koulutus),
          validateKoulutusKoodiUrit(
            AmmOpeErityisopeJaOpoKoulutusKoodit,
            koulutus.koulutuksetKoodiUri,
            koulutusDiffResolver.newKoulutusKoodiUrit(),
            Some(1),
            validationContext
          ),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      case OpePedagOpinnot =>
        and(
          validateSorakuvaus(koulutus),
          assertOneAndOnlyCertainValueInSeq(
            koulutus.koulutuksetKoodiUri,
            "koulutus_919999",
            "koulutuksetKoodiUri"
          ),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      case Lk =>
        and(
          validateSorakuvaus(koulutus),
          validateKoulutusKoodiUrit(
            LukioKoulutusKoodit,
            koulutus.koulutuksetKoodiUri,
            koulutusDiffResolver.newKoulutusKoodiUrit(),
            Some(1),
            validationContext
          ),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      case AikuistenPerusopetus =>
        and(
          assertNotDefined(koulutus.sorakuvausId, "sorakuvausId"),
          assertOneAndOnlyCertainValueInSeq(
            koulutus.koulutuksetKoodiUri,
            "koulutus_201101",
            "koulutuksetKoodiUri"
          ),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      case Erikoislaakari =>
        and(
          validateKoulutusKoodiUrit(
            ErikoislaakariKoulutusKoodit,
            koulutus.koulutuksetKoodiUri,
            koulutusDiffResolver.newKoulutusKoodiUrit(),
            Some(1),
            validationContext
          ),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      case AmmMuu | VapaaSivistystyoMuu =>
        and(
          assertNotDefined(koulutus.sorakuvausId, "sorakuvausId"),
          assertEmpty(koulutus.koulutuksetKoodiUri, "koulutuksetKoodiUri"),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      // TODO: Lisättävä näille koulutustyypeille validointi koulutuksetKoodiUri-kentälle
      case Tuva | Telma | VapaaSivistystyoOpistovuosi =>
        and(
          assertNotDefined(koulutus.sorakuvausId, "sorakuvausId"),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      case _ =>
        and(
          validateSorakuvaus(koulutus),
          assertEmpty(koulutus.koulutuksetKoodiUri, "koulutuksetKoodiUri"),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
    }
  }

  private def validateSorakuvaus(koulutus: Koulutus): IsValid =
    validateSorakuvausIntegrity(
      koulutus.sorakuvausId,
      koulutus.tila,
      koulutus.koulutustyyppi,
      entityKoulutusKoodiUrit = koulutus.koulutuksetKoodiUri
    )

  private def validateCommonMetadataParameters(
      tyyppi: Koulutustyyppi,
      metadata: KoulutusMetadata,
      validationContext: ValidationContext,
      koulutusDiffResolver: KoulutusDiffResolver
  ): IsValid = {
    val koulutustyypitWithMandatoryKuvaus: Set[Koulutustyyppi] =
      Set(
        AmmMuu,
        Tuva,
        Telma,
        VapaaSivistystyoOpistovuosi,
        VapaaSivistystyoMuu,
        AikuistenPerusopetus,
        KkOpintojakso,
        KkOpintokokonaisuus,
        Erikoislaakari
      )
    val koulutustyypitWithoutLisatiedot: Set[Koulutustyyppi] =
      Set(AmmMuu, Tuva, Telma, VapaaSivistystyoOpistovuosi, VapaaSivistystyoMuu, AikuistenPerusopetus)

    and(
      assertTrue(metadata.tyyppi == tyyppi, s"metadata.tyyppi", InvalidMetadataTyyppi),
      validateIfTrueOrElse(
        koulutustyypitWithoutLisatiedot.contains(tyyppi),
        assertEmpty(metadata.lisatiedot, "metadata.lisatiedot"),
        validateIfNonEmptySeq[Lisatieto](
          metadata.lisatiedot,
          koulutusDiffResolver.newLisatiedot(),
          "metadata.lisatiedot",
          (lisatieto, newLisatieto, path) =>
            lisatieto
              .validate(path, newLisatieto, validationContext, koulutusKoodiClient.lisatiedotOtsikkoKoodiUriExists)
        )
      ),
      validateIfJulkaistu(
        validationContext.tila,
        validateIfTrueOrElse(
          koulutustyypitWithMandatoryKuvaus.contains(tyyppi),
          validateKielistetty(validationContext.kielivalinta, metadata.kuvaus, "metadata.kuvaus"),
          validateOptionalKielistetty(validationContext.kielivalinta, metadata.kuvaus, "metadata.kuvaus")
        )
      )
    )
  }

  private def validateMetadataSpecificParameters(
      koulutus: Koulutus,
      validationContext: ValidationContext,
      koulutusDiffResolver: KoulutusDiffResolver
  ): IsValid = {
    koulutus.metadata.get match {
      case ammTutkinnonOsaMetadata: AmmatillinenTutkinnonOsaKoulutusMetadata =>
        validateAmmTutkinnonosaMetadata(
          validationContext,
          koulutusDiffResolver.newNimi(),
          ammTutkinnonOsaMetadata.tutkinnonOsat,
          koulutusDiffResolver.newTutkinnonosat().nonEmpty
        )

      case ammOsaamisalaKoulutusMetadata: AmmatillinenOsaamisalaKoulutusMetadata =>
        validateAmmOsaamisalaKoulutusMetadata(
          validationContext.tila,
          ammOsaamisalaKoulutusMetadata,
          koulutus.ePerusteId,
          koulutusDiffResolver
        )

      case m: AmmatillinenMuuKoulutusMetadata =>
        and(
          assertKoulutusalaKoodiUrit(koulutusDiffResolver.newKoulutusalaKoodiUrit(), validationContext),
          validateOpintojenLaajuusyksikkoAndNumero(
            m.opintojenLaajuusyksikkoKoodiUri,
            koulutusDiffResolver.newOpintojenLaajuusyksikkoKoodiUri(),
            m.opintojenLaajuusNumero,
            true,
            validationContext
          )
        )
      case _: YliopistoKoulutusMetadata =>
        validateKorkeaKoulutusMetadata(validationContext, koulutusDiffResolver)
      case _: AmmattikorkeakouluKoulutusMetadata =>
        validateKorkeaKoulutusMetadata(validationContext, koulutusDiffResolver)
      case m: AmmOpeErityisopeJaOpoKoulutusMetadata =>
        assertOpettajankoulutusMetadata(m.opintojenLaajuusKoodiUri, m.tutkintonimikeKoodiUrit, m.koulutusalaKoodiUrit)
      case m: OpePedagOpinnotKoulutusMetadata =>
        assertOpettajankoulutusMetadata(m.opintojenLaajuusKoodiUri, m.tutkintonimikeKoodiUrit, m.koulutusalaKoodiUrit)

      case m: LukioKoulutusMetadata =>
        and(
          assertOneAndOnlyCertainValueInSeq(
            m.koulutusalaKoodiUrit,
            "kansallinenkoulutusluokitus2016koulutusalataso1_00",
            "metadata.koulutusalaKoodiUrit"
          ),
          assertOpintojenLaajuusKoodiUri(koulutusDiffResolver.newOpintojenLaajuusKoodiUri(), validationContext)
        )
      case m: TuvaKoulutusMetadata =>
        validateTuvaTelma(
          validationContext,
          m.lisatiedot,
          m.linkkiEPerusteisiin,
          m.opintojenLaajuusKoodiUri,
          koulutusDiffResolver.newOpintojenLaajuusKoodiUri()
        )
      case m: TelmaKoulutusMetadata =>
        validateTuvaTelma(
          validationContext,
          m.lisatiedot,
          m.linkkiEPerusteisiin,
          m.opintojenLaajuusKoodiUri,
          koulutusDiffResolver.newOpintojenLaajuusKoodiUri()
        )
      case m: VapaaSivistystyoOpistovuosiKoulutusMetadata =>
        validateVapaaSivistystyoOpistovuosiKoulutus(
          validationContext,
          koulutusDiffResolver,
          m
        )
      case m: VapaaSivistystyoMuuKoulutusMetadata =>
        validateVapaaSivistystyoMuuKoulutus(
          validationContext,
          koulutusDiffResolver,
          m
        )
      case m: AikuistenPerusopetusKoulutusMetadata =>
        and(
          assertEmpty(m.lisatiedot, "metadata.lisatiedot"),
          validateIfNonEmpty(m.linkkiEPerusteisiin, "metadata.linkkiEPerusteisiin", assertValidUrl _),
          validateOpintojenLaajuusyksikkoAndNumero(
            m.opintojenLaajuusyksikkoKoodiUri,
            koulutusDiffResolver.newOpintojenLaajuusyksikkoKoodiUri(),
            m.opintojenLaajuusNumero,
            true,
            validationContext
          ),
          validateIfJulkaistu(
            validationContext.tila,
            and(
              validateOptionalKielistetty(
                validationContext.kielivalinta,
                m.linkkiEPerusteisiin,
                "metadata.linkkiEPerusteisiin"
              )
            )
          )
        )
      case m: KkOpintojaksoKoulutusMetadata =>
        and(
          assertKoulutusalaKoodiUrit(koulutusDiffResolver.newKoulutusalaKoodiUrit(), validationContext),
          validateOpintojenLaajuusyksikkoAndNumero(
            m.opintojenLaajuusyksikkoKoodiUri,
            koulutusDiffResolver.newOpintojenLaajuusyksikkoKoodiUri(),
            m.opintojenLaajuusNumero,
            false,
            validationContext
          )
        )
      case m: KkOpintokokonaisuusKoulutusMetadata =>
        and(
          assertKoulutusalaKoodiUrit(koulutusDiffResolver.newKoulutusalaKoodiUrit(), validationContext),
          validateOpintojenLaajuusyksikko(
            m.opintojenLaajuusyksikkoKoodiUri,
            koulutusDiffResolver.newOpintojenLaajuusyksikkoKoodiUri(),
            false,
            validationContext
          ),
          validateOpintojenLaajuusNumerot(
            koulutus.tila,
            m.opintojenLaajuusNumeroMin,
            m.opintojenLaajuusNumeroMax,
            false
          ),
          validateOpintojenLaajuusIntegrity(koulutus)
        )
      case m: ErikoislaakariKoulutusMetadata =>
        and(
          assertOneAndOnlyCertainValueInSeq(
            m.koulutusalaKoodiUrit,
            "kansallinenkoulutusluokitus2016koulutusalataso2_091",
            "metadata.koulutusalaKoodiUrit"
          ),
          assertTutkintonimikeKoodiUrit(koulutusDiffResolver.newTutkintonimikeKoodiUrit(), validationContext)
        )
      case _ => NoErrors
    }
  }

  private def assertOpettajankoulutusMetadata(
      opintojenLaajuusKoodiUri: Option[String],
      tutkintonimikeKoodiUrit: Seq[String],
      koulutusalaKoodiUrit: Seq[String]
  ): IsValid =
    and(
      assertEmpty(tutkintonimikeKoodiUrit, "metadata.tutkintonimikeKoodiUrit"),
      assertCertainValue(
        opintojenLaajuusKoodiUri,
        "opintojenlaajuus_60",
        "metadata.opintojenLaajuusKoodiUri"
      ),
      assertOneAndOnlyCertainValueInSeq(
        koulutusalaKoodiUrit,
        "kansallinenkoulutusluokitus2016koulutusalataso1_01",
        "metadata.koulutusalaKoodiUrit"
      )
    )

  private def validateOpintojenLaajuusIntegrity(
      k: Koulutus
  ): IsValid = {
    var errors: List[ValidationError]                    = List()
    var errorMap: Map[String, List[Option[ToteutusOid]]] = Map()

    val addErrorOid = (errorKey: String, toteutusOid: Option[ToteutusOid]) => {
      errorMap += (errorKey -> (errorMap.getOrElse(errorKey, List()) ++ List(toteutusOid)))
    }

    (k.oid, k.metadata) match {
      case (Some(koulutusOid), Some(km: KkOpintokokonaisuusKoulutusMetadata)) => {
        val toteutukset = toteutusDAO.getByKoulutusOid(koulutusOid, TilaFilter.onlyJulkaistut())

        val laajuusMin: Double = km.opintojenLaajuusNumeroMin.getOrElse(0)
        val laajuusMax: Double = km.opintojenLaajuusNumeroMax.getOrElse(Double.PositiveInfinity)

        if (k.tila == Julkaistu) {
          toteutukset
            .foreach(t => {
              t.metadata match {
                case Some(toteutusMetadata: KkOpintokokonaisuusToteutusMetadata) => {
                  toteutusMetadata.opintojenLaajuusNumero.foreach(toteutusLaajuusNumero => {
                    if (toteutusLaajuusNumero < laajuusMin) {
                      addErrorOid("metadata.opintojenLaajuusNumeroMin", t.oid)
                    }
                    if (toteutusLaajuusNumero > laajuusMax) {
                      addErrorOid("metadata.opintojenLaajuusNumeroMax", t.oid)
                    }

                    (km.opintojenLaajuusyksikkoKoodiUri, toteutusMetadata.opintojenLaajuusyksikkoKoodiUri) match {
                      case (Some(koulutusLaajuusYksikkoKoodiUri), Some(toteutusLaajuusYksikkoKoodiUri)) => {
                        if (
                          withoutKoodiVersion(koulutusLaajuusYksikkoKoodiUri) != withoutKoodiVersion(
                            toteutusLaajuusYksikkoKoodiUri
                          )
                        ) {
                          addErrorOid("metadata.opintojenLaajuusyksikkoKoodiUri", t.oid)
                        }
                      }
                      case _ =>
                    }
                  })
                }
                case _ =>
              }
            })
        }
        errors = errorMap.toList.map(value => {
          val errorKey    = value._1
          val toteutukset = value._2.flatten
          ValidationError(
            errorKey,
            errorKey match {
              case "metadata.opintojenLaajuusNumeroMin" =>
                invalidKoulutusOpintojenLaajuusNumeroIntegrity(laajuusMin, laajuusMax, toteutukset)
              case "metadata.opintojenLaajuusNumeroMax" =>
                invalidKoulutusOpintojenLaajuusNumeroIntegrity(laajuusMin, laajuusMax, toteutukset)
              case "metadata.opintojenLaajuusyksikkoKoodiUri" =>
                invalidKoulutusOpintojenLaajuusyksikkoIntegrity(km.opintojenLaajuusyksikkoKoodiUri.get, toteutukset)
            }
          )
        })
      }
      case _ =>
    }
    if (errors.isEmpty) NoErrors else errors
  }

  private def validateAmmatillinenKoulutus(
      koulutus: Koulutus,
      koulutusDiffResolver: KoulutusDiffResolver,
      vCtx: ValidationContext
  ): IsValid =
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
          koulutus.koulutustyyppi == Amm && koulutus.koulutuksetKoodiUri.nonEmpty && (koulutusDiffResolver
            .newKoulutusKoodiUrit()
            .nonEmpty || koulutusDiffResolver.newNimi().isDefined),
          koulutusKoodiClient.getKoodiUriVersionOrLatestFromCache(koulutus.koulutuksetKoodiUri.head) match {
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
      validateEPeruste(koulutusDiffResolver.newEPerusteId(), "ePerusteId", koulutusDiffResolver.newKoulutusKoodiUrit())
    )

  private def validateAmmTutkinnonosaMetadata(
      vCtx: ValidationContext,
      newNimi: Option[Kielistetty],
      tutkinnonOsat: Seq[TutkinnonOsa],
      newTutkinnonOsat: Boolean
  ): IsValid = {
    val path = "metadata.tutkinnonOsat"
    def nimiShouldBeValidated(
        newNimi: Option[Kielistetty],
        currentTutkinnonOsat: Seq[TutkinnonOsa],
        tutkinnonOsatFromService: Map[Long, Seq[TutkinnonOsaServiceItem]] = Map()
    ): Boolean = {
      if (newNimi.isDefined && currentTutkinnonOsat.size == 1)
        (currentTutkinnonOsat.head.tutkinnonosaViite, currentTutkinnonOsat.head.tutkinnonosaId) match {
          case (Some(viiteId), Some(osaId)) =>
            tutkinnonOsatFromService.isEmpty || tutkinnonOsatFromService.head._2.exists(osa =>
              osa.viiteId == viiteId && osa.id == osaId
            )
          case _ => false
        }
      else
        false
    }

    and(
      validateIfJulkaistu(vCtx.tila, assertNotEmpty(tutkinnonOsat, path)),
      validateIfTrue(
        nimiShouldBeValidated(newNimi, tutkinnonOsat) || newTutkinnonOsat,
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
                and(
                  validateIfNonEmpty[TutkinnonOsa](
                    tutkinnonOsat,
                    path,
                    _.validate(vCtx, _, tutkinnonOsaIdsByEPerusteId)
                  ),
                  validateIfTrue(
                    nimiShouldBeValidated(
                      newNimi,
                      tutkinnonOsat,
                      tutkinnonOsaIdsByEPerusteId
                    ), {
                      val viiteId = tutkinnonOsat.head.tutkinnonosaViite.get
                      val osaId   = tutkinnonOsat.head.tutkinnonosaId.get
                      assertNimiMatchExternal(
                        newNimi.getOrElse(Map()),
                        tutkinnonOsaIdsByEPerusteId.head._2
                          .find(osa => osa.viiteId == viiteId && osa.id == osaId)
                          .map(_.nimi)
                          .getOrElse(Map()),
                        "nimi",
                        "tutkinnonosassa"
                      )
                    }
                  )
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

  private def validateKorkeaKoulutusMetadata(
      validationContext: ValidationContext,
      koulutusDiffResolver: KoulutusDiffResolver
  ): IsValid =
    and(
      assertKoulutusalaKoodiUrit(koulutusDiffResolver.newKoulutusalaKoodiUrit(), validationContext),
      assertTutkintonimikeKoodiUrit(koulutusDiffResolver.newTutkintonimikeKoodiUrit(), validationContext),
      assertOpintojenLaajuusKoodiUri(koulutusDiffResolver.newOpintojenLaajuusKoodiUri(), validationContext)
    )

  private def validateTuvaTelma(
      validationContext: ValidationContext,
      lisatiedot: Seq[Lisatieto],
      linkkiEPerusteisiin: Kielistetty,
      opintojenLaajuusKoodiUri: Option[String],
      newOpintojenLaajuusKoodiUri: Option[String]
  ): IsValid = {
    and(
      assertEmpty(lisatiedot, "metadata.lisatiedot"),
      validateIfNonEmpty(linkkiEPerusteisiin, "metadata.linkkiEPerusteisiin", assertValidUrl _),
      assertOpintojenLaajuusKoodiUri(newOpintojenLaajuusKoodiUri, validationContext),
      validateIfJulkaistu(
        validationContext.tila,
        and(
          assertNotOptional(opintojenLaajuusKoodiUri, "metadata.opintojenLaajuusKoodiUri"),
          validateOptionalKielistetty(
            validationContext.kielivalinta,
            linkkiEPerusteisiin,
            "metadata.linkkiEPerusteisiin"
          )
        )
      )
    )
  }

  private def validateVapaaSivistystyoKoulutus(
      validationContext: ValidationContext,
      koulutusDiffResolver: KoulutusDiffResolver,
      metadata: VapaaSivistystyoKoulutusMetadata
  ): IsValid = {
    val ePerusteLinkki = metadata.linkkiEPerusteisiin
    and(
      assertKoulutusalaKoodiUrit(koulutusDiffResolver.newKoulutusalaKoodiUrit(), validationContext),
      validateIfNonEmpty(ePerusteLinkki, "metadata.linkkiEPerusteisiin", assertValidUrl _),
      validateIfJulkaistu(
        validationContext.tila,
        validateOptionalKielistetty(validationContext.kielivalinta, ePerusteLinkki, "metadata.linkkiEPerusteisiin")
      )
    )
  }

  private def validateVapaaSivistystyoOpistovuosiKoulutus(
      validationContext: ValidationContext,
      koulutusDiffResolver: KoulutusDiffResolver,
      metadata: VapaaSivistystyoOpistovuosiKoulutusMetadata
  ): IsValid = {
    val laajuusKoodiUri = metadata.opintojenLaajuusKoodiUri
    and(
      validateVapaaSivistystyoKoulutus(validationContext, koulutusDiffResolver, metadata),
      assertOpintojenLaajuusKoodiUri(koulutusDiffResolver.newOpintojenLaajuusKoodiUri(), validationContext),
      validateIfJulkaistu(
        validationContext.tila,
        assertNotOptional(laajuusKoodiUri, "metadata.opintojenLaajuusKoodiUri")
      )
    )
  }

  private def validateVapaaSivistystyoMuuKoulutus(
      validationContext: ValidationContext,
      koulutusDiffResolver: KoulutusDiffResolver,
      metadata: VapaaSivistystyoMuuKoulutusMetadata
  ): IsValid =
    and(
      validateVapaaSivistystyoKoulutus(validationContext, koulutusDiffResolver, metadata),
      validateOpintojenLaajuusyksikkoAndNumero(
        metadata.opintojenLaajuusyksikkoKoodiUri,
        koulutusDiffResolver.newOpintojenLaajuusyksikkoKoodiUri(),
        metadata.opintojenLaajuusNumero,
        true,
        validationContext
      )
    )

  private def validateKoulutusKoodiUrit(
      koodiUriFilter: KoulutusKoodiFilter,
      koulutusKoodiUrit: Seq[String],
      newKoulutusKoodiUrit: Seq[String],
      maxNbrOfKoodit: Option[Int],
      vCtx: ValidationContext
  ): IsValid =
    validateIfJulkaistu(
      vCtx.tila,
      validateIfSuccessful(
        assertKoulutusKoodiuriAmount(koulutusKoodiUrit, maxNbrOfKoodit),
        validateIfNonEmpty[String](
          newKoulutusKoodiUrit,
          "koulutuksetKoodiUri",
          (koodiUri, path) =>
            assertKoulutuskoodiQueryResult(
              koodiUri,
              koodiUriFilter,
              koulutusKoodiClient,
              path,
              vCtx,
              invalidKoulutuskoodiuri(koodiUri)
            )
        )
      )
    )

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

  private def assertKoulutusalaKoodiUrit(koodiUrit: Seq[String], validationContext: ValidationContext): IsValid = {
    validateIfNonEmpty[String](
      koodiUrit,
      "metadata.koulutusalaKoodiUrit",
      (koodiUri, path) =>
        assertKoodistoQueryResult(
          koodiUri,
          koulutusKoodiClient.koulutusalaKoodiUriExists,
          path,
          validationContext,
          invalidKoulutusAlaKoodiuri(koodiUri)
        )
    )
  }

  private def assertTutkintonimikeKoodiUrit(koodiUrit: Seq[String], validationContext: ValidationContext): IsValid = {
    validateIfNonEmpty[String](
      koodiUrit,
      "metadata.tutkintonimikeKoodiUrit",
      (koodiUri, path) =>
        assertKoodistoQueryResult(
          koodiUri,
          koulutusKoodiClient.tutkintoNimikeKoodiUriExists,
          path,
          validationContext,
          invalidTutkintoNimikeKoodiuri(koodiUri)
        )
    )
  }

  private def assertOpintojenLaajuusKoodiUri(koodiUri: Option[String], validationContext: ValidationContext): IsValid =
    validateIfDefined[String](
      koodiUri,
      uri =>
        assertKoodistoQueryResult(
          uri,
          koulutusKoodiClient.opintojenLaajuusKoodiUriExists,
          "metadata.opintojenLaajuusKoodiUri",
          validationContext,
          invalidOpintojenLaajuusKoodiuri(uri)
        )
    )

  private def assertOpintojenLaajuusyksikkoKoodiUri(
      koodiUri: Option[String],
      validationContext: ValidationContext
  ): IsValid =
    validateIfDefined[String](
      koodiUri,
      uri =>
        assertKoodistoQueryResult(
          uri,
          koulutusKoodiClient.opintojenLaajuusyksikkoKoodiUriExists,
          "metadata.opintojenLaajuusyksikkoKoodiUri",
          validationContext,
          invalidOpintojenLaajuusyksikkoKoodiuri(uri)
        )
    )

  private def validateOpintojenLaajuusyksikkoAndNumero(
      laajuusyksikkoKoodiUri: Option[String],
      newLaajuusyksikkoKoodiUri: Option[String],
      laajuusNumero: Option[Double],
      mandatoryIfJulkaistu: Boolean,
      validationContext: ValidationContext
  ): IsValid =
    and(
      assertOpintojenLaajuusyksikkoKoodiUri(newLaajuusyksikkoKoodiUri, validationContext),
      validateIfDefined[Double](
        laajuusNumero,
        assertNotNegative(_, "metadata.opintojenLaajuusNumero")
      ),
      validateIfTrue(
        mandatoryIfJulkaistu,
        validateIfJulkaistu(
          validationContext.tila,
          and(
            assertNotOptional(laajuusyksikkoKoodiUri, "metadata.opintojenLaajuusyksikkoKoodiUri"),
            assertNotOptional(laajuusNumero, "metadata.opintojenLaajuusNumero")
          )
        )
      )
    )

  private def validateOpintojenLaajuusNumerot(
      tila: Julkaisutila,
      laajuusNumeroMin: Option[Double],
      laajuusNumeroMax: Option[Double],
      mandatoryIfJulkaistu: Boolean
  ): IsValid =
    and(
      validateIfDefined[Double](
        laajuusNumeroMin,
        assertNotNegative(_, "metadata.opintojenLaajuusNumeroMin")
      ),
      validateIfDefined[Double](
        laajuusNumeroMax,
        assertNotNegative(_, "metadata.opintojenLaajuusNumeroMax")
      ),
      validateMinMax(laajuusNumeroMin, laajuusNumeroMax, s"metadata.opintojenLaajuusNumeroMin"),
      validateIfTrue(
        mandatoryIfJulkaistu,
        validateIfJulkaistu(
          tila,
          and(
            assertNotOptional(laajuusNumeroMin, "metadata.opintojenLaajuusNumeroMin"),
            assertNotOptional(laajuusNumeroMin, "metadata.opintojenLaajuusNumeroMax")
          )
        )
      )
    )

  private def validateOpintojenLaajuusyksikko(
      koodiUri: Option[String],
      newKoodiUri: Option[String],
      mandatoryIfJulkaistu: Boolean,
      validationContext: ValidationContext
  ): IsValid =
    and(
      assertOpintojenLaajuusyksikkoKoodiUri(newKoodiUri, validationContext),
      validateIfTrue(
        mandatoryIfJulkaistu,
        validateIfJulkaistu(
          validationContext.tila,
          and(
            assertNotOptional(koodiUri, "metadata.opintojenLaajuusyksikkoKoodiUri")
          )
        )
      )
    )

  override def validateInternalDependenciesWhenDeletingEntity(koulutus: Koulutus): IsValid = {
    assertTrue(
      toteutusDAO.getByKoulutusOid(koulutus.oid.get, TilaFilter.onlyOlemassaolevat()).isEmpty,
      "tila",
      integrityViolationMsg("Koulutusta", "toteutuksia")
    )
  }
}
