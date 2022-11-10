package fi.oph.kouta.service

import fi.oph.kouta.client.KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList
import fi.oph.kouta.client.{EPerusteKoodiClient, KoulutusKoodiClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.ToteutusOid
import fi.oph.kouta.repository.{SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.validation.Validations.{ePerusteServiceFailureMsg, error, _}
import fi.oph.kouta.validation.CrudOperations.{create, update}
import fi.oph.kouta.validation.{
  IsValid,
  KoulutusDiffResolver,
  NoErrors,
  ValidationContext,
  amkKoulutustyypit,
  ammOpeErityisopeJaOpoKoulutusKoodiUrit,
  ammatillisetKoulutustyypit,
  erikoislaakariKoulutusKoodiUrit,
  lukioKoulutusKoodiUrit,
  yoKoulutustyypit
}
import fi.oph.kouta.util.MiscUtils.withoutKoodiVersion
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
      validateTarjoajat(koulutus.tarjoajat, oldKoulutus.map(_.tarjoajat).getOrElse(List())),
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
          validateKoulutusKoodiUritOfKoulutustyypit(
            yoKoulutustyypit,
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
          validateKoulutusKoodiUritOfKoulutustyypit(
            amkKoulutustyypit,
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
            ammOpeErityisopeJaOpoKoulutusKoodiUrit,
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
            lukioKoulutusKoodiUrit,
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
            erikoislaakariKoulutusKoodiUrit,
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
          validationContext.tila,
          validationContext.kielivalinta,
          ammTutkinnonOsaMetadata.tutkinnonOsat,
          koulutusDiffResolver.newTutkinnonosat()
        )

      case ammOsaamisalaKoulutusMetadata: AmmatillinenOsaamisalaKoulutusMetadata =>
        validateAmmOsaamisalaKoulutusMetadata(
          validationContext.tila,
          koulutusDiffResolver.newEPerusteId(),
          ammOsaamisalaKoulutusMetadata
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
        assertOpettajankoulutusMetadata(m.opintojenLaajuusyksikkoKoodiUri, m.tutkintonimikeKoodiUrit, m.koulutusalaKoodiUrit, validationContext)
      case m: OpePedagOpinnotKoulutusMetadata =>
        assertOpettajankoulutusMetadata(m.opintojenLaajuusyksikkoKoodiUri, m.tutkintonimikeKoodiUrit, m.koulutusalaKoodiUrit, validationContext)

      case m: LukioKoulutusMetadata =>
        and(
          assertOneAndOnlyCertainValueInSeq(
            m.koulutusalaKoodiUrit,
            "kansallinenkoulutusluokitus2016koulutusalataso1_00",
            "metadata.koulutusalaKoodiUrit"
          ),
          assertOpintojenLaajuusyksikkoKoodiUri(koulutusDiffResolver.newOpintojenLaajuusyksikkoKoodiUri(), validationContext)
        )
      case m: TuvaKoulutusMetadata =>
        validateTuvaTelma(
          validationContext,
          m.lisatiedot,
          m.linkkiEPerusteisiin,
          m.opintojenLaajuusyksikkoKoodiUri,
          koulutusDiffResolver.newOpintojenLaajuusyksikkoKoodiUri()
        )
      case m: TelmaKoulutusMetadata =>
        validateTuvaTelma(
          validationContext,
          m.lisatiedot,
          m.linkkiEPerusteisiin,
          m.opintojenLaajuusyksikkoKoodiUri,
          koulutusDiffResolver.newOpintojenLaajuusyksikkoKoodiUri()
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
      opintojenLaajuusYksikkoKoodiUri: Option[String],
      tutkintonimikeKoodiUrit: Seq[String],
      koulutusalaKoodiUrit: Seq[String],
      context: ValidationContext
  ): IsValid =
    and(
      assertEmpty(tutkintonimikeKoodiUrit, "metadata.tutkintonimikeKoodiUrit"),
      assertOpintojenLaajuusyksikkoKoodiUri(opintojenLaajuusYksikkoKoodiUri, context),
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
      validationContext: ValidationContext
  ): IsValid =
    and(
      validateKoulutusKoodiUritOfKoulutustyypit(
        ammatillisetKoulutustyypit,
        koulutus.koulutuksetKoodiUri,
        koulutusDiffResolver.newKoulutusKoodiUrit(),
        Some(1),
        validationContext
      ),
      validateIfJulkaistu(koulutus.tila, assertNotOptional(koulutus.ePerusteId, "ePerusteId")),
      validateEPeruste(koulutusDiffResolver.newEPerusteId(), "ePerusteId", koulutusDiffResolver.newKoulutusKoodiUrit())
    )

  private def validateAmmTutkinnonosaMetadata(
      tila: Julkaisutila,
      kielivalinta: Seq[Kieli],
      tutkinnonOsat: Seq[TutkinnonOsa],
      newTutkinnonOsat: Seq[TutkinnonOsa]
  ): IsValid = {
    val path = "metadata.tutkinnonOsat"
    and(
      validateIfJulkaistu(tila, assertNotEmpty(tutkinnonOsat, path)),
      validateIfNonEmpty[TutkinnonOsa](
        newTutkinnonOsat,
        path,
        (osa, path) =>
          validateIfSuccessful(
            osa.validate(tila, kielivalinta, path),
            validateIfAnyDefinedOrElse(
              Seq(osa.tutkinnonosaId, osa.tutkinnonosaViite, osa.koulutusKoodiUri), {
                val ePerusteId = osa.ePerusteId
                validateIfSuccessful(
                  validateEPeruste(
                    ePerusteId,
                    s"$path.ePerusteId",
                    Seq(osa.koulutusKoodiUri.getOrElse("")).filter(_.nonEmpty)
                  ),
                  validateIfTrue(
                    ePerusteId.isDefined && (osa.tutkinnonosaId.isDefined || osa.tutkinnonosaViite.isDefined), {
                      ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPerusteFromCache(ePerusteId.get) match {
                        case Right(tutkinnonosaViitteetAndIdt) =>
                          (osa.tutkinnonosaViite, osa.tutkinnonosaId) match {
                            case (Some(viite), Some(id)) =>
                              val viiteJaId = tutkinnonosaViitteetAndIdt.find(_._1 == viite)
                              and(
                                assertTrue(
                                  viiteJaId.isDefined,
                                  s"$path.tutkinnonosaViite",
                                  invalidTutkinnonOsaViiteForEPeruste(ePerusteId.get, viite)
                                ),
                                assertTrue(
                                  viiteJaId.isDefined && viiteJaId.get._2 == id,
                                  s"$path.tutkinnonosaId",
                                  invalidTutkinnonOsaIdForEPeruste(ePerusteId.get, id)
                                )
                              )
                            case (Some(viite), None) =>
                              assertTrue(
                                tutkinnonosaViitteetAndIdt.find(_._1 == viite).isDefined,
                                s"$path.tutkinnonosaViite",
                                invalidTutkinnonOsaViiteForEPeruste(ePerusteId.get, viite)
                              )
                            case (None, Some(id)) =>
                              assertTrue(
                                tutkinnonosaViitteetAndIdt.find(_._2 == id).isDefined,
                                s"$path.tutkinnonosaId",
                                invalidTutkinnonOsaIdForEPeruste(ePerusteId.get, id)
                              )
                            case (_, _) => NoErrors
                          }

                        case _ => error(s"$path.ePerusteId", ePerusteServiceFailureMsg)
                      }
                    }
                  )
                )
              }, {
                validateEPeruste(osa.ePerusteId, s"$path.ePerusteId", Seq())
              }
            )
          )
      )
    )
  }

  private def validateAmmOsaamisalaKoulutusMetadata(
      tila: Julkaisutila,
      ePerusteId: Option[Long],
      osaamisalaMetadata: AmmatillinenOsaamisalaKoulutusMetadata
  ): IsValid =
    and(
      validateIfJulkaistu(
        tila,
        assertNotOptional(osaamisalaMetadata.osaamisalaKoodiUri, "metadata.osaamisalaKoodiUri")
      ),
      validateIfDefined[String](
        osaamisalaMetadata.osaamisalaKoodiUri,
        koodiUri =>
          validateIfDefined[Long](
            ePerusteId,
            ePerusteId =>
              validateIfTrue(
                ePerusteId > 0, {
                  ePerusteKoodiClient
                    .getOsaamisalaKoodiuritForEPerusteFromCache(ePerusteId) match {
                    case Right(osaamisalaKoodiuritForEPeruste) =>
                      assertTrue(
                        koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, osaamisalaKoodiuritForEPeruste),
                        "metadata.osaamisalaKoodiUri",
                        invalidOsaamisalaForEPeruste(ePerusteId, koodiUri)
                      )
                    case _ => error("ePerusteId", ePerusteServiceFailureMsg)
                  }
                }
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
      assertOpintojenLaajuusyksikkoKoodiUri(koulutusDiffResolver.newOpintojenLaajuusyksikkoKoodiUri(), validationContext)
    )

  private def validateTuvaTelma(
      validationContext: ValidationContext,
      lisatiedot: Seq[Lisatieto],
      linkkiEPerusteisiin: Kielistetty,
      opintojenLaajuusYksikkoKoodiUri: Option[String],
      newOpintojenLaajuusYksikkoKoodiUri: Option[String]
  ): IsValid = {
    and(
      assertEmpty(lisatiedot, "metadata.lisatiedot"),
      validateIfNonEmpty(linkkiEPerusteisiin, "metadata.linkkiEPerusteisiin", assertValidUrl _),
      assertOpintojenLaajuusKoodiUri(newOpintojenLaajuusYksikkoKoodiUri, validationContext),
      validateIfJulkaistu(
        validationContext.tila,
        and(
          assertNotOptional(opintojenLaajuusYksikkoKoodiUri, "metadata.opintojenLaajuusYksikkoKoodiUri"), //fixme, check tyyppi?
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
    and(
      validateVapaaSivistystyoKoulutus(validationContext, koulutusDiffResolver, metadata),
      assertOpintojenLaajuusyksikkoKoodiUri(koulutusDiffResolver.newOpintojenLaajuusyksikkoKoodiUri(), validationContext),
      validateIfJulkaistu(
        validationContext.tila,
        assertNotOptional(metadata.opintojenLaajuusyksikkoKoodiUri, "metadata.opintojenLaajuusyksikkoKoodiUri")
      ),
      validateOpintojenLaajuusyksikkoAndNumero(
        metadata.opintojenLaajuusyksikkoKoodiUri,
        koulutusDiffResolver.newOpintojenLaajuusyksikkoKoodiUri(),
        metadata.opintojenLaajuusNumero,
        false,
        validationContext
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

  // Oletus: koodiUriFilter:in URIt eivät sisällä versiotietoa; tarkistetun koodiUrin versiota ei verrata koodiUriFilterissä
  // mahdollisesti annettuihin versioihin.
  private def validateKoulutusKoodiUrit(
      koodiUriFilter: Seq[String],
      koulutusKoodiUrit: Seq[String],
      newKoulutusKoodiUrit: Seq[String],
      maxNbrOfKoodit: Option[Int],
      validationContext: ValidationContext
  ): IsValid =
    validateKoulutusKooditIfJulkaistu(
      validationContext.tila,
      koulutusKoodiUrit,
      maxNbrOfKoodit,
      validateIfNonEmpty[String](
        newKoulutusKoodiUrit,
        "koulutuksetKoodiUri",
        (koodiUri, path) =>
          assertKoulutuskoodiQueryResult(
            koodiUri,
            koodiUriFilter,
            koulutusKoodiClient,
            path,
            validationContext,
            invalidKoulutuskoodiuri(koodiUri)
          )
      )
    )

  private def validateKoulutusKoodiUritOfKoulutustyypit(
      koulutusTyypit: Seq[String],
      koulutusKoodiUrit: Seq[String],
      newKoulutusKoodiUrit: Seq[String],
      maxNbrOfKoodit: Option[Int],
      validationContext: ValidationContext
  ): IsValid = validateKoulutusKooditIfJulkaistu(
    validationContext.tila,
    koulutusKoodiUrit,
    maxNbrOfKoodit,
    validateIfNonEmpty[String](
      newKoulutusKoodiUrit,
      "koulutuksetKoodiUri",
      (koodiUri, path) =>
        assertKoulutustyyppiQueryResult(
          koodiUri,
          koulutusTyypit,
          koulutusKoodiClient,
          path,
          validationContext,
          invalidKoulutuskoodiuri(koodiUri)
        )
    )
  )

  private def validateKoulutusKooditIfJulkaistu(
      tila: Julkaisutila,
      koulutusKoodiUrit: Seq[String],
      maxNbrOfKoodit: Option[Int],
      checkFromKoodisto: => IsValid
  ): IsValid = {
    validateIfJulkaistu(
      tila,
      validateIfSuccessful(
        and(
          assertNotEmpty(koulutusKoodiUrit, "koulutuksetKoodiUri"),
          validateIfDefined[Int](
            maxNbrOfKoodit,
            nbr => assertTrue(koulutusKoodiUrit.size <= nbr, "koulutuksetKoodiUri", tooManyKoodiUris)
          )
        ),
        checkFromKoodisto
      )
    )
  }
  private def validateEPeruste(ePerusteId: Option[Long], path: String, koulutusKoodiUrit: Seq[String]): IsValid = {
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
  }

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
