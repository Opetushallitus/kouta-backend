package fi.oph.kouta

package object validation {
  type IsValid = Either[List[String],Unit]

  trait Validatable extends Validations {
    def validate():IsValid

    def and(validations: IsValid*): IsValid = validations collect { case Left(msgList) => msgList } match {
      case l if l.isEmpty => Right()
      case l => Left(l.flatten.distinct.toList)
    }
  }
}
