package fi.oph.kouta.client

import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.util.GenericKoutaJsonFormats
import org.json4s._
import org.json4s.jackson.JsonMethods._
import scalacache.caffeine._
import scalacache.memoization.memoizeSync
import scalacache.modes.sync._

import scala.concurrent.duration._

trait CachedOrganisaatioHierarkiaClient extends HttpClient with GenericKoutaJsonFormats {

  val organisaatioUrl: String

  implicit val WholeHierarkiaCache = CaffeineCache[OrganisaatioResponse]

  def getWholeOrganisaatioHierarkiaCached(): OrganisaatioResponse = memoizeSync[OrganisaatioResponse](Some(45.minutes)) {
    get(organisaatioUrl, followRedirects = true) { response =>
      parse(response).extract[OrganisaatioResponse]
    }
  }
}

case class OrganisaatioResponse(numHits: Int, organisaatiot: List[OidAndChildren])

case class OidAndChildren(oid: OrganisaatioOid, children: List[OidAndChildren], parentOidPath: String, oppilaitostyyppi: Option[String], status: String) {
  def isPassiivinen: Boolean = status.equalsIgnoreCase("PASSIIVINEN")
}
