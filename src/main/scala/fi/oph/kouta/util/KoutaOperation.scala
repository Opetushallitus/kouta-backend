package fi.oph.kouta.util

import fi.vm.sade.auditlog.Operation

abstract class KoutaOperation(val name: String) extends Operation

sealed abstract class BasicOperations(val entity: String) {
  case object Read   extends KoutaOperation(s"${entity}_READ")
  case object Update extends KoutaOperation(s"${entity}_UPDATE")
  case object Create extends KoutaOperation(s"${entity}_CREATE")
}

trait ResourceOperations {
  def name: String
  case object Read   extends KoutaOperation(s"${name}_READ")
  case object Update extends KoutaOperation(s"${name}_UPDATE")
  case object Create extends KoutaOperation(s"${name}_CREATE")
}

object KoutaOperation {

  object Koulutus         extends BasicOperations("KOULUTUS")
  object Toteutus         extends BasicOperations("TOTEUTUS")
  object Haku             extends BasicOperations("HAKU")
  object Hakukohde        extends BasicOperations("HAKUKOHDE")
  object Valintaperuste   extends BasicOperations("VALINTAPERUSTE")
  object Oppilaitos       extends BasicOperations("OPPILAITOS")
  object OppilaitoksenOsa extends BasicOperations("OPPILAITOKSEN_OSA")

}
