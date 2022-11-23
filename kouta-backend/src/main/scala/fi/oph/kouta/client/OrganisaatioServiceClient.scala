package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{OrganisaatioHierarkia, Organisaatio}
import fi.oph.kouta.servlet.SearchParams.commaSepStringValToSeq
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods.parse
import org.scalatra.Params

import scala.concurrent.duration._

case class OrganisaatioHierarkiaQueryParams(
    oids: List[OrganisaatioOid] = List(),
    oid: Option[OrganisaatioOid] = None,
    searchStr: Option[String] = None,
    aktiiviset: String = "true",
    suunnitellut: String = "true",
    lakkautetut: String = "false",
    skipParents: String = "true"
)

object OrganisaatioServiceClient extends OrganisaatioServiceClient

class OrganisaatioServiceClient extends HttpClient with CallerId with Logging with KoutaJsonFormats {
  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  implicit val organisaatioHierarkiaWithOidsCache: Cache[List[OrganisaatioOid], OrganisaatioHierarkia] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()
  implicit val organisaatioHierarkiaCache: Cache[OrganisaatioHierarkiaQueryParams, OrganisaatioHierarkia] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()
  implicit val organisaatioCache: Cache[OrganisaatioOid, Organisaatio] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()
  implicit val organisaatiotCache: Cache[List[OrganisaatioOid], Seq[Organisaatio]] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()

  def getOrganisaatioHierarkia(queryParams: OrganisaatioHierarkiaQueryParams): OrganisaatioHierarkia = {
    val oidsAsQueryParams = queryParams.oids.mkString("&oidRestrictionList=", "&oidRestrictionList=", "")

    val params = Seq(
      "aktiiviset"   -> queryParams.aktiiviset,
      "suunnitellut" -> queryParams.suunnitellut,
      "lakkautetut"  -> queryParams.lakkautetut,
      "skipParents"  -> queryParams.skipParents,
      "searchStr"    -> queryParams.searchStr,
      "oid"          -> queryParams.oid
    ).collect {
      case (key, Some(s: String))            => (key, s)
      case (key, Some(oid: OrganisaatioOid)) => (key, oid.toString)
      case (key, s: String)                  => (key, s)
    }

    val url =
      urlProperties.url(s"organisaatio-service.organisaatio.hierarkia", toQueryParams(params: _*)) + oidsAsQueryParams
    get(url, followRedirects = true) { response =>
      {
        parse(response).extract[OrganisaatioHierarkia]
      }
    }
  }

  def getOrganisaatioHierarkiaWithOidsFromCache(oids: List[OrganisaatioOid]): OrganisaatioHierarkia = {
    organisaatioHierarkiaWithOidsCache.get(
      oids,
      oids => {
        getOrganisaatioHierarkia(OrganisaatioHierarkiaQueryParams(oids))
      }
    )
  }

  def getOrganisaatioHierarkiaFromCache(params: Params): OrganisaatioHierarkia = {
    val queryParams = OrganisaatioHierarkiaQueryParams(
      searchStr = params.get("searchStr"),
      oids = commaSepStringValToSeq(params.get("oidRestrictionList")).map(OrganisaatioOid(_)).toList,
      oid = params.get("oid").map(OrganisaatioOid(_)),
      aktiiviset = params.get("aktiiviset").getOrElse("true"),
      suunnitellut = params.get("suunnitellut").getOrElse("true"),
      lakkautetut = params.get("lakkautetut").getOrElse("false"),
      skipParents = params.get("skipParents").getOrElse("true")
    )
    organisaatioHierarkiaCache.get(
      queryParams,
      queryParams => {
        getOrganisaatioHierarkia(queryParams)
      }
    )
  }

  def getOrganisaatio(oid: OrganisaatioOid): Organisaatio = {
    val url = urlProperties.url(
      s"organisaatio-service.organisaatio.with.oid", oid)
    get(url, followRedirects = true) {
      response => {
        parse(response).extract[Organisaatio]
      }
    }
  }

  def getOrganisaatioWithOidFromCache(oid: OrganisaatioOid): Organisaatio = {
    organisaatioCache.get(oid, oid => getOrganisaatio(oid))
  }

  def getOrganisaatiot(oids: List[OrganisaatioOid]): Seq[Organisaatio] = {
    val url = urlProperties.url(
      s"organisaatio-service.organisaatiot.with.oids")
    post(url, oids, followRedirects = true) {
      response => {
        parse(response).extract[Seq[Organisaatio]]
      }
    }
  }

  def getOrganisaatiotWithOidsFromCache(oids: List[OrganisaatioOid]): Seq[Organisaatio] = {
      organisaatiotCache.get(oids, oids => getOrganisaatiot(oids))
  }

}
