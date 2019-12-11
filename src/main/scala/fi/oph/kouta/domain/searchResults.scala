package fi.oph.kouta.domain

import java.time.LocalDateTime

import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, UserOid}

package object searchResults {

  val PaikkakuntaModel =
    s"""    Paikkakunta:
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
    s"""    Organisaatio:
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
       |          allOf:
       |            - $$ref: '#/components/schemas/Paikkakunta'
       |""".stripMargin

  val MuokkaajaModel =
    s"""    Muokkaaja:
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

  val KoulutusSearchItemModel =
    s"""    KoulutusSearchItem:
       |      type: object
       |      properties:
       |        oid:
       |          type: string
       |          description: Koulutuksen yksilöivä tunniste
       |          example: "1.2.246.562.13.00000000000000000009"
       |        toteutukset:
       |          type: integer
       |          description: Koulutukseen liitettyjen organisaation toteutusten lukumäärä
       |          example: 6
       |        nimi:
       |          type: object
       |          description: Koulutuksen Opintopolussa näytettävä nimi eri kielillä
       |          allOf:
       |            - $$ref: '#/components/schemas/Nimi'
       |        muokkaaja:
       |          type: object
       |          description: Koulutuksen viimeisin muokkaaja
       |          allOf:
       |            - $$ref: '#/components/schemas/Muokkaaja'
       |        organisaatio:
       |          type: object
       |          description: Koulutuksen luonut organisaatio
       |          allOf:
       |            - $$ref: '#/components/schemas/Organisaatio'
       |        modified:
       |           type: string
       |           format: date-time
       |           description: Koulutuksen viimeisin muokkausaika
       |           example: 2019-08-23T09:55
       |        tila:
       |          type: string
       |          example: "julkaistu"
       |          enum:
       |            - julkaistu
       |            - arkistoitu
       |            - tallennettu
       |          description: Koulutuksen julkaisutila. Jos koulutus on julkaistu, se näkyy oppijalle Opintopolussa.
       |""".stripMargin

  val KoulutusSearchResultModel =
    s"""    KoulutusSearchResult:
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
       |            $$ref: '#/components/schemas/KoulutusSearchItem'
       |""".stripMargin

  val models = Seq(PaikkakuntaModel, OrganisaatioModel, MuokkaajaModel, KoulutusSearchItemModel, KoulutusSearchResultModel)
}

case class KoulutusSearchResult(totalCount: Int,
                                result: Seq[KoulutusItem])

object KoulutusSearchResult {
  def apply(): KoulutusSearchResult = new KoulutusSearchResult(0, Seq())
}

case class KoulutusItem(oid: KoulutusOid,
                        nimi: Kielistetty,
                        organisaatio: Organisaatio,
                        muokkaaja: Muokkaaja,
                        modified: LocalDateTime,
                        toteutukset: Int = 0)

case class Organisaatio(oid: OrganisaatioOid,
                        nimi: Kielistetty,
                        paikkakunta: Paikkakunta)

case class Paikkakunta(koodiUri: String,
                       nimi: Kielistetty)

case class Muokkaaja(nimi: String,
                     oid: UserOid)
