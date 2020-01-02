package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeOppilaitos}
import fi.oph.kouta.indexing.{S3Service, SqsInTransactionService}
import fi.oph.kouta.repository.{OppilaitoksenOsaDAO, OppilaitosDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import fi.vm.sade.auditlog.User
import javax.servlet.http.HttpServletRequest

object OppilaitosService extends OppilaitosService(SqsInTransactionService, S3Service, AuditLog)

class OppilaitosService(sqsInTransactionService: SqsInTransactionService, val s3Service: S3Service, auditLog: AuditLog)
  extends ValidatingService[Oppilaitos] with RoleEntityAuthorizationService with TeemakuvaService[OrganisaatioOid, Oppilaitos, OppilaitosMetadata] {

  protected val roleEntity: RoleEntity = Role.Oppilaitos

  val teemakuvaPrefix = "oppilaitos-teemakuva"

  def get(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Option[(Oppilaitos, Instant)] =
    authorizeGet(OppilaitosDAO.get(oid))

  def put(oppilaitos: Oppilaitos)(implicit authenticated: Authenticated, request: HttpServletRequest): OrganisaatioOid = {
    authorizePut(oppilaitos) {
      withValidation(oppilaitos, putWithIndexing(_, auditLog.getUser))
    }.oid
  }

  def update(oppilaitos: Oppilaitos, notModifiedSince: Instant)(implicit authenticated: Authenticated, request: HttpServletRequest): Boolean =
    authorizeUpdate(OppilaitosDAO.get(oppilaitos.oid)) { oldOppilaitos =>
      withValidation(oppilaitos, updateWithIndexing(_, notModifiedSince, auditLog.getUser, oldOppilaitos))
    }.nonEmpty

  def getOppilaitoksenOsat(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[OppilaitoksenOsa] =
    withRootAccess(indexerRoles)(OppilaitoksenOsaDAO.getByOppilaitosOid(oid))

  def listOppilaitoksenOsat(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[OppilaitoksenOsaListItem] =
    withRootAccess(indexerRoles)(OppilaitoksenOsaDAO.listByOppilaitosOid(oid))

  def listOppilaitoksenOsat(oid: OrganisaatioOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[OppilaitoksenOsaListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, Role.Oppilaitos.readRoles) {
      OppilaitoksenOsaDAO.listByOppilaitosOidAndOrganisaatioOids(oid, _)
    }

  private def putWithIndexing(oppilaitos: Oppilaitos, user: User) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeOppilaitos,
      () => themeImagePutActions(oppilaitos, OppilaitosDAO.getPutActions, OppilaitosDAO.getUpdateActionsWithoutModifiedCheck),
      oppilaitos.oid.toString,
      (added: Oppilaitos) => auditLog.logCreate(added, user))

  private def updateWithIndexing(oppilaitos: Oppilaitos, notModifiedSince: Instant, user: User, before: Oppilaitos): Option[Oppilaitos] =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeOppilaitos,
      () => themeImageUpdateActions(oppilaitos, OppilaitosDAO.getUpdateActions(_, notModifiedSince)),
      oppilaitos.oid.toString,
      (updated: Option[Oppilaitos]) => auditLog.logUpdate(before, updated, user))
}

object OppilaitoksenOsaService extends OppilaitoksenOsaService(SqsInTransactionService, S3Service, AuditLog)

class OppilaitoksenOsaService(sqsInTransactionService: SqsInTransactionService, val s3Service: S3Service, auditLog: AuditLog)
  extends ValidatingService[OppilaitoksenOsa]
    with RoleEntityAuthorizationService
    with TeemakuvaService[OrganisaatioOid, OppilaitoksenOsa, OppilaitoksenOsaMetadata] {

  protected val roleEntity: RoleEntity = Role.Oppilaitos

  val teemakuvaPrefix = "oppilaitoksen-osa-teemakuva"

  def get(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Option[(OppilaitoksenOsa, Instant)] =
    authorizeGet(OppilaitoksenOsaDAO.get(oid))

  def put(oppilaitoksenOsa: OppilaitoksenOsa)(implicit authenticated: Authenticated, request: HttpServletRequest): OrganisaatioOid =
    authorizePut(oppilaitoksenOsa) {
      withValidation(oppilaitoksenOsa, putWithIndexing(_, auditLog.getUser)).oid
    }

  def update(oppilaitoksenOsa: OppilaitoksenOsa, notModifiedSince: Instant)(implicit authenticated: Authenticated, request: HttpServletRequest): Boolean =
    authorizeUpdate(OppilaitoksenOsaDAO.get(oppilaitoksenOsa.oid)) { oldOsa =>
      withValidation(oppilaitoksenOsa, updateWithIndexing(_, notModifiedSince, auditLog.getUser, oldOsa))
    }.nonEmpty

  private def putWithIndexing(oppilaitoksenOsa: OppilaitoksenOsa, user: User): OppilaitoksenOsa =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeOppilaitos,
      () => themeImagePutActions(oppilaitoksenOsa, OppilaitoksenOsaDAO.getPutActions, OppilaitoksenOsaDAO.getUpdateActionsWithoutModifiedCheck),
      oppilaitoksenOsa.oppilaitosOid.toString,
      (added: OppilaitoksenOsa) => auditLog.logCreate(added, user))

  private def updateWithIndexing(oppilaitoksenOsa: OppilaitoksenOsa, notModifiedSince: Instant, user: User, before: OppilaitoksenOsa): Option[OppilaitoksenOsa] =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeOppilaitos,
      () => themeImageUpdateActions(oppilaitoksenOsa, OppilaitoksenOsaDAO.getUpdateActions(_, notModifiedSince)),
      oppilaitoksenOsa.oppilaitosOid.toString,
      (updated: Option[OppilaitoksenOsa]) => auditLog.logUpdate(before, updated, user))
}
