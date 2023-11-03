package fi.oph.kouta.service

import fi.oph.kouta.client.{CachedOrganisaatioHierarkiaClient, CallerId, OrganisaatioServiceClient}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid}
import fi.oph.kouta.domain.{Organisaatio, OrganisaatioHierarkia, oppilaitostyypitForAvoinKorkeakoulutus}
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

  def getOrganisaatio(organisaatioOid: OrganisaatioOid) = {
    organisaatioServiceClient.getOrganisaatioWithOidFromCache(organisaatioOid)
  }

  def getOrganisaatiot(organisaatioOids: Seq[OrganisaatioOid]): Either[Throwable, Seq[Organisaatio]] = {
    Try[Seq[Organisaatio]] {
      organisaatioServiceClient.getOrganisaatiotWithOidsFromCache(organisaatioOids)
    } match {
      case Success(organisaatiot: Seq[Organisaatio]) => Right(organisaatiot)
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
        .map(_.copy(children = List()))
    OrganisaatioHierarkia(organisaatiot = filtered)
  }
}
