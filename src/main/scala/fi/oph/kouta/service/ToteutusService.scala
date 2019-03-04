package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.oid.{OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.domain._
import fi.oph.kouta.indexing.IndexingService
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, ToteutusDAO}

object ToteutusService extends ValidatingService[Toteutus] with AuthorizationService {

  def put(toteutus: Toteutus): Option[ToteutusOid] = {
    val oid = withValidation(toteutus, ToteutusDAO.put(_))
    IndexingService.index(toteutus.copy(oid = oid))
    oid
  }

  def update(toteutus: Toteutus, notModifiedSince: Instant): Boolean = {
    val updated = withValidation(toteutus, ToteutusDAO.update(_, notModifiedSince))
    if (updated) IndexingService.index(toteutus)
    updated
  }

  def get(oid: ToteutusOid): Option[(Toteutus, Instant)] = ToteutusDAO.get(oid)

  def list(organisaatioOid: OrganisaatioOid): Seq[ToteutusListItem] =
    withAuthorizedChildAndParentOrganizationOids(organisaatioOid, ToteutusDAO.listByOrganisaatioOids)

  def listHaut(oid: ToteutusOid): Seq[HakuListItem] =
    HakuDAO.listByToteutusOid(oid)

  def listHakukohteet(oid: ToteutusOid): Seq[HakukohdeListItem] =
    HakukohdeDAO.listByToteutusOid(oid)
}
