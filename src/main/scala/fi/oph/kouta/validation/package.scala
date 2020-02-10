package fi.oph.kouta

import fi.oph.kouta.domain.{Julkaisutila, Kieli}

package object validation {
  type IsValid = Seq[String]
  val NoErrors: IsValid = Nil

  trait Validatable {
    def validate(): IsValid
  }

  trait ValidatableSubEntity {
    def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli]): IsValid
  }
}
