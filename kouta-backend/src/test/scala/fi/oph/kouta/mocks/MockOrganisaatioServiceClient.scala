package fi.oph.kouta.mocks

import fi.oph.kouta.TestOids._
import fi.oph.kouta.client.{KayttooikeusClient, OrgServiceOrganisaatio, OrganisaatioHenkilo, OrganisaatioHierarkia, OrganisaatioServiceClient, OrganisaationOsa}
import fi.oph.kouta.domain.{En, Fi, ListEverythingModel, Sv}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.security.{AuthenticationFailedException, Authority, KayttooikeusUserDetails, SecurityContext}

class MockOrganisaatioServiceClient(securityContext: SecurityContext, defaultAuthorities: Set[Authority]) extends OrganisaatioServiceClient {
  override def getOrganisaatioHierarkiaWithOid(oid: OrganisaatioOid): OrganisaatioHierarkia = {
    OrganisaatioHierarkia(
      organisaatiot = List(
        OrgServiceOrganisaatio(
          oid = ChildOid.toString,
          oppilaitostyyppi = "oppilaitostyyppi_21#1",
          nimi = Map(Fi -> "Organisaatio", Sv -> "Organisation", En -> "Organization"),
          kotipaikkaUri = "kunta_123",
          children = List(
            OrganisaationOsa(
              oid = GrandChildOid.toString,
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
