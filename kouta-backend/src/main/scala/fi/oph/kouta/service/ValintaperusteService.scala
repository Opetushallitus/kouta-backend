package fi.oph.kouta.service

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.KoutaIndexClient
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.domain.{HakukohdeListItem, Koulutustyyppi, Valintaperuste, ValintaperusteListItem, ValintaperusteSearchResult}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeValintaperuste}
import fi.oph.kouta.repository.{HakukohdeDAO, KoutaDatabase, SorakuvausDAO, ValintaperusteDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.validation.Validations
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

object ValintaperusteService extends ValintaperusteService(SqsInTransactionService, AuditLog, OrganisaatioServiceImpl)

class ValintaperusteService(sqsInTransactionService: SqsInTransactionService, auditLog: AuditLog, val organisaatioService: OrganisaatioService)
  extends ValidatingService[Valintaperuste] with RoleEntityAuthorizationService[Valintaperuste] {

  override val roleEntity: RoleEntity = Role.Valintaperuste
  protected val readRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true)

  def get(id: UUID)(implicit authenticated: Authenticated): Option[(Valintaperuste, Instant)] =
    authorizeGet(ValintaperusteDAO.get(id), AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true, Seq(AuthorizationRuleForJulkinen)))

  def put(valintaperuste: Valintaperuste)(implicit authenticated: Authenticated): UUID =
    authorizePut(valintaperuste) { v =>
      withValidation(v, None) { v =>
        doPut(v)
      }
    }.id.get

  def update(valintaperuste: Valintaperuste, notModifiedSince: Instant)
            (implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(ValintaperusteDAO.get(valintaperuste.id.get), valintaperuste) { (oldValintaperuste, v) =>
      withValidation(v, Some(oldValintaperuste)) { v =>
        doUpdate(v, notModifiedSince, oldValintaperuste)
      }
    }.nonEmpty

  def list(organisaatioOid: OrganisaatioOid, myosArkistoidut: Boolean)(implicit authenticated: Authenticated): Seq[ValintaperusteListItem] =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(organisaatioOid, readRules) { case (oids, koulutustyypit) =>
      ValintaperusteDAO.listAllowedByOrganisaatiot(oids, koulutustyypit, myosArkistoidut)
    }

  def listByHaunKohdejoukko(organisaatioOid: OrganisaatioOid, hakuOid: HakuOid, myosArkistoidut: Boolean)(implicit authenticated: Authenticated): Seq[ValintaperusteListItem] =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(organisaatioOid, readRules) { case (oids, koulutustyypit) =>
      ValintaperusteDAO.listAllowedByOrganisaatiotAndHaunKohdejoukko(oids, koulutustyypit, hakuOid, myosArkistoidut)
    }

  def listHakukohteet(valintaperusteId: UUID)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withRootAccess(Role.Hakukohde.readRoles) {
      HakukohdeDAO.listByValintaperusteId(valintaperusteId)
    }

  def search(organisaatioOid: OrganisaatioOid, params: Map[String, String])(implicit authenticated: Authenticated): ValintaperusteSearchResult =
    list(organisaatioOid, myosArkistoidut = true).map(_.id) match {
      case Nil               => ValintaperusteSearchResult()
      case valintaperusteIds => KoutaIndexClient.searchValintaperusteet(valintaperusteIds, params)
    }

  private def doPut(valintaperuste: Valintaperuste)(implicit authenticated: Authenticated): Valintaperuste =
    KoutaDatabase.runBlockingTransactionally {
      for {
        v <- ValintaperusteDAO.getPutActions(valintaperuste)
        _ <- index(Some(v))
        _ <- auditLog.logCreate(v)
      } yield v
    }.get

  private def doUpdate(valintaperuste: Valintaperuste, notModifiedSince: Instant, before: Valintaperuste)(implicit authenticated: Authenticated): Option[Valintaperuste] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _ <- ValintaperusteDAO.checkNotModified(valintaperuste.id.get, notModifiedSince)
        v <- ValintaperusteDAO.getUpdateActions(valintaperuste)
        _ <- index(v)
        _ <- auditLog.logUpdate(before, v)
      } yield v
    }.get

  private def index(valintaperuste: Option[Valintaperuste]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeValintaperuste, valintaperuste.map(_.id.get.toString))
}
