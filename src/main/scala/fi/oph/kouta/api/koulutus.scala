package fi.oph.kouta.api

import java.time.LocalDateTime


import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

package object koulutus {
  @ApiModel(description = "Koulutus")
  case class Koulutus(@ApiModelProperty(description = "Koulutuksen yksilöivä tunniste. Palvelun generoima.", example = "1.2.246.562.13.00000000000000000009") oid: String,
                      @ApiModelProperty(description = "Onko koulutus tutkintoon johtavaa") johtaaTutkintoon: Boolean,
                      @ApiModelProperty(description = "Koulutuksen tyyppi. Sallitut arvot: 'amm' (ammatillinen), 'yo' (yliopisto), 'lk' (lukio), 'amk' (ammattikorkea), 'muu' (muu koulutus)", example = "amm") koulutustyyppi: String,
                      @ApiModelProperty(description = "Koulutuksen koodi URI. Viittaa koodistoon https://virkailija.hahtuvaopintopolku.fi/koodisto-ui/html/koodisto/koulutus/11.", example = "koulutus_371101#1") koulutusKoodiUri: String,
                      @ApiModelProperty(description = "Koulutuksen tila. Sallitut arvot: 'julkaistu', 'tallennettu' ja 'arkistoitu'. Julkaistu koulutus näkyy Opintopolussa oppijoille.", example = "julkaistu") tila: String,
                      @ApiModelProperty(description = "Lista koulutusta tarjoavien organisaatioiden oideista.", example = "[\"1.2.246.562.10.00101010101\", \"1.2.246.562.10.00101010102\"]") tarjoajat: List[String],
                      @ApiModelProperty(description = "Koulutuksen nimi") nimi:  Nimi,
                      metadata: Metadata,
                      @ApiModelProperty(description = "Voivatko muut oppilaitokset käyttää koulutusta", required = false) julkinen: Boolean,
                      @ApiModelProperty(description = "Koulutusta viimeksi muokanneen virkailijan henkilö-oid.", example = "1.2.246.562.10.00101010101") muokkaaja: String,
                      @ApiModelProperty(description = "Koulutuksen luoneen organisaation oid", example = "1.2.246.562.10.00101010101") organisaatioOid: String,
                      @ApiModelProperty(description = "Kielet, joille koulutuksen nimi, kuvailutiedot ja muut tekstit on käännetty. Sallitut arvot: 'fi', 'sv' ja 'en'", example ="[\"fi\", \"sv\"]") kielivalinta: String,
                      @ApiModelProperty(description = "Koulutuksen viimeisin muokkausaika. Järjestelmän generoima.", example = "2019-08-23T09:55", required = false) modified: LocalDateTime)

  @ApiModel(description = "Koulutuksen nimi")
  case class Nimi(@ApiModelProperty(description = "Suomenkielinen käännös, jos kielivalinnassa on 'fi'", example = "Koulutuksen suomenkielinen nimi", required = false) fi: String,
                  @ApiModelProperty(description = "Ruotsinkielinen käännös, jos kielivalinnassa on 'fi'", example = "Koulutuksen ruotsinkielinen nimi", required = false) sv: String,
                  @ApiModelProperty(description = "Englanninkielinen käännös, jos kielivalinnassa on 'fi'", example = "Koulutuksen englanninkielinen nimi", required = false) en: String)

  @ApiModel(description = "Koulutuksen oppijalle näytettävät kuvailutiedot")
  case class Metadata(@ApiModelProperty(description = "Kuvailutiedon tyyppi. Yleensä sama kuin koulutuksen tyyppi", example = "amm") tyyppi: String,
                      @ApiModelProperty(description = "Koulutuksen kuvaus") kuvaus: Kuvaus,
                      @ApiModelProperty(description = "Koulutuksen kuvauksen nimi ('amk', 'yo')", required = false) kuvauksenNimi: Nimi,
                      @ApiModelProperty(description = "Lista koulutuksen tutkintonimikkeistä ('amk', 'yo')'. Viittaa koodistoon https://virkailija.hahtuvaopintopolku.fi/koodisto-ui/html/koodisto/tutkintonimikekk/2", example = "[\"tutkintonimikekk_110#2\"]", required = false) tutkintonimikeKoodiUrit: Seq[String],
                      @ApiModelProperty(description = "Opintojen laajuus ('amk', 'yo')'. Viittaa koodistoon https://virkailija.hahtuvaopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuus/1", example = "opintojenlaajuus_40#1", required = false) opintojenLaajuusKoodiUri: Seq[String],
                      @ApiModelProperty(description = "Lista koulutustalojen koodeja. Viittaa koodistoon ?", required = false ) koulutusalaKoodiUrit: Seq[String])

  @ApiModel(description = "Koulutuksen kuvaus")
  case class Kuvaus(@ApiModelProperty(description = "Suomenkielinen käännös, jos kielivalinnassa on 'fi'", example = "Koulutuksen suomenkielinen kuvaus", required = false) fi: String,
                    @ApiModelProperty(description = "Ruotsinkielinen käännös, jos kielivalinnassa on 'sv'", example = "Koulutuksen ruotsinkielinen kuvaus", required = false) sv: String,
                    @ApiModelProperty(description = "Englanninkielinen käännös, jos kielivalinnassa on 'en'", example = "Koulutuksen englanninkielinen kuvaus", required = false) en: String)

  @ApiModel(description = "Koulutuksen kuvauksen nimi")
  case class KuvausNimi(@ApiModelProperty(description = "Suomenkielinen käännös, jos kielivalinnassa on 'fi'", example = "Koulutuksen kuvauksen suomenkielinen nimi", required = false) fi: String,
                        @ApiModelProperty(description = "Ruotsinkielinen käännös, jos kielivalinnassa on 'sv'", example = "Koulutuksen kuvauksen ruotsinkielinen nimi", required = false) sv: String,
                        @ApiModelProperty(description = "Englanninkielinen käännös, jos kielivalinnassa on 'en'", example = "Koulutuksen kuvauksen englanninkielinen nimi", required = false) en: String)

  val models = Seq(Koulutus, Nimi, Metadata, Kuvaus, KuvausNimi)
}