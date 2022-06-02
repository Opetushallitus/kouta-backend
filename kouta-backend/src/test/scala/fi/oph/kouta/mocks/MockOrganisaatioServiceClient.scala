package fi.oph.kouta.mocks

import fi.oph.kouta.TestData.orgServiceOrganisaatio
import fi.oph.kouta.client.OrganisaatioServiceClient
import fi.oph.kouta.domain.OrganisaatioHierarkia
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.security.{Authority, SecurityContext}

class MockOrganisaatioServiceClient(securityContext: SecurityContext, defaultAuthorities: Set[Authority]) extends OrganisaatioServiceClient {
  override def getOrganisaatioHierarkiaWithOids(oids: List[OrganisaatioOid]): OrganisaatioHierarkia = {
    val orgs = oids.map(oid => {
      orgServiceOrganisaatio.copy(oid = oid.toString)
    })
    OrganisaatioHierarkia(
      organisaatiot = orgs
    )
  }
}
