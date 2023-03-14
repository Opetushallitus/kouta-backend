package fi.oph.kouta.service

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.keyword.{Ammattinimike, Asiasana}
import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid, ToteutusOid, UserOid}
import fi.oph.kouta.domain.searchResults.ToteutusSearchResultFromIndex
import fi.oph.kouta.images.{S3ImageService, TeemakuvaService}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeToteutus}
import fi.oph.kouta.repository._
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.{Authenticated, EntityNotFoundException, SearchParams}
import fi.oph.kouta.util.MiscUtils.{isDIAlukiokoulutus, isEBlukiokoulutus}
import fi.oph.kouta.util.{NameHelper, ServiceUtils}
import slick.dbio.DBIO

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

object ToteutusService
    extends ToteutusService(
      SqsInTransactionService,
      S3ImageService,
      AuditLog,
      KeywordService,
      OrganisaatioServiceImpl,
      KoulutusService,
      LokalisointiClient,
      CachedKoodistoClient,
      OppijanumerorekisteriClient,
      KayttooikeusClient,
      ToteutusServiceValidation,
      KoutaIndeksoijaClient
    )

case class ToteutusCopyOids(
    toteutusOid: Option[ToteutusOid]
)

case class ToteutusCopyResultObject(
    oid: ToteutusOid,
    status: String,
    created: ToteutusCopyOids
)

case class ToteutusTilaChangeResultObject(
  oid: ToteutusOid,
  status: String,
  errorPaths: List[String] = List(),
  errorMessages: List[String] = List(),
  errorTypes: List[String] = List()
)

class ToteutusService(
    sqsInTransactionService: SqsInTransactionService,
    val s3ImageService: S3ImageService,
    auditLog: AuditLog,
    keywordService: KeywordService,
    val organisaatioService: OrganisaatioService,
    koulutusService: KoulutusService,
    lokalisointiClient: LokalisointiClient,
    koodistoClient: CachedKoodistoClient,
    oppijanumerorekisteriClient: OppijanumerorekisteriClient,
    kayttooikeusClient: KayttooikeusClient,
    toteutusServiceValidation: ToteutusServiceValidation,
    koutaIndeksoijaClient: KoutaIndeksoijaClient
) extends RoleEntityAuthorizationService[Toteutus]
    with TeemakuvaService[ToteutusOid, Toteutus] {

  protected val roleEntity: RoleEntity = Role.Toteutus

  val teemakuvaPrefix: String = "toteutus-teemakuva"

  def generateToteutusEsitysnimi(toteutus: Toteutus): Kielistetty = {
    val koulutuksetKoodiUri = toteutus.koulutuksetKoodiUri
    if (
      !koulutuksetKoodiUri.isEmpty && (isDIAlukiokoulutus(koulutuksetKoodiUri) || isEBlukiokoulutus(
        koulutuksetKoodiUri
      ))
    ) {
      toteutus.nimi
    } else {
      (toteutus.metadata, toteutus.koulutusMetadata) match {
        case (Some(toteutusMetadata), Some(koulutusMetadata)) =>
          (toteutusMetadata, koulutusMetadata) match {
            case (lukioToteutusMetadata: LukioToteutusMetadata, lukioKoulutusMetadata: LukioKoulutusMetadata) => {
              val kaannokset = Map(
                "yleiset.opintopistetta" -> lokalisointiClient.getKaannoksetWithKeyFromCache("yleiset.opintopistetta"),
                "toteutuslomake.lukionYleislinjaNimiOsa" -> lokalisointiClient.getKaannoksetWithKeyFromCache(
                  "toteutuslomake.lukionYleislinjaNimiOsa"
                )
              )
              val painotuksetKaannokset = koodistoClient.getKoodistoKaannoksetFromCache("lukiopainotukset")
              val koulutustehtavatKaannokset =
                koodistoClient.getKoodistoKaannoksetFromCache("lukiolinjaterityinenkoulutustehtava")
              val koodistoKaannokset = (painotuksetKaannokset.toSeq ++ koulutustehtavatKaannokset.toSeq).toMap
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

  private def withMData(toteutus: Toteutus, toteutusMetadata: ToteutusMetadata): Toteutus =
    toteutus.copy(metadata = Some(toteutusMetadata))

  private def withMDataAndKoulutusNimi(
      toteutus: Toteutus,
      toteutusMetadata: ToteutusMetadata,
      koulutus: Option[(Koulutus, Instant)]
  ): Toteutus =
    toteutus.copy(
      nimi = koulutus match {
        case Some((koulutus, _)) => NameHelper.mergeNames(koulutus.nimi, toteutus.nimi, toteutus.kielivalinta)
        case _                   => toteutus.nimi
      },
      metadata = Some(toteutusMetadata)
    )

  private def enrichToteutus(t: Toteutus, koulutus: Option[(Koulutus, Instant)]): Toteutus = {
    val muokkaajanOrganisaatiot = kayttooikeusClient.getOrganisaatiotFromCache(t.muokkaaja)
    val isOphVirkailija         = ServiceUtils.hasOphOrganisaatioOid(muokkaajanOrganisaatiot)

    t.metadata match {
      case Some(metadata: ToteutusMetadata) =>
        metadata match {
          case yoMetadata: YliopistoToteutusMetadata =>
            withMData(t, yoMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case amkMetadata: AmmattikorkeakouluToteutusMetadata =>
            withMData(t, amkMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case ammOpeErityisopeJaOpoToteutusMetadata: AmmOpeErityisopeJaOpoToteutusMetadata =>
            withMData(t, ammOpeErityisopeJaOpoToteutusMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case opePedagOpinnotToteutusMetadata: OpePedagOpinnotToteutusMetadata =>
            withMData(t, opePedagOpinnotToteutusMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case ammMetadata: AmmatillinenToteutusMetadata =>
            withMData(t, ammMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case tutkintoonJohtamatonToteutusMetadata: TutkintoonJohtamatonToteutusMetadata =>
            tutkintoonJohtamatonToteutusMetadata match {
              case ammTutkinnonOsaMetadata: AmmatillinenTutkinnonOsaToteutusMetadata =>
                withMDataAndKoulutusNimi(
                  t,
                  ammTutkinnonOsaMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)),
                  koulutus
                )
              case ammOsaamisalaMetadata: AmmatillinenOsaamisalaToteutusMetadata =>
                withMDataAndKoulutusNimi(
                  t,
                  ammOsaamisalaMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)),
                  koulutus
                )
              case ammatillinenMuuToteutusMetadata: AmmatillinenMuuToteutusMetadata =>
                withMData(t, ammatillinenMuuToteutusMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
              case kkOpintojaksoMetadata: KkOpintojaksoToteutusMetadata =>
                withMData(
                  t,
                  kkOpintojaksoMetadata.copy(
                    isMuokkaajaOphVirkailija = Some(isOphVirkailija),
                    opintojenLaajuusyksikkoKoodiUri = koulutus.get._1.metadata match {
                      case Some(kkOpintojaksoKoulutusMetadata: KkOpintojaksoKoulutusMetadata) =>
                        kkOpintojaksoKoulutusMetadata.opintojenLaajuusyksikkoKoodiUri
                    }
                  ))
              case vapaaSivistystyoMuuToteutusMetadata: VapaaSivistystyoMuuToteutusMetadata =>
                withMData(t, vapaaSivistystyoMuuToteutusMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
              case aikuistenPerusopetusToteutusMetadata: AikuistenPerusopetusToteutusMetadata =>
                withMData(
                  t,
                  aikuistenPerusopetusToteutusMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija))
                )
              case kkOpintokokonaisuusMetadata: KkOpintokokonaisuusToteutusMetadata =>
                withMData(
                  t,
                  kkOpintokokonaisuusMetadata.copy(
                    isMuokkaajaOphVirkailija = Some(isOphVirkailija),
                    opintojenLaajuusyksikkoKoodiUri = koulutus.get._1.metadata match {
                      case Some(kkOpintokokonaisuusKoulutusMetadata: KkOpintokokonaisuusKoulutusMetadata) =>
                        kkOpintokokonaisuusKoulutusMetadata.opintojenLaajuusyksikkoKoodiUri
                    }
                  ))
              case erikoistumisKoulutusMetadata: ErikoistumiskoulutusToteutusMetadata =>
                withMData(t, erikoistumisKoulutusMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
              case taiteenPerusopetusToteutusMetadata: TaiteenPerusopetusToteutusMetadata =>
                withMData(
                  t,
                  taiteenPerusopetusToteutusMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija))
                )
              case muuToteutusMetadata: MuuToteutusMetadata => withMData(t, muuToteutusMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
            }
          case lukioMetadata: LukioToteutusMetadata =>
            withMData(t, lukioMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case tuvaMetadata: TuvaToteutusMetadata =>
            withMDataAndKoulutusNimi(t, tuvaMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)), koulutus)
          case telmaMetadata: TelmaToteutusMetadata =>
            withMDataAndKoulutusNimi(t, telmaMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)), koulutus)
          case vapaaSivistystyoOpistovuosiMetadata: VapaaSivistystyoOpistovuosiToteutusMetadata =>
            withMDataAndKoulutusNimi(
              t,
              vapaaSivistystyoOpistovuosiMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)),
              koulutus
            )
          case erikoislaakariToteutusMetadata: ErikoislaakariToteutusMetadata =>
            withMData(t, erikoislaakariToteutusMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
        }
      case None => t.copy(metadata = None)
    }
  }

  def get(oid: ToteutusOid, tilaFilter: TilaFilter)(implicit
      authenticated: Authenticated
  ): Option[(Toteutus, Instant)] = {
    val toteutusWithTime = ToteutusDAO.get(oid, tilaFilter)
    val enrichedToteutus = toteutusWithTime match {
      case Some((t, i)) =>
        val esitysnimi     = generateToteutusEsitysnimi(t)
        val muokkaaja      = oppijanumerorekisteriClient.getHenkilöFromCache(t.muokkaaja)
        val muokkaajanNimi = NameHelper.generateMuokkaajanNimi(muokkaaja)
        Some(t.withEnrichedData(ToteutusEnrichedData(esitysnimi, Some(muokkaajanNimi))).withoutRelatedData(), i)
      case None => None
    }
    authorizeGet(
      enrichedToteutus,
      AuthorizationRules(
        roleEntity.readRoles,
        additionalAuthorizedOrganisaatioOids = getTarjoajat(toteutusWithTime)
      )
    )
  }

  def put(toteutus: Toteutus)(implicit authenticated: Authenticated): CreateResult = {
    authorizePut(
      toteutus,
      AuthorizationRules(
        roleEntity.createRoles,
        false,
        Some(AuthorizedToAllOfGivenOrganizationsRule),
        toteutus.tarjoajat
      )
    ) { t =>
      val koulutusWithLastModified = koulutusService.get(t.koulutusOid, TilaFilter.onlyOlemassaolevat())
      val enrichedToteutus         = enrichToteutus(t, koulutusWithLastModified)
      toteutusServiceValidation.withValidation(enrichedToteutus, None, authenticated) { t =>
        doPut(
          t,
          koulutusService.getUpdateTarjoajatActions(
            koulutusWithLastModified.getOrElse(throw EntityNotFoundException(s"Päivitettävää asiaa ei löytynyt")),
            getTarjoajienOppilaitokset(toteutus),
            Set()
          )
        )
      }
    }
  }

  def copy(toteutusOids: List[ToteutusOid])(implicit authenticated: Authenticated): Seq[ToteutusCopyResultObject] = {
    val toteutukset = ToteutusDAO.getToteutuksetByOids(toteutusOids)
    toteutukset.map(toteutus => {
      try {
        val toteutusCopyAsLuonnos = toteutus.copy(oid = None, tila = Tallennettu)
        val createdToteutusOid    = put(toteutusCopyAsLuonnos).oid.asInstanceOf[ToteutusOid]
        ToteutusCopyResultObject(
          oid = toteutus.oid.get,
          status = "success",
          created = ToteutusCopyOids(Some(createdToteutusOid))
        )
      } catch {
        case error: Throwable =>
          logger.error(s"Copying toteutus failed: $error")
          ToteutusCopyResultObject(oid = toteutus.oid.get, status = "error", created = ToteutusCopyOids(None))
      }
    })
  }

  def update(toteutus: Toteutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): UpdateResult = {
    val toteutusWithTime = ToteutusDAO.get(toteutus.oid.get, TilaFilter.onlyOlemassaolevat())
    val rules            = getAuthorizationRulesForUpdate(toteutusWithTime, toteutus)

    authorizeUpdate(toteutusWithTime, toteutus, rules) { (oldToteutus, t) =>
      val koulutusWithLastModified = koulutusService.get(t.koulutusOid, TilaFilter.onlyOlemassaolevat())
      val enrichedToteutus         = enrichToteutus(t, koulutusWithLastModified)
      toteutusServiceValidation.withValidation(enrichedToteutus, Some(oldToteutus), authenticated) { t =>
        val deletedTarjoajat =
          if (t.tila == Poistettu) t.tarjoajat else oldToteutus.tarjoajat diff t.tarjoajat
        doUpdate(
          t,
          notModifiedSince,
          oldToteutus,
          koulutusService.getUpdateTarjoajatActions(
            koulutusWithLastModified.getOrElse(throw EntityNotFoundException(s"Päivitettävää asiaa ei löytynyt")),
            getTarjoajienOppilaitokset(t),
            getDeletableTarjoajienOppilaitokset(t, deletedTarjoajat)
          )
        )
      }
    }
  }

  private def getAuthorizationRulesForUpdate(
      toteutusWithTime: Option[(Toteutus, Instant)],
      newToteutus: Toteutus
  ): AuthorizationRules = {
    toteutusWithTime match {
      case None => throw EntityNotFoundException(s"Päivitettävää toteutusta ei löytynyt")
      case Some((oldToteutus, _)) =>
        if (Julkaisutila.isTilaUpdateAllowedOnlyForOph(oldToteutus.tila, newToteutus.tila)) {
          AuthorizationRules(Seq(Role.Paakayttaja))
        } else {
          AuthorizationRules(
            roleEntity.updateRoles,
            overridingAuthorizationRule = Some(AuthorizedToAllOfGivenOrganizationsRule),
            additionalAuthorizedOrganisaatioOids = newToteutus.tarjoajat
          )
        }
    }
  }

  def list(organisaatioOid: OrganisaatioOid, vainHakukohteeseenLiitettavat: Boolean = false, tilaFilter: TilaFilter)(
      implicit authenticated: Authenticated
  ): Seq[ToteutusListItem] =
    withAuthorizedOrganizationOids(
      organisaatioOid,
      AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true)
    )(ToteutusDAO.listByAllowedOrganisaatiot(_, vainHakukohteeseenLiitettavat, tilaFilter))

  def listHaut(oid: ToteutusOid, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Seq[HakuListItem] =
    withRootAccess(indexerRoles)(HakuDAO.listByToteutusOid(oid, tilaFilter))

  def listHakukohteet(oid: ToteutusOid, tilaFilter: TilaFilter)(implicit
      authenticated: Authenticated
  ): Seq[HakukohdeListItem] = {
    withRootAccess(indexerRoles)(HakukohdeDAO.listByToteutusOid(oid, tilaFilter))
  }

  def listHakukohteet(oid: ToteutusOid, organisaatioOid: OrganisaatioOid)(implicit
      authenticated: Authenticated
  ): Seq[HakukohdeListItem] = {
    withAuthorizedChildOrganizationOids(organisaatioOid, Role.Hakukohde.readRoles) {
      case Seq(RootOrganisaatioOid) => HakukohdeDAO.listByToteutusOid(oid, TilaFilter.onlyOlemassaolevat())
      case organisaatioOids         => HakukohdeDAO.listByToteutusOidAndAllowedOrganisaatiot(oid, organisaatioOids)
    }
  }

  def listOpintojaksot(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
    withAuthorizedOrganizationOids(organisaatioOid,
      AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true))(
      ToteutusDAO.listOpintojaksotByAllowedOrganisaatiot(_, TilaFilter.onlyOlemassaolevatAndArkistoimattomat()))

  def search(organisaatioOid: OrganisaatioOid, params: SearchParams)(implicit
      authenticated: Authenticated
  ): ToteutusSearchResult = {

    def getCount(t: ToteutusSearchItemFromIndex, organisaatioOids: Seq[OrganisaatioOid]): Integer = {
      val hakukohteet = t.hakutiedot.flatMap(_.hakukohteet)
      organisaatioOids match {
        case Seq(RootOrganisaatioOid) => hakukohteet.length
        case _ =>
          val oidStrings = organisaatioOids.map(_.toString())
          hakukohteet.count(x =>
            x.tila != Arkistoitu && x.tila != Poistettu && oidStrings.contains(x.organisaatio.oid.toString())
          )
      }
    }

    def assocHakukohdeCounts(r: ToteutusSearchResultFromIndex): ToteutusSearchResult =
      withAuthorizedOrganizationOids(
        organisaatioOid,
        AuthorizationRules(Role.Toteutus.readRoles, allowAccessToParentOrganizations = true)
      )(organisaatioOids => {
        ToteutusSearchResult(
          totalCount = r.totalCount,
          result = r.result.map { t =>
            ToteutusSearchItem(
              oid = t.oid,
              nimi = t.nimi,
              organisaatio = t.organisaatio,
              muokkaaja = t.muokkaaja,
              modified = t.modified,
              tila = t.tila,
              organisaatiot = t.organisaatiot,
              koulutustyyppi = t.koulutustyyppi,
              hakukohdeCount = getCount(t, organisaatioOids)
            )
          }
        )
      })

    list(organisaatioOid, vainHakukohteeseenLiitettavat = false, TilaFilter.alsoArkistoidutAddedToOlemassaolevat(true))
      .map(_.oid) match {
      case Nil          => ToteutusSearchResult()
      case toteutusOids => assocHakukohdeCounts(KoutaSearchClient.searchToteutukset(toteutusOids, params))
    }
  }

  private def getDeletableTarjoajienOppilaitokset(
      toteutus: Toteutus,
      tarjoajatDeletedFromToteutus: Seq[OrganisaatioOid]
  ): Set[OrganisaatioOid] = {
    val remainingTarjoajatOfToteutus          = toteutus.tarjoajat diff tarjoajatDeletedFromToteutus
    val currentToteutustenTarjoajatOfKoulutus = ToteutusDAO.getToteutustenTarjoajatByKoulutusOid(toteutus.koulutusOid)
    val newToteutustenTarjojatOfKoulutus =
      if (remainingTarjoajatOfToteutus.nonEmpty)
        currentToteutustenTarjoajatOfKoulutus updated (toteutus.oid.get, remainingTarjoajatOfToteutus)
      else currentToteutustenTarjoajatOfKoulutus - toteutus.oid.get
    val newOppilaitoksetOfKoulutus =
      newToteutustenTarjojatOfKoulutus.values.flatten.toSet
        .flatMap(OrganisaatioServiceImpl.findOppilaitosOidFromOrganisaationHierarkia)
    val oppilaitoksetDeletedFromToteutus =
      tarjoajatDeletedFromToteutus.flatMap(OrganisaatioServiceImpl.findOppilaitosOidFromOrganisaationHierarkia).toSet
    oppilaitoksetDeletedFromToteutus diff newOppilaitoksetOfKoulutus
  }

  private def getTarjoajienOppilaitokset(toteutus: Toteutus): Set[OrganisaatioOid] =
    toteutus.tarjoajat.flatMap(OrganisaatioServiceImpl.findOppilaitosOidFromOrganisaationHierarkia).toSet

  private def getTarjoajat(maybeToteutusWithTime: Option[(Toteutus, Instant)]): Seq[OrganisaatioOid] =
    maybeToteutusWithTime.map(_._1.tarjoajat).getOrElse(Seq())

  private def doPut(toteutus: Toteutus, koulutusAddTarjoajaActions: DBIO[(Koulutus, Option[Koulutus])])(implicit
      authenticated: Authenticated
  ): CreateResult =
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
      val warnings = quickIndex(t.oid)
      CreateResult(t.oid.get, warnings)
    }.get

  private def doUpdate(
      toteutus: Toteutus,
      notModifiedSince: Instant,
      before: Toteutus,
      koulutusAddTarjoajaActions: DBIO[(Koulutus, Option[Koulutus])]
  )(implicit authenticated: Authenticated): UpdateResult =
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
      val warnings = quickIndex(t.flatMap(_.oid))
      UpdateResult(t.isDefined, warnings)
    }.get

  private def index(toteutus: Option[Toteutus]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeToteutus, toteutus.map(_.oid.get.toString))

  private def quickIndex(toteutusOid: Option[ToteutusOid]): List[String] = {
    toteutusOid match {
      case Some(oid) => koutaIndeksoijaClient.quickIndexEntity("toteutus", oid.toString)
      case None => List.empty
    }
  }

  private def insertAsiasanat(toteutus: Toteutus)(implicit authenticated: Authenticated) =
    keywordService.insert(Asiasana, toteutus.metadata.map(_.asiasanat).getOrElse(Seq()))

  private def insertAmmattinimikkeet(toteutus: Toteutus)(implicit authenticated: Authenticated) =
    keywordService.insert(Ammattinimike, toteutus.metadata.map(_.ammattinimikkeet).getOrElse(Seq()))

  def getOidsByTarjoajat(jarjestyspaikkaOids: Seq[OrganisaatioOid], tilaFilter: TilaFilter)(implicit
      authenticated: Authenticated
  ): Seq[ToteutusOid] =
    withRootAccess(indexerRoles) {
      ToteutusDAO.getOidsByTarjoajat(jarjestyspaikkaOids, tilaFilter)
    }

  def getToteutukset(oids: List[ToteutusOid])(implicit authenticated: Authenticated): Seq[Toteutus] =
    withRootAccess(indexerRoles) {
      ToteutusDAO.get(oids)
    }

  def listOpintokokonaisuudet(oids: List[ToteutusOid])(implicit authenticated: Authenticated): Seq[OidAndNimi] =
    withRootAccess(indexerRoles) {
      ToteutusDAO.getOpintokokonaisuudet(oids)
    }

  def changeTila(toteutusOids: Seq[ToteutusOid], tila: String, unModifiedSince: Instant)(implicit
                                                                                           authenticated: Authenticated
  ): List[ToteutusTilaChangeResultObject] = {
    val toteutukset: Seq[Toteutus] = toteutusOids.map(oid => {
      ToteutusDAO.get(oid, TilaFilter.all())
    }).collect {
      case Some(result) => result._1
    }

    val updatedToteutusOids = scala.collection.mutable.Set[ToteutusOid]()

    val tilaChangeResults = toteutukset.toList.map(toteutus => {
      try {
        val toteutusWithNewTila = toteutus.copy(tila = Julkaisutila.withName(tila), muokkaaja = UserOid(authenticated.id))
        val updateResult = update(toteutusWithNewTila, unModifiedSince)
        if (updateResult.updated) {
          updatedToteutusOids += toteutus.oid.get
          ToteutusTilaChangeResultObject(
            oid = toteutus.oid.get,
            status = "success"
          )
        } else {
          updatedToteutusOids += toteutus.oid.get
          ToteutusTilaChangeResultObject(
            oid = toteutus.oid.get,
            status = "error",
            errorPaths = List("toteutus"),
            errorMessages = List("Toteutuksen tilaa ei voitu päivittää"),
            errorTypes = List("possible transaction error")
          )
        }
      } catch {
        case error: KoutaValidationException =>
          logger.error(s"Changing of tila of toteutus: ${toteutus.oid.get} failed: $error")
          updatedToteutusOids += toteutus.oid.get
          ToteutusTilaChangeResultObject(
            oid = toteutus.oid.get,
            status = "error",
            errorPaths = error.getPaths,
            errorMessages =  error.getMsgs,
            errorTypes = error.getErrorTypes
          )
        case error: OrganizationAuthorizationFailedException =>
          logger.error(s"Changing of tila of toteutus: ${toteutus.oid.get} failed: $error")
          updatedToteutusOids += toteutus.oid.get
          ToteutusTilaChangeResultObject(
            oid = toteutus.oid.get,
            status = "error",
            errorPaths = List("toteutus"),
            errorMessages = List(error.getMessage),
            errorTypes = List("organizationauthorization")
          )
        case error: RoleAuthorizationFailedException =>
          logger.error(s"Changing of tila of toteutus: ${toteutus.oid.get} failed: $error")
          updatedToteutusOids += toteutus.oid.get
          ToteutusTilaChangeResultObject(
            oid = toteutus.oid.get,
            status = "error",
            errorPaths = List("toteutus"),
            errorMessages = List(error.getMessage),
            errorTypes = List("roleAuthorization")
          )
        case error: Exception =>
          logger.error(s"Changing of tila of toteutus: ${toteutus.oid.get} failed: $error")
          updatedToteutusOids += toteutus.oid.get
          ToteutusTilaChangeResultObject(
            oid = toteutus.oid.get,
            status = "error",
            errorPaths = List("toteutus"),
            errorMessages = List(error.getMessage),
            errorTypes = List("internalServerError")
          )
      }
    })

    val notFound =
      for {
        totOid <- toteutusOids.filterNot(toteutusOid => updatedToteutusOids.contains(toteutusOid))
      } yield ToteutusTilaChangeResultObject(
        oid = totOid,
        status = "error",
        errorPaths = List("toteutus"),
        errorMessages = List("Toteutusta ei löytynyt"),
        errorTypes = List("not found")
      )

    tilaChangeResults ++ notFound
  }
}
