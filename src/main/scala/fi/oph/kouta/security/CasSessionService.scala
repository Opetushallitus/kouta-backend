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
import scala.util.{Failure, Success, Try}

class AuthenticationFailedException(msg: String, cause: Throwable) extends RuntimeException(msg, cause) {
  def this(msg: String) = this(msg, null)
  def this() = this(null, null)
}

class AuthorizationFailedException(msg: String, cause: Throwable) extends RuntimeException(msg, cause) {
  def this(msg: String) = this(msg, null)
  def this() = this(null, null)
}

object CasSessionService extends CasSessionService(ProductionSecurityContext(KoutaConfigurationFactory.configuration.casConfiguration))

abstract class CasSessionService(val securityContext: SecurityContext /*, userDetailsService: KayttooikeusUserDetailsService*/ ) extends Logging {
  logger.info(s"Using security context ${securityContext.getClass.getSimpleName}")

  val serviceIdentifier: String = securityContext.casServiceIdentifier
  val casUrl: String = securityContext.casUrl

  private val casClient = securityContext.casClient

  private def validateServiceTicket(ticket: ServiceTicket): Try[Username] = {
    val ServiceTicket(s) = ticket
    casClient.validateServiceTicket(securityContext.casServiceIdentifier)(s).handleWith {
      case NonFatal(t) => Task.fail(new AuthenticationFailedException(s"Failed to validate service ticket $s", t))
    }.unsafePerformSyncAttemptFor(Duration(1, TimeUnit.SECONDS)).toEither.toTry
  }

  private def storeSession(ticket: ServiceTicket, user: KayttooikeusUserDetails): Try[(UUID, CasSession)] = {
    val session = CasSession(ticket, user.oid, user.roles)
    logger.debug(s"Storing to session: ${session.casTicket} ${session.personOid} ${session.roles}")
    Try(SessionDAO.store(session)).map(id => (id, session))
  }

  private def createSession(ticket: ServiceTicket): Try[(UUID, CasSession)] = {
    validateServiceTicket(ticket)
      //.right.flatMap(userDetailsService.getUserByUsername)
      // TODO: authorization
      .map(KayttooikeusUserDetails(Role.all.values.toSet, _))
      .flatMap(storeSession(ticket, _))
  }

  private def getSession(id: UUID): Try[(UUID, Session)] = {
    Try(SessionDAO.get(id)) match {
      case Success(Some(session)) => Success((id, session))
      case Success(None) => Failure(new AuthenticationFailedException(s"Session $id doesn't exist"))
      case Failure(t) => Failure(t)
    }
  }

  def getSession(ticket: Option[ServiceTicket], id: Option[UUID]): Try[(UUID, Session)] = {
    logger.trace(s"Getting session with ticket $ticket and session id $id")
    (ticket, id) match {
      case (None, None) =>
        logger.trace(s"No session found")
        Failure(new AuthenticationFailedException(s"No credentials given"))
      case (None, Some(i)) => getSession(i)
      case (Some(t), None) => createSession(t)
      case (Some(t), Some(i)) => getSession(i).recoverWith {
        case _: AuthenticationFailedException => createSession(t)
      }
    }
  }

  def deleteSession(ticket: ServiceTicket): Try[Unit] = {
    Try(SessionDAO.delete(ticket))
  }
}
