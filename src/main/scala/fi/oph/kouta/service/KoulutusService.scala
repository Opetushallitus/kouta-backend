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

  private val createRoles = Seq(Role.Koulutus.Crud)
  private val readRoles = Seq(Role.Koulutus.Read, Role.Koulutus.Crud, Role.Indexer)
  private val updateRoles = Seq(Role.Koulutus.Update, Role.Koulutus.Crud)
  private val indexerRoles = Seq(Role.Indexer)

  def put(koulutus: Koulutus)(implicit authenticated: Authenticated): KoulutusOid = {
    withAuthorizedChildOrganizationOids(createRoles) { authorizedOrganizations =>
      authorize(koulutus.organisaatioOid, authorizedOrganizations) {
        withValidation(koulutus, putWithIndexing)
      }
    }
  }

  def update(koulutus: Koulutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    withAuthorizedChildOrganizationOids(updateRoles) { authorizedOrganizations =>
      val existing = KoulutusDAO.get(koulutus.oid.get).getOrElse(throw new NoSuchElementException("koulutusOid"))._1

      authorize(existing.organisaatioOid, authorizedOrganizations) {
        withValidation(koulutus, updateWithIndexing(_, notModifiedSince))
      }
    }
  }

  def get(oid: KoulutusOid)(implicit authenticated: Authenticated): Option[(Koulutus, Instant)] = {
    KoulutusDAO.get(oid).map {
      case (koulutus, lastModified) if hasRootAccess(readRoles) => (koulutus, lastModified)
      case (koulutus, lastModified) if koulutus.julkinen => (koulutus, lastModified)
      case (koulutus, lastModified) =>
        withAuthorizedChildOrganizationOids(readRoles) { authorizedOrganizations =>
          authorize(koulutus.organisaatioOid, authorizedOrganizations) {
            (koulutus, lastModified)
          }
        }
    }
  }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[KoulutusListItem] = {
    withAuthorizedChildOrganizationOids(organisaatioOid, readRoles) { oids =>
      KoulutusDAO.listByOrganisaatioOidsOrJulkinen(oids)
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
    withRootAccess(readRoles)(ToteutusDAO.listByKoulutusOid(oid))

  def listToteutukset(oid: KoulutusOid, organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[ToteutusListItem] = {
    withAuthorizedChildOrganizationOids(organisaatioOid, readRoles) {
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
