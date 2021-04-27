package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid, RootOrganisaatioOid}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeHaku}
import fi.oph.kouta.repository.DBIOHelpers.try2DBIOCapableTry
import fi.oph.kouta.repository._
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

object HakuService extends HakuService(SqsInTransactionService, AuditLog, OhjausparametritClient, OrganisaatioServiceImpl)

class HakuService(sqsInTransactionService: SqsInTransactionService,
                  auditLog: AuditLog,
                  ohjausparametritClient: OhjausparametritClient,
                  val organisaatioService: OrganisaatioService)
  extends ValidatingService[Haku] with RoleEntityAuthorizationService[Haku] {

  override val roleEntity: RoleEntity = Role.Haku
  protected val readRules: AuthorizationRules = AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true)

  def get(oid: HakuOid)(implicit authenticated: Authenticated): Option[(Haku, Instant)] =
    authorizeGet(HakuDAO.get(oid), readRules)

  def put(haku: Haku)(implicit authenticated: Authenticated): HakuOid =
    authorizePut(haku) { h =>
      withValidation(h, None)(haku => doPut(haku))
    }.oid.get

  def update(haku: Haku, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(HakuDAO.get(haku.oid.get), haku) { (oldHaku, h) =>
      withValidation(h, Some(oldHaku)) {
        doUpdate(_, notModifiedSince, oldHaku)
      }
    }.nonEmpty

  def list(organisaatioOid: OrganisaatioOid, myosArkistoidut: Boolean)(implicit authenticated: Authenticated): Seq[HakuListItem] =
    withAuthorizedOrganizationOids(organisaatioOid, readRules)(oids => HakuDAO.listByAllowedOrganisaatiot(oids, myosArkistoidut))

  def listHakukohteet(hakuOid: HakuOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withRootAccess(indexerRoles)(HakukohdeDAO.listByHakuOid(hakuOid))

  def listHakukohteet(hakuOid: HakuOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, Role.Hakukohde.readRoles) {
      case Seq(RootOrganisaatioOid) => HakukohdeDAO.listByHakuOid(hakuOid)
      case x => HakukohdeDAO.listByHakuOidAndAllowedOrganisaatiot(hakuOid, x)
    }

  def listKoulutukset(hakuOid: HakuOid)(implicit authenticated: Authenticated): Seq[KoulutusListItem] =
    withRootAccess(indexerRoles)(KoulutusDAO.listByHakuOid(hakuOid))

  def listToteutukset(hakuOid: HakuOid)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
    withRootAccess(indexerRoles)(ToteutusDAO.listByHakuOid(hakuOid))

  def search(organisaatioOid: OrganisaatioOid, params: Map[String, String])(implicit authenticated: Authenticated): HakuSearchResult = {
    def getCount(t: HakuSearchItemFromIndex, organisaatioOids: Seq[OrganisaatioOid]): Integer = {
      organisaatioOids match {
        case Seq(RootOrganisaatioOid) => t.hakukohteet.length
        case _ =>
          val oidStrings = organisaatioOids.map(_.toString())
          t.hakukohteet.count(x => x.tila != Arkistoitu && oidStrings.contains(x.organisaatio.oid.toString()))
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
                  hakukohdeCount = getCount(h, organisaatioOids))
            }
          )
        }
      )

    list(organisaatioOid, myosArkistoidut = true).map(_.oid) match {
      case Nil      => HakuSearchResult()
      case hakuOids => assocHakukohdeCounts(KoutaIndexClient.searchHaut(hakuOids, params))
    }
  }

  private def doPut(haku: Haku)(implicit authenticated: Authenticated): Haku =
    KoutaDatabase.runBlockingTransactionally {
      for {
        h <- HakuDAO.getPutActions(haku)
        _ <- setHaunOhjausparametrit(h)
        _ <- index(Some(h))
        _ <- auditLog.logCreate(h)
      } yield h
    }.get

  private def doUpdate(haku: Haku, notModifiedSince: Instant, before: Haku)(implicit authenticated: Authenticated): Option[Haku] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _ <- HakuDAO.checkNotModified(haku.oid.get, notModifiedSince)
        h <- HakuDAO.getUpdateActions(haku)
        _ <- index(h)
        _ <- auditLog.logUpdate(before, h)
      } yield h
    }.get

  private def index(haku: Option[Haku]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeHaku, haku.map(_.oid.get.toString))

  private def setHaunOhjausparametrit(haku: Haku): DBIO[Unit] =
    Try(ohjausparametritClient.postHaunOhjausparametrit(HaunOhjausparametrit(
      hakuOid = haku.oid.get,
      paikanVastaanottoPaattyy = Some(Instant.ofEpochMilli(46800000L)), // 1970-01-01T15:00+02
      hakijakohtainenPaikanVastaanottoaika = Some(14),
      hakukierrosPaattyy = Some(Instant.ofEpochMilli(1640987999000L)))) // 2021-12-31T23:59:59+02
    ).toDBIO
}
