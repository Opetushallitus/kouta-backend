package fi.oph.kouta.domain

import java.util.UUID

import fi.oph.kouta.validation.{IsValid, Validatable}

package object valintatapa {

  val ValintatapaModel =
    s"""    Valintatapa:
       |      type: object
       |      properties:
       |        valintatapaKoodiUri:
       |          type: string
       |          description: Valintatapa. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/valintatapajono/1)
       |          example: valintatapajono_av#1
       |        kuvaus:
       |          type: object
       |          description: Valintatavan kuvausteksti eri kielillä. Kielet on määritetty valintaperusteen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Kuvaus'
       |        sisalto:
       |          type: object
       |          description: Valintatavan sisältö. TODO!
       |        kaytaMuuntotaulukkoa:
       |          type: boolean
       |          description: "Käytetäänkö muuntotaulukkoa?"
       |        kynnysehto:
       |          type: object
       |          description: Kynnysehdon kuvausteksti eri kielillä. Kielet on määritetty valintaperusteen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Kuvaus'
       |        enimmaispisteet:
       |          type: double
       |          description: Valintatavan enimmäispisteet
       |          example: 20.0
       |        vahimmaispisteet:
       |          type: double
       |          description: Valintatavan vähimmäispisteet
       |          example: 10.0
       |""".stripMargin

  val AmmatillinenValintatapaModel =
    s"""    AmmatillinenValintatapa:
       |      type: object
       |      description: Ammatillisen koulutuksen valintatapakuvaus
       |      allOf:
       |        - $$ref: '#/components/schemas/Valintatapa'
       |""".stripMargin

  val KorkeakoulutusValintatapaModel =
    s"""    KorkeakoulutusValintatapa:
       |      type: object
       |      description: Korkeakoulutuksen valintatapakuvaus
       |      allOf:
       |        - $$ref: '#/components/schemas/Valintatapa'
       |      properties:
       |        nimi:
       |          type: object
       |          description: Valintatapakuvauksen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Nimi'
       |""".stripMargin

  val AmmattikorkeakouluValintatapaModel =
    s"""    AmmattikorkeakouluValintatapa:
       |      type: object
       |      description: Ammattikorkeakoulutuksen valintatapakuvaus
       |      allOf:
       |        - $$ref: '#/components/schemas/KorkeakoulutusValintatapa'
       |""".stripMargin

  val YliopistoValintatapaModel =
    s"""    YliopistoValintatapa:
       |      type: object
       |      description: Yliopistokoulutuksen valintatapakuvaus
       |      allOf:
       |        - $$ref: '#/components/schemas/KorkeakoulutusValintatapa'
       |""".stripMargin

  def models = List(YliopistoValintatapaModel, KorkeakoulutusValintatapaModel, AmmatillinenValintatapaModel,
    AmmattikorkeakouluValintatapaModel, ValintatapaModel)
}

sealed trait Valintatapa extends Validatable {
  def valintatapaKoodiUri: Option[String]
  def kuvaus: Kielistetty
  def sisalto: Seq[ValintatapaSisalto]
  def kaytaMuuntotaulukkoa: Boolean
  def kynnysehto: Kielistetty
  def enimmaispisteet: Option[Double]
  def vahimmaispisteet: Option[Double]

  override def validate(): IsValid = and(
    validateIfDefined[String](valintatapaKoodiUri, assertMatch(_, ValintatapajonoKoodiPattern))
  )
}

case class AmmatillinenValintatapa(valintatapaKoodiUri: Option[String] = None,
                                   kuvaus: Kielistetty = Map(),
                                   sisalto: Seq[ValintatapaSisalto],
                                   kaytaMuuntotaulukkoa: Boolean = false,
                                   kynnysehto: Kielistetty = Map(),
                                   enimmaispisteet: Option[Double] = None,
                                   vahimmaispisteet: Option[Double] = None) extends Valintatapa

sealed trait KorkeakoulutusValintatapa extends Valintatapa {
  def nimi: Kielistetty
}

case class AmmattikorkeakouluValintatapa(nimi: Kielistetty = Map(),
                                         valintatapaKoodiUri: Option[String] = None,
                                         kuvaus: Kielistetty = Map(),
                                         sisalto: Seq[ValintatapaSisalto],
                                         kaytaMuuntotaulukkoa: Boolean = false,
                                         kynnysehto: Kielistetty = Map(),
                                         enimmaispisteet: Option[Double] = None,
                                         vahimmaispisteet: Option[Double] = None) extends KorkeakoulutusValintatapa

case class YliopistoValintatapa(nimi: Kielistetty = Map(),
                                valintatapaKoodiUri: Option[String] = None,
                                kuvaus: Kielistetty = Map(),
                                sisalto: Seq[ValintatapaSisalto],
                                kaytaMuuntotaulukkoa: Boolean = false,
                                kynnysehto: Kielistetty = Map(),
                                enimmaispisteet: Option[Double] = None,
                                vahimmaispisteet: Option[Double] = None) extends KorkeakoulutusValintatapa

sealed trait ValintatapaSisalto

case class Taulukko(id: Option[UUID],
                    nimi: Kielistetty = Map(),
                    rows: Seq[Row] = Seq()) extends ValintatapaSisalto

case class ValintatapaSisaltoTeksti(teksti: Kielistetty) extends ValintatapaSisalto

case class Row(index: Int,
               isHeader: Boolean = false,
               columns: Seq[Column] = Seq())

case class Column(index: Int,
                  text: Kielistetty = Map())
