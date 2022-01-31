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
      |          enum:
      |            - amm
      |            - yo
      |            - amk
      |            - lk
      |            - amm-tutkinnon-osa
      |            - amm-osaamisala
      |            - amm-muu
      |            - tuva
      |            - telma
      |            - vapaa-sivistystyo-opistovuosi
      |            - vapaa-sivistystyo-muu
      |            - aikuisten-perusopetus
      |          example: amm
      |        oppilaitostyypit:
      |          type: array
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
