package fi.oph.kouta.service

import java.time.{Instant, LocalDateTime}
import java.util.UUID

import fi.oph.kouta.client.KoutaIndexClient
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.domain.{HakukohdeListItem, Valintaperuste, ValintaperusteListItem, ValintaperusteSearchResult}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeValintaperuste}
import fi.oph.kouta.repository.{HakukohdeDAO, ValintaperusteDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.{AuditLog, CreateAudit, Resource, UpdateAudit}
import javax.servlet.http.HttpServletRequest

object ValintaperusteService extends ValintaperusteService(SqsInTransactionService, AuditLog)

class ValintaperusteService(sqsInTransactionService: SqsInTransactionService, auditLog: AuditLog) extends ValidatingService[Valintaperuste] with RoleEntityAuthorizationService {

  override val roleEntity: RoleEntity = Role.Valintaperuste
  protected val readRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles, true)

  def get(id: UUID)(implicit authenticated: Authenticated): Option[(Valintaperuste, Instant)] =
    authorizeGet(ValintaperusteDAO.get(id), AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true, Seq(AuthorizationRuleForJulkinen)))

  def put(valintaperuste: Valintaperuste)(implicit authenticated: Authenticated, request: HttpServletRequest): UUID =
    authorizePut(valintaperuste) {
      withValidation(valintaperuste, putWithIndexing(_, CreateAudit[UUID, Valintaperuste](valintaperuste))._1)
    }

  def update(valintaperuste: Valintaperuste, notModifiedSince: Instant)
            (implicit authenticated: Authenticated, request: HttpServletRequest): Boolean = {
    val oldValintaperuste = ValintaperusteDAO.get(valintaperuste.id.get)
    authorizeUpdate(oldValintaperuste) {
      withValidation(valintaperuste, updateWithIndexing(_, notModifiedSince, UpdateAudit[UUID, Valintaperuste](oldValintaperuste.get._1, valintaperuste))._1)
    }
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

  private def putWithIndexing(valintaperuste: Valintaperuste, createAudit: CreateAudit[UUID, Valintaperuste]) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeValintaperuste,
      () => ValintaperusteDAO.getPutActions(valintaperuste),
      (result: (UUID, LocalDateTime)) => result._1.toString,
      (result: (UUID, LocalDateTime)) => auditLog.logCreate(createAudit, Resource.Valintaperuste, result._1, result._2))

  private def updateWithIndexing(valintaperuste: Valintaperuste, notModifiedSince: Instant, auditUpdate: UpdateAudit[UUID, Valintaperuste]) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeValintaperuste,
      () => ValintaperusteDAO.getUpdateActions(valintaperuste, notModifiedSince),
      valintaperuste.id.get.toString,
      (result: (Boolean, LocalDateTime)) => auditLog.logUpdate(auditUpdate, Resource.Valintaperuste, result._1, result._2))
}
