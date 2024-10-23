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

case class SessionDbResult(casTicket: String,
                           person: String,
                           authorities: List[String])

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
    runBlockingTransactionally(deleteSession(id), timeout = Duration(15, TimeUnit.SECONDS), ReadCommitted).get

  override def delete(ticket: ServiceTicket): Boolean =
    runBlockingTransactionally(deleteSession(ticket), timeout = Duration(15, TimeUnit.SECONDS), ReadCommitted).get

  override def get(id: UUID): Option[Session] = {
    runBlocking(getSessionInfo(id), timeout = Duration(15, TimeUnit.SECONDS))
      .map(result => CasSession(ServiceTicket(result.casTicket), result.person, result.authorities.map(Authority(_)).toSet))
  }
}

sealed trait SessionSQL extends SQLHelpers with SessionExtractors {

  protected def storeCasSession(id: UUID,
                                ticket: String,
                                personOid: String,
                                authorities: Set[Authority]): DBIO[_] = {
    DBIO.seq(
      sqlu"""insert into sessions (id, cas_ticket, person) values ($id, $ticket, $personOid)""",
      DBIO.sequence(authorities.map(a => sqlu"""insert into authorities (session, authority) values ($id, ${a.authority})""").toSeq))
  }

  protected def deleteSession(id: UUID): DBIO[Boolean] =
    sqlu"""delete from sessions where id = $id""".map(_ > 0)

  protected def deleteSession(ticket: ServiceTicket): DBIO[Boolean] =
    sqlu"""delete from sessions where cas_ticket = ${ticket.s}""".map(_ > 0)

  protected def getSessionInfo(id: UUID): DBIO[Option[SessionDbResult]] =
    sql"""select s.cas_ticket,
           s.person,
           array_agg(a.authority)
          from sessions s
                   left join public.authorities a on s.id = a.session
          where s.id = $id
            and s.created > now() - interval '60 minutes'
          group by s.cas_ticket, s.person"""
      .as[SessionDbResult].headOption
}