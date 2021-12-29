package fi.oph.kouta.service

import java.time.Instant
import java.util.UUID
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{KoutaIndexClient, LokalisointiClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.util.MiscUtils.isToisenAsteenYhteishaku
import fi.oph.kouta.domain.oid.{HakukohdeOid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.domain.{Hakukohde, HakukohdeListItem, HakukohdeSearchResult}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeHakukohde}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, KoutaDatabase, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.{HakukohdeServiceUtil, NameHelper}
import fi.oph.kouta.validation.Validations
import fi.oph.kouta.validation.Validations.validateStateChange
import org.checkerframework.checker.units.qual.m
import slick.dbio.DBIO

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

object HakukohdeService
    extends HakukohdeService(SqsInTransactionService, AuditLog, OrganisaatioServiceImpl, LokalisointiClient)

class HakukohdeService(
    sqsInTransactionService: SqsInTransactionService,
    auditLog: AuditLog,
    val organisaatioService: OrganisaatioService,
    val lokalisointiClient: LokalisointiClient
) extends ValidatingService[Hakukohde]
    with RoleEntityAuthorizationService[Hakukohde] {

  protected val roleEntity: RoleEntity = Role.Hakukohde

  def get(oid: HakukohdeOid, myosPoistetut: Boolean = false)(implicit authenticated: Authenticated): Option[(Hakukohde, Instant)] = {
    val hakukohde = HakukohdeDAO.get(oid, myosPoistetut)

    val enrichedHakukohde = hakukohde match {
      case Some((h, i)) =>
        val toteutus = ToteutusDAO.get(h.toteutusOid)
        toteutus match {
          case Some((t, _)) =>
            val esitysnimi = generateHakukohdeEsitysnimi(h, t.metadata)
            Some(h.copy(_enrichedData = Some(EnrichedData(esitysnimi = esitysnimi))), i)
          case None => hakukohde
        }

      case None => None
     }

    authorizeGet(
      enrichedHakukohde,
      AuthorizationRules(
        roleEntity.readRoles,
        additionalAuthorizedOrganisaatioOids = ToteutusDAO.getTarjoajatByHakukohdeOid(oid)
      )
    )
  }

  def generateHakukohdeEsitysnimi(hakukohde: Hakukohde, toteutusMetadata: Option[ToteutusMetadata]): Kielistetty = {
    toteutusMetadata match {
      case Some(metadata) =>
        metadata match {
          case tuva: TuvaToteutusMetadata =>
            val kaannokset = lokalisointiClient.getKaannoksetWithKey("yleiset.vaativanaErityisenaTukena")
            NameHelper.generateHakukohdeDisplayNameForTuva(hakukohde.nimi, metadata, kaannokset)
          case _ => hakukohde.nimi
        }
      case None => hakukohde.nimi
    }
  }

  def put(hakukohde: Hakukohde)(implicit authenticated: Authenticated): HakukohdeOid = {
    authorizePut(hakukohde) { h =>
      withValidation(h, None) { h =>
        validateDependenciesIntegrity(h, authenticated, "put")
        doPut(h)
      }
    }.oid.get
  }

  def update(hakukohde: Hakukohde, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    val rules = AuthorizationRules(
      roleEntity.updateRoles,
      additionalAuthorizedOrganisaatioOids = ToteutusDAO.getTarjoajatByHakukohdeOid(hakukohde.oid.get)
    )
    authorizeUpdate(HakukohdeDAO.get(hakukohde.oid.get), hakukohde, rules) { (oldHakukohde, h) =>
      withValidation(h, Some(oldHakukohde)) { h =>
        throwValidationErrors(validateStateChange("hakukohteelle", oldHakukohde.tila, hakukohde.tila))
        validateDependenciesIntegrity(h, authenticated, "update")
        doUpdate(h, notModifiedSince, oldHakukohde)
      }
    }.nonEmpty
  }

  def listInclPoistetut(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, roleEntity.readRoles)(HakukohdeDAO.listByAllowedOrganisaatiot)

  def search(organisaatioOid: OrganisaatioOid, params: Map[String, String])(implicit
      authenticated: Authenticated
  ): HakukohdeSearchResult =
    listInclPoistetut(organisaatioOid).map(_.oid) match {
      case Nil           => HakukohdeSearchResult()
      case hakukohdeOids => KoutaIndexClient.searchHakukohteet(hakukohdeOids, params)
    }

  def getOidsByJarjestyspaikkaInclPoistetut(jarjestyspaikkaOid: OrganisaatioOid)(implicit authenticated: Authenticated) =
    withRootAccess(indexerRoles) {
      HakukohdeDAO.getOidsByJarjestyspaikka(jarjestyspaikkaOid);
    }

  private def validateDependenciesIntegrity(hakukohde: Hakukohde, authenticated: Authenticated, method: String): Unit = {
    val isOphPaakayttaja = authenticated.session.roles.contains(Role.Paakayttaja)
    val deps = HakukohdeDAO.getDependencyInformation(hakukohde)
    val haku = HakuDAO.get(hakukohde.hakuOid).map(_._1)

    throwValidationErrors(HakukohdeServiceValidation.validate(hakukohde, haku, isOphPaakayttaja, deps, method))
  }

  private def doPut(hakukohde: Hakukohde)(implicit authenticated: Authenticated): Hakukohde =
    KoutaDatabase.runBlockingTransactionally {
      for {
        h <- HakukohdeDAO.getPutActions(hakukohde)
        _ <- index(Some(h))
        _ <- auditLog.logCreate(h)
      } yield h
    }.get

  private def doUpdate(hakukohde: Hakukohde, notModifiedSince: Instant, before: Hakukohde)(implicit
      authenticated: Authenticated
  ): Option[Hakukohde] =
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
