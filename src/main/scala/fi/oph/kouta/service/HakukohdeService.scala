package fi.oph.kouta.service

import java.time.Instant
import java.util.UUID

import fi.oph.kouta.domain.Hakukohde
import fi.oph.kouta.repository.HakukohdeDAO

object HakukohdeService extends ValidatingService[Hakukohde] {

  def put(hakukohde:Hakukohde): Option[String] = withValidation(hakukohde, HakukohdeDAO.put(_))

  def update(hakukohde:Hakukohde, notModifiedSince:Instant): Boolean = withValidation(hakukohde, HakukohdeDAO.update(_, notModifiedSince))

  def get(oid:String): Option[(Hakukohde, Instant)] = HakukohdeDAO.get(oid)
}
