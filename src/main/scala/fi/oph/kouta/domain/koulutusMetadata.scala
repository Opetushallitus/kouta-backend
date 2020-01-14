package fi.oph.kouta.domain

import fi.oph.kouta.validation.{IsValid, Validations}

package object koulutusMetadata {

  val KoulutusMetadataModel =
    """    KoulutusMetadata:
      |      type: object
      |      properties:
      |        koulutusalaKoodiUrit:
      |          type: array
      |          description: Lista koulutusaloja. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kansallinenkoulutusluokitus2016koulutusalataso2/1)
      |          items:
      |            type: string
      |            example:
      |              - kansallinenkoulutusluokitus2016koulutusalataso2_054#1
      |              - kansallinenkoulutusluokitus2016koulutusalataso2_055#1
      |        kuvaus:
      |          type: object
      |          description: Koulutuksen kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Kuvaus'
      |        lisatiedot:
      |          type: array
      |          description: Koulutukseen liittyviä lisätietoja, jotka näkyvät oppijalle Opintopolussa
      |          items:
      |            type: object
      |            $ref: '#/components/schemas/Lisatieto'
      |        teemakuva:
      |          type: string
      |          description: Koulutuksen Opintopolussa näytettävän teemakuvan URL.
      |          example: https://konfo-files.opintopolku.fi/koulutus-teema/1.2.246.562.13.00000000000000000009/f4ecc80a-f664-40ef-98e6-eaf8dfa57f6e.png
      |""".stripMargin

  val KorkeakouluMetadataModel =
    """    KorkeakouluMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |      properties:
      |        kuvauksenNimi:
      |          type: object
      |          description: Koulutuksen kuvaukseni nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Nimi'
      |        tutkintonimikeKoodiUrit:
      |          type: array
      |          description: Lista koulutuksen tutkintonimikkeistä. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/tutkintonimikekk/2)
      |          items:
      |            type: string
      |          example:
      |            - tutkintonimikekk_110#2
      |            - tutkintonimikekk_111#2
      |        opintojenLaajuusKoodiUri:
      |          type: string
      |          description: "Tutkinnon laajuus. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuus/1)"
      |          example: opintojenlaajuus_40#1
      |""".stripMargin

  val YliopistoKoulutusMetadataModel =
    """    YliopistoKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KorkeakouluMetadata'
      |        - type: object
      |          properties:
      |            koulutustyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: yo
      |              enum:
      |                - yo
      |""".stripMargin

  val AmmattikorkeaKoulutusMetadataModel =
    """    AmmattikorkeaKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KorkeakouluMetadata'
      |        - type: object
      |          properties:
      |            koulutustyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: amk
      |              enum:
      |                - amk
      |""".stripMargin

  val AmmatillinenKoulutusMetadataModel =
    """    AmmatillinenKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            koulutustyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: amm
      |              enum:
      |                - amm
      |""".stripMargin

  val models = List(KoulutusMetadataModel, AmmatillinenKoulutusMetadataModel, KorkeakouluMetadataModel, AmmattikorkeaKoulutusMetadataModel, YliopistoKoulutusMetadataModel)
}

sealed trait KoulutusMetadata extends Validations {
  val tyyppi: Koulutustyyppi
  val kuvaus: Kielistetty
  val lisatiedot: Seq[Lisatieto]
  val koulutusalaKoodiUrit: Seq[String]

  def validate(koulutustyyppi: Koulutustyyppi, tila: Julkaisutila, kielivalinta: Seq[Kieli]): IsValid = and(
    assertTrue(tyyppi == koulutustyyppi, InvalidMetadataTyyppi),
    validateIfJulkaistu(tila, validateOptionalKielistetty(kielivalinta, kuvaus, "kuvaus")),
    validateIfNonEmpty[Lisatieto](lisatiedot, _.validate(tila, kielivalinta)),
    validateIfNonEmpty[String](koulutusalaKoodiUrit, assertMatch(_, KoulutusalaKoodiPattern)),
  )
}

trait KorkeakoulutusKoulutusMetadata extends KoulutusMetadata {
  val kuvauksenNimi: Kielistetty
  val tutkintonimikeKoodiUrit: Seq[String]
  val opintojenLaajuusKoodiUri: Option[String]

  override def validate(koulutustyyppi: Koulutustyyppi, tila: Julkaisutila, kielivalinta: Seq[Kieli]): IsValid = and(
    super.validate(koulutustyyppi, tila, kielivalinta),
    validateIfJulkaistu(tila, validateKielistetty(kielivalinta, kuvauksenNimi, "kuvauksenNimi")),
    validateIfNonEmpty[String](tutkintonimikeKoodiUrit, assertMatch(_, TutkintonimikeKoodiPattern)),
    validateIfDefined[String](opintojenLaajuusKoodiUri, assertMatch(_, OpintojenLaajuusKoodiPattern))
  )
}

case class AmmatillinenKoulutusMetadata(tyyppi: Koulutustyyppi = Amm,
                                        kuvaus: Kielistetty = Map(),
                                        lisatiedot: Seq[Lisatieto] = Seq(),
                                        koulutusalaKoodiUrit: Seq[String] = Seq()) extends KoulutusMetadata

case class YliopistoKoulutusMetadata(tyyppi: Koulutustyyppi = Yo,
                                     kuvaus: Kielistetty = Map(),
                                     lisatiedot: Seq[Lisatieto] = Seq(),
                                     koulutusalaKoodiUrit: Seq[String] = Seq(),
                                     tutkintonimikeKoodiUrit: Seq[String] = Seq(),
                                     opintojenLaajuusKoodiUri: Option[String] = None,
                                     kuvauksenNimi: Kielistetty = Map()) extends KorkeakoulutusKoulutusMetadata

case class AmmattikorkeakouluKoulutusMetadata(tyyppi: Koulutustyyppi = Amk,
                                              kuvaus: Kielistetty = Map(),
                                              lisatiedot: Seq[Lisatieto] = Seq(),
                                              koulutusalaKoodiUrit: Seq[String] = Seq(),
                                              tutkintonimikeKoodiUrit: Seq[String] = Seq(),
                                              opintojenLaajuusKoodiUri: Option[String] = None,
                                              kuvauksenNimi: Kielistetty = Map()) extends KorkeakoulutusKoulutusMetadata
