package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeHaku}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, KoulutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}

object HakuService extends HakuService(SqsInTransactionService)

abstract class HakuService(sqsInTransactionService: SqsInTransactionService) extends ValidatingService[Haku] with AuthorizationService {

  override val roleEntity: RoleEntity = Role.Haku

  def put(haku: Haku): HakuOid =
    withValidation(haku, putWithIndexing)

  def update(haku: Haku, notModifiedSince: Instant): Boolean =
    withValidation(haku, updateWithIndexing(_, notModifiedSince))

  def get(oid: HakuOid): Option[(Haku, Instant)] = HakuDAO.get(oid)

  def list(organisaatioOid: OrganisaatioOid): Seq[HakuListItem] =
    withAuthorizedChildAndParentOrganizationOids(organisaatioOid, HakuDAO.listByOrganisaatioOids)

  def listHakukohteet(hakuOid: HakuOid, organisaatioOid: OrganisaatioOid): Seq[HakukohdeListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, HakukohdeDAO.listByHakuOidAndOrganisaatioOids(hakuOid, _))

  def listHakukohteet(hakuOid: HakuOid): Seq[HakukohdeListItem] =
    HakukohdeDAO.listByHakuOid(hakuOid)

  def listKoulutukset(hakuOid: HakuOid): Seq[KoulutusListItem] =
    KoulutusDAO.listByHakuOid(hakuOid)

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
