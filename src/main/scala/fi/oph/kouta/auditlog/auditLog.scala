package fi.oph.kouta.auditlog

import java.net.InetAddress
import java.util.UUID

import fi.oph.kouta.domain.HasPrimaryId
import fi.oph.kouta.security.{CasSession, ServiceTicket, Session}
import fi.oph.kouta.servlet.Authenticated
import fi.vm.sade.auditlog._
import javax.servlet.http.HttpServletRequest
import org.ietf.jgss.Oid
import org.slf4j.LoggerFactory
import slick.dbio.DBIO

import scala.util.Try

import fi.oph.kouta.repository.DBIOHelpers.tryToDbioCapableTry

object AuditLogger extends Logger {
  private val logger = LoggerFactory.getLogger(classOf[Audit])

  override def log(msg: String): Unit = logger.info(msg)
}

object AuditLog extends AuditLog(AuditLogger)

class AuditLog(val logger: Logger) extends GsonSupport {

  val audit = new Audit(logger, "kouta-backend", ApplicationType.VIRKAILIJA)

  private val TargetEpaselva = "Tuntematon tai muutosten implikoima kohde"

  def init(): Unit = {}

  def logCreate[T <: HasPrimaryId[_, T]](added: T)(implicit authenticated: Authenticated): DBIO[_] = {
    val resource = AuditResource(added)
    val target   = getTarget(resource, added.primaryId)
    logCreate(added, resource, target)
  }

  def logCreate[T <: AnyRef](added: T, resource: AuditResource, targets: Seq[(String, String)])(implicit authenticated: Authenticated): DBIO[_] = {
    val target = new Target.Builder()
    targets.foreach { case (name, value) => target.setField(name, value) }
    logCreate(added, resource, target)
  }

  private def logCreate[T <: AnyRef](added: T, resource: AuditResource, target: Target.Builder)(implicit authenticated: Authenticated): DBIO[_] =
    Try {
      val changes = new Changes.Builder().added(toGson(added).getAsJsonObject).build()
      audit.log(getUser, resource.Create, target.build(), changes)
    }.toDBIO

  def logUpdate[T <: HasPrimaryId[_, T]](before: T, after: Option[T])(implicit authenticated: Authenticated): DBIO[_] =
    Try {
      after.foreach { updated =>
        val resource = AuditResource(updated)
        val target   = getTarget(resource, updated.primaryId)
        val changes  = ChangeFactory.getChanges(before, updated)
        audit.log(getUser, resource.Update, target.build(), changes)
      }
    }.toDBIO

  def logLogin(sessionId: UUID, session: CasSession, ticket: ServiceTicket)(implicit request: HttpServletRequest): (UUID, CasSession) = {
    val target  = new Target.Builder().setField("personOid", session.personOid).build()
    val user    = getUser(sessionId, session)
    val changes = new Changes.Builder().added("ticket", ticket.s).build()
    audit.log(user, AuditOperation.Login, target, changes)
    (sessionId, session)
  }

  def getUser(implicit authenticated: Authenticated): User =
    getUser(authenticated.id, authenticated.session, authenticated.userAgent, authenticated.ip)

  def getUser(sessionId: UUID, session: Session)(implicit request: HttpServletRequest): User =
    getUser(Authenticated(sessionId, session))

  def getUser(sessionId: UUID, session: Session, userAgent: String, ip: InetAddress): User =
    new User(new Oid(session.personOid), ip, sessionId.toString, userAgent)

  private def getTarget[ID](resource: AuditResource, targetId: Option[ID]): Target.Builder =
    new Target.Builder()
      .setField("type", resource.name)
      .setField(resource.idField, targetId.map(_.toString).getOrElse(TargetEpaselva))
}
