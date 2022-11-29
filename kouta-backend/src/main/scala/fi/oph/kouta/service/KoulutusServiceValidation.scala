package fi.oph.kouta.service

import fi.oph.kouta.client.KoodistoUtils.{koodiUriFromString, koodiUriWithEqualOrHigherVersioNbrInList, koodiUrisEqual}
import fi.oph.kouta.client.{EPerusteKoodiClient, KoulutusKoodiClient, TutkinnonOsaServiceItem}
import fi.oph.kouta.domain.Koulutustyyppi.isAmmatillinen
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.ToteutusOid
import fi.oph.kouta.repository.{SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.service.validation.AmmatillinenKoulutusServiceValidation
import fi.oph.kouta.util.MiscUtils.withoutKoodiVersion
import fi.oph.kouta.validation.CrudOperations.{create, update}
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation._

object KoulutusServiceValidation
    extends KoulutusServiceValidation(
      KoulutusKoodiClient,
      OrganisaatioServiceImpl,
      ToteutusDAO,
      SorakuvausDAO,
      AmmatillinenKoulutusServiceValidation
    )

class KoulutusServiceValidation(
    val koulutusKoodiClient: KoulutusKoodiClient,
    val organisaatioService: OrganisaatioService,
    toteutusDAO: ToteutusDAO,
    val sorakuvausDAO: SorakuvausDAO,
    ammatillinenKoulutusServiceValidation: AmmatillinenKoulutusServiceValidation
) extends KoulutusToteutusValidatingService[Koulutus] {

  override def validateEntity(koulutus: Koulutus, oldKoulutus: Option[Koulutus]): IsValid = {
    val validationContext =
      ValidationContext(koulutus.tila, koulutus.kielivalinta, if (oldKoulutus.isDefined) update else create)
    val commonErrors = validateCommonParameters(koulutus, oldKoulutus)
    val koulutusLevelErrors =
      validateKoulutustyyppiSpecificParameters(koulutus, oldKoulutus, validationContext)
    val metadataErrors = validateMetadata(koulutus, oldKoulutus, validationContext)

    Seq(commonErrors, koulutusLevelErrors, metadataErrors).flatten.distinct
  }

  private def validateMetadata(
      koulutus: Koulutus,
      oldKoulutus: Option[Koulutus],
      validationContext: ValidationContext
  ): IsValid = {
    val koulutusDiffResolver = KoulutusDiffResolver(koulutus, oldKoulutus)
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

    val isAvoinKorkeakoulutus = koulutus.isAvoinKorkeakoulutus()

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
      validateTarjoajat(
        tyyppi,
        koulutus.tarjoajat,
        oldKoulutus.map(_.tarjoajat).getOrElse(List()),
        if (isAvoinKorkeakoulutus) oppilaitostyypitForAvoinKorkeakoulutus else Seq()
      ),
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
      oldKoulutus: Option[Koulutus],
      validationContext: ValidationContext
  ): IsValid = {
    val koulutusDiffResolver = KoulutusDiffResolver(koulutus, oldKoulutus)
    koulutus.koulutustyyppi match {
      case x if isAmmatillinen(x) =>
        and(
          validateIfTrueOrElse(
            x == AmmMuu,
            assertNotDefined(koulutus.sorakuvausId, "sorakuvausId"),
            validateSorakuvaus(koulutus)
          ),
          ammatillinenKoulutusServiceValidation.validate(koulutus, oldKoulutus, validationContext)
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
      case VapaaSivistystyoMuu =>
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
        Erikoislaakari,
        Erikoistumiskoulutus
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
          assertOpinnonTyyppiKoodiUri(koulutusDiffResolver.newOpinnonTyyppiKoodiUri(), validationContext),
          validateIsAvoinKorkeakoulutusIntegrity(koulutus, koulutusDiffResolver),
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
          assertOpinnonTyyppiKoodiUri(koulutusDiffResolver.newOpinnonTyyppiKoodiUri(), validationContext),
          validateIsAvoinKorkeakoulutusIntegrity(koulutus, koulutusDiffResolver),
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
      case m: ErikoistumiskoulutusMetadata =>
        and(
          validateIfDefined[String](
            koulutusDiffResolver.newErikoistumiskoulutusKoodiUri(),
            uri =>
              assertKoodistoQueryResult(
                uri,
                koulutusKoodiClient.erikoistumisKoulutusKoodiUriExists,
                "metadata.erikoistumiskoulutusKoodiUri",
                validationContext,
                invalidErikoistumiskoulutusKoodiuri(uri)
              )
          ),
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
          validateIfJulkaistu(
            validationContext.tila,
            assertNotOptional(m.erikoistumiskoulutusKoodiUri, "metadata.erikoistumiskoulutusKoodiUri")
          )
        )
      case _ => NoErrors
    }
  }

  private def validateIsAvoinKorkeakoulutusIntegrity(k: Koulutus, koulutusDiffResolver: KoulutusDiffResolver) =
    validateIfTrue(
      koulutusDiffResolver.isAvoinKkChanged(), {
        val toteutukset = toteutusDAO.getByKoulutusOid(k.oid.get, TilaFilter.onlyOlemassaolevat())
        assertTrue(
          toteutukset.isEmpty,
          "metadata.isAvoinKorkeakoulutus",
          cannotChangeIsAvoinKorkeakoulutus
        )
      }
    )

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
      assertOpintojenLaajuusyksikkoKoodiUri(newOpintojenLaajuusYksikkoKoodiUri, validationContext),
      validateIfJulkaistu(
        validationContext.tila,
        and(
          assertNotOptional(opintojenLaajuusYksikkoKoodiUri, "metadata.opintojenLaajuusYksikkoKoodiUri"),
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

  private def assertOpinnonTyyppiKoodiUri(koodiUri: Option[String], validationContext: ValidationContext): IsValid = {
    validateIfDefined[String](
      koodiUri,
      uri =>
        assertKoodistoQueryResult(
          uri,
          koulutusKoodiClient.opinnonTyyppiKoodiUriExists,
          "metadata.opinnonTyyppiKoodiUri",
          validationContext,
          invalidOpinnonTyyppiKoodiuri(uri)
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
