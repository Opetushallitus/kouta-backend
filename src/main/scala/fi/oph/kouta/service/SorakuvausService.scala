package fi.oph.kouta.service

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Sorakuvaus, SorakuvausListItem, ValintaperusteListItem}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeSorakuvaus}
import fi.oph.kouta.repository.{KoutaDatabase, SorakuvausDAO, ValintaperusteDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

object SorakuvausService extends SorakuvausService(SqsInTransactionService, AuditLog)

class SorakuvausService(sqsInTransactionService: SqsInTransactionService, auditLog: AuditLog) extends ValidatingService[Sorakuvaus] with RoleEntityAuthorizationService {

  override val roleEntity: RoleEntity = Role.Valintaperuste
  protected val readRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles, true)

  def get(id: UUID)(implicit authenticated: Authenticated): Option[(Sorakuvaus, Instant)] =
    authorizeGet(SorakuvausDAO.get(id), AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true, Seq(AuthorizationRuleForJulkinen)))

  def put(sorakuvaus: Sorakuvaus)(implicit authenticated: Authenticated): UUID =
    authorizePut(sorakuvaus) {
      withValidation(sorakuvaus, doPut)
    }.id.get

  def update(sorakuvaus: Sorakuvaus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(SorakuvausDAO.get(sorakuvaus.id.get)) { oldSorakuvaus =>
      withValidation(sorakuvaus, doUpdate(_, notModifiedSince, oldSorakuvaus))
    }.nonEmpty

  def listValintaperusteet(sorakuvausId: UUID)(implicit authenticated: Authenticated): Seq[ValintaperusteListItem] =
    withRootAccess(indexerRoles) {
      ValintaperusteDAO.listBySorakuvausId(sorakuvausId)
    }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[SorakuvausListItem] =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(organisaatioOid, readRules) { case (oids, koulutustyypit) =>
      SorakuvausDAO.listAllowedByOrganisaatiot(oids, koulutustyypit)
    }

  private def doPut(sorakuvaus: Sorakuvaus)(implicit authenticated: Authenticated): Sorakuvaus =
    KoutaDatabase.runBlockingTransactionally {
      for {
        added <- SorakuvausDAO.getPutActions(sorakuvaus)
        _     <- index(Some(added))
        _     <- auditLog.logCreate(added)
      } yield added
    }.get

  private def doUpdate(sorakuvaus: Sorakuvaus, notModifiedSince: Instant, before: Sorakuvaus)(implicit authenticated: Authenticated): Option[Sorakuvaus] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _       <- SorakuvausDAO.checkNotModified(sorakuvaus.id.get, notModifiedSince)
        updated <- SorakuvausDAO.getUpdateActions(sorakuvaus)
        _       <- index(updated)
        _       <- auditLog.logUpdate(before, updated)
      } yield updated
    }.get

  private def index(sorakuvaus: Option[Sorakuvaus]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeSorakuvaus, sorakuvaus.map(_.id.get.toString))
}
