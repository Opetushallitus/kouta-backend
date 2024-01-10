package fi.oph.kouta.service

import fi.oph.kouta.client.{CachedOrganisaatioHierarkiaClient, CallerId, OrganisaatioServiceClient}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid}
import fi.oph.kouta.domain.{OrganisaatioServiceOrg, OrganisaatioHierarkia, oppilaitostyypitForAvoinKorkeakoulutus}
import fi.vm.sade.properties.OphProperties
import org.scalatra.{MultiParams, Params}

import scala.util.{Failure, Success, Try}

object OrganisaatioServiceImpl extends OrganisaatioServiceImpl(KoutaConfigurationFactory.configuration.urlProperties)

class OrganisaatioServiceImpl(urlProperties: OphProperties, organisaatioServiceClient: OrganisaatioServiceClient)
    extends OrganisaatioService {
  def this(urlProperties: OphProperties) = this(urlProperties, new OrganisaatioServiceClient)

  override protected val cachedOrganisaatioHierarkiaClient: CachedOrganisaatioHierarkiaClient =
    new CachedOrganisaatioHierarkiaClient with CallerId {
      override val organisaatioUrl: String =
        urlProperties.url("organisaatio-service.organisaatio.oid.jalkelaiset", RootOrganisaatioOid.s)
    }

  def getOrganisaatioHierarkiaWithOids(oids: List[OrganisaatioOid]): OrganisaatioHierarkia =
    organisaatioServiceClient.getOrganisaatioHierarkiaWithOidsFromCache(oids)

  def getOrganisaatio(organisaatioOid: OrganisaatioOid): Either[Throwable, OrganisaatioServiceOrg] = {
    Try[OrganisaatioServiceOrg] {
      organisaatioServiceClient.getOrganisaatioWithOidFromCache(organisaatioOid)
    } match {
      case Success(organisaatio: OrganisaatioServiceOrg) =>
        Right(organisaatio)
      case Failure(exception) =>
        Left(exception)
    }
  }

  def getOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid]): Either[Throwable, Seq[OrganisaatioServiceOrg]] = {
    Try[Seq[OrganisaatioServiceOrg]] {
      organisaatioServiceClient.getOrganisaatiotWithOidsFromCache(organisaatioOids)
    } match {
      case Success(organisaatiot: Seq[OrganisaatioServiceOrg]) => Right(organisaatiot)
      case Failure(exception)                        => Left(exception)
    }
  }

  def getOrganisaatioHierarkia(params: Params, multiParams: MultiParams): Either[Throwable, OrganisaatioHierarkia] = {
    Try[OrganisaatioHierarkia] {
      organisaatioServiceClient.getOrganisaatioHierarkiaFromCache(Some(params), Some(multiParams))
    } match {
      case Success(organisaatiohierarkia: OrganisaatioHierarkia) => Right(organisaatiohierarkia)
      case Failure(exception) => Left(exception)
    }
  }

  def getOppilaitoksetForAvoinKorkeakoulutus(): OrganisaatioHierarkia = {
    val filtered =
      organisaatioServiceClient
        .getOrganisaatioHierarkiaFromCache(None, None, oppilaitostyypitForAvoinKorkeakoulutus)
        .organisaatiot
        .map(_.copy(children = None))
    OrganisaatioHierarkia(organisaatiot = filtered)
  }

  def getOrganisaatioChildren(oid: OrganisaatioOid): Either[Throwable, Seq[OrganisaatioServiceOrg]] = {
    Try[Seq[OrganisaatioServiceOrg]] {
      organisaatioServiceClient.getOrganisaatioChildrenFromCache(oid)
    } match {
      case Success(organisaatiot: Seq[OrganisaatioServiceOrg]) => Right(organisaatiot)
      case Failure(exception) => Left(exception)
    }
  }
}
