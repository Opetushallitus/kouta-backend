package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.client.OrganisaatioClient
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid}
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeKoulutus}
import fi.oph.kouta.indexing.{S3Service, SqsInTransactionService}
import fi.oph.kouta.repository.{HakutietoDAO, KoulutusDAO, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated

trait KoulutusAuthorizationService extends RoleEntityAuthorizationService {
  protected val roleEntity: RoleEntity = Role.Koulutus

  def authorizeGetKoulutus(koulutusWithTime: Option[(Koulutus, Instant)])(implicit authenticated: Authenticated): Option[(Koulutus, Instant)] = {
    def allowedByOrgOrJulkinen(koulutus: Koulutus, oids: Set[OrganisaatioOid]): Boolean =
      lazyFlatChildren(oids).exists {
        case (orgs, tyypit) =>
          (koulutus.julkinen && koulutus.koulutustyyppi.exists(tyypit.contains)) || orgs.contains(koulutus.organisaatioOid)
      }

    koulutusWithTime.map {
      case (koulutus, lastModified) if hasRootAccess(Role.Koulutus.readRoles) => (koulutus, lastModified)
      case (koulutus, lastModified) =>
        organizationsForRoles(Role.Koulutus.readRoles) match {
          case oids if oids.isEmpty => throw RoleAuthorizationFailedException(Role.Koulutus.readRoles, authenticated.session.roles)
          case oids =>
            if (allowedByOrgOrJulkinen(koulutus, oids)) {
              (koulutus, lastModified)
            } else {
              throw OrganizationAuthorizationFailedException(koulutus.organisaatioOid)
            }
        }
    }
  }
}

object KoulutusService extends KoulutusService(SqsInTransactionService, S3Service)

class KoulutusService(sqsInTransactionService: SqsInTransactionService, val s3Service: S3Service)
  extends ValidatingService[Koulutus] with KoulutusAuthorizationService with TeemakuvaService[KoulutusOid, Koulutus, KoulutusMetadata] {

  val teemakuvaPrefix = "koulutus-teemakuva"

  def get(oid: KoulutusOid)(implicit authenticated: Authenticated): Option[(Koulutus, Instant)] =
    authorizeGetKoulutus(KoulutusDAO.get(oid))

  def put(koulutus: Koulutus)(implicit authenticated: Authenticated): KoulutusOid =
    authorizePut(koulutus) {
      withValidation(koulutus, checkTeemakuvaInPut(_, putWithIndexing, updateWithIndexing(_, Instant.now())))
    }

  def update(koulutus: Koulutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(KoulutusDAO.get(koulutus.oid.get)) {
      withValidation(koulutus, checkTeemakuvaInUpdate(_, updateWithIndexing(_, notModifiedSince)))
    }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[KoulutusListItem] = {
    withAuthorizedChildOrganizationOidsAndOppilaitostyypit(organisaatioOid, roleEntity.readRoles) { case (oids, koulutustyypit) =>
      KoulutusDAO.listByOrganisaatioOidsOrJulkinen(oids, koulutustyypit)
    }
  }

  def getTarjoajanJulkaistutKoulutukset(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[Koulutus] = {
    withRootAccess(indexerRoles) {
      KoulutusDAO.getJulkaistutByTarjoajaOids(OrganisaatioClient.getAllChildOidsAndOppilaitostyypitFlat(organisaatioOid)._1)
    }
  }

  def toteutukset(oid: KoulutusOid, vainJulkaistut: Boolean)(implicit authenticated: Authenticated): Seq[Toteutus] =
    withRootAccess(indexerRoles) {
      if (vainJulkaistut) {
        ToteutusDAO.getJulkaistutByKoulutusOid(oid)
      } else {
        ToteutusDAO.getByKoulutusOid(oid)
      }
    }

  def hakutiedot(oid: KoulutusOid)(implicit authenticated: Authenticated): Seq[Hakutieto] = {
    withRootAccess(indexerRoles) {
      HakutietoDAO.getByKoulutusOid(oid)
    }
  }

  def listToteutukset(oid: KoulutusOid)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
    withRootAccess(Role.Toteutus.readRoles)(ToteutusDAO.listByKoulutusOid(oid))

  def listToteutukset(oid: KoulutusOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, Role.Toteutus.readRoles) {
      ToteutusDAO.listByKoulutusOidAndOrganisaatioOids(oid, _)
    }

  private def putWithIndexing(koulutus: Koulutus) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeKoulutus,
      () => KoulutusDAO.getPutActions(koulutus))

  private def updateWithIndexing(koulutus: Koulutus, notModifiedSince: Instant) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeKoulutus,
      () => KoulutusDAO.getUpdateActions(koulutus, notModifiedSince),
      koulutus.oid.get.toString)
}
