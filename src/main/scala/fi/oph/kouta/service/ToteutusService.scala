package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.oid.{OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.domain._
import fi.oph.kouta.indexing.IndexingService
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, ToteutusDAO}
import fi.oph.kouta.util.WithSideEffect

object ToteutusService extends ValidatingService[Toteutus] with AuthorizationService with WithSideEffect {

  def put(toteutus: Toteutus): Option[ToteutusOid] = {
    withSideEffect(withValidation(toteutus, ToteutusDAO.put)) {
      case oid => IndexingService.index(toteutus.copy(oid = oid))
    }
  }

  def update(toteutus: Toteutus, notModifiedSince: Instant): Boolean = {
    withSideEffect(withValidation(toteutus, ToteutusDAO.update(_, notModifiedSince))) {
      case true => IndexingService.index(toteutus)
    }
  }

  def get(oid: ToteutusOid): Option[(Toteutus, Instant)] = ToteutusDAO.get(oid)

  def list(organisaatioOid: OrganisaatioOid): Seq[ToteutusListItem] =
    withAuthorizedChildAndParentOrganizationOids(organisaatioOid, ToteutusDAO.listByOrganisaatioOids)

  def listHaut(oid: ToteutusOid): Seq[HakuListItem] =
    HakuDAO.listByToteutusOid(oid)

  def listHakukohteet(oid: ToteutusOid): Seq[HakukohdeListItem] =
    HakukohdeDAO.listByToteutusOid(oid)
}
