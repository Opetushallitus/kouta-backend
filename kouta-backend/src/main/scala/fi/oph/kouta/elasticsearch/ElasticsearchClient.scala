package fi.oph.kouta.elasticsearch

import com.sksamuel.elastic4s.{Hit, HitReader}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.http.{JavaClient, NoOpHttpClientConfigCallback}
import com.sksamuel.elastic4s.requests.searches.{SearchRequest, SearchResponse}
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties, RequestFailure, RequestSuccess}
import fi.oph.kouta.domain.{HasTila, SearchResult, Tallennettu}
import fi.oph.kouta.logging.Logging
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.client.config.RequestConfig.Builder
import org.apache.http.impl.client.BasicCredentialsProvider
import fi.oph.kouta.util.KoutaJsonFormats
import fi.oph.kouta.config.{ElasticSearchConfiguration, KoutaConfigurationFactory}
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback
import org.json4s.Formats
import org.json4s.jackson.JsonMethods.parse

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

trait ElasticsearchClient { this: KoutaJsonFormats with Logging =>
  val client: ElasticClient

  /**
   * Indeksidokumentit luetaan sallivammilla formaateilla kuin muu JSON, ks.
   * GenericKoutaFormats.indexedDocumentFormats. Ilman tätä kouta-indeksoijan kirjoittamat
   * dokumentit, joista esim. haun metadata.koulutuksenAlkamiskausi puuttuu tai on null, katoavat
   * hakutuloksista kesken sivutuksen.
   */
  private lazy val indexedFormats: Formats = indexedDocumentKoutaFormats

  implicit def koutaHitReader[T: Manifest]: HitReader[T] =
    (hit: Hit) =>
      Try(
        parse(hit.sourceAsString, indexedFormats.wantsBigDecimal, indexedFormats.wantsBigInt)
          .extract[T](indexedFormats, implicitly[Manifest[T]])
      )

  private def mapResponseToSearchResult[T: HitReader](response: RequestSuccess[SearchResponse]) =
    toSearchResult[T](response.result.totalHits, response.result.hits.hits)

  /**
   * Deserialisoinnissa epäonnistuneet dokumentit jäävät pois hakutuloksista, jolloin sivulla voi
   * näkyä vähemmän tuloksia kuin totalCount antaisi olettaa. Lokitetaan ne, jotta vika ei jää
   * huomaamatta. totalCount on Elasticsearchin ilmoittama koko tulosjoukon koko, eikä sitä korjata
   * pudotetuilla dokumenteilla - ero näiden välillä on juuri se signaali, joka halutaan nähdä.
   */
  private[elasticsearch] def toSearchResult[T: HitReader](totalCount: Long, hits: Seq[Hit]): SearchResult[T] = {
    val results  = hits.map(hit => (hit.id, hit.safeTo[T]))
    val failures = results.collect { case (id, Failure(e)) => (id, e) }

    // Ensimmäisestä virheestä stack trace, lopuista pelkkä viesti: jos indeksin rakenne on muuttunut,
    // jokainen hakupyyntö tuottaisi muuten sivun kokoisen määrän stack traceja.
    failures.headOption.foreach { case (id, e) =>
      logger.error(s"Elasticsearch-dokumentin $id deserialisointi epäonnistui, dokumentti jätetään pois hakutuloksista", e)
    }
    failures.drop(1).foreach { case (id, e) =>
      logger.warn(s"Elasticsearch-dokumentin $id deserialisointi epäonnistui: ${e.getMessage}")
    }
    if (failures.nonEmpty) {
      logger.error(s"${failures.length}/${results.length} hakutuloksen deserialisointi epäonnistui")
    }

    SearchResult[T](totalCount = totalCount, result = results.collect { case (_, Success(item)) => item })
  }

  def searchElastic[T: HitReader: ClassTag](req: SearchRequest): SearchResult[T] = {
    implicit val duration: FiniteDuration = Duration(1, TimeUnit.MINUTES)

    logger.info(s"Elasticsearch request: ${req.show}")

    client
      .execute(req)
      .flatMap {
        case failure: RequestFailure =>
          logger.error(s"Elasticsearch request failure: {}", failure.error)
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
