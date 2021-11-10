package fi.oph.kouta.domain

import java.util.UUID
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.service.HakukohdeService

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
      |              tila:
      |                type: string
      |                example: "julkaistu"
      |                enum:
      |                  - julkaistu
      |                  - arkistoitu
      |                  - tallennettu
      |                  - poistettu
      |                description: Haun julkaisutila. Jos haku on julkaistu, se näkyy oppijalle Opintopolussa.
      |              koulutuksenAlkamiskausi:
      |                type: object
      |                description: Koulutuksen alkamiskausi
      |                $ref: '#/components/schemas/KoulutuksenAlkamiskausi'
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
      |                example: 2019-08-23T09:55:17
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
      |                    tila:
      |                      type: string
      |                      example: "julkaistu"
      |                      enum:
      |                        - julkaistu
      |                        - arkistoitu
      |                        - tallennettu
      |                        - poistettu
      |                      description: Hakukohteen julkaisutila. Jos hakukohde on julkaistu, se näkyy oppijalle Opintopolussa.
      |                    esikatselu:
      |                      type: boolean
      |                      description: Onko hakukohde nähtävissä esikatselussa
      |                    nimi:
      |                      type: object
      |                      description: Hakukohteen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |                      $ref: '#/components/schemas/Nimi'
      |                    koulutuksenAlkamiskausi:
      |                      type: object
      |                      description: Koulutuksen alkamiskausi
      |                      $ref: '#/components/schemas/KoulutuksenAlkamiskausi'
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
      |                      type: object
      |                      description: Hakukohteen aloituspaikat
      |                      $ref: '#/components/schemas/Aloituspaikat'
      |                    hakukohteenLinja:
      |                      type: object
      |                      description: lukiototeutuksen hakukohteen linja
      |                      $ref: '#/components/schemas/HakukohteenLinja'
      |                    kaytetaanHaunAikataulua:
      |                      type: boolean
      |                      description: Käytetäänkö haun hakuaikoja vai onko hakukohteelle määritelty omat hakuajat?
      |                    hakuajat:
      |                      type: array
      |                      description: Hakukohteen hakuajat, jos ei käytetä haun hakuaikoja
      |                      items:
      |                        $ref: '#/components/schemas/Ajanjakso'
      |                    pohjakoulutusvaatimusKoodiUrit:
      |                      type: array
      |                      description: Hakukohteen pohjakoulutusvaatimusKoodiUrit
      |                      items:
      |                        type: string
      |                      example: ["pohjakoulutusvaatimuskouta_pk#1", "pohjakoulutusvaatimuskouta_yo#1"]
      |                    pohjakoulutusvaatimusTarkenne:
      |                      type: object
      |                      description: Hakukohteen pohjakoulutusvaatimuksen tarkennus
      |                      $ref: '#/components/schemas/Kuvaus'
      |                    muokkaaja:
      |                      type: string
      |                      description: Hakukohdetta viimeksi muokanneen virkailijan henkilö-oid
      |                      example: 1.2.246.562.10.00101010101
      |                    organisaatioOid:
      |                       type: string
      |                       description: Hakukohteen luoneen organisaation oid
      |                       example: 1.2.246.562.10.00101010101
      |                    valintatapaKoodiUrit:
      |                       type: array
      |                       description: Hakukohteeseen liitetyn valintaperusteen valintatapojen koodiUrit
      |                       items:
      |                         type: string
      |                       example: ["valintatapajono_av#1", "valintatapajono_tv#1"]
      |                    modified:
      |                       type: string
      |                       format: date-time
      |                       description: Hakukohteen viimeisin muokkausaika. Järjestelmän generoima
      |                       example: 2019-08-23T09:55:17
      |""".stripMargin

  val models = List(HakutietoModel)
}

case class Hakutieto(toteutusOid:ToteutusOid,
                     haut: Seq[HakutietoHaku])

case class HakutietoHaku(hakuOid: HakuOid,
                         nimi: Kielistetty = Map(),
                         hakutapaKoodiUri: Option[String] = None,
                         tila: Julkaisutila = Tallennettu,
                         koulutuksenAlkamiskausi: Option[KoulutuksenAlkamiskausi],
                         hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                         hakulomakeAtaruId: Option[UUID] = None,
                         hakulomakeKuvaus: Kielistetty = Map(),
                         hakulomakeLinkki: Kielistetty = Map(),
                         organisaatioOid: OrganisaatioOid,
                         hakuajat: Seq[Ajanjakso] = Seq(),
                         muokkaaja: UserOid,
                         modified: Option[Modified],
                         hakukohteet: Seq[HakutietoHakukohde])

case class HakutietoHakukohde(hakukohdeOid: HakukohdeOid,
                              toteutusOid: ToteutusOid,
                              hakuOid: HakuOid,
                              nimi: Kielistetty = Map(),
                              hakukohdeKoodiUri: Option[String] = None,
                              tila: Julkaisutila = Tallennettu,
                              esikatselu: Boolean = false,
                              valintaperusteId: Option[UUID] = None,
                              koulutuksenAlkamiskausi: Option[KoulutuksenAlkamiskausi],
                              kaytetaanHaunAlkamiskautta: Option[Boolean] = None,
                              jarjestyspaikkaOid: Option[OrganisaatioOid] = None,
                              hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                              hakulomakeAtaruId: Option[UUID] = None,
                              hakulomakeKuvaus: Kielistetty = Map(),
                              hakulomakeLinkki: Kielistetty = Map(),
                              kaytetaanHaunHakulomaketta: Option[Boolean] = None,
                              aloituspaikat: Option[Aloituspaikat] = None,
                              hakukohteenLinja: Option[HakukohteenLinja] = None,
                              kaytetaanHaunAikataulua: Option[Boolean] = None,
                              hakuajat: Seq[Ajanjakso] = Seq(),
                              pohjakoulutusvaatimusKoodiUrit: Seq[String] = Seq(),
                              pohjakoulutusvaatimusTarkenne: Kielistetty = Map(),
                              muokkaaja: UserOid,
                              organisaatioOid: OrganisaatioOid,
                              valintatapaKoodiUrit: Seq[String] = Seq(),
                              modified: Option[Modified],
                              toteutusMetadata: Option[ToteutusMetadata] = None)

object HakutietoHakukohde {
  def apply(e: HakutietoHakukohdeEnriched): HakutietoHakukohde = {
    new HakutietoHakukohde(
      e.hakukohdeOid,
      e.toteutusOid,
      e.hakuOid,
      e.nimi,
      e.hakukohdeKoodiUri,
      e.tila,
      e.esikatselu,
      e.valintaperusteId,
      e.koulutuksenAlkamiskausi,
      e.kaytetaanHaunAlkamiskautta,
      e.jarjestyspaikkaOid,
      e.hakulomaketyyppi,
      e.hakulomakeAtaruId,
      e.hakulomakeKuvaus,
      e.hakulomakeLinkki,
      e.kaytetaanHaunHakulomaketta,
      e.aloituspaikat,
      e.hakukohteenLinja,
      e.kaytetaanHaunAikataulua,
      e.hakuajat,
      e.pohjakoulutusvaatimusKoodiUrit,
      e.pohjakoulutusvaatimusTarkenne,
      e.muokkaaja,
      e.organisaatioOid,
      e.valintatapaKoodiUrit,
      e.modified)
    }
  }

case class HakutietoHakukohdeEnriched(
  hakukohdeOid: HakukohdeOid,
  toteutusOid: ToteutusOid,
  hakuOid: HakuOid,
  nimi: Kielistetty = Map(),
  hakukohdeKoodiUri: Option[String] = None,
  tila: Julkaisutila = Tallennettu,
  esikatselu: Boolean = false,
  valintaperusteId: Option[UUID] = None,
  koulutuksenAlkamiskausi: Option[KoulutuksenAlkamiskausi],
  kaytetaanHaunAlkamiskautta: Option[Boolean] = None,
  jarjestyspaikkaOid: Option[OrganisaatioOid] = None,
  hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
  hakulomakeAtaruId: Option[UUID] = None,
  hakulomakeKuvaus: Kielistetty = Map(),
  hakulomakeLinkki: Kielistetty = Map(),
  kaytetaanHaunHakulomaketta: Option[Boolean] = None,
  aloituspaikat: Option[Aloituspaikat] = None,
  hakukohteenLinja: Option[HakukohteenLinja] = None,
  kaytetaanHaunAikataulua: Option[Boolean] = None,
  hakuajat: Seq[Ajanjakso] = Seq(),
  pohjakoulutusvaatimusKoodiUrit: Seq[String] = Seq(),
  pohjakoulutusvaatimusTarkenne: Kielistetty = Map(),
  muokkaaja: UserOid,
  organisaatioOid: OrganisaatioOid,
  valintatapaKoodiUrit: Seq[String] = Seq(),
  modified: Option[Modified],
  toteutusMetadata: Option[ToteutusMetadata])

object HakutietoHakukohdeEnriched {
  def apply(
    hakukohdeOid: HakukohdeOid,
    toteutusOid: ToteutusOid,
    hakuOid: HakuOid,
    nimi: Kielistetty,
    hakukohdeKoodiUri: Option[String],
    tila: Julkaisutila,
    esikatselu: Boolean,
    valintaperusteId: Option[UUID] = None,
    koulutuksenAlkamiskausi: Option[KoulutuksenAlkamiskausi],
    kaytetaanHaunAlkamiskautta: Option[Boolean] = None,
    jarjestyspaikkaOid: Option[OrganisaatioOid] = None,
    hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
    hakulomakeAtaruId: Option[UUID] = None,
    hakulomakeKuvaus: Kielistetty = Map(),
    hakulomakeLinkki: Kielistetty = Map(),
    kaytetaanHaunHakulomaketta: Option[Boolean] = None,
    aloituspaikat: Option[Aloituspaikat] = None,
    hakukohteenLinja: Option[HakukohteenLinja] = None,
    kaytetaanHaunAikataulua: Option[Boolean] = None,
    hakuajat: Seq[Ajanjakso] = Seq(),
    pohjakoulutusvaatimusKoodiUrit: Seq[String] = Seq(),
    pohjakoulutusvaatimusTarkenne: Kielistetty = Map(),
    muokkaaja: UserOid,
    organisaatioOid: OrganisaatioOid,
    valintatapaKoodiUrit: Seq[String] = Seq(),
    modified: Option[Modified],
    toteutusMetadata: Option[ToteutusMetadata]
  ): HakutietoHakukohdeEnriched = {
      val esitysnimi = HakukohdeService.generateHakukohdeEsitysnimi(
        Hakukohde(
          oid = Some(hakukohdeOid),
          toteutusOid = toteutusOid,
          hakuOid = hakuOid,
          nimi = nimi,
          muokkaaja = muokkaaja,
          organisaatioOid = organisaatioOid,
          modified = modified),
        toteutusMetadata
      )
      new HakutietoHakukohdeEnriched(
        hakukohdeOid,
        toteutusOid,
        hakuOid,
        esitysnimi,
        hakukohdeKoodiUri,
        tila,
        esikatselu,
        valintaperusteId,
        koulutuksenAlkamiskausi,
        kaytetaanHaunAlkamiskautta,
        jarjestyspaikkaOid,
        hakulomaketyyppi,
        hakulomakeAtaruId, hakulomakeKuvaus,
        hakulomakeLinkki,
        kaytetaanHaunHakulomaketta,
        aloituspaikat,
        hakukohteenLinja,
        kaytetaanHaunAikataulua,
        hakuajat,
        pohjakoulutusvaatimusKoodiUrit,
        pohjakoulutusvaatimusTarkenne,
        muokkaaja,
        organisaatioOid,
        valintatapaKoodiUrit,
        modified,
        toteutusMetadata)
      }
    }
