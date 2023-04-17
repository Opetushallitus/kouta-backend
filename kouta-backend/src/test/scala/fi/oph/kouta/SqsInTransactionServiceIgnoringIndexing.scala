package fi.oph.kouta

import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{IndexType, Priority}
import slick.dbio.DBIO

object SqsInTransactionServiceIgnoringIndexing extends SqsInTransactionService {

  override def toSQSQueue(priority: Priority, values: Map[IndexType, Seq[String]]): Either[String, _] = Right()

}
