package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.Hakukohde
import fi.oph.kouta.repository.HakukohdeDAO

object HakukohdeService {

  def put(haku:Hakukohde): Option[String] = HakukohdeDAO.put(haku)

  def update(haku:Hakukohde, notModifiedSince:Instant): Boolean = HakukohdeDAO.update(haku, notModifiedSince)

  def get(oid:String): Option[(Hakukohde, Instant)] = HakukohdeDAO.get(oid)
}
