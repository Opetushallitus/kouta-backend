package fi.oph.kouta.service

import fi.oph.kouta.client.{CachedOrganisaatioHierarkiaClient, OidAndChildren}
import fi.oph.kouta.domain.Koulutustyyppi
import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid}
import scalacache.caffeine.CaffeineCache
import scalacache.modes.sync._
import scalacache.sync

import scala.annotation.tailrec
import scala.concurrent.duration._

trait OrganisaatioService {
  type OrganisaatioOidsAndOppilaitostyypitFlat = (Seq[OrganisaatioOid], Seq[Koulutustyyppi])

  protected def cachedOrganisaatioHierarkiaClient: CachedOrganisaatioHierarkiaClient

  def getAllChildOidsFlat(oid: OrganisaatioOid, lakkautetut: Boolean = false): Seq[OrganisaatioOid] = oid match {
    case RootOrganisaatioOid => Seq(RootOrganisaatioOid)
    case _ => getPartialHierarkia(oid, hierarkia => children(hierarkia), lakkautetut)
  }

  def getAllChildOidsAndOppilaitostyypitFlat(oid: OrganisaatioOid): OrganisaatioOidsAndOppilaitostyypitFlat = oid match {
    case RootOrganisaatioOid => (Seq(RootOrganisaatioOid), Koulutustyyppi.values)
    case _ => getPartialHierarkia(oid, hierarkia => (children(hierarkia), oppilaitostyypit(hierarkia)))
  }

  def getAllChildAndParentOidsWithOppilaitostyypitFlat(oid: OrganisaatioOid): OrganisaatioOidsAndOppilaitostyypitFlat = oid match {
    case RootOrganisaatioOid => (Seq(RootOrganisaatioOid), Koulutustyyppi.values)
    case _ => getPartialHierarkia(oid, hierarkia => (parentsAndChildren(hierarkia), oppilaitostyypit(hierarkia)))
  }

  private def children(hierarkia: Option[OidAndChildren]): Seq[OrganisaatioOid] =
    hierarkia.map(x => x.oid +: childOidsFlat(x)).getOrElse(Seq()).distinct

  private def parentsAndChildren(hierarkia: Option[OidAndChildren]): Seq[OrganisaatioOid] =
    hierarkia.map(x => parentOidsFlat(x) ++ Seq(x.oid) ++ childOidsFlat(x)).getOrElse(Seq()).distinct

  private def oppilaitostyypit(hierarkia: Option[OidAndChildren]): Seq[Koulutustyyppi] =
    hierarkia
      .map(x => parentOppilaitostyypitFlat(x, hierarkia) ++ Seq(x.oppilaitostyyppi) ++ childOppilaitostyypitFlat(x))
      .getOrElse(Seq())
      .flatten
      .distinct
      .map(Koulutustyyppi.fromOppilaitostyyppi)

  @tailrec
  private def find(oid: OrganisaatioOid, level: Set[OidAndChildren]): Option[OidAndChildren] =
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

  private def parentOppilaitostyypitFlat(item: OidAndChildren, hierarkia: Option[OidAndChildren]): Seq[Option[String]] =
    parentOidsFlat(item).map(find(_, hierarkia.toSet)).collect {
      case Some(org) => org.oppilaitostyyppi
    }

  private def pickChildrenRecursively(current: OidAndChildren, oid: OrganisaatioOid): Option[OidAndChildren] = {
    if (current.oid.equals(oid)) {
      Some(current)
    } else {
      current.children
        .iterator
        .map(pickChildrenRecursively(_, oid))
        .collectFirst { case Some(child) => current.copy(children = List(child)) }
    }
  }

  private def findHierarkia(oid: OrganisaatioOid): Option[OidAndChildren] =
    cachedOrganisaatioHierarkiaClient.getWholeOrganisaatioHierarkiaCached()
      .organisaatiot
      .iterator
      .map(pickChildrenRecursively(_, oid))
      .collectFirst { case Some(child) => child }

  implicit val HierarkiaCache = CaffeineCache[Option[OidAndChildren]]

  private def removeLakkautetutRecursively(current: OidAndChildren): Option[OidAndChildren] = {
    if (current.isPassiivinen) {
      None
    } else {
      Some(current.copy(children = current.children.flatMap(removeLakkautetutRecursively)))
    }
  }

  private def getPartialHierarkia[R](oid: OrganisaatioOid, result: Option[OidAndChildren] => R, lakkautetut: Boolean = false): R =
    result(find(oid, getHierarkia(oid, lakkautetut).toSet))

  private def getHierarkia(oid: OrganisaatioOid, lakkautetut: Boolean = false): Option[OidAndChildren] = {
    val hierarkia: Option[OidAndChildren] = sync.caching(oid)(Some(45.minutes)) {
      findHierarkia(oid)
    }
    if (lakkautetut) {
      hierarkia
    } else {
      hierarkia.flatMap(removeLakkautetutRecursively)
    }
  }
}
