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

object KoulutusService extends KoulutusService(SqsInTransactionService)

abstract class KoulutusService(sqsInTransactionService: SqsInTransactionService) extends ValidatingService[Koulutus] with KoulutusAuthorizationService {

  def get(oid: KoulutusOid)(implicit authenticated: Authenticated): Option[(Koulutus, Instant)] =
    authorizeGetKoulutus(KoulutusDAO.get(oid))

  def putWithTempImage(koulutus: Koulutus, f: Koulutus => KoulutusOid): KoulutusOid = {
    koulutus.metadata.flatMap(_.teemakuva) match {
      case Some(S3Service.tempUrl(filename)) =>
        val oid = f(koulutus.copy(metadata = koulutus.metadata.map(_.withTeemakuva(None))))
        val url = S3Service.copyImage(S3Service.getTempKey(filename), s"koulutus-teemakuva/$oid/$filename")
        updateWithIndexing(koulutus.copy(oid = Some(oid), metadata = koulutus.metadata.map(_.withTeemakuva(Some(url)))), Instant.now())
        S3Service.deleteImage(S3Service.getTempKey(filename))
        oid
      case _ => f(koulutus)
    }
  }

  def put(koulutus: Koulutus)(implicit authenticated: Authenticated): KoulutusOid =
    authorizePut(koulutus) {
      withValidation(koulutus, putWithTempImage(_, putWithIndexing))
    }

  def updateWithTempImage(koulutus: Koulutus, f: Koulutus => Boolean): Boolean = {
    koulutus.metadata.flatMap(_.teemakuva) match {
      case Some(S3Service.tempUrl(filename)) =>
        val url = S3Service.copyImage(S3Service.getTempKey(filename), s"koulutus-teemakuva/${koulutus.oid.get}/$filename")
        val changed = f(koulutus.copy(metadata = koulutus.metadata.map(_.withTeemakuva(Some(url)))))
        S3Service.deleteImage(S3Service.getTempKey(filename))
        changed
      case _ => f(koulutus)
    }
  }

  def update(koulutus: Koulutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(KoulutusDAO.get(koulutus.oid.get)) {
      withValidation(koulutus, updateWithTempImage(_, updateWithIndexing(_, notModifiedSince)))
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
