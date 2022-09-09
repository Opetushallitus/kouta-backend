package fi.oph.kouta.service

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{KayttooikeusClient, KoulutusKoodiClient, KoutaSearchClient, OppijanumerorekisteriClient}
import fi.oph.kouta.domain.Koulutustyyppi.oppilaitostyyppi2koulutustyyppi
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, RootOrganisaatioOid}
import fi.oph.kouta.domain.searchResults.{KoulutusSearchResult, KoulutusSearchResultFromIndex}
import fi.oph.kouta.images.{S3ImageService, TeemakuvaService}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeKoulutus}
import fi.oph.kouta.repository._
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.{Authenticated, EntityNotFoundException, SearchParams}
import fi.oph.kouta.util.{NameHelper, ServiceUtils}
import fi.vm.sade.utils.slf4j.Logging
import slick.dbio.DBIO

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

case class ExternalModifyAuthorizationFailedException(message: String) extends RuntimeException(message)

object KoulutusService
    extends KoulutusService(
      SqsInTransactionService,
      S3ImageService,
      AuditLog,
      OrganisaatioServiceImpl,
      OppijanumerorekisteriClient,
      KayttooikeusClient,
      KoulutusKoodiClient,
      KoulutusServiceValidation,
      KoutaSearchClient
    ) {
  def apply(sqsInTransactionService: SqsInTransactionService,
    s3ImageService: S3ImageService, auditLog: AuditLog,
    organisaatioService: OrganisaatioService,
    oppijanumerorekisteriClient: OppijanumerorekisteriClient,
    kayttooikeusClient: KayttooikeusClient,
    koodistoClient: KoulutusKoodiClient,
    koulutusServiceValidation: KoulutusServiceValidation): KoulutusService = {
    new KoulutusService(
      sqsInTransactionService,
      s3ImageService,
      auditLog,
      organisaatioService,
      oppijanumerorekisteriClient,
      kayttooikeusClient,
      koodistoClient,
      koulutusServiceValidation,
      KoutaSearchClient
    )
  }
}

class KoulutusService(
    sqsInTransactionService: SqsInTransactionService,
    val s3ImageService: S3ImageService,
    auditLog: AuditLog,
    val organisaatioService: OrganisaatioService,
    oppijanumerorekisteriClient: OppijanumerorekisteriClient,
    kayttooikeusClient: KayttooikeusClient,
    koodistoClient: KoulutusKoodiClient,
    koulutusServiceValidation: KoulutusServiceValidation,
    koutaSearchClient: KoutaSearchClient
) extends RoleEntityAuthorizationService[Koulutus]
    with TeemakuvaService[KoulutusOid, Koulutus]
    with Logging {

  protected val roleEntity: RoleEntity = Role.Koulutus
  protected val readRules: AuthorizationRules =
    AuthorizationRules(roleEntity.readRoles, allowAccessToParentOrganizations = true)

  val teemakuvaPrefix = "koulutus-teemakuva"

  private def authorizedForTarjoajaOids(
      oids: Set[OrganisaatioOid],
      roles: Seq[Role] = roleEntity.updateRoles
  ): Option[AuthorizationRules] =
    if (oids.nonEmpty) {
      Some(
        AuthorizationRules(
          requiredRoles = roles,
          allowAccessToParentOrganizations = true,
          overridingAuthorizationRules = Seq(AuthorizationRuleForJulkinen),
          additionalAuthorizedOrganisaatioOids = oids.toSeq
        )
      )
    } else { None }

  private def enrichKoulutusMetadata(koulutus: Koulutus): Option[KoulutusMetadata] = {
    val muokkaajanOrganisaatiot = kayttooikeusClient.getOrganisaatiotFromCache(koulutus.muokkaaja)
    val isOphVirkailija         = ServiceUtils.hasOphOrganisaatioOid(muokkaajanOrganisaatiot)

    koulutus.metadata match {
      case Some(metadata) =>
        metadata match {
          case korkeakoulutusKoulutusMetadata: KorkeakoulutusKoulutusMetadata =>
            korkeakoulutusKoulutusMetadata match {
              case yoMetadata: YliopistoKoulutusMetadata =>
                Some(yoMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
              case amkMetadata: AmmattikorkeakouluKoulutusMetadata =>
                Some(amkMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
              case m: AmmOpeErityisopeJaOpoKoulutusMetadata =>
                val opintojenLaajuusKoodiUri =
                  if (m.opintojenLaajuusKoodiUri.isDefined) m.opintojenLaajuusKoodiUri
                  else
                    Some(koodistoClient.getKoodiUriWithLatestVersionFromCache("opintojenlaajuus_60"))
                val koulutusalaKoodiUrit =
                  if (m.koulutusalaKoodiUrit.nonEmpty) m.koulutusalaKoodiUrit
                  else
                    Seq(
                      koodistoClient.getKoodiUriWithLatestVersionFromCache("kansallinenkoulutusluokitus2016koulutusalataso1_01")
                    )
                Some(
                  m.copy(
                    isMuokkaajaOphVirkailija = Some(isOphVirkailija),
                    opintojenLaajuusKoodiUri = opintojenLaajuusKoodiUri,
                    koulutusalaKoodiUrit = koulutusalaKoodiUrit
                  )
                )

            }
          case ammMetadata: AmmatillinenKoulutusMetadata =>
            Some(ammMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case ammTutkinnonOsaMetadata: AmmatillinenTutkinnonOsaKoulutusMetadata =>
            Some(ammTutkinnonOsaMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case ammOsaamisalaMetadata: AmmatillinenOsaamisalaKoulutusMetadata =>
            Some(ammOsaamisalaMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case ammatillinenMuuKoulutusMetadata: AmmatillinenMuuKoulutusMetadata =>
            Some(ammatillinenMuuKoulutusMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case lukioMetadata: LukioKoulutusMetadata =>
            val koulutusalaKoodiUrit =
              if (lukioMetadata.koulutusalaKoodiUrit.nonEmpty) lukioMetadata.koulutusalaKoodiUrit
              else
                Seq(koodistoClient.getKoodiUriWithLatestVersionFromCache("kansallinenkoulutusluokitus2016koulutusalataso1_00"))
            Some(
              lukioMetadata.copy(
                isMuokkaajaOphVirkailija = Some(isOphVirkailija),
                koulutusalaKoodiUrit = koulutusalaKoodiUrit
              )
            )
          case tuvaMetadata: TuvaKoulutusMetadata =>
            Some(tuvaMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case telmaMetadata: TelmaKoulutusMetadata =>
            Some(telmaMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case vapaaSivistystyoKoulutusMetadata: VapaaSivistystyoKoulutusMetadata =>
            vapaaSivistystyoKoulutusMetadata match {
              case vapaaSivistystyoMuuMetadata: VapaaSivistystyoMuuKoulutusMetadata =>
                Some(vapaaSivistystyoMuuMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
              case vapaaSivistystyoOpistovuosiMetadata: VapaaSivistystyoOpistovuosiKoulutusMetadata =>
                Some(vapaaSivistystyoOpistovuosiMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
            }
          case aikuistenPerusopetusKoulutusMetadata: AikuistenPerusopetusKoulutusMetadata =>
            Some(aikuistenPerusopetusKoulutusMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case kkOpintojaksoMetadata: KkOpintojaksoKoulutusMetadata =>
            Some(kkOpintojaksoMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case erikoislaakariKoulutusMetadata: ErikoislaakariKoulutusMetadata =>
            Some(
              erikoislaakariKoulutusMetadata.copy(
                isMuokkaajaOphVirkailija = Some(isOphVirkailija),
                koulutusalaKoodiUrit = Seq(
                  koodistoClient.getKoodiUriWithLatestVersionFromCache("kansallinenkoulutusluokitus2016koulutusalataso2_091")
                )
              )
            )
          case kkOpintokokonaisuusMetadata: KkOpintokokonaisuusKoulutusMetadata => 
            Some(kkOpintokokonaisuusMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
        }
      case None => None
    }
  }

  def enrichAndPopulateFixedDefaultValues(koulutus: Koulutus): Koulutus = {
    val enrichedMetadata: Option[KoulutusMetadata] = enrichKoulutusMetadata(koulutus)
    koulutus.koulutustyyppi match {
      case AikuistenPerusopetus =>
        val koulutusKoodiUrit =
          if (koulutus.koulutuksetKoodiUri.nonEmpty) koulutus.koulutuksetKoodiUri
          else
            Seq(koodistoClient.getKoodiUriWithLatestVersionFromCache("koulutus_201101"))
        koulutus.copy(koulutuksetKoodiUri = koulutusKoodiUrit, metadata = enrichedMetadata)
      case _ => koulutus.copy(metadata = enrichedMetadata)
    }
  }

  def get(oid: KoulutusOid, tilaFilter: TilaFilter)(implicit
      authenticated: Authenticated
  ): Option[(Koulutus, Instant)] = {
    val koulutusWithTime: Option[(Koulutus, Instant)] = KoulutusDAO.get(oid, tilaFilter)

    val enrichedKoulutus = koulutusWithTime match {
      case Some((k, i)) => {
        val muokkaaja      = oppijanumerorekisteriClient.getHenkilöFromCache(k.muokkaaja)
        val muokkaajanNimi = NameHelper.generateMuokkaajanNimi(muokkaaja)
        Some(k.copy(_enrichedData = Some(KoulutusEnrichedData(muokkaajanNimi = Some(muokkaajanNimi)))), i)
      }
      case None => None
    }

    authorizeGet(
      enrichedKoulutus,
      AuthorizationRules(
        roleEntity.readRoles,
        allowAccessToParentOrganizations = true,
        Seq(AuthorizationRuleForJulkinen),
        getTarjoajat(koulutusWithTime)
      )
    )
  }

  def put(koulutus: Koulutus)(implicit authenticated: Authenticated): KoulutusOid = {
    val rules = if (Koulutustyyppi.isKoulutusSaveAllowedOnlyForOph(koulutus.koulutustyyppi)) {
      AuthorizationRules(Seq(Role.Paakayttaja))
    } else {
      AuthorizationRules(roleEntity.createRoles)
    }

    authorizePut(koulutus, rules) { k =>
      val enrichedKoulutusWithFixedDefaultValues = enrichAndPopulateFixedDefaultValues(k)
      koulutusServiceValidation.withValidation(enrichedKoulutusWithFixedDefaultValues, None) { ek =>
        doPut(ek)
      }
    }.oid.get
  }

  def update(newKoulutus: Koulutus, notModifiedSince: Instant, fromExternal: Boolean = false)(implicit
      authenticated: Authenticated
  ): Boolean = {
    val oldKoulutusWithInstant: Option[(Koulutus, Instant)] =
      KoulutusDAO.get(newKoulutus.oid.get, TilaFilter.onlyOlemassaolevat())
    oldKoulutusWithInstant match {
      case Some((oldKoulutus, _)) =>
        if (fromExternal) {
          authorizeAddedTarjoajatFromExternal(
            newKoulutus.organisaatioOid,
            newKoulutus.tarjoajat.toSet diff oldKoulutus.tarjoajat.toSet
          )
        }
        val rules: List[AuthorizationRules] = getAuthorizationRulesForUpdate(newKoulutus, oldKoulutus)
        rules.nonEmpty && authorizeUpdate(oldKoulutusWithInstant, newKoulutus, rules) { (_, k) =>
          val enrichedKoulutusWithFixedDefaultValues = enrichAndPopulateFixedDefaultValues(k)
          koulutusServiceValidation.withValidation(enrichedKoulutusWithFixedDefaultValues, Some(oldKoulutus)) {
            doUpdate(_, notModifiedSince, oldKoulutus)
          }
        }.nonEmpty
      case _ => throw EntityNotFoundException(s"Päivitettävää asiaa ei löytynyt")
    }
  }

  private def getAuthorizationRulesForUpdate(newKoulutus: Koulutus, oldKoulutus: Koulutus) = {
    if (Julkaisutila.isTilaUpdateAllowedOnlyForOph(oldKoulutus.tila, newKoulutus.tila)) {
      List(AuthorizationRules(Seq(Role.Paakayttaja)))
    } else {
      oldKoulutus.koulutustyyppi match {
        case kt if Koulutustyyppi.isKoulutusSaveAllowedOnlyForOph(kt) =>
          List(AuthorizationRules(Seq(Role.Paakayttaja)))
        case _ =>
          val rulesForUpdatingKoulutus = Some(AuthorizationRules(roleEntity.updateRoles))
          val newTarjoajat             = newKoulutus.tarjoajat.toSet
          val oldTarjoajat             = oldKoulutus.tarjoajat.toSet
          val rulesForAddedTarjoajat   = authorizedForTarjoajaOids(newTarjoajat diff oldTarjoajat)
          val rulesForRemovedTarjoajat = authorizedForTarjoajaOids(oldTarjoajat diff newTarjoajat)
          (rulesForUpdatingKoulutus :: rulesForAddedTarjoajat :: rulesForRemovedTarjoajat :: Nil).flatten
      }
    }
  }

  def authorizeAddedTarjoajatFromExternal(
      organisaatioOid: OrganisaatioOid,
      addedTarjoajat: Set[OrganisaatioOid]
  ): Unit = {
    if (addedTarjoajat.nonEmpty) {
      val allowedOrganisaatioOids = organisaatioService.findOrganisaatioOidsFlatByMemberOid(organisaatioOid).toSet
      if (!addedTarjoajat.subsetOf(allowedOrganisaatioOids)) {
        var msg = "Valittuja tarjoajia ei voi lisätä koulutukselle ulkoisen rajapinnan kautta. "
        msg += s"Tarjoajiksi voi lisätä ainoastaan koulutuksen omistavaan organisaatioon ($organisaatioOid) kuuluvia organisaatioOID:eja."
        throw ExternalModifyAuthorizationFailedException(msg)
      }
    }
  }

  def list(organisaatioOid: OrganisaatioOid, tilaFilter: TilaFilter)(implicit
      authenticated: Authenticated
  ): Seq[KoulutusListItem] =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(organisaatioOid, readRules) { case (oids, koulutustyypit) =>
      KoulutusDAO.listAllowedByOrganisaatiot(oids, koulutustyypit, tilaFilter)
    }

  def listByKoulutustyyppi(organisaatioOid: OrganisaatioOid, koulutustyyppi: Koulutustyyppi, tilaFilter: TilaFilter)(
      implicit authenticated: Authenticated
  ): Seq[KoulutusListItem] =
    withAuthorizedOrganizationOids(organisaatioOid, readRules) { oids =>
      KoulutusDAO.listAllowedByOrganisaatiotAndKoulutustyyppi(oids, koulutustyyppi, tilaFilter)
    }

  def getTarjoajanJulkaistutKoulutukset(
      organisaatioOid: OrganisaatioOid
  )(implicit authenticated: Authenticated): Seq[Koulutus] =
    withRootAccess(indexerRoles) {
      KoulutusDAO.getJulkaistutByTarjoajaOids(
        organisaatioService.getAllChildOidsFlat(organisaatioOid, lakkautetut = true)
      )
    }

  def toteutukset(oid: KoulutusOid, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Seq[Toteutus] =
    withRootAccess(indexerRoles) {
      ToteutusDAO.getByKoulutusOid(oid, tilaFilter)
    }

  def hakutiedot(oid: KoulutusOid)(implicit authenticated: Authenticated): Seq[Hakutieto] =
    withRootAccess(indexerRoles) {
      HakutietoDAO.getByKoulutusOid(oid)
    }

  def listToteutukset(oid: KoulutusOid, tilaFilter: TilaFilter)(implicit
      authenticated: Authenticated
  ): Seq[ToteutusListItem] =
    withRootAccess(indexerRoles)(ToteutusDAO.listByKoulutusOid(oid, tilaFilter))

  def listToteutukset(oid: KoulutusOid, organisaatioOid: OrganisaatioOid)(implicit
      authenticated: Authenticated
  ): Seq[ToteutusListItem] =
    withAuthorizedOrganizationOids(
      organisaatioOid,
      AuthorizationRules(Role.Toteutus.readRoles, allowAccessToParentOrganizations = true)
    ) {
      case Seq(RootOrganisaatioOid) => ToteutusDAO.listByKoulutusOid(oid, TilaFilter.onlyOlemassaolevat())
      case x                        => ToteutusDAO.listByKoulutusOidAndAllowedOrganisaatiot(oid, x)
    }

  def search(organisaatioOid: OrganisaatioOid, params: SearchParams)(implicit
      authenticated: Authenticated
  ): KoulutusSearchResult = {
    def getCount(k: KoulutusSearchItemFromIndex, organisaatioOids: Seq[OrganisaatioOid]): Integer = {
      organisaatioOids match {
        case Seq(RootOrganisaatioOid) => k.toteutukset.length
        case _ =>
          val oidStrings = organisaatioOids.map(_.toString())
          k.toteutukset.count(t =>
            t.tila != Arkistoitu && t.tila != Poistettu && t.organisaatiot.exists(o => oidStrings.contains(o))
          )
      }
    }

    def assocToteutusCounts(r: KoulutusSearchResultFromIndex): KoulutusSearchResult =
      withAuthorizedOrganizationOids(
        organisaatioOid,
        AuthorizationRules(Role.Toteutus.readRoles, allowAccessToParentOrganizations = true)
      )(organisaatioOids => {
        SearchResult[KoulutusSearchItem](
          totalCount = r.totalCount,
          result = r.result.map { k =>
            KoulutusSearchItem(
              oid = k.oid,
              nimi = k.nimi,
              organisaatio = k.organisaatio,
              muokkaaja = k.muokkaaja,
              modified = k.modified,
              tila = k.tila,
              eperuste = k.eperuste,
              julkinen = k.julkinen,
              koulutustyyppi = k.koulutustyyppi,
              toteutusCount = getCount(k, organisaatioOids)
            )
          }
        )
      })

    list(organisaatioOid, TilaFilter.alsoArkistoidutAddedToOlemassaolevat(true)).map(_.oid) match {
      case Nil          => SearchResult[KoulutusSearchItem]()
      case koulutusOids => assocToteutusCounts(koutaSearchClient.searchKoulutukset(koulutusOids, params))
    }
  }

  def search(organisaatioOid: OrganisaatioOid, koulutusOid: KoulutusOid, params: SearchParams)(implicit
      authenticated: Authenticated
  ): Option[KoulutusSearchItemFromIndex] = {
    def filterToteutukset(koulutus: Option[KoulutusSearchItemFromIndex]): Option[KoulutusSearchItemFromIndex] =
      withAuthorizedOrganizationOids(
        organisaatioOid,
        AuthorizationRules(Role.Toteutus.readRoles, allowAccessToParentOrganizations = true)
      ) {
        case Seq(RootOrganisaatioOid) => koulutus
        case organisaatioOids => {
          koulutus.flatMap(koulutusItem => {
            val oidStrings = organisaatioOids.map(_.toString())
            Some(
              koulutusItem.copy(toteutukset =
                koulutusItem.toteutukset.filter(toteutus => toteutus.organisaatiot.exists(o => oidStrings.contains(o)))
              )
            )
          })
        }
      }

    filterToteutukset(
      koutaSearchClient.searchKoulutukset(Seq(koulutusOid), params).result.headOption
    )
  }

  def getUpdateTarjoajatActions(
      koulutusOid: KoulutusOid,
      newTarjoajatInToteutus: Set[OrganisaatioOid],
      tarjoajatSafeToDelete: Set[OrganisaatioOid]
  )(implicit authenticated: Authenticated): DBIO[(Koulutus, Option[Koulutus])] = {
    val koulutusWithLastModified = get(koulutusOid, TilaFilter.onlyOlemassaolevat())

    if (koulutusWithLastModified.isEmpty) {
      throw EntityNotFoundException(s"Päivitettävää asiaa ei löytynyt")
    }

    val Some((koulutus, lastModified)) = koulutusWithLastModified

    val tarjoajatAddedToKoulutus     = newTarjoajatInToteutus diff koulutus.tarjoajat.toSet
    val newTarjoajatForKoulutus      = (koulutus.tarjoajat.toSet diff tarjoajatSafeToDelete) ++ tarjoajatAddedToKoulutus
    val tarjoajatRemovedFromKoulutus = koulutus.tarjoajat.toSet diff newTarjoajatForKoulutus

    if (tarjoajatAddedToKoulutus.isEmpty && tarjoajatRemovedFromKoulutus.isEmpty) {
      DBIO.successful((koulutus, None))
    } else {
      val newKoulutus: Koulutus = koulutus.copy(tarjoajat = newTarjoajatForKoulutus.toList)
      authorizeUpdate(
        koulutusWithLastModified,
        newKoulutus,
        List(
          authorizedForTarjoajaOids(tarjoajatAddedToKoulutus ++ tarjoajatRemovedFromKoulutus, roleEntity.readRoles).get
        )
      ) { (_, k) =>
        koulutusServiceValidation.withValidation(newKoulutus, Some(k)) {
          DBIO.successful(koulutus) zip getUpdateTarjoajatActions(_, lastModified)
        }
      }
    }
  }

  private def getTarjoajat(maybeKoulutusWithTime: Option[(Koulutus, Instant)]): Seq[OrganisaatioOid] =
    maybeKoulutusWithTime.map(_._1.tarjoajat).getOrElse(Seq())

  private def getUpdateTarjoajatActions(koulutus: Koulutus, notModifiedSince: Instant)(implicit
      authenticated: Authenticated
  ): DBIO[Option[Koulutus]] = {
    for {
      _ <- KoulutusDAO.checkNotModified(koulutus.oid.get, notModifiedSince)
      k <- KoulutusDAO.getUpdateTarjoajatActions(koulutus)
    } yield Some(k)
  }

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

  private def doUpdate(koulutus: Koulutus, notModifiedSince: Instant, before: Koulutus)(implicit
      authenticated: Authenticated
  ): Option[Koulutus] =
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

  def index(koulutus: Option[Koulutus]): DBIO[_] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeKoulutus, koulutus.map(_.oid.get.toString))

  def getOppilaitosTyypitByKoulutustyypit()(implicit
      authenticated: Authenticated
  ): KoulutustyyppiToOppilaitostyyppiResult = {
    val koulutustyyppi2oppilaitostyyppi: Seq[KoulutustyyppiToOppilaitostyypit] =
      oppilaitostyyppi2koulutustyyppi
        .foldLeft(Map[Koulutustyyppi, Seq[String]]().withDefaultValue(Seq())) {
          case (initialMap, (oppilaitostyyppi, koulutustyypit)) =>
            koulutustyypit.foldLeft(initialMap)((subMap, koulutustyyppi) =>
              subMap.updated(koulutustyyppi, initialMap(koulutustyyppi) :+ oppilaitostyyppi)
            )
        }
        .map(entry => KoulutustyyppiToOppilaitostyypit(entry._1, entry._2))
        .toSeq

    KoulutustyyppiToOppilaitostyyppiResult(koulutustyyppi2oppilaitostyyppi)
  }
}
