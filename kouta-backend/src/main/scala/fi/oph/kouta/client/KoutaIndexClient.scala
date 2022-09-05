package fi.oph.kouta.client

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.json4s.ElasticJson4s.Implicits._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.domain.searchResults.KoulutusSearchResultFromIndex
import fi.oph.kouta.elasticsearch.ElasticsearchClient
import fi.oph.kouta.servlet.SearchParams
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.Serialization.write

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration

//TODO: Nimeä tämä jotenkin muuten kun refaktorointi valmis

class KoutaIndexClient(val client: ElasticClient) extends KoutaJsonFormats with Logging with ElasticsearchClient {

  private val DEFAULT_SOURCE_FIELDS = Set("oid", "nimi", "tila", "muokkaaja", "modified", "organisaatio")

  private val KOULUTUS_SOURCE_FIELDS = DEFAULT_SOURCE_FIELDS ++ Set(
    "julkinen",
    "koulutustyyppi",
    "metadata.eperuste",
    "toteutukset.oid",
    "toteutukset.tila",
    "toteutukset.modified",
    "toteutukset.nimi",
    "toteutukset.organisaatio",
    "toteutukset.organisaatiot"
  )

  private def getQueryFrom(page: Option[Int], size: Option[Int]) = {
    (page, size) match {
      case (Some(p), Some(s)) => if (p > 0) ((p - 1) * s) else 0
      case _                  => 0
    }
  }

  def searchKoulutukset(koulutusOids: Seq[KoulutusOid], params: SearchParams): KoulutusSearchResultFromIndex = {
    val baseQuery = termsQuery("oid.keyword", koulutusOids)

    val from = getQueryFrom(params.page, params.size)

    val req =
      search("koulutus-kouta")
        .source(write(KOULUTUS_SOURCE_FIELDS))
        .from(from)
        .size(params.size.getOrElse(0))
        .sortBy() // TODO
        .query(baseQuery)

    Await.result(searchElastic[KoulutusSearchItemFromIndex](req), Duration(5, TimeUnit.SECONDS))
  }

  def searchToteutukset(toteutusOids: Seq[ToteutusOid], params: Map[String, String]): ToteutusSearchResultFromIndex =
    ToteutusSearchResultFromIndex() // TODO

  def searchHaut(hakuOids: Seq[HakuOid], params: Map[String, String]): HakuSearchResultFromIndex =
    HakuSearchResultFromIndex() // TODO

  def searchHakukohteet(hakukohdeOids: Seq[HakukohdeOid], params: Map[String, String]): HakukohdeSearchResult =
    HakukohdeSearchResult()

  def searchValintaperusteet(valintaperusteIds: Seq[UUID], params: Map[String, String]): ValintaperusteSearchResult =
    ValintaperusteSearchResult()
}

object KoutaIndexClient extends KoutaIndexClient(ElasticsearchClient.client)
