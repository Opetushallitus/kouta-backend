package fi.oph.kouta

import fi.oph.kouta.domain.{Julkaisutila, Kieli}

package object validation {
  type IsValid = Seq[ValidationError]
  val NoErrors: IsValid = Nil

  trait Validatable {
    val tila: Julkaisutila

    def validate(): IsValid

    def validateOnJulkaisu(): IsValid = NoErrors

    def getEntityDescriptionAllative(): String
  }

  trait ValidatableSubEntity {
    def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid

    def validateOnJulkaisu(path: String): IsValid = NoErrors
  }

  case class ErrorMessage(msg: String, id: String, meta: Option[Map[String, AnyRef]] = None)

  case class ValidationError(path: String, msg: String, errorType: String, meta: Option[Map[String, AnyRef]] = None) {
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
  val ammatillisetKoulutustyypit =
    Seq(
      "koulutustyyppi_1",
      "koulutustyyppi_4",
      "koulutustyyppi_5",
      "koulutustyyppi_7",
      "koulutustyyppi_8",
      "koulutustyyppi_11",
      "koulutustyyppi_12",
      "koulutustyyppi_13",
      "koulutustyyppi_18",
      "koulutustyyppi_19",
      "koulutustyyppi_26"
    )

  val yoKoulutustyypit =
    Seq(
      "tutkintotyyppi_13",
      "tutkintotyyppi_14",
      "tutkintotyyppi_15",
      "eqf_8"
    )

  val amkKoulutustyypit                      = Seq("tutkintotyyppi_06", "tutkintotyyppi_07", "tutkintotyyppi_12")
  val ammOpeErityisopeJaOpoKoulutusKoodiUrit = Seq("koulutus_000001", "koulutus_000002", "koulutus_000003")
  val lukioKoulutusKoodiUrit =
    Seq("koulutus_309902", "koulutus_301102", "koulutus_301101", "koulutus_301103", "koulutus_301104")
  val erikoislaakariKoulutusKoodiUrit        = Seq("koulutus_775101", "koulutus_775201", "koulutus_775301")

  val ammatillinenPerustutkintoKoulutustyyppi = "koulutustyyppi_26"
  val lukioKoulutusKoodiUritAllowedForKaksoistutkinto = Seq("koulutus_301101", "koulutus_309902")
}
