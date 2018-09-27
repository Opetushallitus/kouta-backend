package fi.oph.kouta.domain

object Julkaisutila extends Enumeration {
  type Julkaisutila = Value
  val julkaisematta, julkaistu, poistettu = Value
}

object Kieli extends Enumeration {
  type Kieli = Value
  val fi, sv, en = Value
}