package fi.oph.kouta.domain

import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, ValidatableSubEntity}

package object valintaperusteMetadata {

  val ValintaperusteMetadataModel =
    """    ValintaperusteMetadata:
      |      type: object
      |      properties:
      |        tyyppi:
      |          type: string
      |          description: Valintaperustekuvauksen metatiedon (koulutus)tyyppi
      |          $ref: '#/components/schemas/Koulutustyyppi'
      |        valintatavat:
      |          type: array
      |          description: Lista valintaperustekuvauksen valintatavoista
      |          items:
      |            $ref: '#/components/schemas/Valintatapa'
      |        valintakokeidenYleiskuvaus:
      |          type: object
      |          description: Valintakokeiden yleiskuvaus eri kielillä. Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        kuvaus:
      |          type: object
      |          description: Valintaperustekuvauksen kuvausteksti eri kielillä. Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        hakukelpoisuus:
      |          type: object
      |          description: Valintaperustekuvauksen hakukelpoisuus eri kielillä. Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        lisatiedot:
      |          type: object
      |          description: Valintaperustekuvauksen lisatiedot eri kielillä. Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        sisalto:
      |          type: array
      |          description: Valintaperusteen kuvauksen sisältö. Voi sisältää sekä teksti- että taulukkoelementtejä.
      |          items:
      |            type: object
      |            $ref: '#/components/schemas/SisaltoItem'
      |""".stripMargin

  def models = List(
    ValintaperusteMetadataModel
  )
}

sealed trait ValintaperusteMetadata {
  def tyyppi: Koulutustyyppi
  def valintatavat: Seq[Valintatapa]
  def kuvaus: Kielistetty
  def hakukelpoisuus: Kielistetty
  def lisatiedot: Kielistetty
  def valintakokeidenYleiskuvaus: Kielistetty
  def sisalto: Seq[Sisalto]
  def isMuokkaajaOphVirkailija: Option[Boolean]
}

case class GenericValintaperusteMetadata(
    tyyppi: Koulutustyyppi,
    valintatavat: Seq[Valintatapa],
    kuvaus: Kielistetty,
    hakukelpoisuus: Kielistetty = Map(),
    lisatiedot: Kielistetty = Map(),
    sisalto: Seq[Sisalto] = Seq(),
    valintakokeidenYleiskuvaus: Kielistetty = Map(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends ValintaperusteMetadata
