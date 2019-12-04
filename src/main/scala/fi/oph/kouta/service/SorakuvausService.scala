package fi.oph.kouta.service

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Sorakuvaus, SorakuvausListItem, ValintaperusteListItem}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeSorakuvaus}
import fi.oph.kouta.repository.{SorakuvausDAO, ValintaperusteDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.AuditLog
import fi.vm.sade.auditlog.User
import javax.servlet.http.HttpServletRequest

object SorakuvausService extends SorakuvausService(SqsInTransactionService, AuditLog)

class SorakuvausService(sqsInTransactionService: SqsInTransactionService, auditLog: AuditLog) extends ValidatingService[Sorakuvaus] with RoleEntityAuthorizationService {

  override val roleEntity: RoleEntity = Role.Valintaperuste
  protected val readRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles, true)

  def get(id: UUID)(implicit authenticated: Authenticated): Option[(Sorakuvaus, Instant)] =
    authorizeGet(SorakuvausDAO.get(id), AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true, Seq(AuthorizationRuleForJulkinen)))

  def put(sorakuvaus: Sorakuvaus)(implicit authenticated: Authenticated, request: HttpServletRequest): UUID =
    authorizePut(sorakuvaus) {
      withValidation(sorakuvaus, putWithIndexing(_, auditLog.getUser))
    }.id.get

  def update(sorakuvaus: Sorakuvaus, notModifiedSince: Instant)(implicit authenticated: Authenticated, request: HttpServletRequest): Boolean =
    authorizeUpdate(SorakuvausDAO.get(sorakuvaus.id.get)) { oldSorakuvaus =>
      withValidation(sorakuvaus, updateWithIndexing(_, notModifiedSince, auditLog.getUser, oldSorakuvaus))
    }.nonEmpty

  def listValintaperusteet(sorakuvausId: UUID)(implicit authenticated: Authenticated): Seq[ValintaperusteListItem] =
    withRootAccess(indexerRoles) {
      ValintaperusteDAO.listBySorakuvausId(sorakuvausId)
    }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[SorakuvausListItem] =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(organisaatioOid, readRules) { case (oids, koulutustyypit) =>
      SorakuvausDAO.listAllowedByOrganisaatiot(oids, koulutustyypit)
    }

  private def putWithIndexing(sorakuvaus: Sorakuvaus, user: User): Sorakuvaus =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeSorakuvaus,
      () => SorakuvausDAO.getPutActions(sorakuvaus),
      (added: Sorakuvaus) => added.id.get.toString,
      (added: Sorakuvaus) => auditLog.logCreate(added, user))

  private def updateWithIndexing(sorakuvaus: Sorakuvaus, notModifiedSince: Instant, user: User, before: Sorakuvaus) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeSorakuvaus,
      () => SorakuvausDAO.getUpdateActions(sorakuvaus, notModifiedSince),
      sorakuvaus.id.get.toString,
      (updated: Option[Sorakuvaus]) => auditLog.logUpdate(before, updated, user))
}
