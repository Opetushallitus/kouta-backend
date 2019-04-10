package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.oid.{OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.domain._
import fi.oph.kouta.indexing.SqsInTransactionService.runActionAndUpdateIndex
import fi.oph.kouta.indexing.indexing.{HighPriority, IndexTypeToteutus}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, ToteutusDAO}

object ToteutusService extends ValidatingService[Toteutus] with AuthorizationService {

  def put(toteutus: Toteutus): ToteutusOid =
    withValidation(toteutus, putWithIndexing)

  def update(toteutus: Toteutus, notModifiedSince: Instant): Boolean =
    withValidation(toteutus, updateWithIndexing(_, notModifiedSince))

  def get(oid: ToteutusOid): Option[(Toteutus, Instant)] = ToteutusDAO.get(oid)

  def list(organisaatioOid: OrganisaatioOid): Seq[ToteutusListItem] =
    withAuthorizedChildAndParentOrganizationOids(organisaatioOid, ToteutusDAO.listByOrganisaatioOids)

  def listHaut(oid: ToteutusOid): Seq[HakuListItem] =
    HakuDAO.listByToteutusOid(oid)

  def listHakukohteet(oid: ToteutusOid): Seq[HakukohdeListItem] =
    HakukohdeDAO.listByToteutusOid(oid)

  private def putWithIndexing(toteutus: Toteutus) =
    runActionAndUpdateIndex(
      HighPriority,
      IndexTypeToteutus,
      () => ToteutusDAO.getPutActions(toteutus))

  private def updateWithIndexing(toteutus: Toteutus, notModifiedSince: Instant) =
    runActionAndUpdateIndex(
      HighPriority,
      IndexTypeToteutus,
      () => ToteutusDAO.getUpdateActions(toteutus, notModifiedSince), toteutus.oid.get.toString)
}
