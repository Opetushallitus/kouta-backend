package fi.oph.kouta.service

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.domain.{HakukohdeListItem, IdListItem, Valintaperuste, ValintaperusteListItem}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeValintaperuste}
import fi.oph.kouta.repository.{HakukohdeDAO, ValintaperusteDAO}

object ValintaperusteService extends ValintaperusteService(SqsInTransactionService)

abstract class ValintaperusteService(sqsInTransactionService: SqsInTransactionService) extends ValidatingService[Valintaperuste] with AuthorizationService {

  def put(valintaperuste: Valintaperuste): UUID =
    withValidation(valintaperuste, putWithIndexing)

  def update(valintaperuste: Valintaperuste, notModifiedSince: Instant): Boolean =
    withValidation(valintaperuste, updateWithIndexing(_, notModifiedSince))

  def get(id: UUID): Option[(Valintaperuste, Instant)] = ValintaperusteDAO.get(id)

  def list(organisaatioOid: OrganisaatioOid): Seq[IdListItem] =
    withAuthorizedChildAndParentOrganizationOids(organisaatioOid, ValintaperusteDAO.listByOrganisaatioOids)

  def listByHaunKohdejoukko(organisaatioOid: OrganisaatioOid, hakuOid: HakuOid): Seq[ValintaperusteListItem] =
    withAuthorizedChildAndParentOrganizationOids(organisaatioOid, ValintaperusteDAO.ListByOrganisaatioOidAndHaunKohdejoukko(_, hakuOid))

  def listByValintaperusteId(valintaperusteId: UUID): Seq[HakukohdeListItem] =
    HakukohdeDAO.listByValintaperusteId(valintaperusteId)

  private def putWithIndexing(valintaperuste: Valintaperuste) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeValintaperuste,
      () => ValintaperusteDAO.getPutActions(valintaperuste))

  private def updateWithIndexing(valintaperuste: Valintaperuste, notModifiedSince: Instant) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeValintaperuste,
      () => ValintaperusteDAO.getUpdateActions(valintaperuste, notModifiedSince),
      valintaperuste.id.get.toString)
}
