package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.{Koulutus, ListParams, OidListResponse}
import fi.oph.kouta.repository.KoulutusDAO

object KoulutusService extends ValidatingService[Koulutus] {

    def put(koulutus:Koulutus): Option[String] = withValidation(koulutus, KoulutusDAO.put(_))

    def update(koulutus:Koulutus, notModifiedSince:Instant): Boolean = withValidation(koulutus, KoulutusDAO.update(_, notModifiedSince))

    def get(oid:String): Option[(Koulutus, Instant)] = KoulutusDAO.get(oid)

    def list(params:ListParams):List[OidListResponse] = KoulutusDAO.list(params)
}