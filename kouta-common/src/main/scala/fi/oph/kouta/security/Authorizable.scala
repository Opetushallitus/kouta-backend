package fi.oph.kouta.security

import fi.oph.kouta.domain.{HasMuokkaaja, Koulutustyyppi}
import fi.oph.kouta.domain.oid.OrganisaatioOid

trait Authorizable {
  val organisaatioOid: OrganisaatioOid
}

trait AuthorizableEntity[T] extends Authorizable with HasMuokkaaja[T]

trait AuthorizableMaybeJulkinen[T] extends AuthorizableEntity[T] {
  val koulutustyyppi: Koulutustyyppi
  val julkinen: Boolean
}
