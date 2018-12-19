package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.ListEverything
import fi.oph.kouta.repository.ModificationDAO

object ModificationService {

  def getModifiedSince(modifiedSince: Instant): ListEverything = ModificationDAO.listModifiedSince(modifiedSince)

}
