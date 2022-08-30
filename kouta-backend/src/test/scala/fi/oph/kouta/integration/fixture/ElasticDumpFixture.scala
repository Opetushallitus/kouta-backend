package fi.oph.kouta.integration.fixture

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.count.CountResponse
import com.sksamuel.elastic4s.{RequestFailure, RequestSuccess}
import fi.oph.kouta.TempElasticClient
import fi.oph.kouta.util.CommandLine.runBlocking

trait ElasticDumpFixture {
  private def elasticEmpty(): Boolean = {
    TempElasticClient.client.execute(count("_all")).await match {
      case result: RequestSuccess[CountResponse] =>
        result.result.count == 0
      case failure: RequestFailure =>
        print(s"Count request failed: ${failure.toString}")
        true
    }
  }

  def initIndices(): Unit = {
    val elasticHost = TempElasticClient.url.replace("localhost", "host.docker.internal")

    println("Importing elasticsearch data...")
    if (elasticEmpty()) {
      runBlocking(s"src/test/resources/load_elastic_dump.sh $elasticHost data,mapping,alias,settings,template")
    } else {
      println("Elasticsearch not empty. Data already imported. Doing nothing.")
    }
  }
}
