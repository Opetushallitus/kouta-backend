package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.util.GenericKoutaJsonFormats
import fi.vm.sade.utils.slf4j.Logging
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.concurrent.duration._

trait CachedOrganisaatioHierarkiaClient extends HttpClient with GenericKoutaJsonFormats with Logging {
  val organisaatioUrl: String

  implicit val wholeHierarkiaCache: Cache[String, OrganisaatioResponse] = Scaffeine()
    .expireAfterWrite(2.hours)
    .build()

  logger.info("Filling up Organisaatiohierarkiacache...")
  getWholeOrganisaatioHierarkiaCached()
  logger.info("Organisaatiohierarkiacache full")


  private def getWholeOrganisaatioHierarkia(): OrganisaatioResponse = {
    get(organisaatioUrl, followRedirects = true) { response =>
      parse(response).extract[OrganisaatioResponse]
    }
  }

  //TODO: organisaatiohierarkiakutsu, ladataanko heti kun palvelu käynnistetään. Tutkittava!
  def getWholeOrganisaatioHierarkiaCached(): OrganisaatioResponse = {
    wholeHierarkiaCache.get("ALL", _ => getWholeOrganisaatioHierarkia())
  }
}

case class OrganisaatioResponse(numHits: Int, organisaatiot: List[OidAndChildren])

case class OidAndChildren(oid: OrganisaatioOid,
                          children: List[OidAndChildren],
                          parentOidPath: String,
                          oppilaitostyyppi: Option[String],
                          status: String,
                          organisaatiotyypit: List[String] = List()) {

  def isPassiivinen: Boolean = status.equalsIgnoreCase("PASSIIVINEN")

  def isOppilaitos: Boolean = organisaatiotyypit.contains("organisaatiotyyppi_02")
}
