package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Koulutustyyppi, oppilaitostyyppi2koulutustyyppi}
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.properties.OphProperties
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.annotation.tailrec

trait OrganisaatioClient {
  type OrganisaatioOidsAndOppilaitostyypitFlat = (Seq[OrganisaatioOid], Seq[Koulutustyyppi])
}

object OrganisaatioClient extends OrganisaatioClient with HttpClient with KoutaJsonFormats {
  val urlProperties: OphProperties = KoutaConfigurationFactory.configuration.urlProperties

  val OphOid: OrganisaatioOid = KoutaConfigurationFactory.configuration.securityConfiguration.rootOrganisaatio

  def getAllChildOidsFlat(oid: OrganisaatioOid, lakkautetut: Boolean = false): Seq[OrganisaatioOid] = oid match {
    case OphOid => Seq(OphOid)
    case _ => getHierarkia(oid, orgs => children(oid, orgs), lakkautetut)
  }

  def getAllChildOidsAndOppilaitostyypitFlat(oid: OrganisaatioOid): OrganisaatioOidsAndOppilaitostyypitFlat = oid match {
    case OphOid => (Seq(OphOid), Koulutustyyppi.values)
    case _ => getHierarkia(oid, orgs => (children(oid, orgs), oppilaitostyypit(oid, orgs)))
  }

  def getAllChildAndParentOidsWithOppilaitostyypitFlat(oid: OrganisaatioOid): OrganisaatioOidsAndOppilaitostyypitFlat = oid match {
    case OphOid => (Seq(OphOid), Koulutustyyppi.values)
    case _ => getHierarkia(oid, orgs => (parentsAndChildren(oid, orgs), oppilaitostyypit(oid, orgs)))
  }

  case class OrganisaatioResponse(numHits: Int, organisaatiot: List[OidAndChildren])
  case class OidAndChildren(oid: OrganisaatioOid, children: List[OidAndChildren], parentOidPath: String, oppilaitostyyppi: Option[String])

  private def getHierarkia[R](oid: OrganisaatioOid, result: List[OidAndChildren] => R, lakkautetut: Boolean = false) = {
    val url = urlProperties.url("organisaatio-service.organisaatio.hierarkia", queryParams(oid.toString, lakkautetut))
    get(url, followRedirects = true) { response =>
      result(parse(response).extract[OrganisaatioResponse].organisaatiot)
    }
  }

  private def queryParams(oid: String, lakkautetut: Boolean = false) =
    toQueryParams(
      "oid" -> oid,
      "aktiiviset" -> "true",
      "suunnitellut" -> "true",
      "lakkautetut" -> Option(lakkautetut).map(_.toString).getOrElse("false"))

  private def children(oid: OrganisaatioOid, organisaatiot: List[OidAndChildren]): Seq[OrganisaatioOid] =
    find(oid, organisaatiot).map(x => x.oid +: childOidsFlat(x)).getOrElse(Seq()).distinct

  private def parentsAndChildren(oid: OrganisaatioOid, organisaatiot: List[OidAndChildren]): Seq[OrganisaatioOid] =
    find(oid, organisaatiot).map(x => parentOidsFlat(x) ++ Seq(x.oid) ++ childOidsFlat(x)).getOrElse(Seq()).distinct

  private def oppilaitostyypit(oid: OrganisaatioOid, organisaatiot: Seq[OidAndChildren]): Seq[Koulutustyyppi] =
    find(oid, organisaatiot).map{
      x => parentOppilaitostyypitFlat(x, organisaatiot) ++ Seq(x.oppilaitostyyppi) ++ childOppilaitostyypitFlat(x)
    }.getOrElse(Seq()).filter(_.isDefined).map(_.get).distinct.map(oppilaitostyyppi2koulutustyyppi)

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
}
