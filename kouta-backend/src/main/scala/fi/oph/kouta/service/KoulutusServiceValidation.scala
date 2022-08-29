package fi.oph.kouta.service

import fi.oph.kouta.client.KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList
import fi.oph.kouta.client.{EPerusteKoodiClient, KoulutusKoodiClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.repository.{SorakuvausDAO, ToteutusDAO}
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
    val validationContext    = new ValidationContext()
    val koulutusDiffResolver = KoulutusDiffResolver(koulutus, oldKoulutus)
    val commonErrors         = validateCommonParameters(koulutus, oldKoulutus)
    val koulutusLevelErrors  = validateKoulutustyyppiSpecificParameters(koulutus, oldKoulutus, validationContext, koulutusDiffResolver)

    val metadataErrors = koulutus.metadata match {
      case Some(metadata) =>
        and(
          validateCommonMetadataParameters(
            koulutus.tila,
            koulutus.koulutustyyppi,
            koulutus.kielivalinta,
            validationContext,
            metadata,
            koulutusDiffResolver
          ),
          validateMetadataSpecificParameters(
            koulutus.tila,
            koulutus.kielivalinta,
            validationContext,
            metadata,
            koulutusDiffResolver
          )
        )
      case _ => if (koulutus.tila == Julkaistu) error("metadata", missingMsg) else NoErrors
    }

    Seq(commonErrors, koulutusLevelErrors, metadataErrors).flatten.distinct
  }

  private def validateCommonParameters(koulutus: Koulutus, oldKoulutus: Option[Koulutus]): IsValid = {
    val tila   = koulutus.tila
    val tyyppi = koulutus.koulutustyyppi
    and(
      koulutus.validate(),
      validateIfTrueOrElse(
        oldKoulutus.isDefined,
        assertNotOptional(koulutus.oid, "oid"),
        assertNotDefined(koulutus.oid, "oid")
      ),
      validateTarjoajat(koulutus.tarjoajat, oldKoulutus.map(_.tarjoajat).getOrElse(List())),
      validateIfNonEmpty[String](
        koulutus.koulutuksetKoodiUri,
        "koulutuksetKoodiUri",
        assertMatch(_, KoulutusKoodiPattern, _)
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
            koulutus.tila,
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
            koulutus.tila,
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
            koulutus.tila,
            koulutus.koulutuksetKoodiUri,
            koulutusDiffResolver.newKoulutusKoodiUrit(),
            Some(1),
            validationContext
          ),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      case Lk =>
        and(
          validateSorakuvaus(koulutus),
          validateKoulutusKoodiUrit(
            lukioKoulutusKoodiUrit,
            koulutus.tila,
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
            "koulutuksetKoodiUri",
            koodiUriTipText("koulutus_201101")
          ),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      case Erikoislaakari =>
        and(
          validateKoulutusKoodiUrit(
            erikoislaakariKoulutusKoodiUrit,
            koulutus.tila,
            koulutus.koulutuksetKoodiUri,
            koulutusDiffResolver.newKoulutusKoodiUrit(),
            Some(1),
            validationContext
          ),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      case AmmMuu | Tuva | Telma | VapaaSivistystyoMuu | VapaaSivistystyoOpistovuosi =>
        and(
          assertNotDefined(koulutus.sorakuvausId, "sorakuvausId"),
          assertEmpty(koulutus.koulutuksetKoodiUri, "koulutuksetKoodiUri"),
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
      tila: Julkaisutila,
      tyyppi: Koulutustyyppi,
      kielivalinta: Seq[Kieli],
      validationContext: ValidationContext,
      metadata: KoulutusMetadata,
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
        Erikoislaakari
      )
    val koulutustyypitWithoutLisatiedot: Set[Koulutustyyppi] =
      Set(AmmMuu, Tuva, Telma, VapaaSivistystyoOpistovuosi, VapaaSivistystyoMuu, AikuistenPerusopetus)

    and(
      assertTrue(metadata.tyyppi == tyyppi, s"metadata.tyyppi", InvalidMetadataTyyppi),
      validateIfTrueOrElse(
        koulutustyypitWithoutLisatiedot.contains(tyyppi),
        assertEmpty(metadata.lisatiedot, "metadata.lisatiedot"),
        validateIfNonEmpty[Lisatieto](
          koulutusDiffResolver.newLisatiedot(),
          "metadata.lisatiedot",
          (lisatieto, path) =>
            validateIfSuccessful(
              lisatieto.validate(tila, kielivalinta, path),
              assertKoodistoQueryResult(
                lisatieto.otsikkoKoodiUri,
                koulutusKoodiClient.lisatiedotOtsikkoKoodiUriExists,
                path = s"$path.otsikkoKoodiUri",
                validationContext,
                invalidLisatietoOtsikkoKoodiuri(lisatieto.otsikkoKoodiUri)
              )
            )
        )
      ),
      validateIfJulkaistu(
        tila,
        validateIfTrueOrElse(
          koulutustyypitWithMandatoryKuvaus.contains(tyyppi),
          validateKielistetty(kielivalinta, metadata.kuvaus, "metadata.kuvaus"),
          validateOptionalKielistetty(kielivalinta, metadata.kuvaus, "metadata.kuvaus")
        )
      )
    )
  }

  private def validateMetadataSpecificParameters(
      tila: Julkaisutila,
      kielivalinta: Seq[Kieli],
      validationContext: ValidationContext,
      metadata: KoulutusMetadata,
      koulutusDiffResolver: KoulutusDiffResolver
  ): IsValid = {
    metadata match {
      case ammTutkinnonOsaMetadata: AmmatillinenTutkinnonOsaKoulutusMetadata =>
        validateAmmTutkinnonosaMetadata(
          tila,
          kielivalinta,
          ammTutkinnonOsaMetadata.tutkinnonOsat,
          koulutusDiffResolver.newTutkinnonosat()
        )

      case ammOsaamisalaKoulutusMetadata: AmmatillinenOsaamisalaKoulutusMetadata =>
        validateAmmOsaamisalaKoulutusMetadata(
          tila,
          koulutusDiffResolver.newEPerusteId(),
          ammOsaamisalaKoulutusMetadata
        )

      case m: AmmatillinenMuuKoulutusMetadata =>
        and(
          assertKoulutusalaKoodiUrit(koulutusDiffResolver.newKoulutusalaKoodiUrit(), validationContext),
          validateOpintojenLaajuusyksikko(
            tila,
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
        and(
          assertEmpty(m.tutkintonimikeKoodiUrit, "metadata.tutkintonimikeKoodiUrit"),
          assertCertainValue(
            m.opintojenLaajuusKoodiUri,
            "opintojenlaajuus_60",
            "metadata.opintojenLaajuusKoodiUri",
            koodiUriTipText("opintojenlaajuus_60")
          ),
          assertOneAndOnlyCertainValueInSeq(
            m.koulutusalaKoodiUrit,
            "kansallinenkoulutusluokitus2016koulutusalataso1_01",
            "metadata.koulutusalaKoodiUrit",
            koodiUriTipText("kansallinenkoulutusluokitus2016koulutusalataso1_01")
          )
        )

      case m: LukioKoulutusMetadata =>
        and(
          assertOneAndOnlyCertainValueInSeq(
            m.koulutusalaKoodiUrit,
            "kansallinenkoulutusluokitus2016koulutusalataso1_00",
            "metadata.koulutusalaKoodiUrit",
            koodiUriTipText("kansallinenkoulutusluokitus2016koulutusalataso1_00")
          ),
          assertOpintojenLaajuusKoodiUri(koulutusDiffResolver.newOpintojenLaajuusKoodiUri(), validationContext)
        )
      case m: TuvaKoulutusMetadata =>
        validateTuvaTelma(
          tila,
          kielivalinta,
          validationContext,
          m.lisatiedot,
          m.linkkiEPerusteisiin,
          m.opintojenLaajuusKoodiUri,
          koulutusDiffResolver.newOpintojenLaajuusKoodiUri()
        )
      case m: TelmaKoulutusMetadata =>
        validateTuvaTelma(
          tila,
          kielivalinta,
          validationContext,
          m.lisatiedot,
          m.linkkiEPerusteisiin,
          m.opintojenLaajuusKoodiUri,
          koulutusDiffResolver.newOpintojenLaajuusKoodiUri()
        )
      case m: VapaaSivistystyoOpistovuosiKoulutusMetadata =>
        validateVapaaSivistystyoOpistovuosiKoulutus(tila, kielivalinta, validationContext, koulutusDiffResolver, m)
      case m: VapaaSivistystyoMuuKoulutusMetadata =>
        validateVapaaSivistystyoMuuKoulutus(tila, kielivalinta, validationContext, koulutusDiffResolver, m)
      case m: AikuistenPerusopetusKoulutusMetadata =>
        and(
          assertEmpty(m.lisatiedot, "metadata.lisatiedot"),
          validateIfNonEmpty(m.linkkiEPerusteisiin, "metadata.linkkiEPerusteisiin", assertValidUrl _),
          validateOpintojenLaajuusyksikko(
            tila,
            m.opintojenLaajuusyksikkoKoodiUri,
            koulutusDiffResolver.newOpintojenLaajuusyksikkoKoodiUri(),
            m.opintojenLaajuusNumero,
            true,
            validationContext
          ),
          validateIfJulkaistu(
            tila,
            and(
              validateOptionalKielistetty(kielivalinta, m.linkkiEPerusteisiin, "metadata.linkkiEPerusteisiin")
            )
          )
        )
      case m: KkOpintojaksoKoulutusMetadata =>
        and(
          assertKoulutusalaKoodiUrit(koulutusDiffResolver.newKoulutusalaKoodiUrit(), validationContext),
          validateOpintojenLaajuusyksikko(
            tila,
            m.opintojenLaajuusyksikkoKoodiUri,
            koulutusDiffResolver.newOpintojenLaajuusyksikkoKoodiUri(),
            m.opintojenLaajuusNumero,
            false,
            validationContext
          )
        )
      case m: ErikoislaakariKoulutusMetadata =>
        and(
          assertOneAndOnlyCertainValueInSeq(
            m.koulutusalaKoodiUrit,
            "kansallinenkoulutusluokitus2016koulutusalataso2_091",
            "metadata.koulutusalaKoodiUrit",
            koodiUriTipText("kansallinenkoulutusluokitus2016koulutusalataso2_091")
          ),
          assertTutkintonimikeKoodiUrit(koulutusDiffResolver.newTutkintonimikeKoodiUrit(), validationContext)
        )
      case _ => NoErrors
    }
  }

  private def validateAmmatillinenKoulutus(
      koulutus: Koulutus,
      koulutusDiffResolver: KoulutusDiffResolver,
      validationContext: ValidationContext
  ): IsValid =
    and(
      validateKoulutusKoodiUritOfKoulutustyypit(
        ammatillisetKoulutustyypit,
        koulutus.tila,
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
                val ePerusteId               = osa.ePerusteId
                var ePerusteValidationStatus = NoErrors
                validateIfSuccessful(
                  {
                    ePerusteValidationStatus = validateEPeruste(
                      ePerusteId,
                      s"$path.ePerusteId",
                      Seq(osa.koulutusKoodiUri.getOrElse("")).filter(_.nonEmpty)
                    )
                    ePerusteValidationStatus
                  },
                  validateIfTrue(
                    ePerusteId.isDefined && (osa.tutkinnonosaId.isDefined || osa.tutkinnonosaViite.isDefined), {
                      val (tutkinnonosaViitteetAndIdt, ePerusteServiceOk) = {
                        if (Validations.ePerusteServiceOk(ePerusteValidationStatus))
                          ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(ePerusteId.get)
                        else (Seq(), false)
                      }
                      validateIfTrueOrElse(
                        ePerusteServiceOk, {
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
                        },
                        error(s"$path.ePerusteId", ePerusteServiceFailureMsg)
                      )
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
          validateIfSuccessful(
            assertMatch(koodiUri, OsaamisalaKoodiPattern, "metadata.osaamisalaKoodiUri"),
            validateIfDefined[Long](
              ePerusteId,
              ePerusteId =>
                validateIfTrue(
                  ePerusteId > 0, {
                    val (osaamisalaKoodiuritForEPeruste, ePerusteServiceOk) = ePerusteKoodiClient
                      .getOsaamisalaKoodiuritForEPeruste(ePerusteId)
                    validateIfTrueOrElse(
                      ePerusteServiceOk,
                      assertTrue(
                        koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, osaamisalaKoodiuritForEPeruste),
                        "metadata.osaamisalaKoodiUri",
                        invalidOsaamisalaForEPeruste(ePerusteId, koodiUri)
                      ),
                      error("ePerusteId", ePerusteServiceFailureMsg)
                    )
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
      tila: Julkaisutila,
      kielivalinta: Seq[Kieli],
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
        tila,
        and(
          assertNotOptional(opintojenLaajuusKoodiUri, "metadata.opintojenLaajuusKoodiUri"),
          validateOptionalKielistetty(kielivalinta, linkkiEPerusteisiin, "metadata.linkkiEPerusteisiin")
        )
      )
    )
  }

  private def validateVapaaSivistystyoKoulutus(
      tila: Julkaisutila,
      kielivalinta: Seq[Kieli],
      validationContext: ValidationContext,
      koulutusDiffResolver: KoulutusDiffResolver,
      metadata: VapaaSivistystyoKoulutusMetadata
  ): IsValid = {
    val ePerusteLinkki = metadata.linkkiEPerusteisiin
    and(
      assertKoulutusalaKoodiUrit(koulutusDiffResolver.newKoulutusalaKoodiUrit(), validationContext),
      validateIfNonEmpty(ePerusteLinkki, "metadata.linkkiEPerusteisiin", assertValidUrl _),
      validateIfJulkaistu(
        tila,
        validateOptionalKielistetty(kielivalinta, ePerusteLinkki, "metadata.linkkiEPerusteisiin")
      )
    )
  }

  private def validateVapaaSivistystyoOpistovuosiKoulutus(
      tila: Julkaisutila,
      kielivalinta: Seq[Kieli],
      validationContext: ValidationContext,
      koulutusDiffResolver: KoulutusDiffResolver,
      metadata: VapaaSivistystyoOpistovuosiKoulutusMetadata
  ): IsValid = {
    val laajuusKoodiUri = metadata.opintojenLaajuusKoodiUri
    and(
      validateVapaaSivistystyoKoulutus(tila, kielivalinta, validationContext, koulutusDiffResolver, metadata),
      assertOpintojenLaajuusKoodiUri(koulutusDiffResolver.newOpintojenLaajuusKoodiUri(), validationContext),
      validateIfJulkaistu(
        tila,
        assertNotOptional(laajuusKoodiUri, "metadata.opintojenLaajuusKoodiUri")
      )
    )
  }

  private def validateVapaaSivistystyoMuuKoulutus(
      tila: Julkaisutila,
      kielivalinta: Seq[Kieli],
      validationContext: ValidationContext,
      koulutusDiffResolver: KoulutusDiffResolver,
      metadata: VapaaSivistystyoMuuKoulutusMetadata
  ): IsValid =
    and(
      validateVapaaSivistystyoKoulutus(tila, kielivalinta, validationContext, koulutusDiffResolver, metadata),
      validateOpintojenLaajuusyksikko(
        tila,
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
      tila: Julkaisutila,
      koulutusKoodiUrit: Seq[String],
      newKoulutusKoodiUrit: Seq[String],
      maxNbrOfKoodit: Option[Int],
      validationContext: ValidationContext
  ): IsValid =
    validateKoulutusKooditIfJulkaistu(
      tila,
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
      tila: Julkaisutila,
      koulutusKoodiUrit: Seq[String],
      newKoulutusKoodiUrit: Seq[String],
      maxNbrOfKoodit: Option[Int],
      validationContext: ValidationContext
  ): IsValid = validateKoulutusKooditIfJulkaistu(
    tila,
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
            assertNotNegative(ePerusteId, path), {
              val (koodiUritForEperuste, ePerusteServiceOk) =
                ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(ePerusteId)
              validateIfTrueOrElse(
                ePerusteServiceOk,
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
                ),
                error(path, ePerusteServiceFailureMsg)
              )
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
        validateIfSuccessful(
          assertMatch(koodiUri, KoulutusalaKoodiPattern, path),
          assertKoodistoQueryResult(
            koodiUri,
            koulutusKoodiClient.koulutusalaKoodiUriExists,
            path,
            validationContext,
            invalidKoulutusAlaKoodiuri(koodiUri)
          )
        )
    )
  }

  private def assertTutkintonimikeKoodiUrit(koodiUrit: Seq[String], validationContext: ValidationContext): IsValid = {
    validateIfNonEmpty[String](
      koodiUrit,
      "metadata.tutkintonimikeKoodiUrit",
      (koodiUri, path) =>
        validateIfSuccessful(
          assertMatch(koodiUri, TutkintonimikeKoodiPattern, path),
          assertKoodistoQueryResult(
            koodiUri,
            koulutusKoodiClient.tutkintoNimikeKoodiUriExists,
            path,
            validationContext,
            invalidTutkintoNimikeKoodiuri(koodiUri)
          )
        )
    )
  }

  private def assertOpintojenLaajuusKoodiUri(koodiUri: Option[String], validationContext: ValidationContext): IsValid =
    validateIfDefined[String](
      koodiUri,
      uri =>
        validateIfSuccessful(
          assertMatch(uri, OpintojenLaajuusKoodiPattern, "metadata.opintojenLaajuusKoodiUri"),
          assertKoodistoQueryResult(
            uri,
            koulutusKoodiClient.opintojenLaajuusKoodiUriExists,
            "metadata.opintojenLaajuusKoodiUri",
            validationContext,
            invalidOpintojenLaajuusKoodiuri(uri)
          )
        )
    )

  private def validateOpintojenLaajuusyksikko(
      tila: Julkaisutila,
      koodiUri: Option[String],
      newKoodiUri: Option[String],
      laajuusNumero: Option[Double],
      mandatoryIfJulkaistu: Boolean,
      validationContext: ValidationContext
  ): IsValid =
    and(
      validateIfDefined[String](
        newKoodiUri,
        uri =>
          validateIfSuccessful(
            assertMatch(uri, OpintojenLaajuusyksikkoKoodiPattern, "metadata.opintojenLaajuusyksikkoKoodiUri"),
            assertKoodistoQueryResult(
              uri,
              koulutusKoodiClient.opintojenLaajuusyksikkoKoodiUriExists,
              "metadata.opintojenLaajuusyksikkoKoodiUri",
              validationContext,
              invalidOpintojenLaajuusyksikkoKoodiuri(uri)
            )
          )
      ),
      validateIfDefined[Double](
        laajuusNumero,
        assertNotNegative(_, "metadata.opintojenLaajuusNumero")
      ),
      validateIfTrue(
        mandatoryIfJulkaistu,
        validateIfJulkaistu(
          tila,
          and(
            assertNotOptional(koodiUri, "metadata.opintojenLaajuusyksikkoKoodiUri"),
            assertNotOptional(laajuusNumero, "metadata.opintojenLaajuusNumero")
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
