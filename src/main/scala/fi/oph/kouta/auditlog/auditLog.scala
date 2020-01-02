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

object AuditLogger extends Logger {
  private val logger = LoggerFactory.getLogger(classOf[Audit])

  override def log(msg: String): Unit = logger.info(msg)
}

object AuditLog extends AuditLog(AuditLogger)

class AuditLog(val logger: Logger) extends GsonSupport {

  val audit = new Audit(logger, "kouta-backend", ApplicationType.BACKEND) // TODO: Vai ApplicationType.VIRKAILIJA?

  private val TargetEpaselva = "Tuntematon tai muutosten implikoima kohde"

  def init(): Unit = {}

  def logCreate[T <: HasPrimaryId[_, T]](added: T, user: User): DBIO[_] = {
    val resource = AuditResource(added)
    val target   = getTarget(resource, added.primaryId)
    val changes  = new Changes.Builder().added(toGson(added).getAsJsonObject).build()
    audit.log(user, resource.Create, target.build(), changes)
    DBIO.successful(true)
  }

  def logUpdate[T <: HasPrimaryId[_, T]](before: T, after: Option[T], user: User): DBIO[_] = {
    after match {
      case Some(updated) =>
        val resource = AuditResource(updated)
        val target   = getTarget(resource, updated.primaryId)
        val changes  = ChangeFactory.getChanges(before, updated)
        audit.log(user, resource.Update, target.build(), changes)
      case None =>
    }
    DBIO.successful(true)
  }

  def logCreate[T <: AnyRef](
                              added: T,
                              resource: AuditResource,
                              user: User,
                              targets: Seq[(String, String)]
  ): DBIO[Boolean] = {
    val target = new Target.Builder()
    targets.foreach { case (name, value) => target.setField(name, value) }
    val changes = new Changes.Builder().added(toGson(added).getAsJsonObject).build()
    audit.log(user, resource.Create, target.build(), changes)
    DBIO.successful(true)
  }

  def logLogin(sessionId: UUID, session: CasSession, ticket: ServiceTicket)(implicit request: HttpServletRequest): (UUID, CasSession) = {
    val target  = new Target.Builder().setField("personOid", session.personOid).build()
    val user    = getUser(sessionId, session)
    val changes = new Changes.Builder().added("ticket", ticket.s).build()
    audit.log(user, AuditOperation.Login, target, changes)
    (sessionId, session)
  }

  def getUser(implicit request: HttpServletRequest, authenticated: Authenticated): User =
    getUser(authenticated.id, authenticated.session)

  def getUser(sessionId: UUID, session: Session)(implicit request: HttpServletRequest): User = {
    val userOid   = session.personOid
    val userAgent = request.getHeader("User-Agent")
    val ip        = InetAddress.getByName(request.getRemoteAddr) // TODO: Pitääkö ottaa huomioon proxyt yms?
    new User(new Oid(userOid), ip, sessionId.toString, userAgent)
  }

  private def getTarget[ID](resource: AuditResource, targetId: Option[ID]): Target.Builder =
    new Target.Builder()
      .setField("type", resource.name)
      .setField(resource.idField, targetId.map(_.toString).getOrElse(TargetEpaselva))
}
