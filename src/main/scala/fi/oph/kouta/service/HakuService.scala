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

object HakuService extends HakuService(SqsInTransactionService)

abstract class HakuService(sqsInTransactionService: SqsInTransactionService) extends ValidatingService[Haku] with RoleEntityAuthorizationService {

  override val roleEntity: RoleEntity = Role.Haku
  protected val readRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles, true)

  def get(oid: HakuOid)(implicit authenticated: Authenticated): Option[(Haku, Instant)] =
    authorizeGet(HakuDAO.get(oid), AuthorizationRules(roleEntity.readRoles, true))

  def put(haku: Haku)(implicit authenticated: Authenticated): HakuOid = {
    authorizePut(haku) {
      withValidation(haku, putWithIndexing)
    }
  }

  def update(haku: Haku, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(HakuDAO.get(haku.oid.get)) {
      withValidation(haku, updateWithIndexing(_, notModifiedSince))
    }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[HakuListItem] =
    withAuthorizedOrganizationOids(organisaatioOid, readRules)(HakuDAO.listByOrganisaatioOids)

  def listHakukohteet(hakuOid: HakuOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withRootAccess(indexerRoles)(HakukohdeDAO.listByHakuOid(hakuOid))

  def listHakukohteet(hakuOid: HakuOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, Role.Hakukohde.readRoles) {
      HakukohdeDAO.listByHakuOidAndOrganisaatioOids(hakuOid, _)
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

  private def putWithIndexing(haku: Haku) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeHaku,
      () => HakuDAO.getPutActions(haku))

  private def updateWithIndexing(haku: Haku, notModifiedSince: Instant) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeHaku,
      () => HakuDAO.getUpdateActions(haku, notModifiedSince),
      haku.oid.get.toString)
}
