package fi.oph.kouta.elasticsearch

import com.sksamuel.elastic4s.HitReader
import com.sksamuel.elastic4s.requests.searches.queries.Query
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.http.{JavaClient, NoOpHttpClientConfigCallback}
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.searches.{SearchRequest, SearchResponse}
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties, RequestFailure, RequestSuccess}
import fi.oph.kouta.domain.{HasTila, SearchResult, Tallennettu}
import fi.vm.sade.utils.Timer.timed
import fi.vm.sade.utils.slf4j.Logging
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.config.RequestConfig.Builder
import org.apache.http.impl.client.BasicCredentialsProvider
import fi.oph.kouta.util.KoutaJsonFormats
import fi.oph.kouta.config.{ElasticSearchConfiguration, KoutaConfigurationFactory}
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

// Kopioitu muutoksin kouta-internalista. Kakutus on poistettu toistaiseksi versioristiriitojen takia
// (scalacache riippuu caffeine v2:sta ja scaffeine riippuu caffeine v3:sta).
// Jos halutaan myöhemmin käyttää kouta-internalin kanssa samaa ElasticsearchClient:a, pitää kakutusongelma ratkaista.
// iterativeElasticFetch:a ei ole nyt käytetty, koska kouta-index:n elastic-kutsuissa vastaavaa ei oltu tehty
// ja se se olisi pitänyt muuttaa myös palauttamaan totalHits.
trait ElasticsearchClient { this: KoutaJsonFormats with Logging =>
  val client: ElasticClient
  private val iterativeElasticFetch = new IterativeElasticFetch(client)

  def getItem[T <: HasTila: HitReader](index: String)(id: String): Future[T] = timed(s"GetItem from ElasticSearch (Id: ${id}", 100) {
    val request = get(index, id)
    logger.debug(s"Elasticsearch query: ${request.show}")
    client
      .execute(request)
      .flatMap {
        case failure: RequestFailure =>
          logger.debug(s"Elasticsearch status: {}", failure.status)
          Future.failed(ElasticSearchException(failure.error))
        case response: RequestSuccess[GetResponse] =>
          logger.debug(s"Elasticsearch status: {}", response.status)
          logger.debug(s"Elasticsearch response: {}", response.result.sourceAsString)
          handleSuccesfulReponse(index)(id, response)
      }
      .flatMap {
        case None =>
          Future.failed(new NoSuchElementException(s"Didn't find id $id from index $index"))
        case Some(t) if t.tila == Tallennettu =>
          Future.failed(new NoSuchElementException(s"Entity with id $id from index $index was in tila luonnos"))
        case Some(t) =>
          Future.successful(t)
      }
  }

  private def handleSuccesfulReponse[T <: HasTila: HitReader](index: String)(id: String, response: RequestSuccess[GetResponse]) = {
    response.status match {
      case 404 => Future.successful(None)
      case _   => mapResultToEntity(index)(id, response)
    }
  }


  private def mapResultToEntity[T <: HasTila: HitReader](index: String)(id: String, response: RequestSuccess[GetResponse]) = {
    response.result.safeTo[T] match {
      case Success(x) =>
        Future.successful(Option(x))
      case Failure(exception) =>
        logger.error(
          s"Unable to read response entity with id $id from index $index. Not going to serve coffee from teapot!",
          exception
        )
        Future.failed(
          TeapotException(
            s"Unable to read response entity with id $id from index $index. Not going to serve coffee from teapot!",
            exception
          )
        )
    }
  }

  private def mapResultToEntity[T: HitReader](response: RequestSuccess[SearchResponse]) = {
    val totalHits = response.result.totalHits

    SearchResult[T](
      totalCount = totalHits,
      result = response.result.hits.hits.flatMap(_.safeTo[T].toOption)
    )
  }

 /*
  def searchItems[T: HitReader: ClassTag](query: Option[Query]): Future[IndexedSeq[T]] = {
    timed(s"SearchItems from ElasticSearch (Query: ${query}", 100) {
      val notTallennettu = not(termsQuery("tila.keyword", "tallennettu"))

      query.fold[Future[IndexedSeq[T]]](
        executeScrollQuery(search(index).query(notTallennettu).keepAlive("1m").size(500))
      )(q => {
        val request = search(index).bool(must(notTallennettu, q)).keepAlive("1m").size(500)
        executeScrollQuery(request)
      })
    }
  }

  def searchItemBulks[T: HitReader: ClassTag](
      query: Option[Query],
      offset: Int,
      limit: Option[Int]
  ): Future[IndexedSeq[T]] = {
    timed(s"Search item bulks from ElasticSearch (Query: ${query}, offset: ${offset}, limit: ${limit})", 100) {
      val request = search(index).query(query.get).keepAlive("1m").size(500)
      executeScrollQuery[T](request)
    }
  }
  */

  def executeScrollQuery[T: HitReader](searchRequest: SearchRequest): Future[IndexedSeq[T]] = {
    implicit val duration: FiniteDuration = Duration(1, TimeUnit.MINUTES)
    logger.info(s"Elasticsearch request: ${searchRequest.show}")
      iterativeElasticFetch
        .fetch(searchRequest)
        .map(hit => hit.flatMap(_.safeTo[T].toOption))
        .mapTo[IndexedSeq[T]]
  }

  def searchElastic[T: HitReader: ClassTag](req: SearchRequest): SearchResult[T] = {
    implicit val duration: FiniteDuration = Duration(1, TimeUnit.MINUTES)

    logger.info(s"Elasticsearch request: ${req.show}")

    client
      .execute(req)
      .flatMap {
        case failure: RequestFailure =>
          logger.debug(s"Elasticsearch request failure: {}", failure.error)
          Future.failed(ElasticSearchException(failure.error))
        case response: RequestSuccess[SearchResponse] =>
          logger.debug(s"Elasticsearch status: {}", response.status)
          Future.successful(mapResultToEntity[T](response))
      }.await
  }
}

object ElasticsearchClient {
  val config: ElasticSearchConfiguration = KoutaConfigurationFactory.configuration.elasticSearchConfiguration;
  val httpClientConfigCallback: HttpClientConfigCallback = if (config.authEnabled) {
    lazy val provider = {
      val provider    = new BasicCredentialsProvider
      val credentials = new UsernamePasswordCredentials(config.username, config.password)
      provider.setCredentials(AuthScope.ANY, credentials)
      provider
    }
    (httpClientBuilder: HttpAsyncClientBuilder) => {
      httpClientBuilder.setDefaultCredentialsProvider(provider)
    }
  } else {
    NoOpHttpClientConfigCallback
  }
  val client: ElasticClient = ElasticClient(
    JavaClient(
      ElasticProperties(config.elasticUrl),
      (requestConfigBuilder: Builder) => {
        requestConfigBuilder
      },
      httpClientConfigCallback
    )
  )
}
