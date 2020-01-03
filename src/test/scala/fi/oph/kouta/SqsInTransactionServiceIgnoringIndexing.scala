package fi.oph.kouta

import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{IndexType, Priority}
import fi.oph.kouta.repository.KoutaDatabase
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

object SqsInTransactionServiceIgnoringIndexing extends SqsInTransactionService {
  override def runActionAndUpdateIndex[R](priority: Priority,
                                          index: IndexType,
                                          action: () => DBIO[R],
                                          getIndexableValue: R => String,
                                          auditLog: R => DBIO[_]): R =
    KoutaDatabase.runBlockingTransactionally(
      for {
        result <- action()
        _      <- auditLog(result)
      } yield result).get
}
