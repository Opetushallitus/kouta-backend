package fi.oph.kouta.auditlog

import fi.vm.sade.auditlog.Operation

abstract class AuditOperation(val name: String) extends Operation

object AuditOperation {
  case object Login    extends AuditOperation("kirjautuminen")
  case object S3Upload extends AuditOperation("s3_upload")
  case object S3Copy   extends AuditOperation("s3_copy")
  case object S3Delete extends AuditOperation("s3_delete")
}

trait AuditResourceOperations {
  def name: String
  case object Read   extends AuditOperation(s"${name}_read")
  case object Update extends AuditOperation(s"${name}_update")
  case object Create extends AuditOperation(s"${name}_create")
}
