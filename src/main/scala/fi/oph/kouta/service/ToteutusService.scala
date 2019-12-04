package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.client.KoutaIndexClient
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.keyword.{Ammattinimike, Asiasana}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeToteutus}
import fi.oph.kouta.indexing.{S3Service, SqsInTransactionService}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.AuditLog
import fi.vm.sade.auditlog.User
import javax.servlet.http.HttpServletRequest

import scala.concurrent.ExecutionContext.Implicits.global

object ToteutusService extends ToteutusService(SqsInTransactionService, S3Service, AuditLog, KeywordService)

class ToteutusService(sqsInTransactionService: SqsInTransactionService, val s3Service: S3Service, auditLog: AuditLog, keywordService: KeywordService)
  extends ValidatingService[Toteutus] with RoleEntityAuthorizationService with TeemakuvaService[ToteutusOid, Toteutus, ToteutusMetadata] {

  protected val roleEntity: RoleEntity = Role.Toteutus

  val teemakuvaPrefix: String = "toteutus-teemakuva"

  def get(oid: ToteutusOid)(implicit authenticated: Authenticated): Option[(Toteutus, Instant)] = {
    val toteutusWithTime = ToteutusDAO.get(oid)
    authorizeGet(toteutusWithTime, AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true, additionalAuthorizedOrganisaatioOids = getTarjoajat(toteutusWithTime)))
  }

  def put(toteutus: Toteutus)(implicit authenticated: Authenticated, request: HttpServletRequest): ToteutusOid =
    authorizePut(toteutus) {
      withValidation(toteutus, putWithIndexing(_, auditLog.getUser))
    }.oid.get

  def update(toteutus: Toteutus, notModifiedSince: Instant)(implicit authenticated: Authenticated, request: HttpServletRequest): Boolean = {
    val toteutusWithTime = ToteutusDAO.get(toteutus.oid.get)
    val rules = AuthorizationRules(roleEntity.updateRoles, allowAccessToParentOrganizations = true, additionalAuthorizedOrganisaatioOids = getTarjoajat(toteutusWithTime))
    authorizeUpdate(toteutusWithTime, rules) { oldToteutus =>
      withValidation(toteutus, updateWithIndexing(_, notModifiedSince, auditLog.getUser, oldToteutus)).nonEmpty
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

  private def putWithIndexing(toteutus: Toteutus, user: User) = {
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeToteutus,
      () => themeImagePutActions(toteutus, putActions(_, user), updateActions(_, _, user)),
      (added: Toteutus) => added.oid.get.toString,
      (added: Toteutus) => auditLog.logCreate(added, user))
  }

  private def updateWithIndexing(toteutus: Toteutus, notModifiedSince: Instant, user: User, before: Toteutus) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeToteutus,
      () => themeImageUpdateActions(toteutus, updateActions(_, notModifiedSince, user)),
      toteutus.oid.get.toString,
      (updated: Option[Toteutus]) => auditLog.logUpdate(before, updated, user))

  private def updateActions(toteutus: Toteutus, notModifiedSince: Instant, user: User) =
    for {
      _ <- insertAsiasanat(toteutus, user)
      _ <- insertAmmattinimikkeet(toteutus, user)
      t <- ToteutusDAO.getUpdateActions(toteutus, notModifiedSince)
    } yield t

  private def putActions(toteutus: Toteutus, user: User) =
    for {
      _ <- insertAsiasanat(toteutus, user)
      _ <- insertAmmattinimikkeet(toteutus, user)
      t <- ToteutusDAO.getPutActions(toteutus)
    } yield t

  private def insertAsiasanat(toteutus: Toteutus, user: User) =
    keywordService.insert(Asiasana, user, toteutus.metadata.map(_.asiasanat).getOrElse(Seq()))

  private def insertAmmattinimikkeet(toteutus: Toteutus, user: User) =
    keywordService.insert(Ammattinimike, user, toteutus.metadata.map(_.ammattinimikkeet).getOrElse(Seq()))
}
