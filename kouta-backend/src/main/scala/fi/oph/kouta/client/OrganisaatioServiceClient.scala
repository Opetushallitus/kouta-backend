package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.OrganisaatioHierarkia
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.duration._


object OrganisaatioServiceClient extends OrganisaatioServiceClient

class OrganisaatioServiceClient extends HttpClient with CallerId with Logging with KoutaJsonFormats {
  implicit val organisaatioHierarkiaCache: Cache[List[OrganisaatioOid], OrganisaatioHierarkia] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()
  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  private def getOrganisaatioHierarkiaWithOids(oids: List[OrganisaatioOid]): OrganisaatioHierarkia = {
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

  def getOrganisaatioHierarkiaWithOidsFromCache(oids: List[OrganisaatioOid]): OrganisaatioHierarkia = {
    organisaatioHierarkiaCache.get(oids, oids => getOrganisaatioHierarkiaWithOids(oids))
  }
}

