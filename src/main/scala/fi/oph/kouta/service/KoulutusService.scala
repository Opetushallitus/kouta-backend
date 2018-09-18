package fi.oph.kouta.service

import fi.oph.kouta.domain.Koulutus
import fi.oph.kouta.repository.KoulutusDAO

object KoulutusService {

    def put(koulutus:Koulutus) = KoulutusDAO.put(koulutus)

    def get(oid:String): Option[Koulutus] = KoulutusDAO.get(oid)
}
