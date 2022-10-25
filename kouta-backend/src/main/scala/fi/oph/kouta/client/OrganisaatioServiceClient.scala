package fi.oph.kouta.client

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.{Organisaatio, OrganisaatioHierarkia, OrganisaatioServiceOrganisaatio}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.util.KoutaJsonFormats
import fi.oph.kouta.util.MiscUtils.retryStatusCodes
import fi.vm.sade.utils.slf4j.Logging
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

case class OrganisaatioQueryException(url: String, status: Int, message: String) extends RuntimeException(message)
case class OrganisaatioNotFoundException(message: String) extends RuntimeException(message)

object OrganisaatioServiceClient extends OrganisaatioServiceClient

class OrganisaatioServiceClient extends HttpClient with CallerId with Logging with KoutaJsonFormats {
  private lazy val urlProperties = KoutaConfigurationFactory.configuration.urlProperties

  implicit val organisaatioHierarkiaCache: Cache[List[OrganisaatioOid], OrganisaatioHierarkia] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()
  implicit val organisaatioCache: Cache[OrganisaatioOid, OrganisaatioServiceOrganisaatio] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()
  implicit val organisaatiotCache: Cache[List[OrganisaatioOid], Seq[OrganisaatioServiceOrganisaatio]] = Scaffeine()
    .expireAfterWrite(45.minutes)
    .build()

  def getOrganisaatioHierarkia(oids: List[OrganisaatioOid],
                               oid: Option[OrganisaatioOid] = None,
                               aktiiviset: String = "true",
                               suunnitellut: String = "true",
                               lakkautetut: String = "false",
                               skipParents: String = "true",
                              ): OrganisaatioHierarkia = {
    val oidsAsQueryParams = oids.mkString("&oidRestrictionList=", "&oidRestrictionList=", "")
    val params = toQueryParams(
      "aktiiviset" -> aktiiviset,
      "suunnitellut" -> suunnitellut,
      "lakkautetut" -> lakkautetut,
      "skipParents" -> skipParents)

    oid match {
      case Some(oid) => params.put("oid", oid.toString)
      case None =>
    }

    val url = urlProperties.url(
      s"organisaatio-service.organisaatio.hierarkia", params) + oidsAsQueryParams
    get(url, followRedirects = true) {
      response => {
        parse(response).extract[OrganisaatioHierarkia]
      }
    }
  }

  def getOrganisaatioHierarkiaWithOidsFromCache(oids: List[OrganisaatioOid]): OrganisaatioHierarkia = {
    organisaatioHierarkiaCache.get(oids, oids => getOrganisaatioHierarkia(oids))
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

