package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.Toteutus
import fi.oph.kouta.repository.ToteutusDAO

object ToteutusService {

  def put(toteutus:Toteutus): Option[String] = ToteutusDAO.put(toteutus)

  def update(toteutus:Toteutus, notModifiedSince:Instant): Boolean = ToteutusDAO.update(toteutus, notModifiedSince)

  def get(oid:String): Option[(Toteutus, Instant)] = ToteutusDAO.get(oid)
}
