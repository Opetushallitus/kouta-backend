package fi.oph.kouta.service

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client._
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
import fi.oph.kouta.util.NameHelper.{mergeNames, notFullyPopulated}
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
      CachedKoodistoClient,
      KoulutusServiceValidation,
      KoutaSearchClient,
      EPerusteKoodiClient,
      KoutaIndeksoijaClient
    ) {
  def apply(
      sqsInTransactionService: SqsInTransactionService,
      s3ImageService: S3ImageService,
      auditLog: AuditLog,
      organisaatioService: OrganisaatioService,
      oppijanumerorekisteriClient: OppijanumerorekisteriClient,
      kayttooikeusClient: KayttooikeusClient,
      koodistoClient: CachedKoodistoClient,
      koulutusServiceValidation: KoulutusServiceValidation
  ): KoulutusService = {
    new KoulutusService(
      sqsInTransactionService,
      s3ImageService,
      auditLog,
      organisaatioService,
      oppijanumerorekisteriClient,
      kayttooikeusClient,
      koodistoClient,
      koulutusServiceValidation,
      KoutaSearchClient,
      EPerusteKoodiClient,
      KoutaIndeksoijaClient
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
    koodistoClient: CachedKoodistoClient,
    koulutusServiceValidation: KoulutusServiceValidation,
    koutaSearchClient: KoutaSearchClient,
    ePerusteKoodiClient: EPerusteKoodiClient,
    koutaIndeksoijaClient: KoutaIndeksoijaClient
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
          overridingAuthorizationRule = Some(AuthorizationRuleForUpdateTarjoajat),
          additionalAuthorizedOrganisaatioOids = oids.toSeq
        )
      )
    } else { None }

  private def getKoodiUriVersion(koodiUriAsString: String): KoodiUri =
    koodistoClient.getKoodiUriVersionOrLatestFromCache(koodiUriAsString) match {
      case Left(exp)  => throw exp
      case Right(uri) => uri
    }

  private def getKoodiUriVersionIfEmpty(currentValue: Option[String], koodiUriAsString: String): Option[String] =
    if (currentValue.isDefined) currentValue
    else {
      val koodiUri = getKoodiUriVersion(koodiUriAsString)
      Some(s"${koodiUri.koodiUri}#${koodiUri.versio}")
    }

  private def getKoodiUriVersionAsStrSeqIfEmpty(currentValue: Seq[String], koodiUriAsString: String): Seq[String] =
    if (currentValue.isEmpty)
      Seq(getKoodiUriVersionIfEmpty(None, koodiUriAsString).get)
    else currentValue

  private def getLaajuusyksikkoKoodiUriVersionAsNeeded(
      laajuusNumero: Option[Double],
      currentKoodiUri: Option[String],
      koodiUriAsString: String
  ): Option[String] =
    if (laajuusNumero.isDefined)
      getKoodiUriVersionIfEmpty(currentKoodiUri, koodiUriAsString)
    else
      currentKoodiUri

  private def enrichKoulutusMetadata(koulutus: Koulutus): Option[KoulutusMetadata] = {
    val muokkaajanOrganisaatiot = kayttooikeusClient.getOrganisaatiotFromCache(koulutus.muokkaaja)
    val isOphVirkailija         = ServiceUtils.hasOphOrganisaatioOid(muokkaajanOrganisaatiot)

    koulutus.metadata match {
      case Some(metadata) =>
        metadata match {
          case korkeakoulutusKoulutusMetadata: KorkeakoulutusKoulutusMetadata =>
            korkeakoulutusKoulutusMetadata match {
              case yoMetadata: YliopistoKoulutusMetadata =>
                Some(
                  yoMetadata.copy(
                    isMuokkaajaOphVirkailija = Some(isOphVirkailija),
                    opintojenLaajuusyksikkoKoodiUri = getLaajuusyksikkoKoodiUriVersionAsNeeded(
                      yoMetadata.opintojenLaajuusNumero,
                      yoMetadata.opintojenLaajuusyksikkoKoodiUri,
                      opintojenLaajuusOpintopiste
                    )
                  )
                )
              case amkMetadata: AmmattikorkeakouluKoulutusMetadata =>
                Some(
                  amkMetadata.copy(
                    isMuokkaajaOphVirkailija = Some(isOphVirkailija),
                    opintojenLaajuusyksikkoKoodiUri = getLaajuusyksikkoKoodiUriVersionAsNeeded(
                      amkMetadata.opintojenLaajuusNumero,
                      amkMetadata.opintojenLaajuusyksikkoKoodiUri,
                      opintojenLaajuusOpintopiste
                    )
                  )
                )
              case m: AmmOpeErityisopeJaOpoKoulutusMetadata =>
                Some(
                  m.copy(
                    isMuokkaajaOphVirkailija = Some(isOphVirkailija),
                    koulutusalaKoodiUrit = getKoodiUriVersionAsStrSeqIfEmpty(
                      m.koulutusalaKoodiUrit,
                      "kansallinenkoulutusluokitus2016koulutusalataso1_01"
                    ),
                    opintojenLaajuusyksikkoKoodiUri =
                      getKoodiUriVersionIfEmpty(m.opintojenLaajuusyksikkoKoodiUri, opintojenLaajuusOpintopiste),
                    opintojenLaajuusNumero = Some(m.opintojenLaajuusNumero.getOrElse(60))
                  )
                )
              case m: OpePedagOpinnotKoulutusMetadata =>
                Some(
                  m.copy(
                    isMuokkaajaOphVirkailija = Some(isOphVirkailija),
                    koulutusalaKoodiUrit = getKoodiUriVersionAsStrSeqIfEmpty(
                      m.koulutusalaKoodiUrit,
                      "kansallinenkoulutusluokitus2016koulutusalataso1_01"
                    ),
                    opintojenLaajuusyksikkoKoodiUri =
                      getKoodiUriVersionIfEmpty(m.opintojenLaajuusyksikkoKoodiUri, opintojenLaajuusOpintopiste),
                    opintojenLaajuusNumero = Some(m.opintojenLaajuusNumero.getOrElse(60))
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
            Some(
              lukioMetadata.copy(
                isMuokkaajaOphVirkailija = Some(isOphVirkailija),
                koulutusalaKoodiUrit = getKoodiUriVersionAsStrSeqIfEmpty(
                  lukioMetadata.koulutusalaKoodiUrit,
                  "kansallinenkoulutusluokitus2016koulutusalataso1_00"
                ),
                opintojenLaajuusyksikkoKoodiUri = getLaajuusyksikkoKoodiUriVersionAsNeeded(
                  lukioMetadata.opintojenLaajuusNumero,
                  lukioMetadata.opintojenLaajuusyksikkoKoodiUri,
                  opintojenLaajuusOpintopiste
                )
              )
            )
          case tuvaMetadata: TuvaKoulutusMetadata =>
            Some(
              tuvaMetadata.copy(
                isMuokkaajaOphVirkailija = Some(isOphVirkailija),
                opintojenLaajuusyksikkoKoodiUri =
                  getKoodiUriVersionIfEmpty(tuvaMetadata.opintojenLaajuusyksikkoKoodiUri, opintojenLaajuusViikko)
              )
            )
          case telmaMetadata: TelmaKoulutusMetadata =>
            Some(
              telmaMetadata.copy(
                isMuokkaajaOphVirkailija = Some(isOphVirkailija),
                opintojenLaajuusyksikkoKoodiUri =
                  getKoodiUriVersionIfEmpty(telmaMetadata.opintojenLaajuusyksikkoKoodiUri, opintojenLaajuusOsaamispiste)
              )
            )
          case vapaaSivistystyoKoulutusMetadata: VapaaSivistystyoKoulutusMetadata =>
            vapaaSivistystyoKoulutusMetadata match {
              case vapaaSivistystyoMuuMetadata: VapaaSivistystyoMuuKoulutusMetadata =>
                Some(vapaaSivistystyoMuuMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
              case m: VapaaSivistystyoOpistovuosiKoulutusMetadata =>
                Some(
                  m.copy(
                    isMuokkaajaOphVirkailija = Some(isOphVirkailija),
                    opintojenLaajuusyksikkoKoodiUri =
                      getKoodiUriVersionIfEmpty(m.opintojenLaajuusyksikkoKoodiUri, opintojenLaajuusOpintopiste)
                  )
                )
            }
          case aikuistenPerusopetusKoulutusMetadata: AikuistenPerusopetusKoulutusMetadata =>
            Some(aikuistenPerusopetusKoulutusMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case m: KkOpintojaksoKoulutusMetadata =>
            Some(
              m.copy(
                opintojenLaajuusyksikkoKoodiUri = getLaajuusyksikkoKoodiUriVersionAsNeeded(
                  m.opintojenLaajuusNumeroMin,
                  m.opintojenLaajuusyksikkoKoodiUri,
                  opintojenLaajuusOpintopiste
                ),
                isMuokkaajaOphVirkailija = Some(isOphVirkailija)
              )
            )
          case m: ErikoislaakariKoulutusMetadata =>
            Some(
              m.copy(
                isMuokkaajaOphVirkailija = Some(isOphVirkailija),
                koulutusalaKoodiUrit = getKoodiUriVersionAsStrSeqIfEmpty(
                  m.koulutusalaKoodiUrit,
                  "kansallinenkoulutusluokitus2016koulutusalataso2_091"
                )
              )
            )
          case m: KkOpintokokonaisuusKoulutusMetadata =>
            Some(
              m.copy(
                opintojenLaajuusyksikkoKoodiUri = getLaajuusyksikkoKoodiUriVersionAsNeeded(
                  m.opintojenLaajuusNumeroMin,
                  m.opintojenLaajuusyksikkoKoodiUri,
                  opintojenLaajuusOpintopiste
                ),
                isMuokkaajaOphVirkailija = Some(isOphVirkailija)
              )
            )
          case m: ErikoistumiskoulutusMetadata =>
            Some(
              m.copy(
                opintojenLaajuusyksikkoKoodiUri = getLaajuusyksikkoKoodiUriVersionAsNeeded(
                  m.opintojenLaajuusNumeroMin,
                  m.opintojenLaajuusyksikkoKoodiUri,
                  opintojenLaajuusOpintopiste
                ),
                isMuokkaajaOphVirkailija = Some(isOphVirkailija)
              )
            )
          case tpoMetadata: TaiteenPerusopetusKoulutusMetadata =>
            Some(tpoMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case muuMetadata: MuuKoulutusMetadata =>
            Some(muuMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
        }
      case None => None
    }
  }

  def enrichAndPopulateFixedDefaultValues(koulutus: Koulutus): Koulutus = {
    val enrichedMetadata: Option[KoulutusMetadata] = enrichKoulutusMetadata(koulutus)
    val enrichedKoulutus = koulutus.koulutustyyppi match {
      case Amm if koulutus.nimi.isEmpty && koulutus.koulutuksetKoodiUri.nonEmpty =>
        val koodiUri = getKoodiUriVersion(koulutus.koulutuksetKoodiUri.head)
        koulutus.copy(
          nimi = NameHelper.mergeNames(koodiUri.nimi, koulutus.nimi, koulutus.kielivalinta)
        )
      case AmmTutkinnonOsa if notFullyPopulated(koulutus.nimi, koulutus.kielivalinta) && koulutus.metadata.isDefined =>
        koulutus.metadata match {
          case Some(m: AmmatillinenTutkinnonOsaKoulutusMetadata)
              if m.tutkinnonOsat.size == 1 && m.tutkinnonOsat.head.idValuesPopulated() =>
            val ePerusteId = m.tutkinnonOsat.head.ePerusteId.get
            val osaId      = m.tutkinnonOsat.head.tutkinnonosaId.get
            val viiteId    = m.tutkinnonOsat.head.tutkinnonosaViite.get
            ePerusteKoodiClient.getTutkinnonosatForEPerusteetFromCache(Seq(ePerusteId)) match {
              case Left(exp) => throw exp
              case Right(osaMap) if osaMap(ePerusteId).exists(osa => osa.viiteId == viiteId && osa.id == osaId) =>
                val nimiFromService = osaMap(ePerusteId)
                  .find(osa => osa.viiteId == viiteId && osa.id == osaId)
                  .map(_.nimi)
                  .getOrElse(Map())
                koulutus.copy(
                  nimi = mergeNames(nimiFromService, koulutus.nimi, koulutus.kielivalinta)
                )
              case _ => koulutus
            }
          case _ => koulutus
        }
      case AmmOsaamisala
          if notFullyPopulated(
            koulutus.nimi,
            koulutus.kielivalinta
          ) && koulutus.ePerusteId.isDefined && koulutus.metadata.isDefined =>
        koulutus.metadata match {
          case Some(m: AmmatillinenOsaamisalaKoulutusMetadata) if m.osaamisalaKoodiUri.isDefined =>
            ePerusteKoodiClient.getOsaamisalaKoodiuritForEPerusteFromCache(koulutus.ePerusteId.get) match {
              case Left(exp) => throw exp
              case Right(osaamisAlat) =>
                val koodiUriFromService = osaamisAlat.find(_.koodiUri == m.osaamisalaKoodiUri.get)
                if (koodiUriFromService.isDefined)
                  koulutus.copy(nimi =
                    NameHelper.mergeNames(koodiUriFromService.get.nimi, koulutus.nimi, koulutus.kielivalinta)
                  )
                else
                  koulutus
            }
          case _ => koulutus
        }
      case OpePedagOpinnot =>
        koulutus.copy(koulutuksetKoodiUri =
          getKoodiUriVersionAsStrSeqIfEmpty(koulutus.koulutuksetKoodiUri, "koulutus_919999")
        )
      case AikuistenPerusopetus =>
        koulutus.copy(koulutuksetKoodiUri =
          getKoodiUriVersionAsStrSeqIfEmpty(koulutus.koulutuksetKoodiUri, "koulutus_201101")
        )
      case TaiteenPerusopetus =>
        koulutus.copy(koulutuksetKoodiUri =
          getKoodiUriVersionAsStrSeqIfEmpty(koulutus.koulutuksetKoodiUri, "koulutus_999907")
        )
      case _ => koulutus
    }
    enrichedKoulutus.copy(metadata = enrichedMetadata)
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
        Some(AuthorizationRuleForReadJulkinen),
        getTarjoajat(koulutusWithTime)
      )
    )
  }


  def put(koulutus: Koulutus)(implicit authenticated: Authenticated): CreateResult = {
    val rules = if (Koulutustyyppi.isKoulutusSaveAllowedOnlyForOph(koulutus.koulutustyyppi)) {
      List(AuthorizationRules(Seq(Role.Paakayttaja)))
    } else {
      val rulesForCreatingKoulutus = Some(
        AuthorizationRules(
          roleEntity.createRoles,
          overridingAuthorizationRule = Some(AuthorizationRuleByOrganizationAndKoulutustyyppi)
        )
      )
      val rulesForTarjoajat = if (koulutus.isAvoinKorkeakoulutus()) None else authorizedForTarjoajaOids(koulutus.tarjoajat.toSet)
      (rulesForCreatingKoulutus :: rulesForTarjoajat :: Nil).flatten
    }

    authorizePut(koulutus, rules) { k =>
      val enrichedKoulutusWithFixedDefaultValues = enrichAndPopulateFixedDefaultValues(k)
      koulutusServiceValidation.withValidation(enrichedKoulutusWithFixedDefaultValues, None) { ek =>
        doPut(ek)
      }
    }
  }

  def update(newKoulutus: Koulutus, notModifiedSince: Instant, fromExternal: Boolean = false)(implicit
      authenticated: Authenticated
  ): UpdateResult = {
    val oldKoulutusWithInstant = KoulutusDAO.get(newKoulutus.oid.get, TilaFilter.onlyOlemassaolevat())
    oldKoulutusWithInstant match {
      case Some((oldKoulutus, _)) =>
        if (fromExternal) {
          authorizeAddedTarjoajatFromExternal(
            newKoulutus.organisaatioOid,
            newKoulutus.tarjoajat.toSet diff oldKoulutus.tarjoajat.toSet
          )
        }
        val rules: List[AuthorizationRules] = getAuthorizationRulesForUpdate(newKoulutus, oldKoulutus)
        val result = authorizeUpdate(oldKoulutusWithInstant, newKoulutus, rules) { (_, k) =>
          val enrichedKoulutusWithFixedDefaultValues = enrichAndPopulateFixedDefaultValues(k)
          koulutusServiceValidation.withValidation(enrichedKoulutusWithFixedDefaultValues, Some(oldKoulutus)) {
            doUpdate(_, notModifiedSince, oldKoulutus)
          }
        }
        result
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
            if (newKoulutus.isAvoinKorkeakoulutus()) {
              rulesForUpdatingKoulutus.toList
            } else {
              val newTarjoajat = newKoulutus.tarjoajat.toSet
              val oldTarjoajat = oldKoulutus.tarjoajat.toSet
              val rulesForAddedTarjoajat   = authorizedForTarjoajaOids(newTarjoajat diff oldTarjoajat)
              (rulesForUpdatingKoulutus :: rulesForAddedTarjoajat :: Nil).flatten
            }
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

  def getUpdateTarjoajatActions(
      koulutusWithLastModified: (Koulutus, Instant),
      newTarjoajatInToteutus: Set[OrganisaatioOid],
      tarjoajatSafeToDelete: Set[OrganisaatioOid]
  )(implicit authenticated: Authenticated): DBIO[(Koulutus, Option[Koulutus])] = {

    val (koulutus, lastModified) = koulutusWithLastModified

    val tarjoajatAddedToKoulutus     = newTarjoajatInToteutus diff koulutus.tarjoajat.toSet
    val newTarjoajatForKoulutus      = (koulutus.tarjoajat.toSet diff tarjoajatSafeToDelete) ++ tarjoajatAddedToKoulutus
    val tarjoajatRemovedFromKoulutus = koulutus.tarjoajat.toSet diff newTarjoajatForKoulutus

    val isAvoinKorkeakoulutus = koulutus.isAvoinKorkeakoulutus()

    if (isAvoinKorkeakoulutus || (tarjoajatAddedToKoulutus.isEmpty && tarjoajatRemovedFromKoulutus.isEmpty)) {
      DBIO.successful((koulutus, None))
    } else {
      val newKoulutus: Koulutus = koulutus.copy(tarjoajat = newTarjoajatForKoulutus.toList)
      authorizeUpdate(
        Some(koulutusWithLastModified),
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

  private def doPut(koulutus: Koulutus)(implicit authenticated: Authenticated): CreateResult =
    KoutaDatabase.runBlockingTransactionally {
      for {
        (teema, k) <- checkAndMaybeClearTeemakuva(koulutus)
        k          <- KoulutusDAO.getPutActions(k)
        k          <- maybeCopyTeemakuva(teema, k)
        k          <- teema.map(_ => KoulutusDAO.updateJustKoulutus(k)).getOrElse(DBIO.successful(k))
        _          <- auditLog.logCreate(k)
      } yield (teema, k)
    }.map { case (teema, k: Koulutus) =>
      maybeDeleteTempImage(teema)
      val warnings = quickIndex(k.oid) ++ index(Some(k))
      CreateResult(k.oid.get, warnings)
    }.get

  private def doUpdate(koulutus: Koulutus, notModifiedSince: Instant, before: Koulutus)(implicit
      authenticated: Authenticated
  ): UpdateResult =
    KoutaDatabase.runBlockingTransactionally {
      for {
        _          <- KoulutusDAO.checkNotModified(koulutus.oid.get, notModifiedSince)
        (teema, k) <- checkAndMaybeCopyTeemakuva(koulutus)
        k          <- KoulutusDAO.getUpdateActions(k)
        _          <- auditLog.logUpdate(before, k)
      } yield (teema, k)
    }.map { case (teema, k: Option[Koulutus]) =>
      maybeDeleteTempImage(teema)
      val warnings = quickIndex(k.flatMap(_.oid)) ++ index(k)
      UpdateResult(updated = k.isDefined, warnings)
    }.get

  def index(koulutus: Option[Koulutus]): List[String] =
    sqsInTransactionService.toSQSQueue(HighPriority, IndexTypeKoulutus, koulutus.map(_.oid.get.toString))

  private def quickIndex(koulutusOid: Option[KoulutusOid]): List[String] = {
    koulutusOid match {
      case Some(oid) => koutaIndeksoijaClient.quickIndexEntity("koulutus", oid.toString)
      case None => List.empty
    }
  }

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
