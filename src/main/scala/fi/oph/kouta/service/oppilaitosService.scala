package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{OppilaitoksenOsa, OppilaitoksenOsaListItem, Oppilaitos}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeOppilaitos}
import fi.oph.kouta.repository.{OppilaitoksenOsaDAO, OppilaitosDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated

object OppilaitosService extends OppilaitosService(SqsInTransactionService)

abstract class OppilaitosService(sqsInTransactionService: SqsInTransactionService) extends ValidatingService[Oppilaitos] with RoleEntityAuthorizationService {

  protected val roleEntity: RoleEntity = Role.Oppilaitos

  def get(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Option[(Oppilaitos, Instant)] =
    authorizeGet(OppilaitosDAO.get(oid))

  def put(oppilaitos: Oppilaitos)(implicit authenticated: Authenticated): OrganisaatioOid =
    authorizePut(oppilaitos) {
      withValidation(oppilaitos, putWithIndexing)
    }

  def update(oppilaitos: Oppilaitos, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(OppilaitosDAO.get(oppilaitos.oid)) {
      withValidation(oppilaitos, updateWithIndexing(_, notModifiedSince))
    }

  def getOppilaitoksenOsat(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[OppilaitoksenOsa] =
    withRootAccess(indexerRoles)(OppilaitoksenOsaDAO.getByOppilaitosOid(oid))

  def listOppilaitoksenOsat(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[OppilaitoksenOsaListItem] =
    withRootAccess(Role.Oppilaitos.readRoles)(OppilaitoksenOsaDAO.listByOppilaitosOid(oid))

  def listOppilaitoksenOsat(oid: OrganisaatioOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[OppilaitoksenOsaListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, Role.Oppilaitos.readRoles) {
      OppilaitoksenOsaDAO.listByOppilaitosOidAndOrganisaatioOids(oid, _)
    }

  private def putWithIndexing(oppilaitos: Oppilaitos) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeOppilaitos,
      () => OppilaitosDAO.getPutActions(oppilaitos))

  private def updateWithIndexing(oppilaitos: Oppilaitos, notModifiedSince: Instant) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeOppilaitos,
      () => OppilaitosDAO.getUpdateActions(oppilaitos, notModifiedSince),
      oppilaitos.oid.toString)
}

object OppilaitoksenOsaService extends OppilaitoksenOsaService(SqsInTransactionService)

abstract class OppilaitoksenOsaService(sqsInTransactionService: SqsInTransactionService) extends ValidatingService[OppilaitoksenOsa] with RoleEntityAuthorizationService {

  protected val roleEntity: RoleEntity = Role.Oppilaitos

  def get(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Option[(OppilaitoksenOsa, Instant)] =
    authorizeGet(OppilaitoksenOsaDAO.get(oid))

  def put(oppilaitoksenOsa: OppilaitoksenOsa)(implicit authenticated: Authenticated): OrganisaatioOid =
    authorizePut(oppilaitoksenOsa) {
      withValidation(oppilaitoksenOsa, putWithIndexing)
    }

  def update(oppilaitoksenOsa: OppilaitoksenOsa, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(OppilaitoksenOsaDAO.get(oppilaitoksenOsa.oid)) {
      withValidation(oppilaitoksenOsa, updateWithIndexing(_, notModifiedSince))
    }

  private def putWithIndexing(oppilaitoksenOsa: OppilaitoksenOsa) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeOppilaitos,
      () => OppilaitoksenOsaDAO.getPutActions(oppilaitoksenOsa),
      oppilaitoksenOsa.oppilaitosOid.toString)

  private def updateWithIndexing(oppilaitoksenOsa: OppilaitoksenOsa, notModifiedSince: Instant) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeOppilaitos,
      () => OppilaitoksenOsaDAO.getUpdateActions(oppilaitoksenOsa, notModifiedSince),
      oppilaitoksenOsa.oppilaitosOid.toString)
}