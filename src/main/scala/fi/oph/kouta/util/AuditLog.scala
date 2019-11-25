package fi.oph.kouta.util

import java.net.InetAddress

import fi.oph.kouta.domain.HasPrimaryId
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

class AuditLog(val logger: Logger) extends GsonSupport {

  val audit = new Audit(logger, "kouta-backend", ApplicationType.BACKEND) // TODO: Vai ApplicationType.VIRKAILIJA?

  private val TargetEpaselva = "Tuntematon tai muutosten implikoima kohde"

  def init(): Unit = {}

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

  def logCreate[ID](
      auditSession: AuditSession[ID],
      resource: Resource,
      targetId: String
  ): DBIO[_] = {
    val target = getTarget(resource, Some(targetId))
    audit.log(auditSession.user, resource.Create, target.build(), auditSession.changes)
    DBIO.successful(true)
  }

  def logUpdate[ID](
      auditSession: AuditSession[ID],
      resource: Resource,
      success: Boolean
  ): DBIO[_] = {
    val target = getTarget(resource, auditSession.id)
    if (success) {
      audit.log(auditSession.user, resource.Update, target.build(), auditSession.changes)
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

  def getPutSession[ID, T <: HasPrimaryId[ID, T]](
      added: T
  )(implicit request: HttpServletRequest, authenticated: Authenticated): AuditSession[ID] = {
    val changes = new Changes.Builder().added(toGson(added).getAsJsonObject).build()
    AuditSession(None, getUser, changes)
  }

  def getUpdateSession[ID, T <: HasPrimaryId[ID, T]](
      before: T,
      after: T
  )(implicit request: HttpServletRequest, authenticated: Authenticated): AuditSession[ID] = {
    val changes = ChangeFactory.getChanges(before, after)
    AuditSession(before.primaryId, getUser, changes)
  }

  private def getTarget[ID](resource: Resource, targetId: Option[ID]): Target.Builder =
    new Target.Builder()
      .setField("type", resource.name)
      .setField(resource.idField, targetId.map(_.toString).getOrElse(TargetEpaselva))
}

case class AuditSession[ID](id: Option[ID], user: User, changes: Changes)
