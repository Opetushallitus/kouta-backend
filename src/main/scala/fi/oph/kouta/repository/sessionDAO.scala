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
  import KoutaDatabase.runBlocking

  override def store(session: Session): UUID = session match {
    case CasSession(ServiceTicket(ticket), personOid, roles) =>
      val id = UUID.randomUUID()
      runBlocking(storeCasSession(id, ticket, personOid, roles), timeout = Duration(1, TimeUnit.MINUTES))
      id
  }

  def store(session: CasSession, id: UUID) =
    runBlocking(storeCasSession(id, session.casTicket.s, session.personOid, session.roles), timeout = Duration(1, TimeUnit.MINUTES))


  override def delete(id: UUID): Unit = {
    runBlocking(deleteSession(id), timeout = Duration(10, TimeUnit.SECONDS))
  }

  override def delete(ticket: ServiceTicket): Unit = {
    runBlocking(deleteSession(ticket), timeout = Duration(10, TimeUnit.SECONDS))
  }

  override def get(id: UUID): Option[Session] = {
    runBlocking(getSession(id), timeout = Duration(2, TimeUnit.SECONDS)).map {
      case (casTicket, personOid) =>
        val roolit = runBlocking(searchRoolitBySessio(id), Duration(2, TimeUnit.SECONDS))
        CasSession(ServiceTicket(casTicket.get), personOid, roolit.map(Role(_)).toSet)
    }
  }

}

sealed trait SessionSQL extends SQLHelpers {

  protected def storeCasSession(
      id: UUID,
      ticket: String,
      personOid: String,
      roles: Set[Role]
  ): DBIOAction[Unit, NoStream, Effect] = {
    DBIO.seq(
      sqlu"""insert into sessiot (id, cas_tiketti, henkilo) values ($id, $ticket, $personOid)""",
      DBIO.sequence(roles.map(role => sqlu"""insert into roolit (sessio, rooli) values ($id, ${role.name})""").toSeq)
    )
  }

  protected def deleteSession(id: UUID)              = sqlu"""delete from sessiot where id = $id"""
  protected def deleteSession(ticket: ServiceTicket) = sqlu"""delete from sessiot where cas_tiketti = ${ticket.s}"""

  protected def getSession(id: UUID) =
    getSessionQuery(id)
      .map(_.headOption)
      .flatMap {
        case None =>
          deleteSession(id).andThen(DBIO.successful(None))
        case Some(t) =>
          updateViimeksiLuettu(id).andThen(DBIO.successful(Some(t)))
      }
      .transactionally

  private def getSessionQuery(id: UUID) =
    sql"""select cas_tiketti, henkilo from sessiot
          where id = $id and viimeksi_luettu > now() - interval '60 minutes'"""
      .as[(Option[String], String)]

  private def updateViimeksiLuettu(id: UUID) =
    sqlu"""update sessiot set viimeksi_luettu = now()
           where id = $id and viimeksi_luettu < now() - interval '30 minutes'"""

  protected def searchRoolitBySessio(sessioId: UUID) =
    sql"""select rooli from roolit where sessio = $sessioId""".as[String]

}
