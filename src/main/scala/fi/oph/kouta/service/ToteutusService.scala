package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.{OidListItem, Toteutus}
import fi.oph.kouta.repository.{HakuDAO, ToteutusDAO}

object ToteutusService extends ValidatingService[Toteutus] with AuthorizationService {

  def put(toteutus:Toteutus): Option[String] = withValidation(toteutus, ToteutusDAO.put(_))

  def update(toteutus:Toteutus, notModifiedSince:Instant): Boolean = withValidation(toteutus, ToteutusDAO.update(_, notModifiedSince))

  def get(oid:String): Option[(Toteutus, Instant)] = ToteutusDAO.get(oid)

  def list(organisaatioOid:String):Seq[OidListItem] =
    withAuthorizedChildAndParentOrganizationOids(organisaatioOid, ToteutusDAO.listByOrganisaatioOids)

  def listHaut(oid:String):Seq[OidListItem] =
    HakuDAO.listByToteutusOid(oid)
}
