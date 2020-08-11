package fi.oph.kouta.domain

import fi.oph.kouta.domain.oid.UserOid

trait HasMuokkaaja[T] {
  def muokkaaja: UserOid
  def withMuokkaaja(muokkaaja: UserOid): T
}
