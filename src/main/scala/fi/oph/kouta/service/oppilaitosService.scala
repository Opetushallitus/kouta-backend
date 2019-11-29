package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeOppilaitos}
import fi.oph.kouta.indexing.{S3Service, SqsInTransactionService}
import fi.oph.kouta.repository.{OppilaitoksenOsaDAO, OppilaitosDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated

object OppilaitosService extends OppilaitosService(SqsInTransactionService, S3Service)

class OppilaitosService(sqsInTransactionService: SqsInTransactionService, val s3Service: S3Service)
  extends ValidatingService[Oppilaitos] with RoleEntityAuthorizationService with TeemakuvaService[OrganisaatioOid, Oppilaitos, OppilaitosMetadata] {

  protected val roleEntity: RoleEntity = Role.Oppilaitos

  val teemakuvaPrefix = "oppilaitos-teemakuva"

  def get(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Option[(Oppilaitos, Instant)] =
    authorizeGet(OppilaitosDAO.get(oid))

  def put(oppilaitos: Oppilaitos)(implicit authenticated: Authenticated): OrganisaatioOid = {
    authorizePut(oppilaitos) {
      withValidation(oppilaitos, checkTeemakuvaInPut(_, putWithIndexing, updateWithIndexing))
    }
  }

  def update(oppilaitos: Oppilaitos, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(OppilaitosDAO.get(oppilaitos.oid)) { oldOppilaitos =>
      withValidation(oppilaitos, checkTeemakuvaInUpdate(_, updateWithIndexing(_, notModifiedSince)))
    }

  def getOppilaitoksenOsat(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[OppilaitoksenOsa] =
    withRootAccess(indexerRoles)(OppilaitoksenOsaDAO.getByOppilaitosOid(oid))

  def listOppilaitoksenOsat(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[OppilaitoksenOsaListItem] =
    withRootAccess(indexerRoles)(OppilaitoksenOsaDAO.listByOppilaitosOid(oid))

  def listOppilaitoksenOsat(oid: OrganisaatioOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[OppilaitoksenOsaListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, Role.Oppilaitos.readRoles) {
      OppilaitoksenOsaDAO.listByOppilaitosOidAndOrganisaatioOids(oid, _)
    }

  private def putWithIndexing(oppilaitos: Oppilaitos) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeOppilaitos,
      () => OppilaitosDAO.getPutActions(oppilaitos),
      oppilaitos.oid.toString)

  private def updateWithIndexing(oppilaitos: Oppilaitos, notModifiedSince: Instant) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeOppilaitos,
      () => OppilaitosDAO.getUpdateActions(oppilaitos, notModifiedSince),
      oppilaitos.oid.toString)
}

object OppilaitoksenOsaService extends OppilaitoksenOsaService(SqsInTransactionService, S3Service)

class OppilaitoksenOsaService(sqsInTransactionService: SqsInTransactionService, val s3Service: S3Service)
  extends ValidatingService[OppilaitoksenOsa]
    with RoleEntityAuthorizationService
    with TeemakuvaService[OrganisaatioOid, OppilaitoksenOsa, OppilaitoksenOsaMetadata] {

  protected val roleEntity: RoleEntity = Role.Oppilaitos

  val teemakuvaPrefix = "oppilaitoksen-osa-teemakuva"

  def get(oid: OrganisaatioOid)(implicit authenticated: Authenticated): Option[(OppilaitoksenOsa, Instant)] =
    authorizeGet(OppilaitoksenOsaDAO.get(oid))

  def put(oppilaitoksenOsa: OppilaitoksenOsa)(implicit authenticated: Authenticated): OrganisaatioOid =
    authorizePut(oppilaitoksenOsa) {
      withValidation(oppilaitoksenOsa, checkTeemakuvaInPut(_, putWithIndexing, updateWithIndexing))
    }

  def update(oppilaitoksenOsa: OppilaitoksenOsa, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(OppilaitoksenOsaDAO.get(oppilaitoksenOsa.oid)) { oldOsa =>
      withValidation(oppilaitoksenOsa, checkTeemakuvaInUpdate(_, updateWithIndexing(_, notModifiedSince)))
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
