package fi.oph.kouta.domain

import java.time.LocalDateTime

import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, ToteutusOid, UserOid}
import fi.oph.kouta.validation.IsValid

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
      |          example:
      |            tyyppi: amm
      |            kuvaus:
      |              fi: Suomenkielinen kuvaus
      |              sv: Ruotsinkielinen kuvaus
      |            osaamisalat:
      |              - koodiUri: osaamisala_0001#1
      |                linkki:
      |                  fi: http://osaamisala/linkki/fi
      |                  sv: http://osaamisala/linkki/sv
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
                    tarjoajat: List[OrganisaatioOid] = List(),
                    nimi: Kielistetty = Map(),
                    metadata: Option[ToteutusMetadata] = None,
                    muokkaaja: UserOid,
                    organisaatioOid: OrganisaatioOid,
                    kielivalinta: Seq[Kieli] = Seq(),
                    modified: Option[LocalDateTime])
  extends PerustiedotWithOid with HasTeemakuvaMetadata[Toteutus, ToteutusMetadata] with HasPrimaryId[ToteutusOid, Toteutus] {

  override def validate(): IsValid = and(
     super.validate(),
     assertValid(koulutusOid),
     validateIfDefined[ToteutusOid](oid, assertValid(_)),
     validateOidList(tarjoajat)
  )

  override def primaryId: Option[ToteutusOid] = oid

  override def withPrimaryID(oid: ToteutusOid): Toteutus = copy(oid = Some(oid))

  override def withMetadata(metadata: ToteutusMetadata): Toteutus = this.copy(metadata = Some(metadata))
}

case class ToteutusListItem(oid: ToteutusOid,
                            koulutusOid: KoulutusOid,
                            nimi: Kielistetty,
                            tila: Julkaisutila,
                            tarjoajat: List[OrganisaatioOid],
                            organisaatioOid: OrganisaatioOid,
                            muokkaaja: UserOid,
                            modified: LocalDateTime) extends OidListItem
