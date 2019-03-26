package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.oid.{OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.domain._
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, ToteutusDAO}

object ToteutusService extends ValidatingService[Toteutus] with AuthorizationService {

  def put(toteutus: Toteutus): Option[ToteutusOid] =
    withValidation(toteutus, ToteutusDAO.put)

  def update(toteutus: Toteutus, notModifiedSince: Instant): Boolean =
    withValidation(toteutus, ToteutusDAO.update(_, notModifiedSince))

  def get(oid: ToteutusOid): Option[(Toteutus, Instant)] = ToteutusDAO.get(oid)

  def list(organisaatioOid: OrganisaatioOid): Seq[ToteutusListItem] =
    withAuthorizedChildAndParentOrganizationOids(organisaatioOid, ToteutusDAO.listByOrganisaatioOids)

  def listHaut(oid: ToteutusOid): Seq[HakuListItem] =
    HakuDAO.listByToteutusOid(oid)

  def listHakukohteet(oid: ToteutusOid): Seq[HakukohdeListItem] =
    HakukohdeDAO.listByToteutusOid(oid)
}
