package fi.oph.kouta.repository

import java.time.Instant

import fi.oph.kouta.domain.ListEverything
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

trait ModificationDAO {
  def listModifiedSince(modifiedSince:Instant): ListEverything
}

object ModificationDAO extends ModificationDAO {

  def listModifiedSince(modifiedSince: Instant): ListEverything = KoutaDatabase.runBlockingTransactionally( for {
    k <- KoulutusDAO.selectModifiedSince(modifiedSince)
    t <- ToteutusDAO.selectModifiedSince(modifiedSince)
    h <- HakuDAO.selectModifiedSince(modifiedSince)
    a <- HakukohdeDAO.selectModifiedSince(modifiedSince)
    p <- ValintaperusteDAO.selectModifiedSince(modifiedSince)
  } yield (k, t, h, a, p)) match {
    case Left(t) => throw t
    case Right((k, t, h, a, p)) => ListEverything(k, t, h, a, p)
  }
}

trait EntityModificationDAO[T] {

  def getLastModified(id:T): Option[Instant] =
    KoutaDatabase.runBlocking(selectLastModified(id))

  def listModifiedSince(since:Instant):Seq[T] =
    KoutaDatabase.runBlocking(selectModifiedSince(since))

  def selectLastModified(id:T):DBIO[Option[Instant]]
  def selectModifiedSince(since:Instant): DBIO[Seq[T]]
}