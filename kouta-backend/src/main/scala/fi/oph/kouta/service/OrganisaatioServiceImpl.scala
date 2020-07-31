package fi.oph.kouta.service

import fi.oph.kouta.client.CachedOrganisaatioHierarkiaClient
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.domain.oid.RootOrganisaatioOid
import fi.vm.sade.properties.OphProperties

object OrganisaatioServiceImpl extends OrganisaatioServiceImpl(KoutaConfigurationFactory.configuration.urlProperties)

class OrganisaatioServiceImpl(urlProperties: OphProperties) extends OrganisaatioService {
  override protected val cachedOrganisaatioHierarkiaClient: CachedOrganisaatioHierarkiaClient =
    new CachedOrganisaatioHierarkiaClient {
      override val organisaatioUrl: String = urlProperties.url("organisaatio-service.organisaatio.oid.jalkelaiset", RootOrganisaatioOid.s)

      override def callerId: String = "kouta-backend"
    }
}
