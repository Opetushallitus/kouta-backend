package fi.oph.kouta.domain

import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, ValidatableSubEntity}

package object valintaperusteMetadata {

  val ValintaperusteMetadataModel =
    """    ValintaperusteMetadata:
      |      type: object
      |      properties:
      |        kielitaitovaatimukset:
      |          type: array
      |          description: Lista valintaperustekuvauksen kielitaitovaatimuksista
      |          items:
      |            $ref: '#/components/schemas/Kielitaitovaatimus'
      |        valintatavat:
      |          type: array
      |          description: Lista valintaperustekuvauksen valintatavoista
      |          items:
      |            $ref: '#/components/schemas/Valintatapa'
      |""".stripMargin

  val AmmatillinenValintaperusteMetadataModel =
    """    AmmatillinenValintaperusteMetadata:
      |      type: object
      |      allOf:
      |        - $ref: '#/components/schemas/ValintaperusteMetadata'
      |      properties:
      |        tyyppi:
      |          type: string
      |          description: Valintaperustekuvauksen metatiedon tyyppi
      |          example: amm
      |          enum:
      |            - amm
      |""".stripMargin

  val KorkeakoulutusValintaperusteMetadataModel =
    """    KorkeakoulutusValintaperusteMetadata:
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
      |            - $ref: '#/components/schemas/Kuvaus'
      |""".stripMargin

  val YliopistoValintaperusteMetadata =
    """    YliopistoValintaperusteMetadata:
      |      type: object
      |      allOf:
      |        - $ref: '#/components/schemas/KorkeakoulutusValintaperusteMetadata'
      |      properties:
      |        tyyppi:
      |          type: string
      |          description: Valintaperustekuvauksen metatiedon tyyppi
      |          example: yo
      |          enum:
      |            - yo
      |""".stripMargin

  val AmmattikorkeakouluValintaperusteMetadata =
    """    AmmattikorkeakouluValintaperusteMetadata:
      |      type: object
      |      allOf:
      |        - $ref: '#/components/schemas/KorkeakoulutusValintaperusteMetadata'
      |      properties:
      |        tyyppi:
      |          type: string
      |          description: Valintaperustekuvauksen metatiedon tyyppi
      |          example: amk
      |          enum:
      |            - amk
      |""".stripMargin

  val KielitaitovaatimusModel =
    """    Kielitaitovaatimus:
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
      |                  - $ref: '#/components/schemas/Lisatieto'
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

sealed trait ValintaperusteMetadata extends ValidatableSubEntity {
  def tyyppi: Koulutustyyppi
  def valintatavat: Seq[Valintatapa]
  def kielitaitovaatimukset: Seq[ValintaperusteKielitaitovaatimus]
  def kuvaus: Kielistetty

  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfNonEmpty[Valintatapa](valintatavat, s"$path.valintatavat", _.validate(tila, kielivalinta, _)),
    validateIfNonEmpty[ValintaperusteKielitaitovaatimus](kielitaitovaatimukset, s"$path.kielitaitovaatimukset", _.validate(tila, kielivalinta, _)),
    validateIfJulkaistu(tila, validateOptionalKielistetty(kielivalinta, kuvaus, s"$path.kuvaus"))
  )
}

sealed trait KorkeakoulutusValintaperusteMetadata extends ValintaperusteMetadata {
  def osaamistaustaKoodiUrit: Seq[String]

  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    validateIfNonEmpty[String](osaamistaustaKoodiUrit, s"$path.osaamistaustaKoodiUrit", assertMatch(_, OsaamistaustaKoodiPattern, _))
  )
}

case class AmmatillinenValintaperusteMetadata(tyyppi: Koulutustyyppi = Amm,
                                              valintatavat: Seq[Valintatapa],
                                              kielitaitovaatimukset: Seq[ValintaperusteKielitaitovaatimus],
                                              kuvaus: Kielistetty = Map())
    extends ValintaperusteMetadata

case class YliopistoValintaperusteMetadata(tyyppi: Koulutustyyppi = Yo,
                                           valintatavat: Seq[Valintatapa],
                                           kielitaitovaatimukset: Seq[ValintaperusteKielitaitovaatimus],
                                           osaamistaustaKoodiUrit: Seq[String] = Seq(),
                                           kuvaus: Kielistetty = Map())
    extends KorkeakoulutusValintaperusteMetadata

case class AmmattikorkeakouluValintaperusteMetadata(tyyppi: Koulutustyyppi = Amk,
                                                    valintatavat: Seq[Valintatapa],
                                                    kielitaitovaatimukset: Seq[ValintaperusteKielitaitovaatimus],
                                                    osaamistaustaKoodiUrit: Seq[String] = Seq(),
                                                    kuvaus: Kielistetty = Map())
    extends KorkeakoulutusValintaperusteMetadata

case class ValintaperusteKielitaitovaatimus(kieliKoodiUri: Option[String] = None,
                                            kielitaidonVoiOsoittaa: Seq[Kielitaito] = Seq(),
                                            vaatimukset: Seq[Kielitaitovaatimus] = Seq()) extends ValidatableSubEntity {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfDefined[String](kieliKoodiUri, assertMatch(_, KieliKoodiPattern, s"$path.kieliKoodiUri")),
    validateIfNonEmpty[Kielitaito](kielitaidonVoiOsoittaa, s"$path.kielitaidonVoiOsoittaa", _.validate(tila, kielivalinta, _)),
    validateIfNonEmpty[Kielitaitovaatimus](vaatimukset, s"$path.vaatimukset", _.validate(tila, kielivalinta, _)),
    validateIfJulkaistu(tila, assertNotOptional(kieliKoodiUri, s"$path.kieliKoodiUri"))
  )
}

case class Kielitaito(kielitaitoKoodiUri: Option[String] = None,
                      lisatieto: Kielistetty = Map()) extends ValidatableSubEntity {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfDefined[String](kielitaitoKoodiUri, assertMatch(_, KielitaitoKoodiPattern, s"$path.kielitaitoKoodiUri")),
    validateIfJulkaistu(tila, validateOptionalKielistetty(kielivalinta, lisatieto, s"$path.lisatieto"))
  )
}

case class Kielitaitovaatimus(kielitaitovaatimusKoodiUri: Option[String] = None,
                              kielitaitovaatimusKuvaukset: Seq[KielitaitovaatimusKuvaus] = Seq()) extends ValidatableSubEntity {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfDefined[String](kielitaitovaatimusKoodiUri, assertMatch(_, KielitaitovaatimusKoodiPattern, s"$path.kielitaitovaatimusKoodiUri")),
    validateIfNonEmpty[KielitaitovaatimusKuvaus](kielitaitovaatimusKuvaukset, s"$path.kielitaitovaatimusKuvaukset", _.validate(tila, kielivalinta, _))
  )
}

case class KielitaitovaatimusKuvaus(kielitaitovaatimusKuvausKoodiUri: Option[String] = None,
                                    kielitaitovaatimusTaso: Option[String] = None) extends ValidatableSubEntity {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid =
    validateIfDefined[String](kielitaitovaatimusKuvausKoodiUri, assertMatch(_, KielitaitovaatimusKuvausKoodiPattern, s"$path.kielitaitovaatimusKuvausKoodiUri"))

}
