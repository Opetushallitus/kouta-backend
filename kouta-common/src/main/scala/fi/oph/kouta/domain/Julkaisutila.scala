package fi.oph.kouta.domain

sealed trait Julkaisutila extends EnumType

object Julkaisutila extends Enum[Julkaisutila] {
  override def name: String = "julkaisutila"
  val values = List(Tallennettu, Julkaistu, Arkistoitu, Poistettu)
}

case object Tallennettu extends Julkaisutila { val name = "tallennettu" }
case object Julkaistu extends Julkaisutila { val name = "julkaistu" }
case object Arkistoitu extends Julkaisutila { val name = "arkistoitu" }
case object Poistettu extends Julkaisutila { val name = "poistettu" }
