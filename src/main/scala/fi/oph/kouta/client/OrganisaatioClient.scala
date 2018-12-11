package fi.oph.kouta.client

import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.util.KoutaJsonFormats
import org.json4s._
import org.json4s.jackson.JsonMethods._

object OrganisaatioClient extends HttpClient with KoutaJsonFormats {

  val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  def getAllParentAndChildOidsFlat(oid:String):Seq[String] = {
    get(
      urlProperties.url("organisaatio-service.organisaatio.hierarkia",
        toQueryParams(
          "oid" -> oid,
          "aktiiviset" -> "true",
          "suunnitellut" -> "true",
          "lakkautetut" -> "false"
      )),
      (r) => oids(parse(r).extract[OrganisaatioResponse].organisaatiot)
    )
  }

  case class OrganisaatioResponse(numHits:Int, organisaatiot:List[OidAndChildren])

  case class OidAndChildren(oid:String, children:List[OidAndChildren])

  private def oids(l:List[OidAndChildren]):Seq[String] =
    l.map(_.oid) ++ l.map(x => oids(x.children)).flatten
}