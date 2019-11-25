package fi.oph.kouta.util

import fi.vm.sade.auditlog.Operation

abstract class KoutaOperation(val name: String) extends Operation

trait ResourceOperations {
  def name: String
  case object Read   extends KoutaOperation(s"${name}_read")
  case object Update extends KoutaOperation(s"${name}_update")
  case object Create extends KoutaOperation(s"${name}_create")
}
