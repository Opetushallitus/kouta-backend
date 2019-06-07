package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeToteutus}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, ToteutusDAO}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.servlet.Authenticated

object ToteutusService extends ToteutusService(SqsInTransactionService)

abstract class ToteutusService(sqsInTransactionService: SqsInTransactionService) extends ValidatingService[Toteutus] with AuthorizationService {

  protected val roleEntity: RoleEntity = Role.Toteutus

  def get(oid: ToteutusOid)(implicit authenticated: Authenticated): Option[(Toteutus, Instant)] = ToteutusDAO.get(oid).map {
    case (toteutus, lastModified) =>
      withAuthorizedChildOrganizationOids(readRoles) { authorizedOrganizations =>
        authorize(toteutus.organisaatioOid, authorizedOrganizations) {
          (toteutus, lastModified)
        }
      }
  }

  def put(toteutus: Toteutus)(implicit authenticated: Authenticated): ToteutusOid =
    withAuthorizedChildOrganizationOids(createRoles) { authorizedOrganizations =>
      authorize(toteutus.organisaatioOid, authorizedOrganizations) {
        withValidation(toteutus, putWithIndexing)
      }
    }

  def update(toteutus: Toteutus, notModifiedSince: Instant)(implicit authenticated: Authenticated): Boolean = {
    withAuthorizedChildOrganizationOids(updateRoles) { authorizedOrganizations =>
      val existing = ToteutusDAO.get(toteutus.oid.get).getOrElse(throw new NoSuchElementException("toteutusOid"))._1
      authorize(existing.organisaatioOid, authorizedOrganizations) {
        withValidation(toteutus, updateWithIndexing(_, notModifiedSince))
      }
    }
  }

  def list(organisaatioOid: OrganisaatioOid)(implicit authenticated: Authenticated): Seq[ToteutusListItem] =
    withAuthorizedChildOrganizationOids(organisaatioOid, readRoles) { oids =>
      logger.error(s"ASDF ${oids.mkString(", ")}")
      ToteutusDAO.listByOrganisaatioOids(oids)
    }

  def listHaut(oid: ToteutusOid)(implicit authenticated: Authenticated): Seq[HakuListItem] =
    ToteutusDAO.get(oid).map { case (toteutus, _) =>
      withAuthorizedChildOrganizationOids(readRoles) { oids =>
        authorize(toteutus.organisaatioOid, oids) {
          HakuDAO.listByToteutusOid(oid)
        }
      }
    }.getOrElse(Seq.empty)

  def listHakukohteet(oid: ToteutusOid)(implicit authenticated: Authenticated): Seq[HakukohdeListItem] =
    ToteutusDAO.get(oid).map { case (toteutus, _) =>
      withAuthorizedChildOrganizationOids(readRoles) { oids =>
        authorize(toteutus.organisaatioOid, oids) {
          HakukohdeDAO.listByToteutusOid(oid)
        }
      }
    }.getOrElse(Seq.empty)


  private def putWithIndexing(toteutus: Toteutus) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeToteutus,
      () => ToteutusDAO.getPutActions(toteutus))

  private def updateWithIndexing(toteutus: Toteutus, notModifiedSince: Instant) =
    sqsInTransactionService.runActionAndUpdateIndex(
      HighPriority,
      IndexTypeToteutus,
      () => ToteutusDAO.getUpdateActions(toteutus, notModifiedSince),
      toteutus.oid.get.toString)
}
