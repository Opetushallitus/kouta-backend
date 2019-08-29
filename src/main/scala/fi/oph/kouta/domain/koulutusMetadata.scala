package fi.oph.kouta.domain

package object koulutusMetadata {

  val KoulutusMetadataModel =
    s"""    KoulutusMetadata:
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
       |            - $$ref: '#/components/schemas/Kuvaus'
       |        lisatiedot:
       |          type: array
       |          description: Koulutukseen liittyviä lisätietoja, jotka näkyvät oppijalle Opintopolussa
       |          items:
       |            type: object
       |            $$ref: '#/components/schemas/Lisatieto'
       |""".stripMargin

  val KorkeakouluMetadataModel =
    s"""    KorkeakouluMetadata:
       |      allOf:
       |        - $$ref: '#/components/schemas/KoulutusMetadata'
       |      properties:
       |        kuvauksenNimi:
       |          type: object
       |          description: Koulutuksen kuvaukseni nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Nimi'
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
    s"""    YliopistoKoulutusMetadata:
       |      allOf:
       |        - $$ref: '#/components/schemas/KorkeakouluMetadata'
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
    s"""    AmmattikorkeaKoulutusMetadata:
       |      allOf:
       |        - $$ref: '#/components/schemas/KorkeakouluMetadata'
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
    s"""    AmmatillinenKoulutusMetadata:
       |      allOf:
       |        - $$ref: '#/components/schemas/KoulutusMetadata'
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

sealed trait KoulutusMetadata {
  val tyyppi: Koulutustyyppi
  val kuvaus: Map[Kieli, String]
  val lisatiedot: Seq[Lisatieto]
  val koulutusalaKoodiUrit: Seq[String]
}

trait KorkeakoulutusKoulutusMetadata extends KoulutusMetadata {
  val kuvauksenNimi: Map[Kieli, String]
  val tutkintonimikeKoodiUrit: Seq[String]
  val opintojenLaajuusKoodiUri: Option[String]
}

case class AmmatillinenKoulutusMetadata(tyyppi: Koulutustyyppi = Amm,
                                        kuvaus: Map[Kieli, String] = Map(),
                                        lisatiedot: Seq[Lisatieto] = Seq(),
                                        koulutusalaKoodiUrit: Seq[String] = Seq()) extends KoulutusMetadata

case class YliopistoKoulutusMetadata(tyyppi: Koulutustyyppi = Yo,
                                     kuvaus: Map[Kieli, String] = Map(),
                                     lisatiedot: Seq[Lisatieto] = Seq(),
                                     koulutusalaKoodiUrit: Seq[String] = Seq(),
                                     tutkintonimikeKoodiUrit: Seq[String] = Seq(),
                                     opintojenLaajuusKoodiUri: Option[String] = None,
                                     kuvauksenNimi: Map[Kieli, String] = Map()) extends KorkeakoulutusKoulutusMetadata

case class AmmattikorkeakouluKoulutusMetadata(tyyppi: Koulutustyyppi = Amk,
                                              kuvaus: Map[Kieli, String] = Map(),
                                              lisatiedot: Seq[Lisatieto] = Seq(),
                                              koulutusalaKoodiUrit: Seq[String] = Seq(),
                                              tutkintonimikeKoodiUrit: Seq[String] = Seq(),
                                              opintojenLaajuusKoodiUri: Option[String] = None,
                                              kuvauksenNimi: Map[Kieli, String] = Map()) extends KorkeakoulutusKoulutusMetadata