package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Koulutustyyppi, oppilaitostyyppi2koulutustyyppi}
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.properties.OphProperties
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.annotation.tailrec

object OrganisaatioClient extends HttpClient with KoutaJsonFormats {
  val urlProperties: OphProperties = KoutaConfigurationFactory.configuration.urlProperties

  val OphOid: OrganisaatioOid = KoutaConfigurationFactory.configuration.securityConfiguration.rootOrganisaatio

  def getAllChildOidsAndOppilaitostyypitFlat(oid: OrganisaatioOid): (Seq[OrganisaatioOid], Seq[Koulutustyyppi]) = oid match {
    case OphOid => (Seq(OphOid), Koulutustyyppi.values)
    case _ => getHierarkia(oid, orgs => (children(oid, orgs), oppilaitostyypit(oid, orgs)))
  }

  case class OrganisaatioResponse(numHits: Int, organisaatiot: List[OidAndChildren])
  case class OidAndChildren(oid: OrganisaatioOid, children: List[OidAndChildren], parentOidPath: String, oppilaitostyyppi: Option[String])

  private def getHierarkia[R](oid: OrganisaatioOid, result: List[OidAndChildren] => R) = {
    val url = urlProperties.url("organisaatio-service.organisaatio.hierarkia", queryParams(oid.toString))
    get(url, followRedirects = true) { response =>
      result(parse(response).extract[OrganisaatioResponse].organisaatiot)
    }
  }

  private def queryParams(oid: String) =
    toQueryParams(
      "oid" -> oid,
      "aktiiviset" -> "true",
      "suunnitellut" -> "true",
      "lakkautetut" -> "false")

  private def children(oid: OrganisaatioOid, organisaatiot: List[OidAndChildren]): Seq[OrganisaatioOid] =
    find(oid, organisaatiot).map(x => x.oid +: childOidsFlat(x)).getOrElse(Seq()).distinct

  private def parentsAndChildren(oid: OrganisaatioOid, organisaatiot: List[OidAndChildren]): Seq[OrganisaatioOid] =
    find(oid, organisaatiot).map(x => parentOidsFlat(x) ++ Seq(x.oid) ++ childOidsFlat(x)).getOrElse(Seq()).distinct

  @tailrec
  private def find(oid: OrganisaatioOid, level: List[OidAndChildren]): Option[OidAndChildren] =
    level.find(_.oid == oid) match {
      case None if level.isEmpty => None
      case Some(c) => Some(c)
      case None => find(oid, level.flatMap(_.children))
    }

  private def childOidsFlat(item: OidAndChildren): Seq[OrganisaatioOid] =
    item.children.flatMap(c => c.oid +: childOidsFlat(c))

  private def parentOidsFlat(item: OidAndChildren): Seq[OrganisaatioOid] =
    item.parentOidPath.split('/').toSeq.reverse.map(OrganisaatioOid)

  private def oppilaitostyypit(oid: OrganisaatioOid, organisaatiot: Seq[OidAndChildren]): Seq[Koulutustyyppi] =
    organisaatiot
      .flatMap(findWithParents(oid, _, Seq()))
      .flatMap {
        case (organisaatio, parents) => (parents.map(_.oppilaitostyyppi) :+ organisaatio.oppilaitostyyppi) ++ organisaatio.children.flatMap(childOppilaitostyypitFlat)
      }.flatten
      .map(oppilaitostyyppi2koulutustyyppi)

  private def childOppilaitostyypitFlat(item: OidAndChildren): Seq[Option[String]] =
    item.children.flatMap(c => c.oppilaitostyyppi +: childOppilaitostyypitFlat(c))

  private def findWithParents(oid: OrganisaatioOid, current: OidAndChildren, parents: Seq[OidAndChildren]): Option[(OidAndChildren, Seq[OidAndChildren])] =
    current match {
      case c if c.oid == oid => Some((c, parents))
      case c if c.children.isEmpty => None
      case c => c.children.flatMap(child => findWithParents(oid, child, parents :+ c)).headOption
    }
}
