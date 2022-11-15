package fi.oph.kouta.domain

import java.util.UUID
import fi.oph.kouta.domain.oid._
import fi.vm.sade.utils.slf4j.Logging

import java.time.LocalDateTime

package object searchResults {

  val IndexedOrganisaatioModel =
    """    IndexedOrganisaatio:
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
      |          $ref: '#/components/schemas/IndexedOrganisaatio'
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
      |    KoulutusSearchItemWithToteutukset:
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
      |            toteutukset:
      |              type: object
      |              description: Koulutukseen liitetyt toteutukset
      |              properties:
      |                oid:
      |                  type: string
      |                  description: Toteutuksen yksilöivä tunniste
      |                  example: "1.2.246.562.17.00000000000000000009"
      |                nimi:
      |                  type: object
      |                  description: Opintopolussa näytettävä nimi eri kielillä
      |                  $ref: '#/components/schemas/Nimi'
      |                organisaatio:
      |                  type: object
      |                  description: Luoja-organisaatio
      |                  $ref: '#/components/schemas/IndexedOrganisaatio'
      |                modified:
      |                  type: string
      |                  format: date-time
      |                  description: Viimeisin muokkausaika
      |                  example: 2019-08-23T09:55:17
      |                tila:
      |                  type: string
      |                  example: "julkaistu"
      |                  enum:
      |                    - julkaistu
      |                    - arkistoitu
      |                    - tallennettu
      |                  description: Julkaisutila. Jos julkaistu, näkyy oppijalle Opintopolussa.
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
      |    ToteutusSearchItemWithHakukohteet:
      |      allOf:
      |        - $ref: '#/components/schemas/SearchItem'
      |        - type: object
      |          properties:
      |            oid:
      |              type: string
      |              description: Toteutuksen yksilöivä tunniste
      |              example: "1.2.246.562.17.00000000000000000009"
      |            hakukohteet:
      |              type: object
      |              description: Toteutukseen liitetyt hakukohteet
      |              properties:
      |                hakukohdeOid:
      |                  type: string
      |                  description: Hakukohteen yksilöivä tunniste
      |                  example: "1.2.246.562.20.00000000000000000009"
      |                nimi:
      |                  type: object
      |                  description: Opintopolussa näytettävä nimi eri kielillä
      |                  $ref: '#/components/schemas/Nimi'
      |                organisaatio:
      |                  type: object
      |                  description: Luoja-organisaatio
      |                  $ref: '#/components/schemas/IndexedOrganisaatio'
      |                modified:
      |                  type: string
      |                  format: date-time
      |                  description: Viimeisin muokkausaika
      |                  example: 2019-08-23T09:55:17
      |                tila:
      |                  type: string
      |                  example: "julkaistu"
      |                  enum:
      |                    - julkaistu
      |                    - arkistoitu
      |                    - tallennettu
      |                  description: Julkaisutila. Jos julkaistu, näkyy oppijalle Opintopolussa.
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
      |    HakuSearchItemWithHakukohteet:
      |      allOf:
      |        - $ref: '#/components/schemas/SearchItem'
      |        - type: object
      |          properties:
      |            oid:
      |              type: string
      |              description: Haun yksilöivä tunniste
      |              example: "1.2.246.562.29.00000000000000000009"
      |            hakukohteet:
      |              type: object
      |              description: Hakuun liitetyt hakukohteet
      |              properties:
      |                oid:
      |                  type: string
      |                  description: Hakukohteen yksilöivä tunniste
      |                  example: "1.2.246.562.20.00000000000000000009"
      |                nimi:
      |                  type: object
      |                  description: Opintopolussa näytettävä nimi eri kielillä
      |                  $ref: '#/components/schemas/Nimi'
      |                organisaatio:
      |                  type: object
      |                  description: Luoja-organisaatio
      |                  $ref: '#/components/schemas/IndexedOrganisaatio'
      |                modified:
      |                  type: string
      |                  format: date-time
      |                  description: Viimeisin muokkausaika
      |                  example: 2019-08-23T09:55:17
      |                tila:
      |                  type: string
      |                  example: "julkaistu"
      |                  enum:
      |                    - julkaistu
      |                    - arkistoitu
      |                    - tallennettu
      |                  description: Julkaisutila. Jos julkaistu, näkyy oppijalle Opintopolussa.
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

  val models = Seq(IndexedOrganisaatioModel, MuokkaajaModel, SearchItemModel,
    KoulutusSearchItemModel, KoulutusSearchResultModel, ToteutusSearchItemModel, ToteutusSearchResultModel,
    HakukohdeSearchItemModel, HakukohdeSearchResultModel, HakuSearchItemModel, HakuSearchResultModel,
    ValintaperusteSearchItemModel, ValintaperusteSearchResultModel)

  type KoulutusSearchResultFromIndex = SearchResult[KoulutusSearchItemFromIndex]
  type ToteutusSearchResultFromIndex = SearchResult[ToteutusSearchItemFromIndex]
  type HakuSearchResultFromIndex = SearchResult[HakuSearchItemFromIndex]
  type KoulutusSearchResult = SearchResult[KoulutusSearchItem]
  type HakukohdeSearchResult = SearchResult[HakukohdeSearchItem]
  type ValintaperusteSearchResult = SearchResult[ValintaperusteSearchItem]
}

case class KoulutusSearchItem (oid: KoulutusOid,
                               nimi: Kielistetty,
                               organisaatio: IndexedOrganisaatio,
                               muokkaaja: Muokkaaja,
                               modified: Modified,
                               tila: Julkaisutila,
                               koulutustyyppi: Koulutustyyppi,
                               julkinen: Option[Boolean] = None,
                               eperuste: Option[EPeruste] = None,
                               toteutusCount: Int = 0) extends KoulutusItemCommon

case class KoulutusSearchItemFromIndex (oid: KoulutusOid,
                                        nimi: Kielistetty,
                                        organisaatio: IndexedOrganisaatio,
                                        muokkaaja: Muokkaaja,
                                        modified: Modified,
                                        tila: Julkaisutila,
                                        koulutustyyppi: Koulutustyyppi,
                                        julkinen: Option[Boolean] = None,
                                        eperuste: Option[EPeruste] = None,
                                        toteutukset: Seq[KoulutusSearchItemToteutus] = Seq()) extends KoulutusItemCommon with Logging

trait KoulutusItemCommon extends HasTila {
  val oid: KoulutusOid
  val nimi: Kielistetty
  val organisaatio: IndexedOrganisaatio
  val muokkaaja: Muokkaaja
  val modified: Modified
  val eperuste: Option[EPeruste]
}

case class EPeruste(id: String,
                    diaarinumero: String,
                    voimassaoloLoppuu: Option[String])

case class KoulutusSearchItemToteutus(oid: ToteutusOid,
                                      nimi: Kielistetty,
                                      tila: Julkaisutila,
                                      modified: Modified,
                                      organisaatio: IndexedOrganisaatio,
                                      organisaatiot: Seq[String] = Seq())


case class SearchResult[TItem](totalCount: Long = 0,
                               result: Seq[TItem] = Seq.empty)

case class ToteutusSearchResult(totalCount: Long = 0,
                                result: Seq[ToteutusSearchItem] = Seq())

case class ToteutusSearchItem(oid: ToteutusOid,
                              nimi: Kielistetty,
                              organisaatio: IndexedOrganisaatio,
                              muokkaaja: Muokkaaja,
                              modified: Modified,
                              tila: Julkaisutila,
                              organisaatiot: Seq[String] = Seq(),
                              koulutustyyppi: Option[Koulutustyyppi] = None,
                              hakukohteet: Seq[ToteutusSearchItemHakukohde] = Seq.empty,
                              hakukohdeCount: Int = 0) extends ToteutusItemCommon

case class ToteutusHakutieto(hakukohteet: Seq[ToteutusSearchItemHakukohde] = Seq.empty)
case class ToteutusSearchItemFromIndex(oid: ToteutusOid,
                                       nimi: Kielistetty,
                                       organisaatio: IndexedOrganisaatio,
                                       muokkaaja: Muokkaaja,
                                       modified: Modified,
                                       tila: Julkaisutila,
                                       koulutustyyppi: Option[Koulutustyyppi] = None,
                                       organisaatiot: Seq[String] = Seq(),
                                       hakutiedot: Seq[ToteutusHakutieto] = Seq.empty
                                      ) extends ToteutusItemCommon with Logging

trait ToteutusItemCommon extends HasTila {
  val oid: ToteutusOid
  val nimi: Kielistetty
  val organisaatio: IndexedOrganisaatio
  val muokkaaja: Muokkaaja
  val modified: Modified
}

case class ToteutusSearchItemHakukohde(hakukohdeOid: HakukohdeOid, // TODO: Why is this hakukohdeOid?
                                       nimi: Kielistetty,
                                       tila: Julkaisutila,
                                       modified: Modified,
                                       organisaatio: IndexedOrganisaatio)

case class HakuSearchResult(totalCount: Long = 0,
                            result: Seq[HakuSearchItem] = Seq())

case class HakuSearchItem(oid: HakuOid,
                          nimi: Kielistetty,
                          organisaatio: IndexedOrganisaatio,
                          muokkaaja: Muokkaaja,
                          modified: Modified,
                          tila: Julkaisutila,
                          hakutapa: Hakutapa,
                          koulutuksenAlkamiskausi: KoulutuksenAlkamiskausiSearchItem,
                          hakukohdeCount: Int = 0) extends HakuItemCommon

case class HakuMetadataIndexed(koulutuksenAlkamiskausi: KoulutuksenAlkamiskausiSearchItem)

case class HakuSearchItemFromIndex(oid: HakuOid,
                                   nimi: Kielistetty,
                                   organisaatio: IndexedOrganisaatio,
                                   muokkaaja: Muokkaaja,
                                   modified: Modified,
                                   tila: Julkaisutila,
                                   hakutapa: Hakutapa,
                                   metadata: HakuMetadataIndexed,
                                   hakukohteet: Seq[HakuSearchItemHakukohde] = Seq()) extends HakuItemCommon with Logging

trait HakuItemCommon extends HasTila {
  val oid: HakuOid
  val nimi: Kielistetty
  val organisaatio: IndexedOrganisaatio
  val muokkaaja: Muokkaaja
  val modified: Modified
}

case class HakuSearchItemHakukohde(oid: HakukohdeOid,
                                   nimi: Kielistetty,
                                   tila: Julkaisutila,
                                   modified: Modified,
                                   organisaatio: IndexedOrganisaatio)

case class HakukohdeSearchItem(oid: HakukohdeOid,
                               nimi: Kielistetty,
                               organisaatio: IndexedOrganisaatio,
                               muokkaaja: Muokkaaja,
                               modified: Modified,
                               tila: Julkaisutila,
                               koulutustyyppi: Option[Koulutustyyppi] = None,
                               hakuOid: Option[HakuOid],
                               hakuNimi: Option[Kielistetty]
                              ) extends HasTila

case class ValintaperusteSearchItem(id: UUID,
                                    nimi: Kielistetty,
                                    organisaatio: IndexedOrganisaatio,
                                    muokkaaja: Muokkaaja,
                                    modified: Modified,
                                    tila: Julkaisutila,
                                    koulutustyyppi: Koulutustyyppi,
                                    julkinen: Option[Boolean] = None) extends HasTila

case class IndexedOrganisaatio(oid: OrganisaatioOid,
                               nimi: Kielistetty)

case class Muokkaaja(nimi: String,
                     oid: UserOid)

case class Hakutapa(koodiUri: String,
                    nimi: Kielistetty)

case class KoulutuksenAlkamiskausiSearchItem(
  alkamiskausityyppi: Option[Alkamiskausityyppi] = None,
  henkilokohtaisenSuunnitelmanLisatiedot: Kielistetty = Map(),
  koulutuksenAlkamiskausi: Option[KoulutuksenAlkamisKausiObject] = None,
  koulutuksenAlkamispaivamaara: Option[LocalDateTime] = None,
  koulutuksenPaattymispaivamaara: Option[LocalDateTime] = None,
  formatoituKoulutuksenalkamispaivamaara: Option[Kielistetty] = None,
  formatoituKoulutuksenpaattymispaivamaara: Option[Kielistetty] = None,
  koulutuksenAlkamiskausiKoodiUri: Option[String] = None,
  koulutuksenAlkamisvuosi: Option[String] = None
)

case class KoulutuksenAlkamisKausiObject(koodiUri: String,
                                         nimi: Kielistetty)
