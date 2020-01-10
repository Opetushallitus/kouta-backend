package fi.oph.kouta

package object validation {
  type IsValid = List[String]
  val NoErrors: IsValid = Nil

  trait Validatable extends Validations {
    def validate(): IsValid
  }
}
