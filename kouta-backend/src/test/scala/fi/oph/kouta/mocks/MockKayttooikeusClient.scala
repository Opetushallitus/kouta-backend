package fi.oph.kouta.mocks

import fi.oph.kouta.TestOids._
import fi.oph.kouta.client.{KayttooikeusClient, OrganisaatioHenkilo}
import fi.oph.kouta.domain.oid.UserOid
import fi.oph.kouta.security.{AuthenticationFailedException, Authority, KayttooikeusUserDetails, SecurityContext}

class MockKayttooikeusClient(securityContext: SecurityContext, defaultAuthorities: Set[Authority]) extends KayttooikeusClient {
  override def getUserByUsername(username: String): KayttooikeusUserDetails = {
    username match {
      case "testuser" => KayttooikeusUserDetails(defaultAuthorities, TestUserOid.s)
      case _ => throw new AuthenticationFailedException(s"User not found with username: $username")
    }
  }

  override def getOrganisaatiotFromCache(oid: UserOid): List[OrganisaatioHenkilo] = {
    val organisaatiot = List(OrganisaatioHenkilo(GrandChildOid.s), OrganisaatioHenkilo(EvilCousin.s))
    if (oid.equals(OphUserOid)) {
      OrganisaatioHenkilo(OphOid.s) :: organisaatiot
    } else {
      organisaatiot
    }
  }
}
