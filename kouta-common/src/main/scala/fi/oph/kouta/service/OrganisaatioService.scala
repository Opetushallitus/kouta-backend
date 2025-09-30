package fi.oph.kouta.service

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.client.{CachedOrganisaatioHierarkiaClient, OidAndChildren}
import fi.oph.kouta.domain.Koulutustyyppi
import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid}

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait OrganisaatioService {
  type OrganisaatioOidsAndKoulutustyypitFlat = (Seq[OrganisaatioOid], Seq[Koulutustyyppi])

  protected def cachedOrganisaatioHierarkiaClient: CachedOrganisaatioHierarkiaClient

  private def findMatchingTopLevelOrgs(pred: OidAndChildren => Boolean, orgs: Seq[OidAndChildren]): Seq[OidAndChildren] = {
    orgs.flatMap(
      org => {
        if (pred(org)) Some(org)
        else if (org.children.nonEmpty) findMatchingTopLevelOrgs(pred, org.children)
        else None
      }
    )
  }

  def findMatchingOppilaitosBranches(organisaatioOids: Seq[OrganisaatioOid]): List[OidAndChildren] = {
    if (organisaatioOids.isEmpty) List()
    else findMatchingTopLevelOrgs(
      org => !org.isPassiivinen && org.isOppilaitos && find(o => organisaatioOids.contains(o.oid), Set(org)).isDefined,
      cachedOrganisaatioHierarkiaClient.getWholeOrganisaatioHierarkiaCached().organisaatiot).toList
  }

  def getAllChildOidsFlat(oid: OrganisaatioOid, lakkautetut: Boolean = false): Seq[OrganisaatioOid] = oid match {
    case RootOrganisaatioOid => Seq(RootOrganisaatioOid)
    case _                   => children(getPartialHierarkia(oid, lakkautetut))
  }

  def getAllChildOidsAndKoulutustyypitFlat(oid: OrganisaatioOid): OrganisaatioOidsAndKoulutustyypitFlat = oid match {
    case RootOrganisaatioOid => (Seq(RootOrganisaatioOid), Koulutustyyppi.values)
    case _                   => (children(getPartialHierarkia(oid)), hierarkiaToKoulutustyypit(getHierarkiaFromCache(oid)))
  }

  def withoutOppilaitostyypit(oids: Seq[OrganisaatioOid], oppilaitostyypit: Seq[String]) = {
    if (oppilaitostyypit.isEmpty) {
      Seq()
    } else {
      oids.filter(oid => !(this.oppilaitostyypit(getHierarkiaFromCache(oid)).exists(oppilaitostyypit.contains(_))))
    }
  }

  def getAllChildAndParentOidsWithKoulutustyypitFlat(oid: OrganisaatioOid): OrganisaatioOidsAndKoulutustyypitFlat =
    oid match {
      case RootOrganisaatioOid => (Seq(RootOrganisaatioOid), Koulutustyyppi.values)
      case _                   => (parentsAndChildren(getPartialHierarkia(oid)), hierarkiaToKoulutustyypit(getHierarkiaFromCache(oid)))
    }

  def findOppilaitosOidFromOrganisaationHierarkia(oid: OrganisaatioOid): Option[OrganisaatioOid] =
    find(_.isOppilaitos, getHierarkiaFromCache(oid).toSet).map(_.oid)

  def findOrganisaatioOidsFlatByMemberOid(oid: OrganisaatioOid): Seq[OrganisaatioOid] =
    getHierarkiaFromCache(oid) match {
      case Some(hierarkia) => childOidsFlat(hierarkia) :+ hierarkia.oid
      case _               => Seq()
    }

  def findUnknownOrganisaatioOidsFromHierarkia(
      checkedOrganisaatiot: Set[OrganisaatioOid]
  ): Either[Throwable, Set[OrganisaatioOid]] = {
    def findChildren(item: OidAndChildren): Seq[OrganisaatioOid] = {
      item.children.flatMap(c => {
        if (checkedOrganisaatiot.contains(c.oid)) c.oid +: findChildren(c)
        else findChildren(c)
      })
    }

    var returnValue: Either[Throwable, Set[OrganisaatioOid]] = Right(Set())
    var allChildren: Seq[OrganisaatioOid]                    = Seq()
    Try[OidAndChildren] {
      OidAndChildren(
        RootOrganisaatioOid,
        Map("fi" -> "Opetushallitus"),
        cachedOrganisaatioHierarkiaClient.getWholeOrganisaatioHierarkiaCached().organisaatiot,
        RootOrganisaatioOid.s,
        None,
        "AKTIIVINEN"
      )
    } match {
      case Success(topLevelItem) => allChildren = findChildren(topLevelItem)
      case Failure(exp)          => returnValue = Left(exp)
    }

    if (returnValue.left.toOption.isDefined) returnValue else Right(checkedOrganisaatiot diff allChildren.toSet)
  }

  private def children(hierarkia: Option[OidAndChildren]): Seq[OrganisaatioOid] =
    hierarkia.map(x => x.oid +: childOidsFlat(x)).getOrElse(Seq()).distinct

  private def parentsAndChildren(hierarkia: Option[OidAndChildren]): Seq[OrganisaatioOid] =
    hierarkia.map(x => parentOidsFlat(x) ++ Seq(x.oid) ++ childOidsFlat(x)).getOrElse(Seq()).distinct

  private def oppilaitostyypit(hierarkia: Option[OidAndChildren]): Seq[String] =
    hierarkia
      .map(x => parentOppilaitostyypitFlat(x, hierarkia) ++ Seq(x.oppilaitostyyppi) ++ childOppilaitostyypitFlat(x))
      .getOrElse(Seq())
      .flatten
      .distinct

  private def hierarkiaToKoulutustyypit(hierarkia: Option[OidAndChildren]): Seq[Koulutustyyppi] = {
    hierarkia
      .map(h => oppilaitostyypit(Some(h)))
      .map(oppilaitostyypitToKoulutustyypit)
      .getOrElse(Seq())
  }
  private def oppilaitostyypitToKoulutustyypit(oppilaitostyypit: Seq[String]) =
    oppilaitostyypit.flatMap(Koulutustyyppi.fromOppilaitostyyppi).distinct

  @tailrec
  protected final def find(pred: OidAndChildren => Boolean, level: Set[OidAndChildren]): Option[OidAndChildren] =
    level.find(pred) match {
      case None if level.isEmpty => None
      case Some(c)               => Some(c)
      case None                  => find(pred, level.flatMap(_.children))
    }

  private def childOidsFlat(item: OidAndChildren): Seq[OrganisaatioOid] =
    item.children.flatMap(c => c.oid +: childOidsFlat(c))

  private def parentOidsFlat(item: OidAndChildren): Seq[OrganisaatioOid] =
    item.parentOidPath.split('/').toSeq.reverse.map(OrganisaatioOid)

  private def childOppilaitostyypitFlat(item: OidAndChildren): Seq[Option[String]] =
    item.children.flatMap(c => c.oppilaitostyyppi +: childOppilaitostyypitFlat(c))

  private def parentOppilaitostyypitFlat(item: OidAndChildren, hierarkia: Option[OidAndChildren]): Seq[Option[String]] =
    parentOidsFlat(item).map { case oid =>
      find(_.oid == oid, hierarkia.toSet)
    }.collect { case Some(org) =>
      org.oppilaitostyyppi
    }

  private def pickChildrenRecursively(current: OidAndChildren, oid: OrganisaatioOid): Option[OidAndChildren] = {
    if (current.oid.equals(oid)) {
      Some(current)
    } else {
      current.children.iterator
        .map(pickChildrenRecursively(_, oid))
        .collectFirst { case Some(child) => current.copy(children = List(child)) }
    }
  }

  private def findHierarkia(oid: OrganisaatioOid): Option[OidAndChildren] =
    cachedOrganisaatioHierarkiaClient
      .getWholeOrganisaatioHierarkiaCached()
      .organisaatiot
      .iterator
      .map(pickChildrenRecursively(_, oid))
      .collectFirst { case Some(child) => child }

  implicit val hierarkiaCache: Cache[OrganisaatioOid, Option[OidAndChildren]] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()

  private def removeLakkautetutRecursively(current: OidAndChildren): Option[OidAndChildren] = {
    if (current.isPassiivinen) {
      None
    } else {
      Some(current.copy(children = current.children.flatMap(removeLakkautetutRecursively)))
    }
  }

  private def getPartialHierarkia(oid: OrganisaatioOid, lakkautetut: Boolean = false): Option[OidAndChildren] =
    find(_.oid == oid, getHierarkiaFromCache(oid, lakkautetut).toSet)

  private def getHierarkiaFromCache(oid: OrganisaatioOid, lakkautetut: Boolean = false): Option[OidAndChildren] = {
    val hierarkia: Option[OidAndChildren] = hierarkiaCache.get(oid, oid => findHierarkia(oid))

    if (lakkautetut) {
      hierarkia
    } else {
      hierarkia.flatMap(removeLakkautetutRecursively)
    }
  }
}
