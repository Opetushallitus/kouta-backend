package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{KoutaIndexClient, OrganisaatioClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid}
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeKoulutus}
import fi.oph.kouta.indexing.{S3Service, SqsInTransactionService}
import fi.oph.kouta.repository.{HakutietoDAO, KoulutusDAO, KoutaDatabase, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

object KoulutusService extends KoulutusService(SqsInTransactionService, S3Service, AuditLog)

class KoulutusService(sqsInTransactionService: SqsInTransactionService, val s3Service: S3Service, auditLog: AuditLog)
  extends ValidatingService[Koulutus] with RoleEntityAuthorizationService with TeemakuvaService[KoulutusOid, Koulutus, KoulutusMetadata] {

  protected val roleEntity: RoleEntity = Role.Koulutus
  protected val readRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles, true)

  val teemakuvaPrefix = "koulutus-teemakuva"

  def get(oid: KoulutusOid)(implicit authenticated: Authenticated): Option[(Koulutus, Instant)] = {
    val koulutusWithTime: Option[(Koulutus, Instant)] = KoulutusDAO.get(oid)
    authorizeGet(koulutusWithTime, AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true, Seq(AuthorizationRuleForJulkinen), getTarjoajat(koulutusWithTime)))
  }

  def put(koulutus: Koulutus)(implicit authenticated: Authenticated): KoulutusOid =
    authorizePut(koulutus) {
      withValidation(koulutus, doPut)
    }.oid.get

  //TODO: Tarkista oikeudet, kun tarjoajien lisäämiseen tarkoitettu rajapinta tulee käyttöön
  def update(koulutus: Koulutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    val koulutusWithTime: Option[(Koulutus, Instant)] = KoulutusDAO.get(koulutus.oid.get)
    val rules = AuthorizationRules(roleEntity.updateRoles, allowAccessToParentOrganizations = true, Seq(AuthorizationRuleForJulkinen), getTarjoajat(koulutusWithTime))
    authorizeUpdate(koulutusWithTime, rules) { oldKoulutus =>
      withValidation(koulutus, doUpdate(_, notModifiedSince, oldKoulutus))
    }.nonEmpty
  }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[KoulutusListItem] =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(organisaatioOid, readRules) { case (oids, koulutustyypit) =>
      KoulutusDAO.listAllowedByOrganisaatiot(oids, koulutustyypit)
    }

  def getTarjoajanJulkaistutKoulutukset(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[Koulutus] =
    withRootAccess(indexerRoles) {
      KoulutusDAO.getJulkaistutByTarjoajaOids(OrganisaatioClient.getAllChildOidsAndOppilaitostyypitFlat(organisaatioOid)._1)
    }

  def toteutukset(oid: KoulutusOid, vainJulkaistut: Boolean)(implicit authenticated: Authenticated): Seq[Toteutus] =
    withRootAccess(indexerRoles) {
      if (vainJulkaistut) {
        ToteutusDAO.getJulkaistutByKoulutusOid(oid)
      } else {
        ToteutusDAO.getByKoulutusOid(oid)
      }
    }

  def hakutiedot(oid: KoulutusOid)(implicit authenticated: Authenticated): Seq[Hakutieto] =
    withRootAccess(indexerRoles) {
      HakutietoDAO.getByKoulutusOid(oid)
    }

  def listToteutukset(oid: KoulutusOid)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
    withRootAccess(indexerRoles)(ToteutusDAO.listByKoulutusOid(oid))

  def listToteutukset(oid: KoulutusOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
    withAuthorizedOrganizationOids(organisaatioOid, AuthorizationRules(Role.Toteutus.readRoles, allowAccessToParentOrganizations = true)) {
      ToteutusDAO.listByKoulutusOidAndAllowedOrganisaatiot(oid, _)
    }

  def search(organisaatioOid: OrganisaatioOid, params: Map[String, String])(implicit authenticated: Authenticated): KoulutusSearchResult = {

    def assocToteutusCounts(r: KoulutusSearchResult): KoulutusSearchResult =
      r.copy(result = r.result.map {
          k => k.copy(toteutukset = listToteutukset(k.oid, organisaatioOid).size)
      })

    list(organisaatioOid).map(_.oid) match {
      case Nil          => KoulutusSearchResult()
      case koulutusOids => assocToteutusCounts(KoutaIndexClient.searchKoulutukset(koulutusOids, params))
    }
  }

  private def getTarjoajat(maybeKoulutusWithTime: Option[(Koulutus, Instant)]): Seq[OrganisaatioOid] =
    maybeKoulutusWithTime.map(_._1.tarjoajat).getOrElse(Seq())

  private def doPut(koulutus: Koulutus)(implicit authenticated: Authenticated): Koulutus =
    KoutaDatabase.runBlockingTransactionally {
      for {
        (teema, k) <- checkAndMaybeClearTempImage(koulutus)
        k          <- KoulutusDAO.getPutActions(k)
        k          <- maybeCopyThemeImage(teema, k)
        k          <- teema.map(_ => KoulutusDAO.updateJustKoulutus(k)).getOrElse(DBIO.successful(k))
        _          <- maybeDeleteTempImage(teema)
        _          <- index(Some(k))
        _          <- auditLog.logCreate(k)
      } yield k
    }.get

  private def doUpdate(koulutus: Koulutus, notModifiedSince: Instant, before: Koulutus)(implicit authenticated: Authenticated): Option[Koulutus] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _          <- KoulutusDAO.checkNotModified(koulutus.oid.get, notModifiedSince)
        (teema, k) <- checkAndMaybeCopyTempImage(koulutus)
        k          <- KoulutusDAO.getUpdateActions(k)
        _          <- maybeDeleteTempImage(teema)
        _          <- index(k)
        _          <- auditLog.logUpdate(before, k)
      } yield k
    }.get

  private def index(koulutus: Option[Koulutus]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeKoulutus, koulutus.map(_.oid.get.toString))
}
