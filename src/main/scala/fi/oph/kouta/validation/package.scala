package fi.oph.kouta

package object validation {
  type IsValid = Seq[String]
  val NoErrors: IsValid = Nil

  trait Validatable extends Validations {
    def validate(): IsValid
  }
}
