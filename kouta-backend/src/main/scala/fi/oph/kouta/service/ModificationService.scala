package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.ListEverything
import fi.oph.kouta.repository.ModificationDAO
import fi.oph.kouta.servlet.Authenticated

object ModificationService extends AuthorizationService {

  def getModifiedSince(modifiedSince: Instant)(implicit authenticated: Authenticated): ListEverything =
    withRootAccess(indexerRoles)(ModificationDAO.listModifiedSince(modifiedSince))

}
