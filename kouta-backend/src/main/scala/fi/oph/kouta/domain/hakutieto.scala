package fi.oph.kouta.domain

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.domain.oid._

package object hakutieto {
  val HakutietoModel: String =
    """    Hakutieto:
      |      type: object
      |      properties:
      |        toteutusOid:
      |          type: string
      |          description: Toteutuksen yksilöivä tunniste.
      |          example: "1.2.246.562.13.00000000000000000009"
      |        haut:
      |          type: array
      |          items:
      |            type: object
      |            properties:
      |              hakuOid:
      |                type: string
      |                description: Toteutukseen liittyvän haun yksilöivä tunniste.
      |                example: "1.2.246.562.29.00000000000000000009"
      |              nimi:
      |                type: object
      |                description: Haun Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |                $ref: '#/components/schemas/Nimi'
      |              hakutapaKoodiUri:
      |                type: string
      |                description: Haun hakutapa. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/hakutapa/11)
      |                example: hakutapa_03#1
      |              alkamiskausiKoodiUri:
      |                type: string
      |                description: Haun koulutusten alkamiskausi. Hakukohteella voi olla eri alkamiskausi kuin haulla.
      |                  Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kausi/1)
      |                example: kausi_k#1
      |              alkamisvuosi:
      |                type: string
      |                description: Haun koulutusten alkamisvuosi. Hakukohteella voi olla eri alkamisvuosi kuin haulla.
      |                example: 2020
      |              hakulomaketyyppi:
      |                type: string
      |                description: Hakulomakkeen tyyppi. Kertoo, käytetäänkö Atarun (hakemuspalvelun) hakulomaketta, muuta hakulomaketta
      |                  (jolloin voidaan lisätä hakulomakkeeseen linkki) tai onko niin, ettei sähkököistä hakulomaketta ole lainkaan, jolloin sille olisi hyvä lisätä kuvaus.
      |                  Hakukohteella voi olla eri hakulomake kuin haulla.
      |                example: "ataru"
      |                enum:
      |                  - ataru
      |                  - ei sähköistä
      |                  - muu
      |              hakulomakeAtaruId:
      |                type: string
      |                description: Hakulomakkeen yksilöivä tunniste, jos käytössä on Atarun (hakemuspalvelun) hakulomake. Hakukohteella voi olla eri hakulomake kuin haulla.
      |                example: "ea596a9c-5940-497e-b5b7-aded3a2352a7"
      |              hakulomakeKuvaus:
      |                type: object
      |                description: Hakulomakkeen kuvausteksti eri kielillä. Kielet on määritetty haun kielivalinnassa. Hakukohteella voi olla eri hakulomake kuin haulla.
      |                $ref: '#/components/schemas/Kuvaus'
      |              hakulomakeLinkki:
      |                type: object
      |                description: Hakulomakkeen linkki eri kielillä. Kielet on määritetty haun kielivalinnassa. Hakukohteella voi olla eri hakulomake kuin haulla.
      |                $ref: '#/components/schemas/Linkki'
      |              organisaatioOid:
      |                 type: string
      |                 description: Haun luoneen organisaation oid
      |                 example: 1.2.246.562.10.00101010101
      |              hakuajat:
      |                type: array
      |                description: Haun hakuajat. Hakukohteella voi olla omat hakuajat.
      |                items:
      |                  $ref: '#/components/schemas/Ajanjakso'
      |              muokkaaja:
      |                type: string
      |                description: Hakua viimeksi muokanneen virkailijan henkilö-oid
      |                example: 1.2.246.562.10.00101010101
      |              modified:
      |                type: string
      |                format: date-time
      |                description: Haun viimeisin muokkausaika. Järjestelmän generoima
      |                example: 2019-08-23T09:55
      |              hakukohteet:
      |                type: array
      |                description: Hakuun liittyvien hakukohteiden hakutiedot
      |                items:
      |                  type: object
      |                  properties:
      |                    hakukohdeOid:
      |                      type: string
      |                      description: Hakukohteen yksilöivä tunniste. Järjestelmän generoima.
      |                      example: "1.2.246.562.20.00000000000000000009"
      |                    nimi:
      |                      type: object
      |                      description: Hakukohteen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |                      $ref: '#/components/schemas/Nimi'
      |                    alkamiskausiKoodiUri:
      |                      type: string
      |                      description: Hakukohteen koulutusten alkamiskausi, jos ei käytetä haun alkamiskautta.
      |                        Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kausi/1)
      |                      example: kausi_k#1
      |                    alkamisvuosi:
      |                      type: string
      |                      description: Hakukohteen koulutusten alkamisvuosi, jos ei käytetä haun alkamisvuotta
      |                      example: 2020
      |                    kaytetaanHaunAlkamiskautta:
      |                      type: boolean
      |                      description: Käytetäänkö haun alkamiskautta ja -vuotta vai onko hakukohteelle määritelty oma alkamisajankohta?
      |                    jarjestyspaikkaOid:
      |                      type: string
      |                      description: Hakukohteen järjestyspaikan organisaatio
      |                      example: 1.2.246.562.10.00101010101
      |                    hakulomaketyyppi:
      |                      type: string
      |                      description: Hakulomakkeen tyyppi. Kertoo, käytetäänkö Atarun (hakemuspalvelun) hakulomaketta, muuta hakulomaketta
      |                        (jolloin voidaan lisätä hakulomakkeeseen linkki) tai onko niin, ettei sähkököistä hakulomaketta ole lainkaan, jolloin sille olisi hyvä lisätä kuvaus.
      |                      example: "ataru"
      |                      enum:
      |                        - ataru
      |                        - ei sähköistä
      |                        - muu
      |                    hakulomakeAtaruId:
      |                      type: string
      |                      description: Hakulomakkeen yksilöivä tunniste, jos käytössä on Atarun (hakemuspalvelun) hakulomake
      |                      example: "ea596a9c-5940-497e-b5b7-aded3a2352a7"
      |                    hakulomakeKuvaus:
      |                      type: object
      |                      description: Hakulomakkeen kuvausteksti eri kielillä. Kielet on määritetty haun kielivalinnassa.
      |                      $ref: '#/components/schemas/Kuvaus'
      |                    hakulomakeLinkki:
      |                      type: object
      |                      description: Hakulomakkeen linkki eri kielillä. Kielet on määritetty haun kielivalinnassa.
      |                      $ref: '#/components/schemas/Linkki'
      |                    kaytetaanHaunHakulomaketta:
      |                      type: boolean
      |                      description: Käytetäänkö haun hakulomaketta vai onko hakukohteelle määritelty oma hakulomake?
      |                    aloituspaikat:
      |                      type: integer
      |                      description: Hakukohteen aloituspaikkojen lukumäärä
      |                      example: 100
      |                    minAloituspaikat:
      |                      type: integer
      |                      description: Hakukohteen aloituspaikkojen minimimäärä
      |                      example: 75
      |                    maxAloituspaikat:
      |                      type: integer
      |                      description: Hakukohteen aloituspaikkojen maksimimäärä
      |                      example: 110
      |                    ensikertalaisenAloituspaikat:
      |                      type: integer
      |                      description: Hakukohteen ensikertalaisen aloituspaikkojen lukumäärä
      |                      example: 50
      |                    kaytetaanHaunAikataulua:
      |                      type: boolean
      |                      description: Käytetäänkö haun hakuaikoja vai onko hakukohteelle määritelty omat hakuajat?
      |                    hakuajat:
      |                      type: array
      |                      description: Hakukohteen hakuajat, jos ei käytetä haun hakuaikoja
      |                      items:
      |                        $ref: '#/components/schemas/Ajanjakso'
      |                    muokkaaja:
      |                      type: string
      |                      description: Hakukohdetta viimeksi muokanneen virkailijan henkilö-oid
      |                      example: 1.2.246.562.10.00101010101
      |                    organisaatioOid:
      |                       type: string
      |                       description: Hakukohteen luoneen organisaation oid
      |                       example: 1.2.246.562.10.00101010101
      |                    modified:
      |                       type: string
      |                       format: date-time
      |                       description: Hakukohteen viimeisin muokkausaika. Järjestelmän generoima
      |                       example: 2019-08-23T09:55
      |""".stripMargin

  val models = List(HakutietoModel)
}

case class Hakutieto(toteutusOid:ToteutusOid,
                     haut: Seq[HakutietoHaku])

case class HakutietoHaku(hakuOid: HakuOid,
                         nimi: Kielistetty = Map(),
                         hakutapaKoodiUri: Option[String] = None,
                         alkamiskausiKoodiUri: Option[String] = None,
                         alkamisvuosi: Option[String] = None,
                         koulutuksenAlkamiskausi: Option[KoulutuksenAlkamiskausi],
                         hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                         hakulomakeAtaruId: Option[UUID] = None,
                         hakulomakeKuvaus: Kielistetty = Map(),
                         hakulomakeLinkki: Kielistetty = Map(),
                         organisaatioOid: OrganisaatioOid,
                         hakuajat: Seq[Ajanjakso] = Seq(),
                         muokkaaja: UserOid,
                         modified: Option[LocalDateTime],
                         hakukohteet: Seq[HakutietoHakukohde])

case class HakutietoHakukohde(hakukohdeOid: HakukohdeOid,
                              nimi: Kielistetty = Map(),
                              valintaperusteId: Option[UUID] = None,
                              koulutuksenAlkamiskausi: Option[KoulutuksenAlkamiskausi],
                              alkamiskausiKoodiUri: Option[String] = None,
                              alkamisvuosi: Option[String] = None,
                              kaytetaanHaunAlkamiskautta: Option[Boolean] = None,
                              jarjestyspaikkaOid: Option[OrganisaatioOid] = None,
                              hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                              hakulomakeAtaruId: Option[UUID] = None,
                              hakulomakeKuvaus: Kielistetty = Map(),
                              hakulomakeLinkki: Kielistetty = Map(),
                              kaytetaanHaunHakulomaketta: Option[Boolean] = None,
                              aloituspaikat: Option[Int] = None,
                              ensikertalaisenAloituspaikat: Option[Int] = None,
                              kaytetaanHaunAikataulua: Option[Boolean] = None,
                              hakuajat: Seq[Ajanjakso] = Seq(),
                              pohjakoulutusvaatimusKoodiUrit: Seq[String] = Seq(),
                              pohjakoulutusvaatimusTarkenne: Kielistetty = Map(),
                              muokkaaja: UserOid,
                              organisaatioOid: OrganisaatioOid,
                              modified: Option[LocalDateTime])
