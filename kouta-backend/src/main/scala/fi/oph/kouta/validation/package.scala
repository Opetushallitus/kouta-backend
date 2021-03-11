package fi.oph.kouta

import fi.oph.kouta.domain.{Julkaisutila, Kieli}

package object validation {
  type IsValid = Seq[ValidationError]
  val NoErrors: IsValid = Nil

  trait Validatable {
    val tila: Julkaisutila

    def validate(): IsValid

    def validateOnJulkaisu(): IsValid = NoErrors
  }

  trait ValidatableSubEntity {
    def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid

    def validateOnJulkaisu(path: String): IsValid = NoErrors
  }

  case class ErrorMessage(msg: String, id: String)

  case class ValidationError(path: String, msg: String, errorType: String) {
    override def toString: String = s"""{"path":"$path","msg":"$msg","errorType":"$errorType"}"""
  }

  object ValidationError {
    def apply(path: String, error: ErrorMessage): ValidationError = {
      ValidationError(path, error.msg, error.id)
    }
  }
}
