package fi.oph.kouta.api

import java.time.LocalDateTime

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

package object koulutus {

  val teksti =
    s"""    Kieli:
       |      type: string
       |      enum:
       |        - fi
       |        - sv
       |        - en
       |    Julkaisutila:
       |      type: string
       |      enum:
       |        - julkaistu
       |        - tallennettu
       |        - arkistoitu
       |    Teksti:
       |      type: object
       |      properties:
       |        fi:
       |          type: string
       |          example: Suomenkielinen teksti
       |          description: "Suomenkielinen teksti, jos kielivalinnassa on 'fi'"
       |        sv:
       |          type: string
       |          example: Ruotsinkielinen teksti
       |          description: "Ruotsinkielinen teksti, jos kielivalinnassa on 'sv'"
       |        en:
       |          type: string
       |          example: Englanninkielinen teksti
       |          description: "Englanninkielinen teksti, jos kielivalinnassa on 'en'"
       |    Nimi:
       |      type: object
       |      properties:
       |        fi:
       |          type: string
       |          example: Suomenkielinen nimi
       |          description: "Suomenkielinen nimi, jos kielivalinnassa on 'fi'"
       |        sv:
       |          type: string
       |          example: Ruotsinkielinen nimi
       |          description: "Ruotsinkielinen nimi, jos kielivalinnassa on 'sv'"
       |        en:
       |          type: string
       |          example: Englanninkielinen nimi
       |          description: "Englanninkielinen nimi, jos kielivalinnassa on 'en'"
       |    Kuvaus:
       |      type: object
       |      properties:
       |        fi:
       |          type: string
       |          example: Suomenkielinen kuvaus
       |          description: "Suomenkielinen kuvaus, jos kielivalinnassa on 'fi'"
       |        sv:
       |          type: string
       |          example: Ruotsinkielinen kuvaus
       |          description: "Ruotsinkielinen kuvaus, jos kielivalinnassa on 'sv'"
       |        en:
       |          type: string
       |          example: Englanninkielinen kuvaus
       |          description: "Englanninkielinen kuvaus, jos kielivalinnassa on 'en'"
       |    Linkki:
       |      type: object
       |      properties:
       |        fi:
       |          type: string
       |          example: Linkki suomenkieliselle sivulle
       |          description: "Linkki suomenkieliselle sivulle, jos kielivalinnassa on 'fi'"
       |        sv:
       |          type: string
       |          example: Linkki ruotsinkieliselle sivulle
       |          description: "Linkki ruotsinkieliselle sivulle, jos kielivalinnassa on 'sv'"
       |        en:
       |          type: string
       |          example: Linkki englanninkieliselle sivulle
       |          description: "Linkki englanninkieliselle sivulle, jos kielivalinnassa on 'en'"
       |    Lisatieto:
       |      type: object
       |      properties:
       |        otsikkoKoodiUri:
       |          type: string
       |          description: Lisätiedon otsikon koodi URI. Viittaa koodistoon
       |        teksti:
       |          type: object
       |          description: Lisätiedon teksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Teksti'
       |    Yhteyshenkilo:
       |      type: object
       |      properties:
       |      nimi:
       |        type: object
       |        description: Yhteyshenkilön nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |        allOf:
       |          - $$ref: '#/components/schemas/Teksti'
       |      titteli:
       |        type: object
       |        description: Yhteyshenkilön titteli eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |        allOf:
       |          - $$ref: '#/components/schemas/Teksti'
       |      sahkoposti:
       |        type: object
       |        description: Yhteyshenkilön sähköpostiosoite eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |        allOf:
       |          - $$ref: '#/components/schemas/Teksti'
       |      puhelinnumero:
       |        type: object
       |        description: Yhteyshenkilön puhelinnumero eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |        allOf:
       |          - $$ref: '#/components/schemas/Teksti'
       |      wwwSivu:
       |        type: object
       |        description: Yhteyshenkilön www-sivu eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |        allOf:
       |          - $$ref: '#/components/schemas/Teksti'
       |    Asiasana:
       |      type: object
       |      properties:
       |        kieli:
       |          type: string
       |          desciption: Asiasanan kieli
       |          allOf:
       |            - $$ref: '#/components/schemas/Kieli'
       |          example: fi
       |        arvo:
       |          type: string
       |          description: Asiasana annetulla kielellä
       |          example: robotiikka
       |    Ammattinimike:
       |      type: object
       |      properties:
       |        kieli:
       |          type: string
       |          desciption: Ammattinimikkeen kieli
       |          allOf:
       |            - $$ref: '#/components/schemas/Kieli'
       |          example: fi
       |        arvo:
       |          type: string
       |          description: Ammattinimike annetulla kielellä
       |          example: insinööri
       |
       |""".stripMargin

  val koulutus =
    s"""    Koulutus:
       |      type: object
       |      properties:
       |        oid:
       |          type: string
       |          description: Koulutuksen yksilöivä tunniste. Järjestelmän generoima.
       |          example: "1.2.246.562.13.00000000000000000009"
       |        johtaaTutkintoon:
       |          type: boolean
       |          description: Onko koulutus tutkintoon johtavaa
       |        koulutustyyppi:
       |          type: string
       |          description: "Koulutuksen tyyppi. Sallitut arvot: 'amm' (ammatillinen), 'yo' (yliopisto), 'lk' (lukio), 'amk' (ammattikorkea), 'muu' (muu koulutus)"
       |          enum:
       |            - amm
       |            - yo
       |            - amk
       |            - lk
       |            - muu
       |          example: amm
       |        koulutusKoodiUri:
       |          type: string
       |          description: Koulutuksen koodi URI. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/koulutus/11)
       |          example: koulutus_371101#1
       |        tila:
       |          type: string
       |          example: "julkaistu"
       |          items:
       |            $$ref: '#/components/schemas/Julkaisutila'
       |          description: Koulutuksen julkaisutila. Jos koulutus on julkaistu, se näkyy oppijalle Opintopolussa.
       |        tarjoajat:
       |          type: array
       |          description: Koulutusta tarjoavien organisaatioiden yksilöivät organisaatio-oidit
       |          items:
       |            type: string
       |          example:
       |            - 1.2.246.562.10.00101010101
       |            - 1.2.246.562.10.00101010102
       |        julkinen:
       |          type: boolean
       |          description: Voivatko muut oppilaitokset käyttää koulutusta
       |        kielivalinta:
       |          type: array
       |          description: Kielet, joille koulutuksen nimi, kuvailutiedot ja muut tekstit on käännetty
       |          items:
       |            $$ref: '#/components/schemas/Kieli'
       |          example:
       |            - fi
       |            - sv
       |        nimi:
       |          type: object
       |          description: Koulutuksen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Nimi'
       |        metadata:
       |          type: object
       |          oneOf:
       |            - $$ref: '#/components/schemas/YliopistoKoulutusMetadata'
       |            - $$ref: '#/components/schemas/AmmatillinenKoulutusMetadata'
       |            - $$ref: '#/components/schemas/AmmattikorkeaKoulutusMetadata'
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
       |          description: Koulutusta viimeksi muokanneen virkailijan henkilö-oid
       |          example: 1.2.246.562.10.00101010101
       |        organisaatioOid:
       |           type: string
       |           description: Koulutuksen luoneen organisaation oid
       |           example: 1.2.246.562.10.00101010101
       |        modified:
       |           type: string
       |           format: date-time
       |           description: Koulutuksen viimeisin muokkausaika. Järjestelmän generoima
       |           example: 2019-08-23T09:55
       |""".stripMargin

  val metadata =
    s"""    KoulutusMetadata:
       |      type: object
       |      properties:
       |        koulutusalaKoodiUrit:
       |          type: array
       |          description: Lista koulutusaloja. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kansallinenkoulutusluokitus2016koulutusalataso2/1)
       |          items:
       |            type: string
       |            example:
       |              - kansallinenkoulutusluokitus2016koulutusalataso2_054#1
       |              - kansallinenkoulutusluokitus2016koulutusalataso2_055#1
       |        kuvaus:
       |          type: object
       |          description: Koulutuksen kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Kuvaus'
       |        lisatiedot:
       |          type: array
       |          description: Koulutukseen liittyviä lisätietoja, jotka näkyvät oppijalle Opintopolussa
       |          items:
       |            type: object
       |            $$ref: '#/components/schemas/Teksti'
       |    KorkeakouluMetadata:
       |      allOf:
       |        - $$ref: '#/components/schemas/KoulutusMetadata'
       |      properties:
       |        kuvauksenNimi:
       |          type: object
       |          description: Koulutuksen kuvaukseni nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Nimi'
       |        tutkintonimikeKoodiUrit:
       |          type: array
       |          description: Lista koulutuksen tutkintonimikkeistä. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/tutkintonimikekk/2)
       |          items:
       |            type: string
       |          example:
       |            - tutkintonimikekk_110#2
       |            - tutkintonimikekk_111#2
       |        opintojenLaajuusKoodiUri:
       |          type: string
       |          description: "Tutkinnon laajuus. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuus/1)"
       |          example: opintojenlaajuus_40#1
       |    YliopistoKoulutusMetadata:
       |      allOf:
       |        - $$ref: '#/components/schemas/KorkeakouluMetadata'
       |        - type: object
       |          properties:
       |            koulutustyyppi:
       |              type: string
       |              description: Koulutuksen metatiedon tyyppi
       |              example: yo
       |              enum:
       |                - yo
       |    AmmattikorkeaKoulutusMetadata:
       |      allOf:
       |        - $$ref: '#/components/schemas/KorkeakouluMetadata'
       |        - type: object
       |          properties:
       |            koulutustyyppi:
       |              type: string
       |              description: Koulutuksen metatiedon tyyppi
       |              example: amk
       |              enum:
       |                - amk
       |    AmmatillinenKoulutusMetadata:
       |      allOf:
       |        - $$ref: '#/components/schemas/KoulutusMetadata'
       |        - type: object
       |          properties:
       |            koulutustyyppi:
       |              type: string
       |              description: Koulutuksen metatiedon tyyppi
       |              example: amm
       |              enum:
       |                - amm
       |""".stripMargin


/*
  val common =
    s"""
       |    Perustiedot:
       |      type: object
       |      properties:
       |        tila: Julkaisutila
       |        nimi: Kielistetty
       |        muokkaaja: UserOid
       |        kielivalinta: Seq[Kieli]
       |        organisaatioOid: OrganisaatioOid
       |        modified: Option[LocalDateTime]
       |""".stripMargin

  val foo =
    s"""    Julkaisutila:
       |      type: string
       |      example: "julkaistu"
       |      enum:
       |        - julkaistu
       |        - tallennettu
       |        - arkistoitu
       |""".stripMargin
 */





  @ApiModel(description = "Koulutus")
  case class Koulutus(@ApiModelProperty(description = "Koulutuksen yksilöivä tunniste. Palvelun generoima.", example = "1.2.246.562.13.00000000000000000009") oid: String,
                      @ApiModelProperty(description = "Onko koulutus tutkintoon johtavaa") johtaaTutkintoon: Boolean,
                      @ApiModelProperty(description = "Koulutuksen tyyppi. Sallitut arvot: 'amm' (ammatillinen), 'yo' (yliopisto), 'lk' (lukio), 'amk' (ammattikorkea), 'muu' (muu koulutus)", example = "amm") koulutustyyppi: String,
                      @ApiModelProperty(description = "Koulutuksen koodi URI. Viittaa koodistoon https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/koulutus/11.", example = "koulutus_371101#1") koulutusKoodiUri: String,
                      @ApiModelProperty(description = "Koulutuksen tila. Sallitut arvot: 'julkaistu', 'tallennettu' ja 'arkistoitu'. Julkaistu koulutus näkyy Opintopolussa oppijoille.", example = "julkaistu") tila: String,
                      @ApiModelProperty(description = "Lista koulutusta tarjoavien organisaatioiden oideista.", example = "[\"1.2.246.562.10.00101010101\", \"1.2.246.562.10.00101010102\"]") tarjoajat: List[String],
                      @ApiModelProperty(description = "Koulutuksen nimi") nimi:  KoulutusNimi,
                      metadata: KoulutusMetadata,
                      @ApiModelProperty(description = "Voivatko muut oppilaitokset käyttää koulutusta", required = false) julkinen: Boolean,
                      @ApiModelProperty(description = "Koulutusta viimeksi muokanneen virkailijan henkilö-oid.", example = "1.2.246.562.10.00101010101") muokkaaja: String,
                      @ApiModelProperty(description = "Koulutuksen luoneen organisaation oid", example = "1.2.246.562.10.00101010101") organisaatioOid: String,
                      @ApiModelProperty(description = "Kielet, joille koulutuksen nimi, kuvailutiedot ja muut tekstit on käännetty. Sallitut arvot: 'fi', 'sv' ja 'en'", example ="[\"fi\", \"sv\"]") kielivalinta: List[String],
                      @ApiModelProperty(description = "Koulutuksen viimeisin muokkausaika. Järjestelmän generoima.", example = "2019-08-23T09:55", required = false) modified: LocalDateTime)

  @ApiModel(description = "Koulutuksen nimi")
  case class KoulutusNimi(@ApiModelProperty(description = "Suomenkielinen käännös, jos kielivalinnassa on 'fi'", example = "Koulutuksen suomenkielinen nimi", required = false) fi: String,
                          @ApiModelProperty(description = "Ruotsinkielinen käännös, jos kielivalinnassa on 'fi'", example = "Koulutuksen ruotsinkielinen nimi", required = false) sv: String,
                          @ApiModelProperty(description = "Englanninkielinen käännös, jos kielivalinnassa on 'fi'", example = "Koulutuksen englanninkielinen nimi", required = false) en: String)

  @ApiModel(description = "Koulutuksen oppijalle näytettävät kuvailutiedot")
  case class KoulutusMetadata(@ApiModelProperty(description = "Kuvailutiedon tyyppi. Yleensä sama kuin koulutuksen tyyppi", example = "amm") tyyppi: String,
                              @ApiModelProperty(description = "Koulutuksen kuvaus") kuvaus: KoulutusKuvaus,
                              @ApiModelProperty(description = "Koulutuksen kuvauksen nimi ('amk', 'yo')", required = false) kuvauksenNimi: KoulutusNimi,
                              @ApiModelProperty(description = "Lista koulutuksen tutkintonimikkeistä ('amk', 'yo')'. Viittaa koodistoon https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/tutkintonimikekk/2", example = "[\"tutkintonimikekk_110#2\"]", required = false) tutkintonimikeKoodiUrit: Seq[String],
                              @ApiModelProperty(description = "Opintojen laajuus ('amk', 'yo')'. Viittaa koodistoon https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuus/1", example = "opintojenlaajuus_40#1", required = false) opintojenLaajuusKoodiUri: Seq[String],
                              @ApiModelProperty(description = "Lista koulutustalojen koodeja. Viittaa koodistoon https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kansallinenkoulutusluokitus2016koulutusalataso2/1", example = "[\"kansallinenkoulutusluokitus2016koulutusalataso2_054#1\"]",  required = false ) koulutusalaKoodiUrit: Seq[String])

  @ApiModel(description = "Koulutuksen kuvaus")
  case class KoulutusKuvaus(@ApiModelProperty(description = "Suomenkielinen käännös, jos kielivalinnassa on 'fi'", example = "Koulutuksen suomenkielinen kuvaus", required = false) fi: String,
                            @ApiModelProperty(description = "Ruotsinkielinen käännös, jos kielivalinnassa on 'sv'", example = "Koulutuksen ruotsinkielinen kuvaus", required = false) sv: String,
                            @ApiModelProperty(description = "Englanninkielinen käännös, jos kielivalinnassa on 'en'", example = "Koulutuksen englanninkielinen kuvaus", required = false) en: String)

  @ApiModel(description = "Koulutuksen kuvauksen nimi")
  case class KoulutusKuvausNimi(@ApiModelProperty(description = "Suomenkielinen käännös, jos kielivalinnassa on 'fi'", example = "Koulutuksen kuvauksen suomenkielinen nimi", required = false) fi: String,
                                @ApiModelProperty(description = "Ruotsinkielinen käännös, jos kielivalinnassa on 'sv'", example = "Koulutuksen kuvauksen ruotsinkielinen nimi", required = false) sv: String,
                                @ApiModelProperty(description = "Englanninkielinen käännös, jos kielivalinnassa on 'en'", example = "Koulutuksen kuvauksen englanninkielinen nimi", required = false) en: String)

  @ApiModel(description = "Koulutuslistan elementti")
  case class KoulutusListItem(@ApiModelProperty(description = "Koulutuksen luoneen organisaation oid", example = "1.2.246.562.10.00101010101") organisaatioOid: String,
                              @ApiModelProperty(description = "Koulutuksen tila. Sallitut arvot: 'julkaistu', 'tallennettu' ja 'arkistoitu'. Julkaistu koulutus näkyy Opintopolussa oppijoille.", example = "julkaistu") tila: String,
                              @ApiModelProperty(description = "Koulutuksen yksilöivä tunniste. Palvelun generoima.", example = "1.2.246.562.13.00000000000000000009") oid: String,
                              @ApiModelProperty(description = "Koulutuksen viimeisin muokkausaika. Järjestelmän generoima.", example = "2019-08-23T09:55") modified: LocalDateTime,
                              @ApiModelProperty(description = "Koulutuksen nimi") nimi:  KoulutusNimi,
                              @ApiModelProperty(description = "Koulutusta viimeksi muokanneen virkailijan henkilö-oid.", example = "1.2.246.562.24.33333333333") muokkaaja: String)
}