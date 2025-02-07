package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{OrgServiceOrganisaatioHierarkia, OrganisaatioServiceOrg, OrganisaatioHierarkia}
import fi.oph.kouta.util.{KoutaJsonFormats, OrganisaatioServiceUtil}
import fi.oph.kouta.logging.Logging
import org.json4s.jackson.JsonMethods.parse
import org.scalatra.{MultiParams, Params}

import java.net.URLEncoder
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

case class OrganisaatioServiceQueryException(url: String, status: Int, message: String)
    extends RuntimeException(message)

case class OrganisaatioHierarkiaQueryParams(
    oids: Seq[OrganisaatioOid] = Seq(),
    oid: Option[OrganisaatioOid] = None,
    searchStr: Option[String] = None,
    aktiiviset: String = "true",
    suunnitellut: String = "true",
    lakkautetut: String = "false",
    skipParents: String = "true",
    oppilaitostyypit: Seq[String] = Seq()
)

object OrganisaatioServiceClient extends OrganisaatioServiceClient

class OrganisaatioServiceClient extends HttpClient with CallerId with Logging with KoutaJsonFormats {
  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  val errorHandler = (url: String, status: Int, response: String) => throw OrganisaatioServiceQueryException(url, status, response)

  implicit val organisaatioHierarkiaWithOidsCache: Cache[Seq[OrganisaatioOid], OrganisaatioHierarkia] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()
  implicit val organisaatioHierarkiaCache: Cache[OrganisaatioHierarkiaQueryParams, OrganisaatioHierarkia] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()
  implicit val organisaatioCache: Cache[OrganisaatioOid, OrganisaatioServiceOrg] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()
  implicit val organisaatiotCache: Cache[Seq[OrganisaatioOid], Seq[OrganisaatioServiceOrg]] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()
  implicit val organisaatioChildrenCache: Cache[OrganisaatioHierarkiaQueryParams, OrganisaatioHierarkia] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()

  def getOrganisaatioHierarkia(queryParams: OrganisaatioHierarkiaQueryParams): OrganisaatioHierarkia = {
    val oidsAsQueryParams = queryParams.oids.mkString("&oidRestrictionList=", "&oidRestrictionList=", "")
    val oppilaitostyypitAsQueryParams = queryParams.oppilaitostyypit
      .map(URLEncoder.encode(_, "UTF-8"))
      .mkString("&oppilaitostyyppi=", "&oppilaitostyyppi=", "")

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
      case (key, list: List[_])              => (key, list.toString())
    }.filter(_._2.nonEmpty)

    val queryParamsStr = toQueryParams(params: _*)
    val url = urlProperties.url(
      s"organisaatio-service.organisaatio.hierarkia",
      queryParamsStr
    ) + oidsAsQueryParams + oppilaitostyypitAsQueryParams

    get(url, errorHandler, followRedirects = true) { response =>
      {
        val parsedOrganisaatioHierarkia = parse(response).extract[OrgServiceOrganisaatioHierarkia]
        OrganisaatioHierarkia(
          organisaatiot = parsedOrganisaatioHierarkia.organisaatiot.map(org =>
            OrganisaatioServiceUtil.organisaatioServiceOrgToOrganisaatio(org))
        )
      }
    }
  }

  def getOrganisaatioHierarkiaWithOidsFromCache(oids: Seq[OrganisaatioOid]): OrganisaatioHierarkia = {
    organisaatioHierarkiaWithOidsCache.get(
      oids,
      oids => {
        getOrganisaatioHierarkia(OrganisaatioHierarkiaQueryParams(oids))
      }
    )
  }

  def getOrganisaatioHierarkiaFromCache(
      params: Option[Params],
      multiParams: Option[MultiParams],
      oppilaitostyypit: List[String] = List()
  ): OrganisaatioHierarkia = {
    val oids = multiParams.flatMap(_.get("oidRestrictionList")).getOrElse(Seq()).map(OrganisaatioOid(_))
    val queryParams = params match {
      case Some(params) =>
        OrganisaatioHierarkiaQueryParams(
          searchStr = params.get("searchStr"),
          oids = oids,
          oid = params.get("oid").map(OrganisaatioOid(_)),
          aktiiviset = params.get("aktiiviset").getOrElse("true"),
          suunnitellut = params.get("suunnitellut").getOrElse("true"),
          lakkautetut = params.get("lakkautetut").getOrElse("false"),
          skipParents = params.get("skipParents").getOrElse("true"),
          oppilaitostyypit = oppilaitostyypit
        )
      case None =>
        OrganisaatioHierarkiaQueryParams(
          oids = oids,
          oppilaitostyypit = oppilaitostyypit
        )
    }

    Try[OrganisaatioHierarkia] {
      organisaatioHierarkiaCache.get(
        queryParams,
        queryParams => {
          getOrganisaatioHierarkia(queryParams)
        }
      )
    } match {
      case Success(organisaatioHierarkia: OrganisaatioHierarkia) => organisaatioHierarkia
      case Failure(exp: OrganisaatioServiceQueryException) if exp.status == 404 =>
        OrganisaatioHierarkia(organisaatiot = List())
      case Failure(exp: Exception) => throw exp
    }
  }

  def getOrganisaatio(oid: OrganisaatioOid): OrganisaatioServiceOrg = {
    val url = urlProperties.url(s"organisaatio-service.organisaatio.with.oid", oid)

    get(url, errorHandler, followRedirects = true) { response =>
      {
        parse(response).extract[OrganisaatioServiceOrg]
      }
    }
  }

  def getOrganisaatioWithOidFromCache(oid: OrganisaatioOid): OrganisaatioServiceOrg = {
    organisaatioCache.get(oid, oid => getOrganisaatio(oid))
  }

  def getOrganisaatiot(oids: Seq[OrganisaatioOid]): Seq[OrganisaatioServiceOrg] = {
    val url = urlProperties.url(s"organisaatio-service.organisaatiot.with.oids")
    post(url, oids, errorHandler, followRedirects = true) { response =>
      {
        parse(response).extract[Seq[OrganisaatioServiceOrg]]
      }
    }
  }

  def getOrganisaatiotWithOidsFromCache(oids: Seq[OrganisaatioOid]): Seq[OrganisaatioServiceOrg] = {
    organisaatiotCache.get(oids, oids => getOrganisaatiot(oids))
  }

  def getOrganisaatioChildrenFromCache(oid: OrganisaatioOid): OrganisaatioHierarkia = {
    val queryParams = OrganisaatioHierarkiaQueryParams(
      oid = Some(oid)
    )
    organisaatioChildrenCache.get(queryParams, queryParams => getOrganisaatioHierarkia(queryParams))
  }
}
