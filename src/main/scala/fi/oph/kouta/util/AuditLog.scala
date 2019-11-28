package fi.oph.kouta.util

import java.net.InetAddress
import java.time.{Instant, LocalDateTime}

import fi.oph.kouta.domain.{HasModified, HasPrimaryId}
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.TimeUtils.instantToLocalDateTime
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

class AuditLog(val logger: Logger) extends GsonSupport {

  val audit = new Audit(logger, "kouta-backend", ApplicationType.BACKEND) // TODO: Vai ApplicationType.VIRKAILIJA?

  private val TargetEpaselva = "Tuntematon tai muutosten implikoima kohde"

  def init(): Unit = {}

  def logCreate[T <: HasPrimaryId[_, T] with HasModified[T]](
      added: T,
      user: User,
      resource: Resource
  ): DBIO[_] = {
    val target  = getTarget(resource, added.primaryId)
    val changes = new Changes.Builder().added(toGson(added).getAsJsonObject).build()
    audit.log(user, resource.Create, target.build(), changes)
    DBIO.successful(true)
  }

  def logUpdate[ID, T <: HasPrimaryId[ID, T] with HasModified[T]](
      updateAudit: UpdateAudit[ID, T],
      resource: Resource,
      success: Boolean,
      modifiedAfter: Instant
  ): DBIO[_] = {
    val target = getTarget(resource, updateAudit.after.primaryId)
    if (success) {
      val changes = updateAudit.withModified(instantToLocalDateTime(modifiedAfter)).changes
      audit.log(updateAudit.user, resource.Update, target.build(), changes)
    }
    DBIO.successful(true)
  }

  def getUser(implicit request: HttpServletRequest, authenticated: Authenticated): User = {
    val userOid   = authenticated.session.personOid
    val userAgent = request.getHeader("User-Agent")
    val session   = authenticated.id.toString
    val ip        = InetAddress.getByName(request.getRemoteAddr) // TODO: Pitääkö ottaa huomioon proxyt yms?
    new User(new Oid(userOid), ip, session, userAgent)
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

trait AuditSession {
  def getUser(implicit request: HttpServletRequest, authenticated: Authenticated): User = {
    val userOid   = authenticated.session.personOid
    val userAgent = request.getHeader("User-Agent")
    val session   = authenticated.id.toString
    val ip        = InetAddress.getByName(request.getRemoteAddr) // TODO: Pitääkö ottaa huomioon proxyt yms?
    new User(new Oid(userOid), ip, session, userAgent)
  }
}
