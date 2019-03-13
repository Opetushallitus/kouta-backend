package fi.oph.kouta.service

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.domain.{HakukohdeListItem, IdListItem, Valintaperuste, ValintaperusteListItem}
import fi.oph.kouta.indexing.IndexingService
import fi.oph.kouta.repository.{HakukohdeDAO, ValintaperusteDAO}
import fi.oph.kouta.util.WithSideEffect

object ValintaperusteService extends ValidatingService[Valintaperuste] with AuthorizationService with WithSideEffect {

  def put(valintaperuste: Valintaperuste): Option[UUID] = {
    withSideEffect(withValidation(valintaperuste, v => ValintaperusteDAO.put(v.copy(id = Some(UUID.randomUUID))))) {
      case uuid => IndexingService.index(valintaperuste.copy(id=uuid))
    }
  }

  def update(valintaperuste: Valintaperuste, notModifiedSince: Instant): Boolean = {
    withSideEffect(withValidation(valintaperuste, ValintaperusteDAO.update(_, notModifiedSince))) {
      case true => IndexingService.index(valintaperuste)
    }
  }

  def get(id: UUID): Option[(Valintaperuste, Instant)] = ValintaperusteDAO.get(id)

  def list(organisaatioOid: OrganisaatioOid): Seq[IdListItem] =
    withAuthorizedChildAndParentOrganizationOids(organisaatioOid, ValintaperusteDAO.listByOrganisaatioOids)

  def listByHaunKohdejoukko(organisaatioOid: OrganisaatioOid, hakuOid: HakuOid): Seq[ValintaperusteListItem] =
    withAuthorizedChildAndParentOrganizationOids(organisaatioOid, ValintaperusteDAO.ListByOrganisaatioOidAndHaunKohdejoukko(_, hakuOid))

  def listByValintaperusteId(valintaperusteId: UUID): Seq[HakukohdeListItem] =
    HakukohdeDAO.listByValintaperusteId(valintaperusteId)
}
