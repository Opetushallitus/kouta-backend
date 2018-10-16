package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.Haku
import fi.oph.kouta.repository.HakuDAO

object HakuService {
  
  def put(haku:Haku): Option[String] = HakuDAO.put(haku)

  def update(haku:Haku, notModifiedSince:Instant): Boolean = HakuDAO.update(haku, notModifiedSince)

  def get(oid:String): Option[(Haku, Instant)] = HakuDAO.get(oid)
}
