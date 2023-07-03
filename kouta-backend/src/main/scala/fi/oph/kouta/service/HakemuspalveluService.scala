package fi.oph.kouta.service

import fi.oph.kouta.client.HakemusPalveluClient
import fi.oph.kouta.domain.oid.HakukohdeOid

object HakemuspalveluService extends HakemuspalveluService(HakemusPalveluClient)

case class HakemuspalveluHakukohdeInfo(hakemustenMaara: Int)

class HakemuspalveluService(hakemusPalveluClient: HakemusPalveluClient) {

  def getHakukohdeInfo(hakukohdeOid: HakukohdeOid): HakemuspalveluHakukohdeInfo = {
    hakemusPalveluClient.getHakukohdeInfo(hakukohdeOid).toHakemuspalveluHakukohdeInfo
  }

}
