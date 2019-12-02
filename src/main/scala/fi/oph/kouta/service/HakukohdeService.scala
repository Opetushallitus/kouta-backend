package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.client.KoutaIndexClient
import fi.oph.kouta.domain.oid.{HakukohdeOid, OrganisaatioOid}
import fi.oph.kouta.domain.{Hakukohde, HakukohdeListItem, HakukohdeSearchResult}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeHakukohde}
import fi.oph.kouta.repository.{HakukohdeDAO, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.AuditLog
import fi.vm.sade.auditlog.User
import javax.servlet.http.HttpServletRequest

object HakukohdeService extends HakukohdeService(SqsInTransactionService, AuditLog)

class HakukohdeService(sqsInTransactionService: SqsInTransactionService, auditLog: AuditLog) extends ValidatingService[Hakukohde] with RoleEntityAuthorizationService {

  protected val roleEntity: RoleEntity = Role.Hakukohde

  def get(oid: HakukohdeOid)(implicit authenticated: Authenticated): Option[(Hakukohde, Instant)] =
    authorizeGet(HakukohdeDAO.get(oid), AuthorizationRules(roleEntity.readRoles, additionalAuthorizedOrganisaatioOids = ToteutusDAO.getTarjoajatByHakukohdeOid(oid)))

  def put(hakukohde: Hakukohde)(implicit authenticated: Authenticated, request: HttpServletRequest): HakukohdeOid =
    authorizePut(hakukohde) {
      withValidation(hakukohde, putWithIndexing(_, auditLog.getUser))
    }.oid.get

  def update(hakukohde: Hakukohde, notModifiedSince: Instant)(implicit authenticated: Authenticated, request: HttpServletRequest): Boolean = {
    val rules = AuthorizationRules(roleEntity.updateRoles, additionalAuthorizedOrganisaatioOids = ToteutusDAO.getTarjoajatByHakukohdeOid(hakukohde.oid.get))
    authorizeUpdate(HakukohdeDAO.get(hakukohde.oid.get), rules) { oldHakuKohde =>
      withValidation(hakukohde, updateWithIndexing(_, notModifiedSince, auditLog.getUser, oldHakuKohde))
    }.isDefined
  }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, roleEntity.readRoles)(HakukohdeDAO.listByAllowedOrganisaatiot)

  def search(organisaatioOid: OrganisaatioOid, params: Map[String, String])(implicit authenticated: Authenticated): HakukohdeSearchResult =
    list(organisaatioOid).map(_.oid) match {
      case Nil           => HakukohdeSearchResult()
      case hakukohdeOids => KoutaIndexClient.searchHakukohteet(hakukohdeOids, params)
    }

  private def putWithIndexing(hakukohde: Hakukohde, user: User): Hakukohde =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeHakukohde,
      () => HakukohdeDAO.getPutActions(hakukohde),
      (added: Hakukohde) => added.oid.get.toString,
      (added: Hakukohde) => auditLog.logCreate(added, user))

  private def updateWithIndexing(hakukohde: Hakukohde, notModifiedSince: Instant, user: User, before: Hakukohde) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeHakukohde,
      () => HakukohdeDAO.getUpdateActions(hakukohde, notModifiedSince),
      hakukohde.oid.get.toString,
      (updated: Option[Hakukohde]) => auditLog.logUpdate(before, updated, user))
}
