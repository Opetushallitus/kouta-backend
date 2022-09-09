package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.util.GenericKoutaJsonFormats
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.concurrent.duration._

trait CachedOrganisaatioHierarkiaClient extends HttpClient with GenericKoutaJsonFormats {
  val organisaatioUrl: String

  implicit val WholeHierarkiaCache: Cache[String, OrganisaatioResponse] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()

  private def getWholeOrganisaatioHierarkia(): OrganisaatioResponse = {
    get(organisaatioUrl, followRedirects = true) { response =>
      parse(response).extract[OrganisaatioResponse]
    }
  }

  def getWholeOrganisaatioHierarkiaCached(): OrganisaatioResponse = {
    WholeHierarkiaCache.get("ALL", _ => getWholeOrganisaatioHierarkia())
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
