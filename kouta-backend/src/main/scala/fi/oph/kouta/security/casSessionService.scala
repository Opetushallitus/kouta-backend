package fi.oph.kouta.security

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.logging.Logging
import fi.oph.kouta.repository.SessionDAO
import fi.vm.sade.javautils.nio.cas.UserDetails

import java.util.UUID
import javax.servlet.http.HttpServletRequest
import scala.collection.JavaConverters._

case class AuthenticationFailedException(msg: String, cause: Throwable) extends RuntimeException(msg, cause) {
  def this(msg: String) = this(msg, null)
  def this() = this(null, null)
}

case class KayttooikeusUserDetails(authorities: Set[Authority], oid: String)

object CasSessionService extends CasSessionService(ProductionSecurityContext(KoutaConfigurationFactory.configuration.securityConfiguration),
                                                   AuditLog)

abstract class CasSessionService(val securityContext: SecurityContext, auditLog: AuditLog) extends Logging {
  logger.info(s"Using security context ${securityContext.getClass.getSimpleName}")

  val serviceIdentifier: String = securityContext.casServiceIdentifier
  val casUrl: String = securityContext.casUrl

  private val casClient = securityContext.casClient

  private def validateServiceTicket(ticket: ServiceTicket): Either[Throwable, UserDetails] = {
    val ServiceTicket(s) = ticket
    try {
      Right(casClient.validateServiceTicketWithVirkailijaUserDetailsBlocking(securityContext.casServiceIdentifier, ticket.s))
    } catch {
      case e: Exception => Left(AuthenticationFailedException(s"Failed to validate service ticket $s", e))
    }
  }

  private def storeSession(ticket: ServiceTicket, user: KayttooikeusUserDetails): (UUID, CasSession) = {
    val session = CasSession(ticket, user.oid, user.authorities)
    logger.debug(s"Storing to session: ${session.casTicket} ${session.personOid} ${session.authorities}")
    val id = SessionDAO.store(session)
    (id, session)
  }

  private def createSession(ticket: ServiceTicket)(implicit request: HttpServletRequest): Either[Throwable, (UUID, CasSession)] = {
    validateServiceTicket(ticket)
      .map(extractUserDetails)
      .map(storeSession(ticket, _))
      .map { case (id, session) => auditLog.logLogin(id, session, ticket) }
  }

  private def extractUserDetails(userDetails: UserDetails) =
    KayttooikeusUserDetails(
      userDetails.getRoles.asScala.map(a => Authority(a.replace("ROLE_", ""))).toSet,
      userDetails.getHenkiloOid
    )

  private def getSession(id: UUID): Either[Throwable, (UUID, Session)] =
    SessionDAO.get(id)
      .map(session => (id, session))
      .toRight(new AuthenticationFailedException(s"Session $id doesn't exist"))

  def getSession(ticket: Option[ServiceTicket], id: Option[UUID])(implicit request: HttpServletRequest): Either[Throwable, (UUID, Session)] = {
    logger.trace(s"Getting session with ticket $ticket and session id $id")
    (ticket, id) match {
      case (None, None) =>
        logger.trace("No session found")
        Left(new AuthenticationFailedException("No credentials given"))
      case (None, Some(i)) => getSession(i)
      case (Some(t), None) => createSession(t)
      case (Some(t), Some(i)) => getSession(i).left.flatMap {
        case e: AuthenticationFailedException =>
          logger.warn(s"Creating session after authentication failed exception: ${e.getMessage}")
          createSession(t)
        case e => Left(e)
      }
    }
  }

  def deleteSession(ticket: ServiceTicket): Boolean = SessionDAO.delete(ticket)
}
