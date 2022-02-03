package fi.oph.kouta.util

import fi.oph.kouta.client.OrganisaatioHenkilo
import fi.oph.kouta.domain.oid.RootOrganisaatioOid

object ServiceUtils {
  def hasOphOrganisaatioOid(organisaatioHenkilot: List[OrganisaatioHenkilo]): Boolean = {
    organisaatioHenkilot.exists(organisaatioHenkilo => {
      organisaatioHenkilo.organisaatioOid.equals(RootOrganisaatioOid.toString)
    })
  }
}
