package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.Hakukohde
import fi.oph.kouta.domain.oid.HakukohdeOid
import fi.oph.kouta.indexing.IndexingService
import fi.oph.kouta.repository.HakukohdeDAO
import fi.oph.kouta.util.WithSideEffect

object HakukohdeService extends ValidatingService[Hakukohde] with WithSideEffect {

  def put(hakukohde: Hakukohde): Option[HakukohdeOid] = {
    withSideEffect(withValidation(hakukohde, HakukohdeDAO.put)) {
      case oid => IndexingService.index(hakukohde.copy(oid = oid))
    }
  }

  def update(hakukohde: Hakukohde, notModifiedSince:Instant): Boolean = {
    withSideEffect(withValidation(hakukohde, HakukohdeDAO.update(_, notModifiedSince))) {
      case true => IndexingService.index(hakukohde)
    }
  }

  def get(oid: HakukohdeOid): Option[(Hakukohde, Instant)] = HakukohdeDAO.get(oid)
}
