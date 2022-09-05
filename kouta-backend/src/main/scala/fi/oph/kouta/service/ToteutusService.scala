package fi.oph.kouta.service

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.keyword.{Ammattinimike, Asiasana}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid, ToteutusOid}
import fi.oph.kouta.domain.searchResults.ToteutusSearchResultFromIndex
import fi.oph.kouta.images.{S3ImageService, TeemakuvaService}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeToteutus}
import fi.oph.kouta.repository._
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.{Authenticated, EntityNotFoundException, SearchParams}
import fi.oph.kouta.util.MiscUtils.{isDIAlukiokoulutus, isEBlukiokoulutus}
import fi.oph.kouta.util.{NameHelper, ServiceUtils}
import fi.oph.kouta.validation.Validations.{assertTrue, integrityViolationMsg, validateIfTrue, validateStateChange}
import fi.oph.kouta.validation.{IsValid, NoErrors, Validations}
import slick.dbio.DBIO

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

object ToteutusService extends ToteutusService(SqsInTransactionService, S3ImageService, AuditLog, KeywordService, OrganisaatioServiceImpl, KoulutusService, LokalisointiClient, KoodistoKaannosClient, OppijanumerorekisteriClient, KayttooikeusClient, ToteutusServiceValidation)



case class ToteutusCopyOids(
                     toteutusOid: Option[ToteutusOid]
                   )

case class ToteutusCopyResultObject(
                                     oid: ToteutusOid,
                                     status: String,
                                     created: ToteutusCopyOids
                                   )

class ToteutusService(sqsInTransactionService: SqsInTransactionService,
                      val s3ImageService: S3ImageService,
                      auditLog: AuditLog,
                      keywordService: KeywordService,
                      val organisaatioService: OrganisaatioService,
                      koulutusService: KoulutusService,
                      lokalisointiClient: LokalisointiClient,
                      koodistoClient: KoodistoKaannosClient,
                      oppijanumerorekisteriClient: OppijanumerorekisteriClient,
                      kayttooikeusClient: KayttooikeusClient,
                      toteutusServiceValidation: ToteutusServiceValidation
  )
  extends RoleEntityAuthorizationService[Toteutus] with TeemakuvaService[ToteutusOid, Toteutus] {

  protected val roleEntity: RoleEntity = Role.Toteutus

  val teemakuvaPrefix: String = "toteutus-teemakuva"

  def generateToteutusEsitysnimi(toteutus: Toteutus): Kielistetty = {
    val koulutuksetKoodiUri = toteutus.koulutuksetKoodiUri
    if (!koulutuksetKoodiUri.isEmpty && (isDIAlukiokoulutus(koulutuksetKoodiUri) || isEBlukiokoulutus(koulutuksetKoodiUri))) {
      toteutus.nimi
    } else {
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
  }

  private def enrichToteutusMetadata(toteutus: Toteutus) : Option[ToteutusMetadata] = {
    val muokkaajanOrganisaatiot = kayttooikeusClient.getOrganisaatiot(toteutus.muokkaaja)
    val isOphVirkailija = ServiceUtils.hasOphOrganisaatioOid(muokkaajanOrganisaatiot)

    toteutus.metadata match {
      case Some(metadata) =>
        metadata match {
          case yoMetadata: YliopistoToteutusMetadata => Some(yoMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case amkMetadata: AmmattikorkeakouluToteutusMetadata => Some(amkMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case ammOpeErityisopeJaOpoToteutusMetadata: AmmOpeErityisopeJaOpoToteutusMetadata => Some(ammOpeErityisopeJaOpoToteutusMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case ammMetadata: AmmatillinenToteutusMetadata => Some(ammMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case tutkintoonJohtamatonToteutusMetadata: TutkintoonJohtamatonToteutusMetadata =>
            tutkintoonJohtamatonToteutusMetadata match {
              case ammTutkinnonOsaMetadata: AmmatillinenTutkinnonOsaToteutusMetadata => Some(ammTutkinnonOsaMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
              case ammOsaamisalaMetadata: AmmatillinenOsaamisalaToteutusMetadata => Some(ammOsaamisalaMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
              case ammatillinenMuuToteutusMetadata: AmmatillinenMuuToteutusMetadata => Some(ammatillinenMuuToteutusMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
              case kkOpintojaksoMetadata: KkOpintojaksoToteutusMetadata => Some(kkOpintojaksoMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
              case vapaaSivistystyoMuuToteutusMetadata: VapaaSivistystyoMuuToteutusMetadata => Some(vapaaSivistystyoMuuToteutusMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
              case aikuistenPerusopetusToteutusMetadata: AikuistenPerusopetusToteutusMetadata => Some(aikuistenPerusopetusToteutusMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
            }
          case lukioMetadata: LukioToteutusMetadata => Some(lukioMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case tuvaMetadata: TuvaToteutusMetadata => Some(tuvaMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case telmaMetadata: TelmaToteutusMetadata => Some(telmaMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case vapaaSivistystyoOpistovuosiMetadata: VapaaSivistystyoOpistovuosiToteutusMetadata => Some(vapaaSivistystyoOpistovuosiMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case erikoislaakariToteutusMetadata: ErikoislaakariToteutusMetadata => Some(erikoislaakariToteutusMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
        }
      case None => None
    }
  }

  def get(oid: ToteutusOid, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Option[(Toteutus, Instant)] = {
    val toteutusWithTime = ToteutusDAO.get(oid, tilaFilter)
    val enrichedToteutus = toteutusWithTime match {
      case Some((t, i)) =>
        val esitysnimi = generateToteutusEsitysnimi(t)
        val muokkaaja = oppijanumerorekisteriClient.getHenkilö(t.muokkaaja)
        val muokkaajanNimi = NameHelper.generateMuokkaajanNimi(muokkaaja)
        Some(t.withEnrichedData(ToteutusEnrichedData(esitysnimi, Some(muokkaajanNimi))).withoutRelatedData(), i)
      case None => None
    }
    authorizeGet(enrichedToteutus, AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true, additionalAuthorizedOrganisaatioOids = getTarjoajat(toteutusWithTime)))
  }

  def put(toteutus: Toteutus)(implicit authenticated: Authenticated): ToteutusOid = {
    authorizePut(toteutus) { t =>
      val enrichedMetadata: Option[ToteutusMetadata] = enrichToteutusMetadata(t)
      val enrichedToteutus = t.copy(metadata = enrichedMetadata)
      toteutusServiceValidation.withValidation(enrichedToteutus, None) { t =>
        doPut(t, koulutusService.getUpdateTarjoajatActions(toteutus.koulutusOid, getTarjoajienOppilaitokset(toteutus), Set()))
      }
    }.oid.get
  }

  def copy(toteutusOids: List[ToteutusOid])(implicit authenticated: Authenticated): Seq[ToteutusCopyResultObject] = {
    val toteutukset = ToteutusDAO.getToteutuksetByOids(toteutusOids)
    toteutukset.map(toteutus => {
      try {
        val toteutusCopyAsLuonnos = toteutus.copy(tila = Tallennettu)
        val createdToteutusOid = put(toteutusCopyAsLuonnos)
        ToteutusCopyResultObject(oid = toteutus.oid.get, status = "success", created = ToteutusCopyOids(Some(createdToteutusOid)))
      } catch {
        case error: Throwable =>
          logger.error(s"Copying toteutus failed: $error")
          ToteutusCopyResultObject(oid = toteutus.oid.get, status = "error", created = ToteutusCopyOids(None))
      }
    })
  }

  def update(toteutus: Toteutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    val toteutusWithTime = ToteutusDAO.get(toteutus.oid.get, TilaFilter.onlyOlemassaolevat())
    val rules: AuthorizationRules = getAuthorizationRulesForUpdate(toteutusWithTime, toteutus)

    authorizeUpdate(toteutusWithTime, toteutus, rules) { (oldToteutus, t) =>
      val enrichedMetadata: Option[ToteutusMetadata] = enrichToteutusMetadata(t)
      val enrichedToteutus = t.copy(metadata = enrichedMetadata)
      toteutusServiceValidation.withValidation(enrichedToteutus, Some(oldToteutus)) { t =>
        val deletedTarjoajat =
          if (t.tila == Poistettu) t.tarjoajat else oldToteutus.tarjoajat diff t.tarjoajat
        doUpdate(t, notModifiedSince, oldToteutus,
          koulutusService.getUpdateTarjoajatActions(t.koulutusOid, getTarjoajienOppilaitokset(t), getDeletableTarjoajienOppilaitokset(t, deletedTarjoajat)))
      }
    }
  }.nonEmpty

  private def getAuthorizationRulesForUpdate(toteutusWithTime: Option[(Toteutus, Instant)], newToteutus: Toteutus): AuthorizationRules = {
    toteutusWithTime match {
      case None => throw EntityNotFoundException(s"Päivitettävää toteutusta ei löytynyt")
      case Some((oldToteutus, _)) =>
        if (Julkaisutila.isTilaUpdateAllowedOnlyForOph(oldToteutus.tila, newToteutus.tila)) {
          AuthorizationRules(Seq(Role.Paakayttaja))
        } else {
          AuthorizationRules(
            roleEntity.updateRoles,
            allowAccessToParentOrganizations = true,
            additionalAuthorizedOrganisaatioOids = getTarjoajat(toteutusWithTime)
          )
        }
    }
  }

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

  def search(organisaatioOid: OrganisaatioOid, params: SearchParams)(implicit authenticated: Authenticated): ToteutusSearchResult = {

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

    list(organisaatioOid, vainHakukohteeseenLiitettavat = false, TilaFilter.alsoArkistoidutAddedToOlemassaolevat(true)).map(_.oid) match {
      case Nil          => ToteutusSearchResult()
      case toteutusOids => assocHakukohdeCounts(KoutaSearchClient.searchToteutukset(toteutusOids, params))
    }
  }

  def search(organisaatioOid: OrganisaatioOid, toteutusOid: ToteutusOid, params: SearchParams)(implicit authenticated: Authenticated): Option[ToteutusSearchItemFromIndex] = {
    def filterHakukohteet(toteutus: Option[ToteutusSearchItemFromIndex]): Option[ToteutusSearchItemFromIndex] =
      withAuthorizedOrganizationOids(organisaatioOid, AuthorizationRules(Role.Toteutus.readRoles, allowAccessToParentOrganizations = true)) {
        case Seq(RootOrganisaatioOid) => toteutus
        case organisaatioOids =>
          toteutus.flatMap(toteutusItem => {
            val oidStrings = organisaatioOids.map(_.toString())
            Some(toteutusItem.copy(hakukohteet = toteutusItem.hakukohteet.filter(hakukohde => oidStrings.contains(hakukohde.organisaatio.oid.toString()))))
          })
      }

    filterHakukohteet(KoutaSearchClient.searchToteutukset(Seq(toteutusOid), params).result.headOption)
  }

  private def getDeletableTarjoajienOppilaitokset(toteutus: Toteutus, tarjoajatDeletedFromToteutus: Seq[OrganisaatioOid]): Set[OrganisaatioOid] = {
    val remainingTarjoajatOfToteutus = toteutus.tarjoajat diff tarjoajatDeletedFromToteutus
    val currentToteutustenTarjoajatOfKoulutus = ToteutusDAO.getToteutustenTarjoajatByKoulutusOid(toteutus.koulutusOid)
    val newToteutustenTarjojatOfKoulutus =
      if (remainingTarjoajatOfToteutus.nonEmpty) currentToteutustenTarjoajatOfKoulutus updated (toteutus.oid.get, remainingTarjoajatOfToteutus)
      else currentToteutustenTarjoajatOfKoulutus - toteutus.oid.get
    val newOppilaitoksetOfKoulutus =
      newToteutustenTarjojatOfKoulutus.values.flatten.toSet.flatMap(OrganisaatioServiceImpl.findOppilaitosOidFromOrganisaationHierarkia)
    val oppilaitoksetDeletedFromToteutus = tarjoajatDeletedFromToteutus.flatMap(OrganisaatioServiceImpl.findOppilaitosOidFromOrganisaationHierarkia).toSet
    oppilaitoksetDeletedFromToteutus diff newOppilaitoksetOfKoulutus
  }

  private def getTarjoajienOppilaitokset(toteutus:Toteutus): Set[OrganisaatioOid] =
    toteutus.tarjoajat.flatMap(OrganisaatioServiceImpl.findOppilaitosOidFromOrganisaationHierarkia).toSet

  private def getTarjoajat(maybeToteutusWithTime: Option[(Toteutus, Instant)]): Seq[OrganisaatioOid] =
    maybeToteutusWithTime.map(_._1.tarjoajat).getOrElse(Seq())

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

  def getOidsByTarjoajat(jarjestyspaikkaOids: Seq[OrganisaatioOid], tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Seq[ToteutusOid] =
    withRootAccess(indexerRoles) {
      ToteutusDAO.getOidsByTarjoajat(jarjestyspaikkaOids, tilaFilter)
    }
}
