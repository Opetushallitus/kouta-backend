package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.util.KoutaJsonFormats
import org.json4s._
import org.json4s.jackson.JsonMethods._

object OrganisaatioClient extends HttpClient with KoutaJsonFormats {

  val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  private def queryParams(oid:String) =
    toQueryParams(
      "oid" -> oid,
      "aktiiviset" -> "true",
      "suunnitellut" -> "true",
      "lakkautetut" -> "false")

  private def getHierarkia[R](oid:String, result: (List[OidAndChildren]) => R) = get(
    urlProperties.url("organisaatio-service.organisaatio.hierarkia", queryParams(oid)),
    (r) => result(parse(r).extract[OrganisaatioResponse].organisaatiot))


  def getAllParentAndChildOidsFlat(oid:String):Seq[String] = getHierarkia(oid, parentsAndChildren(oid, _))

  def getAllChildOidsFlat(oid:String):Seq[String] = getHierarkia(oid, children(oid, _))

  case class OrganisaatioResponse(numHits:Int, organisaatiot:List[OidAndChildren])

  case class OidAndChildren(oid:String, children:List[OidAndChildren], parentOidPath:String)

  private def children(oid:String, organisaatiot:List[OidAndChildren]):Seq[String] =
    find(oid, organisaatiot).map(x => x.oid +: childOisFlat(x)).getOrElse(Seq()).distinct

  private def parentsAndChildren(oid:String, organisaatiot:List[OidAndChildren]):Seq[String] =
    find(oid, organisaatiot).map(x => parentOidsFlat(x) ++ Seq(x.oid) ++ childOisFlat(x)).getOrElse(Seq()).distinct

  private def find(oid:String, level: List[OidAndChildren]):Option[OidAndChildren] =
    level.find(_.oid == oid) match {
      case None if level.isEmpty => None
      case Some(c) => Some(c)
      case None => find(oid, level.map(_.children).flatten)
    }

  private def childOisFlat(item:OidAndChildren):Seq[String] =
    item.children.map(c => c.oid +: childOisFlat(c)).flatten

  private def parentOidsFlat(item:OidAndChildren):Seq[String] =
    item.parentOidPath.split('/').toSeq.reverse

}