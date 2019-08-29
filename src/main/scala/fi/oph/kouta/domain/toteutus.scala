package fi.oph.kouta.domain

import java.time.LocalDateTime

import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, ToteutusOid, UserOid}
import fi.oph.kouta.validation.{IsValid, Validatable}

package object toteutus {

  val ToteutusModel =
    s"""    Toteutus:
       |      type: object
       |      properties:
       |        oid:
       |          type: string
       |          description: Toteutuksen yksilöivä tunniste. Järjestelmän generoima.
       |          example: "1.2.246.562.17.00000000000000000009"
       |        tila:
       |          type: string
       |          example: "julkaistu"
       |          enum:
       |            - julkaistu
       |            - arkistoitu
       |            - tallennettu
       |          description: Toteutuksen julkaisutila. Jos toteutus on julkaistu, se näkyy oppijalle Opintopolussa.
       |        tarjoajat:
       |          type: array
       |          description: Toteutusta tarjoavien organisaatioiden yksilöivät organisaatio-oidit
       |          items:
       |            type: string
       |          example:
       |            - 1.2.246.562.10.00101010101
       |            - 1.2.246.562.10.00101010102
       |        kielivalinta:
       |          type: array
       |          description: Kielet, joille toteutuksen nimi, kuvailutiedot ja muut tekstit on käännetty
       |          items:
       |            $$ref: '#/components/schemas/Kieli'
       |          example:
       |            - fi
       |            - sv
       |        nimi:
       |          type: object
       |          description: Toteutuksen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Nimi'
       |        metadata:
       |          type: object
       |          oneOf:
       |            - $$ref: '#/components/schemas/YliopistoToteutusMetadata'
       |            - $$ref: '#/components/schemas/AmmatillinenToteutusMetadata'
       |            - $$ref: '#/components/schemas/AmmattikorkeaToteutusMetadata'
       |          example:
       |            koulutustyyppi: amm
       |            koulutusalaKoodiUrit:
       |              - kansallinenkoulutusluokitus2016koulutusalataso2_054#1
       |              - kansallinenkoulutusluokitus2016koulutusalataso2_055#1
       |            kuvaus:
       |              fi: Suomenkielinen kuvaus
       |              sv: Ruotsinkielinen kuvaus
       |        muokkaaja:
       |          type: string
       |          description: Toteutusta viimeksi muokanneen virkailijan henkilö-oid
       |          example: 1.2.246.562.10.00101010101
       |        organisaatioOid:
       |           type: string
       |           description: Toteutuksen luoneen organisaation oid
       |           example: 1.2.246.562.10.00101010101
       |        modified:
       |           type: string
       |           format: date-time
       |           description: Toteutuksen viimeisin muokkausaika. Järjestelmän generoima
       |           example: 2019-08-23T09:55
       |""".stripMargin

  val ToteutusListItemModel =
    s"""    ToteutusListItem:
       |      type: object
       |      properties:
       |        oid:
       |          type: string
       |          description: Toteutuksen yksilöivä tunniste. Järjestelmän generoima.
       |          example: "1.2.246.562.17.00000000000000000009"
       |        koulutusOid:
       |          type: string
       |          description: Toteutukseen liittyvän koulutuksen yksilöivä tunniste.
       |          example: "1.2.246.562.13.00000000000000000009"
       |        tila:
       |          type: string
       |          example: "julkaistu"
       |          enum:
       |            - julkaistu
       |            - arkistoitu
       |            - tallennettu
       |          description: Koulutuksen toteutuksen julkaisutila. Jos koulutus on julkaistu, se näkyy oppijalle Opintopolussa.
       |        nimi:
       |          type: object
       |          description: Toteutuksen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty toteutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Nimi'
       |        muokkaaja:
       |          type: string
       |          description: Toteutusta viimeksi muokanneen virkailijan henkilö-oid
       |          example: 1.2.246.562.10.00101010101
       |        organisaatioOid:
       |           type: string
       |           description: Toteutuksen luoneen organisaation oid
       |           example: 1.2.246.562.10.00101010101
       |        modified:
       |           type: string
       |           format: date-time
       |           description: Toteutuksen viimeisin muokkausaika. Järjestelmän generoima
       |           example: 2019-08-23T09:55
       |""".stripMargin

  def models = List(ToteutusModel, ToteutusListItemModel)
}

case class Toteutus(oid: Option[ToteutusOid] = None,
                    koulutusOid: KoulutusOid,
                    tila: Julkaisutila = Tallennettu,
                    tarjoajat: List[OrganisaatioOid] = List(),
                    nimi: Kielistetty = Map(),
                    metadata: Option[ToteutusMetadata] = None,
                    muokkaaja: UserOid,
                    organisaatioOid: OrganisaatioOid,
                    kielivalinta: Seq[Kieli] = Seq(),
                    modified: Option[LocalDateTime]) extends PerustiedotWithOid with Validatable {

  override def validate(): IsValid = and(
     super.validate(),
     assertValid(koulutusOid),
     validateIfDefined[ToteutusOid](oid, assertValid(_)),
     validateOidList(tarjoajat)
  )
}

case class ToteutusListItem(oid: ToteutusOid,
                            koulutusOid: KoulutusOid,
                            nimi: Kielistetty,
                            tila: Julkaisutila,
                            organisaatioOid: OrganisaatioOid,
                            muokkaaja: UserOid,
                            modified: LocalDateTime) extends OidListItem