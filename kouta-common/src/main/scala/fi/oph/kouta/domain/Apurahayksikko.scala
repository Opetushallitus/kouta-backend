package fi.oph.kouta.domain

sealed trait Apurahayksikko extends EnumType

object Apurahayksikko extends Enum[Apurahayksikko] {
  override def name: String = "apurahayksikko"
  val values = List(Euro, Prosentti)
}

case object Euro extends Apurahayksikko { val name = "euro" }
case object Prosentti extends Apurahayksikko { val name = "prosentti" }

