package fi.oph.kouta.domain

package object valintaperusteMetadata {

  val ValintaperusteMetadataModel =
    s"""    ValintaperusteMetadata:
       |      type: object
       |      properties:
       |        kielitaitovaatimukset:
       |          type: array
       |          description: Lista valintaperustekuvauksen kielitaitovaatimuksista
       |          items:
       |            $$ref: '#/components/schemas/Kielitaitovaatimus'
       |""".stripMargin

  val AmmatillinenValintaperusteMetadataModel =
    s"""    AmmatillinenValintaperusteMetadata:
       |      type: object
       |      allOf:
       |        - $$ref: '#/components/schemas/ValintaperusteMetadata'
       |      properties:
       |        valintatavat:
       |          type: array
       |          description: Lista valintaperustekuvauksen valintatavoista
       |          items:
       |            $$ref: '#/components/schemas/AmmatillinenValintatapa'
       |        koulutustyyppi:
       |          type: string
       |          description: Valintaperustekuvauksen metatiedon tyyppi
       |          example: amm
       |          enum:
       |            - amm
       |""".stripMargin

  val KorkeakoulutusValintaperusteMetadataModel =
    s"""    KorkeakoulutusValintaperusteMetadata:
       |      type: object
       |      properties:
       |        osaamistaustaKoodiUrit:
       |          type: array
       |          description: Lista valintaperustekuvauksen osaamistaustoista.
       |            Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/osaamistausta/1)
       |          items:
       |            - type: string
       |          example:
       |            - osaamistausta_001#1
       |            - osaamistausta_002#1
       |        kuvaus:
       |          type: object
       |          description: Valintaperustekuvauksen kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Kuvaus'
       |""".stripMargin

  val YliopistoValintaperusteMetadata =
    s"""    YliopistoValintaperusteMetadata:
       |      type: object
       |      allOf:
       |        - $$ref: '#/components/schemas/KorkeakoulutusValintaperusteMetadata'
       |      properties:
       |        valintatavat:
       |          type: array
       |          description: Lista valintaperustekuvauksen valintatavoista
       |          items:
       |            $$ref: '#/components/schemas/YliopistoValintatapa'
       |        koulutustyyppi:
       |          type: string
       |          description: Valintaperustekuvauksen metatiedon tyyppi
       |          example: yo
       |          enum:
       |            - yo
       |""".stripMargin

  val AmmattikorkeakouluValintaperusteMetadata =
    s"""    AmmattikorkeakouluValintaperusteMetadata:
       |      type: object
       |      allOf:
       |        - $$ref: '#/components/schemas/KorkeakoulutusValintaperusteMetadata'
       |      properties:
       |        valintatavat:
       |          type: array
       |          description: Lista valintaperustekuvauksen valintatavoista
       |          items:
       |            $$ref: '#/components/schemas/AmmattikorkeakouluValintatapa'
       |        koulutustyyppi:
       |          type: string
       |          description: Valintaperustekuvauksen metatiedon tyyppi
       |          example: amk
       |          enum:
       |            - amk
       |""".stripMargin

  val KielitaitovaatimusModel =
    s"""    Kielitaitovaatimus:
       |      type: object
       |      properties:
       |        kieliKoodiUri:
       |          type: string
       |          description: Kielitaitovaatimuksen kieli.
       |            Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kieli/1)
       |          example: kieli_en#1
       |        kielitaidonVoiOsoittaa:
       |          type: array
       |          description: Lista tavoista, joilla kielitaidon voi osoittaa
       |          items:
       |            type: object
       |            properties:
       |              kielitaitoKoodiUri:
       |                type: string
       |                description: Kielitaidon osoittaminen.
       |                  Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kielitaidonosoittaminen/1)
       |                example: kielitaidonosoittaminen_01#1
       |              lisatieto:
       |                type: object
       |                description: Kielitaidon osoittamisen lisätieto eri kielillä.
       |                allOf:
       |                  - $$ref: '#/components/schemas/Lisatieto'
       |        vaatimukset:
       |          type: array
       |          description: Lista kielitaitovaatimuksista
       |          items:
       |            type: object
       |            properties:
       |              kielitaitovaatimusKoodiUri:
       |                type: string
       |                description: Kielitaitovaatimuksen koodiUri.
       |                  Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kielitaitovaatimustyypit/1)
       |                example: kielitaitovaatimustyypit_01#1
       |              kielitaitovaatimusKuvaukset:
       |                type: array
       |                description: Lista kielitaitovaatimusten kuvauksia eri kielillä.
       |                items:
       |                  type: object
       |                  properties:
       |                    kielitaitovaatimusKuvausKoodiUri:
       |                      type: string
       |                      description: Kielitaitovaatimuksen kuvauksen koodiUri.
       |                        Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kielitaitovaatimustyypitkuvaus/1)
       |                      example: kielitaitovaatimustyypitkuvaus_01#1
       |                    kielitaitovaatimusTaso:
       |                      type: string
       |                      description: Kielitaitovaatimuksen taso
       |                      example: A
       |""".stripMargin

  def models = List(ValintaperusteMetadataModel, KielitaitovaatimusModel, AmmatillinenValintaperusteMetadataModel,
    KorkeakoulutusValintaperusteMetadataModel, AmmattikorkeakouluValintaperusteMetadata, YliopistoValintaperusteMetadata)
}

sealed trait ValintaperusteMetadata {
  def koulutustyyppi: Koulutustyyppi
  def valintatavat: Seq[Valintatapa]
  def kielitaitovaatimukset: Seq[ValintaperusteKielitaitovaatimus]
}

sealed trait KorkeakoulutusValintaperusteMetadata extends ValintaperusteMetadata {
  def valintatavat: Seq[KorkeakoulutusValintatapa]
  def kielitaitovaatimukset: Seq[ValintaperusteKielitaitovaatimus]
  def osaamistaustaKoodiUrit: Seq[String]
  def kuvaus: Kielistetty
}

case class AmmatillinenValintaperusteMetadata(koulutustyyppi: Koulutustyyppi = Amm,
                                              valintatavat: Seq[AmmatillinenValintatapa],
                                              kielitaitovaatimukset: Seq[ValintaperusteKielitaitovaatimus])
    extends ValintaperusteMetadata

case class YliopistoValintaperusteMetadata(koulutustyyppi: Koulutustyyppi = Yo,
                                           valintatavat: Seq[YliopistoValintatapa],
                                           kielitaitovaatimukset: Seq[ValintaperusteKielitaitovaatimus],
                                           osaamistaustaKoodiUrit: Seq[String] = Seq(),
                                           kuvaus: Kielistetty = Map())
    extends KorkeakoulutusValintaperusteMetadata

case class AmmattikorkeakouluValintaperusteMetadata(koulutustyyppi: Koulutustyyppi = Amk,
                                                    valintatavat: Seq[AmmattikorkeakouluValintatapa],
                                                    kielitaitovaatimukset: Seq[ValintaperusteKielitaitovaatimus],
                                                    osaamistaustaKoodiUrit: Seq[String] = Seq(),
                                                    kuvaus: Kielistetty = Map())
    extends KorkeakoulutusValintaperusteMetadata

case class ValintaperusteKielitaitovaatimus(kieliKoodiUri: Option[String] = None,
                                            kielitaidonVoiOsoittaa: Seq[Kielitaito] = Seq(),
                                            vaatimukset: Seq[Kielitaitovaatimus] = Seq())

case class Kielitaito(kielitaitoKoodiUri: Option[String] = None,
                      lisatieto: Kielistetty = Map())

case class Kielitaitovaatimus(kielitaitovaatimusKoodiUri: Option[String] = None,
                              kielitaitovaatimusKuvaukset: Seq[KielitaitovaatimusKuvaus] = Seq())

case class KielitaitovaatimusKuvaus(kielitaitovaatimusKuvausKoodiUri: Option[String] = None,
                                    kielitaitovaatimusTaso: Option[String] = None)
