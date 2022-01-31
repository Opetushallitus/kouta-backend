package fi.oph.kouta.service

import com.hubspot.jinjava.lib.filter.ListFilter

import java.time.Instant
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{KoodistoClient, KoutaIndexClient, LokalisointiClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.keyword.{Ammattinimike, Asiasana}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid, ToteutusOid}
import fi.oph.kouta.images.{S3ImageService, TeemakuvaService}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeToteutus}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, KoulutusDAO, KoutaDatabase, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.{Authenticated, EntityNotFoundException}
import fi.oph.kouta.util.NameHelper
import fi.oph.kouta.validation.Validations
import fi.oph.kouta.validation.Validations.{TutkintonimikeKoodiPattern, assertTrue, integrityViolationMsg, validateIfTrue, validateStateChange}
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext.Implicits.global

object ToteutusService extends ToteutusService(SqsInTransactionService, S3ImageService, AuditLog, KeywordService, OrganisaatioServiceImpl, KoulutusService, LokalisointiClient, KoodistoClient)

class ToteutusService(sqsInTransactionService: SqsInTransactionService,
                      val s3ImageService: S3ImageService,
                      auditLog: AuditLog,
                      keywordService: KeywordService,
                      val organisaatioService: OrganisaatioService,
                      koulutusService: KoulutusService,
                      lokalisointiClient: LokalisointiClient,
                      koodistoClient: KoodistoClient
                     )
  extends ValidatingService[Toteutus] with RoleEntityAuthorizationService[Toteutus] with TeemakuvaService[ToteutusOid, Toteutus] {

  protected val roleEntity: RoleEntity = Role.Toteutus

  val teemakuvaPrefix: String = "toteutus-teemakuva"

  def generateToteutusEsitysnimi(toteutus: Toteutus): Kielistetty = {
    (toteutus.metadata, toteutus.koulutusMetadata) match {
      case (Some(toteutusMetadata), Some(koulutusMetadata)) =>
        (toteutusMetadata, koulutusMetadata) match {
          case (lukioToteutusMetadata: LukioToteutusMetadata, lukioKoulutusMetadata: LukioKoulutusMetadata) => {
            val kaannokset = Map(
              "yleiset.opintopistetta" -> lokalisointiClient.getKaannoksetWithKey("yleiset.opintopistetta"),
              "toteutuslomake.lukionYleislinjaNimiOsa" -> lokalisointiClient.getKaannoksetWithKey(
                "toteutuslomake.lukionYleislinjaNimiOsa"
              )
            )
            val painotuksetKaannokset      = koodistoClient.getKoodistoKaannokset("lukiopainotukset")
            val koulutustehtavatKaannokset = koodistoClient.getKoodistoKaannokset("lukiolinjaterityinenkoulutustehtava")
            val koodistoKaannokset         = (painotuksetKaannokset.toSeq ++ koulutustehtavatKaannokset.toSeq).toMap
            NameHelper.generateLukioToteutusDisplayName(
              lukioToteutusMetadata,
              lukioKoulutusMetadata,
              kaannokset,
              koodistoKaannokset
            )
          }
          case _ => toteutus.nimi
        }
      case _ => toteutus.nimi
    }
  }

  def get(oid: ToteutusOid, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Option[(Toteutus, Instant)] = {
    val toteutusWithTime = ToteutusDAO.get(oid, tilaFilter)
    val enrichedToteutus = toteutusWithTime match {
      case Some((t, i)) => {
        val esitysnimi = generateToteutusEsitysnimi(t)
        Some(t.withEnrichedData(ToteutusEnrichedData(esitysnimi)).withoutRelatedData(), i)
      }
      case None => None
    }
    authorizeGet(enrichedToteutus, AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true, additionalAuthorizedOrganisaatioOids = getTarjoajat(toteutusWithTime)))
  }

  def put(toteutus: Toteutus)(implicit authenticated: Authenticated): ToteutusOid = {
    authorizePut(toteutus) { t =>
      withValidation(t, None) { t =>
        validateKoulutusIntegrity(t)
        doPut(t, koulutusService.getAddTarjoajatActions(toteutus.koulutusOid, getTarjoajienOppilaitokset(toteutus)))
      }
    }.oid.get
  }

  def put(toteutusOidit: List[ToteutusOid])(implicit authenticated: Authenticated): List[ToteutusOid] = {
    toteutusOidit.map(oid => {
      val toteutusWithTime = ToteutusDAO.get(oid, TilaFilter.all())
      toteutusWithTime match {
        case Some((t, _)) =>
          println(t)
          val toteutusCopyAsLuonnos = t.copy(tila = Tallennettu)
          val oidOfTheCopy = put(toteutusCopyAsLuonnos)
          println(oidOfTheCopy)
          Some(oidOfTheCopy)
        case None =>
          println(s"No toteutus with the oid ${oid}")
          None
      }
    }).flatten
  }

  def update(toteutus: Toteutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    val toteutusWithTime = ToteutusDAO.get(toteutus.oid.get, TilaFilter.onlyOlemassaolevat())
    val rules = AuthorizationRules(roleEntity.updateRoles, allowAccessToParentOrganizations = true, additionalAuthorizedOrganisaatioOids = getTarjoajat(toteutusWithTime))
    authorizeUpdate(toteutusWithTime, toteutus, rules) { (oldToteutus, t) =>
      withValidation(t, Some(oldToteutus)) { t =>
        throwValidationErrors(validateStateChange("toteutukselle", oldToteutus.tila, toteutus.tila))
        validateKoulutusIntegrity(t)
        validateHakukohdeIntegrityIfDeletingToteutus(oldToteutus.tila, toteutus.tila, toteutus.oid.get)
        doUpdate(t, notModifiedSince, oldToteutus, koulutusService.getAddTarjoajatActions(toteutus.koulutusOid, getTarjoajienOppilaitokset(toteutus)))
      }
    }
  }.nonEmpty

  def list(organisaatioOid: OrganisaatioOid, vainHakukohteeseenLiitettavat: Boolean = false, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
    withAuthorizedOrganizationOids(organisaatioOid,
      AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true))(
      ToteutusDAO.listByAllowedOrganisaatiot(_, vainHakukohteeseenLiitettavat, tilaFilter))

  def listHaut(oid: ToteutusOid, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Seq[HakuListItem] =
    withRootAccess(indexerRoles)(HakuDAO.listByToteutusOid(oid, tilaFilter))

  def listHakukohteet(oid: ToteutusOid, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] = {
    withRootAccess(indexerRoles)(HakukohdeDAO.listByToteutusOid(oid, tilaFilter))
  }

  def listHakukohteet(oid: ToteutusOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] = {
    withAuthorizedChildOrganizationOids(organisaatioOid, Role.Hakukohde.readRoles) {
      case Seq(RootOrganisaatioOid) => HakukohdeDAO.listByToteutusOid(oid, TilaFilter.onlyOlemassaolevat())
      case organisaatioOids => HakukohdeDAO.listByToteutusOidAndAllowedOrganisaatiot(oid, organisaatioOids)
    }
  }

  def search(organisaatioOid: OrganisaatioOid, params: Map[String, String])(implicit authenticated: Authenticated): ToteutusSearchResult = {

    def getCount(t: ToteutusSearchItemFromIndex, organisaatioOids: Seq[OrganisaatioOid]): Integer = {
      organisaatioOids match {
        case Seq(RootOrganisaatioOid) => t.hakukohteet.length
        case _ =>
          val oidStrings = organisaatioOids.map(_.toString())
          t.hakukohteet.count(x => x.tila != Arkistoitu && x.tila != Poistettu && oidStrings.contains(x.organisaatio.oid.toString()))
      }
    }

    def assocHakukohdeCounts(r: ToteutusSearchResultFromIndex): ToteutusSearchResult =
      withAuthorizedOrganizationOids(organisaatioOid, AuthorizationRules(Role.Toteutus.readRoles, allowAccessToParentOrganizations = true))(
        organisaatioOids => {
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
                  koulutustyyppi = t.koulutustyyppi,
                  hakukohdeCount = getCount(t, organisaatioOids))
            }
          )
        }
      )

    list(organisaatioOid, false, TilaFilter.alsoArkistoidutAddedToOlemassaolevat(true)).map(_.oid) match {
      case Nil          => ToteutusSearchResult()
      case toteutusOids => assocHakukohdeCounts(KoutaIndexClient.searchToteutukset(toteutusOids, params))
    }
  }

  def search(organisaatioOid: OrganisaatioOid, toteutusOid: ToteutusOid, params: Map[String, String])(implicit authenticated: Authenticated): Option[ToteutusSearchItemFromIndex] = {
    def filterHakukohteet(toteutus: Option[ToteutusSearchItemFromIndex]): Option[ToteutusSearchItemFromIndex] =
      withAuthorizedOrganizationOids(organisaatioOid, AuthorizationRules(Role.Toteutus.readRoles, allowAccessToParentOrganizations = true)) {
        case Seq(RootOrganisaatioOid) => toteutus
        case organisaatioOids => {
          toteutus.flatMap(toteutusItem => {
            val oidStrings = organisaatioOids.map(_.toString())
            Some(toteutusItem.copy(hakukohteet = toteutusItem.hakukohteet.filter(hakukohde => oidStrings.contains(hakukohde.organisaatio.oid.toString()))))
          })
        }
      }

    filterHakukohteet(KoutaIndexClient.searchToteutukset(Seq(toteutusOid), params).result.headOption)
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
      validateIfDefined[Koulutustyyppi](koulutusTyyppi, koulutusTyyppi => and(
        validateIfTrue(koulutusTyyppi != Lk, validateKielistetty(toteutus.kielivalinta, toteutus.nimi, "nimi")),
        validateIfDefined[ToteutusMetadata](toteutus.metadata, toteutusMetadata =>
          assertTrue(koulutusTyyppi == toteutusMetadata.tyyppi, "metadata.tyyppi", tyyppiMismatch("koulutuksen", toteutus.koulutusOid))
        ))
      )
    ))
  }

  private def validateHakukohdeIntegrityIfDeletingToteutus(aiempiTila: Julkaisutila, tulevaTila: Julkaisutila, toteutusOid: ToteutusOid) = {
    throwValidationErrors(
      validateIfTrue(tulevaTila == Poistettu && tulevaTila != aiempiTila, assertTrue(
        HakukohdeDAO.listByToteutusOid(toteutusOid, TilaFilter.onlyOlemassaolevat()).isEmpty,
        "tila",
        integrityViolationMsg("Toteutusta", "hakukohteita")))
    )
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
