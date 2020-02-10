package fi.oph.kouta.domain

import java.util.UUID

import fi.oph.kouta.validation.{IsValid, NoErrors, ValidatableSubEntity}
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
      |          allOf:
      |            - $ref: '#/components/schemas/Nimi'
      |        kuvaus:
      |          type: object
      |          description: Valintatavan kuvausteksti eri kielillä. Kielet on määritetty valintaperusteen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Kuvaus'
      |        sisalto:
      |          type: array
      |          description: Valintatavan sisältö. Voi sisältää sekä teksti- että taulukkoelementtejä.
      |          items:
      |            type: object
      |            oneOf:
      |              - $ref: '#/components/schemas/ValintatapaSisaltoTeksti'
      |              - $ref: '#/components/schemas/ValintatapaSisaltoTaulukko'
      |        kaytaMuuntotaulukkoa:
      |          type: boolean
      |          description: "Käytetäänkö muuntotaulukkoa?"
      |        kynnysehto:
      |          type: object
      |          description: Kynnysehdon kuvausteksti eri kielillä. Kielet on määritetty valintaperusteen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Kuvaus'
      |        enimmaispisteet:
      |          type: double
      |          description: Valintatavan enimmäispisteet
      |          example: 20.0
      |        vahimmaispisteet:
      |          type: double
      |          description: Valintatavan vähimmäispisteet
      |          example: 10.0
      |""".stripMargin

  val ValintatapaSisaltoTekstiModel =
    """    ValintatapaSisaltoTeksti:
      |      type: object
      |      description: Tekstimuotoinen valintatavan sisällön kuvaus
      |      properties:
      |        teksti:
      |          type: object
      |          description: Valintatavan Opintopolussa näytettävä kuvausteksti eri kielillä. Kielet on määritetty valintaperusteen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Teksti'
      |""".stripMargin

  val ValintatapaSisaltoTaulukkoModel =
    """    ValintatapaSisaltoTaulukko:
      |      type: object
      |      description: Taulukkomuotoinen valintatavan sisällön kuvaus
      |      properties:
      |        id:
      |          type: string
      |          description: Taulukon yksilöivä tunnus
      |          example: "ea596a9c-5940-497e-b5b7-aded3a2352a7"
      |        nimi:
      |          type: object
      |          description: Taulukon Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty valintaperusteen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Nimi'
      |        rows:
      |          type: array
      |          description: Taukon rivit
      |          items:
      |            type: object
      |            properties:
      |              index:
      |                type: integer
      |                description: Rivin järjestysnumero
      |              isHeader:
      |                type: boolean
      |                description: Onko rivi otsikkorivi
      |              columns:
      |                type: array
      |                description: Rivin sarakkeet
      |                items:
      |                  type: object
      |                  properties:
      |                    index:
      |                      type: integer
      |                      description: Sarakkeen järjestysnumero
      |                    text:
      |                      type: object
      |                      description: Sarakkeen Opintopolussa näytettävä teksti eri kielillä.
      |                        Kielet on määritetty valintaperusteen kielivalinnassa.
      |                      allOf:
      |                        - $ref: '#/components/schemas/Teksti'
      |""".stripMargin

  def models = List(ValintatapaModel, ValintatapaSisaltoTekstiModel, ValintatapaSisaltoTaulukkoModel)
}

case class Valintatapa(nimi: Kielistetty = Map(),
                       valintatapaKoodiUri: Option[String] = None,
                       kuvaus: Kielistetty = Map(),
                       sisalto: Seq[ValintatapaSisalto],
                       kaytaMuuntotaulukkoa: Boolean = false,
                       kynnysehto: Kielistetty = Map(),
                       enimmaispisteet: Option[Double] = None,
                       vahimmaispisteet: Option[Double] = None) extends ValidatableSubEntity {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli]): IsValid = and(
    validateKielistetty(kielivalinta, nimi, "nimi"),
    validateIfDefined[String](valintatapaKoodiUri, assertMatch(_, ValintatapajonoKoodiPattern)),
    validateIfNonEmpty[ValintatapaSisalto](sisalto, _.validate(tila, kielivalinta)),
    validateIfDefined[Double](enimmaispisteet, assertNotNegative(_, "enimmaispisteet")),
    validateIfDefined[Double](vahimmaispisteet, assertNotNegative(_, "vahimmaispisteet")),
    validateIfJulkaistu(tila, and(
      assertNotOptional(valintatapaKoodiUri, "valintatapaKoodiUri"),
      validateOptionalKielistetty(kielivalinta, kuvaus, "kuvaus"),
      validateOptionalKielistetty(kielivalinta, kynnysehto, "kynnysehto"),
      validateMinMax(vahimmaispisteet, enimmaispisteet, "vahimmaispisteet", "enimmaispisteet")
    ))
  )

}

sealed trait ValintatapaSisalto extends ValidatableSubEntity {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli]): IsValid
}

case class ValintatapaSisaltoTeksti(teksti: Kielistetty) extends ValintatapaSisalto {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli]): IsValid =
    validateIfJulkaistu(tila,
      validateKielistetty(kielivalinta, teksti, "teksti")
    )
}

case class Taulukko(id: Option[UUID],
                    nimi: Kielistetty = Map(),
                    rows: Seq[Row] = Seq()) extends ValintatapaSisalto {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli]): IsValid = and(
    validateIfJulkaistu(tila, validateOptionalKielistetty(kielivalinta, nimi, "nimi")),
    validateIfNonEmpty[Row](rows, _.validate(tila, kielivalinta))
  )
}

case class Row(index: Int,
               isHeader: Boolean = false,
               columns: Seq[Column] = Seq()) extends ValidatableSubEntity {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli]): IsValid = and(
    assertNotNegative(index, s"row $index"),
    validateIfNonEmpty[Column](columns, _.validate(tila, kielivalinta, index))
  )
}

case class Column(index: Int,
                  text: Kielistetty = Map()) {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], row: Int): IsValid = and(
    assertNotNegative(index, s"row $row column $index"),
    validateOptionalKielistetty(kielivalinta, text, s"row $row column $index text")
  )

}
