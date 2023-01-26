package fi.oph.kouta.service

import fi.oph.kouta.client.CachedKoodistoClient
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.repository.SorakuvausDAO
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, NoErrors, Validatable, ValidationContext}

import java.util.UUID
import scala.util.{Failure, Success, Try}
import scalaz.syntax.std.boolean._

trait ValidatingService[E <: Validatable] {
  def validateEntity(e: E, oldE: Option[E]): IsValid
  def validateEntityOnJulkaisu(e: E): IsValid = NoErrors
  def validateInternalDependenciesWhenDeletingEntity(e: E): IsValid

  def withValidation[R](e: E, oldE: Option[E])(f: E => R): R = {
    validate(e, oldE) match {
      case NoErrors => f(e)
      case errors   => throw KoutaValidationException(errors)
    }
  }

  def validate(e: E, oldE: Option[E]): IsValid = {
    var errors = if (oldE.isDefined) {
      if (oldE.get.tila == Tallennettu && e.tila == Julkaistu) {
        validateEntity(e, oldE) ++ validateStateChange(e.getEntityDescriptionAllative(), oldE.get.tila, e.tila) ++
          validateEntityOnJulkaisu(e)
      } else {
        validateEntity(e, oldE) ++ validateStateChange(e.getEntityDescriptionAllative(), oldE.get.tila, e.tila)
      }
    } else {
      if (e.tila == Julkaistu) {
        validateEntity(e, None) ++ validateEntityOnJulkaisu(e)
      } else {
        validateEntity(e, None)
      }
    }

    if (errors.isEmpty && oldE.isDefined) {
      val tulevaTila = e.tila
      val aiempiTila = oldE.get.tila
      if (tulevaTila == Poistettu && tulevaTila != aiempiTila)
        errors = validateInternalDependenciesWhenDeletingEntity(e)
    }

    errors
  }

  def koodiUriTipText(koodiUri: String): Option[String] =
    Some(s"$koodiUri#<versionumero>, esim. $koodiUri#1")
}

trait KoulutusToteutusValidatingService[E <: Validatable] extends ValidatingService[E] with KoulutusKoodiValidator {
  def organisaatioService: OrganisaatioService
  def sorakuvausDAO: SorakuvausDAO

  /** Validoi koulutuksen tai toteutuksen tarjoajat-kentän.
    * @param koulutustyyppi koulutustyyppi, jonka perusteella halutaan rajoittaa tarjoajien sallittuja oppilaitostyyppejä (kts. OrganisaatioService.oppilaitostyypitToKoulutustyypit)
    * @param tarjoajat Uusi tarjoajat-kentän arvo
    * @param oldTarjoajat Entinen tarjoajat-kentän arvo. (tyhjä, jos ollaan luomassa)
    * @param oppilaitostyypit Oppilaitostyypit, jotka ovat koulutustyypin perusteella valittujen asemesta sallittuja tarjoajille. Käytetään ainoastaan koulutuksen validoinnissa. HUOM! Yliajaa koulutustyyppi-tarkistuksen!
    * @return Mahdolliset validointivirheet listana tai tyhjä lista, jos validi
    */
  def validateTarjoajat(
      koulutustyyppi: Koulutustyyppi,
      tarjoajat: Seq[OrganisaatioOid],
      oldTarjoajat: Seq[OrganisaatioOid],
      oppilaitostyypit: Seq[String] = Seq()
  ): IsValid = {
    val newTarjoajat                        = if (tarjoajat.toSet != oldTarjoajat.toSet) tarjoajat else List()
    val validTarjoajat                      = newTarjoajat.filter(_.isValid)
    var organisaatioServiceOk               = true
    var tarjoajatWoRequiredKoulutustyyppi   = Seq[OrganisaatioOid]()
    var tarjoajatWoRequiredOppilaitostyypit = Seq[OrganisaatioOid]()

    Try[(Seq[OrganisaatioOid], Seq[OrganisaatioOid])] {
      (
        organisaatioService.withoutOppilaitostyypit(validTarjoajat, oppilaitostyypit),
        validTarjoajat.filterNot(
          organisaatioService.getAllChildOidsAndKoulutustyypitFlat(_)._2.contains(koulutustyyppi)
        )
      )
    } match {
      case Success((tarjoajaOidsWoRequiredOppilaitostyyppi, tarjoajaOidsWoRequiredKoulutustyyppi)) =>
        tarjoajatWoRequiredOppilaitostyypit = tarjoajaOidsWoRequiredOppilaitostyyppi
        tarjoajatWoRequiredKoulutustyyppi = tarjoajaOidsWoRequiredKoulutustyyppi
      case Failure(_) => organisaatioServiceOk = false
    }

    validateIfTrueOrElse(
      organisaatioServiceOk,
      validateIfNonEmpty[OrganisaatioOid](
        newTarjoajat,
        "tarjoajat",
        (oid, path) =>
          validateIfSuccessful(
            assertTrue(oid.isValid, path, validationMsg(oid.s)),
            if (oppilaitostyypit.isEmpty) {
              assertFalse(
                tarjoajatWoRequiredKoulutustyyppi.contains(oid),
                path,
                tarjoajaOidWoRequiredKoulutustyyppi(oid, koulutustyyppi)
              )
            } else {
              assertFalse(
                tarjoajatWoRequiredOppilaitostyypit.contains(oid),
                path,
                tarjoajaOidWoRequiredOppilaitostyyppi(oid, oppilaitostyypit)
              )
            }
          )
      ),
      error("tarjoajat", organisaatioServiceFailureMsg)
    )
  }

  def validateSorakuvausIntegrity(
      sorakuvausId: Option[UUID],
      entityTila: Julkaisutila,
      entityTyyppi: Koulutustyyppi,
      entityTyyppiPath: String = "koulutustyyppi",
      entityKoulutusKoodiUrit: Seq[String] = Seq()
  ): IsValid = {

    validateIfDefined[UUID](
      sorakuvausId,
      sorakuvausId => {
        val (sorakuvausTila, sorakuvausTyyppi, koulutuskoodiUrit) =
          sorakuvausDAO.getTilaTyyppiAndKoulutusKoodit(sorakuvausId)
        and(
          validateDependency(entityTila, sorakuvausTila, sorakuvausId, "Sorakuvausta", "sorakuvausId"),
          validateIfDefined[Koulutustyyppi](
            sorakuvausTyyppi,
            sorakuvausTyyppi =>
              // "Tutkinnon osa" ja Osaamisala koulutuksiin saa liittää myös SORA-kuvauksen, jonka koulutustyyppi on "ammatillinen"
              assertTrue(
                sorakuvausTyyppi == entityTyyppi || (sorakuvausTyyppi == Amm && Seq(
                  AmmOsaamisala,
                  AmmTutkinnonOsa
                ).contains(entityTyyppi)),
                entityTyyppiPath,
                tyyppiMismatch("sorakuvauksen", sorakuvausId)
              )
          ),
          validateIfDefined[Seq[String]](
            koulutuskoodiUrit,
            koulutuskoodiUrit => {
              validateIfTrue(
                koulutuskoodiUrit.nonEmpty && entityKoulutusKoodiUrit.nonEmpty,
                assertTrue(
                  koulutuskoodiUrit.intersect(entityKoulutusKoodiUrit).nonEmpty,
                  "koulutuksetKoodiUri",
                  valuesDontMatch("Sorakuvauksen", "koulutusKoodiUrit")
                )
              )
            }
          )
        )
      }
    )
  }
}

trait KoulutusKoodiValidator {
  def cachedKoodistoClient: CachedKoodistoClient

  def validateKoulutusKoodiUrit(
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
              cachedKoodistoClient,
              path,
              vCtx,
              invalidKoulutuskoodiuri(koodiUri)
            )
        )
      )
    )

  def assertKoulutusalaKoodiUrit(koodiUrit: Seq[String], validationContext: ValidationContext): IsValid =
    validateIfNonEmpty[String](
      koodiUrit,
      "metadata.koulutusalaKoodiUrit",
      (koodiUri, path) =>
        assertKoodistoQueryResult(
          koodiUri,
          cachedKoodistoClient.koodiUriExistsInKoodisto(KoulutusalaKoodisto, _),
          path,
          validationContext,
          invalidKoulutusAlaKoodiuri(koodiUri)
        )
    )

  def assertOpintojenLaajuusyksikkoKoodiUri(
      koodiUri: Option[String],
      validationContext: ValidationContext
  ): IsValid =
    validateIfDefined[String](
      koodiUri,
      uri =>
        assertKoodistoQueryResult(
          uri,
          cachedKoodistoClient.koodiUriExistsInKoodisto(OpintojenLaajuusyksikkoKoodisto, _),
          "metadata.opintojenLaajuusyksikkoKoodiUri",
          validationContext,
          invalidOpintojenLaajuusyksikkoKoodiuri(uri)
        )
    )

  def validateLaajuusMinMax(laajuusNumeroMin: Option[Double], laajuusNumeroMax: Option[Double]): IsValid = {
    and(
      validateIfDefined[Double](
        laajuusNumeroMin,
        assertNotNegative(_, "metadata.opintojenLaajuusNumeroMin")
      ),
      validateIfDefined[Double](
        laajuusNumeroMax,
        assertNotNegative(_, "metadata.opintojenLaajuusNumeroMax")
      ),
      validateMinMax(laajuusNumeroMin, laajuusNumeroMax, s"metadata.opintojenLaajuusNumeroMin")
    )
  }

  def validateOpintojenLaajuusyksikkoAndNumero(
      laajuusyksikkoKoodiUri: Option[String],
      hasLaajuusyksikkoChanged: Boolean,
      laajuusNumero: Option[Double],
      mandatoryIfJulkaistu: Boolean,
      validationContext: ValidationContext,
  ): IsValid =
    and(
      validateOpintojenLaajuusyksikko(laajuusyksikkoKoodiUri, validationContext, hasLaajuusyksikkoChanged),
      validateIfDefined[Double](
        laajuusNumero,
        assertNotNegative(_, "metadata.opintojenLaajuusNumero")
      ),
      validateIfTrue(
        mandatoryIfJulkaistu,
        validateIfJulkaistu(
          validationContext.tila,
          assertNotOptional(laajuusNumero, "metadata.opintojenLaajuusNumero")
        )
      )
    )

  def validateOpintojenLaajuusyksikko(
      koodiUri: Option[String],
      validationContext: ValidationContext,
      hasChanged: Boolean,
  ): IsValid =
    and(
      assertOpintojenLaajuusyksikkoKoodiUri(hasChanged.option(koodiUri).flatten, validationContext),
      validateIfJulkaistu(
        validationContext.tila,
        and(
          assertNotOptional(koodiUri, "metadata.opintojenLaajuusyksikkoKoodiUri")
        )
      )
    )
}

trait ValidatingSubService[E] {
  def validate(entity: E, oldEntity: Option[E], vCtx: ValidationContext): IsValid
}

case class KoutaValidationException(errorMessages: IsValid) extends RuntimeException {
  override def getMessage: String = "[" + errorMessages.mkString(",") + "]"
}
