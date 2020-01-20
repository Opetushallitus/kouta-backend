package fi.oph.kouta.client

import java.util.UUID

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods.parse

import scala.reflect.Manifest

object KoutaIndexClient extends KoutaIndexClient

trait KoutaIndexClient extends HttpClient with Logging with KoutaJsonFormats {

  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  def searchKoulutukset(koulutusOids: Seq[KoulutusOid], params: Map[String, String]): KoulutusSearchResult =
    search[KoulutusSearchResult]("kouta-index.koulutus.filtered-list", paramsWithOids(koulutusOids, params))

  def searchToteutukset(toteutusOids: Seq[ToteutusOid], params: Map[String, String]): ToteutusSearchResult =
    search[ToteutusSearchResult]("kouta-index.toteutus.filtered-list", paramsWithOids(toteutusOids, params))

  def searchHaut(hakuOids: Seq[HakuOid], params: Map[String, String]): HakuSearchResult =
    search[HakuSearchResult]("kouta-index.haku.filtered-list", paramsWithOids(hakuOids, params))

  def searchHakukohteet(hakukohdeOids: Seq[HakukohdeOid], params: Map[String, String]): HakukohdeSearchResult =
    search[HakukohdeSearchResult]("kouta-index.hakukohde.filtered-list", paramsWithOids(hakukohdeOids, params))

  def searchValintaperusteet(valintaperusteIds: Seq[UUID], params: Map[String, String]): ValintaperusteSearchResult =
    search[ValintaperusteSearchResult]("kouta-index.valintaperuste.filtered-list", paramsWithIds(valintaperusteIds, params))

  private def search[T](urlKey: String, params: Map[String, String])(implicit mf: Manifest[T]): T = {
    val url = urlProperties.url(urlKey, toQueryParams(params.toSeq:_*))
    get(url, followRedirects = true) { response =>
      parse(response).extract[T]
    }
  }

  private def paramsWithOids(oids: Seq[Oid], params: Map[String, String]): Map[String, String] =
    params + ("oids" -> oids.map(_.s).sorted.mkString(","))

  private def paramsWithIds(ids: Seq[UUID], params: Map[String, String]): Map[String, String] =
    params + ("ids" -> ids.map(_.toString).sorted.mkString(","))
}