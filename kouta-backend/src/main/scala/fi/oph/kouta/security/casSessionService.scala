package fi.oph.kouta.security

import java.util.UUID
import java.util.concurrent.TimeUnit

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.KayttooikeusClient
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.repository.SessionDAO
import fi.vm.sade.utils.cas.CasClient.Username
import fi.vm.sade.utils.slf4j.Logging
import javax.servlet.http.HttpServletRequest
import scalaz.concurrent.Task

import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

case class AuthenticationFailedException(msg: String, cause: Throwable) extends RuntimeException(msg, cause) {
  def this(msg: String) = this(msg, null)
  def this() = this(null, null)
}

case class KayttooikeusUserDetails(authorities: Set[Authority], oid: String)

object CasSessionService extends CasSessionService(ProductionSecurityContext(KoutaConfigurationFactory.configuration.securityConfiguration),
                                                   KayttooikeusClient,
                                                   AuditLog)

abstract class CasSessionService(val securityContext: SecurityContext,
                                 val userDetailsService: KayttooikeusClient,
                                 auditLog: AuditLog) extends Logging {
  logger.info(s"Using security context ${securityContext.getClass.getSimpleName}")

  val serviceIdentifier: String = securityContext.casServiceIdentifier
  val casUrl: String = securityContext.casUrl

  private val casClient = securityContext.casClient

  private def validateServiceTicket(ticket: ServiceTicket): Either[Throwable, Username] = {
    val ServiceTicket(s) = ticket
    casClient.validateServiceTicketWithVirkailijaUsername(securityContext.casServiceIdentifier)(s).handleWith {
      case NonFatal(t) =>
        logger.debug("Ticket validation error", t)
        Task.fail(new AuthenticationFailedException(s"Failed to validate service ticket $s", t))
    }.unsafePerformSyncAttemptFor(Duration(15, TimeUnit.SECONDS)).toEither
  }

  private def storeSession(ticket: ServiceTicket, user: KayttooikeusUserDetails): (UUID, CasSession) = {
    val session = CasSession(ticket, user.oid, user.authorities)
    logger.debug(s"Storing to session: ${session.casTicket} ${session.personOid} ${session.authorities}")
    val id = SessionDAO.store(session)
    (id, session)
  }

  private def createSession(ticket: ServiceTicket)(implicit request: HttpServletRequest): Either[Throwable, (UUID, CasSession)] = {
    validateServiceTicket(ticket)
      .map(userDetailsService.getUserByUsername)
      .map(storeSession(ticket, _))
      .map { case (id, session) => auditLog.logLogin(id, session, ticket) }
  }

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
