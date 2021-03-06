package fi.oph.kouta.service

import fi.oph.kouta.domain.Julkaistu
import fi.oph.kouta.validation.{IsValid, NoErrors, Validatable}

trait ValidatingService[E <: Validatable] {

  def withValidation[R](e: E, oldE: Option[E])(f: E => R): R = {
    val errors = if (!oldE.exists(_.tila == Julkaistu) && e.tila == Julkaistu) {
      e.validate() ++ e.validateOnJulkaisu()
    } else {
      e.validate()
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
