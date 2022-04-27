package fi.oph.kouta.domain

import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, ValidatableSubEntity}

package object valintaperusteMetadata {

  val ValintaperusteMetadataModel =
    """    ValintaperusteMetadata:
      |      type: object
      |      properties:
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

  val AmmatillinenValintaperusteMetadataModel =
    """    AmmatillinenValintaperusteMetadata:
      |      type: object
      |      $ref: '#/components/schemas/ValintaperusteMetadata'
      |      properties:
      |        tyyppi:
      |          type: string
      |          description: Valintaperustekuvauksen metatiedon tyyppi
      |          example: amm
      |          enum:
      |            - amm
      |""".stripMargin

  val LukioValintaperusteMetadataModel =
    """    LukioValintaperusteMetadata:
      |      type: object
      |      $ref: '#/components/schemas/ValintaperusteMetadata'
      |      properties:
      |        tyyppi:
      |          type: string
      |          description: Valintaperustekuvauksen metatiedon tyyppi
      |          example: lk
      |          enum:
      |            - lk
      |""".stripMargin

  val YliopistoValintaperusteMetadataModel =
    """    YliopistoValintaperusteMetadata:
      |      type: object
      |      $ref: '#/components/schemas/ValintaperusteMetadata'
      |      properties:
      |        tyyppi:
      |          type: string
      |          description: Valintaperustekuvauksen metatiedon tyyppi
      |          example: yo
      |          enum:
      |            - yo
      |""".stripMargin

  val AmmattikorkeakouluValintaperusteMetadataModel =
    """    AmmattikorkeakouluValintaperusteMetadata:
      |      type: object
      |      $ref: '#/components/schemas/ValintaperusteMetadata'
      |      properties:
      |        tyyppi:
      |          type: string
      |          description: Valintaperustekuvauksen metatiedon tyyppi
      |          example: amk
      |          enum:
      |            - amk
      |""".stripMargin

  val AmmatillinenTutkinnonOsaValintaperusteMetadataModel =
    """    AmmatillinenTutkinnonOsaValintaperusteMetadata:
      |      type: object
      |      $ref: '#/components/schemas/ValintaperusteMetadata'
      |      properties:
      |        tyyppi:
      |          type: string
      |          description: Valintaperustekuvauksen metatiedon tyyppi
      |          example: amm-tutkinnon-osa
      |          enum:
      |            - amm-tutkinnon-osa
      |""".stripMargin

  val AmmatillinenOsaamisalaValintaperusteMetadataModel =
    """    AmmatillinenOsaamisalaValintaperusteMetadata:
      |      type: object
      |      $ref: '#/components/schemas/ValintaperusteMetadata'
      |      properties:
      |        tyyppi:
      |          type: string
      |          description: Valintaperustekuvauksen metatiedon tyyppi
      |          example: amm-osaamisala
      |          enum:
      |            - amm-osaamisala
      |""".stripMargin

  val AmmatillinenMuuValintaperusteMetadataModel =
    """    AmmatillinenMuuValintaperusteMetadata:
      |      type: object
      |      $ref: '#/components/schemas/ValintaperusteMetadata'
      |      properties:
      |        tyyppi:
      |          type: string
      |          description: Valintaperustekuvauksen metatiedon tyyppi
      |          example: amm-muu
      |          enum:
      |            - amm-muu
      |""".stripMargin

  val TutkintokoulutukseenValmentavaValintaperusteMetadataModel =
    """    TutkintokoulutukseenValmentavaValintaperusteMetadata:
      |      type: object
      |      $ref: '#/components/schemas/ValintaperusteMetadata'
      |      properties:
      |        tyyppi:
      |          type: string
      |          description: Valintaperustekuvauksen metatiedon tyyppi
      |          example: tuva
      |          enum:
      |            - tuva
      |""".stripMargin

  val TelmaValintaperusteMetadataModel =
    """    TelmaValintaperusteMetadata:
      |      type: object
      |      $ref: '#/components/schemas/ValintaperusteMetadata'
      |      properties:
      |        tyyppi:
      |          type: string
      |          description: Valintaperustekuvauksen metatiedon tyyppi
      |          example: telma
      |          enum:
      |            - telma
      |""".stripMargin

  val VapaaSivistystyoOpistovuosiValintaperusteMetadataModel =
    """    VapaaSivistystyoOpistovuosiValintaperusteMetadata:
      |      type: object
      |      $ref: '#/components/schemas/ValintaperusteMetadata'
      |      properties:
      |        tyyppi:
      |          type: string
      |          description: Valintaperustekuvauksen metatiedon tyyppi
      |          example: vapaa-sivistystyo-opistovuosi
      |          enum:
      |            - vapaa-sivistystyo-opistovuosi
      |""".stripMargin

  val VapaaSivistystyoMuuValintaperusteMetadataModel =
    """    VapaaSivistystyoMuuValintaperusteMetadata:
      |      type: object
      |      $ref: '#/components/schemas/ValintaperusteMetadata'
      |      properties:
      |        tyyppi:
      |          type: string
      |          description: Valintaperustekuvauksen metatiedon tyyppi
      |          example: vapaa-sivistystyo-muu
      |          enum:
      |            - vapaa-sivistystyo-muu
      |""".stripMargin

  val AikuistenPerusopetusValintaperusteMetadataModel =
    """    AikuistenPerusopetusValintaperusteMetadata:
      |      type: object
      |      $ref: '#/components/schemas/ValintaperusteMetadata'
      |      properties:
      |        tyyppi:
      |          type: string
      |          description: Valintaperustekuvauksen metatiedon tyyppi
      |          example: aikuisten-perusopetus
      |          enum:
      |            - aikuisten-perusopetus
      |""".stripMargin

  val MuuValintaperusteMetadataModel =
    """    MuuValintaperusteMetadata:
      |      type: object
      |      $ref: '#/components/schemas/ValintaperusteMetadata'
      |      properties:
      |        tyyppi:
      |          type: string
      |          description: Valintaperustekuvauksen metatiedon tyyppi
      |          example: muu
      |          enum:
      |            - muu
      |""".stripMargin

  def models = List(ValintaperusteMetadataModel, AmmatillinenValintaperusteMetadataModel,
    AmmattikorkeakouluValintaperusteMetadataModel, YliopistoValintaperusteMetadataModel, LukioValintaperusteMetadataModel,
    AmmatillinenTutkinnonOsaValintaperusteMetadataModel, AmmatillinenOsaamisalaValintaperusteMetadataModel, AmmatillinenMuuValintaperusteMetadataModel,
    TutkintokoulutukseenValmentavaValintaperusteMetadataModel, TelmaValintaperusteMetadataModel, VapaaSivistystyoOpistovuosiValintaperusteMetadataModel,
    VapaaSivistystyoMuuValintaperusteMetadataModel, AikuistenPerusopetusValintaperusteMetadataModel, MuuValintaperusteMetadataModel)
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
    validateIfJulkaistu(tila, and(
      validateOptionalKielistetty(kielivalinta, kuvaus, s"$path.kuvaus"),
      validateOptionalKielistetty(kielivalinta, hakukelpoisuus, s"$path.hakukelpoisuus"),
      validateOptionalKielistetty(kielivalinta, lisatiedot, s"$path.lisatiedot"),
      validateOptionalKielistetty(kielivalinta, valintakokeidenYleiskuvaus, s"$path.valintakokeidenYleiskuvaus")
    ))
  )
}

case class AmmatillinenValintaperusteMetadata(tyyppi: Koulutustyyppi = Amm,
                                              valintatavat: Seq[Valintatapa],
                                              kuvaus: Kielistetty = Map(),
                                              hakukelpoisuus: Kielistetty = Map(),
                                              lisatiedot: Kielistetty = Map(),
                                              sisalto: Seq[Sisalto] = Seq(),
                                              valintakokeidenYleiskuvaus: Kielistetty = Map(),
                                              isMuokkaajaOphVirkailija: Option[Boolean] = None)
    extends ValintaperusteMetadata

case class LukioValintaperusteMetadata(tyyppi: Koulutustyyppi = Lk,
                                       valintatavat: Seq[Valintatapa],
                                       kuvaus: Kielistetty = Map(),
                                       hakukelpoisuus: Kielistetty = Map(),
                                       lisatiedot: Kielistetty = Map(),
                                       sisalto: Seq[Sisalto] = Seq(),
                                       valintakokeidenYleiskuvaus: Kielistetty = Map(),
                                       isMuokkaajaOphVirkailija: Option[Boolean] = None)
  extends ValintaperusteMetadata

case class YliopistoValintaperusteMetadata(tyyppi: Koulutustyyppi = Yo,
                                           valintatavat: Seq[Valintatapa],
                                           kuvaus: Kielistetty = Map(),
                                           hakukelpoisuus: Kielistetty = Map(),
                                           lisatiedot: Kielistetty = Map(),
                                           sisalto: Seq[Sisalto] = Seq(),
                                           valintakokeidenYleiskuvaus: Kielistetty = Map(),
                                           isMuokkaajaOphVirkailija: Option[Boolean] = None)
    extends ValintaperusteMetadata

case class AmmattikorkeakouluValintaperusteMetadata(tyyppi: Koulutustyyppi = Amk,
                                                    valintatavat: Seq[Valintatapa],
                                                    kuvaus: Kielistetty = Map(),
                                                    hakukelpoisuus: Kielistetty = Map(),
                                                    lisatiedot: Kielistetty = Map(),
                                                    sisalto: Seq[Sisalto] = Seq(),
                                                    valintakokeidenYleiskuvaus: Kielistetty = Map(),
                                                    isMuokkaajaOphVirkailija: Option[Boolean] = None)
    extends ValintaperusteMetadata

case class AmmatillinenTutkinnonOsaValintaperusteMetadata(tyyppi: Koulutustyyppi = AmmTutkinnonOsa,
                                                          valintatavat: Seq[Valintatapa],
                                                          kuvaus: Kielistetty = Map(),
                                                          hakukelpoisuus: Kielistetty = Map(),
                                                          lisatiedot: Kielistetty = Map(),
                                                          sisalto: Seq[Sisalto] = Seq(),
                                                          valintakokeidenYleiskuvaus: Kielistetty = Map(),
                                                          isMuokkaajaOphVirkailija: Option[Boolean] = None)
  extends ValintaperusteMetadata

case class AmmatillinenOsaamisalaValintaperusteMetadata(tyyppi: Koulutustyyppi = AmmOsaamisala,
                                                        valintatavat: Seq[Valintatapa],
                                                        kuvaus: Kielistetty = Map(),
                                                        hakukelpoisuus: Kielistetty = Map(),
                                                        lisatiedot: Kielistetty = Map(),
                                                        sisalto: Seq[Sisalto] = Seq(),
                                                        valintakokeidenYleiskuvaus: Kielistetty = Map(),
                                                        isMuokkaajaOphVirkailija: Option[Boolean] = None)
  extends ValintaperusteMetadata

case class AmmatillinenMuuValintaperusteMetadata(tyyppi: Koulutustyyppi = AmmMuu,
                                                 valintatavat: Seq[Valintatapa],
                                                 kuvaus: Kielistetty = Map(),
                                                 hakukelpoisuus: Kielistetty = Map(),
                                                 lisatiedot: Kielistetty = Map(),
                                                 sisalto: Seq[Sisalto] = Seq(),
                                                 valintakokeidenYleiskuvaus: Kielistetty = Map(),
                                                 isMuokkaajaOphVirkailija: Option[Boolean] = None)
  extends ValintaperusteMetadata

case class TutkintokoulutukseenValmentavaValintaperusteMetadata(tyyppi: Koulutustyyppi = Tuva,
                                                                valintatavat: Seq[Valintatapa],
                                                                kuvaus: Kielistetty = Map(),
                                                                hakukelpoisuus: Kielistetty = Map(),
                                                                lisatiedot: Kielistetty = Map(),
                                                                sisalto: Seq[Sisalto] = Seq(),
                                                                valintakokeidenYleiskuvaus: Kielistetty = Map(),
                                                                isMuokkaajaOphVirkailija: Option[Boolean] = None)
  extends ValintaperusteMetadata

case class TelmaValintaperusteMetadata(tyyppi: Koulutustyyppi = Telma,
                                       valintatavat: Seq[Valintatapa],
                                       kuvaus: Kielistetty = Map(),
                                       hakukelpoisuus: Kielistetty = Map(),
                                       lisatiedot: Kielistetty = Map(),
                                       sisalto: Seq[Sisalto] = Seq(),
                                       valintakokeidenYleiskuvaus: Kielistetty = Map(),
                                       isMuokkaajaOphVirkailija: Option[Boolean] = None)
  extends ValintaperusteMetadata

case class VapaaSivistystyoOpistovuosiValintaperusteMetadata(tyyppi: Koulutustyyppi = VapaaSivistystyoOpistovuosi,
                                                             valintatavat: Seq[Valintatapa],
                                                             kuvaus: Kielistetty = Map(),
                                                             hakukelpoisuus: Kielistetty = Map(),
                                                             lisatiedot: Kielistetty = Map(),
                                                             sisalto: Seq[Sisalto] = Seq(),
                                                             valintakokeidenYleiskuvaus: Kielistetty = Map(),
                                                             isMuokkaajaOphVirkailija: Option[Boolean] = None)
  extends ValintaperusteMetadata

case class VapaaSivistystyoMuuValintaperusteMetadata(tyyppi: Koulutustyyppi = VapaaSivistystyoMuu,
                                                     valintatavat: Seq[Valintatapa],
                                                     kuvaus: Kielistetty = Map(),
                                                     hakukelpoisuus: Kielistetty = Map(),
                                                     lisatiedot: Kielistetty = Map(),
                                                     sisalto: Seq[Sisalto] = Seq(),
                                                     valintakokeidenYleiskuvaus: Kielistetty = Map(),
                                                     isMuokkaajaOphVirkailija: Option[Boolean] = None)
  extends ValintaperusteMetadata

case class AikuistenPerusopetusValintaperusteMetadata(tyyppi: Koulutustyyppi = AikuistenPerusopetus,
                                                      valintatavat: Seq[Valintatapa],
                                                      kuvaus: Kielistetty = Map(),
                                                      hakukelpoisuus: Kielistetty = Map(),
                                                      lisatiedot: Kielistetty = Map(),
                                                      sisalto: Seq[Sisalto] = Seq(),
                                                      valintakokeidenYleiskuvaus: Kielistetty = Map(),
                                                      isMuokkaajaOphVirkailija: Option[Boolean] = None)
  extends ValintaperusteMetadata

case class MuuValintaperusteMetadata(tyyppi: Koulutustyyppi = Muu,
                                     valintatavat: Seq[Valintatapa],
                                     kuvaus: Kielistetty = Map(),
                                     hakukelpoisuus: Kielistetty = Map(),
                                     lisatiedot: Kielistetty = Map(),
                                     sisalto: Seq[Sisalto] = Seq(),
                                     valintakokeidenYleiskuvaus: Kielistetty = Map(),
                                     isMuokkaajaOphVirkailija: Option[Boolean] = None)
  extends ValintaperusteMetadata

