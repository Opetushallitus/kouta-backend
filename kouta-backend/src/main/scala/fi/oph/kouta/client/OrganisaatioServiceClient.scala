package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{OrganisaatioHierarkia, OrganisaatioServiceOrganisaatio}
import fi.oph.kouta.servlet.SearchParams.commaSepStringValToSeq
import fi.oph.kouta.util.KoutaJsonFormats
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods.parse
import org.scalatra.Params

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

case class OrganisaatioQueryException(url: String, status: Int, message: String) extends RuntimeException(message)
case class OrganisaatioNotFoundException(message: String) extends RuntimeException(message)

case class OrganisaatioHierarkiaQueryParams(oids: List[OrganisaatioOid] = List(),
                                            oid: Option[OrganisaatioOid] = None,
                                            searchStr: Option[String] = None,
                                            aktiiviset: String = "true",
                                            suunnitellut: String = "true",
                                            lakkautetut: String = "false",
                                            skipParents: String = "true")

object OrganisaatioServiceClient extends OrganisaatioServiceClient

class OrganisaatioServiceClient extends HttpClient with CallerId with Logging with KoutaJsonFormats {
  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  implicit val organisaatioHierarkiaWithOidsCache: Cache[List[OrganisaatioOid], OrganisaatioHierarkia] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()
  implicit val organisaatioHierarkiaCache: Cache[OrganisaatioHierarkiaQueryParams, OrganisaatioHierarkia] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()
  implicit val organisaatioCache: Cache[OrganisaatioOid, OrganisaatioServiceOrganisaatio] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()
  implicit val organisaatiotCache: Cache[List[OrganisaatioOid], Seq[OrganisaatioServiceOrganisaatio]] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()

  def getOrganisaatioHierarkia(queryParams: OrganisaatioHierarkiaQueryParams): OrganisaatioHierarkia = {
    val oidsAsQueryParams = queryParams.oids.mkString("&oidRestrictionList=", "&oidRestrictionList=", "")

    val params = Seq(
      "aktiiviset" -> queryParams.aktiiviset,
      "suunnitellut" -> queryParams.suunnitellut,
      "lakkautetut" -> queryParams.lakkautetut,
      "skipParents" -> queryParams.skipParents,
      "searchStr" -> queryParams.searchStr,
      "oid" -> queryParams.oid
      ).collect {
        case (key, Some(s: String)) => (key, s)
        case (key, Some(oid: OrganisaatioOid)) => (key, oid.toString)
        case (key, s: String) => (key, s)
      }

    val url = urlProperties.url(
      s"organisaatio-service.organisaatio.hierarkia", toQueryParams(params: _*)) + oidsAsQueryParams
    get(url, followRedirects = true) {
      response => {
        parse(response).extract[OrganisaatioHierarkia]
      }
    }
  }

  def getOrganisaatioHierarkiaWithOidsFromCache(oids: List[OrganisaatioOid]): OrganisaatioHierarkia = {
    organisaatioHierarkiaWithOidsCache.get(oids, oids => {
      getOrganisaatioHierarkia(OrganisaatioHierarkiaQueryParams(oids))
    })
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
    organisaatioHierarkiaCache.get(queryParams, queryParams => {
      getOrganisaatioHierarkia(queryParams)}
    )
  }

  def getOrganisaatio(oid: OrganisaatioOid): OrganisaatioServiceOrganisaatio = {
    val url = urlProperties.url(
      s"organisaatio-service.organisaatio.with.oid", oid)
    get(url, followRedirects = true) {
      response => {
        parse(response).extract[OrganisaatioServiceOrganisaatio]
      }
    }
  }

  def getOrganisaatioWithOid(oid: OrganisaatioOid): OrganisaatioServiceOrganisaatio = {
    Try[OrganisaatioServiceOrganisaatio] {
      getOrganisaatio(oid)
    } match {
      case Success(organisaatio) => organisaatio
      case Failure(exp: OrganisaatioQueryException) if exp.status == 404 =>
        throw OrganisaatioNotFoundException(s"Failed to find organisaatio with oid $oid, got response ${exp.status}, ${exp.message}")
      case Failure(exp: OrganisaatioQueryException) =>
        throw new RuntimeException(s"Failed to get organisaatio with oid $oid, got response ${exp.status}, ${exp.message}")
    }
  }

  def getOrganisaatioWithOidFromCache(oid: OrganisaatioOid): Option[OrganisaatioServiceOrganisaatio] = {
    try {
      val organisaatio = organisaatioCache.get(oid, oid => getOrganisaatioWithOid(oid))
      Some(organisaatio)
    } catch {
      case exp: RuntimeException => throw exp
      case _: Throwable => None
    }
  }

  def getOrganisaatiot(oids: List[OrganisaatioOid]): Seq[OrganisaatioServiceOrganisaatio] = {
    val url = urlProperties.url(
      s"organisaatio-service.organisaatiot.with.oids")
    post(url, oids, followRedirects = true) {
      response => {
        parse(response).extract[Seq[OrganisaatioServiceOrganisaatio]]
      }
    }
  }

  def getOrganisaatiotWithOids(oids: List[OrganisaatioOid]): Seq[OrganisaatioServiceOrganisaatio] = {
    Try[Seq[OrganisaatioServiceOrganisaatio]] {
      getOrganisaatiot(oids)
    } match {
      case Success(organisaatiot) => organisaatiot
      case Failure(exp: OrganisaatioQueryException) if exp.status == 404 =>
        throw OrganisaatioNotFoundException(s"Failed to find organisaatiot with oids $oids, got response ${exp.status}, ${exp.message}")
      case Failure(exp: OrganisaatioQueryException) =>
        throw new RuntimeException(s"Failed to get organisaatiot with oids $oids, got response ${exp.status}, ${exp.message}")
    }
  }

  def getOrganisaatiotWithOidsFromCache(oids: List[OrganisaatioOid]): Seq[OrganisaatioServiceOrganisaatio] = {
    try {
      organisaatiotCache.get(oids, oids => getOrganisaatiotWithOids(oids))
    } catch {
      case exp: RuntimeException => throw exp
    }
  }

}

