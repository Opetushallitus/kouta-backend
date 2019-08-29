package fi.oph.kouta.api

import java.time.LocalDateTime

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

package object toteutus {













  @ApiModel(description = "Koulutuksen toteutus")
  case class Toteutus(@ApiModelProperty(description = "Toteutuksen yksilöivä tunniste. Palvelun generoima.", example = "1.2.246.562.17.00000000000000000009") oid: String,
                      @ApiModelProperty(description = "Toteutukseen liittyvän koulutuksen tunniste.", example = "1.2.246.562.13.00000000000000000009", required = true) koulutusOid: String,
                      @ApiModelProperty(description = "Toteutuksen tila. Sallitut arvot: 'julkaistu', 'tallennettu' ja 'arkistoitu'. Julkaistu toteutus näkyy Opintopolussa oppijoille.", example = "julkaistu") tila: String,
                      @ApiModelProperty(description = "Lista toteutusta tarjoavien organisaatioiden oideista.", example = "1.2.246.562.10.00101010101, 1.2.246.562.10.00101010102") tarjoajat: List[String],
                      @ApiModelProperty(description = "Toteutuksen nimi") nimi:  ToteutusNimi,
                      metadata: ToteutusMetadata,
                      @ApiModelProperty(description = "Toteutusta viimeksi muokanneen virkailijan henkilö-oid.", example = "1.2.246.562.10.00101010101") muokkaaja: String,
                      @ApiModelProperty(description = "Toteutuksen luoneen organisaation oid", example = "1.2.246.562.10.00101010101") organisaatioOid: String,
                      @ApiModelProperty(description = "Kielet, joille toteutuksen nimi, kuvailutiedot ja muut tekstit on käännetty. Sallitut arvot: 'fi', 'sv' ja 'en'", example ="fi, sv") kielivalinta: List[String],
                      @ApiModelProperty(description = "Toteutuksen viimeisin muokkausaika. Järjestelmän generoima.", example = "2019-08-23T09:55", required = false) modified: LocalDateTime)

  @ApiModel(description = "Toteutuksen nimi")
  case class ToteutusNimi(@ApiModelProperty(description = "Suomenkielinen käännös, jos kielivalinnassa on 'fi'", example = "Toteutuksen suomenkielinen nimi", required = false) fi: String,
                          @ApiModelProperty(description = "Ruotsinkielinen käännös, jos kielivalinnassa on 'fi'", example = "Toteutuksen ruotsinkielinen nimi", required = false) sv: String,
                          @ApiModelProperty(description = "Englanninkielinen käännös, jos kielivalinnassa on 'fi'", example = "Toteutuksen englanninkielinen nimi", required = false) en: String)

  @ApiModel(description = "Toteutuksen oppijalle näytettävät kuvailutiedot")
  case class ToteutusMetadata(@ApiModelProperty(description = "Kuvailutiedon tyyppi. Yleensä sama kuin koulutuksen tyyppi", example = "amm") tyyppi: String,
                              @ApiModelProperty(description = "Toteutuksen kuvaus") kuvaus: ToteutusKuvaus,
                              @ApiModelProperty(description = "Toteutuksen oppijalle näytettävät opetustiedot", required = false) opetus: ToteutuksenOpetusMetadata,
                              @ApiModelProperty(description = "Lista ammattinimikkeitä, joiden avulla oppija voi hakea koulutuksia", required = false ) ammattinimikkeet: Seq[Ammattinimike],
                              @ApiModelProperty(description = "Lista asiasanoja, joiden avulla oppija voi hakea koulutuksia", required = false ) asiasanat: Seq[Asiasana],
                              @ApiModelProperty(description = "Toteutuksen yhteyshenkilön tiedot", required = false ) yhteyshenkilo: Yhteyshenkilo,
                              @ApiModelProperty(description = "Lista alemman korkeakoulututkinnon erikoistumisalojen, opintosuuntien, pääaineiden tms. kuvauksista ('amk' ja 'yo')", required = false ) alemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala],
                              @ApiModelProperty(description = "Lista ylemmän korkeakoulututkinnon erikoistumisalojen, opintosuuntien, pääaineiden tms. kuvauksista ('amk' ja 'yo')", required = false ) ylemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala])

  @ApiModel(description = "Toteutuksen oppijalle näytettävät opestustiedot")
  case class ToteutuksenOpetusMetadata()

  @ApiModel(description = "Yhteyshenkilön tiedot")
  case class Yhteyshenkilo()

  @ApiModel(description = "Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. kuvaus")
  case class KorkeakouluOsaamisala(@ApiModelProperty(description = "Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. nimi", example = "{\"fi\" : \"Suomenkielinen nimi\", \"sv\" : \"Ruotsinkielinen nimi\"}")nimi: String,
                                   kuvaus: KorkeakouluOsaamisalaKuvaus,
                                   linkki: KorkeakouluOsaamisalaLinkki,
                                   otsikko: KorkeakouluOsaamisalaOtsikko)

  @ApiModel(description = "Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. nimi")
  case class KorkeakouluOsaamisalaNimi(@ApiModelProperty(description = "Suomenkielinen käännös, jos toteutuksen kielivalinnassa on 'fi'", example = "Erikoistumisalan, opintosuunnan, pääaineen tms. suomenkielinen nimi", required = false) fi: String,
                                       @ApiModelProperty(description = "Ruotsinkielinen käännös, jos toteutuksen kielivalinnassa on 'sv'", example = "Erikoistumisalan, opintosuunnan, pääaineen tms. ruotsinkielinen nimi", required = false) sv: String,
                                       @ApiModelProperty(description = "Englanninkielinen käännös, jos toteutuksen kielivalinnassa on 'en'", example = "Erikoistumisalan, opintosuunnan, pääaineen tms. englanninkielinen nimi", required = false) en: String)

  @ApiModel(description = "Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. kuvaus")
  case class KorkeakouluOsaamisalaKuvaus(@ApiModelProperty(description = "Suomenkielinen käännös, jos toteutuksen kielivalinnassa on 'fi'", example = "Erikoistumisalan, opintosuunnan, pääaineen tms. suomenkielinen kuvaus", required = false) fi: String,
                                         @ApiModelProperty(description = "Ruotsinkielinen käännös, jos toteutuksen kielivalinnassa on 'sv'", example = "Erikoistumisalan, opintosuunnan, pääaineen tms. ruotsinkielinen kuvaus", required = false) sv: String,
                                         @ApiModelProperty(description = "Englanninkielinen käännös, jos toteutuksen kielivalinnassa on 'en'", example = "Erikoistumisalan, opintosuunnan, pääaineen tms. englanninkielinen kuvaus", required = false) en: String)

  @ApiModel(description = "Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. linkki")
  case class KorkeakouluOsaamisalaLinkki(@ApiModelProperty(description = "Suomenkielinen käännös, jos toteutuksen kielivalinnassa on 'fi'", example = "Erikoistumisalan, opintosuunnan, pääaineen tms. suomenkielinen linkki", required = false) fi: String,
                                         @ApiModelProperty(description = "Ruotsinkielinen käännös, jos toteutuksen kielivalinnassa on 'sv'", example = "Erikoistumisalan, opintosuunnan, pääaineen tms. ruotsinkielinen linkki", required = false) sv: String,
                                         @ApiModelProperty(description = "Englanninkielinen käännös, jos toteutuksen kielivalinnassa on 'en'", example = "Erikoistumisalan, opintosuunnan, pääaineen tms. englanninkielinen linkki", required = false) en: String)

  @ApiModel(description = "Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. linkin otsikko")
  case class KorkeakouluOsaamisalaOtsikko(@ApiModelProperty(description = "Suomenkielinen käännös, jos toteutuksen kielivalinnassa on 'fi'", example = "Erikoistumisalan, opintosuunnan, pääaineen tms. suomenkielinen linkin otsikko", required = false) fi: String,
                                          @ApiModelProperty(description = "Ruotsinkielinen käännös, jos toteutuksen kielivalinnassa on 'sv'", example = "Erikoistumisalan, opintosuunnan, pääaineen tms. ruotsinkielinen linkin otsikko", required = false) sv: String,
                                          @ApiModelProperty(description = "Englanninkielinen käännös, jos toteutuksen kielivalinnassa on 'en'", example = "Erikoistumisalan, opintosuunnan, pääaineen tms. englanninkielinen linkin otsikko", required = false) en: String)

  @ApiModel(description = "Toteutukseen liittyvä ammattinimike tietyllä kielellä")
  case class Ammattinimike(@ApiModelProperty(description = "Ammattinimikkeen käännöksen kieli. Sallitut arvot: 'fi', 'sv' ja 'en'.", example = "fi") kieli: String,
                           @ApiModelProperty(description = "Ammattinimike annetulla kielellä.", example = "insinööri") arvo: String)

  @ApiModel(description = "Toteutukseen liittyvä asiasana tietyllä kielellä")
  case class Asiasana(@ApiModelProperty(description = "Asiasanan kieli. Sallitut arvot: 'fi', 'sv' ja 'en'.", example = "fi") kieli: String,
                      @ApiModelProperty(description = "Asiasana annetulla kielellä.", example = "robotiikka") arvo: String)

  @ApiModel(description = "Toteutuksen kuvaus")
  case class ToteutusKuvaus(@ApiModelProperty(description = "Suomenkielinen käännös, jos kielivalinnassa on 'fi'", example = "Toteutuksen suomenkielinen kuvaus", required = false) fi: String,
                            @ApiModelProperty(description = "Ruotsinkielinen käännös, jos kielivalinnassa on 'sv'", example = "Toteutuksen ruotsinkielinen kuvaus", required = false) sv: String,
                            @ApiModelProperty(description = "Englanninkielinen käännös, jos kielivalinnassa on 'en'", example = "Toteutuksen englanninkielinen kuvaus", required = false) en: String)

  @ApiModel(description = "Toteutuslistan elementti")
  case class ToteutusListItem(@ApiModelProperty(description = "Toteutuksen luoneen organisaation oid", example = "1.2.246.562.10.00101010101") organisaatioOid: String,
                              @ApiModelProperty(description = "Toteutuksen tila. Sallitut arvot: 'julkaistu', 'tallennettu' ja 'arkistoitu'. Julkaistu koulutus näkyy Opintopolussa oppijoille.", example = "julkaistu") tila: String,
                              @ApiModelProperty(description = "Toteutuksen yksilöivä tunniste. Palvelun generoima.", example = "1.2.246.562.13.00000000000000000009") oid: String,
                              @ApiModelProperty(description = "Toteutukseen liittyvän koulutuksen tunniste.", example = "1.2.246.562.13.00000000000000000009") koulutusOid: String,
                              @ApiModelProperty(description = "Toteutuksen viimeisin muokkausaika. Järjestelmän generoima.", example = "2019-08-23T09:55") modified: LocalDateTime,
                              @ApiModelProperty(description = "Toteutuksen nimi") nimi:  ToteutusNimi,
                              @ApiModelProperty(description = "Toteutusta viimeksi muokanneen virkailijan henkilö-oid.", example = "1.2.246.562.24.33333333333") muokkaaja: String)
}
