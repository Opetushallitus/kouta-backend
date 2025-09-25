package fi.oph.kouta.mocks

import fi.oph.kouta.TestOids._
import fi.oph.kouta.client.{KayttooikeusClient, OrganisaatioHenkilo}
import fi.oph.kouta.domain.oid.UserOid
import fi.oph.kouta.security.{AuthenticationFailedException, Authority, KayttooikeusUserDetails, SecurityContext}

class MockKayttooikeusClient extends KayttooikeusClient {

  override def getOrganisaatiotFromCache(oid: UserOid): List[OrganisaatioHenkilo] = {
    val organisaatiot = List(OrganisaatioHenkilo(GrandChildOid.s), OrganisaatioHenkilo(EvilCousin.s))
    if (oid.equals(OphUserOid) || oid.equals(OphUserOid2)) {
      OrganisaatioHenkilo(OphOid.s) :: organisaatiot
    } else {
      organisaatiot
    }
  }
}
