package fi.oph.kouta.service

import fi.oph.kouta.client.{CachedOrganisaatioHierarkiaClient, CallerId, OrganisaatioServiceClient}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid}
import fi.oph.kouta.domain.{Organisaatio, OrganisaatioHierarkia, OrganisaatioServiceOrg, oppilaitostyypitForAvoinKorkeakoulutus}
import fi.oph.kouta.util.OrganisaatioServiceUtil
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
      case Failure(exception) => Left(exception)
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

  private def listDescendants(organisaatio: Organisaatio): List[Organisaatio] = {
    organisaatio.children match {
      case Some(children) =>
        children ::: children.flatMap(listDescendants)
      case None =>
        List()
    }
  }

  private def flattenOrganisaatioWithChildren(organisaatio: Organisaatio): (OrganisaatioOid, Organisaatio) = {
    (OrganisaatioOid(organisaatio.oid), organisaatio.copy(children = Some(listDescendants(organisaatio))))
  }

  def getOrganisaatioChildren(oid: OrganisaatioOid, yhteystiedotForOsat: Boolean = false): Either[Throwable, Seq[Organisaatio]] = {
    Try[OrganisaatioHierarkia] {
      organisaatioServiceClient.getOrganisaatioChildrenFromCache(oid)
    } match {
      case Success(organisaatioHierarkia: OrganisaatioHierarkia) =>
        val children = if (organisaatioHierarkia.organisaatiot.nonEmpty) {
          flattenOrganisaatioWithChildren(organisaatioHierarkia.organisaatiot.head)._2.children.getOrElse(List())
        } else List()

        val childrenWithYhteystiedot = if (yhteystiedotForOsat) {
          children.map(child => {
            getOrganisaatio(OrganisaatioOid(child.oid)) match {
              case Right(organisaatio: OrganisaatioServiceOrg) =>
                OrganisaatioServiceUtil.organisaatioServiceOrgToOrganisaatio(organisaatio)
            }
          })
        } else {
          children
        }

        Right(childrenWithYhteystiedot)
      case Failure(exception) => Left(exception)
    }
  }
}
