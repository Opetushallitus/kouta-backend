package fi.oph.kouta.service

import fi.oph.kouta.domain.{Julkaistu, Poistettu}
import fi.oph.kouta.validation.Validations.{error, validateStateChange}
import fi.oph.kouta.validation.{IsValid, NoErrors, Validatable}

trait ValidatingService[E <: Validatable] {
  def validateParameterFormatAndExistence(e: E): IsValid
  def validateParameterFormatAndExistenceOnJulkaisu(e: E): IsValid = NoErrors
  def validateDependenciesToExternalServices(e: E): IsValid
  def validateInternalDependenciesWhenDeletingEntity(e: E): IsValid

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
}

case class KoutaValidationException(errorMessages: IsValid) extends RuntimeException {
  override def getMessage: String = "[" + errorMessages.mkString(",") + "]"
}
