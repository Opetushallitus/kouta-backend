package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.Hakukohde
import fi.oph.kouta.domain.oid.HakukohdeOid
import fi.oph.kouta.indexing.SqsInTransactionService.runActionAndUpdateIndex
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeHakukohde}
import fi.oph.kouta.repository.HakukohdeDAO

object HakukohdeService extends ValidatingService[Hakukohde] {

  def put(hakukohde: Hakukohde): HakukohdeOid =
    withValidation(hakukohde, putWithIndexing)

  def update(hakukohde: Hakukohde, notModifiedSince:Instant): Boolean =
    withValidation(hakukohde, updateWithIndexing(_, notModifiedSince))

  def get(oid: HakukohdeOid): Option[(Hakukohde, Instant)] = HakukohdeDAO.get(oid)

  private def putWithIndexing(hakukohde: Hakukohde) =
    runActionAndUpdateIndex(
      HighPriority,
      IndexTypeHakukohde,
      () => HakukohdeDAO.getPutActions(hakukohde))

  private def updateWithIndexing(hakukohde: Hakukohde, notModifiedSince: Instant) =
    runActionAndUpdateIndex(
      HighPriority,
      IndexTypeHakukohde,
      () => HakukohdeDAO.getUpdateActions(hakukohde, notModifiedSince), hakukohde.oid.get.toString)
}
