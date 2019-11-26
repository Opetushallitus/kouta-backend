package fi.oph.kouta.util

import java.net.InetAddress
import java.time.{LocalDate, LocalDateTime}

import fi.oph.kouta.domain.{HasModified, HasPrimaryId}
import fi.oph.kouta.servlet.Authenticated
import fi.vm.sade.auditlog._
import javax.servlet.http.HttpServletRequest
import org.ietf.jgss.Oid
import org.slf4j.LoggerFactory
import slick.dbio.DBIO

object AuditLogger extends Logger {
  private val logger = LoggerFactory.getLogger(classOf[Audit])

  override def log(msg: String): Unit = logger.info(msg)
}

object AuditLog extends AuditLog(AuditLogger)

class AuditLog(val logger: Logger) {

  val audit = new Audit(logger, "kouta-backend", ApplicationType.BACKEND) // TODO: Vai ApplicationType.VIRKAILIJA?

  private val TargetEpaselva = "Tuntematon tai muutosten implikoima kohde"

  def init(): Unit = {}
/*
  def log[ID](
      auditSession: AuditSession[ID],
      operation: Operation,
      resource: Resource,
      targetId: Option[String],
      additionalTargets: Iterable[(String, String)] = Map.empty[String, String]
  ): DBIO[_] = {
    val target = getTarget(resource, targetId)
    additionalTargets.foreach { case (f, v) => target.setField(f, v) }
    audit.log(auditSession.user, operation, target.build(), auditSession.changes)
    DBIO.successful(true)
  }
*/
  def logCreate[ID, T <: HasPrimaryId[ID, T] with HasModified[T]](
      createAudit: CreateAudit[ID, T],
      resource: Resource,
      targetId: ID,
      modified: LocalDateTime
  ): DBIO[_] = {
    val target = getTarget(resource, Some(targetId))
    audit.log(createAudit.user, resource.Create, target.build(), createAudit.withPrimaryId(targetId).withModified(modified).changes)
    DBIO.successful(true)
  }

  def logUpdate[ID, T <: HasPrimaryId[ID, T] with HasModified[T]](
      updateAudit: UpdateAudit[ID, T],
      resource: Resource,
      success: Boolean,
      modifiedAfter: LocalDateTime
  ): DBIO[_] = {
    val target = getTarget(resource, updateAudit.after.primaryId)
    if (success) {
      audit.log(updateAudit.user, resource.Update, target.build(), updateAudit.withModified(modifiedAfter).changes)
    }
    DBIO.successful(true)
  }

  private def getTarget[ID](resource: Resource, targetId: Option[ID]): Target.Builder =
    new Target.Builder()
      .setField("type", resource.name)
      .setField(resource.idField, targetId.map(_.toString).getOrElse(TargetEpaselva))
}

case class UpdateAudit[ID, T <: HasPrimaryId[ID, T] with HasModified[T]](user: User, before: T, after: T) {
  def withModified(modified: LocalDateTime): UpdateAudit[ID, T] = this.copy(user, before, after.withModified(modified))

  def changes: Changes = ChangeFactory.getChanges(before, after)
}

object UpdateAudit extends AuditSession {
  def apply[ID, T <: HasPrimaryId[ID, T] with HasModified[T]](
      before: T,
      after: T
  )(implicit request: HttpServletRequest, authenticated: Authenticated): UpdateAudit[ID, T] = {
    UpdateAudit(getUser, before, after)
  }
}

case class CreateAudit[ID, T <: HasPrimaryId[ID, T] with HasModified[T]](user: User, added: T) extends GsonSupport {
  def withModified(modified: LocalDateTime): CreateAudit[ID, T] = this.copy(user, added.withModified(modified))
  def withPrimaryId(id: ID): CreateAudit[ID, T] = this.copy(user, added.withPrimaryID(id))

  def changes: Changes = new Changes.Builder().added(toGson(added).getAsJsonObject).build()
}

object CreateAudit extends AuditSession {
  def apply[ID, T <: HasPrimaryId[ID, T] with HasModified[T]](
      added: T
  )(implicit request: HttpServletRequest, authenticated: Authenticated): CreateAudit[ID, T] = {
    CreateAudit(getUser, added)
  }
}

trait AuditSession {
  def getUser(implicit request: HttpServletRequest, authenticated: Authenticated): User = {
    val userOid   = authenticated.session.personOid
    val userAgent = request.getHeader("User-Agent")
    val session   = authenticated.id.toString
    val ip        = InetAddress.getByName(request.getRemoteAddr) // TODO: Pitääkö ottaa huomioon proxyt yms?
    new User(new Oid(userOid), ip, session, userAgent)
  }
}
