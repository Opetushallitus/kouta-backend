package fi.oph.kouta.client

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.json4s.ElasticJson4s.Implicits._
import com.sksamuel.elastic4s.requests.searches.SearchRequest
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder
import com.sksamuel.elastic4s.{ElasticClient, HitReader}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.elasticsearch.ElasticsearchClient
import fi.oph.kouta.servlet.SearchParams
import fi.oph.kouta.util.KoutaJsonFormats
import fi.oph.kouta.util.MiscUtils.withoutKoodiVersion
import fi.vm.sade.utils.slf4j.Logging

import java.util.UUID
import scala.reflect.ClassTag
import scala.util.Try

class KoutaSearchClient(val client: ElasticClient) extends KoutaJsonFormats with Logging with ElasticsearchClient {

  private def getQueryFrom(page: Int, size: Int) = {
    if (page > 0) ((page - 1) * size) else 0
  }

  private def getSortFieldKeyword(lng: Kieli, field: String) = getFieldKeyword(true, lng, field)

  private def getSearchFieldKeyword(lng: Kieli, field: String) = getFieldKeyword(false, lng, field)

  private def getFieldKeyword(forSort: Boolean, lng: Kieli, field: String): String = {
    field.toLowerCase.trim match {
      case "nimi"           => s"nimi.${lng}.keyword"
      case "tila"           => "tila.keyword"
      case "muokkaaja"      => "muokkaaja.nimi.keyword"
      case "hakutapa"       => if (forSort) s"hakutapa.nimi.${lng}.keyword" else s"hakutapa.koodiUri.keyword"
      case "hakuOid"        => "hakuOid.keyword"
      case "toteutusOid"    => "toteutusOid.keyword"
      case "orgWhitelist"   => "organisaatio.oid.keyword"
      case "modified"       => "modified"
      case "koulutustyyppi" => "koulutustyyppi.keyword"
      case "julkinen"       => "julkinen"
      case "koulutuksenAlkamiskausi" =>
        if (forSort) s"metadata.koulutuksenAlkamiskausi.koulutuksenAlkamiskausi.nimi.${lng}.keyword"
        else s"metadata.koulutuksenAlkamiskausi.koulutuksenAlkamiskausi.koodiUri.keyword"
      case "koulutuksenAlkamisvuosi" => "metadata.koulutuksenAlkamiskausi.koulutuksenAlkamisvuosi.keyword"
      case _                         => field
    }
  }

  private def getFieldSort(field: String, order: String, lng: Kieli) =
    fieldSort(getSortFieldKeyword(lng, field))
      .order(if (order == "desc") SortOrder.Desc else SortOrder.Asc)
      .unmappedType("string")

  private def withSorts(req: SearchRequest, params: SearchParams) = {
    params.orderBy
      .flatMap(orderBy => {
        val lng           = params.lng
        val baseFieldSort = getFieldSort(orderBy, params.order, lng)
        Some(req).map {
          if (orderBy == "nimi") {
            _.sortBy(baseFieldSort, getFieldSort("modified", "asc", lng))
          } else {
            _.sortBy(baseFieldSort, getFieldSort("nimi", "asc", lng))
          }
        }
      })
      .getOrElse(req)
  }

  private def isOid(s: String)  = GenericOid(s).isValid

  private def isUUID(s: String) = Try(UUID.fromString(s)).isSuccess

  private def getFilterQueries(params: SearchParams) = {
    val lng = params.lng
    val nimiFilter = params.nimi.map(searchTerm => {
      if (isOid(searchTerm)) {
        termQuery("oid.keyword", searchTerm)
      } else if (isUUID(searchTerm)) {
        termQuery("id.keyword", searchTerm)
      } else {
        bool(
          mustQueries = Seq(),
          shouldQueries = Seq(
            should(Kieli.values.map(l => matchQuery(s"nimi.${l.toString}", searchTerm))),
            should(
              Kieli.values.map(l => wildcardQuery(s"nimi.${l.toString}.keyword", s"*${searchTerm}*"))
            )
          ),
          notQueries = Seq()
        )
      }
    })

    val muokkaajaFilter = params.muokkaaja.map(muokkaaja => {
      if (isOid(muokkaaja)) {
        termQuery("muokkaaja.oid", muokkaaja)
      } else {
        matchQuery("muokkaaja.nimi", muokkaaja)
      }
    })

    val koulutustyyppiFilter = Option(params.koulutustyyppi)
      .filter(_.nonEmpty)
      .map(x => termsQuery(getSearchFieldKeyword(lng, "koulutustyyppi"), x.map(_.toString)))
    val tilaFilter =
      Option(params.tila).filter(_.nonEmpty).map(x => termsQuery(getSearchFieldKeyword(lng, "tila"), x.map(_.toString)))
    val julkinenFilter = params.julkinen.map(termQuery(getSearchFieldKeyword(lng, "julkinen"), _))
    val hakutapaFilter = Option(params.hakutapa)
      .filter(_.nonEmpty)
      .map(h => termsQuery(getSearchFieldKeyword(lng, "hakutapa"), h.map(withoutKoodiVersion(_))))
    val hakuOidFilter     = params.hakuOid.map(termQuery(getSearchFieldKeyword(lng, "hakuOid"), _))
    val toteutusOidFilter = params.toteutusOid.map(termQuery(getSearchFieldKeyword(lng, "toteutusOid"), _))
    val orgWhitelistFilter =
      Option(params.orgWhitelist)
        .filter(_.nonEmpty)
        .map(x => termsQuery(getSearchFieldKeyword(lng, "orgWhitelist"), x.map(_.toString)))
    val koulutuksenAlkamiskausiFilter = Option(params.koulutuksenAlkamisvuosi)
      .filter(_.nonEmpty)
      .map(termsQuery(getSearchFieldKeyword(lng, "koulutuksenAlkamiskausi"), _))
    val koulutuksenAlkamisvuosiFilter = Option(params.koulutuksenAlkamiskausi)
      .filter(_.nonEmpty)
      .map(termsQuery(getSearchFieldKeyword(lng, "koulutuksenAlkamisvuosi"), _))

    List(
      nimiFilter,
      koulutustyyppiFilter,
      tilaFilter,
      muokkaajaFilter,
      julkinenFilter,
      hakutapaFilter,
      hakuOidFilter,
      toteutusOidFilter,
      orgWhitelistFilter,
      koulutuksenAlkamiskausiFilter,
      koulutuksenAlkamisvuosiFilter
    ).flatten
  }

  def searchEntities[TSearchItem: HitReader: ClassTag](
      index: String,
      oids: Seq[String],
      params: SearchParams,
      idKey: String = "oid"
  ): SearchResult[TSearchItem] = {
    val baseQuery = termsQuery(s"${idKey}.keyword", oids)

    val from          = getQueryFrom(params.page, params.size)
    val filterQueries = getFilterQueries(params)

    val req =
      Some(search(index).from(from).size(params.size))
        .map(r => {
          if (filterQueries.isEmpty) {
            r.query(baseQuery)
          } else {
            r.query(must(baseQuery).filter(filterQueries))
          }
        })
        .map(withSorts(_, params))
        .get

    searchElastic[TSearchItem](req)
  }

  def searchKoulutukset(koulutusOids: Seq[KoulutusOid], params: SearchParams) = {
    searchEntities[KoulutusSearchItemFromIndex]("koulutus-kouta", koulutusOids.map(_.toString), params)
  }

  def searchToteutukset(toteutusOids: Seq[ToteutusOid], params: SearchParams) =
    searchEntities[ToteutusSearchItemFromIndex]("toteutus-kouta", toteutusOids.map(_.toString), params)

  def searchHaut(hakuOids: Seq[HakuOid], params: SearchParams) =
    searchEntities[HakuSearchItemFromIndex]("haku-kouta", hakuOids.map(_.toString), params)

  def searchHakukohteet(hakukohdeOids: Seq[HakukohdeOid], params: SearchParams) =
    searchEntities[HakukohdeSearchItem]("hakukohde-kouta", hakukohdeOids.map(_.toString), params)

  def searchValintaperusteet(valintaperusteIds: Seq[UUID], params: SearchParams) =
    searchEntities[ValintaperusteSearchItem](
      "valintaperuste-kouta",
      valintaperusteIds.map(_.toString),
      params,
      idKey = "id"
    )
}

object KoutaSearchClient extends KoutaSearchClient(ElasticsearchClient.client)