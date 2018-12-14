package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.util.KoutaJsonFormats
import org.json4s._
import org.json4s.jackson.JsonMethods._

object OrganisaatioClient extends HttpClient with KoutaJsonFormats {

  val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  private def queryParams(oid:String, skipParents:Boolean) =
    toQueryParams(
      "oid" -> oid,
      "aktiiviset" -> "true",
      "suunnitellut" -> "true",
      "lakkautetut" -> "false",
      "skipParents" -> s"$skipParents")

  private def getHierarkiaFlat(oid:String, skipParents:Boolean) = get(
    urlProperties.url("organisaatio-service.organisaatio.hierarkia", queryParams(oid, skipParents)),
    (r) => oids(parse(r).extract[OrganisaatioResponse].organisaatiot))


  def getAllParentAndChildOidsFlat(oid:String):Seq[String] = getHierarkiaFlat(oid, false)

  def getAllChildOidsFlat(oid:String):Seq[String] = getHierarkiaFlat(oid, true)

  case class OrganisaatioResponse(numHits:Int, organisaatiot:List[OidAndChildren])

  case class OidAndChildren(oid:String, children:List[OidAndChildren])

  private def oids(l:List[OidAndChildren]):Seq[String] =
    l.map(_.oid) ++ l.map(x => oids(x.children)).flatten
}