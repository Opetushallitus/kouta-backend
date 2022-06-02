package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.{Kielistetty, OrganisaatioHierarkia}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods.parse
import scalacache.caffeine.CaffeineCache
import scalacache.memoization.memoizeSync
import scalacache.modes.sync._

import scala.concurrent.duration._


object OrganisaatioServiceClient extends OrganisaatioServiceClient

class OrganisaatioServiceClient extends HttpClient with CallerId with Logging with KoutaJsonFormats {
  implicit val OrganisaatioHierarkiaCache = CaffeineCache[OrganisaatioHierarkia]
  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  def getOrganisaatioHierarkiaWithOids(oids: List[OrganisaatioOid]): OrganisaatioHierarkia = memoizeSync[OrganisaatioHierarkia](Some(45.minutes)) {
    val oidsAsQueryParams = oids.mkString("&oidRestrictionList=", "&oidRestrictionList=", "")
    val url = urlProperties.url(
      s"organisaatio-service.organisaatio.hierarkia.with.oids",
      toQueryParams(
        "aktiiviset" -> "true",
        "suunnitellut" -> "true",
        "lakkautetut" -> "false",
        "skipParents" -> "true")) + oidsAsQueryParams
    get(url, followRedirects = true) {
      response => {
        parse(response).extract[OrganisaatioHierarkia]
      }
    }
  }
}

