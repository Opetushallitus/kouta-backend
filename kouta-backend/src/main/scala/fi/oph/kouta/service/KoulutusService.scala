package fi.oph.kouta.service

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{KayttooikeusClient, KoutaIndexClient, OppijanumerorekisteriClient}
import fi.oph.kouta.domain.Koulutustyyppi.oppilaitostyyppi2koulutustyyppi
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, RootOrganisaatioOid}
import fi.oph.kouta.images.{S3ImageService, TeemakuvaService}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeKoulutus}
import fi.oph.kouta.repository._
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.{Authenticated, EntityNotFoundException}
import fi.oph.kouta.util.{NameHelper, ServiceUtils}
import fi.oph.kouta.validation.Validations._
import fi.vm.sade.utils.slf4j.Logging
import slick.dbio.DBIO

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

object KoulutusService
    extends KoulutusService(SqsInTransactionService, S3ImageService, AuditLog, OrganisaatioServiceImpl, OppijanumerorekisteriClient, KayttooikeusClient)

class KoulutusService(
    sqsInTransactionService: SqsInTransactionService,
    val s3ImageService: S3ImageService,
    auditLog: AuditLog,
    val organisaatioService: OrganisaatioService,
    oppijanumerorekisteriClient: OppijanumerorekisteriClient,
    kayttooikeusClient: KayttooikeusClient
) extends ValidatingService[Koulutus]
    with RoleEntityAuthorizationService[Koulutus]
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


  private def enrichKoulutusMetadata(koulutus: Koulutus) : Option[KoulutusMetadata] = {
    val muokkaajanOrganisaatiot = kayttooikeusClient.getOrganisaatiot(koulutus.muokkaaja)
    val isOphVirkailija = ServiceUtils.hasOphOrganisaatioOid(muokkaajanOrganisaatiot)

    koulutus.metadata match {
      case Some(metadata) =>
        metadata match {
          case kkMetadata: KorkeakoulutusKoulutusMetadata => kkMetadata match {
            case yoMetadata: YliopistoKoulutusMetadata => Some(yoMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
            case amkMetadata: AmmattikorkeakouluKoulutusMetadata => Some(amkMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          }
          case ammMetadata: AmmatillinenKoulutusMetadata => Some(ammMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case ammTutkinnonOsaMetadata: AmmatillinenTutkinnonOsaKoulutusMetadata => Some(ammTutkinnonOsaMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case ammOsaamisalaMetadata: AmmatillinenOsaamisalaKoulutusMetadata => Some(ammOsaamisalaMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case lukioMetadata: LukioKoulutusMetadata => Some(lukioMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case tuvaMetadata: TuvaKoulutusMetadata => Some(tuvaMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case telmaMetadata: TelmaKoulutusMetadata => Some(telmaMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
          case vapaaSivistystyoMetadata: VapaaSivistystyoKoulutusMetadata =>
            vapaaSivistystyoMetadata match {
              case vapaaSivistystyoMuuMetadata: VapaaSivistystyoMuuKoulutusMetadata => Some(vapaaSivistystyoMuuMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
              case vapaaSivistystyoOpistovuosiMetadata: VapaaSivistystyoOpistovuosiKoulutusMetadata => Some(vapaaSivistystyoOpistovuosiMetadata.copy(isMuokkaajaOphVirkailija = Some(isOphVirkailija)))
            }
        }
      case None => None
    }
  }


  def get(oid: KoulutusOid, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Option[(Koulutus, Instant)] = {
    val koulutusWithTime: Option[(Koulutus, Instant)] = KoulutusDAO.get(oid, tilaFilter)

    val enrichedKoulutus = koulutusWithTime match {
      case Some((k, i)) => {
        val muokkaaja = oppijanumerorekisteriClient.getHenkilö(k.muokkaaja)
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

    val enrichedMetadata: Option[KoulutusMetadata] = enrichKoulutusMetadata(koulutus)
    val enrichedKoulutus = koulutus.copy(metadata = enrichedMetadata)

    authorizePut(enrichedKoulutus, rules) { k =>
      withValidation(k, None) { k =>
        validateSorakuvausIntegrity(koulutus)
        doPut(k)
      }
    }.oid.get
  }

  def update(newKoulutus: Koulutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    val oldKoulutusWithInstant: Option[(Koulutus, Instant)] = KoulutusDAO.get(newKoulutus.oid.get, TilaFilter.onlyOlemassaolevat())
    oldKoulutusWithInstant match {
      case Some((oldKoulutus, _)) =>
        val rules: List[AuthorizationRules] = oldKoulutus.koulutustyyppi match {
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
        rules.nonEmpty && authorizeUpdate(oldKoulutusWithInstant, newKoulutus, rules) { (_, k) =>
          val enrichedMetadata: Option[KoulutusMetadata] = enrichKoulutusMetadata(k)
          val enrichedKoulutus = k.copy(metadata = enrichedMetadata)
          withValidation(enrichedKoulutus, Some(oldKoulutus)) {
            throwValidationErrors(validateStateChange("koulutukselle", oldKoulutus.tila, newKoulutus.tila))
            validateSorakuvausIntegrity(k)
            validateToteutusIntegrityIfDeletingKoulutus(oldKoulutus.tila, newKoulutus.tila, newKoulutus.oid.get)
            doUpdate(_, notModifiedSince, oldKoulutus)
          }
        }.nonEmpty
      case _ => throw EntityNotFoundException(s"Päivitettävää asiaa ei löytynyt")
    }
  }

  private def validateSorakuvausIntegrity(koulutus: Koulutus): Unit = {
    throwValidationErrors(
      validateIfDefined[UUID](
        koulutus.sorakuvausId,
        sorakuvausId => {
          val (sorakuvausTila, sorakuvausTyyppi, koulutuskoodiUrit) =
            SorakuvausDAO.getTilaTyyppiAndKoulutusKoodit(sorakuvausId)
          and(
            validateDependency(koulutus.tila, sorakuvausTila, sorakuvausId, "Sorakuvausta", "sorakuvausId"),
            validateIfDefined[Koulutustyyppi](
              sorakuvausTyyppi,
              sorakuvausTyyppi =>
                // "Tutkinnon osa" ja Osaamisala koulutuksiin saa liittää myös SORA-kuvauksen, jonka koulutustyyppi on "ammatillinen"
                assertTrue(
                  sorakuvausTyyppi == koulutus.koulutustyyppi || (sorakuvausTyyppi == Amm && Seq(
                    AmmOsaamisala,
                    AmmTutkinnonOsa
                  ).contains(koulutus.koulutustyyppi)),
                  "koulutustyyppi",
                  tyyppiMismatch("sorakuvauksen", sorakuvausId)
                )
            ),
            validateIfDefined[Seq[String]](
              koulutuskoodiUrit,
              koulutuskoodiUrit => {
                validateIfTrue(
                  koulutuskoodiUrit.nonEmpty,
                  assertTrue(
                    koulutuskoodiUrit.intersect(koulutus.koulutuksetKoodiUri).nonEmpty,
                    "koulutuksetKoodiUri",
                    valuesDontMatch("Sorakuvauksen", "koulutusKoodiUrit")
                  )
                )
              }
            )
          )
        }
      )
    )
  }

  private def validateToteutusIntegrityIfDeletingKoulutus(aiempiTila: Julkaisutila, tulevaTila: Julkaisutila, koulutusOid: KoulutusOid) = {
    throwValidationErrors(
      validateIfTrue(tulevaTila == Poistettu && tulevaTila != aiempiTila, assertTrue(
        ToteutusDAO.getByKoulutusOid(koulutusOid, TilaFilter.onlyOlemassaolevat()).isEmpty,
          "tila",
          integrityViolationMsg("Koulutusta", "toteutuksia")))
    )
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

  def toteutukset(oid: KoulutusOid, tilaFilter: TilaFilter)
                 (implicit authenticated: Authenticated): Seq[Toteutus] =
    withRootAccess(indexerRoles) {
      ToteutusDAO.getByKoulutusOid(oid, tilaFilter)
    }

  def hakutiedot(oid: KoulutusOid)(implicit authenticated: Authenticated): Seq[Hakutieto] =
    withRootAccess(indexerRoles) {
      HakutietoDAO.getByKoulutusOid(oid)
    }

  def listToteutukset(oid: KoulutusOid, tilaFilter: TilaFilter)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
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

  def search(organisaatioOid: OrganisaatioOid, params: Map[String, String])(implicit
      authenticated: Authenticated
  ): KoulutusSearchResult = {
    def getCount(k: KoulutusSearchItemFromIndex, organisaatioOids: Seq[OrganisaatioOid]): Integer = {
      organisaatioOids match {
        case Seq(RootOrganisaatioOid) => k.toteutukset.length
        case _ =>
          val oidStrings = organisaatioOids.map(_.toString())
          k.toteutukset.count(t => t.tila != Arkistoitu && t.tila != Poistettu && t.organisaatiot.exists(o => oidStrings.contains(o)))
      }
    }

    def assocToteutusCounts(r: KoulutusSearchResultFromIndex): KoulutusSearchResult =
      withAuthorizedOrganizationOids(
        organisaatioOid,
        AuthorizationRules(Role.Toteutus.readRoles, allowAccessToParentOrganizations = true)
      )(organisaatioOids => {
        KoulutusSearchResult(
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
      case Nil          => KoulutusSearchResult()
      case koulutusOids => assocToteutusCounts(KoutaIndexClient.searchKoulutukset(koulutusOids, params))
    }
  }

  def search(organisaatioOid: OrganisaatioOid, koulutusOid: KoulutusOid, params: Map[String, String])(implicit
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

    filterToteutukset(KoutaIndexClient.searchKoulutukset(Seq(koulutusOid), params).result.headOption)
  }

  def getUpdateTarjoajatActions(koulutusOid: KoulutusOid, newTarjoajatInToteutus: Set[OrganisaatioOid],
                                tarjoajatSafeToDelete: Set[OrganisaatioOid])
                               (implicit authenticated: Authenticated): DBIO[(Koulutus, Option[Koulutus])] = {
    val koulutusWithLastModified = get(koulutusOid, TilaFilter.onlyOlemassaolevat())

    if (koulutusWithLastModified.isEmpty) {
      throw EntityNotFoundException(s"Päivitettävää asiaa ei löytynyt")
    }

    val Some((koulutus, lastModified)) = koulutusWithLastModified

    val tarjoajatAddedToKoulutus = newTarjoajatInToteutus diff koulutus.tarjoajat.toSet
    val newTarjoajatForKoulutus = (koulutus.tarjoajat.toSet diff tarjoajatSafeToDelete) ++ tarjoajatAddedToKoulutus
    val tarjoajatRemovedFromKoulutus = koulutus.tarjoajat.toSet diff newTarjoajatForKoulutus

    if (tarjoajatAddedToKoulutus.isEmpty && tarjoajatRemovedFromKoulutus.isEmpty) {
      DBIO.successful((koulutus, None))
    } else {
      val newKoulutus: Koulutus = koulutus.copy(tarjoajat = newTarjoajatForKoulutus.toList)
      authorizeUpdate(
        koulutusWithLastModified,
        newKoulutus,
        List(authorizedForTarjoajaOids(tarjoajatAddedToKoulutus ++ tarjoajatRemovedFromKoulutus, roleEntity.readRoles).get)
      ) { (_, k) =>
        withValidation(newKoulutus, Some(k)) {
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
      oppilaitostyyppi2koulutustyyppi.foldLeft(Map[Koulutustyyppi, Seq[String]]().withDefaultValue(Seq())) {
        case (m, (a, bs)) => bs.foldLeft(m)((map, b) => map.updated(b, m(b) :+ a))
      }.map(entry => KoulutustyyppiToOppilaitostyypit(entry._1, entry._2)).toSeq

    KoulutustyyppiToOppilaitostyyppiResult(koulutustyyppi2oppilaitostyyppi)
  }
}
