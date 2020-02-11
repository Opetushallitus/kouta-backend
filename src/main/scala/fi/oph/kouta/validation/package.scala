package fi.oph.kouta

import fi.oph.kouta.domain.{Julkaisutila, Kieli}

package object validation {
  type IsValid = Seq[ValidationError]
  val NoErrors: IsValid = Nil

  trait Validatable {
    def validate(): IsValid
  }

  trait ValidatableSubEntity {
    def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid
  }

  case class ValidationError(path: String, msg: String) {
    override def toString: String = s"""{"path":"$path","msg":"$msg"}"""
  }
}
