package fi.oph.kouta.domain

import fi.oph.kouta.validation.ExternalQueryResults.ExternalQueryResult

import java.util.UUID
import fi.oph.kouta.validation.{IsValid, NoErrors, ValidatableSubEntity, ValidationContext}
import fi.oph.kouta.validation.Validations._

package object valintatapa {

  val ValintatapaModel =
    """    Valintatapa:
      |      type: object
      |      properties:
      |        valintatapaKoodiUri:
      |          type: string
      |          description: Valintatapa. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/valintatapajono/1)
      |          example: valintatapajono_av#1
      |        nimi:
      |          type: object
      |          description: Valintatapakuvauksen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty valintaperusteen kielivalinnassa.
      |          $ref: '#/components/schemas/Nimi'
      |        kuvaus:
      |          type: object
      |          description: Valintatavan kuvausteksti eri kielillä. Kielet on määritetty valintaperusteen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        sisalto:
      |          type: array
      |          description: Valintatavan sisältö. Voi sisältää sekä teksti- että taulukkoelementtejä.
      |          items:
      |            type: object
      |            $ref: '#/components/schemas/SisaltoItem'
      |        kaytaMuuntotaulukkoa:
      |          type: boolean
      |          description: "Käytetäänkö muuntotaulukkoa?"
      |        kynnysehto:
      |          type: object
      |          description: Kynnysehdon kuvausteksti eri kielillä. Kielet on määritetty valintaperusteen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        enimmaispisteet:
      |          type: number
      |          format: double
      |          minimum: 0
      |          description: Valintatavan enimmäispisteet
      |          example: 20.0
      |        vahimmaispisteet:
      |          type: number
      |          format: double
      |          minimum: 0
      |          description: Valintatavan vähimmäispisteet
      |          example: 10.0
      |""".stripMargin

  val SisaltoTekstiModel =
    """    SisaltoTeksti:
      |      type: object
      |      description: Valintatavan Opintopolussa näytettävä kuvausteksti eri kielillä. Kielet on määritetty valintaperusteen kielivalinnassa.
      |      properties:
      |        tyyppi:
      |          type: string
      |          enum:
      |            - teksti
      |        data:
      |          type: object
      |          $ref: '#/components/schemas/Teksti'
      |      required:
      |        - tyyppi
      |""".stripMargin

  val SisaltoTaulukkoModel =
    """    SisaltoTaulukko:
      |      type: object
      |      description: Taulukkomuotoinen valintatavan sisällön kuvaus
      |      properties:
      |        tyyppi:
      |          type: string
      |          enum:
      |            - taulukko
      |        data:
      |          type: object
      |          properties:
      |            id:
      |              type: string
      |              description: Taulukon yksilöivä tunnus
      |              example: "ea596a9c-5940-497e-b5b7-aded3a2352a7"
      |            nimi:
      |              type: object
      |              description: Taulukon Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty valintaperusteen kielivalinnassa.
      |              $ref: '#/components/schemas/Nimi'
      |            rows:
      |              type: array
      |              description: Taulukon rivit
      |              items:
      |                type: object
      |                properties:
      |                  index:
      |                    type: integer
      |                    description: Rivin järjestysnumero
      |                  isHeader:
      |                    type: boolean
      |                    description: Onko rivi otsikkorivi
      |                  columns:
      |                    type: array
      |                    description: Rivin sarakkeet
      |                    items:
      |                      type: object
      |                      properties:
      |                        index:
      |                          type: integer
      |                          description: Sarakkeen järjestysnumero
      |                        text:
      |                          type: object
      |                          description: Sarakkeen Opintopolussa näytettävä teksti eri kielillä. Kielet on määritetty valintaperusteen kielivalinnassa.
      |                          $ref: '#/components/schemas/Teksti'
      |      required:
      |        - tyyppi
      |""".stripMargin

  val SisaltoModel =
    """    SisaltoItem:
      |      type: object
      |      oneOf:
      |        - $ref: '#/components/schemas/SisaltoTeksti'
      |        - $ref: '#/components/schemas/SisaltoTaulukko'
      |""".stripMargin

  def models = List(ValintatapaModel, SisaltoTekstiModel, SisaltoTaulukkoModel, SisaltoModel)
}

case class Valintatapa(
    nimi: Kielistetty = Map(),
    valintatapaKoodiUri: Option[String] = None,
    sisalto: Seq[Sisalto],
    kaytaMuuntotaulukkoa: Boolean = false,
    kynnysehto: Kielistetty = Map(),
    enimmaispisteet: Option[Double] = None,
    vahimmaispisteet: Option[Double] = None
) {
  def validate(
      path: String,
      entityWithNewValues: Option[Valintatapa],
      vCtx: ValidationContext,
      koodistoCheckFunc: String => ExternalQueryResult
  ): IsValid = and(
    validateKielistetty(vCtx.kielivalinta, nimi, s"$path.nimi"),
    validateIfDefined[String](
      entityWithNewValues.flatMap(_.valintatapaKoodiUri),
      koodiUri =>
        assertKoodistoQueryResult(
          koodiUri,
          koodistoCheckFunc,
          s"$path.valintatapaKoodiUri",
          vCtx,
          invalidValintatapaKoodiUri(koodiUri)
        )
    ),
    validateIfNonEmpty[Sisalto](sisalto, s"$path.sisalto", _.validate(vCtx, _)),
    validateIfDefined[Double](enimmaispisteet, assertNotNegative(_, s"$path.enimmaispisteet")),
    validateIfDefined[Double](vahimmaispisteet, assertNotNegative(_, s"$path.vahimmaispisteet")),
    validateIfJulkaistu(
      vCtx.tila,
      and(
        assertNotOptional(valintatapaKoodiUri, s"$path.valintatapaKoodiUri"),
        validateOptionalKielistetty(vCtx.kielivalinta, kynnysehto, s"$path.kynnysehto"),
        validateMinMax(vahimmaispisteet, enimmaispisteet, s"$path.vahimmaispisteet")
      )
    )
  )
}

sealed trait Sisalto extends ValidatableSubEntity

case class SisaltoTeksti(teksti: Kielistetty) extends Sisalto {
  def validate(vCtx: ValidationContext, path: String): IsValid =
    validateIfJulkaistu(vCtx.tila, validateKielistetty(vCtx.kielivalinta, teksti, s"$path.teksti"))
}

case class Taulukko(id: Option[UUID], nimi: Kielistetty = Map(), rows: Seq[Row] = Seq()) extends Sisalto {
  def validate(vCtx: ValidationContext, path: String): IsValid = and(
    validateIfJulkaistu(vCtx.tila, validateOptionalKielistetty(vCtx.kielivalinta, nimi, s"$path.nimi")),
    validateIfNonEmpty[Row](rows, s"$path.rows", _.validate(vCtx, _))
  )
}

case class Row(index: Int, isHeader: Boolean = false, columns: Seq[Column] = Seq()) extends ValidatableSubEntity {
  def validate(vCtx: ValidationContext, path: String): IsValid = and(
    assertNotNegative(index, s"$path.index"),
    validateIfNonEmpty[Column](columns, s"$path.columns", _.validate(vCtx, _))
  )
}

case class Column(index: Int, text: Kielistetty = Map()) {
  def validate(vCtx: ValidationContext, path: String): IsValid = and(
    assertNotNegative(index, s"$path.index"),
    validateOptionalKielistetty(vCtx.kielivalinta, text, s"$path.text")
  )
}
