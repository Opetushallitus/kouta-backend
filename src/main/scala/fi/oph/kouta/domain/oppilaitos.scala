package fi.oph.kouta.domain

import java.time.LocalDateTime

import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.security.Authorizable
import fi.oph.kouta.validation.{IsValid, Validatable}

package object oppilaitos {

  val OppilaitosModel =
    s"""    Oppilaitos:
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
       |            $$ref: '#/components/schemas/Kieli'
       |          example:
       |            - fi
       |            - sv
       |        metadata:
       |          type: object
       |          $$ref: '#/components/schemas/OppilaitosMetadata'
       |        muokkaaja:
       |          type: string
       |          description: Oppilaitosta kuvailutietoja viimeksi muokanneen virkailijan henkilö-oid
       |          example: 1.2.246.562.10.00101010101
       |        organisaatioOid:
       |           type: string
       |           description: Oppilaitoksen kuvailutiedot luoneen organisaation oid
       |           example: 1.2.246.562.10.00101010101
       |        modified:
       |           type: string
       |           format: date-time
       |           description: Oppilaitoksen kuvailutietojen viimeisin muokkausaika. Järjestelmän generoima
       |           example: 2019-08-23T09:55
       |""".stripMargin

  val OppilaitosMetadataModel =
    s"""    OppilaitosMetadata:
       |      type: object
       |      properties:
       |        osoite:
       |          type: object
       |          description: Oppilaitoksen Opintopolussa näytettävä osoite eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Osoite'
       |        wwwSivu:
       |          type: object
       |          description: Oppilaitoksen Opintopolussa näytettävä www-sivu eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Teksti'
       |        tietoaOpiskelusta:
       |          type: array
       |          description: Oppilaitokseen liittyviä lisätietoja, jotka näkyvät oppijalle Opintopolussa
       |          items:
       |            type: object
       |            $$ref: '#/components/schemas/Lisatieto'
       |        esittely:
       |          type: object
       |          description: Oppilaitoksen Opintopolussa näytettävä esittely eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Kuvaus'
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
    s"""    OppilaitoksenOsa:
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
       |            $$ref: '#/components/schemas/Kieli'
       |          example:
       |            - fi
       |            - sv
       |        metadata:
       |          type: object
       |          $$ref: '#/components/schemas/OppilaitoksenOsaMetadata'
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

  val OppilaitoksenOsaMetadataModel =
    s"""    OppilaitoksenOsaMetadata:
       |      type: object
       |      properties:
       |        osoite:
       |          type: object
       |          description: Oppilaitoksen osan Opintopolussa näytettävä osoite eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Osoite'
       |        wwwSivu:
       |          type: object
       |          description: Oppilaitoksen osan Opintopolussa näytettävä www-sivu eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Teksti'
       |        esittely:
       |          type: object
       |          description: Oppilaitoksen osan Opintopolussa näytettävä esittely eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Kuvaus'
       |        kampus:
       |          type: object
       |          description: Oppilaitoksen osan kampuksen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Nimi'
       |        opiskelijoita:
       |          type: integer
       |          description: Oppilaitoksen osan opiskelijoiden lkm
       |""".stripMargin

  val OppilaitoksenOsaListItemModel =
    s"""    OppilaitoksenOsaListItem:
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

  def models = Seq(OppilaitosModel, OppilaitoksenOsaModel, OppilaitosMetadataModel, OppilaitoksenOsaMetadataModel, OppilaitoksenOsaListItemModel)

}

case class Oppilaitos(oid: OrganisaatioOid,
                      tila: Julkaisutila = Tallennettu,
                      metadata: Option[OppilaitosMetadata] = None,
                      kielivalinta: Seq[Kieli] = Seq(),
                      organisaatioOid: OrganisaatioOid,
                      muokkaaja: UserOid,
                      modified: Option[LocalDateTime] = None) extends Validatable with Authorizable {

  override def validate(): IsValid = and(
    assertValid(muokkaaja),
    assertValid(organisaatioOid),
    validateIfTrue(tila == Julkaistu, () => and(
      assertTrue(kielivalinta.nonEmpty, MissingKielivalinta)/*,
      validateIfDefined(metadata, and(
        validateKielistetty(kielivalinta, metadata.get.esittely, "esittely"),
        validateKielistetty(kielivalinta, metadata.get.wwwSivu, "wwwSivu"),
        validateKielistetty(kielivalinta, metadata.get., "esittely"),
      ))*/
    )))
}

case class OppilaitoksenOsa(oid: OrganisaatioOid,
                            oppilaitosOid: OrganisaatioOid,
                            tila: Julkaisutila = Tallennettu,
                            metadata: Option[OppilaitoksenOsaMetadata] = None,
                            kielivalinta: Seq[Kieli] = Seq(),
                            organisaatioOid: OrganisaatioOid,
                            muokkaaja: UserOid,
                            modified: Option[LocalDateTime] = None) extends Validatable with Authorizable {

  override def validate(): IsValid = and(
    assertValid(muokkaaja),
    assertValid(organisaatioOid),
    validateIfTrue(tila == Julkaistu, () => and(
      assertTrue(kielivalinta.nonEmpty, MissingKielivalinta)/*,
      validateIfDefined(metadata, and(
        validateKielistetty(kielivalinta, metadata.get.esittely, "esittely"),
        validateKielistetty(kielivalinta, metadata.get.wwwSivu, "wwwSivu"),
        validateKielistetty(kielivalinta, metadata.get., "esittely"),
      ))*/
    )))
}

case class OppilaitosMetadata(osoite: Option[Osoite] = None,
                              wwwSivu: Kielistetty = Map(),
                              tietoaOpiskelusta: Seq[Lisatieto] = Seq(),
                              esittely: Kielistetty = Map(),
                              opiskelijoita: Option[Integer] = None,
                              korkeakouluja: Option[Integer] = None,
                              tiedekuntia: Option[Integer] = None,
                              kampuksia: Option[Integer] = None,
                              yksikoita: Option[Integer] = None,
                              toimipisteita: Option[Integer] = None,
                              akatemioita: Option[Integer] = None)

case class OppilaitoksenOsaMetadata(osoite: Option[Osoite] = None,
                                    wwwSivu: Kielistetty = Map(),
                                    opiskelijoita: Option[Integer] = None,
                                    kampus: Kielistetty = Map(),
                                    esittely: Kielistetty = Map())

case class OppilaitoksenOsaListItem(oid: OrganisaatioOid,
                                    oppilaitosOid: OrganisaatioOid,
                                    tila: Julkaisutila,
                                    organisaatioOid: OrganisaatioOid,
                                    muokkaaja: UserOid,
                                    modified: LocalDateTime)