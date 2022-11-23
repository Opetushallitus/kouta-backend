package fi.oph.kouta.service

import fi.oph.kouta.client.{CachedOrganisaatioHierarkiaClient, CallerId, OrganisaatioServiceClient}
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid}
import fi.oph.kouta.domain.{Organisaatio, OrganisaatioHierarkia, oppilaitostyypitForAvoinKorkeakoulutus}
import fi.oph.kouta.util.MiscUtils
import fi.vm.sade.properties.OphProperties
import org.scalatra.Params

object OrganisaatioServiceImpl extends OrganisaatioServiceImpl(KoutaConfigurationFactory.configuration.urlProperties)

class OrganisaatioServiceImpl(urlProperties: OphProperties, organisaatioServiceClient: OrganisaatioServiceClient) extends OrganisaatioService {
  def this(urlProperties: OphProperties) = this(urlProperties, new OrganisaatioServiceClient)

  override protected val cachedOrganisaatioHierarkiaClient: CachedOrganisaatioHierarkiaClient =
    new CachedOrganisaatioHierarkiaClient with CallerId {
      override val organisaatioUrl: String = urlProperties.url("organisaatio-service.organisaatio.oid.jalkelaiset", RootOrganisaatioOid.s)
    }

  def getOrganisaatioHierarkiaWithOids(oids: List[OrganisaatioOid]): OrganisaatioHierarkia =
    organisaatioServiceClient.getOrganisaatioHierarkiaWithOidsFromCache(oids)

  def getOrganisaatio(organisaatioOid: OrganisaatioOid) = {
    organisaatioServiceClient.getOrganisaatioWithOidFromCache(organisaatioOid)
  }

  def getOrganisaatiot(organisaatioOids: List[OrganisaatioOid]): Seq[Organisaatio] = {
    organisaatioServiceClient.getOrganisaatiotWithOidsFromCache(organisaatioOids)
  }

  def getOrganisaatioHierarkia(params: Params) = {
    organisaatioServiceClient.getOrganisaatioHierarkiaFromCache(Some(params))
  }

  def getOppilaitoksetForAvoinKorkeakoulutus(): OrganisaatioHierarkia = {
    val oppilaitosOrganisaatiotyyppi = "organisaatiotyyppi_02"
    val oppilaitostyypit = oppilaitostyypitForAvoinKorkeakoulutus
    val filtered = MiscUtils.filterOrganisaatiotWithOrganisaatiotyyppi(
      organisaatioServiceClient.getOrganisaatioHierarkiaFromCache(None, oppilaitostyypit).organisaatiot,
      oppilaitosOrganisaatiotyyppi
    )
    OrganisaatioHierarkia(organisaatiot = filtered)
  }
}
