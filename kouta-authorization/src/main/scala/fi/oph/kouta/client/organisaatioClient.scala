package fi.oph.kouta.client

import fi.oph.kouta.domain.Koulutustyyppi
import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid}
import fi.oph.kouta.util.GenericKoutaJsonFormats
import org.json4s._
import org.json4s.jackson.JsonMethods._
import scalacache._
import scalacache.caffeine._
import scalacache.memoization.memoizeSync
import scalacache.modes.sync._

import scala.annotation.tailrec
import scala.concurrent.duration._

trait OrganisaatioService {
  type OrganisaatioOidsAndOppilaitostyypitFlat = (Seq[OrganisaatioOid], Seq[Koulutustyyppi])

  protected def cachedOrganisaatioHierarkiaClient: CachedOrganisaatioHierarkiaClient

  def getAllChildOidsFlat(oid: OrganisaatioOid, lakkautetut: Boolean = false): Seq[OrganisaatioOid] = oid match {
    case RootOrganisaatioOid => Seq(RootOrganisaatioOid)
    case _ => getHierarkia(oid, orgs => children(oid, orgs), lakkautetut)
  }

  def getAllChildOidsAndOppilaitostyypitFlat(oid: OrganisaatioOid): OrganisaatioOidsAndOppilaitostyypitFlat = oid match {
    case RootOrganisaatioOid => (Seq(RootOrganisaatioOid), Koulutustyyppi.values)
    case _ => getHierarkia(oid, orgs => (children(oid, orgs), oppilaitostyypit(oid, orgs)))
  }

  def getAllChildAndParentOidsWithOppilaitostyypitFlat(oid: OrganisaatioOid): OrganisaatioOidsAndOppilaitostyypitFlat = oid match {
    case RootOrganisaatioOid => (Seq(RootOrganisaatioOid), Koulutustyyppi.values)
    case _ => getHierarkia(oid, orgs => (parentsAndChildren(oid, orgs), oppilaitostyypit(oid, orgs)))
  }

  private def children(oid: OrganisaatioOid, organisaatiot: List[OidAndChildren]): Seq[OrganisaatioOid] =
    find(oid, organisaatiot).map(x => x.oid +: childOidsFlat(x)).getOrElse(Seq()).distinct

  private def parentsAndChildren(oid: OrganisaatioOid, organisaatiot: List[OidAndChildren]): Seq[OrganisaatioOid] =
    find(oid, organisaatiot).map(x => parentOidsFlat(x) ++ Seq(x.oid) ++ childOidsFlat(x)).getOrElse(Seq()).distinct

  private def oppilaitostyypit(oid: OrganisaatioOid, organisaatiot: Seq[OidAndChildren]): Seq[Koulutustyyppi] =
    find(oid, organisaatiot).map{
      x => parentOppilaitostyypitFlat(x, organisaatiot) ++ Seq(x.oppilaitostyyppi) ++ childOppilaitostyypitFlat(x)
    }.getOrElse(Seq()).filter(_.isDefined).map(_.get).distinct.map(Koulutustyyppi.fromOppilaitostyyppi)

  @tailrec
  private def find(oid: OrganisaatioOid, level: Seq[OidAndChildren]): Option[OidAndChildren] =
    level.find(_.oid == oid) match {
      case None if level.isEmpty => None
      case Some(c) => Some(c)
      case None => find(oid, level.flatMap(_.children))
    }

  private def childOidsFlat(item: OidAndChildren): Seq[OrganisaatioOid] =
    item.children.flatMap(c => c.oid +: childOidsFlat(c))

  private def parentOidsFlat(item: OidAndChildren): Seq[OrganisaatioOid] =
    item.parentOidPath.split('/').toSeq.reverse.map(OrganisaatioOid)

  private def childOppilaitostyypitFlat(item: OidAndChildren): Seq[Option[String]] =
    item.children.flatMap(c => c.oppilaitostyyppi +: childOppilaitostyypitFlat(c))

  private def parentOppilaitostyypitFlat(item: OidAndChildren, hierarkia: Seq[OidAndChildren]): Seq[Option[String]] =
    parentOidsFlat(item).map(find(_, hierarkia)).map { _ match {
      case None => None
      case Some(org) => org.oppilaitostyyppi
    }}

  private def pickChildrenRecursively(current: OidAndChildren, oid: OrganisaatioOid): Option[OidAndChildren] = {
    if(current.oid.equals(oid)) {
      Some(current)
    } else {
      current.children.view
        .map(pickChildrenRecursively(_, oid))
        .collectFirst { case Some(child) => current.copy(children = List(child)) }
    }
  }

  private def findHierarkia(oid: OrganisaatioOid): List[OidAndChildren] =
    cachedOrganisaatioHierarkiaClient.getWholeOrganisaatioHierarkiaCached()
      .organisaatiot.view
      .map(pickChildrenRecursively(_, oid))
      .collectFirst { case Some(child) => List(child) }
      .getOrElse(List())

  implicit val HierarkiaCache = CaffeineCache[List[OidAndChildren]]

  private def removeLakkautetutRecursively(current: OidAndChildren): Option[OidAndChildren] = {
    if(current.isPassiivinen) {
      None
    } else {
      Some(current.copy(children = current.children.flatMap(removeLakkautetutRecursively(_))))
    }
  }

  protected def getHierarkia[R](oid: OrganisaatioOid, result: List[OidAndChildren] => R, lakkautetut: Boolean = false) = {
    val hierarkia: List[OidAndChildren] = sync.caching(oid)(Some(45.minutes)) {
      findHierarkia(oid)
    }
    result {
      if(lakkautetut) { hierarkia } else { hierarkia.flatMap(removeLakkautetutRecursively) }
    }
  }
}

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
  def isPassiivinen = status.equalsIgnoreCase("PASSIIVINEN")
}
