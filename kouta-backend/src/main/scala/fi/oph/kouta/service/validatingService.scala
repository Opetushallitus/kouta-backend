package fi.oph.kouta.service

import fi.oph.kouta.client.KoulutusKoodiClient
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.repository.SorakuvausDAO
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, NoErrors, Validatable}

import java.util.UUID
import scala.util.{Failure, Success, Try}

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

trait KoulutusToteutusValidatingService[E <: Validatable] extends ValidatingService[E] {
  def organisaatioService: OrganisaatioService
  def koulutusKoodiClient: KoulutusKoodiClient
  def sorakuvausDAO: SorakuvausDAO

  def validateTarjoajat(koulutustyyppi: Koulutustyyppi, tarjoajat: List[OrganisaatioOid], oldTarjojat: List[OrganisaatioOid]): IsValid = {
    val newTarjoajat = if (tarjoajat.toSet != oldTarjojat.toSet) tarjoajat else List()
    val validTarjoajat = newTarjoajat.filter(_.isValid)
    var tarjoajatWoRequiredKoulutustyyppi = Seq[OrganisaatioOid]()
    var organisaatioServiceOk = true
    Try[Seq[OrganisaatioOid]] {
      validTarjoajat.filterNot(organisaatioService.getAllChildOidsAndOppilaitostyypitFlat(_)._2.contains(koulutustyyppi))
    } match {
      case Success(tarjoajaOidsWoRequiredKoulutustyyppi) => tarjoajatWoRequiredKoulutustyyppi = tarjoajaOidsWoRequiredKoulutustyyppi
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
            assertFalse(tarjoajatWoRequiredKoulutustyyppi.contains(oid), path, tarjoajaOidWoRequiredKoulutustyyppi(oid, koulutustyyppi))
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

case class KoutaValidationException(errorMessages: IsValid) extends RuntimeException {
  override def getMessage: String = "[" + errorMessages.mkString(",") + "]"
}
