package fi.oph.kouta.service

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.domain.Valintaperuste
import fi.oph.kouta.repository.ValintaperusteDAO

object ValintaperusteService {

  def put(valintaperuste:Valintaperuste): Option[UUID] = ValintaperusteDAO.put(valintaperuste.copy(id = Some(UUID.randomUUID())))

  def update(valintaperuste:Valintaperuste, notModifiedSince:Instant): Boolean = ValintaperusteDAO.update(valintaperuste, notModifiedSince)

  def get(id:UUID): Option[(Valintaperuste, Instant)] = ValintaperusteDAO.get(id)

}
