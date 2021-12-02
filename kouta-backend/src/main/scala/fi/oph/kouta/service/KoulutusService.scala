package fi.oph.kouta.service

import java.time.Instant
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.KoutaIndexClient
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, RootOrganisaatioOid}
import fi.oph.kouta.images.{S3ImageService, TeemakuvaService}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeKoulutus}
import fi.oph.kouta.repository.{HakutietoDAO, KoulutusDAO, KoutaDatabase, SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.{Authenticated, EntityNotFoundException}
import fi.vm.sade.utils.slf4j.Logging
import slick.dbio.DBIO

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

object KoulutusService
    extends KoulutusService(SqsInTransactionService, S3ImageService, AuditLog, OrganisaatioServiceImpl)

class KoulutusService(
    sqsInTransactionService: SqsInTransactionService,
    val s3ImageService: S3ImageService,
    auditLog: AuditLog,
    val organisaatioService: OrganisaatioService
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

  def get(oid: KoulutusOid)(implicit authenticated: Authenticated): Option[(Koulutus, Instant)] = {
    val koulutusWithTime: Option[(Koulutus, Instant)] = KoulutusDAO.get(oid)
    authorizeGet(
      koulutusWithTime,
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
      withValidation(k, None) { k =>
        validateSorakuvausIntegrity(koulutus)
        doPut(k)
      }
    }.oid.get
  }

  def update(newKoulutus: Koulutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    val oldKoulutusWithInstant: Option[(Koulutus, Instant)] = KoulutusDAO.get(newKoulutus.oid.get)
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
          withValidation(k, Some(oldKoulutus)) {
            validateSorakuvausIntegrity(k)
            doUpdate(_, notModifiedSince, oldKoulutus)
          }
        }.nonEmpty
      case _ => throw EntityNotFoundException(s"Päivitettävää asiaa ei löytynyt")
    }
  }

  private def validateSorakuvausIntegrity(koulutus: Koulutus): Unit = {
    import fi.oph.kouta.validation.Validations._

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

  def list(organisaatioOid: OrganisaatioOid, myosArkistoidut: Boolean)(implicit
      authenticated: Authenticated
  ): Seq[KoulutusListItem] =
    withAuthorizedOrganizationOidsAndOppilaitostyypit(organisaatioOid, readRules) { case (oids, koulutustyypit) =>
      KoulutusDAO.listAllowedByOrganisaatiot(oids, koulutustyypit, myosArkistoidut)
    }

  def listByKoulutustyyppi(organisaatioOid: OrganisaatioOid, koulutustyyppi: Koulutustyyppi, myosArkistoidut: Boolean)(
      implicit authenticated: Authenticated
  ): Seq[KoulutusListItem] =
    withAuthorizedOrganizationOids(organisaatioOid, readRules) { oids =>
      KoulutusDAO.listAllowedByOrganisaatiotAndKoulutustyyppi(oids, koulutustyyppi, myosArkistoidut)
    }

  def getTarjoajanJulkaistutKoulutukset(
      organisaatioOid: OrganisaatioOid
  )(implicit authenticated: Authenticated): Seq[Koulutus] =
    withRootAccess(indexerRoles) {
      KoulutusDAO.getJulkaistutByTarjoajaOids(
        organisaatioService.getAllChildOidsFlat(organisaatioOid, lakkautetut = true)
      )
    }

  def toteutukset(oid: KoulutusOid, vainJulkaistut: Boolean)(implicit authenticated: Authenticated): Seq[Toteutus] =
    withRootAccess(indexerRoles) {
      ToteutusDAO.getByKoulutusOid(oid, vainJulkaistut)
    }

  def hakutiedot(oid: KoulutusOid)(implicit authenticated: Authenticated): Seq[Hakutieto] =
    withRootAccess(indexerRoles) {
      HakutietoDAO.getByKoulutusOid(oid)
    }

  def listToteutukset(oid: KoulutusOid)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
    withRootAccess(indexerRoles)(ToteutusDAO.listByKoulutusOid(oid))

  def listToteutukset(oid: KoulutusOid, organisaatioOid: OrganisaatioOid)(implicit
      authenticated: Authenticated
  ): Seq[ToteutusListItem] =
    withAuthorizedOrganizationOids(
      organisaatioOid,
      AuthorizationRules(Role.Toteutus.readRoles, allowAccessToParentOrganizations = true)
    ) {
      case Seq(RootOrganisaatioOid) => ToteutusDAO.listByKoulutusOid(oid)
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
          k.toteutukset.count(t => t.tila != Arkistoitu && t.organisaatiot.exists(o => oidStrings.contains(o)))
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
              toteutusCount = getCount(k, organisaatioOids)
            )
          }
        )
      })

    list(organisaatioOid, myosArkistoidut = true).map(_.oid) match {
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

  def getAddTarjoajatActions(koulutusOid: KoulutusOid, tarjoajaOids: Set[OrganisaatioOid])(implicit
      authenticated: Authenticated
  ): DBIO[(Koulutus, Option[Koulutus])] = {
    val koulutusWithLastModified = get(koulutusOid)

    if (koulutusWithLastModified.isEmpty) {
      throw EntityNotFoundException(s"Päivitettävää asiaa ei löytynyt")
    }

    val Some((koulutus, lastModified)) = koulutusWithLastModified

    val newTarjoajat = tarjoajaOids diff koulutus.tarjoajat.toSet

    if (newTarjoajat.isEmpty) {
      DBIO.successful((koulutus, None))
    } else {
      val newKoulutus: Koulutus = koulutus.copy(tarjoajat = koulutus.tarjoajat ++ newTarjoajat)
      authorizeUpdate(
        koulutusWithLastModified,
        newKoulutus,
        List(authorizedForTarjoajaOids(tarjoajaOids, roleEntity.readRoles).get)
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
}
