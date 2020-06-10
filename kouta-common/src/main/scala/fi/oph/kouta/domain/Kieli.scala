package fi.oph.kouta.domain

import fi.oph.kouta.swagger.SwaggerModel

@SwaggerModel(
  """    Kieli:
    |      type: string
    |      enum:
    |        - fi
    |        - sv
    |        - en
    |""")
sealed trait Kieli extends EnumType

object Kieli extends Enum[Kieli] {
  override def name: String = "kieli"
  def values = List(Fi, Sv, En)
}
case object Fi extends Kieli { val name = "fi" }
case object Sv extends Kieli { val name = "sv" }
case object En extends Kieli { val name = "en" }
