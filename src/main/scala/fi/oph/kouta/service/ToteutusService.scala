package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeToteutus}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated

object ToteutusService extends ToteutusService(SqsInTransactionService)

abstract class ToteutusService(sqsInTransactionService: SqsInTransactionService) extends ValidatingService[Toteutus] with AuthorizationService {

  protected val roleEntity: RoleEntity = Role.Toteutus

  def get(oid: ToteutusOid)(implicit authenticated: Authenticated): Option[(Toteutus, Instant)] =
    authorizeGet(ToteutusDAO.get(oid))

  def put(toteutus: Toteutus)(implicit authenticated: Authenticated): ToteutusOid =
    authorizePut(toteutus) {
      withValidation(toteutus, putWithIndexing)
    }

  def update(toteutus: Toteutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(ToteutusDAO.get(toteutus.oid.get).map(_._1)) {
      withValidation(toteutus, updateWithIndexing(_, notModifiedSince))
    }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, roleEntity.readRoles)(ToteutusDAO.listByOrganisaatioOids)

  def listHaut(oid: ToteutusOid)(implicit authenticated: Authenticated): Seq[HakuListItem] =
    withRootAccess(Role.Haku.readRoles)(HakuDAO.listByToteutusOid(oid))

  def listHakukohteet(oid: ToteutusOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withRootAccess(Role.Hakukohde.readRoles)(HakukohdeDAO.listByToteutusOid(oid))

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
