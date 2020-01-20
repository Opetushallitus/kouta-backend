package fi.oph.kouta.service

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.KoutaIndexClient
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.domain.{HakukohdeListItem, Valintaperuste, ValintaperusteListItem, ValintaperusteSearchResult}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeValintaperuste}
import fi.oph.kouta.repository.{HakukohdeDAO, KoutaDatabase, ValintaperusteDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

object ValintaperusteService extends ValintaperusteService(SqsInTransactionService, AuditLog)

class ValintaperusteService(sqsInTransactionService: SqsInTransactionService, auditLog: AuditLog) extends ValidatingService[Valintaperuste] with RoleEntityAuthorizationService {

  override val roleEntity: RoleEntity = Role.Valintaperuste
  protected val readRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles, true)

  def get(id: UUID)(implicit authenticated: Authenticated): Option[(Valintaperuste, Instant)] =
    authorizeGet(ValintaperusteDAO.get(id), AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true, Seq(AuthorizationRuleForJulkinen)))

  def put(valintaperuste: Valintaperuste)(implicit authenticated: Authenticated): UUID =
    authorizePut(valintaperuste) {
      withValidation(valintaperuste, doPut)
    }.id.get

  def update(valintaperuste: Valintaperuste, notModifiedSince: Instant)
            (implicit authenticated: Authenticated): Boolean = {
    authorizeUpdate(ValintaperusteDAO.get(valintaperuste.id.get)) {  oldValintaperuste =>
      withValidation(valintaperuste, doUpdate(_, notModifiedSince, oldValintaperuste)).nonEmpty
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

  private def doPut(valintaperuste: Valintaperuste)(implicit authenticated: Authenticated): Valintaperuste =
    KoutaDatabase.runBlockingTransactionally {
      for {
        added <- ValintaperusteDAO.getPutActions(valintaperuste)
        _     <- index(Some(added))
        _     <- auditLog.logCreate(added)
      } yield added
    }.get

  private def doUpdate(valintaperuste: Valintaperuste, notModifiedSince: Instant, before: Valintaperuste)(implicit authenticated: Authenticated): Option[Valintaperuste] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _       <- ValintaperusteDAO.checkNotModified(valintaperuste.id.get, notModifiedSince)
        updated <- ValintaperusteDAO.getUpdateActions(valintaperuste)
        _       <- index(updated)
        _       <- auditLog.logUpdate(before, updated)
      } yield updated
    }.get

  private def index(valintaperuste: Option[Valintaperuste]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeValintaperuste, valintaperuste.map(_.id.get.toString))
}
