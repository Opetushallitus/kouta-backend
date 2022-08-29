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
      case Amm | AmmOsaamisala =>
        and(
          validateSorakuvaus(koulutus),
          validateAmmatillinenKoulutus(koulutus)
        )
      case Yo =>
        and(
          validateSorakuvaus(koulutus),
          validateKoulutusKoodiUritOfKoulutustyypit(yoKoulutustyypit, koulutus, None),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      case Amk =>
        and(
          validateSorakuvaus(koulutus),
          validateKoulutusKoodiUritOfKoulutustyypit(amkKoulutustyypit, koulutus, None),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      case AmmOpeErityisopeJaOpo =>
        and(
          validateSorakuvaus(koulutus),
          validateKoulutusKoodiUrit(ammOpeErityisopeJaOpoKoulutusKoodiUrit, koulutus, Some(1)),
          assertNotDefined(koulutus.ePerusteId, "ePerusteId")
        )
      case Lk =>
        and(
          validateSorakuvaus(koulutus),
          validateKoulutusKoodiUrit(lukioKoulutusKoodiUrit, koulutus, Some(1)),
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
      case Erikoislaakari => and(
        validateKoulutusKoodiUrit(erikoislaakariKoulutusKoodiUrit, koulutus, Some(1)),
        assertNotDefined(koulutus.ePerusteId, "ePerusteId"),
      )
      case AmmMuu | Tuva | Telma | VapaaSivistystyoMuu | VapaaSivistystyoOpistovuosi =>
        and (
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
      metadata: KoulutusMetadata
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
          validateOpintojenLaajuusyksikkoAndNumero(tila, m.opintojenLaajuusyksikkoKoodiUri, m.opintojenLaajuusNumero, true)
        )
      case yoKoulutusMetadata: YliopistoKoulutusMetadata =>
        validateKorkeaKoulutusMetadata(tila, kielivalinta, yoKoulutusMetadata)
      case amkKoulutusMetadata: AmmattikorkeakouluKoulutusMetadata =>
        validateKorkeaKoulutusMetadata(tila, kielivalinta, amkKoulutusMetadata)
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
          assertOpintojenLaajuusKoodiUri(m.opintojenLaajuusKoodiUri)
        )
      case m: TuvaKoulutusMetadata =>
        validateTuvaTelma(tila, kielivalinta, m.lisatiedot, m.linkkiEPerusteisiin, m.opintojenLaajuusKoodiUri)
      case m: TelmaKoulutusMetadata =>
        validateTuvaTelma(tila, kielivalinta, m.lisatiedot, m.linkkiEPerusteisiin, m.opintojenLaajuusKoodiUri)
      case m: VapaaSivistystyoOpistovuosiKoulutusMetadata =>
        validateVapaaSivistystyoOpistovuosiKoulutus(tila, kielivalinta, m)
      case m: VapaaSivistystyoMuuKoulutusMetadata =>
        validateVapaaSivistystyoMuuKoulutus(tila, kielivalinta, m)
      case m: AikuistenPerusopetusKoulutusMetadata =>
        and(
          assertEmpty(m.lisatiedot, "metadata.lisatiedot"),
          validateIfNonEmpty(m.linkkiEPerusteisiin, "metadata.linkkiEPerusteisiin", assertValidUrl _),
          validateOpintojenLaajuusyksikkoAndNumero(tila, m.opintojenLaajuusyksikkoKoodiUri, m.opintojenLaajuusNumero, true),
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
          validateOpintojenLaajuusyksikkoAndNumero(tila, m.opintojenLaajuusyksikkoKoodiUri, m.opintojenLaajuusNumero, false),
        )
      case m: KkOpintokokonaisuusKoulutusMetadata =>
        and(
          assertKoulutusalaKoodiUrit(m.koulutusalaKoodiUrit),
          validateOpintojenLaajuusyksikko(tila, m.opintojenLaajuusyksikkoKoodiUri, false),
          validateOpintojenLaajuusNumerot(tila, m.opintojenLaajuusNumeroMin, m.opintojenLaajuusNumeroMax, false),
        )
      case m: ErikoislaakariKoulutusMetadata =>
        and(
          // TODO: Validoi koulutusalaKoodiUrit (kovakoodattu)
          assertTutkintonimikeKoodiUrit(m.tutkintonimikeKoodiUrit)
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
      assertOpintojenLaajuusKoodiUri(metadata.opintojenLaajuusKoodiUri)
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
    val ePerusteLinkki  = metadata.linkkiEPerusteisiin
    and(
      assertKoulutusalaKoodiUrit(metadata.koulutusalaKoodiUrit),
      validateIfNonEmpty(ePerusteLinkki, "metadata.linkkiEPerusteisiin", assertValidUrl _),
      validateIfJulkaistu(
        tila,
        validateOptionalKielistetty(kielivalinta, ePerusteLinkki, "metadata.linkkiEPerusteisiin")
      )
    )
  }

  private def validateVapaaSivistystyoOpistovuosiKoulutus(tila: Julkaisutila,
                                                          kielivalinta: Seq[Kieli],
                                                          metadata: VapaaSivistystyoOpistovuosiKoulutusMetadata): IsValid = {
    val laajuusKoodiUri = metadata.opintojenLaajuusKoodiUri
    and(
        validateVapaaSivistystyoKoulutus(tila, kielivalinta, metadata),
        assertOpintojenLaajuusKoodiUri(laajuusKoodiUri),
        validateIfJulkaistu(
          tila,
          assertNotOptional(laajuusKoodiUri, "metadata.opintojenLaajuusKoodiUri")
        )
      )
  }

  private def validateVapaaSivistystyoMuuKoulutus(tila: Julkaisutila,
                                                  kielivalinta: Seq[Kieli],
                                                  metadata: VapaaSivistystyoMuuKoulutusMetadata): IsValid =
    and(
      validateVapaaSivistystyoKoulutus(tila, kielivalinta, metadata),
      validateOpintojenLaajuusyksikkoAndNumero(
        tila,
        metadata.opintojenLaajuusyksikkoKoodiUri,
        metadata.opintojenLaajuusNumero, true))

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

  private def assertOpintojenLaajuusyksikkoKoodiUri(koodiUri: Option[String]): IsValid =
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
    )

  private def validateOpintojenLaajuusyksikkoAndNumero(
      tila: Julkaisutila,
      koodiUri: Option[String],
      laajuusNumero: Option[Double],
      mandatoryIfJulkaistu: Boolean
  ): IsValid =
    and(
      assertOpintojenLaajuusyksikkoKoodiUri(koodiUri),
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
    tila: Julkaisutila,
    koodiUri: Option[String],
    mandatoryIfJulkaistu: Boolean
  ): IsValid =
    and(
      assertOpintojenLaajuusyksikkoKoodiUri(koodiUri),
      validateIfTrue(
        mandatoryIfJulkaistu,
        validateIfJulkaistu(
          tila,
          and(
            assertNotOptional(koodiUri, "metadata.opintojenLaajuusyksikkoKoodiUri"),
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
