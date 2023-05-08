package fi.oph.kouta.service

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client._
import fi.oph.kouta.domain.Koulutustyyppi.{isKorkeakoulu, isToisenAsteenYhteishakuKoulutustyyppi}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid, RootOrganisaatioOid}
import fi.oph.kouta.domain.searchResults.HakuSearchResultFromIndex
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeHaku}
import fi.oph.kouta.repository.DBIOHelpers.try2DBIOCapableTry
import fi.oph.kouta.repository._
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.{Authenticated, EntityNotFoundException, SearchParams}
import fi.oph.kouta.util.{MiscUtils, NameHelper, ServiceUtils}
import fi.oph.kouta.validation.CrudOperations.CrudOperation
import fi.oph.kouta.validation.{IsValid, NoErrors}
import fi.oph.kouta.validation.Validations.{assertTrue, integrityViolationMsg, validateIfTrue, validateStateChange}
import slick.dbio.DBIO

import java.time.{Instant, LocalDateTime}
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

object HakuService extends HakuService(SqsInTransactionService, AuditLog, OhjausparametritClient, OrganisaatioServiceImpl, OppijanumerorekisteriClient, KayttooikeusClient, HakuServiceValidation, KoutaIndeksoijaClient)

class HakuService(sqsInTransactionService: SqsInTransactionService,
                  auditLog: AuditLog,
                  ohjausparametritClient: OhjausparametritClient,
                  val organisaatioService: OrganisaatioService,
                  oppijanumerorekisteriClient: OppijanumerorekisteriClient,
                  kayttooikeusClient: KayttooikeusClient,
                  hakuServiceValidation: HakuServiceValidation,
                  koutaIndeksoijaClient: KoutaIndeksoijaClient
                 )
  extends RoleEntityAuthorizationService[Haku] {

  override val roleEntity: RoleEntity = Role.Haku
  protected val readRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true)

  private def enrichHakuMetadata(haku: Haku) : Option[HakuMetadata] = {
    val muokkaajanOrganisaatiot = kayttooikeusClient.getOrganisaatiotFromCache(haku.muokkaaja)
    val isOphVirkailija = ServiceUtils.hasOphOrganisaatioOid(muokkaajanOrganisaatiot)

    haku.metadata match {
      case Some(metadata) => Some(metadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
      case None => None
    }
  }

  def get(oid: HakuOid, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Option[(Haku, Instant)] = {
    val hakuWithTime = HakuDAO.get(oid, tilaFilter)
    val enrichedHaku = hakuWithTime match {
      case Some((h, i)) => {
        val muokkaaja = oppijanumerorekisteriClient.getHenkilöFromCache(h.muokkaaja)
        val muokkaajanNimi = NameHelper.generateMuokkaajanNimi(muokkaaja)
        Some(h.copy(_enrichedData = Some(HakuEnrichedData(muokkaajanNimi = Some(muokkaajanNimi)))), i)
      }
      case None => None
    }

    authorizeGet(
      enrichedHaku,
      readRules)
  }
  def put(haku: Haku)(implicit authenticated: Authenticated): CreateResult = {
    val rules = if (MiscUtils.isYhteishakuHakutapa(haku.hakutapaKoodiUri)) {
      AuthorizationRules(Seq(Role.Paakayttaja))
    } else {
      AuthorizationRules(roleEntity.createRoles)
    }

    authorizePut(haku, rules) { h =>
      val enrichedMetadata: Option[HakuMetadata] = enrichHakuMetadata(h)
      val enrichedHaku = h.copy(metadata = enrichedMetadata)
      hakuServiceValidation.withValidation(enrichedHaku, None)(doPut(_))
    }
  }

  def update(haku: Haku, notModifiedSince: Instant)(implicit authenticated: Authenticated): UpdateResult = {
    val oldHaku = HakuDAO.get(haku.oid.get, TilaFilter.onlyOlemassaolevat())
    val rules: AuthorizationRules = getAuthorizationRulesForUpdate(haku, oldHaku)

    authorizeUpdate(oldHaku, haku, rules) { (oldHaku, h) =>
      val enrichedMetadata: Option[HakuMetadata] = enrichHakuMetadata(h)
      val enrichedHaku = if (shouldDeleteSchedulerTimestamp(h)) {
        h.copy(metadata = enrichedMetadata, ajastettuHaunJaHakukohteidenArkistointiAjettu = None)
      } else {
        h.copy(metadata = enrichedMetadata)
      }

      hakuServiceValidation.withValidation(enrichedHaku, Some(oldHaku))(doUpdate(_, notModifiedSince, oldHaku))
    }
  }

  private def shouldDeleteSchedulerTimestamp(haku: Haku): Boolean = {
    haku.ajastettuHaunJaHakukohteidenArkistointi.getOrElse(None) match {
      case ajastettuHaunJaHakukohteidenArkistointi: LocalDateTime =>
        if (ajastettuHaunJaHakukohteidenArkistointi.toLocalDate.isAfter(LocalDateTime.now().toLocalDate) && haku.tila.equals(Julkaistu)) {
          true
        } else false
      case _ => false
    }
  }

  private def getAuthorizationRulesForUpdate(newHaku: Haku, oldHakuWithTime: Option[(Haku, Instant)]) = {
    oldHakuWithTime match {
      case None => throw EntityNotFoundException(s"Päivitettävää hakua ei löytynyt")
      case Some((oldHaku, _)) =>
        if (MiscUtils.isYhteishakuHakutapa(newHaku.hakutapaKoodiUri) || Julkaisutila.isTilaUpdateAllowedOnlyForOph(oldHaku.tila, newHaku.tila)) {
          AuthorizationRules(Seq(Role.Paakayttaja))
        } else {
          AuthorizationRules(roleEntity.updateRoles)
        }
    }
  }

  private def getYhteishakuFilter(yhteishaut: Boolean, koulutustyypit: Seq[Koulutustyyppi]): YhteishakuFilter = {
    if (yhteishaut) {
      YhteishakuFilter(
        removeKk = !koulutustyypit.exists(kt => isKorkeakoulu(kt)),
        removeToinenaste = !koulutustyypit.exists(kt => isToisenAsteenYhteishakuKoulutustyyppi(kt)))
    } else {
      YhteishakuFilter(removeKk = true, removeToinenaste = true)
    }
  }

  def list(organisaatioOid: OrganisaatioOid, tilaFilter: TilaFilter, yhteishaut: Boolean)(implicit authenticated: Authenticated): Seq[HakuListItem] =
    withAuthorizedOrganizationOidsAndRelevantKoulutustyyppis(organisaatioOid, readRules)(
      oidsAndTyypit => HakuDAO.listByAllowedOrganisaatiot(oidsAndTyypit._1, tilaFilter, getYhteishakuFilter(yhteishaut, oidsAndTyypit._2)))

  def listHakukohteet(hakuOid: HakuOid, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withRootAccess(indexerRoles)(HakukohdeDAO.listByHakuOid(hakuOid, tilaFilter))

  def listHakukohteet(hakuOid: HakuOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, Role.Hakukohde.readRoles) {
      case Seq(RootOrganisaatioOid) => HakukohdeDAO.listByHakuOid(hakuOid, TilaFilter.onlyOlemassaolevat())
      case x => HakukohdeDAO.listByHakuOidAndAllowedOrganisaatiot(hakuOid, x)
    }

  def listKoulutukset(hakuOid: HakuOid)(implicit authenticated: Authenticated): Seq[KoulutusListItem] =
    withRootAccess(indexerRoles)(KoulutusDAO.listByHakuOid(hakuOid))

  def listToteutukset(hakuOid: HakuOid)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
    withRootAccess(indexerRoles)(ToteutusDAO.listByHakuOid(hakuOid))

  def search(organisaatioOid: OrganisaatioOid, params: SearchParams)(implicit authenticated: Authenticated): HakuSearchResult = {
    def getCount(t: HakuSearchItemFromIndex, organisaatioOids: Seq[OrganisaatioOid]): Integer = {
      organisaatioOids match {
        case Seq(RootOrganisaatioOid) => t.hakukohteet.length
        case _ =>
          val oidStrings = organisaatioOids.map(_.toString())
          t.hakukohteet.count(x => x.tila != Arkistoitu && x.tila != Poistettu && oidStrings.contains(x.organisaatio.oid.toString()))
      }
    }

    def assocHakukohdeCounts(r: HakuSearchResultFromIndex): HakuSearchResult =
      withAuthorizedOrganizationOids(organisaatioOid, AuthorizationRules(Role.Toteutus.readRoles, allowAccessToParentOrganizations = true))(
        organisaatioOids => {
          HakuSearchResult(
            totalCount = r.totalCount,
            result = r.result.map {
              h =>
                HakuSearchItem(
                  oid = h.oid,
                  nimi = h.nimi,
                  organisaatio = h.organisaatio,
                  muokkaaja = h.muokkaaja,
                  modified = h.modified,
                  tila = h.tila,
                  hakutapa = h.hakutapa,
                  koulutuksenAlkamiskausi = h.metadata.koulutuksenAlkamiskausi,
                  hakukohdeCount = getCount(h, organisaatioOids))
            }
          )
        }
      )

    list(organisaatioOid, TilaFilter.alsoArkistoidutAddedToOlemassaolevat(true), true).map(_.oid) match {
      case Nil      => HakuSearchResult()
      case hakuOids => assocHakukohdeCounts(KoutaSearchClient.searchHaut(hakuOids, params))
    }
  }

  private def doPut(haku: Haku)(implicit authenticated: Authenticated): CreateResult =
    KoutaDatabase.runBlockingTransactionally {
      for {
        h <- HakuDAO.getPutActions(haku)
        _ <- setHaunOhjausparametrit(h)
        _ <- auditLog.logCreate(h)
      } yield h
    }.map { h =>
      val warnings = quickIndex(h.oid) ++ index(Some(h))
      CreateResult(h.oid.get, warnings)
    }.get

  private def doUpdate(haku: Haku, notModifiedSince: Instant, before: Haku)(implicit authenticated: Authenticated): UpdateResult =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _ <- HakuDAO.checkNotModified(haku.oid.get, notModifiedSince)
        h <- HakuDAO.getUpdateActions(haku)
        _ <- auditLog.logUpdate(before, h)
      } yield h
    }.map { h =>
      val warnings = quickIndex(h.flatMap(_.oid)) ++ index(h)
      UpdateResult(updated = h.isDefined, warnings)
    }.get

  private def quickIndex(hakuOid: Option[HakuOid]): List[String] = {
    hakuOid match {
      case Some(oid) => koutaIndeksoijaClient.quickIndexEntity("haku", oid.toString)
      case None => List.empty
    }
  }
  private def index(haku: Option[Haku]): List[String] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeHaku, haku.map(_.oid.get.toString))

  def indexByOid(hakuOid: HakuOid): List[String] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeHaku, hakuOid.toString)

  private def setHaunOhjausparametrit(haku: Haku): DBIO[Unit] = {
    Try(ohjausparametritClient.postHaunOhjausparametrit(HaunOhjausparametrit(
      hakuOid = haku.oid.get,
      paikanVastaanottoPaattyy = Some(Instant.ofEpochMilli(46800000L)), // 1970-01-01T15:00+02
      hakijakohtainenPaikanVastaanottoaika = Some(14),
      hakukierrosPaattyy = Some(Instant.now().plus(365, ChronoUnit.DAYS)),
      sijoittelu = Some(false),
      // Tämä on true, koska käyttöliittymässä parametri on päin vastoin eli "Vain yksi hakemus -rajoitus"
      useitaHakemuksia = Some(true),
      jarjestetytHakutoiveet = Some(false),
      hakutoiveidenMaaraRajoitettu = Some(false)))
    ).toDBIO
  }
}
