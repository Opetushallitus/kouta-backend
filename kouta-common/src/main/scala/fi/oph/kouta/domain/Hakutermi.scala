package fi.oph.kouta.domain

sealed trait Hakutermi extends EnumType

object Hakutermi extends Enum[Hakutermi] {
  override def name: String = "hakutermi"

  override def values: List[Hakutermi] = List(Hakeutuminen, Ilmoittautuminen)
}

case object Hakeutuminen extends Hakutermi { val name = "hakeutuminen"}
case object Ilmoittautuminen extends Hakutermi { val name = "ilmoittautuminen"}
