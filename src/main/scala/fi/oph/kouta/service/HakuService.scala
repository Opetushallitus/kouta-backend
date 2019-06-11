package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeHaku}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, KoulutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated

object HakuService extends HakuService(SqsInTransactionService)

abstract class HakuService(sqsInTransactionService: SqsInTransactionService) extends ValidatingService[Haku] with RoleEntityAuthorizationService {

  override val roleEntity: RoleEntity = Role.Haku

  def get(oid: HakuOid)(implicit authenticated: Authenticated): Option[(Haku, Instant)] =
    authorizeGet(HakuDAO.get(oid))

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
    withAuthorizedChildOrganizationOids(organisaatioOid, roleEntity.readRoles)(HakuDAO.listByOrganisaatioOids)

  def listHakukohteet(hakuOid: HakuOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withRootAccess(Role.Hakukohde.readRoles)(HakukohdeDAO.listByHakuOid(hakuOid))

  def listHakukohteet(hakuOid: HakuOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, Role.Hakukohde.readRoles) {
      HakukohdeDAO.listByHakuOidAndOrganisaatioOids(hakuOid, _)
    }

  def listKoulutukset(hakuOid: HakuOid)(implicit authenticated: Authenticated): Seq[KoulutusListItem] =
    withRootAccess(Role.Koulutus.readRoles)(KoulutusDAO.listByHakuOid(hakuOid))

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
