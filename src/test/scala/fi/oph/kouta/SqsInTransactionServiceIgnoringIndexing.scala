package fi.oph.kouta

import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{IndexType, Priority}
import fi.oph.kouta.repository.KoutaDatabase
import slick.dbio.DBIO

object SqsInTransactionServiceIgnoringIndexing extends SqsInTransactionService {
  override def runActionAndUpdateIndex[R](priority: Priority,
                                          index: IndexType,
                                          action: () => DBIO[R],
                                          getIndexableValue: R => String): R =
    KoutaDatabase.runBlockingTransactionally(action()).get
}
