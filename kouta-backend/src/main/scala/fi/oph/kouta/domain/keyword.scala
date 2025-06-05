package fi.oph.kouta.domain

package object keyword {

  val AsiasanaModel: String =
    """    Asiasana:
      |      type: object
      |      properties:
      |        kieli:
      |          type: string
      |          description: Asiasanan kieli
      |          allOf:
      |            - $ref: '#/components/schemas/Kieli'
      |          example: fi
      |        arvo:
      |          type: string
      |          description: Asiasana annetulla kielellä
      |          example: robotiikka
      |""".stripMargin

  val AmmattinimikeModel: String =
    """    Ammattinimike:
      |      type: object
      |      properties:
      |        kieli:
      |          type: string
      |          description: Ammattinimikkeen kieli
      |          allOf:
      |            - $ref: '#/components/schemas/Kieli'
      |          example: fi
      |        arvo:
      |          type: string
      |          description: Ammattinimike annetulla kielellä
      |          example: insinööri
      |""".stripMargin

  val LuokittelutermiModel: String =
    """    Luokittelutermi:
      |      type: array
      |      items:
      |        type: string
      |        example:
      |          - ohjaajakoulutus
      |          - jod-ohjaaja
      |""".stripMargin

  val models = List(AsiasanaModel, AmmattinimikeModel, LuokittelutermiModel)

  sealed trait KeywordType extends EnumType
  object KeywordType extends Enum[KeywordType] {
    override def name: String              = "Asiasanan tyyppi"
    override val values: List[KeywordType] = List(Asiasana, Ammattinimike, Luokittelutermi)
  }

  case object Asiasana extends KeywordType { val name = "asiasana" }

  case object Luokittelutermi extends KeywordType { val name = "luokittelutermi" }

  case object Ammattinimike extends KeywordType { val name = "ammattinimike" }

  case class Keyword(kieli: Kieli, arvo: String)

  sealed trait KeywordSearchBase {
    val term: String
    val limit: Int
    val `type`: KeywordType
  }

  case class KeywordSearch(term: String, kieli: Kieli, `type`: KeywordType, limit: Int) extends KeywordSearchBase

  case class LuokittelutermiSearch(term: String, limit: Int, `type`: KeywordType) extends KeywordSearchBase
}
