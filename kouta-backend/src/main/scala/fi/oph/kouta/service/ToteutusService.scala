package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.KoutaIndexClient
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.keyword.{Ammattinimike, Asiasana}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, ToteutusOid, RootOrganisaatioOid}
import fi.oph.kouta.images.{S3ImageService, TeemakuvaService}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeToteutus}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, KoulutusDAO, KoutaDatabase, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.{Authenticated, EntityNotFoundException}
import fi.oph.kouta.validation.Validations
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

object ToteutusService extends ToteutusService(SqsInTransactionService, S3ImageService, AuditLog, KeywordService, OrganisaatioServiceImpl, KoulutusService)

class ToteutusService(sqsInTransactionService: SqsInTransactionService,
                      val s3ImageService: S3ImageService,
                      auditLog: AuditLog,
                      keywordService: KeywordService,
                      val organisaatioService: OrganisaatioService,
                      koulutusService: KoulutusService)
  extends ValidatingService[Toteutus] with RoleEntityAuthorizationService[Toteutus] with TeemakuvaService[ToteutusOid, Toteutus] {

  protected val roleEntity: RoleEntity = Role.Toteutus

  val teemakuvaPrefix: String = "toteutus-teemakuva"

  def get(oid: ToteutusOid)(implicit authenticated: Authenticated): Option[(Toteutus, Instant)] = {
    val toteutusWithTime = ToteutusDAO.get(oid)
    authorizeGet(toteutusWithTime, AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true, additionalAuthorizedOrganisaatioOids = getTarjoajat(toteutusWithTime)))
  }

  def put(toteutus: Toteutus)(implicit authenticated: Authenticated): ToteutusOid = {
    authorizePut(toteutus) { t =>
      withValidation(t, None) { t =>
        validateKoulutusIntegrity(t)
        doPut(t, koulutusService.getAddTarjoajatActions(toteutus.koulutusOid, getTarjoajienOppilaitokset(toteutus)))
      }
    }.oid.get
  }

  def update(toteutus: Toteutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    val toteutusWithTime = ToteutusDAO.get(toteutus.oid.get)
    val rules = AuthorizationRules(roleEntity.updateRoles, allowAccessToParentOrganizations = true, additionalAuthorizedOrganisaatioOids = getTarjoajat(toteutusWithTime))
    authorizeUpdate(toteutusWithTime, toteutus, rules) { (oldToteutus, t) =>
      withValidation(t, Some(oldToteutus)) { t =>
        validateKoulutusIntegrity(t)
        doUpdate(t, notModifiedSince, oldToteutus, koulutusService.getAddTarjoajatActions(toteutus.koulutusOid, getTarjoajienOppilaitokset(toteutus)))
      }
    }
  }.nonEmpty

  def list(organisaatioOid: OrganisaatioOid, vainHakukohteeseenLiitettavat: Boolean = false, myosArkistoidut: Boolean)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
    withAuthorizedOrganizationOids(organisaatioOid, AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true))(ToteutusDAO.listByAllowedOrganisaatiot(_, vainHakukohteeseenLiitettavat, myosArkistoidut))

  def listHaut(oid: ToteutusOid)(implicit authenticated: Authenticated): Seq[HakuListItem] =
    withRootAccess(indexerRoles)(HakuDAO.listByToteutusOid(oid))

  def listHakukohteet(oid: ToteutusOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    withRootAccess(indexerRoles)(HakukohdeDAO.listByToteutusOid(oid))

  def listHakukohteet(oid: ToteutusOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] = {
    withAuthorizedChildOrganizationOids(organisaatioOid, Role.Hakukohde.readRoles) {
      case Seq(RootOrganisaatioOid) => HakukohdeDAO.listByToteutusOid(oid)
      case organisaatioOids => HakukohdeDAO.listByToteutusOidAndAllowedOrganisaatiot(oid, organisaatioOids)
    }
  }

  def search(organisaatioOid: OrganisaatioOid, params: Map[String, String])(implicit authenticated: Authenticated): ToteutusSearchResult = {

    def getCount(k: ToteutusSearchItemFromIndex): Integer =
      withAuthorizedOrganizationOids(organisaatioOid, AuthorizationRules(Role.Toteutus.readRoles, allowAccessToParentOrganizations = true))(
        oids => {
          val oidStrings = oids.map(_.toString())
          return k.hakukohteet.count(x => x.tila != Arkistoitu && oidStrings.contains(x.organisaatioOid))
        }
      )

    def assocHakukohdeCounts(r: ToteutusSearchResultFromIndex): ToteutusSearchResult = {
      ToteutusSearchResult(
        totalCount = r.totalCount,
        result = r.result.map {
            t =>
              ToteutusSearchItem(
                oid = t.oid,
                nimi = t.nimi,
                organisaatio = t.organisaatio,
                muokkaaja = t.muokkaaja,
                modified = t.modified,
                tila = t.tila,
                hakukohdeCount = getCount(t)
              )
          }
      )
    }

    list(organisaatioOid, myosArkistoidut = true).map(_.oid) match {
      case Nil          => ToteutusSearchResult()
      case toteutusOids => assocHakukohdeCounts(KoutaIndexClient.searchToteutukset(toteutusOids, params))
    }
  }

  private def getTarjoajienOppilaitokset(toteutus:Toteutus): Set[OrganisaatioOid] =
    toteutus.tarjoajat.map(OrganisaatioServiceImpl.findOppilaitosOidFromOrganisaationHierarkia).flatten.toSet

  private def getTarjoajat(maybeToteutusWithTime: Option[(Toteutus, Instant)]): Seq[OrganisaatioOid] =
    maybeToteutusWithTime.map(_._1.tarjoajat).getOrElse(Seq())

  private def validateKoulutusIntegrity(toteutus: Toteutus): Unit = {
    import Validations._
    val (koulutusTila, koulutusTyyppi) = KoulutusDAO.getTilaAndTyyppi(toteutus.koulutusOid)

    throwValidationErrors(and(
      validateDependency(toteutus.tila, koulutusTila, toteutus.koulutusOid, "Koulutusta", "koulutusOid"),
      validateIfDefined[Koulutustyyppi](koulutusTyyppi, koulutusTyyppi =>
        validateIfDefined[ToteutusMetadata](toteutus.metadata, toteutusMetadata =>
          assertTrue(koulutusTyyppi == toteutusMetadata.tyyppi, "metadata.tyyppi", tyyppiMismatch("koulutuksen", toteutus.koulutusOid))
        ))
    ))
  }

  private def doPut(toteutus: Toteutus, koulutusAddTarjoajaActions: DBIO[(Koulutus, Option[Koulutus])])(implicit authenticated: Authenticated): Toteutus =
    KoutaDatabase.runBlockingTransactionally {
      for {
        (oldK, k)  <- koulutusAddTarjoajaActions
        (teema, t) <- checkAndMaybeClearTeemakuva(toteutus)
        _          <- insertAsiasanat(t)
        _          <- insertAmmattinimikkeet(t)
        t          <- ToteutusDAO.getPutActions(t)
        t          <- maybeCopyTeemakuva(teema, t)
        t          <- teema.map(_ => ToteutusDAO.updateJustToteutus(t)).getOrElse(DBIO.successful(t))
        _          <- koulutusService.index(k)
        _          <- index(Some(t))
        _          <- auditLog.logUpdate(oldK, k)
        _          <- auditLog.logCreate(t)
      } yield (teema, t)
    }.map { case (teema, t) =>
      maybeDeleteTempImage(teema)
      t
    }.get

  private def doUpdate(toteutus: Toteutus, notModifiedSince: Instant, before: Toteutus, koulutusAddTarjoajaActions: DBIO[(Koulutus, Option[Koulutus])])(implicit authenticated: Authenticated): Option[Toteutus] =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _          <- ToteutusDAO.checkNotModified(toteutus.oid.get, notModifiedSince)
        (oldK, k)  <- koulutusAddTarjoajaActions
        (teema, t) <- checkAndMaybeCopyTeemakuva(toteutus)
        _          <- insertAsiasanat(t)
        _          <- insertAmmattinimikkeet(t)
        t          <- ToteutusDAO.getUpdateActions(t)
        _          <- koulutusService.index(k)
        _          <- index(t)
        _          <- auditLog.logUpdate(oldK, k)
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
