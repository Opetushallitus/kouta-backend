package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid}
import fi.oph.kouta.domain._
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeKoulutus}
import fi.oph.kouta.repository.{HakutietoDAO, KoulutusDAO, ToteutusDAO}
import fi.oph.kouta.security.Role
import fi.oph.kouta.servlet.Authenticated

object KoulutusService extends KoulutusService(SqsInTransactionService)

abstract class KoulutusService(sqsInTransactionService: SqsInTransactionService) extends ValidatingService[Koulutus] with AuthorizationService {

  def put(koulutus: Koulutus)(implicit authenticated: Authenticated): KoulutusOid = {
    withAuthorizedChildOrganizationOids(Role.CrudUser) { authorizedOrganizations =>
      authorizeRootOrAll(koulutus.tarjoajat.toSet + koulutus.organisaatioOid, authorizedOrganizations) {
        withValidation(koulutus, putWithIndexing)
      }
    }
  }

  def update(koulutus: Koulutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    withAuthorizedChildOrganizationOids(Role.CrudUser) { authorizedOrganizations =>
      val existing = KoulutusDAO.get(koulutus.oid.get).getOrElse(throw new NoSuchElementException("koulutusOid"))._1

      val tarjoajatToBeAdded = koulutus.tarjoajat.toSet diff existing.tarjoajat.toSet
      val tarjoajatToBeRemoved = existing.tarjoajat.toSet diff koulutus.tarjoajat.toSet

      authorizeRootOrAll(tarjoajatToBeAdded ++ tarjoajatToBeRemoved + koulutus.organisaatioOid, authorizedOrganizations) {
        withValidation(koulutus, updateWithIndexing(_, notModifiedSince))
      }
    }
  }

  def get(oid: KoulutusOid)(implicit authenticated: Authenticated): Option[(Koulutus, Instant)] = {
    KoulutusDAO.get(oid).map {
      case (koulutus, lastModified) if hasRootAccess(Role.Read, Role.CrudUser) => (koulutus, lastModified)
      case (koulutus, lastModified) if koulutus.julkinen => (koulutus, lastModified)
      case (koulutus, lastModified) =>
        withAuthorizedParentAndChildOrganizationOids(Role.Read, Role.CrudUser) { authorizedOrganizations =>
          val allowedOrganizations = koulutus.tarjoajat.toSet + koulutus.organisaatioOid
          authorizeAny(allowedOrganizations, authorizedOrganizations) {
            (koulutus, lastModified)
          }
        }
    }
  }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[KoulutusListItem] = {
    withAuthorizedParentAndChildOrganizationOids(organisaatioOid, Role.Read, Role.CrudUser) { oids =>
      KoulutusDAO.listByOrganisaatioOidsOrJulkinen(oids)
    }
  }

  def toteutukset(oid: KoulutusOid, vainJulkaistut: Boolean)(implicit authenticated: Authenticated): Seq[Toteutus] =
    withRootAccess(Role.Read) {
      if (vainJulkaistut) {
        ToteutusDAO.getJulkaistutByKoulutusOid(oid)
      } else {
        ToteutusDAO.getByKoulutusOid(oid)
      }
    }

  def hakutiedot(oid: KoulutusOid)(implicit authenticated: Authenticated): Seq[Hakutieto] = {
    withRootAccess(Role.Read) {
      HakutietoDAO.getByKoulutusOid(oid)
    }
  }

  def listToteutukset(oid: KoulutusOid)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
    withRootAccess(Role.Read, Role.CrudUser)(ToteutusDAO.listByKoulutusOid(oid))

  def listToteutukset(oid: KoulutusOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[ToteutusListItem] = {
    withAuthorizedChildOrganizationOids(organisaatioOid, Role.Read, Role.CrudUser) {
      ToteutusDAO.listByKoulutusOidAndOrganisaatioOids(oid, _)
    }
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
