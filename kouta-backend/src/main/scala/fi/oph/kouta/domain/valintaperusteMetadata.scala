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
      |            oneOf:
      |              - $ref: '#/components/schemas/SisaltoTeksti'
      |              - $ref: '#/components/schemas/SisaltoTaulukko'
      |""".stripMargin

  def models = List(
    ValintaperusteMetadataModel
  )
}

sealed trait ValintaperusteMetadata extends ValidatableSubEntity {
  def tyyppi: Koulutustyyppi
  def valintatavat: Seq[Valintatapa]
  def kuvaus: Kielistetty
  def hakukelpoisuus: Kielistetty
  def lisatiedot: Kielistetty
  def valintakokeidenYleiskuvaus: Kielistetty
  def sisalto: Seq[Sisalto]
  def isMuokkaajaOphVirkailija: Option[Boolean]

  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfNonEmpty[Valintatapa](valintatavat, s"$path.valintatavat", _.validate(tila, kielivalinta, _)),
    validateIfNonEmpty[Sisalto](sisalto, s"$path.sisalto", _.validate(tila, kielivalinta, _)),
    validateIfJulkaistu(
      tila,
      and(
        validateOptionalKielistetty(kielivalinta, kuvaus, s"$path.kuvaus"),
        validateOptionalKielistetty(kielivalinta, hakukelpoisuus, s"$path.hakukelpoisuus"),
        validateOptionalKielistetty(kielivalinta, lisatiedot, s"$path.lisatiedot"),
        validateOptionalKielistetty(kielivalinta, valintakokeidenYleiskuvaus, s"$path.valintakokeidenYleiskuvaus")
      )
    )
  )
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
