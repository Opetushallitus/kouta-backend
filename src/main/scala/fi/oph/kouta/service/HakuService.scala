package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.Haku
import fi.oph.kouta.repository.HakuDAO

object HakuService extends ValidatingService[Haku] {
  
  def put(haku:Haku): Option[String] = withValidation(haku, HakuDAO.put(_))

  def update(haku:Haku, notModifiedSince:Instant): Boolean = withValidation(haku, HakuDAO.update(_, notModifiedSince))

  def get(oid:String): Option[(Haku, Instant)] = HakuDAO.get(oid)
}
