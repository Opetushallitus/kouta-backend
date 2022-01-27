package fi.oph.kouta.service

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.OppijanumerorekisteriClient
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Julkaisutila, Poistettu, Sorakuvaus, SorakuvausEnrichedData, SorakuvausListItem, TilaFilter}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeSorakuvaus}
import fi.oph.kouta.repository.{KoulutusDAO, KoutaDatabase, SorakuvausDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.NameHelper
import fi.oph.kouta.validation.Validations.{assertTrue, integrityViolationMsg, validateIfTrue, validateStateChange}
import slick.dbio.DBIO

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

object SorakuvausService extends SorakuvausService(SqsInTransactionService, AuditLog, OrganisaatioServiceImpl, OppijanumerorekisteriClient)

class SorakuvausService(
  sqsInTransactionService: SqsInTransactionService,
  auditLog: AuditLog,
  val organisaatioService: OrganisaatioService,
  oppijanumerorekisteriClient: OppijanumerorekisteriClient
) extends ValidatingService[Sorakuvaus] with RoleEntityAuthorizationService[Sorakuvaus] {

  override val roleEntity: RoleEntity = Role.Valintaperuste
  protected val readRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true)
  protected val createRules: AuthorizationRules = AuthorizationRules(Seq(Role.Paakayttaja))
  protected val updateRules: AuthorizationRules = AuthorizationRules(Seq(Role.Paakayttaja))

  def get(id: UUID, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Option[(Sorakuvaus, Instant)] = {
    val sorakuvausWithTime = SorakuvausDAO.get(id, tilaFilter)

    val enrichedSorakuvaus = sorakuvausWithTime match {
      case Some((s, i)) => {
        val muokkaaja = oppijanumerorekisteriClient.getHenkilö(s.muokkaaja)
        val muokkaajanNimi = NameHelper.generateMuokkaajanNimi(muokkaaja)
        Some(s.copy(_enrichedData = Some(SorakuvausEnrichedData(muokkaajanNimi = Some(muokkaajanNimi)))), i)
      }
      case None => None
    }

    authorizeGet(
      enrichedSorakuvaus,
      AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true, Seq(authorizationRuleByKoulutustyyppi)))
  }

  def put(sorakuvaus: Sorakuvaus)(implicit authenticated: Authenticated): UUID =
    authorizePut(sorakuvaus, createRules) { s =>
      withValidation(s, None)(doPut)
    }.id.get

  def update(sorakuvaus: Sorakuvaus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(SorakuvausDAO.get(sorakuvaus.id.get, TilaFilter.onlyOlemassaolevat()), sorakuvaus, updateRules) { (oldSorakuvaus, s) =>
      withValidation(s, Some(oldSorakuvaus)) {
        throwValidationErrors(validateStateChange("sorakuvaukselle", oldSorakuvaus.tila, sorakuvaus.tila))
        validateKoulutusIntegrityIfDeletingSorakuvaus(oldSorakuvaus.tila, sorakuvaus.tila, sorakuvaus.id.get)
        doUpdate(_, notModifiedSince, oldSorakuvaus)
      }
    }.nonEmpty


  private def validateKoulutusIntegrityIfDeletingSorakuvaus(aiempiTila: Julkaisutila, tulevaTila: Julkaisutila, sorakuvausId: UUID) =
    throwValidationErrors(
      validateIfTrue(tulevaTila == Poistettu && tulevaTila != aiempiTila, assertTrue(
        KoulutusDAO.listBySorakuvausId(sorakuvausId, TilaFilter.onlyOlemassaolevat()).isEmpty,
        "tila",
        integrityViolationMsg("Sorakuvausta", "koulutuksia")))
    )

  def listKoulutusOids(sorakuvausId: UUID, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Seq[String] =
    withRootAccess(indexerRoles) {
      KoulutusDAO.listBySorakuvausId(sorakuvausId, tilaFilter)
    }

  def list(organisaatioOid: OrganisaatioOid, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Seq[SorakuvausListItem] =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(organisaatioOid, readRules) { case (_, koulutustyypit) =>
      SorakuvausDAO.listByKoulutustyypit(koulutustyypit, tilaFilter)
    }

  private def doPut(sorakuvaus: Sorakuvaus)(implicit authenticated: Authenticated): Sorakuvaus =
    KoutaDatabase.runBlockingTransactionally {
      for {
        s <- SorakuvausDAO.getPutActions(sorakuvaus)
        _ <- index(Some(s))
        _ <- auditLog.logCreate(s)
      } yield s
    }.get

  private def doUpdate(sorakuvaus: Sorakuvaus, notModifiedSince: Instant, before: Sorakuvaus)(implicit authenticated: Authenticated): Option[Sorakuvaus] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _ <- SorakuvausDAO.checkNotModified(sorakuvaus.id.get, notModifiedSince)
        s <- SorakuvausDAO.getUpdateActions(sorakuvaus)
        _ <- index(s)
        _ <- auditLog.logUpdate(before, s)
      } yield s
    }.get

  private def index(sorakuvaus: Option[Sorakuvaus]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeSorakuvaus, sorakuvaus.map(_.id.get.toString))
}
