package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid}
import fi.oph.kouta.domain._
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeKoulutus}
import fi.oph.kouta.repository.{HakutietoDAO, KoulutusDAO, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated

object KoulutusService extends KoulutusService(SqsInTransactionService)

abstract class KoulutusService(sqsInTransactionService: SqsInTransactionService) extends ValidatingService[Koulutus] with AuthorizationService {

  protected val roleEntity: RoleEntity = Role.Koulutus

  def get(oid: KoulutusOid)(implicit authenticated: Authenticated): Option[(Koulutus, Instant)] = {
    KoulutusDAO.get(oid).map {
      case (koulutus, lastModified) if hasRootAccess(roleEntity.readRoles) => (koulutus, lastModified)
      case (koulutus, lastModified) if koulutus.julkinen => (koulutus, lastModified) // TODO: sallittu vain saman koulutustyypin käyttäjille
      case (koulutus, lastModified) =>
        withAuthorizedChildOrganizationOids(roleEntity.readRoles) { authorizedOrganizations =>
          authorize(koulutus.organisaatioOid, authorizedOrganizations) {
            (koulutus, lastModified)
          }
        }
    }
  }

  def put(koulutus: Koulutus)(implicit authenticated: Authenticated): KoulutusOid =
    authorizePut(koulutus) {
      withValidation(koulutus, putWithIndexing)
    }

  def update(koulutus: Koulutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean =
    authorizeUpdate(KoulutusDAO.get(koulutus.oid.get).map(_._1)) {
      withValidation(koulutus, updateWithIndexing(_, notModifiedSince))
    }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[KoulutusListItem] = {
    withAuthorizedChildOrganizationOids(organisaatioOid, roleEntity.readRoles) { oids =>
      KoulutusDAO.listByOrganisaatioOidsOrJulkinen(oids) // TODO: julkiset vain samasta koulutustyypistä
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
