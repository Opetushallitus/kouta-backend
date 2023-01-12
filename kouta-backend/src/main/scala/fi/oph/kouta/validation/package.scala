package fi.oph.kouta

import fi.oph.kouta.domain.Julkaisutila

package object validation {
  type IsValid = Seq[ValidationError]
  val NoErrors: IsValid = Nil

  trait Validatable {
    val tila: Julkaisutila

    def validate(): IsValid

    def validateOnJulkaisu(): IsValid = NoErrors

    def getEntityDescriptionAllative(): String
  }

  trait JulkaisuValidatableSubEntity {
    def validateOnJulkaisu(path: String): IsValid
  }

  trait ValidatableSubEntity {
    def validate(validationContext: ValidationContext, path: String): IsValid

    def validateOnJulkaisu(path: String): IsValid = NoErrors
  }

  case class ErrorMessage(msg: String, id: String, meta: Option[Map[String, AnyRef]] = None)

  case class ValidationError(path: String, msg: String, errorType: String, meta: Option[Map[String, AnyRef]] = None) {
    def getPath: String = path

    def getMsg: String = msg

    def getErrorType: String = errorType

    override def toString: String = {
      meta match {
        case Some(metaInfo) =>
          s"""{"path":"$path","msg":"$msg","errorType":"$errorType", ${metaInfo
            .map(x => x._1 + ":" + x._2.toString)
            .mkString(", ")}}"""
        case None => s"""{"path":"$path","msg":"$msg","errorType":"$errorType"}"""
      }
    }
  }

  object ValidationError {
    def apply(path: String, error: ErrorMessage): ValidationError = {
      ValidationError(path, error.msg, error.id, error.meta)
    }
  }

  object CrudOperations extends Enumeration {
    type CrudOperation = Value

    val create, update = Value
  }

  object ExternalQueryResults extends Enumeration {
    type ExternalQueryResult = Value

    val itemFound, itemNotFound, queryFailed = Value
    def fromBoolean(boolValue: Boolean): ExternalQueryResult =
      if (boolValue) itemFound else itemNotFound
  }
}
