package fi.oph.kouta.domain

package object koulutustyyppiToOppilaitostyyppiResult {
  val KoulutustyyppiToOppilaitostyypitModel: String =
    """    KoulutustyyppiToOppilaitostyypit:
      |      type: object
      |      description: Koulutustyyppiin liitetyt oppilaitostyypit
      |      properties:
      |        koulutustyyppi:
      |          type: string
      |          description: Koulutustyyppi
      |          $ref: '#/components/schemas/Koulutustyyppi'
      |          example: amm
      |        oppilaitostyypit:
      |          type: array
      |          items:
      |            type: string
      |          description: Oppilaitostyypit (koodiUrit)
      |          example:
      |            - oppilaitostyyppi_21#1
      |            - oppilaitostyyppi_22#1
      |""".stripMargin

  val KoulutustyyppiToOppilaitostyyppiResultModel: String =
    """    KoulutustyyppiToOppilaitostyyppiResult:
      |      type: object
      |      description: Jokaisen koulutustyypin osalta oppilaitostyypit, jotka voivat tarjota ko. koulutusta,
      |        ts. mäppäykset koulutustyypistä oppilaitostyyppeihin
      |      properties:
      |        koulutustyypitToOppilaitostyypit:
      |          type: array
      |          items:
      |            $ref: '#/components/schemas/KoulutustyyppiToOppilaitostyypit'
      |""".stripMargin

  def models = List(KoulutustyyppiToOppilaitostyypitModel, KoulutustyyppiToOppilaitostyyppiResultModel)
}

case class KoulutustyyppiToOppilaitostyyppiResult(
    koulutustyypitToOppilaitostyypit: Seq[KoulutustyyppiToOppilaitostyypit]
)

case class KoulutustyyppiToOppilaitostyypit(koulutustyyppi: Koulutustyyppi, oppilaitostyypit: Seq[String])
