package fi.oph.kouta.domain

import java.util.UUID

import fi.oph.kouta.domain.oid._

package object searchResults {

  val PaikkakuntaModel =
    """    Paikkakunta:
      |      type: object
      |      description: Organisaation paikkakunta
      |      properties:
      |        koodiUri:
      |          type: string
      |          example: kunta_398
      |          description: Paikkakunnan kuntakoodi
      |        nimi:
      |          description: Paikkakunnan nimi
      |          type: object
      |          properties:
      |            fi:
      |              type: string
      |              example: Suomenkielinen nimi
      |              description: "Paikkakunnan suomenkielinen nimi"
      |            sv:
      |              type: string
      |              example: Ruotsinkielinen nimi
      |              description: "Paikkakunnan ruotsinkielinen nimi"
      |            en:
      |              type: string
      |              example: Englanninkielinen nimi
      |              description: "Paikkakunnan englanninkielinen nimi"
      |""".stripMargin

  val OrganisaatioModel =
    """    Organisaatio:
      |      type: object
      |      properties:
      |        nimi:
      |          type: string
      |          description: Organisaation nimi
      |          example: Hieno oppilaitos
      |        oid:
      |          type: string
      |          description: Oorganisaation oid
      |          example: 1.2.246.562.10.00101010101
      |        paikkakunta:
      |          type: object
      |          description: Organisaation paikkakunta
      |          $ref: '#/components/schemas/Paikkakunta'
      |""".stripMargin

  val MuokkaajaModel =
    """    Muokkaaja:
      |      type: object
      |      properties:
      |        nimi:
      |          type: string
      |          description: Muokkaajan (virkailijan) nimi
      |          example: Mauri Muokkaaja
      |        oid:
      |          type: string
      |          description: Muokkaajan (virkailijan) henkilö-oid
      |          example: 1.2.246.562.10.00101010101
      |""".stripMargin

  val SearchItemModel =
    """    SearchItem:
      |      type: object
      |      properties:
      |        nimi:
      |          type: object
      |          description: Opintopolussa näytettävä nimi eri kielillä
      |          $ref: '#/components/schemas/Nimi'
      |        muokkaaja:
      |          type: object
      |          description: Viimeisin muokkaaja
      |          $ref: '#/components/schemas/Muokkaaja'
      |        organisaatio:
      |          type: object
      |          description: Luoja-organisaatio
      |          $ref: '#/components/schemas/Organisaatio'
      |        modified:
      |          type: string
      |          format: date-time
      |          description: Viimeisin muokkausaika
      |          example: 2019-08-23T09:55:17
      |        tila:
      |          type: string
      |          example: "julkaistu"
      |          enum:
      |            - julkaistu
      |            - arkistoitu
      |            - tallennettu
      |          description: Julkaisutila. Jos julkaistu, näkyy oppijalle Opintopolussa.
      |""".stripMargin

  val KoulutusSearchItemModel =
    """    EPeruste:
      |      type: object
      |      properties:
      |        id:
      |          type: integer
      |          description: Koulutuksen ePerusteen id
      |          example: 1234
      |        diaarinumero:
      |          type: string
      |          description: Koulutuksen ePerusteen diaarinumero
      |          example: 1234-OPH-2021
      |        voimassaoloLoppuu:
      |          type: string
      |          description: Koulutuksen ePerusteen voimassaolon loppumishetki
      |          example: 2030-12-12T00:00:00
      |    KoulutusSearchItem:
      |      allOf:
      |        - $ref: '#/components/schemas/SearchItem'
      |        - type: object
      |          properties:
      |            oid:
      |              type: string
      |              description: Koulutuksen yksilöivä tunniste
      |              example: "1.2.246.562.13.00000000000000000009"
      |            eperuste:
      |              type: object
      |              description: Koulutuksen ePerusteen tiedot
      |              $ref: '#/components/schemas/EPeruste'
      |            toteutusCount:
      |              type: integer
      |              description: Koulutukseen liitettyjen organisaation toteutusten lukumäärä
      |              example: 6
      |""".stripMargin

  val ToteutusSearchItemModel =
    """    ToteutusSearchItem:
      |      allOf:
      |        - $ref: '#/components/schemas/SearchItem'
      |        - type: object
      |          properties:
      |            oid:
      |              type: string
      |              description: Toteutuksen yksilöivä tunniste
      |              example: "1.2.246.562.17.00000000000000000009"
      |            hakukohdeCount:
      |              type: integer
      |              description: Toteutukseen liitettyjen hakukohteiden lukumäärä
      |              example: 6
      |""".stripMargin

  val HakuSearchItemModel =
    """    HakuSearchItem:
      |      allOf:
      |        - $ref: '#/components/schemas/SearchItem'
      |        - type: object
      |          properties:
      |            oid:
      |              type: string
      |              description: Haun yksilöivä tunniste
      |              example: "1.2.246.562.29.00000000000000000009"
      |            hakukohdeCount:
      |              type: integer
      |              description: Hakuun liitettyjen hakukohteiden lukumäärä
      |              example: 6
      |""".stripMargin

  val HakukohdeSearchItemModel =
    """    HakukohdeSearchItem:
      |      allOf:
      |        - $ref: '#/components/schemas/SearchItem'
      |        - type: object
      |          properties:
      |            oid:
      |              type: string
      |              description: Hakukohteen yksilöivä tunniste
      |              example: "1.2.246.562.20.00000000000000000009"
      |""".stripMargin

  val ValintaperusteSearchItemModel =
    """    ValintaperusteSearchItem:
      |      allOf:
      |        - $ref: '#/components/schemas/SearchItem'
      |        - type: object
      |          properties:
      |            id:
      |              type: string
      |              description: Valintaperusteen yksilöivä tunniste
      |              example: "ea596a9c-5940-497e-b5b7-aded3a2352a7"
      |""".stripMargin

  val KoulutusSearchResultModel =
    """    KoulutusSearchResult:
      |      type: object
      |      properties:
      |        totalCount:
      |          type: integer
      |          description: Hakutulosten kokonaismäärä
      |          example: 100
      |        result:
      |          type: array
      |          description: Haussa löytyneet koulutukset
      |          items:
      |            type: object
      |            $ref: '#/components/schemas/KoulutusSearchItem'
      |""".stripMargin

  val ToteutusSearchResultModel =
    """    ToteutusSearchResult:
      |      type: object
      |      properties:
      |        totalCount:
      |          type: integer
      |          description: Hakutulosten kokonaismäärä
      |          example: 100
      |        result:
      |          type: array
      |          description: Haussa löytyneet toteutukset
      |          items:
      |            type: object
      |            $ref: '#/components/schemas/ToteutusSearchItem'
      |""".stripMargin

  val HakuSearchResultModel =
    """    HakuSearchResult:
      |      type: object
      |      properties:
      |        totalCount:
      |          type: integer
      |          description: Hakutulosten kokonaismäärä
      |          example: 100
      |        result:
      |          type: array
      |          description: Haussa löytyneet haut
      |          items:
      |            type: object
      |            $ref: '#/components/schemas/HakuSearchItem'
      |""".stripMargin

  val HakukohdeSearchResultModel =
    """    HakukohdeSearchResult:
      |      type: object
      |      properties:
      |        totalCount:
      |          type: integer
      |          description: Hakutulosten kokonaismäärä
      |          example: 100
      |        result:
      |          type: array
      |          description: Haussa löytyneet hakukohteet
      |          items:
      |            type: object
      |            $ref: '#/components/schemas/HakukohdeSearchItem'
      |""".stripMargin

  val ValintaperusteSearchResultModel =
    """    ValintaperusteSearchResult:
      |      type: object
      |      properties:
      |        totalCount:
      |          type: integer
      |          description: Hakutulosten kokonaismäärä
      |          example: 100
      |        result:
      |          type: array
      |          description: Haussa löytyneet valintaperusteet
      |          items:
      |            type: object
      |            $ref: '#/components/schemas/ValintaperusteSearchItem'
      |""".stripMargin

  val models = Seq(PaikkakuntaModel, OrganisaatioModel, MuokkaajaModel, SearchItemModel,
    KoulutusSearchItemModel, KoulutusSearchResultModel, ToteutusSearchItemModel, ToteutusSearchResultModel,
    HakukohdeSearchItemModel, HakukohdeSearchResultModel, HakuSearchItemModel, HakuSearchResultModel,
    ValintaperusteSearchItemModel, ValintaperusteSearchResultModel)
}

case class KoulutusSearchResult(totalCount: Int = 0,
                                result: Seq[KoulutusSearchItem] = Seq())

case class KoulutusSearchResultFromIndex(totalCount: Int = 0,
                                         result: Seq[KoulutusSearchItemFromIndex] = Seq())

case class KoulutusSearchItem (oid: KoulutusOid,
                               nimi: Kielistetty,
                               organisaatio: Organisaatio,
                               muokkaaja: Muokkaaja,
                               modified: Modified,
                               tila: Julkaisutila,
                               eperuste: Option[EPeruste] = None,
                               toteutusCount: Int = 0) extends KoulutusItemCommon

case class KoulutusSearchItemFromIndex (oid: KoulutusOid,
                                        nimi: Kielistetty,
                                        organisaatio: Organisaatio,
                                        muokkaaja: Muokkaaja,
                                        modified: Modified,
                                        tila: Julkaisutila,
                                        eperuste: Option[EPeruste] = None,
                                        toteutukset: Seq[KoulutusSearchItemToteutus] = Seq()) extends KoulutusItemCommon

trait KoulutusItemCommon {
  val oid: KoulutusOid
  val nimi: Kielistetty
  val organisaatio: Organisaatio
  val muokkaaja: Muokkaaja
  val modified: Modified
  val tila: Julkaisutila
  val eperuste: Option[EPeruste]
}

case class EPeruste(id: String,
                    diaarinumero: String,
                    voimassaoloLoppuu: Option[String])

case class KoulutusSearchItemToteutus(oid: ToteutusOid,
                                      tila: Julkaisutila,
                                      organisaatiot: Array[String])

case class ToteutusSearchResult(totalCount: Int = 0,
                                result: Seq[ToteutusSearchItem] = Seq())

case class ToteutusSearchResultFromIndex(totalCount: Int = 0,
                                         result: Seq[ToteutusSearchItemFromIndex] = Seq())

case class ToteutusSearchItem(oid: ToteutusOid,
                              nimi: Kielistetty,
                              organisaatio: Organisaatio,
                              muokkaaja: Muokkaaja,
                              modified: Modified,
                              tila: Julkaisutila,
                              hakukohdeCount: Int = 0) extends ToteutusItemCommon

case class ToteutusSearchItemFromIndex(oid: ToteutusOid,
                                       nimi: Kielistetty,
                                       organisaatio: Organisaatio,
                                       muokkaaja: Muokkaaja,
                                       modified: Modified,
                                       tila: Julkaisutila,
                                       organisaatiot: Array[String],
                                       hakukohteet: Array[ToteutusSearchItemHakukohde]) extends ToteutusItemCommon

trait ToteutusItemCommon {
  val oid: ToteutusOid
  val nimi: Kielistetty
  val organisaatio: Organisaatio
  val muokkaaja: Muokkaaja
  val modified: Modified
  val tila: Julkaisutila
}

case class ToteutusSearchItemHakukohde(tila: Julkaisutila,
                                       organisaatio: Organisaatio)

case class HakuSearchResult(totalCount: Int = 0,
                            result: Seq[HakuSearchItem] = Seq())

case class HakuSearchItem(oid: HakuOid,
                          nimi: Kielistetty,
                          organisaatio: Organisaatio,
                          muokkaaja: Muokkaaja,
                          modified: Modified,
                          tila: Julkaisutila,
                          hakukohdeCount: Int = 0)

case class HakukohdeSearchResult(totalCount: Int = 0,
                                 result: Seq[HakuSearchItem] = Seq())

case class HakukohdeSearchItem(oid: HakukohdeOid,
                               nimi: Kielistetty,
                               organisaatio: Organisaatio,
                               muokkaaja: Muokkaaja,
                               modified: Modified,
                               tila: Julkaisutila)

case class ValintaperusteSearchResult(totalCount: Int = 0,
                                      result: Seq[ValintaperusteSearchItem] = Seq())

case class ValintaperusteSearchItem(id: UUID,
                                    nimi: Kielistetty,
                                    organisaatio: Organisaatio,
                                    muokkaaja: Muokkaaja,
                                    modified: Modified,
                                    tila: Julkaisutila)

case class Organisaatio(oid: OrganisaatioOid,
                        nimi: Kielistetty,
                        paikkakunta: Paikkakunta)

case class Paikkakunta(koodiUri: String,
                       nimi: Kielistetty)

case class Muokkaaja(nimi: String,
                     oid: UserOid)
