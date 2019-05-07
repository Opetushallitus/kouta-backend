package fi.oph.kouta.security

import java.util.UUID
import java.util.concurrent.TimeUnit

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.repository.SessionDAO
import fi.vm.sade.utils.cas.CasClient.Username
import fi.vm.sade.utils.slf4j.Logging
import scalaz.concurrent.Task

import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

case class AuthenticationFailedException(msg: String, cause: Throwable) extends RuntimeException(msg, cause) {
  def this(msg: String) = this(msg, null)
  def this() = this(null, null)
}

case class KayttooikeusUserDetails(authorities: Set[Authority], oid: String)

object CasSessionService extends CasSessionService(ProductionSecurityContext(KoutaConfigurationFactory.configuration.securityConfiguration),
                                                   KayttooikeusUserDetailsService)

abstract class CasSessionService(val securityContext: SecurityContext , val userDetailsService: KayttooikeusUserDetailsService) extends Logging {
  logger.info(s"Using security context ${securityContext.getClass.getSimpleName}")

  val serviceIdentifier: String = securityContext.casServiceIdentifier
  val casUrl: String = securityContext.casUrl

  private val casClient = securityContext.casClient

  private def validateServiceTicket(ticket: ServiceTicket): Either[Throwable, Username] = {
    val ServiceTicket(s) = ticket
    casClient.validateServiceTicket(securityContext.casServiceIdentifier)(s).handleWith {
      case NonFatal(t) =>
        logger.debug("Ticket validation error", t)
        Task.fail(new AuthenticationFailedException(s"Failed to validate service ticket $s", t))
    }.attemptRunFor(Duration(1, TimeUnit.SECONDS)).toEither
  }

  private def storeSession(ticket: ServiceTicket, user: KayttooikeusUserDetails): (UUID, CasSession) = {
    val session = CasSession(ticket, user.oid, user.authorities)
    logger.debug(s"Storing to session: ${session.casTicket} ${session.personOid} ${session.authorities}")
    val id = SessionDAO.store(session)
    (id, session)
  }

  private def createSession(ticket: ServiceTicket): Either[Throwable, (UUID, CasSession)] = {
    validateServiceTicket(ticket)
      .flatMap(userDetailsService.getUserByUsername)
      .map(storeSession(ticket, _))
  }

  private def getSession(id: UUID): Either[Throwable, (UUID, Session)] =
    SessionDAO.get(id)
      .map(session => (id, session))
      .toRight(new AuthenticationFailedException(s"Session $id doesn't exist"))

  def getSession(ticket: Option[ServiceTicket], id: Option[UUID]): Either[Throwable, (UUID, Session)] = {
    logger.trace(s"Getting session with ticket $ticket and session id $id")
    (ticket, id) match {
      case (None, None) =>
        logger.trace("No session found")
        Left(new AuthenticationFailedException("No credentials given"))
      case (None, Some(i)) => getSession(i)
      case (Some(t), None) => createSession(t)
      case (Some(t), Some(i)) => getSession(i).left.flatMap {
        case _: AuthenticationFailedException => createSession(t)
        case e => Left(e)
      }
    }
  }

  def deleteSession(ticket: ServiceTicket): Boolean = SessionDAO.delete(ticket)
}
