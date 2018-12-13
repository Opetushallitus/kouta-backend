package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.{Haku, OidListItem}
import fi.oph.kouta.repository.HakuDAO

object HakuService extends ValidatingService[Haku] with AuthorizationService {
  
  def put(haku:Haku): Option[String] = withValidation(haku, HakuDAO.put(_))

  def update(haku:Haku, notModifiedSince:Instant): Boolean = withValidation(haku, HakuDAO.update(_, notModifiedSince))

  def get(oid:String): Option[(Haku, Instant)] = HakuDAO.get(oid)

  def list(organisaatioOid:String):Seq[OidListItem] = withAuthorizedOrganizationOids(organisaatioOid, HakuDAO.listByOrganisaatioOids)
}
