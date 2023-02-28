package fi.oph.kouta.domain

sealed trait Julkaisutila extends EnumType

object Julkaisutila extends Enum[Julkaisutila] {
  override def name: String = "julkaisutila"
  val values = List(Tallennettu, Julkaistu, Arkistoitu, Poistettu)

  def isTilaUpdateAllowedOnlyForOph(oldTila: Julkaisutila, newTila: Julkaisutila): Boolean = {
    (oldTila, newTila) match {
      case (oldTila, newTila) if oldTila == Julkaistu && newTila == Tallennettu => true
      case (_, _)                                                               => false
    }
  }

  def getDisplauName(julkaisutila: Julkaisutila): String = julkaisutila match {
    case Tallennettu => "Luonnos"
    case Julkaistu => "Julkaistu"
    case Arkistoitu => "Arkistoitu"
    case Poistettu => "Poistettu"
  }
}

case object Tallennettu extends Julkaisutila { val name = "tallennettu" }
case object Julkaistu extends Julkaisutila { val name = "julkaistu" }
case object Arkistoitu extends Julkaisutila { val name = "arkistoitu" }
case object Poistettu extends Julkaisutila { val name = "poistettu" }
