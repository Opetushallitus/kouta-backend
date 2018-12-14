package fi.oph.kouta.service

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.domain.{IdListItem, Valintaperuste}
import fi.oph.kouta.repository.ValintaperusteDAO

object ValintaperusteService extends ValidatingService[Valintaperuste] with AuthorizationService {

  def put(valintaperuste:Valintaperuste): Option[UUID] = withValidation(valintaperuste, (v) => ValintaperusteDAO.put(v.copy(id = Some(UUID.randomUUID()))))

  def update(valintaperuste:Valintaperuste, notModifiedSince:Instant): Boolean = withValidation(valintaperuste, ValintaperusteDAO.update(_, notModifiedSince))

  def get(id:UUID): Option[(Valintaperuste, Instant)] = ValintaperusteDAO.get(id)

  def list(organisaatioOid:String):Seq[IdListItem] =
    withAuthorizedChildAndParentOrganizationOids(organisaatioOid, ValintaperusteDAO.listByOrganisaatioOids)

}
