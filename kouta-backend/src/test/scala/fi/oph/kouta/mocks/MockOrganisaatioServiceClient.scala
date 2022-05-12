package fi.oph.kouta.mocks

import fi.oph.kouta.TestOids._
import fi.oph.kouta.client.{KayttooikeusClient, OrgServiceOrganisaatio, OrganisaatioHenkilo, OrganisaatioHierarkia, OrganisaatioServiceClient, OrganisaationOsa}
import fi.oph.kouta.domain.{En, Fi, ListEverythingModel, Sv}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.security.{AuthenticationFailedException, Authority, KayttooikeusUserDetails, SecurityContext}

class MockOrganisaatioServiceClient(securityContext: SecurityContext, defaultAuthorities: Set[Authority]) extends OrganisaatioServiceClient {
  override def getOrganisaatioHierarkiaWithOids(oids: List[OrganisaatioOid]): OrganisaatioHierarkia = {
    OrganisaatioHierarkia(
      organisaatiot = List(
        OrgServiceOrganisaatio(
          oid = ChildOid.toString,
          parentOidPath = s"${ChildOid.toString}/1.2.246.562.10.97036773279/1.2.246.562.10.00000000001",
          oppilaitostyyppi = Some("oppilaitostyyppi_21#1"),
          nimi = Map(Fi -> "Organisaatio", Sv -> "Organisation", En -> "Organization"),
          kotipaikkaUri = "kunta_123",
          children = List(
            OrganisaationOsa(
              oid = GrandChildOid.toString,
              parentOidPath = s"${GrandChildOid.toString}/${ChildOid.toString}/1.2.246.562.10.97036773279/1.2.246.562.10.00000000001",
              nimi = Map(Fi -> "Organisation osa", Sv -> "Barn Organisation", En -> "Child Organization"),
              kotipaikkaUri = "kunta_123",
              children = List(),
              status = "AKTIIVINEN",
              organisaatiotyypit = List("organisaatiotyyppi_1")
            )
          )
        )
      )
    )
  }
}
