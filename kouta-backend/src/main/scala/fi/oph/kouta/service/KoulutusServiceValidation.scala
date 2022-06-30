package fi.oph.kouta.service

import fi.oph.kouta.client.KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList
import fi.oph.kouta.client.{EPerusteKoodiClient, KoulutusKoodiClient}
import fi.oph.kouta.domain.{koulutus, _}
import fi.oph.kouta.repository.{SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.validation.Validations.{validateIfJulkaistu, _}
import fi.oph.kouta.validation.{
  IsValid,
  NoErrors,
  amkKoulutustyypit,
  ammOpeErityisopeJaOpoKoulutusKoodiUrit,
  ammatillisetKoulutustyypit,
  lukioKoulutusKoodiUrit,
  yoKoulutustyypit
}

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

  override def validateEntity(koulutus: Koulutus): IsValid = {
    val commonErrors        = validateCommonParameters(koulutus)
    val koulutusLevelErrors = validateKoulutustyyppiSpecificParameters(koulutus)

    val metadataErrors = koulutus.metadata match {
      case Some(metadata) =>
        and(
          validateCommonMetadataParameters(koulutus.tila, koulutus.koulutustyyppi, koulutus.kielivalinta, metadata),
          validateMetadataSpecificParameters(koulutus.tila, koulutus.kielivalinta, metadata, koulutus.ePerusteId)
        )
      case _ => if (koulutus.tila == Julkaistu) error("metadata", missingMsg) else NoErrors
    }

    Seq(commonErrors, koulutusLevelErrors, metadataErrors).flatten
  }

  private def validateCommonParameters(koulutus: Koulutus): IsValid = {
    val tila   = koulutus.tila
    val tyyppi = koulutus.koulutustyyppi
    and(
      koulutus.validate(),
      validateTarjoajat(koulutus.tarjoajat),
      validateIfNonEmpty[String](
        koulutus.koulutuksetKoodiUri,
        "koulutuksetKoodiUri",
        assertMatch(_, KoulutusKoodiPattern, _)
      ),
      validateSorakuvausIntegrity(
        koulutus.sorakuvausId,
        tila,
        tyyppi,
        entityKoulutusKoodiUrit = koulutus.koulutuksetKoodiUri
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

  private def validateKoulutustyyppiSpecificParameters(koulutus: Koulutus): IsValid = {
    koulutus.koulutustyyppi match {
      case Amm | AmmOsaamisala => validateAmmatillinenKoulutus(koulutus)
      case Yo =>
        and(
          validateKoulutusKoodiUritOfKoulutustyypit(yoKoulutustyypit, koulutus, None),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      case Amk =>
        and(
          validateKoulutusKoodiUritOfKoulutustyypit(amkKoulutustyypit, koulutus, None),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      case AmmOpeErityisopeJaOpo =>
        and(
          validateKoulutusKoodiUrit(ammOpeErityisopeJaOpoKoulutusKoodiUrit, koulutus, Some(1)),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      case Lk =>
        and(
          validateKoulutusKoodiUrit(lukioKoulutusKoodiUrit, koulutus, Some(1)),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      case AikuistenPerusopetus =>
        assertNotDefined(koulutus.ePerusteId, "ePerusteId")
      case _ =>
        and(
          assertEmpty(koulutus.koulutuksetKoodiUri, "koulutuksetKoodiUri"),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
    }
  }

  private def validateCommonMetadataParameters(
      tila: Julkaisutila,
      tyyppi: Koulutustyyppi,
      kielivalinta: Seq[Kieli],
      metadata: KoulutusMetadata
  ): IsValid = {
    val koulutustyypitWithMandatoryKuvaus: Set[Koulutustyyppi] =
      Set(AmmMuu, Tuva, Telma, VapaaSivistystyoOpistovuosi, VapaaSivistystyoMuu, AikuistenPerusopetus, KkOpintojakso)

    and(
      assertTrue(metadata.tyyppi == tyyppi, s"metadata.tyyppi", InvalidMetadataTyyppi),
      validateIfNonEmpty[Lisatieto](
        metadata.lisatiedot,
        "metadata.lisatiedot",
        (lisatieto, path) =>
          validateIfSuccessful(
            lisatieto.validate(tila, kielivalinta, path),
            assertTrue(
              koulutusKoodiClient.lisatiedotOtsikkoKoodiUriExists(lisatieto.otsikkoKoodiUri),
              path = s"$path.otsikkoKoodiUri",
              invalidLisatietoOtsikkoKoodiuri(lisatieto.otsikkoKoodiUri)
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
      metadata: KoulutusMetadata,
      ePerusteId: Option[Long]
  ): IsValid = {
    metadata match {
      case ammTutkinnonOsaMetadata: AmmatillinenTutkinnonOsaKoulutusMetadata =>
        validateAmmTutkinnonosaMetadata(tila, kielivalinta, ammTutkinnonOsaMetadata)

      case ammOsaamisalaKoulutusMetadata: AmmatillinenOsaamisalaKoulutusMetadata =>
        validateAmmOsaamisalaKoulutusMetadata(tila, ePerusteId, ammOsaamisalaKoulutusMetadata)

      case m: AmmatillinenMuuKoulutusMetadata =>
        and(
          assertKoulutusalaKoodiUrit(m.koulutusalaKoodiUrit),
          validateOpintojenLaajuusyksikko(tila, m.opintojenLaajuusyksikkoKoodiUri, m.opintojenLaajuusNumero, true)
        )
      case yoKoulutusMetadata: YliopistoKoulutusMetadata =>
        validateKorkeaKoulutusMetadata(tila, kielivalinta, yoKoulutusMetadata)
      case amkKoulutusMetadata: AmmattikorkeakouluKoulutusMetadata =>
        validateKorkeaKoulutusMetadata(tila, kielivalinta, amkKoulutusMetadata)
      case m: AmmOpeErityisopeJaOpoKoulutusMetadata =>
        and(
          assertEmpty(m.tutkintonimikeKoodiUrit, "metadata.tutkintonimikeKoodiUrit"),
          assertOpintojenLaajuusKoodiUri(m.opintojenLaajuusKoodiUri),
          validateIfJulkaistu(
            tila,
            validateKielistetty(kielivalinta, m.kuvauksenNimi, "metadata.kuvauksenNimi")
          )
        )

      case lukioKoulutusMetadata: LukioKoulutusMetadata =>
        assertOpintojenLaajuusKoodiUri(lukioKoulutusMetadata.opintojenLaajuusKoodiUri)
      case m: TuvaKoulutusMetadata =>
        validateTuvaTelma(tila, kielivalinta, m.lisatiedot, m.linkkiEPerusteisiin, m.opintojenLaajuusKoodiUri)
      case m: TelmaKoulutusMetadata =>
        validateTuvaTelma(tila, kielivalinta, m.lisatiedot, m.linkkiEPerusteisiin, m.opintojenLaajuusKoodiUri)
      case vapaaSivistystyoKoulutusMetadata: VapaaSivistystyoKoulutusMetadata =>
        validateVapaaSivistystyoKoulutus(tila, kielivalinta, vapaaSivistystyoKoulutusMetadata)
      case m: AikuistenPerusopetusKoulutusMetadata =>
        and(
          assertEmpty(m.lisatiedot, "metadata.lisatiedot"),
          validateIfNonEmpty(m.linkkiEPerusteisiin, "metadata.linkkiEPerusteisiin", assertValidUrl _),
          validateOpintojenLaajuusyksikko(tila, m.opintojenLaajuusyksikkoKoodiUri, m.opintojenLaajuusNumero, true),
          validateIfJulkaistu(
            tila,
            and(
              validateOptionalKielistetty(kielivalinta, m.linkkiEPerusteisiin, "metadata.linkkiEPerusteisiin")
            )
          )
        )
      case m: KkOpintojaksoKoulutusMetadata =>
        and(
          assertKoulutusalaKoodiUrit(m.koulutusalaKoodiUrit),
          validateOpintojenLaajuusyksikko(tila, m.opintojenLaajuusyksikkoKoodiUri, m.opintojenLaajuusNumero, false),
          validateIfJulkaistu(tila, validateKielistetty(kielivalinta, m.kuvauksenNimi, "metadata.kuvauksenNimi"))
        )
      case _ => NoErrors
    }
  }

  private def validateAmmatillinenKoulutus(koulutus: Koulutus): IsValid =
    and(
      validateKoulutusKoodiUritOfKoulutustyypit(ammatillisetKoulutustyypit, koulutus, Some(1)),
      validateIfJulkaistu(koulutus.tila, assertNotOptional(koulutus.ePerusteId, "ePerusteId")),
      validateEPeruste(koulutus.ePerusteId, "ePerusteId", koulutus.koulutuksetKoodiUri)
    )

  private def validateAmmTutkinnonosaMetadata(
      tila: Julkaisutila,
      kielivalinta: Seq[Kieli],
      ammTutkinnonOsaMetadata: AmmatillinenTutkinnonOsaKoulutusMetadata
  ): IsValid = {
    val path = "metadata.tutkinnonOsat"
    val osat = ammTutkinnonOsaMetadata.tutkinnonOsat
    and(
      validateIfJulkaistu(tila, assertNotEmpty(osat, path)),
      validateIfNonEmpty[TutkinnonOsa](
        osat,
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
                      val tutkinnonosaViitteetAndIdt =
                        ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(ePerusteId.get)
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
                  ePerusteId > 0,
                  assertTrue(
                    koodiUriWithEqualOrHigherVersioNbrInList(
                      koodiUri,
                      ePerusteKoodiClient
                        .getOsaamisalaKoodiuritForEPeruste(ePerusteId)
                    ),
                    "metadata.osaamisalaKoodiUri",
                    invalidOsaamisalaForEPeruste(ePerusteId, koodiUri)
                  )
                )
            )
          )
      )
    )

  private def validateKorkeaKoulutusMetadata(
      tila: Julkaisutila,
      kielivalinta: Seq[Kieli],
      metadata: KorkeakoulutusKoulutusMetadata
  ): IsValid =
    and(
      assertKoulutusalaKoodiUrit(metadata.koulutusalaKoodiUrit),
      assertTutkintonimikeKoodiUrit(metadata.tutkintonimikeKoodiUrit),
      assertOpintojenLaajuusKoodiUri(metadata.opintojenLaajuusKoodiUri),
      validateIfJulkaistu(tila, validateKielistetty(kielivalinta, metadata.kuvauksenNimi, "metadata.kuvauksenNimi"))
    )

  private def validateTuvaTelma(
      tila: Julkaisutila,
      kielivalinta: Seq[Kieli],
      lisatiedot: Seq[Lisatieto],
      linkkiEPerusteisiin: Kielistetty,
      opintojenLaajuusKoodiUri: Option[String]
  ): IsValid = {
    and(
      assertEmpty(lisatiedot, "metadata.lisatiedot"),
      validateIfNonEmpty(linkkiEPerusteisiin, "metadata.linkkiEPerusteisiin", assertValidUrl _),
      assertOpintojenLaajuusKoodiUri(opintojenLaajuusKoodiUri),
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
      metadata: VapaaSivistystyoKoulutusMetadata
  ): IsValid = {
    val laajuusKoodiUri = metadata.opintojenLaajuusKoodiUri
    val ePerusteLinkki  = metadata.linkkiEPerusteisiin
    and(
      assertKoulutusalaKoodiUrit(metadata.koulutusalaKoodiUrit),
      assertOpintojenLaajuusKoodiUri(laajuusKoodiUri),
      validateIfNonEmpty(ePerusteLinkki, "metadata.linkkiEPerusteisiin", assertValidUrl _),
      validateIfJulkaistu(
        tila,
        and(
          assertNotOptional(laajuusKoodiUri, "metadata.opintojenLaajuusKoodiUri"),
          validateOptionalKielistetty(kielivalinta, ePerusteLinkki, "metadata.linkkiEPerusteisiin")
        )
      )
    )
  }

  // Oletus: koodiUriFilter:in URIt eivät sisällä versiotietoa; tarkistetun koodiUrin versiota ei verrata koodiUriFilterissä
  // mahdollisesti annettuihin versioihin.
  private def validateKoulutusKoodiUrit(
      koodiUriFilter: Seq[String],
      koulutus: Koulutus,
      maxNbrOfKoodit: Option[Int]
  ): IsValid =
    validateKoulutusKooditIfJulkaistu(
      koulutus,
      maxNbrOfKoodit,
      validateIfNonEmpty[String](
        koulutus.koulutuksetKoodiUri,
        "koulutuksetKoodiUri",
        (koodiUri, path) =>
          assertTrue(
            koulutusKoodiClient.koulutusKoodiUriExists(koodiUriFilter, koodiUri),
            path,
            invalidKoulutuskoodiuri(koodiUri)
          )
      )
    )

  private def validateKoulutusKoodiUritOfKoulutustyypit(
      koulutusTyypit: Seq[String],
      koulutus: Koulutus,
      maxNbrOfKoodit: Option[Int]
  ): IsValid = validateKoulutusKooditIfJulkaistu(
    koulutus,
    maxNbrOfKoodit,
    validateIfNonEmpty[String](
      koulutus.koulutuksetKoodiUri,
      "koulutuksetKoodiUri",
      (koodiUri, path) =>
        assertTrue(
          koulutusKoodiClient.koulutusKoodiUriOfKoulutustyypitExist(koulutusTyypit, koodiUri),
          path,
          invalidKoulutuskoodiuri(koodiUri)
        )
    )
  )

  private def validateKoulutusKooditIfJulkaistu(
      koulutus: Koulutus,
      maxNbrOfKoodit: Option[Int],
      checkFromKoodisto: => IsValid
  ): IsValid = {
    val koodiUrit = koulutus.koulutuksetKoodiUri
    validateIfJulkaistu(
      koulutus.tila,
      validateIfSuccessful(
        and(
          assertNotEmpty(koodiUrit, "koulutuksetKoodiUri"),
          validateIfDefined[Int](
            maxNbrOfKoodit,
            nbr => assertTrue(koodiUrit.size <= nbr, "koulutuksetKoodiUri", tooManyKoodiUris)
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
              val koodiUritForEperuste = ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(ePerusteId)
              // ePeruste oletetaan tuntemattomaksi, ellei sille löydy yhtään koulutusKoodiUria
              and(
                assertTrue(koodiUritForEperuste.nonEmpty, path, invalidEPerusteId(ePerusteId)),
                validateIfTrue(
                  koulutusKoodiUrit.nonEmpty && koodiUritForEperuste.nonEmpty,
                  assertTrue(
                    koulutusKoodiUrit.forall(koodiUri =>
                      koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, koodiUritForEperuste, false)
                    ),
                    path,
                    invalidEPerusteIdForKoulutusKoodiUri(ePerusteId, koulutusKoodiUrit.head)
                  )
                )
              )
            }
          )
      )
    )
  }

  private def assertKoulutusalaKoodiUrit(koodiUrit: Seq[String]): IsValid = {
    validateIfNonEmpty[String](
      koodiUrit,
      "metadata.koulutusalaKoodiUrit",
      (koodiUri, path) =>
        validateIfSuccessful(
          assertMatch(koodiUri, KoulutusalaKoodiPattern, path),
          assertTrue(
            koulutusKoodiClient.koulutusalaKoodiUriExists(koodiUri),
            path,
            invalidKoulutusAlaKoodiuri(koodiUri)
          )
        )
    )
  }

  private def assertTutkintonimikeKoodiUrit(koodiUrit: Seq[String]): IsValid = {
    validateIfNonEmpty[String](
      koodiUrit,
      "metadata.tutkintonimikeKoodiUrit",
      (koodiUri, path) =>
        validateIfSuccessful(
          assertMatch(koodiUri, TutkintonimikeKoodiPattern, path),
          assertTrue(
            koulutusKoodiClient.tutkintoNimikeKoodiUriExists(koodiUri),
            path,
            invalidTutkintoNimikeKoodiuri(koodiUri)
          )
        )
    )
  }

  private def assertOpintojenLaajuusKoodiUri(koodiUri: Option[String]): IsValid =
    validateIfDefined[String](
      koodiUri,
      uri =>
        validateIfSuccessful(
          assertMatch(uri, OpintojenLaajuusKoodiPattern, "metadata.opintojenLaajuusKoodiUri"),
          assertTrue(
            koulutusKoodiClient.opintojenLaajuusKoodiUriExists(uri),
            "metadata.opintojenLaajuusKoodiUri",
            invalidOpintojenLaajuusKoodiuri(uri)
          )
        )
    )

  private def validateOpintojenLaajuusyksikko(
      tila: Julkaisutila,
      koodiUri: Option[String],
      laajuusNumero: Option[Double],
      mandatoryIfJulkaistu: Boolean
  ): IsValid =
    and(
      validateIfDefined[String](
        koodiUri,
        uri =>
          validateIfSuccessful(
            assertMatch(uri, OpintojenLaajuusyksikkoKoodiPattern, "metadata.opintojenLaajuusyksikkoKoodiUri"),
            assertTrue(
              koulutusKoodiClient.opintojenLaajuusyksikkoKoodiUriExists(uri),
              "metadata.opintojenLaajuusyksikkoKoodiUri",
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
