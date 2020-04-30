package fi.oph.kouta.repository

import java.util.UUID
import java.util.concurrent.TimeUnit

import fi.oph.kouta.security.{Authority, CasSession, ServiceTicket, Session}
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.TransactionIsolation.ReadCommitted

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

trait SessionDAO {
  def delete(ticket: ServiceTicket): Boolean
  def delete(id: UUID): Boolean
  def store(session: CasSession): UUID
  def store(session: CasSession, id: UUID): UUID
  def get(id: UUID): Option[Session]
}

object SessionDAO extends SessionDAO with SessionSQL {

  import KoutaDatabase.{runBlocking, runBlockingTransactionally}

  override def store(session: CasSession): UUID = {
    val CasSession(ServiceTicket(ticket), personOid, authorities) = session
    val id = UUID.randomUUID()
    runBlockingTransactionally(storeCasSession(id, ticket, personOid, authorities), timeout = Duration(1, TimeUnit.MINUTES), ReadCommitted)
      .map(_ => id).get
  }

  override def store(session: CasSession, id: UUID): UUID =
    runBlockingTransactionally(storeCasSession(id, session.casTicket.s, session.personOid, session.authorities), timeout = Duration(1, TimeUnit.MINUTES), ReadCommitted)
      .map(_ => id).get

  override def delete(id: UUID): Boolean =
    runBlockingTransactionally(deleteSession(id), timeout = Duration(10, TimeUnit.SECONDS), ReadCommitted).get

  override def delete(ticket: ServiceTicket): Boolean =
    runBlockingTransactionally(deleteSession(ticket), timeout = Duration(10, TimeUnit.SECONDS), ReadCommitted).get

  override def get(id: UUID): Option[Session] = {
    runBlockingTransactionally(getSession(id), timeout = Duration(2, TimeUnit.SECONDS), ReadCommitted).get.map {
      case (casTicket, personOid) =>
        val authorities = runBlocking(searchAuthoritiesBySession(id), Duration(2, TimeUnit.SECONDS))
        CasSession(ServiceTicket(casTicket.get), personOid, authorities.map(Authority(_)).toSet)
    }
  }
}

sealed trait SessionSQL extends SQLHelpers {

  protected def storeCasSession(id: UUID,
                                ticket: String,
                                personOid: String,
                                authorities: Set[Authority]): DBIO[_] = {
    DBIO.seq(
      sqlu"""insert into sessions (id, cas_ticket, person) values ($id, $ticket, $personOid)""",
      DBIO.sequence(authorities.map(a => sqlu"""insert into authorities (session, authority) values ($id, ${a.authority})""").toSeq)
    )
  }

  protected def deleteSession(id: UUID): DBIO[Boolean] =
    sqlu"""delete from sessions where id = $id""".map(_ > 0)

  protected def deleteSession(ticket: ServiceTicket): DBIO[Boolean] =
    sqlu"""delete from sessions where cas_ticket = ${ticket.s}""".map(_ > 0)

  protected def getSession(id: UUID): DBIO[Option[(Option[String], String)]] =
    getSessionQuery(id)
      .flatMap {
        case None =>
          deleteSession(id).map(_ => None)
        case Some((ticket, person, needs_update)) if needs_update =>
          updateLastRead(id).map {
            case n if n > 0 => Some((ticket, person))
            case _          => None
          }
        case Some((ticket, person, _)) =>
          DBIO.successful(Some((ticket, person)))
      }

  private def getSessionQuery(id: UUID): DBIO[Option[(Option[String], String, Boolean)]] =
    sql"""select cas_ticket, person, last_read < now() - interval '30 minutes' from sessions
          where id = $id and last_read > now() - interval '60 minutes'"""
      .as[(Option[String], String, Boolean)].headOption

  private def updateLastRead(id: UUID): DBIO[Int] =
    sqlu"""update sessions set last_read = now() where id = $id"""

  protected def searchAuthoritiesBySession(sessionId: UUID): DBIO[Vector[String]] =
    sql"""select authority from authorities where session = $sessionId""".as[String]

}
