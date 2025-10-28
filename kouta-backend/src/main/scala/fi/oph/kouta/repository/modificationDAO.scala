package fi.oph.kouta.repository

import java.time.Instant
import java.util.ConcurrentModificationException
import fi.oph.kouta.domain.ListEverything
import fi.oph.kouta.servlet.EntityNotFoundException
import slick.dbio.DBIO
import slick.jdbc.TransactionIsolation.ReadCommitted

import scala.concurrent.ExecutionContext.Implicits.global

trait ModificationDAO {
  def listModifiedSince(modifiedSince:Instant): ListEverything
}

object ModificationDAO extends ModificationDAO {

  def listModifiedSince(modifiedSince: Instant): ListEverything = KoutaDatabase
    .runBlockingTransactionally(isolation = ReadCommitted)(for {
      k <- KoulutusDAO.selectModifiedSince(modifiedSince)
      t <- ToteutusDAO.selectModifiedSince(modifiedSince)
      h <- HakuDAO.selectModifiedSince(modifiedSince)
      a <- HakukohdeDAO.selectModifiedSince(modifiedSince)
      p <- ValintaperusteDAO.selectModifiedSince(modifiedSince)
      l <- OppilaitosDAO.selectModifiedSince(modifiedSince)
      o <- OppilaitoksenOsaDAO.selectModifiedSince(modifiedSince)
      s <- SorakuvausDAO.selectModifiedSince(modifiedSince)
    } yield ListEverything(k, t, h, a, p, o.union(l), s))
    .get
}

trait EntityModificationDAO[T] {
  def listModifiedSince(since: Instant): Seq[T] =
    KoutaDatabase.runBlocking(selectModifiedSince(since))

  def selectLastModified(id: T): DBIO[Option[Instant]]

  def selectModifiedSince(since: Instant): DBIO[Seq[T]]

  def checkNotModified(id: T, notModifiedSince: Instant): DBIO[Instant] =
    selectLastModified(id).flatMap {
      case None => DBIO.failed(EntityNotFoundException(s"Unknown oid/id ${id.toString}"))
      case Some(time) if time.isAfter(notModifiedSince) => DBIO.failed(
        new ConcurrentModificationException(s"Another user has modified ${id.toString} concurrently!"))
      case Some(time) => DBIO.successful(time)
  }
}
