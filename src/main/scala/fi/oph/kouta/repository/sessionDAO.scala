package fi.oph.kouta.repository

import java.util.UUID
import java.util.concurrent.TimeUnit

import fi.oph.kouta.security.{CasSession, Role, ServiceTicket, Session}
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

trait SessionDAO {
  def delete(ticket: ServiceTicket): Unit

  def delete(id: UUID): Unit

  def store(session: Session): UUID

  def get(id: UUID): Option[Session]
}

object SessionDAO extends SessionDAO with SessionSQL {

  import KoutaDatabase.{runBlocking, runBlockingTransactionally}

  override def store(session: Session): UUID = session match {
    case CasSession(ServiceTicket(ticket), personOid, roles) =>
      val id = UUID.randomUUID()
      runBlockingTransactionally(storeCasSession(id, ticket, personOid, roles), timeout = Duration(1, TimeUnit.MINUTES))
      id
  }

  def store(session: CasSession, id: UUID): Unit =
    runBlockingTransactionally(storeCasSession(id, session.casTicket.s, session.personOid, session.roles), timeout = Duration(1, TimeUnit.MINUTES))


  override def delete(id: UUID): Unit = {
    runBlockingTransactionally(deleteSession(id), timeout = Duration(10, TimeUnit.SECONDS))
  }

  override def delete(ticket: ServiceTicket): Unit = {
    runBlockingTransactionally(deleteSession(ticket), timeout = Duration(10, TimeUnit.SECONDS))
  }

  override def get(id: UUID): Option[Session] = {
    runBlocking(getSession(id), timeout = Duration(2, TimeUnit.SECONDS)).map {
      case (casTicket, personOid) =>
        val roles = runBlocking(searchRolesBySessio(id), Duration(2, TimeUnit.SECONDS))
        CasSession(ServiceTicket(casTicket.get), personOid, roles.map(Role(_)).toSet)
    }
  }

}

sealed trait SessionSQL extends SQLHelpers {

  protected def storeCasSession(id: UUID,
                                ticket: String,
                                personOid: String,
                                roles: Set[Role]) = {
    DBIO.seq(
      sqlu"""insert into sessions (id, cas_ticket, person) values ($id, $ticket, $personOid)""",
      DBIO.sequence(roles.map(role => sqlu"""insert into roles (session, role) values ($id, ${role.name})""").toSeq)
    )
  }

  protected def deleteSession(id: UUID) =
    sqlu"""delete from sessions where id = $id"""

  protected def deleteSession(ticket: ServiceTicket) =
    sqlu"""delete from sessions where cas_ticket = ${ticket.s}"""

  protected def getSession(id: UUID) =
    getSessionQuery(id)
      .map(_.headOption)
      .flatMap {
        case None =>
          deleteSession(id).andThen(DBIO.successful(None))
        case Some(t) =>
          updateViimeksiLuettu(id).andThen(DBIO.successful(Some(t)))
      }

  private def getSessionQuery(id: UUID) =
    sql"""select cas_ticket, person from sessions
          where id = $id and last_read > now() - interval '60 minutes'"""
      .as[(Option[String], String)]

  private def updateViimeksiLuettu(id: UUID) =
    sqlu"""update sessions set last_read = now()
           where id = $id and last_read < now() - interval '30 minutes'"""

  protected def searchRolesBySessio(sessioId: UUID) =
    sql"""select role from roles where session = $sessioId""".as[String]

}
