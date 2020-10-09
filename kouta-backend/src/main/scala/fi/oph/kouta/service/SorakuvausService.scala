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

object SorakuvausService extends SorakuvausService(SqsInTransactionService, AuditLog, OrganisaatioServiceImpl)

class SorakuvausService(sqsInTransactionService: SqsInTransactionService, auditLog: AuditLog, val organisaatioService: OrganisaatioService) extends ValidatingService[Sorakuvaus] with RoleEntityAuthorizationService[Sorakuvaus] {

  override val roleEntity: RoleEntity = Role.Valintaperuste
  protected val readRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true)
  protected val createRules: AuthorizationRules = AuthorizationRules(Seq(Role.Paakayttaja))
  protected val updateRules: AuthorizationRules = AuthorizationRules(Seq(Role.Paakayttaja))

  def get(id: UUID)(implicit authenticated: Authenticated): Option[(Sorakuvaus, Instant)] =
    authorizeGet(SorakuvausDAO.get(id), AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true, Seq(authorizationRuleByKoulutustyyppi)))

  def put(sorakuvaus: Sorakuvaus)(implicit authenticated: Authenticated): UUID =
    authorizePut(sorakuvaus, createRules) { s =>
      withValidation(s, None)(doPut)
    }.id.get

  def update(sorakuvaus: Sorakuvaus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(SorakuvausDAO.get(sorakuvaus.id.get), sorakuvaus, updateRules) { (oldSorakuvaus, s) =>
      withValidation(s, Some(oldSorakuvaus)) {
        doUpdate(_, notModifiedSince, oldSorakuvaus)
      }
    }.nonEmpty

  def listValintaperusteet(sorakuvausId: UUID)(implicit authenticated: Authenticated): Seq[ValintaperusteListItem] =
    withRootAccess(indexerRoles) {
      ValintaperusteDAO.listBySorakuvausId(sorakuvausId)
    }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[SorakuvausListItem] =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(organisaatioOid, readRules) { case (_, koulutustyypit) =>
      SorakuvausDAO.listByKoulutustyypit(koulutustyypit)
    }

  private def doPut(sorakuvaus: Sorakuvaus)(implicit authenticated: Authenticated): Sorakuvaus =
    KoutaDatabase.runBlockingTransactionally {
      for {
        s <- SorakuvausDAO.getPutActions(sorakuvaus)
        _ <- index(Some(s))
        _ <- auditLog.logCreate(s)
      } yield s
    }.get

  private def doUpdate(sorakuvaus: Sorakuvaus, notModifiedSince: Instant, before: Sorakuvaus)(implicit authenticated: Authenticated): Option[Sorakuvaus] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _ <- SorakuvausDAO.checkNotModified(sorakuvaus.id.get, notModifiedSince)
        s <- SorakuvausDAO.getUpdateActions(sorakuvaus)
        _ <- index(s)
        _ <- auditLog.logUpdate(before, s)
      } yield s
    }.get

  private def index(sorakuvaus: Option[Sorakuvaus]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeSorakuvaus, sorakuvaus.map(_.id.get.toString))
}
