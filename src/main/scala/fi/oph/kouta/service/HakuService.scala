package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.KoutaIndexClient
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeHaku}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, KoulutusDAO, KoutaDatabase, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

object HakuService extends HakuService(SqsInTransactionService, AuditLog)

class HakuService(sqsInTransactionService: SqsInTransactionService, auditLog: AuditLog)
  extends ValidatingService[Haku] with RoleEntityAuthorizationService {

  override val roleEntity: RoleEntity = Role.Haku
  protected val readRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles, true)

  def get(oid: HakuOid)(implicit authenticated: Authenticated): Option[(Haku, Instant)] =
    authorizeGet(HakuDAO.get(oid), readRules)

  def put(haku: Haku)(implicit authenticated: Authenticated): HakuOid =
    authorizePut(haku) {
      withValidation(haku, doPut)
    }.oid.get

  def update(haku: Haku, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(HakuDAO.get(haku.oid.get)) { oldHaku =>
      withValidation(haku, doUpdate(_, notModifiedSince, oldHaku))
    }.nonEmpty

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[HakuListItem] =
    withAuthorizedOrganizationOids(organisaatioOid, readRules)(HakuDAO.listByAllowedOrganisaatiot)

  def listHakukohteet(hakuOid: HakuOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withRootAccess(indexerRoles)(HakukohdeDAO.listByHakuOid(hakuOid))

  def listHakukohteet(hakuOid: HakuOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, Role.Hakukohde.readRoles) {
      HakukohdeDAO.listByHakuOidAndAllowedOrganisaatiot(hakuOid, _)
    }

  def listKoulutukset(hakuOid: HakuOid)(implicit authenticated: Authenticated): Seq[KoulutusListItem] =
    withRootAccess(indexerRoles)(KoulutusDAO.listByHakuOid(hakuOid))

  def listToteutukset(hakuOid: HakuOid)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
    withRootAccess(indexerRoles)(ToteutusDAO.listByHakuOid(hakuOid))

  def search(organisaatioOid: OrganisaatioOid, params: Map[String, String])(implicit authenticated: Authenticated): HakuSearchResult = {

    def assocHakukohdeCounts(r: HakuSearchResult): HakuSearchResult =
      r.copy(result = r.result.map {
        h => h.copy(hakukohteet = listHakukohteet(h.oid, organisaatioOid).size)
      })

    list(organisaatioOid).map(_.oid) match {
      case Nil      => HakuSearchResult()
      case hakuOids => assocHakukohdeCounts(KoutaIndexClient.searchHaut(hakuOids, params))
    }
  }

  private def doPut(haku: Haku)(implicit authenticated: Authenticated): Haku =
    KoutaDatabase.runBlockingTransactionally {
      for {
        added <- HakuDAO.getPutActions(haku)
        _     <- index(added)
        _     <- auditLog.logCreate(added)
      } yield added
    }.get

  private def doUpdate(haku: Haku, notModifiedSince: Instant, before: Haku)(implicit authenticated: Authenticated): Option[Haku] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _       <- HakuDAO.checkNotModified(haku.oid.get, notModifiedSince)
        updated <- HakuDAO.getUpdateActions(haku)
        _       <- index(updated)
        _       <- auditLog.logUpdate(before, updated)
      } yield updated
    }.get

  private def index(haku: Haku): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeHaku, haku.oid.get.toString)

  private def index(haku: Option[Haku]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeHaku, haku.map(_.oid.get.toString))
}
