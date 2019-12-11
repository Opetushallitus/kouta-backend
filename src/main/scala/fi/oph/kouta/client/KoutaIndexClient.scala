package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.KoulutusSearchResult
import fi.oph.kouta.domain.oid.KoulutusOid
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods.parse

object KoutaIndexClient extends KoutaIndexClient

trait KoutaIndexClient extends HttpClient with Logging with KoutaJsonFormats {

  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  def searchKoulutukset(koulutusOids: Seq[KoulutusOid], params: Map[String, String]): KoulutusSearchResult = {
    val queryParams = (params + ("oids" -> koulutusOids.map(_.s).sorted.mkString(",")))
    val url = urlProperties.url("kouta-index.koulutus.filtered-list", toQueryParams(queryParams.toSeq:_*))
      get(url, followRedirects = true) { response =>
        parse(response).extract[KoulutusSearchResult]
      }
  }
}