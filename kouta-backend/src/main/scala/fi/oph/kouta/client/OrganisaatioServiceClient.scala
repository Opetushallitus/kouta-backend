package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Organisaatio, OrganisaatioHierarkia}
import fi.oph.kouta.servlet.SearchParams.commaSepStringValToSeq
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods.parse
import org.scalatra.Params

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

  implicit val organisaatioHierarkiaWithOidsCache: Cache[Seq[OrganisaatioOid], OrganisaatioHierarkia] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()
  implicit val organisaatioHierarkiaCache: Cache[OrganisaatioHierarkiaQueryParams, OrganisaatioHierarkia] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()
  implicit val organisaatioCache: Cache[OrganisaatioOid, Organisaatio] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()
  implicit val organisaatiotCache: Cache[Seq[OrganisaatioOid], Seq[Organisaatio]] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()

  def getOrganisaatioHierarkia(queryParams: OrganisaatioHierarkiaQueryParams): OrganisaatioHierarkia = {
    val oidsAsQueryParams = queryParams.oids.mkString("&oidRestrictionList=", "&oidRestrictionList=", "")

    val params = Seq(
      "aktiiviset"       -> queryParams.aktiiviset,
      "suunnitellut"     -> queryParams.suunnitellut,
      "lakkautetut"      -> queryParams.lakkautetut,
      "skipParents"      -> queryParams.skipParents,
      "searchStr"        -> queryParams.searchStr,
      "oid"              -> queryParams.oid,
      "oppilaitostyypit" -> queryParams.oppilaitostyypit
    ).collect {
      case (key, Some(s: String))            => (key, s)
      case (key, Some(oid: OrganisaatioOid)) => (key, oid.toString)
      case (key, s: String)                  => (key, s)
      case (key, list: List[_])              => (key, list.toString())
    }

    val url =
      urlProperties.url(s"organisaatio-service.organisaatio.hierarkia", toQueryParams(params: _*)) + oidsAsQueryParams
    get(url, followRedirects = true) { response =>
      {
        parse(response).extract[OrganisaatioHierarkia]
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
      oppilaitostyypit: List[String] = List()
  ): OrganisaatioHierarkia = {
    val queryParams = params match {
      case Some(params) =>
        OrganisaatioHierarkiaQueryParams(
          searchStr = params.get("searchStr"),
          oids = commaSepStringValToSeq(params.get("oidRestrictionList")).map(OrganisaatioOid(_)).toList,
          oid = params.get("oid").map(OrganisaatioOid(_)),
          aktiiviset = params.get("aktiiviset").getOrElse("true"),
          suunnitellut = params.get("suunnitellut").getOrElse("true"),
          lakkautetut = params.get("lakkautetut").getOrElse("false"),
          skipParents = params.get("skipParents").getOrElse("true"),
          oppilaitostyypit = oppilaitostyypit
        )
      case None =>
        OrganisaatioHierarkiaQueryParams(
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
    }
  }

  def getOrganisaatio(oid: OrganisaatioOid): Organisaatio = {
    val url = urlProperties.url(s"organisaatio-service.organisaatio.with.oid", oid)
    get(url, followRedirects = true) { response =>
      {
        parse(response).extract[Organisaatio]
      }
    }
  }

  def getOrganisaatioWithOidFromCache(oid: OrganisaatioOid): Organisaatio = {
    organisaatioCache.get(oid, oid => getOrganisaatio(oid))
  }

  def getOrganisaatiot(oids: Seq[OrganisaatioOid]): Seq[Organisaatio] = {
    val url = urlProperties.url(s"organisaatio-service.organisaatiot.with.oids")
    post(url, oids, followRedirects = true) { response =>
      {
        parse(response).extract[Seq[Organisaatio]]
      }
    }
  }

  def getOrganisaatiotWithOidsFromCache(oids: Seq[OrganisaatioOid]): Seq[Organisaatio] = {
    organisaatiotCache.get(oids, oids => getOrganisaatiot(oids))
  }
}
