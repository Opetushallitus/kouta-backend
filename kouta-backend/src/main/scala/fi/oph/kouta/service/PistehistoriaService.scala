package fi.oph.kouta.service

import fi.oph.kouta.domain.Pistetieto
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.repository.PistehistoriaDAO

object PistehistoriaService extends PistehistoriaService

class PistehistoriaService {
  def getPistehistoria(tarjoaja: OrganisaatioOid, hakukohdeKoodi: String): Seq[Pistetieto] = {
      PistehistoriaDAO.getPistehistoria(tarjoaja, hakukohdeKoodi)
    }
}
