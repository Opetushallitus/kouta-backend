package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.Hakukohde
import fi.oph.kouta.domain.oid.HakukohdeOid
import fi.oph.kouta.indexing.IndexingService
import fi.oph.kouta.repository.HakukohdeDAO

object HakukohdeService extends ValidatingService[Hakukohde] {

  def put(hakukohde: Hakukohde): Option[HakukohdeOid] = {
    val oid = withValidation(hakukohde, HakukohdeDAO.put)
    IndexingService.index(hakukohde.copy(oid = oid))
    oid
  }

  def update(hakukohde: Hakukohde, notModifiedSince:Instant): Boolean = {
    val updated = withValidation(hakukohde, HakukohdeDAO.update(_, notModifiedSince))
    if (updated) IndexingService.index(hakukohde)
    updated
  }

  def get(oid: HakukohdeOid): Option[(Hakukohde, Instant)] = HakukohdeDAO.get(oid)
}
