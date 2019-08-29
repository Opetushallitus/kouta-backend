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

  val KielitaitovaatimusModel =
    s"""    Kielitaitovaatimus:
       |      type: object
       |      properties:
       |        kieliKoodiUri:
       |          type: string
       |          description: Kielitaitovaatimuksen kieli. Viittaa kodistoon
       |          example:
       |        kielitaidonVoiOsoittaa:
       |          type: array
       |          description: Lista tavoista, joilla kielitaidon voi osoittaa
       |        vaatimukset:
       |          type: array
       |          description: Lista kielitaitovaatimuksista
       |          items:
       |            - type: object
       |              properties:
       |                kielitaitovaatimusKoodiUri:
       |                  type: string
       |                  description: Kielitaitovaatimuksen koodiUri. Viittaa koodistoon
       |                  example:
       |                kielitaitovaatimusKuvaukset:
       |                  type: array
       |                  description: Lista kielitaitovaatimusten kuvauksia eri kielillä.
       |                  items:
       |                    - type: object
       |                      allOf:
       |                        - $$ref: '#/components/schemas/Kuvaus'

       |        valintatavat:
       |          type: array
       |          description: Lista valintaperustekuvauksen valintatavoista
       |          items:
       |            $$ref: '#/components/schemas/Valintatapa'
       |        kielitaitovaatimukset:
       |          type: array
       |          description: Lista valintaperustekuvauksen kielitaitovaatimuksista
       |          items:
       |            $$ref: '#/components/schemas/Kielitaitovaatimus'
       |""".stripMargin
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
