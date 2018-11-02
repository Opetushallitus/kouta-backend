package fi.oph.kouta.service

import java.time.Instant

import fi.oph.kouta.domain.Toteutus
import fi.oph.kouta.repository.ToteutusDAO

object ToteutusService extends ValidatingService[Toteutus] {

  def put(toteutus:Toteutus): Option[String] = withValidation(toteutus, ToteutusDAO.put(_))

  def update(toteutus:Toteutus, notModifiedSince:Instant): Boolean = withValidation(toteutus, ToteutusDAO.update(_, notModifiedSince))

  def get(oid:String): Option[(Toteutus, Instant)] = ToteutusDAO.get(oid)
}
