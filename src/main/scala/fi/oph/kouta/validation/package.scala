package fi.oph.kouta

package object validation {
  type IsValid = Either[String,Unit]

  trait Validatable extends Validations {
    def validate():IsValid
  }
}
