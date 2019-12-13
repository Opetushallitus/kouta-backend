package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.client.KoutaIndexClient
import fi.oph.kouta.domain.{Hakukohde, HakukohdeListItem, HakukohdeSearchResult}
import fi.oph.kouta.domain.oid.{HakukohdeOid, OrganisaatioOid}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeHakukohde}
import fi.oph.kouta.repository.HakukohdeDAO
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated

object HakukohdeService extends HakukohdeService(SqsInTransactionService)

abstract class HakukohdeService(sqsInTransactionService: SqsInTransactionService) extends ValidatingService[Hakukohde] with RoleEntityAuthorizationService {

  protected val roleEntity: RoleEntity = Role.Hakukohde

  def get(oid: HakukohdeOid)(implicit authenticated: Authenticated): Option[(Hakukohde, Instant)] =
    authorizeGet(HakukohdeDAO.get(oid))

  def put(hakukohde: Hakukohde)(implicit authenticated: Authenticated): HakukohdeOid =
    authorizePut(hakukohde) {
      withValidation(hakukohde, putWithIndexing)
    }

  def update(hakukohde: Hakukohde, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(HakukohdeDAO.get(hakukohde.oid.get)) {
      withValidation(hakukohde, updateWithIndexing(_, notModifiedSince))
    }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, roleEntity.readRoles)(HakukohdeDAO.listByOrganisaatioOids)

  def search(organisaatioOid: OrganisaatioOid, params: Map[String, String])(implicit authenticated: Authenticated): HakukohdeSearchResult =
    list(organisaatioOid).map(_.oid) match {
      case Nil           => HakukohdeSearchResult()
      case hakukohdeOids => KoutaIndexClient.searchHakukohteet(hakukohdeOids, params)
    }

  private def putWithIndexing(hakukohde: Hakukohde) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeHakukohde,
      () => HakukohdeDAO.getPutActions(hakukohde))

  private def updateWithIndexing(hakukohde: Hakukohde, notModifiedSince: Instant) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeHakukohde,
      () => HakukohdeDAO.getUpdateActions(hakukohde, notModifiedSince),
      hakukohde.oid.get.toString)
}
