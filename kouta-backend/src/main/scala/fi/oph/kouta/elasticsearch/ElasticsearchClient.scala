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

trait ElasticsearchClient { this: KoutaJsonFormats with Logging =>
  val client: ElasticClient

  private def mapResponseToSearchResult[T: HitReader](response: RequestSuccess[SearchResponse]) = {
    SearchResult[T](
      totalCount = response.result.totalHits,
      result = response.result.hits.hits.flatMap(_.safeTo[T].toOption)
    )
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
          Future.successful(mapResponseToSearchResult[T](response))
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
