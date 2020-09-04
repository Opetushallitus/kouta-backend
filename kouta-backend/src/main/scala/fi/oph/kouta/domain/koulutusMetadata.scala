package fi.oph.kouta.domain

import fi.oph.kouta.validation.{IsValid, ValidatableSubEntity}
import fi.oph.kouta.validation.Validations._

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

sealed trait KoulutusMetadata extends ValidatableSubEntity {
  val tyyppi: Koulutustyyppi
  val kuvaus: Kielistetty
  val lisatiedot: Seq[Lisatieto]
  val koulutusalaKoodiUrit: Seq[String]

  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfJulkaistu(tila, validateOptionalKielistetty(kielivalinta, kuvaus, s"$path.kuvaus")),
    validateIfNonEmpty[Lisatieto](lisatiedot, s"$path.lisatiedot", _.validate(tila, kielivalinta, _)),
    validateIfNonEmpty[String](koulutusalaKoodiUrit, s"$path.koulutusalaKoodiUrit", assertMatch(_, KoulutusalaKoodiPattern, _))
  )
}

trait KorkeakoulutusKoulutusMetadata extends KoulutusMetadata {
  val kuvauksenNimi: Kielistetty
  val tutkintonimikeKoodiUrit: Seq[String]
  val opintojenLaajuusKoodiUri: Option[String]

  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    validateIfJulkaistu(tila, validateKielistetty(kielivalinta, kuvauksenNimi, s"$path.kuvauksenNimi")),
    validateIfNonEmpty[String](tutkintonimikeKoodiUrit, s"$path.tutkintonimikeKoodiUrit", assertMatch(_, TutkintonimikeKoodiPattern, _)),
    validateIfDefined[String](opintojenLaajuusKoodiUri, assertMatch(_, OpintojenLaajuusKoodiPattern, s"$path.opintojenLaajuusKoodiUri"))
  )
}

case class AmmatillinenKoulutusMetadata(tyyppi: Koulutustyyppi = Amm,
                                        kuvaus: Kielistetty = Map(),
                                        lisatiedot: Seq[Lisatieto] = Seq(),
                                        koulutusalaKoodiUrit: Seq[String] = Seq()) extends KoulutusMetadata

case class TutkinnonOsaMetadata(tyyppi: Koulutustyyppi = AmmTutkinnonOsa,
                                kuvaus: Kielistetty = Map(),
                                lisatiedot: Seq[Lisatieto] = Seq(),
                                koulutusalaKoodiUrit: Seq[String] = Seq(),
                                tutkinnonOsat: Seq[TutkinnonOsa] = Seq()) extends KoulutusMetadata {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    validateIfNonEmpty[TutkinnonOsa](tutkinnonOsat, s"$path/tutkinnonOsat", _.validate(tila, kielivalinta, _)),
    validateIfJulkaistu(tila,
      assertNotEmpty(tutkinnonOsat, s"$path/tutkinnonOsat"))
  )
}

case class OsaamisalaMetadata(tyyppi: Koulutustyyppi = AmmOsaamisala,
                              kuvaus: Kielistetty = Map(),
                              lisatiedot: Seq[Lisatieto] = Seq(),
                              koulutusalaKoodiUrit: Seq[String] = Seq(),
                              osaamisalaKoodiUri: Option[String]) extends KoulutusMetadata {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    validateIfDefined[String](osaamisalaKoodiUri, assertMatch(_, OsaamisalaKoodiPattern, s"$path/osaamisalaKoodiUri")),
    validateIfJulkaistu(tila, assertNotOptional(osaamisalaKoodiUri, s"$path/osaamisalaKoodiUri"))
  )
}

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
