package fi.oph.kouta.domain

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, ToteutusOid, UserOid}
import fi.oph.kouta.validation.IsValid
import fi.oph.kouta.validation.Validations._

package object toteutus {

  val ToteutusModel =
    """    Toteutus:
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
      |        esikatselu:
      |          type: boolean
      |          description: Onko toteutus nähtävissä esikatselussa
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
      |            $ref: '#/components/schemas/Kieli'
      |          example:
      |            - fi
      |            - sv
      |        nimi:
      |          type: object
      |          description: Toteutuksen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Nimi'
      |        metadata:
      |          type: object
      |          oneOf:
      |            - $ref: '#/components/schemas/YliopistoToteutusMetadata'
      |            - $ref: '#/components/schemas/AmmatillinenToteutusMetadata'
      |            - $ref: '#/components/schemas/AmmattikorkeaToteutusMetadata'
      |            - $ref: '#/components/schemas/AmmatillinenTutkinnonOsaToteutusMetadata'
      |            - $ref: '#/components/schemas/AmmatillinenOsaamisalaToteutusMetadata'
      |          example:
      |            tyyppi: amm
      |            kuvaus:
      |              fi: Suomenkielinen kuvaus
      |              sv: Ruotsinkielinen kuvaus
      |            osaamisalat:
      |              - koodiUri: osaamisala_0001#1
      |                linkki:
      |                  fi: http://osaamisala.fi/linkki/fi
      |                  sv: http://osaamisala.fi/linkki/sv
      |                otsikko:
      |                  fi: Katso osaamisalan tarkempi kuvaus tästä
      |                  sv: Katso osaamisalan tarkempi kuvaus tästä ruotsiksi
      |            opetus:
      |              opetuskieliKoodiUrit:
      |                - oppilaitoksenopetuskieli_1#1
      |              opetuskieletKuvaus:
      |                fi: Opetuskielen suomenkielinen kuvaus
      |                sv: Opetuskielen ruotsinkielinen kuvaus
      |              opetusaikaKoodiUrit:
      |                - opetusaikakk_1#1
      |              opetusaikaKuvaus:
      |                fi: Opetusajan suomenkielinen kuvaus
      |                sv: Opetusajan ruotsinkielinen kuvaus
      |              opetustapaKoodiUrit:
      |                - opetuspaikkakk_1#1
      |                - opetuspaikkakk_2#1
      |              opetustapaKuvaus:
      |                fi: Opetustavan suomenkielinen kuvaus
      |                sv: Opetustavan ruotsinkielinen kuvaus
      |              onkoMaksullinen: true
      |              maksullisuusKuvaus:
      |                fi: Maksullisuuden suomenkielinen kuvaus
      |                sv: Maksullisuuden ruotsinkielinen kuvaus
      |              maksunMaara: 200.50
      |              alkamiskausiKoodiUri: kausi_k#1
      |              alkamisvuosi : 2020
      |              alkamisaikaKuvaus:
      |                fi: Alkamisajan suomenkielinen kuvaus
      |                sv: Alkamisajan ruotsinkielinen kuvaus
      |              lisatiedot:
      |                - otsikkoKoodiUri: koulutuksenlisatiedot_03#1
      |                  teksti:
      |                    fi: Suomenkielinen lisätietoteksti
      |                    sv: Ruotsinkielinen lisätietoteksti
      |              onkoLukuvuosimaksua: true
      |              lukuvuosimaksu:
      |                 fi: 200 lukukaudessa
      |                 sv: 200 på svenska
      |              lukuvuosimaksuKuvaus:
      |                fi: Lukuvuosimaksun suomenkielinen kuvaus
      |                sv: Lukuvuosimaksun ruotsinkielinen kuvaus
      |              onkoStipendia: true
      |              stipendinMaara:
      |                 fi: 200 lukukaudessa
      |                 sv: 200 på svenska
      |              stipendinKuvaus:
      |                fi: Stipendin suomenkielinen kuvaus
      |                sv: Stipendin ruotsinkielinen kuvaus
      |            ammattinimikkeet:
      |              - kieli: fi
      |                arvo: insinööri
      |              - kieli: en
      |                arvo: engineer
      |            asiasanat:
      |              - kieli: fi
      |                arvo: ravintotiede
      |              - kieli: en
      |                arvo: nutrition
      |            yhteyshenkilot:
      |              - nimi:
      |                  fi: Aku Ankka
      |                  sv: Kalle Ankka
      |                titteli:
      |                  fi: Ankka
      |                  sv: Ankka ruotsiksi
      |                sahkoposti:
      |                  fi: aku.ankka@ankkalinnankoulu.fi
      |                  sv: aku.ankka@ankkalinnankoulu.fi
      |                puhelinnumero:
      |                  fi: 123
      |                  sv: 223
      |                wwwSivu:
      |                  fi: http://opintopolku.fi
      |                  sv: http://studieinfo.fi
      |        muokkaaja:
      |          type: string
      |          description: Toteutusta viimeksi muokanneen virkailijan henkilö-oid
      |          example: 1.2.246.562.10.00101010101
      |        organisaatioOid:
      |           type: string
      |           description: Toteutuksen luoneen organisaation oid
      |           example: 1.2.246.562.10.00101010101
      |        teemakuva:
      |          type: string
      |          description: Toteutuksen Opintopolussa näytettävän teemakuvan URL.
      |          example: https://konfo-files.opintopolku.fi/toteutus-teemakuva/1.2.246.562.13.00000000000000000009/f4ecc80a-f664-40ef-98e6-eaf8dfa57f6e.png
      |        modified:
      |           type: string
      |           format: date-time
      |           description: Toteutuksen viimeisin muokkausaika. Järjestelmän generoima
      |           example: 2019-08-23T09:55
      |""".stripMargin

  val ToteutusListItemModel =
    """    ToteutusListItem:
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
      |        tarjoajat:
      |          type: array
      |          description: Koulutusta tarjoavien organisaatioiden yksilöivät organisaatio-oidit
      |          items:
      |            type: string
      |          example:
      |            - 1.2.246.562.10.00101010101
      |            - 1.2.246.562.10.00101010102
      |        nimi:
      |          type: object
      |          description: Toteutuksen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty toteutuksen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Nimi'
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
                    esikatselu: Boolean = false,
                    tarjoajat: List[OrganisaatioOid] = List(),
                    nimi: Kielistetty = Map(),
                    metadata: Option[ToteutusMetadata] = None,
                    sorakuvausId: Option[UUID] = None,
                    muokkaaja: UserOid,
                    organisaatioOid: OrganisaatioOid,
                    kielivalinta: Seq[Kieli] = Seq(),
                    teemakuva: Option[String] = None,
                    modified: Option[LocalDateTime])
  extends PerustiedotWithOid[ToteutusOid, Toteutus] with HasTeemakuva[Toteutus] {

  override def validate(): IsValid = and(
    super.validate(),
    assertValid(koulutusOid, "koulutusOid"),
    validateOidList(tarjoajat, "tarjoajat"),
    validateIfDefined[ToteutusMetadata](metadata, _.validate(tila, kielivalinta, "metadata")),
    validateIfDefined[String](teemakuva, assertValidUrl(_, "teemakuva")),
    validateIfJulkaistu(tila, assertNotOptional(metadata, "metadata")),
    validateIfTrue(!metadata.exists(_.allowSorakuvaus), assertNotDefined(sorakuvausId, "sorakuvausId"))
  )

  override def validateOnJulkaisu(): IsValid =
    validateIfDefined[ToteutusMetadata](metadata, _.validateOnJulkaisu("metadata"))

  def withOid(oid: ToteutusOid): Toteutus = copy(oid = Some(oid))

  override def withTeemakuva(teemakuva: Option[String]): Toteutus = this.copy(teemakuva = teemakuva)

  override def withModified(modified: LocalDateTime): Toteutus = copy(modified = Some(modified))

  def withMuokkaaja(oid: UserOid): Toteutus = this.copy(muokkaaja = oid)
}

case class ToteutusListItem(oid: ToteutusOid,
                            koulutusOid: KoulutusOid,
                            nimi: Kielistetty,
                            tila: Julkaisutila,
                            tarjoajat: List[OrganisaatioOid],
                            organisaatioOid: OrganisaatioOid,
                            muokkaaja: UserOid,
                            modified: LocalDateTime) extends OidListItem
