package fi.oph.kouta.domain

import fi.oph.kouta.util.SwaggerModel

package object keyword {

  sealed trait KeywordType extends EnumType
  object KeywordType extends Enum[KeywordType] {
    override def name: String = "Asiasanan tyyppi"
    override def values(): List[KeywordType] = List(Asiasana, Ammattinimike)
  }

  @SwaggerModel(
    """    Asiasana:
      |      type: object
      |      properties:
      |        kieli:
      |          type: string
      |          desciption: Asiasanan kieli
      |          allOf:
      |            - $ref: '#/components/schemas/Kieli'
      |          example: fi
      |        arvo:
      |          type: string
      |          description: Asiasana annetulla kielellä
      |          example: robotiikka
      |""")
  case object Asiasana extends KeywordType { val name = "asiasana" }

  @SwaggerModel(
    """    Ammattinimike:
      |      type: object
      |      properties:
      |        kieli:
      |          type: string
      |          desciption: Ammattinimikkeen kieli
      |          allOf:
      |            - $ref: '#/components/schemas/Kieli'
      |          example: fi
      |        arvo:
      |          type: string
      |          description: Ammattinimike annetulla kielellä
      |          example: insinööri
      |""")
  case object Ammattinimike extends KeywordType { val name = "ammattinimike" }

  case class Keyword(kieli: Kieli, arvo: String)

  case class KeywordSearch(term: String,
                           kieli: Kieli,
                           `type`: KeywordType,
                           limit: Int)
}

