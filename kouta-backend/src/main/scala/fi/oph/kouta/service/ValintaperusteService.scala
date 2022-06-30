package fi.oph.kouta.service

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{KayttooikeusClient, KoutaIndexClient, OppijanumerorekisteriClient}
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.domain.{ GenericValintaperusteMetadata, HakukohdeListItem, Julkaisutila, Koulutustyyppi, Poistettu, TilaFilter, Valintaperuste, ValintaperusteEnrichedData, ValintaperusteListItem, ValintaperusteMetadata, ValintaperusteSearchResult}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeValintaperuste}
import fi.oph.kouta.repository.{HakukohdeDAO, KoutaDatabase, ValintaperusteDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.{Authenticated, EntityNotFoundException}
import fi.oph.kouta.util.{NameHelper, ServiceUtils}
import fi.oph.kouta.validation.{IsValid, NoErrors}
import fi.oph.kouta.validation.Validations.{assertTrue, integrityViolationMsg, validateIfTrue, validateStateChange}
import slick.dbio.DBIO

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

object ValintaperusteService extends ValintaperusteService(SqsInTransactionService, AuditLog, OrganisaatioServiceImpl, OppijanumerorekisteriClient, KayttooikeusClient)

class ValintaperusteService(
  sqsInTransactionService: SqsInTransactionService,
  auditLog: AuditLog,
  val organisaatioService: OrganisaatioService,
  oppijanumerorekisteriClient: OppijanumerorekisteriClient,
  kayttooikeusClient: KayttooikeusClient
) extends ValidatingService[Valintaperuste] with RoleEntityAuthorizationService[Valintaperuste] {

  override val roleEntity: RoleEntity = Role.Valintaperuste
  protected val readRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true)

  def get(id: UUID, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Option[(Valintaperuste, Instant)] = {
    val valintaperusteWithTime = ValintaperusteDAO.get(id, tilaFilter)

    val enrichedValintaperuste = valintaperusteWithTime match {
      case Some((v, i)) =>
        val muokkaaja = oppijanumerorekisteriClient.getHenkilö(v.muokkaaja)
        val muokkaajanNimi = NameHelper.generateMuokkaajanNimi(muokkaaja)
        Some(v.copy(_enrichedData = Some(ValintaperusteEnrichedData(muokkaajanNimi = Some(muokkaajanNimi)))), i)
      case None => None
    }

    authorizeGet(
      enrichedValintaperuste,
      AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true, Seq(AuthorizationRuleForJulkinen)))
  }

  private def enrichValintaperusteMetadata(valintaperuste: Valintaperuste) : Option[ValintaperusteMetadata] = {
    val muokkaajanOrganisaatiot = kayttooikeusClient.getOrganisaatiot(valintaperuste.muokkaaja)
    val isOphVirkailija = ServiceUtils.hasOphOrganisaatioOid(muokkaajanOrganisaatiot)

    valintaperuste.metadata match {
      case Some(metadata) => metadata match {
        case valintaperusteMetadata: GenericValintaperusteMetadata => Some(valintaperusteMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
      }
      case None => None
    }
  }

  def put(valintaperuste: Valintaperuste)(implicit authenticated: Authenticated): UUID = {
    authorizePut(valintaperuste) { v =>
      val enrichedMetadata: Option[ValintaperusteMetadata] = enrichValintaperusteMetadata(v)
      val enrichedValintaperuste = v.copy(metadata = enrichedMetadata)
      withValidation(enrichedValintaperuste, None) { valpe =>
        doPut(valpe)
      }
    }.id.get
  }

  def update(valintaperuste: Valintaperuste, notModifiedSince: Instant)
            (implicit authenticated: Authenticated): Boolean = {
    val oldValintaPerusteWithTime = ValintaperusteDAO.get(valintaperuste.id.get, TilaFilter.onlyOlemassaolevat())
    val rules: AuthorizationRules = getAuthorizationRulesForUpdate(oldValintaPerusteWithTime, valintaperuste)

    authorizeUpdate(oldValintaPerusteWithTime, valintaperuste, rules) { (oldValintaperuste, v) =>
      val enrichedMetadata: Option[ValintaperusteMetadata] = enrichValintaperusteMetadata(v)
      val enrichedValintaperuste = v.copy(metadata = enrichedMetadata)
      withValidation(enrichedValintaperuste, Some(oldValintaperuste)) { v =>
        throwValidationErrors(validateStateChange("valintaperusteelle", oldValintaperuste.tila, v.tila))
        validateHakukohdeIntegrityIfDeletingValintaperuste(oldValintaperuste.tila, v.tila, v.id.get)
        doUpdate(v, notModifiedSince, oldValintaperuste)
      }
    }.nonEmpty
  }

  private def getAuthorizationRulesForUpdate(oldValintaperusteWithTime: Option[(Valintaperuste, Instant)], newValintaperuste: Valintaperuste) = {
    oldValintaperusteWithTime match {
      case None => throw EntityNotFoundException(s"Päivitettävää valintaperustetta ei löytynyt")
      case Some((oldValintaperuste, _)) =>
        if (Julkaisutila.isTilaUpdateAllowedOnlyForOph(oldValintaperuste.tila, newValintaperuste.tila)) {
          AuthorizationRules(Seq(Role.Paakayttaja))
        }
        else {
          AuthorizationRules(roleEntity.updateRoles)
        }
    }
  }

  private def validateHakukohdeIntegrityIfDeletingValintaperuste(aiempiTila: Julkaisutila, tulevaTila: Julkaisutila, valintaperusteId: UUID): Unit =
    throwValidationErrors(
      validateIfTrue(tulevaTila == Poistettu && tulevaTila != aiempiTila, assertTrue(
        HakukohdeDAO.listByValintaperusteId(valintaperusteId, TilaFilter.onlyOlemassaolevat()).isEmpty,
        "tila",
        integrityViolationMsg("Valintaperustetta", "hakukohteita")))
    )

  def list(organisaatioOid: OrganisaatioOid, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Seq[ValintaperusteListItem] =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(organisaatioOid, readRules) { case (oids, koulutustyypit) =>
      ValintaperusteDAO.listAllowedByOrganisaatiot(oids, koulutustyypit, tilaFilter)
    }

  def listByHakuAndKoulutustyyppi(organisaatioOid: OrganisaatioOid, hakuOid: HakuOid, koulutustyyppi: Koulutustyyppi, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Seq[ValintaperusteListItem] =
    withAuthorizedOrganizationOids(organisaatioOid, readRules) { oids =>
      ValintaperusteDAO.listAllowedByOrganisaatiotAndHakuAndKoulutustyyppi(oids, hakuOid, koulutustyyppi, tilaFilter)
    }

  def listHakukohteet(valintaperusteId: UUID, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withRootAccess(Role.Hakukohde.readRoles) {
      HakukohdeDAO.listByValintaperusteId(valintaperusteId, tilaFilter)
    }

  def search(organisaatioOid: OrganisaatioOid, params: Map[String, String])(implicit authenticated: Authenticated): ValintaperusteSearchResult =
    list(organisaatioOid, TilaFilter.alsoArkistoidutAddedToOlemassaolevat(true)).map(_.id) match {
      case Nil               => ValintaperusteSearchResult()
      case valintaperusteIds => KoutaIndexClient.searchValintaperusteet(valintaperusteIds, params)
    }

  private def doPut(valintaperuste: Valintaperuste)(implicit authenticated: Authenticated): Valintaperuste =
    KoutaDatabase.runBlockingTransactionally {
      for {
        v <- ValintaperusteDAO.getPutActions(valintaperuste)
        _ <- index(Some(v))
        _ <- auditLog.logCreate(v)
      } yield v
    }.get

  private def doUpdate(valintaperuste: Valintaperuste, notModifiedSince: Instant, before: Valintaperuste)(implicit authenticated: Authenticated): Option[Valintaperuste] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _ <- ValintaperusteDAO.checkNotModified(valintaperuste.id.get, notModifiedSince)
        v <- ValintaperusteDAO.getUpdateActions(valintaperuste)
        _ <- index(v)
        _ <- auditLog.logUpdate(before, v)
      } yield v
    }.get

  private def index(valintaperuste: Option[Valintaperuste]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeValintaperuste, valintaperuste.map(_.id.get.toString))

  override def validateEntity(valintaperuste: Valintaperuste): IsValid = valintaperuste.validate()
  override def validateEntityOnJulkaisu(valintaperuste: Valintaperuste): IsValid = valintaperuste.validateOnJulkaisu()

  override def validateInternalDependenciesWhenDeletingEntity(valintaperuste: Valintaperuste): IsValid = NoErrors
}
