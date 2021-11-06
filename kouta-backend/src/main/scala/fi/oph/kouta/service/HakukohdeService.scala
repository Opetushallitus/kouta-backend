package fi.oph.kouta.service

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.KoutaIndexClient
import fi.oph.kouta.domain.oid.{HakukohdeOid, OrganisaatioOid}
import fi.oph.kouta.domain.{Hakukohde, HakukohdeListItem, HakukohdeSearchResult}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeHakukohde}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, KoutaDatabase, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import slick.dbio.DBIO

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

object HakukohdeService extends HakukohdeService(SqsInTransactionService, AuditLog, OrganisaatioServiceImpl)

class HakukohdeService(sqsInTransactionService: SqsInTransactionService, auditLog: AuditLog, val organisaatioService: OrganisaatioService) extends ValidatingService[Hakukohde] with RoleEntityAuthorizationService[Hakukohde] {

  protected val roleEntity: RoleEntity = Role.Hakukohde

  def get(oid: HakukohdeOid)(implicit authenticated: Authenticated): Option[(Hakukohde, Instant)] =
    authorizeGet(HakukohdeDAO.get(oid), AuthorizationRules(roleEntity.readRoles, additionalAuthorizedOrganisaatioOids = ToteutusDAO.getTarjoajatByHakukohdeOid(oid)))

  def put(hakukohde: Hakukohde)(implicit authenticated: Authenticated): HakukohdeOid = {
    authorizePut(hakukohde) { h =>
      withValidation(h, None) { h =>
        validateDependenciesIntegrity(h, authenticated)
        doPut(h)
      }
    }.oid.get
  }

  def update(hakukohde: Hakukohde, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    val rules = AuthorizationRules(roleEntity.updateRoles, additionalAuthorizedOrganisaatioOids = ToteutusDAO.getTarjoajatByHakukohdeOid(hakukohde.oid.get))
    authorizeUpdate(HakukohdeDAO.get(hakukohde.oid.get), hakukohde, rules) { (oldHakukohde, h) =>
      withValidation(h, Some(oldHakukohde)) { h =>
        validateDependenciesIntegrity(h, authenticated)
        doUpdate(h, notModifiedSince, oldHakukohde)
      }
    }.nonEmpty
  }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, roleEntity.readRoles)(HakukohdeDAO.listByAllowedOrganisaatiot)

  def search(organisaatioOid: OrganisaatioOid, params: Map[String, String])(implicit authenticated: Authenticated): HakukohdeSearchResult =
    list(organisaatioOid).map(_.oid) match {
      case Nil => HakukohdeSearchResult()
      case hakukohdeOids => KoutaIndexClient.searchHakukohteet(hakukohdeOids, params)
    }

  def getOidsByJarjestyspaikka(jarjestyspaikkaOid: OrganisaatioOid)(implicit authenticated: Authenticated) =
    withRootAccess(indexerRoles) {
      HakukohdeDAO.getOidsByJarjestyspaikka(jarjestyspaikkaOid);
    }

  private def validateDependenciesIntegrity(hakukohde: Hakukohde, authenticated: Authenticated): Unit = {
    val userRoles = authenticated.session.roles
    val isOphPaakayttaja = userRoles.contains(Role.Paakayttaja)
    val deps = HakukohdeDAO.getDependencyInformation(hakukohde)
    val haku = HakuDAO.get(hakukohde.hakuOid).map(_._1)

    throwValidationErrors(HakukohdeServiceValidation.validate(hakukohde, haku, isOphPaakayttaja, deps))
  }

  private def doPut(hakukohde: Hakukohde)(implicit authenticated: Authenticated): Hakukohde =
    KoutaDatabase.runBlockingTransactionally {
      for {
        h <- HakukohdeDAO.getPutActions(hakukohde)
        _ <- index(Some(h))
        _ <- auditLog.logCreate(h)
      } yield h
    }.get

  private def doUpdate(hakukohde: Hakukohde, notModifiedSince: Instant, before: Hakukohde)(implicit authenticated: Authenticated): Option[Hakukohde] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _ <- HakukohdeDAO.checkNotModified(hakukohde.oid.get, notModifiedSince)
        h <- HakukohdeDAO.getUpdateActions(hakukohde)
        _ <- index(h)
        _ <- auditLog.logUpdate(before, h)
      } yield h
    }.get

  private def index(hakukohde: Option[Hakukohde]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeHakukohde, hakukohde.map(_.oid.get.toString))

}
