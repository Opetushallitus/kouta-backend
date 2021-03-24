package fi.oph.kouta.domain

sealed trait Maksullisuustyyppi extends EnumType

object Maksullisuustyyppi extends Enum[Maksullisuustyyppi] {
  override def name: String = "maksullisuustyyppi"
  val values = List(Maksullinen, Maksuton, Lukuvuosimaksu)
}

case object Maksullinen extends Maksullisuustyyppi { val name = "maksullinen" }
case object Maksuton extends Maksullisuustyyppi { val name = "maksuton" }
case object Lukuvuosimaksu extends Maksullisuustyyppi { val name = "lukuvuosimaksu" }