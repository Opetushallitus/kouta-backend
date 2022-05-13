package fi.oph.kouta.mocks

import fi.oph.kouta.TestData.orgServiceOrganisaatio
import fi.oph.kouta.TestOids._
import fi.oph.kouta.client.{KayttooikeusClient, OrgServiceOrganisaatio, OrganisaatioHenkilo, OrganisaatioHierarkia, OrganisaatioServiceClient, OrganisaationOsa}
import fi.oph.kouta.domain.{En, Fi, ListEverythingModel, Sv}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.security.{AuthenticationFailedException, Authority, KayttooikeusUserDetails, SecurityContext}

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
