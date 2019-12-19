package fi.oph.kouta.service

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.client.KoutaIndexClient
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.domain.{HakukohdeListItem, Valintaperuste, ValintaperusteListItem, ValintaperusteSearchResult}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeValintaperuste}
import fi.oph.kouta.repository.{HakukohdeDAO, ValintaperusteDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated

object ValintaperusteService extends ValintaperusteService(SqsInTransactionService)

abstract class ValintaperusteService(sqsInTransactionService: SqsInTransactionService) extends ValidatingService[Valintaperuste] with RoleEntityAuthorizationService {

  override val roleEntity: RoleEntity = Role.Valintaperuste
  protected val readRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles, true)

  def get(id: UUID)(implicit authenticated: Authenticated): Option[(Valintaperuste, Instant)] =
    authorizeGet(ValintaperusteDAO.get(id), AuthorizationRules(roleEntity.readRoles, withParents = true, withTarjoajat = false, Seq(authorizationRuleForJulkinen)))

  def put(valintaperuste: Valintaperuste)(implicit authenticated: Authenticated): UUID =
    authorizePut(valintaperuste) {
      withValidation(valintaperuste, putWithIndexing)
    }

  def update(valintaperuste: Valintaperuste, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(ValintaperusteDAO.get(valintaperuste.id.get)) {
      withValidation(valintaperuste, updateWithIndexing(_, notModifiedSince))
    }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[ValintaperusteListItem] =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(organisaatioOid, readRules) { case (oids, koulutustyypit) =>
      ValintaperusteDAO.listAllowedByOrganisaatiot(oids, koulutustyypit)
    }

  def listByHaunKohdejoukko(organisaatioOid: OrganisaatioOid, hakuOid: HakuOid)(implicit authenticated: Authenticated): Seq[ValintaperusteListItem] =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(organisaatioOid, readRules) { case (oids, koulutustyypit) =>
      ValintaperusteDAO.listAllowedByOrganisaatiotAndHaunKohdejoukko(oids, koulutustyypit, hakuOid)
    }

  def listHakukohteet(valintaperusteId: UUID)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withRootAccess(Role.Hakukohde.readRoles) {
      HakukohdeDAO.listByValintaperusteId(valintaperusteId)
    }

  def search(organisaatioOid: OrganisaatioOid, params: Map[String, String])(implicit authenticated: Authenticated): ValintaperusteSearchResult =
    list(organisaatioOid).map(_.id) match {
      case Nil               => ValintaperusteSearchResult()
      case valintaperusteIds => KoutaIndexClient.searchValintaperusteet(valintaperusteIds, params)
    }

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
