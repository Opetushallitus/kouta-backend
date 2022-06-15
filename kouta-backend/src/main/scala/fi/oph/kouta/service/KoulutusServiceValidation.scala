package fi.oph.kouta.service

import fi.oph.kouta.client.KoodistoUtils.koodiUriWithEqualOrHigherVersioNbrInList
import fi.oph.kouta.client.{EPerusteKoodiClient, KoulutusKoodiClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.repository.{SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, NoErrors, amkKoulutustyypit, ammOpeErityisopeJaOpoKoulutusKoodiUrit, ammatillisetKoulutustyypit, lukioKoulutusKoodiUrit, yoKoulutustyypit}

object KoulutusServiceValidation
    extends KoulutusServiceValidation(KoulutusKoodiClient, EPerusteKoodiClient, OrganisaatioServiceImpl, ToteutusDAO, SorakuvausDAO)

class KoulutusServiceValidation(
   val koulutusKoodiClient: KoulutusKoodiClient,
   ePerusteKoodiClient: EPerusteKoodiClient,
   val organisaatioService: OrganisaatioService,
   toteutusDAO: ToteutusDAO,
   val sorakuvausDAO: SorakuvausDAO
) extends KoulutusToteutusValidatingService[Koulutus] {

  override def validateParameterFormatAndExistence(koulutus: Koulutus): IsValid = koulutus.validate()

  override def validateDependenciesToExternalServices(koulutus: Koulutus): IsValid = {
    val commonErrors = and(
      validateTarjoajat(koulutus.tarjoajat),
      validateIfDefined[KoulutusMetadata](
        koulutus.metadata,
        m =>
          assertTrue(koulutusKoodiClient.lisatiedotOtsikkoKoodiUritExist(m.lisatiedot.map(_.otsikkoKoodiUri)),
            "metadata.lisatiedot.otsikkoKoodiUri",
            invalidLisatietoOtsikkoKoodiuri)
      ),
      validateSorakuvausIntegrity(koulutus.sorakuvausId, koulutus.tila, koulutus.koulutustyyppi,
        entityKoulutusKoodiUrit = koulutus.koulutuksetKoodiUri)
    )

    val koulutusLevelErrors: IsValid =
      koulutus.koulutustyyppi match {
        case Amm | AmmOsaamisala   => validateAmmatillinenKoulutus(koulutus)
        case Yo                    => assertKoulutusKoodiUritOfKoulutustyypit(yoKoulutustyypit, koulutus)
        case Amk                   => assertKoulutusKoodiUritOfKoulutustyypit(amkKoulutustyypit, koulutus)
        case AmmOpeErityisopeJaOpo => assertKoulutusKoodiUrit(ammOpeErityisopeJaOpoKoulutusKoodiUrit, koulutus)
        case Lk                    => assertKoulutusKoodiUrit(lukioKoulutusKoodiUrit, koulutus)
        case _                     => NoErrors
      }

    val metadataErrors = koulutus.metadata match {
      case Some(metadata) =>
        metadata match {
          case ammTutkinnonOsaMetadata: AmmatillinenTutkinnonOsaKoulutusMetadata =>
            validateAmmTutkinnonosaMetadata(ammTutkinnonOsaMetadata)

          // ePerusteId on pakollinen jos osaamisalaKoodiUri annettu
          case ammOsaamisalaKoulutusMetadata: AmmatillinenOsaamisalaKoulutusMetadata =>
            validateIfDefined[String](
              ammOsaamisalaKoulutusMetadata.osaamisalaKoodiUri,
              koodiUri => {
                and(
                  assertNotOptional(koulutus.ePerusteId, "ePerusteId"),
                  validateIfDefined[Long](
                    koulutus.ePerusteId,
                    ePerusteId => {
                      assertTrue(
                        koodiUriWithEqualOrHigherVersioNbrInList(koodiUri,
                        ePerusteKoodiClient
                          .getOsaamisalaKoodiuritForEPeruste(ePerusteId)),
                        "metadata.osaamisalaKoodiUri",
                        invalidOsaamisalaForEPeruste(ePerusteId, koodiUri)
                      )
                    }
                  )
                )
              }
            )

          case ammMuuKoulutusMetadata: AmmatillinenMuuKoulutusMetadata => and(
              assertKoulutusalaKoodiUrit(ammMuuKoulutusMetadata.koulutusalaKoodiUrit),
              assertOpintojenLaajuusyksikkoKoodiUri(ammMuuKoulutusMetadata.opintojenLaajuusyksikkoKoodiUri)
            )
          case yoKoulutusMetadata: YliopistoKoulutusMetadata           => validateKorkeaKoulutus(yoKoulutusMetadata)
          case amkKoulutusMetadata: AmmattikorkeakouluKoulutusMetadata => validateKorkeaKoulutus(amkKoulutusMetadata)
          case ammOpeErityisopeJaOpoKoulutusMetadata: AmmOpeErityisopeJaOpoKoulutusMetadata =>
              assertOpintojenLaajuusKoodiUri(ammOpeErityisopeJaOpoKoulutusMetadata.opintojenLaajuusKoodiUri)

          case lukioKoulutusMetadata: LukioKoulutusMetadata =>
            assertOpintojenLaajuusKoodiUri(lukioKoulutusMetadata.opintojenLaajuusKoodiUri)
          case tuvaKoulutusMetadata: TuvaKoulutusMetadata =>
            assertOpintojenLaajuusKoodiUri(tuvaKoulutusMetadata.opintojenLaajuusKoodiUri)
          case telmaKoulutusMetadata: TelmaKoulutusMetadata =>
            assertOpintojenLaajuusKoodiUri(telmaKoulutusMetadata.opintojenLaajuusKoodiUri)
          case vapaaSivistystyoKoulutusMetadata: VapaaSivistystyoKoulutusMetadata =>
            validateVapaaSivistystyoKoulutus(vapaaSivistystyoKoulutusMetadata)
          case aikuistenPerusopetusKoulutusMetadata: AikuistenPerusopetusKoulutusMetadata =>
            assertOpintojenLaajuusyksikkoKoodiUri(aikuistenPerusopetusKoulutusMetadata.opintojenLaajuusyksikkoKoodiUri)
          case _ => NoErrors
        }

      case _ => NoErrors
    }

    Seq(commonErrors, koulutusLevelErrors, metadataErrors).flatten
  }


  override def validateInternalDependenciesWhenDeletingEntity(koulutus: Koulutus): IsValid = {
    assertTrue(
      toteutusDAO.getByKoulutusOid(koulutus.oid.get, TilaFilter.onlyOlemassaolevat()).isEmpty,
      "tila",
      integrityViolationMsg("Koulutusta", "toteutuksia")
    )
  }

  private def validateAmmatillinenKoulutus(koulutus: Koulutus): IsValid =
    and(
      assertKoulutusKoodiUritOfKoulutustyypit(ammatillisetKoulutustyypit, koulutus),
      validateEPeruste(koulutus.ePerusteId, "ePerusteId", koulutus.koulutuksetKoodiUri)
    )

  private def validateAmmTutkinnonosaMetadata(
      ammTutkinnonOsaMetadata: AmmatillinenTutkinnonOsaKoulutusMetadata
  ): IsValid = {
    val path = "metadata.tutkinnonOsat"
    ammTutkinnonOsaMetadata.tutkinnonOsat.flatMap(osa =>
      validateIfAnyDefinedOrElse(
        Seq(osa.tutkinnonosaId, osa.tutkinnonosaViite, osa.koulutusKoodiUri), {
          val ePerusteId = osa.ePerusteId
          and(
            validateEPeruste(
              ePerusteId,
              s"$path.ePerusteId",
              Seq(osa.koulutusKoodiUri.getOrElse("")).filter(_.nonEmpty)
            ),
            validateIfAnyDefined(
              Seq(osa.tutkinnonosaId, osa.tutkinnonosaViite), {
                val tutkinnonosaViitteetAndIdt =
                  ePerusteKoodiClient.getTutkinnonosaViitteetAndIdtForEPeruste(ePerusteId.get)
                if (osa.tutkinnonosaViite.isDefined && osa.tutkinnonosaId.isDefined) {
                  val tutkinnonosaViiteJaId = tutkinnonosaViitteetAndIdt.find(_._1 == osa.tutkinnonosaViite.get)
                  and(
                    assertTrue(
                      tutkinnonosaViiteJaId.isDefined,
                      s"$path.tutkinnonosaViite",
                      invalidTutkinnonOsaViiteForEPeruste(ePerusteId.get, osa.tutkinnonosaViite.get)
                    ),
                    assertTrue(
                      tutkinnonosaViiteJaId.isDefined && tutkinnonosaViiteJaId.get._2 == osa.tutkinnonosaId.get,
                      s"$path.tutkinnonosaId",
                      invalidTutkinnonOsaIdForEPeruste(ePerusteId.get, osa.tutkinnonosaId.get)
                    )
                  )
                } else if (osa.tutkinnonosaViite.isDefined)
                  assertTrue(
                    tutkinnonosaViitteetAndIdt.find(_._1 == osa.tutkinnonosaViite.get).isDefined,
                    s"$path.tutkinnonosaViite",
                    invalidTutkinnonOsaViiteForEPeruste(ePerusteId.get, osa.tutkinnonosaViite.get)
                  )
                else {
                  assertTrue(
                    tutkinnonosaViitteetAndIdt.find(_._2 == osa.tutkinnonosaId.get).isDefined,
                    s"$path.tutkinnonosaId",
                    invalidTutkinnonOsaIdForEPeruste(ePerusteId.get, osa.tutkinnonosaId.get)
                  )
                }
              }
            )
          )
        }, {
          validateEPeruste(osa.ePerusteId, s"$path.ePerusteId", Seq())
        }
      )
    )
  }

  private def validateKorkeaKoulutus(metadata: KorkeakoulutusKoulutusMetadata): IsValid =
    and(
      assertKoulutusalaKoodiUrit(metadata.koulutusalaKoodiUrit),
      assertTutkintonimikeKoodiUrit(metadata.tutkintonimikeKoodiUrit),
      assertOpintojenLaajuusKoodiUri(metadata.opintojenLaajuusKoodiUri)
    )

  private def validateVapaaSivistystyoKoulutus(metadata: VapaaSivistystyoKoulutusMetadata): IsValid =
    and(
      assertKoulutusalaKoodiUrit(metadata.koulutusalaKoodiUrit),
      assertOpintojenLaajuusKoodiUri(metadata.opintojenLaajuusKoodiUri)
    )

  // Oletus: koodiUriFilter:in URIt eivät sisällä versiotietoa; tarkistetun koodiUrin versiota ei verrata koodiUriFilterissä
  // mahdollisesti annettuihin versioihin.
  private def assertKoulutusKoodiUrit(koodiUriFilter: Seq[String], koulutus: Koulutus): IsValid = {
    assertTrue(koulutusKoodiClient.koulutusKoodiUritExist(koodiUriFilter, koulutus.koulutuksetKoodiUri),
      "koulutuksetKoodiUri",
      invalidKoulutuskoodiuri
    )
  }

  private def assertKoulutusKoodiUritOfKoulutustyypit(koulutusTyypit: Seq[String], koulutus: Koulutus): IsValid = {
    assertTrue(koulutusKoodiClient.koulutusKoodiUritOfKoulutustyypitExist(koulutusTyypit, koulutus.koulutuksetKoodiUri),
      "koulutuksetKoodiUri",
      invalidKoulutuskoodiuri
    )
  }

  private def validateEPeruste(ePerusteId: Option[Long], path: String, koulutusKoodiUrit: Seq[String]): IsValid = {
    validateIfDefined[Long](ePerusteId, ePerusteId => {
      val koodiUritForEperuste = ePerusteKoodiClient.getKoulutusKoodiUritForEPeruste(ePerusteId)
      // ePeruste oletetaan tuntemattomaksi, ellei sille löydy yhtään koulutusKoodiUria
      and(
        assertTrue(koodiUritForEperuste.nonEmpty, path, invalidEPerusteId(ePerusteId)),
        validateIfTrue(
          koulutusKoodiUrit.nonEmpty && koodiUritForEperuste.nonEmpty,
          assertTrue(
            koulutusKoodiUrit.forall(koodiUri =>
              koodiUriWithEqualOrHigherVersioNbrInList(koodiUri, koodiUritForEperuste, false)),
            path,
            // Nykyisellään (6/2022) millään koulutustyypillä ei määritellä ePerusteID:tä + useita koulutusKoodiUreja
            if (koulutusKoodiUrit.size > 1) invalidEPerusteIdForKoulutusKoodiUrit(ePerusteId, koulutusKoodiUrit.toSeq)
            else invalidEPerusteIdForKoulutusKoodiUri(ePerusteId, koulutusKoodiUrit.head)
          )
        )
      )
    })
  }

  private def assertKoulutusalaKoodiUrit(koodiUrit: Seq[String]): IsValid = {
    assertTrue(koulutusKoodiClient.koulutusalaKoodiUritExist(koodiUrit),
      "metadata.koulutusalaKoodiUrit",
      invalidKoulutusAlaKoodiuri
    )
  }

  private def assertTutkintonimikeKoodiUrit(koodiUrit: Seq[String]): IsValid = {
    assertTrue(koulutusKoodiClient.tutkintoNimikeKoodiUritExist(koodiUrit),
      "metadata.tutkintonimikeKoodiUrit",
      invalidTutkintoNimikeKoodiuri
    )
  }

  private def assertOpintojenLaajuusKoodiUri(koodiUri: Option[String]): IsValid =
    validateIfDefined[String](
      koodiUri,
      uri =>
        assertTrue(
          koulutusKoodiClient.opintojenLaajuusKoodiUriExists(uri),
          "metadata.opintojenLaajuusKoodiUri",
          invalidOpintojenLaajuusKoodiuri
        )
    )

  private def assertOpintojenLaajuusyksikkoKoodiUri(koodiUri: Option[String]): IsValid =
    validateIfDefined[String](
      koodiUri,
      uri =>
        assertTrue(
          koulutusKoodiClient.opintojenLaajuusyksikkoKoodiUriExists(uri),
          "metadata.opintojenLaajuusyksikkoKoodiUri",
          invalidOpintojenLaajuusyksikkoKoodiuri
        )
    )
}
