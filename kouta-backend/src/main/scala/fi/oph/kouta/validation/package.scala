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

  case class ValidationError(path: String, msg: String) {
    override def toString: String = s"""{"path":"$path","msg":"$msg"}"""
  }


  type IsValidUusi = Seq[ValidationErrorUusi]
  val NoErrorsUusi: IsValidUusi = Nil

  trait ValidatableUusi {
    val tila: Julkaisutila

    def validate(): IsValidUusi

    def validateOnJulkaisu(): IsValidUusi = NoErrorsUusi
  }

  trait ValidatableSubEntityUusi {
    def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValidUusi

    def validateOnJulkaisu(path: String): IsValidUusi = NoErrorsUusi
  }

  case class ErrorMessage(msg: String, id: String)

  case class ValidationErrorUusi(path: String, error: ErrorMessage) {
    override def toString: String = s"""{"path":"$path","msg":"${error.msg}","errorType":"${error.id}"}"""
  }

}
