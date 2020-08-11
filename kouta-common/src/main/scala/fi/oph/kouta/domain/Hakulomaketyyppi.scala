package fi.oph.kouta.domain

sealed trait Hakulomaketyyppi extends EnumType

object Hakulomaketyyppi extends Enum[Hakulomaketyyppi] {
  override def name: String = "hakulomaketyyppi"
  def values = List(Ataru, HakuApp, MuuHakulomake, EiSähköistä)
}

case object Ataru extends Hakulomaketyyppi { val name = "ataru"}
case object HakuApp extends Hakulomaketyyppi { val name = "haku-app"}
case object MuuHakulomake extends Hakulomaketyyppi { val name = "muu"}
case object EiSähköistä extends Hakulomaketyyppi { val name = "ei sähköistä"}
