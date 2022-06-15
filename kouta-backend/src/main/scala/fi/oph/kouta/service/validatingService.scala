package fi.oph.kouta.service

import fi.oph.kouta.client.KoulutusKoodiClient
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Amm, AmmOsaamisala, AmmTutkinnonOsa, Julkaistu, Julkaisutila, Koulutus, Koulutustyyppi, Poistettu, Tallennettu, Toteutus}
import fi.oph.kouta.repository.SorakuvausDAO
import fi.oph.kouta.validation.Validations.{and, assertEmpty, assertTrue, error, tyyppiMismatch, unknownTajoajaOids, validateDependency, validateIfDefined, validateIfTrue, validateStateChange, valuesDontMatch}
import fi.oph.kouta.validation.{IsValid, NoErrors, Validatable}

import java.util.UUID

trait ValidatingService[E <: Validatable] {
  def validateParameterFormatAndExistence(e: E): IsValid
  def validateParameterFormatAndExistenceOnJulkaisu(e: E): IsValid = NoErrors
  def validateDependenciesToExternalServices(e: E): IsValid
  def validateInternalDependenciesWhenDeletingEntity(e: E): IsValid

  def organisaatioService: OrganisaatioService

  def withValidation[R](e: E, oldE: Option[E])(f: E => R): R = {

    var errors = if (oldE.isDefined) {
      if (oldE.get.tila != Julkaistu && e.tila == Julkaistu) {
        validateParameterFormatAndExistence(e) ++ validateStateChange(e.getEntityDescriptionAllative(), oldE.get.tila, e.tila) ++
          validateParameterFormatAndExistenceOnJulkaisu(e)
      } else {
        validateParameterFormatAndExistence(e) ++ validateStateChange(e.getEntityDescriptionAllative(), oldE.get.tila, e.tila)
      }
    } else {
      if (e.tila == Julkaistu) {
        validateParameterFormatAndExistence(e) ++ validateParameterFormatAndExistenceOnJulkaisu(e)
      } else {
        validateParameterFormatAndExistence(e)
      }
    }

    if (errors.isEmpty) {
      errors = validateDependenciesToExternalServices(e)
    }

    if (errors.isEmpty && oldE.isDefined) {
      val tulevaTila = e.tila
      val aiempiTila = oldE.get.tila
      if (tulevaTila == Poistettu && tulevaTila != aiempiTila)
        errors = validateInternalDependenciesWhenDeletingEntity(e)
    }

    errors match {
      case NoErrors => f(e)
      case errors => throw KoutaValidationException(errors)
    }
  }

  def throwValidationErrors(errors: IsValid): Unit =
    if(errors.nonEmpty) throw KoutaValidationException(errors)

  def validateTarjoajat(tarjoajat: List[OrganisaatioOid]): IsValid = {
    val unknownOrgs = organisaatioService.findUnknownOrganisaatioOidsFromHierarkia(tarjoajat.toSet).toSeq
    assertEmpty(unknownOrgs, "tarjoajat", unknownTajoajaOids(unknownOrgs))
  }

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
                entityKoulutusKoodiUrit.nonEmpty,
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
