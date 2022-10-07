package fi.oph.kouta.mocks

import fi.oph.kouta.client.{Henkilo, OppijanumerorekisteriClient}
import fi.oph.kouta.domain.oid.UserOid

class MockOppijanumerorekisteriClient extends OppijanumerorekisteriClient {
  override def getHenkilöFromCache(oid: UserOid): Henkilo = {
    Henkilo(kutsumanimi = Some("Testi"), sukunimi = Some("Muokkaaja"), etunimet = Some("Testi Tyyppi"))
  }
}
