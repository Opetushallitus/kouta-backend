package fi.oph.kouta.service

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{KayttooikeusClient, KoutaIndeksoijaClient, OppijanumerorekisteriClient}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Julkaisutila, Poistettu, Sorakuvaus, SorakuvausEnrichedData, SorakuvausListItem, SorakuvausMetadata, TilaFilter}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeSorakuvaus}
import fi.oph.kouta.repository.{KoulutusDAO, KoutaDatabase, SorakuvausDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.{Authenticated, EntityNotFoundException}
import fi.oph.kouta.util.{NameHelper, ServiceUtils}
import fi.oph.kouta.validation.CrudOperations.CrudOperation
import fi.oph.kouta.validation.{IsValid, NoErrors}
import fi.oph.kouta.validation.Validations.{assertTrue, integrityViolationMsg, validateIfTrue, validateStateChange}
import slick.dbio.DBIO

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

object SorakuvausService extends SorakuvausService(SqsInTransactionService, AuditLog, OrganisaatioServiceImpl, OppijanumerorekisteriClient, KayttooikeusClient, SorakuvausServiceValidation, KoutaIndeksoijaClient)

class SorakuvausService(
  sqsInTransactionService: SqsInTransactionService,
  auditLog: AuditLog,
  val organisaatioService: OrganisaatioService,
  oppijanumerorekisteriClient: OppijanumerorekisteriClient,
  kayttooikeusClient: KayttooikeusClient,
  sorakuvausServiceValidation: SorakuvausServiceValidation,
  koutaIndeksoijaClient: KoutaIndeksoijaClient
) extends RoleEntityAuthorizationService[Sorakuvaus] {

  override val roleEntity: RoleEntity = Role.Valintaperuste
  protected val readRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true)
  protected val createRules: AuthorizationRules = AuthorizationRules(Seq(Role.Paakayttaja))
  protected val updateRules: AuthorizationRules = AuthorizationRules(Seq(Role.Paakayttaja))

  def get(id: UUID, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Option[(Sorakuvaus, Instant)] = {
    val sorakuvausWithTime = SorakuvausDAO.get(id, tilaFilter)

    val enrichedSorakuvaus = sorakuvausWithTime match {
      case Some((s, i)) => {
        val muokkaaja = oppijanumerorekisteriClient.getHenkilöFromCache(s.muokkaaja)
        val muokkaajanNimi = NameHelper.generateMuokkaajanNimi(muokkaaja)
        Some(s.copy(_enrichedData = Some(SorakuvausEnrichedData(muokkaajanNimi = Some(muokkaajanNimi)))), i)
      }
      case None => None
    }

    authorizeGet(
      enrichedSorakuvaus,
      AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true, Some(AuthorizationRuleByKoulutustyyppi)))
  }

  private def enrichSorakuvausMetadata(sorakuvaus: Sorakuvaus) : Option[SorakuvausMetadata] = {
    val muokkaajanOrganisaatiot = kayttooikeusClient.getOrganisaatiotFromCache(sorakuvaus.muokkaaja)
    val isOphVirkailija = ServiceUtils.hasOphOrganisaatioOid(muokkaajanOrganisaatiot)

    sorakuvaus.metadata match {
      case Some(metadata) => Some(metadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
      case None => None
    }
  }

  def put(sorakuvaus: Sorakuvaus)(implicit authenticated: Authenticated): UUID = {
    authorizePut(sorakuvaus, createRules) { s =>
      val enrichedMetadata: Option[SorakuvausMetadata] = enrichSorakuvausMetadata(s)
      val enrichedSorakuvaus = s.copy(metadata = enrichedMetadata)
      sorakuvausServiceValidation.withValidation(enrichedSorakuvaus, None)(doPut)
    }.id.get
  }

  def update(sorakuvaus: Sorakuvaus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    val oldSorakuvausWithTime = SorakuvausDAO.get(sorakuvaus.id.get, TilaFilter.onlyOlemassaolevat())
    val rules = getAuthorizationRulesForUpdate(sorakuvaus, oldSorakuvausWithTime)

    authorizeUpdate(oldSorakuvausWithTime, sorakuvaus, rules) { (oldSorakuvaus, s) =>
      val enrichedMetadata: Option[SorakuvausMetadata] = enrichSorakuvausMetadata(s)
      val enrichedSorakuvaus = s.copy(metadata = enrichedMetadata)
      sorakuvausServiceValidation.withValidation(enrichedSorakuvaus, Some(oldSorakuvaus)) {
        doUpdate(_, notModifiedSince, oldSorakuvaus)
      }
    }.nonEmpty
  }

  private def getAuthorizationRulesForUpdate(newSorakuvaus: Sorakuvaus, oldSorakuvausWithTime: Option[(Sorakuvaus, Instant)]): AuthorizationRules = {
    oldSorakuvausWithTime match {
      case None => throw EntityNotFoundException(s"Päivitettävää sorakuvausta ei löytynyt")
      case Some((oldSorakuvaus, _)) =>
        if (Julkaisutila.isTilaUpdateAllowedOnlyForOph(oldSorakuvaus.tila, newSorakuvaus.tila)) {
          AuthorizationRules(Seq(Role.Paakayttaja))
        }
        else {
          updateRules
        }
    }
  }

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
        _ <- auditLog.logCreate(s)
      } yield s
    }.map { s =>
      index(Some(s))
      s
    }.get

  private def doUpdate(sorakuvaus: Sorakuvaus, notModifiedSince: Instant, before: Sorakuvaus)(implicit authenticated: Authenticated): Option[Sorakuvaus] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _ <- SorakuvausDAO.checkNotModified(sorakuvaus.id.get, notModifiedSince)
        s <- SorakuvausDAO.getUpdateActions(sorakuvaus)
        _ <- auditLog.logUpdate(before, s)
      } yield s
    }.map { s =>
      index(s)
      s
    }.get

  private def index(sorakuvaus: Option[Sorakuvaus]): List[String] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeSorakuvaus, sorakuvaus.map(_.id.get.toString))
      .fold(warning => List(warning), _ => List.empty)

}
