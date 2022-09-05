package fi.oph.kouta.elasticsearch

import fi.vm.sade.utils.slf4j.Logging
import com.sksamuel.elastic4s.ElasticDsl.{clearScroll}
import com.sksamuel.elastic4s.{ElasticClient, RequestFailure, RequestSuccess, Response}
import com.sksamuel.elastic4s.requests.searches.{SearchHit, SearchRequest, SearchResponse}
import com.sksamuel.elastic4s.ElasticDsl._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class IterativeElasticFetch(client: ElasticClient)(implicit val executor: ExecutionContext) extends Logging {

  def fetch(searchreq: SearchRequest): Future[IndexedSeq[SearchHit]] = fetchUntilDone(searchreq, IndexedSeq.empty)

  private def fetchUntilDone(searchreq: SearchRequest, scrollId: Seq[String]): Future[IndexedSeq[SearchHit]] = {
    def handleResult(resp: Response[SearchResponse]): Future[IndexedSeq[SearchHit]] = {
      def allWhileHasNext(i: Iterator[SearchHit]): IndexedSeq[SearchHit] = if (i.hasNext) {
        IndexedSeq(i.next()) ++ allWhileHasNext(i)
      } else {
        IndexedSeq.empty
      }

      resp match {
        case RequestSuccess(_, _, _, result) =>
          val hits: IndexedSeq[SearchHit] = allWhileHasNext(result.hits.hits.iterator)
          if (hits.nonEmpty) {
            fetchUntilDone(searchreq, scrollId ++ result.scrollId).map(hits ++ _)
          } else {
            client.execute(clearScroll(scrollId)).onComplete {
              case Success(_) =>
                logger.debug("Successfully cleared Elastic scroll indices")
              case Failure(exception) =>
                logger.error(s"Failed to clear Elastic scroll indices", exception)
            }
            Future.successful(hits)
          }

        case failure: RequestFailure =>
          Future.failed(new RuntimeException(s"Iterative Elastic search failed: ${failure.toString}"))
      }
    }
    scrollId.lastOption match {
      case Some(id) =>
        client
          .execute(searchScroll(id, searchreq.keepAlive.get))
          .flatMap(handleResult)
      case None =>
        client
          .execute(searchreq)
          .flatMap(handleResult)
    }
  }
}
