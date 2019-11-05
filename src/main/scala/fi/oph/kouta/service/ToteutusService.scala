package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.indexing.{S3Service, SqsInTransactionService}
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeToteutus}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated

object ToteutusService extends ToteutusService(SqsInTransactionService, S3Service)

class ToteutusService(sqsInTransactionService: SqsInTransactionService, val s3Service: S3Service)
  extends ValidatingService[Toteutus] with RoleEntityAuthorizationService with TeemakuvaService[ToteutusOid, Toteutus, ToteutusMetadata] {

  protected val roleEntity: RoleEntity = Role.Toteutus

  val teemakuvaPrefix: String = "toteutus-teemakuva"

  def get(oid: ToteutusOid)(implicit authenticated: Authenticated): Option[(Toteutus, Instant)] =
    authorizeGet(ToteutusDAO.get(oid))

  def put(toteutus: Toteutus)(implicit authenticated: Authenticated): ToteutusOid =
    authorizePut(toteutus) {
      withValidation(toteutus, checkTeemakuvaInPut(_, putWithIndexing, updateWithIndexing(_, Instant.now())))
    }

  def update(toteutus: Toteutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(ToteutusDAO.get(toteutus.oid.get)) {
      withValidation(toteutus, checkTeemakuvaInUpdate(_, updateWithIndexing(_, notModifiedSince)))
    }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, roleEntity.readRoles)(ToteutusDAO.listByOrganisaatioOids)

  def listHaut(oid: ToteutusOid)(implicit authenticated: Authenticated): Seq[HakuListItem] =
    withRootAccess(Role.Haku.readRoles)(HakuDAO.listByToteutusOid(oid))

  def listHakukohteet(oid: ToteutusOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withRootAccess(Role.Hakukohde.readRoles)(HakukohdeDAO.listByToteutusOid(oid))

  def listHakukohteet(oid: ToteutusOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] = {
    withAuthorizedChildOrganizationOids(organisaatioOid, Role.Hakukohde.readRoles) {
      HakukohdeDAO.listByToteutusOidAndOrganisaatioOids(oid, _)
    }
  }

  private def putWithIndexing(toteutus: Toteutus) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeToteutus,
      () => ToteutusDAO.getPutActions(toteutus))

  private def updateWithIndexing(toteutus: Toteutus, notModifiedSince: Instant) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeToteutus,
      () => ToteutusDAO.getUpdateActions(toteutus, notModifiedSince),
      toteutus.oid.get.toString)
}
