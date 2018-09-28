package fi.oph.kouta.domain

object Julkaisutila extends Enumeration {
  type Julkaisutila = Value
  val tallennettu, julkaistu, arkistoitu = Value
}

object Kieli extends Enumeration {
  type Kieli = Value
  val fi, sv, en = Value
}