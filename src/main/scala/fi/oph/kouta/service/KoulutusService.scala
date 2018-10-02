package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.Koulutus
import fi.oph.kouta.repository.KoulutusDAO

object KoulutusService {

    def put(koulutus:Koulutus): Option[String] = KoulutusDAO.put(koulutus)

    def update(koulutus:Koulutus, notModifiedSince:Instant): Boolean = KoulutusDAO.update(koulutus, notModifiedSince)

    def get(oid:String): Option[(Koulutus, Instant)] = KoulutusDAO.get(oid)
}
