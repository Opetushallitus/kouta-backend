package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeOppilaitos}
import fi.oph.kouta.indexing.{S3Service, SqsInTransactionService}
import fi.oph.kouta.repository.{KoutaDatabase, OppilaitoksenOsaDAO, OppilaitosDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

object OppilaitosService extends OppilaitosService(SqsInTransactionService, S3Service, AuditLog)

class OppilaitosService(sqsInTransactionService: SqsInTransactionService, val s3Service: S3Service, auditLog: AuditLog)
  extends ValidatingService[Oppilaitos] with RoleEntityAuthorizationService with TeemakuvaService[OrganisaatioOid, Oppilaitos, OppilaitosMetadata] {

  protected val roleEntity: RoleEntity = Role.Oppilaitos

  val teemakuvaPrefix = "oppilaitos-teemakuva"

  def get(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Option[(Oppilaitos, Instant)] =
    authorizeGet(OppilaitosDAO.get(oid))

  def put(oppilaitos: Oppilaitos)(implicit authenticated: Authenticated): OrganisaatioOid = {
    authorizePut(oppilaitos) {
      withValidation(oppilaitos, doPut)
    }.oid
  }

  def update(oppilaitos: Oppilaitos, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(OppilaitosDAO.get(oppilaitos.oid)) { oldOppilaitos =>
      withValidation(oppilaitos, doUpdate(_, notModifiedSince, oldOppilaitos))
    }.nonEmpty

  def getOppilaitoksenOsat(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[OppilaitoksenOsa] =
    withRootAccess(indexerRoles)(OppilaitoksenOsaDAO.getByOppilaitosOid(oid))

  def listOppilaitoksenOsat(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[OppilaitoksenOsaListItem] =
    withRootAccess(indexerRoles)(OppilaitoksenOsaDAO.listByOppilaitosOid(oid))

  def listOppilaitoksenOsat(oid: OrganisaatioOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[OppilaitoksenOsaListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, Role.Oppilaitos.readRoles) {
      OppilaitoksenOsaDAO.listByOppilaitosOidAndOrganisaatioOids(oid, _)
    }

  private def doPut(oppilaitos: Oppilaitos)(implicit authenticated: Authenticated): Oppilaitos =
    KoutaDatabase.runBlockingTransactionally {
      for {
        (tempImage, cleared) <- checkAndMaybeClearTempImage(oppilaitos)
        added   <- OppilaitosDAO.getPutActions(cleared)
        themed  <- maybeCopyThemeImage(tempImage, added)
        updated <- tempImage.map(_ => OppilaitosDAO.updateJustOppilaitos(themed)).getOrElse(DBIO.successful(themed))
        _       <- maybeDeleteTempImage(tempImage)
        _       <- index(Some(updated))
        _       <- auditLog.logCreate(updated)
      } yield updated
    }.get

  private def doUpdate(oppilaitos: Oppilaitos, notModifiedSince: Instant, before: Oppilaitos)(implicit authenticated: Authenticated): Option[Oppilaitos] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _       <- OppilaitosDAO.checkNotModified(oppilaitos.oid, notModifiedSince)
        (tempImage, themed) <- checkAndMaybeCopyTempImage(oppilaitos)
        updated <- OppilaitosDAO.getUpdateActions(themed)
        _       <- maybeDeleteTempImage(tempImage)
        _       <- index(updated)
        _       <- auditLog.logUpdate(before, updated)
      } yield updated
    }.get

  private def index(oppilaitos: Option[Oppilaitos]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeOppilaitos, oppilaitos.map(_.oid.toString))
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

  def put(oppilaitoksenOsa: OppilaitoksenOsa)(implicit authenticated: Authenticated): OrganisaatioOid =
    authorizePut(oppilaitoksenOsa) {
      withValidation(oppilaitoksenOsa, doPut).oid
    }

  def update(oppilaitoksenOsa: OppilaitoksenOsa, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(OppilaitoksenOsaDAO.get(oppilaitoksenOsa.oid)) { oldOsa =>
      withValidation(oppilaitoksenOsa, doUpdate(_, notModifiedSince, oldOsa))
    }.nonEmpty

  private def doPut(oppilaitoksenOsa: OppilaitoksenOsa)(implicit authenticated: Authenticated): OppilaitoksenOsa =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _       <- OppilaitoksenOsaDAO.oppilaitosExists(oppilaitoksenOsa)
        (tempImage, cleared) <- checkAndMaybeClearTempImage(oppilaitoksenOsa)
        added   <- OppilaitoksenOsaDAO.getPutActions(cleared)
        themed  <- maybeCopyThemeImage(tempImage, added)
        updated <- tempImage.map(_ => OppilaitoksenOsaDAO.updateJustOppilaitoksenOsa(themed)).getOrElse(DBIO.successful(themed))
        _       <- maybeDeleteTempImage(tempImage)
        _       <- index(Some(updated))
        _       <- auditLog.logCreate(updated)
      } yield updated
    }.get

  private def doUpdate(oppilaitoksenOsa: OppilaitoksenOsa, notModifiedSince: Instant, before: OppilaitoksenOsa)(implicit authenticated: Authenticated): Option[OppilaitoksenOsa] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _       <- OppilaitoksenOsaDAO.checkNotModified(oppilaitoksenOsa.oid, notModifiedSince)
        (tempImage, themed) <- checkAndMaybeCopyTempImage(oppilaitoksenOsa)
        updated <- OppilaitoksenOsaDAO.getUpdateActions(themed)
        _       <- maybeDeleteTempImage(tempImage)
        _       <- index(updated)
        _       <- auditLog.logUpdate(before, updated)
      } yield updated
    }.get

  private def index(oppilaitoksenOsa: Option[OppilaitoksenOsa]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeOppilaitos, oppilaitoksenOsa.map(_.oid.toString))
}
