package fi.oph.kouta.mocks

import fi.oph.kouta.TestOids.{EvilCousin, EvilGrandChildOid, GrandChildOid, OphOid, OphUserOid}
import fi.oph.kouta.client.{KayttooikeusClient, OrganisaatioHenkilo}
import fi.oph.kouta.domain.oid.UserOid

object MockKayttooikeusClient extends KayttooikeusClient {
  override def getOrganisaatiot(oid: UserOid): List[OrganisaatioHenkilo] = {
    val organisaatiot = List(OrganisaatioHenkilo(GrandChildOid.s), OrganisaatioHenkilo(EvilCousin.s))
    if (oid.equals(OphUserOid)) {
      OrganisaatioHenkilo(OphOid.s) :: organisaatiot
    } else {
      organisaatiot
    }
  }
}
