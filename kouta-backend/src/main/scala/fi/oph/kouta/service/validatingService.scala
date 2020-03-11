package fi.oph.kouta.service

import fi.oph.kouta.domain.Julkaistu
import fi.oph.kouta.validation.{IsValid, NoErrors, Validatable, ValidationError}

trait ValidatingService[E <: Validatable] {

  def withValidation[R](e: E, oldE: Option[E])(f: => R): R = {
    val errors = if (!oldE.exists(_.tila == Julkaistu) && e.tila == Julkaistu) {
      e.validate() ++ e.validateOnJulkaisu()
    } else {
      e.validate()
    }

    errors match {
      case NoErrors => f
      case errors => throw KoutaValidationException(errors)
    }
  }

  def singleError(path: String, msg: String) = throw KoutaValidationException(Seq(ValidationError(path, msg)))
}

case class KoutaValidationException(errorMessages: IsValid) extends RuntimeException
