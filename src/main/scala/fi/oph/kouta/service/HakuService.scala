package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.client.KoutaIndexClient
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeHaku}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, KoulutusDAO, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.{AuditLog, Resource}
import fi.vm.sade.auditlog.User
import javax.servlet.http.HttpServletRequest

object HakuService extends HakuService(SqsInTransactionService, AuditLog)

class HakuService(sqsInTransactionService: SqsInTransactionService, auditLog: AuditLog)
  extends ValidatingService[Haku] with RoleEntityAuthorizationService {

  override val roleEntity: RoleEntity = Role.Haku
  protected val readRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles, true)

  def get(oid: HakuOid)(implicit authenticated: Authenticated): Option[(Haku, Instant)] =
    authorizeGet(HakuDAO.get(oid), readRules)

  def put(haku: Haku)(implicit authenticated: Authenticated, request: HttpServletRequest): HakuOid = {
    authorizePut(haku) {
      withValidation(haku, putWithIndexing(_, auditLog.getUser))
    }
  }.oid.get

  def update(haku: Haku, notModifiedSince: Instant)(implicit authenticated: Authenticated, request: HttpServletRequest): Boolean = {
    authorizeUpdate(HakuDAO.get(haku.oid.get)) { oldHaku =>
      withValidation(haku, updateWithIndexing(_, notModifiedSince, auditLog.getUser, oldHaku))
    }
  }._1

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

  private def putWithIndexing(haku: Haku, user: User): Haku =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeHaku,
      () => HakuDAO.getPutActions(haku),
      (added: Haku) => added.oid.get.toString,
      (added: Haku) => auditLog.logCreate(added, user, Resource.Haku))

  private def updateWithIndexing(haku: Haku, notModifiedSince: Instant, user: User, before: Haku): (Boolean, Haku) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeHaku,
      () => HakuDAO.getUpdateActions(haku, notModifiedSince),
      haku.oid.get.toString,
      (result: (Boolean, Haku)) => auditLog.logUpdate(before, result._2, user, Resource.Haku, result._1))
}
