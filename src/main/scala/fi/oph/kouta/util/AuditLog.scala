package fi.oph.kouta.util

import java.net.InetAddress

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

class AuditLog(val logger: Logger) extends GsonSupport {

  val audit = new Audit(logger, "kouta-backend", ApplicationType.BACKEND) // TODO: Vai ApplicationType.VIRKAILIJA?

  private val TargetEpaselva = "Tuntematon tai muutosten implikoima kohde"

  def init(): Unit = {}

  def logCreate[T <: HasPrimaryId[_, T] with HasModified[T]](added: T, user: User): DBIO[_] = {
    val resource = Resource(added)
    val target = getTarget(resource, added.primaryId)
    val changes = new Changes.Builder().added(toGson(added).getAsJsonObject).build()
    audit.log(user, resource.Create, target.build(), changes)
    DBIO.successful(true)
  }

  def logUpdate[T <: HasPrimaryId[_, T] with HasModified[T]](before: T, after: Option[T], user: User): DBIO[_] = {
    after match {
      case Some(updated) =>
        val resource = Resource(updated)
        val target = getTarget(resource, updated.primaryId)
        val changes = ChangeFactory.getChanges(before, updated)
        audit.log(user, resource.Update, target.build(), changes)
      case None =>
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
