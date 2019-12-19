package fi.oph.kouta.service

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Sorakuvaus, SorakuvausListItem, ValintaperusteListItem}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeSorakuvaus}
import fi.oph.kouta.repository.{SorakuvausDAO, ValintaperusteDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated

object SorakuvausService extends SorakuvausService(SqsInTransactionService)

abstract class SorakuvausService(sqsInTransactionService: SqsInTransactionService) extends ValidatingService[Sorakuvaus] with RoleEntityAuthorizationService {

  override val roleEntity: RoleEntity = Role.Valintaperuste
  protected val readRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles, true)

  def get(id: UUID)(implicit authenticated: Authenticated): Option[(Sorakuvaus, Instant)] =
    authorizeGet(SorakuvausDAO.get(id), AuthorizationRules(roleEntity.readRoles, withParents = true, withTarjoajat = false, Seq(authorizationRuleForJulkinen)))

  def put(sorakuvaus: Sorakuvaus)(implicit authenticated: Authenticated): UUID =
    authorizePut(sorakuvaus) {
      withValidation(sorakuvaus, putWithIndexing)
    }

  def update(sorakuvaus: Sorakuvaus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(SorakuvausDAO.get(sorakuvaus.id.get)) {
      withValidation(sorakuvaus, updateWithIndexing(_, notModifiedSince))
    }

  def listValintaperusteet(sorakuvausId: UUID)(implicit authenticated: Authenticated): Seq[ValintaperusteListItem] =
    withRootAccess(indexerRoles) {
      ValintaperusteDAO.listBySorakuvausId(sorakuvausId)
    }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[SorakuvausListItem] =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(organisaatioOid, readRules) { case (oids, koulutustyypit) =>
      SorakuvausDAO.listAllowedByOrganisaatiot(oids, koulutustyypit)
    }

  private def putWithIndexing(sorakuvaus: Sorakuvaus) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeSorakuvaus,
      () => SorakuvausDAO.getPutActions(sorakuvaus))

  private def updateWithIndexing(sorakuvaus: Sorakuvaus, notModifiedSince: Instant) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeSorakuvaus,
      () => SorakuvausDAO.getUpdateActions(sorakuvaus, notModifiedSince),
      sorakuvaus.id.get.toString)
}