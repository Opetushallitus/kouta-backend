package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.Kielistetty
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

  def getOrganisaatioHierarkiaWithOid(oid: OrganisaatioOid): OrganisaatioHierarkia = memoizeSync[OrganisaatioHierarkia](Some(45.minutes)) {
    val url = urlProperties.url(
      s"organisaatio-service.organisaatio.hierarkia.with.oid",
      toQueryParams(
        "oid" -> oid.toString,
        "aktiiviset" -> "true",
        "suunnitellut" -> "true",
        "lakkautetut" -> "false",
        "skipParents" -> "true"))
    get(url, followRedirects = true) {
      response => {
        println(response)
        parse(response).extract[OrganisaatioHierarkia]
      }
    }
  }
}

case class OrgServiceOrganisaatio(
  oid: String,
  oppilaitostyyppi: Option[String] = None,
  nimi: Kielistetty,
  kotipaikkaUri: String,
  children: List[OrganisaationOsa],
  status: Option[String] = None,
  organisaatiotyypit: Option[List[String]] = None,
)

case class OrganisaationOsa(
  oid: String,
  nimi: Kielistetty,
  kotipaikkaUri: String,
  children: List[OrganisaationOsa],
  status: String,
  organisaatiotyypit: List[String],
)

case class OrganisaatioHierarkia(
  organisaatiot: List[OrgServiceOrganisaatio]
)

