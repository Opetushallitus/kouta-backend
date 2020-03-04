package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.KoutaIndexClient
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.keyword.{Ammattinimike, Asiasana}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.images.{S3ImageService, TeemakuvaService}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeToteutus}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, KoutaDatabase, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

object ToteutusService extends ToteutusService(SqsInTransactionService, S3ImageService, AuditLog, KeywordService)

class ToteutusService(sqsInTransactionService: SqsInTransactionService, val s3ImageService: S3ImageService, auditLog: AuditLog, keywordService: KeywordService)
  extends ValidatingService[Toteutus] with RoleEntityAuthorizationService with TeemakuvaService[ToteutusOid, Toteutus] {

  protected val roleEntity: RoleEntity = Role.Toteutus

  val teemakuvaPrefix: String = "toteutus-teemakuva"

  def get(oid: ToteutusOid)(implicit authenticated: Authenticated): Option[(Toteutus, Instant)] = {
    val toteutusWithTime = ToteutusDAO.get(oid)
    authorizeGet(toteutusWithTime, AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true, additionalAuthorizedOrganisaatioOids = getTarjoajat(toteutusWithTime)))
  }

  def put(toteutus: Toteutus)(implicit authenticated: Authenticated): ToteutusOid =
    authorizePut(toteutus) {
      withValidation(toteutus, None, doPut)
    }.oid.get

  def update(toteutus: Toteutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    val toteutusWithTime = ToteutusDAO.get(toteutus.oid.get)
    val rules = AuthorizationRules(roleEntity.updateRoles, allowAccessToParentOrganizations = true, additionalAuthorizedOrganisaatioOids = getTarjoajat(toteutusWithTime))
    authorizeUpdate(toteutusWithTime, rules) { oldToteutus =>
      withValidation(toteutus, Some(oldToteutus), doUpdate(_, notModifiedSince, oldToteutus)).nonEmpty
    }
  }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
    withAuthorizedOrganizationOids(organisaatioOid, AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true))(ToteutusDAO.listByAllowedOrganisaatiot)

  def listHaut(oid: ToteutusOid)(implicit authenticated: Authenticated): Seq[HakuListItem] =
    withRootAccess(indexerRoles)(HakuDAO.listByToteutusOid(oid))

  def listHakukohteet(oid: ToteutusOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withRootAccess(indexerRoles)(HakukohdeDAO.listByToteutusOid(oid))

  def listHakukohteet(oid: ToteutusOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] = {
    withAuthorizedChildOrganizationOids(organisaatioOid, Role.Hakukohde.readRoles) {
      HakukohdeDAO.listByToteutusOidAndAllowedOrganisaatiot(oid, _)
    }
  }

  def search(organisaatioOid: OrganisaatioOid, params: Map[String, String])(implicit authenticated: Authenticated): ToteutusSearchResult = {

    def assocHakukohdeCounts(r: ToteutusSearchResult): ToteutusSearchResult =
      r.copy(result = r.result.map {
        t => t.copy(hakukohteet = listHakukohteet(t.oid, organisaatioOid).size)
      })

    list(organisaatioOid).map(_.oid) match {
      case Nil          => ToteutusSearchResult()
      case toteutusOids => assocHakukohdeCounts(KoutaIndexClient.searchToteutukset(toteutusOids, params))
    }
  }

  private def getTarjoajat(maybeToteutusWithTime: Option[(Toteutus, Instant)]): Seq[OrganisaatioOid] =
    maybeToteutusWithTime.map(_._1.tarjoajat).getOrElse(Seq())

  private def doPut(toteutus: Toteutus)(implicit authenticated: Authenticated): Toteutus =
    KoutaDatabase.runBlockingTransactionally {
      for {
        t          <- setMuokkaajaFromSession(toteutus)
        (teema, t) <- checkAndMaybeClearTeemakuva(t)
        _          <- insertAsiasanat(t)
        _          <- insertAmmattinimikkeet(t)
        t          <- ToteutusDAO.getPutActions(t)
        t          <- maybeCopyTeemakuva(teema, t)
        t          <- teema.map(_ => ToteutusDAO.updateJustToteutus(t)).getOrElse(DBIO.successful(t))
        _          <- index(Some(t))
        _          <- auditLog.logCreate(t)
      } yield (teema, t)
    }.map { case (teema, t) =>
      maybeDeleteTempImage(teema)
      t
    }.get

  private def doUpdate(toteutus: Toteutus, notModifiedSince: Instant, before: Toteutus)(implicit authenticated: Authenticated): Option[Toteutus] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _          <- ToteutusDAO.checkNotModified(toteutus.oid.get, notModifiedSince)
        t          <- setMuokkaajaFromSession(toteutus)
        (teema, t) <- checkAndMaybeCopyTeemakuva(t)
        _          <- insertAsiasanat(t)
        _          <- insertAmmattinimikkeet(t)
        t          <- ToteutusDAO.getUpdateActions(t)
        _          <- index(t)
        _          <- auditLog.logUpdate(before, t)
      } yield (teema, t)
    }.map { case (teema, t) =>
      maybeDeleteTempImage(teema)
      t
    }.get

  private def index(toteutus: Option[Toteutus]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeToteutus, toteutus.map(_.oid.get.toString))

  private def insertAsiasanat(toteutus: Toteutus)(implicit authenticated: Authenticated) =
    keywordService.insert(Asiasana, toteutus.metadata.map(_.asiasanat).getOrElse(Seq()))

  private def insertAmmattinimikkeet(toteutus: Toteutus)(implicit authenticated: Authenticated) =
    keywordService.insert(Ammattinimike, toteutus.metadata.map(_.ammattinimikkeet).getOrElse(Seq()))
}
