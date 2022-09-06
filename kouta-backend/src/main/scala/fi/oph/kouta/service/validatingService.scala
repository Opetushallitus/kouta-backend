package fi.oph.kouta.service

import fi.oph.kouta.client.KoulutusKoodiClient
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.repository.SorakuvausDAO
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, NoErrors, Validatable}

import java.util.UUID

trait ValidatingService[E <: Validatable] {
  def validateEntity(e: E, oldE: Option[E]): IsValid
  def validateEntityOnJulkaisu(e: E): IsValid = NoErrors
  def validateInternalDependenciesWhenDeletingEntity(e: E): IsValid

  def organisaatioService: OrganisaatioService

  def withValidation[R](e: E, oldE: Option[E])(f: E => R): R = {
    validate(e, oldE) match {
      case NoErrors => f(e)
      case errors   => throw KoutaValidationException(errors)
    }
  }

  def validate(e: E, oldE: Option[E]): IsValid = {
    var errors = if (oldE.isDefined) {
      if (oldE.get.tila != Julkaistu && e.tila == Julkaistu) {
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

  def throwValidationErrors(errors: IsValid): Unit =
    if (errors.nonEmpty) throw KoutaValidationException(errors)

  def validateTarjoajat(tarjoajat: List[OrganisaatioOid], oldTarjojat: List[OrganisaatioOid]): IsValid = {
    val newTarjoajat = if (tarjoajat.toSet != oldTarjojat.toSet) tarjoajat else List()
    val validTarjoajat = newTarjoajat.filter(_.isValid)
    val (unknownOrgs: Set[OrganisaatioOid], organisaatioServiceOk: Boolean) =
      if (validTarjoajat.nonEmpty) organisaatioService.findUnknownOrganisaatioOidsFromHierarkia(validTarjoajat.toSet)
      else (Set[OrganisaatioOid](), true)
    validateIfTrueOrElse(
      organisaatioServiceOk,
      validateIfNonEmpty[OrganisaatioOid](
        newTarjoajat,
        "tarjoajat",
        (oid, path) =>
          validateIfSuccessful(
            assertTrue(oid.isValid, path, validationMsg(oid.s)),
            assertFalse(unknownOrgs.contains(oid), path, unknownTarjoajaOid(oid))
          )
      ),
      error("tarjoajat", organisaatioServiceFailureMsg)
    )
  }

  def koodiUriTipText(koodiUri: String): Option[String] =
    Some(s"$koodiUri#<versionumero>, esim. $koodiUri#1")
}

trait KoulutusToteutusValidatingService[E <: Validatable] extends ValidatingService[E] {
  def koulutusKoodiClient: KoulutusKoodiClient
  def sorakuvausDAO: SorakuvausDAO

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
