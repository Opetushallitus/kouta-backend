package fi.oph.kouta.repository

import java.util.UUID
import java.util.concurrent.TimeUnit

import fi.oph.kouta.security.{Authority, CasSession, ServiceTicket, Session}
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

trait SessionDAO {
  def delete(ticket: ServiceTicket): Boolean
  def delete(id: UUID): Boolean
  def store(session: Session): UUID
  def store(session: CasSession, id: UUID): UUID
  def get(id: UUID): Option[Session]
}

object SessionDAO extends SessionDAO with SessionSQL {

  import KoutaDatabase.runBlocking

  override def store(session: Session): UUID = session match {
    case CasSession(ServiceTicket(ticket), personOid, authorities) =>
      val id = UUID.randomUUID()
      runBlocking(storeCasSession(id, ticket, personOid, authorities), timeout = Duration(1, TimeUnit.MINUTES))
      id
  }

  override def store(session: CasSession, id: UUID): UUID = {
    runBlocking(storeCasSession(id, session.casTicket.s, session.personOid, session.authorities), timeout = Duration(1, TimeUnit.MINUTES))
    id
  }

  override def delete(id: UUID): Boolean =
    runBlocking(deleteSession(id), timeout = Duration(10, TimeUnit.SECONDS))

  override def delete(ticket: ServiceTicket): Boolean =
    runBlocking(deleteSession(ticket), timeout = Duration(10, TimeUnit.SECONDS))

  override def get(id: UUID): Option[Session] = {
    runBlocking(
      getSession(id).flatMap {
        case None =>
          deleteSession(id).andThen(DBIO.successful(None))
        case Some(t) =>
          updateLastRead(id).andThen(DBIO.successful(Some(t)))
      }.transactionally, Duration(2, TimeUnit.SECONDS)
    ).map {
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
                                authorities: Set[Authority]) = {
    DBIO.seq(
      sqlu"""insert into sessions (id, cas_ticket, person) values ($id, $ticket, $personOid)""",
      DBIO.sequence(authorities.map(a => sqlu"""insert into authorities (session, authority) values ($id, ${a.authority})""").toSeq)
    )
  }

  protected def deleteSession(id: UUID) =
    sqlu"""delete from sessions where id = $id""".map(_ > 0)

  protected def deleteSession(ticket: ServiceTicket) =
    sqlu"""delete from sessions where cas_ticket = ${ticket.s}""".map(_ > 0)

  protected def getSession(id: UUID) =
    sql"""select cas_ticket, person from sessions
          where id = $id and last_read > now() - interval '60 minutes'"""
      .as[(Option[String], String)].map(_.headOption)

  protected def updateLastRead(id: UUID) =
    sqlu"""update sessions set last_read = now()
           where id = $id and last_read < now() - interval '30 minutes'"""

  protected def searchAuthoritiesBySession(sessionId: UUID) =
    sql"""select authority from authorities where session = $sessionId""".as[String]

}
