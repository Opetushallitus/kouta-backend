package fi.oph.kouta.service

import fi.oph.kouta.domain.Julkaistu
import fi.oph.kouta.validation.Validations.validateStateChange
import fi.oph.kouta.validation.{IsValid, NoErrors, Validatable}

trait ValidatingService[E <: Validatable] {

  def withValidation[R](e: E, oldE: Option[E])(f: E => R): R = {

    val errors = if (oldE.isDefined) {
      if (oldE.get.tila != Julkaistu && e.tila == Julkaistu) {
        e.validate() ++ validateStateChange(e.getEntityDescriptionAllative(), oldE.get.tila, e.tila) ++ e.validateOnJulkaisu()
      } else {
        e.validate() ++ validateStateChange(e.getEntityDescriptionAllative(), oldE.get.tila, e.tila)
      }
    } else {
      if (e.tila == Julkaistu) {
        e.validate() ++ e.validateOnJulkaisu()
      } else {
        e.validate()
      }
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
