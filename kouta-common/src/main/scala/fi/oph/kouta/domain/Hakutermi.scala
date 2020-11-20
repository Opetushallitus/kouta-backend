package fi.oph.kouta.domain

sealed trait Hakutermi extends EnumType

object Hakutermi extends Enum[Hakutermi] {
  override def name: String = "hakutermi"

  override def values: List[Hakutermi] = List(Hakeutuminen, Ilmoittautuminen)

  val SwaggerModel: String =
    """    Hakutermi:
      |      type: string
      |      enum:
      |        - hakeutuminen
      |        - ilmoittautuminen
      |""".stripMargin
}

case object Hakeutuminen extends Hakutermi { val name = "hakeutuminen"}
case object Ilmoittautuminen extends Hakutermi { val name = "ilmoittautuminen"}
