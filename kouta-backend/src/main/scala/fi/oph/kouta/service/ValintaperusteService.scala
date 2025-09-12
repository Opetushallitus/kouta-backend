package fi.oph.kouta.service

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{KayttooikeusClient, KoutaIndeksoijaClient, KoutaSearchClient, OppijanumerorekisteriClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.domain.searchResults.ValintaperusteSearchResult
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeValintaperuste}
import fi.oph.kouta.repository.{HakukohdeDAO, KoutaDatabase, ValintaperusteDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.{Authenticated, EntityNotFoundException, SearchParams}
import fi.oph.kouta.util.{NameHelper, ServiceUtils}

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

object ValintaperusteService
    extends ValintaperusteService(
      SqsInTransactionService,
      AuditLog,
      OrganisaatioServiceImpl,
      OppijanumerorekisteriClient,
      KayttooikeusClient,
      ValintaperusteServiceValidation,
      KoutaIndeksoijaClient,
      HakukohdeUtil
    )

class ValintaperusteService(
    sqsInTransactionService: SqsInTransactionService,
    auditLog: AuditLog,
    val organisaatioService: OrganisaatioService,
    oppijanumerorekisteriClient: OppijanumerorekisteriClient,
    kayttooikeusClient: KayttooikeusClient,
    valintaperusteServiceValidation: ValintaperusteServiceValidation,
    koutaIndeksoijaClient: KoutaIndeksoijaClient,
    hakukohdeUtil: HakukohdeUtil
) extends RoleEntityAuthorizationService[Valintaperuste] {

  override val roleEntity: RoleEntity = Role.Valintaperuste
  protected val readRules: AuthorizationRules =
    AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true)

  def get(id: UUID, tilaFilter: TilaFilter)(implicit
      authenticated: Authenticated
  ): Option[(Valintaperuste, Instant)] = {
    val valintaperusteWithTime = ValintaperusteDAO.get(id, tilaFilter)

    val enrichedValintaperuste = valintaperusteWithTime match {
      case Some((v, i)) =>
        val muokkaaja = oppijanumerorekisteriClient.getHenkilöFromCache(v.muokkaaja)
        val muokkaajanNimi = NameHelper.generateMuokkaajanNimi(muokkaaja)
        Some(v.copy(_enrichedData = Some(ValintaperusteEnrichedData(muokkaajanNimi = Some(muokkaajanNimi)))), i)
      case None => None
    }

    authorizeGet(
      enrichedValintaperuste,
      AuthorizationRules(
        roleEntity.readRoles,
        allowAccessToParentOrganizations = true,
        Some(AuthorizationRuleForReadJulkinen)
      )
    )
  }

  private def enrichValintaperusteMetadata(valintaperuste: Valintaperuste) : Option[ValintaperusteMetadata] = {
    val muokkaajanOrganisaatiot = kayttooikeusClient.getOrganisaatiotFromCache(valintaperuste.muokkaaja)
    val isOphVirkailija = ServiceUtils.hasOphOrganisaatioOid(muokkaajanOrganisaatiot)

    valintaperuste.metadata match {
      case Some(metadata) =>
        metadata match {
          case valintaperusteMetadata: GenericValintaperusteMetadata =>
            Some(valintaperusteMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
        }
      case None => None
    }
  }

  def put(valintaperuste: Valintaperuste)(implicit authenticated: Authenticated): ValintaperusteCreateResult = {
    authorizePut(
      valintaperuste,
      AuthorizationRules(
        roleEntity.createRoles,
        overridingAuthorizationRule = Some(AuthorizationRuleByOrganizationAndKoulutustyyppi)
      )
    ) { v =>
      val enrichedMetadata: Option[ValintaperusteMetadata] = enrichValintaperusteMetadata(v)
      val enrichedValintaperuste                           = v.copy(metadata = enrichedMetadata)
      valintaperusteServiceValidation.withValidation(enrichedValintaperuste, None) { valpe =>
        doPut(valpe)
      }
    }
  }

  def update(valintaperuste: Valintaperuste, notModifiedSince: Instant)(implicit
      authenticated: Authenticated
  ): UpdateResult = {
    val oldValintaPerusteWithTime = ValintaperusteDAO.get(valintaperuste.id.get, TilaFilter.onlyOlemassaolevat())
    val rules: AuthorizationRules = getAuthorizationRulesForUpdate(oldValintaPerusteWithTime, valintaperuste)

    authorizeUpdate(oldValintaPerusteWithTime, valintaperuste, rules) { (oldValintaperuste, v) =>
      val enrichedMetadata: Option[ValintaperusteMetadata] = enrichValintaperusteMetadata(v)
      val enrichedValintaperuste                           = v.copy(metadata = enrichedMetadata)
      valintaperusteServiceValidation.withValidation(enrichedValintaperuste, Some(oldValintaperuste)) { v =>
        doUpdate(v, notModifiedSince, oldValintaperuste)
      }
    }
  }

  private def getAuthorizationRulesForUpdate(
      oldValintaperusteWithTime: Option[(Valintaperuste, Instant)],
      newValintaperuste: Valintaperuste
  ) = {
    oldValintaperusteWithTime match {
      case None => throw EntityNotFoundException(s"Päivitettävää valintaperustetta ei löytynyt")
      case Some((oldValintaperuste, _)) =>
        if (Julkaisutila.isTilaUpdateAllowedOnlyForOph(oldValintaperuste.tila, newValintaperuste.tila)) {
          AuthorizationRules(Seq(Role.Paakayttaja))
        } else {
          AuthorizationRules(roleEntity.updateRoles)
        }
    }
  }

  def list(organisaatioOid: OrganisaatioOid, tilaFilter: TilaFilter)(implicit
      authenticated: Authenticated
  ): Seq[ValintaperusteListItem] =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(organisaatioOid, readRules) { case (oids, koulutustyypit) =>
      ValintaperusteDAO.listAllowedByOrganisaatiot(oids, koulutustyypit, tilaFilter)
    }

  def listByHakuAndKoulutustyyppi(
      organisaatioOid: OrganisaatioOid,
      hakuOid: HakuOid,
      koulutustyyppi: Koulutustyyppi,
      tilaFilter: TilaFilter
  )(implicit authenticated: Authenticated): Seq[ValintaperusteListItem] =
    withAuthorizedOrganizationOids(organisaatioOid, readRules) { oids =>
      ValintaperusteDAO.listAllowedByOrganisaatiotAndHakuAndKoulutustyyppi(oids, hakuOid, koulutustyyppi, tilaFilter)
    }

  def listHakukohteet(valintaperusteId: UUID, tilaFilter: TilaFilter)(implicit
      authenticated: Authenticated
  ): Seq[HakukohdeListItem] =
    withRootAccess(Role.Hakukohde.readRoles) {
      hakukohdeUtil.enrichHakukohteet(HakukohdeDAO.listByValintaperusteId(valintaperusteId, tilaFilter))
    }

  def search(organisaatioOid: OrganisaatioOid, params: SearchParams)(implicit
      authenticated: Authenticated
  ): ValintaperusteSearchResult =
    list(organisaatioOid, TilaFilter.alsoArkistoidutAddedToOlemassaolevat(true)).map(_.id) match {
      case Nil               => SearchResult[ValintaperusteSearchItem]()
      case valintaperusteIds => KoutaSearchClient.searchValintaperusteet(valintaperusteIds, params)
    }

  private def doPut(valintaperuste: Valintaperuste)(implicit authenticated: Authenticated): ValintaperusteCreateResult =
    KoutaDatabase.runBlockingTransactionally {
      for {
        v <- ValintaperusteDAO.getPutActions(valintaperuste)
        _ <- auditLog.logCreate(v)
      } yield v
    }.map { v: Valintaperuste =>
      val warnings = quickIndex(v.id) ++ index(Some(v))
      ValintaperusteCreateResult(v.id, created = v.id.isDefined, warnings)
    }.get

  private def doUpdate(valintaperuste: Valintaperuste, notModifiedSince: Instant, before: Valintaperuste)(implicit
      authenticated: Authenticated
  ): UpdateResult =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _ <- ValintaperusteDAO.checkNotModified(valintaperuste.id.get, notModifiedSince)
        v <- ValintaperusteDAO.getUpdateActions(valintaperuste)
        _ <- auditLog.logUpdate(before, v)
      } yield v
    }.map { v: Option[Valintaperuste] =>
      val warnings = quickIndex(v.flatMap(_.id)) ++ index(v)
      UpdateResult(v.isDefined, warnings)
    }.get

  private def index(valintaperuste: Option[Valintaperuste]): List[String] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeValintaperuste, valintaperuste.map(_.id.get.toString))

  private def quickIndex(valintaperusteId: Option[UUID]): List[String] = {
    valintaperusteId match {
      case Some(id) => koutaIndeksoijaClient.quickIndexValintaperuste(id.toString)
      case None => List.empty
    }
  }
}
