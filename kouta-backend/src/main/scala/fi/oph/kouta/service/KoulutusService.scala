package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.KoutaIndexClient
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, RootOrganisaatioOid}
import fi.oph.kouta.images.{S3ImageService, TeemakuvaService}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeKoulutus}
import fi.oph.kouta.repository.{HakutietoDAO, KoulutusDAO, KoutaDatabase, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.{Authenticated, EntityNotFoundException}
import fi.vm.sade.utils.slf4j.Logging
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

object KoulutusService extends KoulutusService(SqsInTransactionService, S3ImageService, AuditLog, OrganisaatioServiceImpl)

class KoulutusService(sqsInTransactionService: SqsInTransactionService, val s3ImageService: S3ImageService, auditLog: AuditLog, val organisaatioService: OrganisaatioService)
  extends ValidatingService[Koulutus] with RoleEntityAuthorizationService[Koulutus] with TeemakuvaService[KoulutusOid, Koulutus] with Logging {

  protected val roleEntity: RoleEntity = Role.Koulutus
  protected val readRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true)

  val teemakuvaPrefix = "koulutus-teemakuva"

  def get(oid: KoulutusOid)(implicit authenticated: Authenticated): Option[(Koulutus, Instant)] = {
    val koulutusWithTime: Option[(Koulutus, Instant)] = KoulutusDAO.get(oid)
    authorizeGet(koulutusWithTime, AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true, Seq(AuthorizationRuleForJulkinen), getTarjoajat(koulutusWithTime)))
  }

  def put(koulutus: Koulutus)(implicit authenticated: Authenticated): KoulutusOid =
    authorizePut(koulutus) { k =>
      withValidation(k, None)(doPut)
    }.oid.get

  def update(koulutus: Koulutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    val koulutusWithInstant = KoulutusDAO.get(koulutus.oid.get)

    def authorizedForOids(oids: Set[OrganisaatioOid]) = if(oids.nonEmpty) Some(AuthorizationRules(
      roleEntity.updateRoles,
      allowAccessToParentOrganizations = true,
      Seq(AuthorizationRuleForJulkinen),
      oids.toSeq)) else None

    koulutusWithInstant match {
      case Some((oldKoulutus, _)) =>
        val newTarjoajat = koulutus.tarjoajat.toSet
        val oldTarjoajat = oldKoulutus.tarjoajat.toSet

        val updatesOnKoulutus = oldKoulutus.copy(modified=None, tarjoajat = List()) != koulutus.copy(tarjoajat = List())
        val rulesForUpdatingKoulutus = if(updatesOnKoulutus) Some(AuthorizationRules(roleEntity.updateRoles)) else None

        val rulesForAddedTarjoajat = authorizedForOids(newTarjoajat diff oldTarjoajat)
        val rulesForRemovedTarjoajat = authorizedForOids(oldTarjoajat diff newTarjoajat)

        val rules: List[AuthorizationRules] =
          (rulesForUpdatingKoulutus :: rulesForAddedTarjoajat :: rulesForRemovedTarjoajat :: Nil).flatten

        rules.nonEmpty && authorizeUpdate(koulutusWithInstant, koulutus, rules) { (_, k) =>
          withValidation(k, Some(oldKoulutus)) {
            doUpdate(_, notModifiedSince, oldKoulutus)
          }
        }.nonEmpty
      case _ => throw EntityNotFoundException(s"Päivitettävää asiaa ei löytynyt")
    }
  }

  def list(organisaatioOid: OrganisaatioOid, myosArkistoidut: Boolean)(implicit authenticated: Authenticated): Seq[KoulutusListItem] =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(organisaatioOid, readRules) { case (oids, koulutustyypit) =>
      KoulutusDAO.listAllowedByOrganisaatiot(oids, koulutustyypit, myosArkistoidut)
    }

  def getTarjoajanJulkaistutKoulutukset(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[Koulutus] =
    withRootAccess(indexerRoles) {
      KoulutusDAO.getJulkaistutByTarjoajaOids(organisaatioService.getAllChildOidsFlat(organisaatioOid, lakkautetut = true))
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
      case Seq(RootOrganisaatioOid) => ToteutusDAO.listByKoulutusOid(oid)
      case x => ToteutusDAO.listByKoulutusOidAndAllowedOrganisaatiot(oid, x)
    }

  def search(organisaatioOid: OrganisaatioOid, params: Map[String, String])(implicit authenticated: Authenticated): KoulutusSearchResult = {

    def assocToteutusCounts(r: KoulutusSearchResult): KoulutusSearchResult =
      r.copy(result = r.result.map {
          k => k.copy(toteutukset = listToteutukset(k.oid, organisaatioOid).size)
      })

    list(organisaatioOid, myosArkistoidut = true).map(_.oid) match {
      case Nil          => KoulutusSearchResult()
      case koulutusOids => assocToteutusCounts(KoutaIndexClient.searchKoulutukset(koulutusOids, params))
    }
  }

  private def getTarjoajat(maybeKoulutusWithTime: Option[(Koulutus, Instant)]): Seq[OrganisaatioOid] =
    maybeKoulutusWithTime.map(_._1.tarjoajat).getOrElse(Seq())

  private def doPut(koulutus: Koulutus)(implicit authenticated: Authenticated): Koulutus =
    KoutaDatabase.runBlockingTransactionally {
      for {
        (teema, k) <- checkAndMaybeClearTeemakuva(koulutus)
        k          <- KoulutusDAO.getPutActions(k)
        k          <- maybeCopyTeemakuva(teema, k)
        k          <- teema.map(_ => KoulutusDAO.updateJustKoulutus(k)).getOrElse(DBIO.successful(k))
        _          <- index(Some(k))
        _          <- auditLog.logCreate(k)
      } yield (teema, k)
    }.map { case (teema, k) =>
      maybeDeleteTempImage(teema)
      k
    }.get

  private def doUpdate(koulutus: Koulutus, notModifiedSince: Instant, before: Koulutus)(implicit authenticated: Authenticated): Option[Koulutus] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _          <- KoulutusDAO.checkNotModified(koulutus.oid.get, notModifiedSince)
        (teema, k) <- checkAndMaybeCopyTeemakuva(koulutus)
        k          <- KoulutusDAO.getUpdateActions(k)
        _          <- index(k)
        _          <- auditLog.logUpdate(before, k)
      } yield (teema, k)
    }.map { case (teema, k) =>
      maybeDeleteTempImage(teema)
      k
    }.get

  private def index(koulutus: Option[Koulutus]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeKoulutus, koulutus.map(_.oid.get.toString))
}
