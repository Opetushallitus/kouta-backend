package fi.oph.kouta.domain

import java.time.LocalDateTime

import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.security.Authorizable
import fi.oph.kouta.validation.{IsValid, Validatable}

package object oppilaitos {

  val OppilaitosModel =
    """    Oppilaitos:
      |      type: object
      |      properties:
      |        oid:
      |          type: string
      |          description: Oppilaitoksen organisaatio-oid
      |          example: "1.2.246.562.10.00101010101"
      |        tila:
      |          type: string
      |          example: "julkaistu"
      |          enum:
      |            - julkaistu
      |            - arkistoitu
      |            - tallennettu
      |          description: Oppilaitoksen julkaisutila. Jos oppilaitos on julkaistu, se näkyy oppijalle Opintopolussa.
      |        kielivalinta:
      |          type: array
      |          description: Kielet, joille oppilaitoksen kuvailutiedot ja muut tekstit on käännetty
      |          items:
      |            $ref: '#/components/schemas/Kieli'
      |          example:
      |            - fi
      |            - sv
      |        metadata:
      |          type: object
      |          $ref: '#/components/schemas/OppilaitosMetadata'
      |        muokkaaja:
      |          type: string
      |          description: Oppilaitosta kuvailutietoja viimeksi muokanneen virkailijan henkilö-oid
      |          example: 1.2.246.562.24.00101010101.
      |        organisaatioOid:
      |           type: string
      |           description: Oppilaitoksen kuvailutiedot luoneen organisaation oid
      |           example: 1.2.246.562.10.00101010101
      |        teemakuva:
      |          type: string
      |          description: Oppilaitoksen Opintopolussa näytettävän teemakuvan URL.
      |          example: https://konfo-files.opintopolku.fi/oppilaitos-teema/1.2.246.562.10.00000000000000000009/f4ecc80a-f664-40ef-98e6-eaf8dfa57f6e.png
      |        modified:
      |           type: string
      |           format: date-time
      |           description: Oppilaitoksen kuvailutietojen viimeisin muokkausaika. Järjestelmän generoima
      |           example: 2019-08-23T09:55
      |""".stripMargin

  val OppilaitosMetadataModel =
    """    OppilaitosMetadata:
      |      type: object
      |      properties:
      |        yhteystiedot:
      |          type: object
      |          description: Oppilaitoksen Opintopolussa näytettävät yhteystiedot
      |          allOf:
      |            - $ref: '#/components/schemas/Yhteystieto'
      |        tietoaOpiskelusta:
      |          type: array
      |          description: Oppilaitokseen liittyviä lisätietoja, jotka näkyvät oppijalle Opintopolussa
      |          items:
      |            type: object
      |            $ref: '#/components/schemas/Lisatieto'
      |        esittely:
      |          type: object
      |          description: Oppilaitoksen Opintopolussa näytettävä esittely eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Kuvaus'
      |        opiskelijoita:
      |          type: integer
      |          description: Oppilaitoksen opiskelijoiden lkm
      |        korkeakouluja:
      |          type: integer
      |          description: Oppilaitoksen korkeakoulujen lkm
      |        tiedekuntia:
      |          type: integer
      |          description: Oppilaitoksen tiedekuntien lkm
      |        kampuksia:
      |          type: integer
      |          description: Oppilaitoksen kampuksien lkm
      |        yksikoita:
      |          type: integer
      |          description: Oppilaitoksen yksiköiden lkm
      |        toimipisteita:
      |          type: integer
      |          description: Oppilaitoksen toimipisteiden lkm
      |        akatemioita:
      |          type: integer
      |          description: Oppilaitoksen akatemioiden lkm
      |""".stripMargin

  val OppilaitoksenOsaModel =
    """    OppilaitoksenOsa:
      |      type: object
      |      properties:
      |        oid:
      |          type: string
      |          description: Oppilaitoksen osan organisaatio-oid
      |          example: "1.2.246.562.10.00101010102"
      |        oppilaitosOid:
      |          type: string
      |          description: Oppilaitoksen osan oppilaitoksen organisaatio-oid
      |          example: "1.2.246.562.10.00101010101"
      |        tila:
      |          type: string
      |          example: "julkaistu"
      |          enum:
      |            - julkaistu
      |            - arkistoitu
      |            - tallennettu
      |          description: Oppilaitoksen osan julkaisutila. Jos oppilaitoksen osa on julkaistu, se näkyy oppijalle Opintopolussa.
      |        kielivalinta:
      |          type: array
      |          description: Kielet, joille oppilaitoksen osan kuvailutiedot ja muut tekstit on käännetty
      |          items:
      |            $ref: '#/components/schemas/Kieli'
      |          example:
      |            - fi
      |            - sv
      |        metadata:
      |          type: object
      |          $ref: '#/components/schemas/OppilaitoksenOsaMetadata'
      |        muokkaaja:
      |          type: string
      |          description: Oppilaitoksen osan kuvailutietoja viimeksi muokanneen virkailijan henkilö-oid
      |          example: 1.2.246.562.10.00101010101
      |        organisaatioOid:
      |           type: string
      |           description: Oppilaitoksen osan kuvailutiedot luoneen organisaation oid
      |           example: 1.2.246.562.10.00101010101
      |        teemakuva:
      |          type: string
      |          description: Oppilaitoksen osan Opintopolussa näytettävän teemakuvan URL.
      |          example: https://konfo-files.opintopolku.fi/oppilaitoksen-osa-teemakuva/1.2.246.562.10.00000000000000000009/f4ecc80a-f664-40ef-98e6-eaf8dfa57f6e.png
      |        modified:
      |           type: string
      |           format: date-time
      |           description: Oppilaitoksen osan kuvailutietojen viimeisin muokkausaika. Järjestelmän generoima
      |           example: 2019-08-23T09:55
      |""".stripMargin

  val OppilaitoksenOsaMetadataModel =
    """    OppilaitoksenOsaMetadata:
      |      type: object
      |      properties:
      |        yhteystiedot:
      |          type: object
      |          description: Oppilaitoksen osan Opintopolussa näytettävät yhteystiedot
      |          allOf:
      |            - $ref: '#/components/schemas/Yhteystieto'
      |        esittely:
      |          type: object
      |          description: Oppilaitoksen osan Opintopolussa näytettävä esittely eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Kuvaus'
      |        kampus:
      |          type: object
      |          description: Oppilaitoksen osan kampuksen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Nimi'
      |        opiskelijoita:
      |          type: integer
      |          description: Oppilaitoksen osan opiskelijoiden lkm
      |""".stripMargin

  val OppilaitoksenOsaListItemModel =
    """    OppilaitoksenOsaListItem:
      |      type: object
      |      properties:
      |        oid:
      |          type: string
      |          description: Oppilaitoksen osan organisaatio-oid
      |          example: "1.2.246.562.10.00101010102"
      |        oppilaitosOid:
      |          type: string
      |          description: Oppilaitoksen osan oppilaitoksen organisaatio-oid
      |          example: "1.2.246.562.10.00101010101"
      |        tila:
      |          type: string
      |          example: "julkaistu"
      |          enum:
      |            - julkaistu
      |            - arkistoitu
      |            - tallennettu
      |          description: Oppilaitoksen osan julkaisutila. Jos oppilaitoksen osa on julkaistu, se näkyy oppijalle Opintopolussa.
      |        muokkaaja:
      |          type: string
      |          description: Oppilaitoksen osan kuvailutietoja viimeksi muokanneen virkailijan henkilö-oid
      |          example: 1.2.246.562.10.00101010101
      |        organisaatioOid:
      |           type: string
      |           description: Oppilaitoksen osan kuvailutiedot luoneen organisaation oid
      |           example: 1.2.246.562.10.00101010101
      |        modified:
      |           type: string
      |           format: date-time
      |           description: Oppilaitoksen osan kuvailutietojen viimeisin muokkausaika. Järjestelmän generoima
      |           example: 2019-08-23T09:55
      |""".stripMargin

  val YhteystietoModel =
    """    Yhteystieto:
      |      type: object
      |      properties:
      |        osoite:
      |          type: object
      |          description: Opintopolussa näytettävä osoite eri kielillä. Kielet on määritetty kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Osoite'
      |        sahkoposti:
      |          type: object
      |          description: Opintopolussa näytettävä sähköpostiosoite eri kielillä. Kielet on määritetty kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Teksti'
      |        puhelinnumero:
      |          type: object
      |          description: Opintopolussa näytettävä puhelinnumero eri kielillä. Kielet on määritetty kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Teksti'
      |        wwwSivu:
      |          type: object
      |          description: Opintopolussa näytettävä www-sivu eri kielillä. Kielet on määritetty kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Teksti'
      |""".stripMargin

  def models = Seq(OppilaitosModel, OppilaitoksenOsaModel, OppilaitosMetadataModel, OppilaitoksenOsaMetadataModel, OppilaitoksenOsaListItemModel, YhteystietoModel)
}

case class Oppilaitos(oid: OrganisaatioOid,
                      tila: Julkaisutila = Tallennettu,
                      metadata: Option[OppilaitosMetadata] = None,
                      kielivalinta: Seq[Kieli] = Seq(),
                      organisaatioOid: OrganisaatioOid,
                      muokkaaja: UserOid,
                      teemakuva: Option[String] = None,
                      modified: Option[LocalDateTime] = None)
  extends Validatable
    with Authorizable
    with HasPrimaryId[OrganisaatioOid, Oppilaitos]
    with HasModified[Oppilaitos]
    with HasTeemakuva[Oppilaitos] {

  override def validate(): IsValid = and(
    assertValid(muokkaaja),
    assertValid(organisaatioOid),
    validateIfJulkaistu(tila, and(
      assertTrue(kielivalinta.nonEmpty, MissingKielivalinta)/*,
      validateIfDefined(metadata, and(
        validateKielistetty(kielivalinta, metadata.get.esittely, "esittely"),
        validateKielistetty(kielivalinta, metadata.get.wwwSivu, "wwwSivu"),
        validateKielistetty(kielivalinta, metadata.get., "esittely"),
      ))*/
    )))

  override def primaryId: Option[OrganisaatioOid] = Some(oid)

  override def withPrimaryID(oid: OrganisaatioOid): Oppilaitos = copy(oid = oid)

  override def withTeemakuva(teemakuva: Option[String]): Oppilaitos = copy(teemakuva = teemakuva)

  override def withModified(modified: LocalDateTime): Oppilaitos = copy(modified = Some(modified))
}

case class OppilaitoksenOsa(oid: OrganisaatioOid,
                            oppilaitosOid: OrganisaatioOid,
                            tila: Julkaisutila = Tallennettu,
                            metadata: Option[OppilaitoksenOsaMetadata] = None,
                            kielivalinta: Seq[Kieli] = Seq(),
                            organisaatioOid: OrganisaatioOid,
                            muokkaaja: UserOid,
                            teemakuva: Option[String] = None,
                            modified: Option[LocalDateTime] = None)
  extends Validatable
    with Authorizable
    with HasPrimaryId[OrganisaatioOid, OppilaitoksenOsa]
    with HasModified[OppilaitoksenOsa]
    with HasTeemakuva[OppilaitoksenOsa] {

  override def validate(): IsValid = and(
    assertValid(muokkaaja),
    assertValid(organisaatioOid),
    validateIfJulkaistu(tila, and(
      assertTrue(kielivalinta.nonEmpty, MissingKielivalinta)/*,
      validateIfDefined(metadata, and(
        validateKielistetty(kielivalinta, metadata.get.esittely, "esittely"),
        validateKielistetty(kielivalinta, metadata.get.wwwSivu, "wwwSivu"),
        validateKielistetty(kielivalinta, metadata.get., "esittely"),
      ))*/
    )))

  override def primaryId: Option[OrganisaatioOid] = Some(oid)

  override def withPrimaryID(oid: OrganisaatioOid): OppilaitoksenOsa = copy(oid = oid)

  override def withTeemakuva(teemakuva: Option[String]): OppilaitoksenOsa = copy(teemakuva = teemakuva)

  override def withModified(modified: LocalDateTime): OppilaitoksenOsa = copy(modified = Some(modified))
}

case class OppilaitosMetadata(tietoaOpiskelusta: Seq[Lisatieto] = Seq(),
                              yhteystiedot: Option[Yhteystieto] = None,
                              esittely: Kielistetty = Map(),
                              opiskelijoita: Option[Integer] = None,
                              korkeakouluja: Option[Integer] = None,
                              tiedekuntia: Option[Integer] = None,
                              kampuksia: Option[Integer] = None,
                              yksikoita: Option[Integer] = None,
                              toimipisteita: Option[Integer] = None,
                              akatemioita: Option[Integer] = None)

case class OppilaitoksenOsaMetadata(yhteystiedot: Option[Yhteystieto] = None,
                                    opiskelijoita: Option[Integer] = None,
                                    kampus: Kielistetty = Map(),
                                    esittely: Kielistetty = Map())

case class OppilaitoksenOsaListItem(oid: OrganisaatioOid,
                                    oppilaitosOid: OrganisaatioOid,
                                    tila: Julkaisutila,
                                    organisaatioOid: OrganisaatioOid,
                                    muokkaaja: UserOid,
                                    modified: LocalDateTime)

case class Yhteystieto(osoite: Option[Osoite] = None,
                       wwwSivu: Kielistetty = Map(),
                       puhelinnumero: Kielistetty = Map(),
                       sahkoposti: Kielistetty = Map())
