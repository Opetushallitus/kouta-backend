package fi.oph.kouta.domain

import java.time.LocalDateTime
import java.util.UUID
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid, UserOid}
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, ValidatableSubEntity}

package object haku {

  val HakuModel: String =
    """    Haku:
      |      type: object
      |      properties:
      |        oid:
      |          type: string
      |          description: Haun yksilöivä tunniste. Järjestelmän generoima.
      |          example: "1.2.246.562.29.00000000000000000009"
      |        externalId:
      |          type: string
      |          description: Ulkoinen tunniste jota voidaan käyttää Kouta lomakkeiden mäppäykseen oppilaitosten omien tietojärjestelmien kanssa
      |        tila:
      |          $ref: '#/components/schemas/Julkaisutila'
      |          description: Haun julkaisutila. Jos haku on julkaistu, se näkyy oppijalle Opintopolussa.
      |        nimi:
      |          type: object
      |          description: Haun Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Nimi'
      |        hakutapaKoodiUri:
      |          type: string
      |          description: Haun hakutapa. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/hakutapa/1)
      |          example: hakutapa_03#1
      |        hakukohteenLiittamisenTakaraja:
      |          type: string
      |          format: date-time
      |          description: Viimeinen ajanhetki, jolloin hakuun saa liittää hakukohteen.
      |            Hakukohteita ei saa lisätä enää sen jälkeen, kun haku on käynnissä.
      |          example: 2019-08-23T09:55
      |        hakukohteenMuokkaamisenTakaraja:
      |          type: string
      |          format: date-time
      |          description: Viimeinen ajanhetki, jolloin hakuun liitettyä hakukohdetta on sallittua muokata.
      |            Hakukohteen tietoja ei saa muokata enää sen jälkeen, kun haku on käynnissä.
      |          example: 2019-08-23T09:55
      |        hakukohteenLiittajaOrganisaatiot:
      |          type: array
      |          description: Hakukohteen liittajaorganisaatioiden oidit
      |          example: [1.2.246.562.10.00101010101, 1.2.246.562.10.00101010102] 
      |        ajastettuJulkaisu:
      |          type: string
      |          format: date-time
      |          description: Ajanhetki, jolloin haku ja siihen liittyvät hakukohteet ja koulutukset julkaistaan
      |            automaattisesti Opintopolussa, jos ne eivät vielä ole julkisia
      |          example: 2019-08-23T09:55
      |        ajastettuHaunJaHakukohteidenArkistointi:
      |          type: string
      |          format: date-time
      |          description: Ajanhetki, jolloin haku ja siihen liittyvät hakukohteet arkistoidaan. Jos tyhjä,
      |            arkistoidaan hakukohteet 10 kuukautta hakuajan päättymisen jälkeen.
      |          example: 2019-08-23T09:55
      |        ajastettuHaunJaHakukohteidenArkistointiAjettu:
      |          type: string
      |          format: date-time
      |          description: Ajanhetki, jolloin haun ja siihen liittyvät hakukohteiden ajastettu arkistointi on
      |            ajettu kyseiselle haulle.
      |          example: 2019-08-23T09:55
      |        kohdejoukkoKoodiUri:
      |          type: string
      |          description: Haun kohdejoukko. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/haunkohdejoukko/1)
      |          example: haunkohdejoukko_17#1
      |        kohdejoukonTarkenneKoodiUri:
      |          type: string
      |          description: Haun kohdejoukon tarkenne. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/haunkohdejoukontarkenne/1)
      |          example: haunkohdejoukontarkenne_1#1
      |        hakulomaketyyppi:
      |          type: string
      |          description: Hakulomakkeen tyyppi. Kertoo, käytetäänkö Atarun (hakemuspalvelun) hakulomaketta, muuta hakulomaketta
      |            (jolloin voidaan lisätä hakulomakkeeseen linkki) tai onko niin, ettei sähkököistä hakulomaketta ole lainkaan, jolloin sille olisi hyvä lisätä kuvaus.
      |            Hakukohteella voi olla eri hakulomake kuin haulla.
      |          example: "ataru"
      |          enum:
      |            - ataru
      |            - ei sähköistä
      |            - muu
      |        hakulomakeAtaruId:
      |          type: string
      |          description: Hakulomakkeen yksilöivä tunniste, jos käytössä on Atarun (hakemuspalvelun) hakulomake. Hakukohteella voi olla eri hakulomake kuin haulla.
      |          example: "ea596a9c-5940-497e-b5b7-aded3a2352a7"
      |        hakulomakeKuvaus:
      |          type: object
      |          description: Hakulomakkeen kuvausteksti eri kielillä. Kielet on määritetty haun kielivalinnassa. Hakukohteella voi olla eri hakulomake kuin haulla.
      |          $ref: '#/components/schemas/Kuvaus'
      |        hakulomakeLinkki:
      |          type: object
      |          description: Hakulomakkeen linkki eri kielillä. Kielet on määritetty haun kielivalinnassa. Hakukohteella voi olla eri hakulomake kuin haulla.
      |          $ref: '#/components/schemas/Linkki'
      |        hakuajat:
      |          type: array
      |          description: Haun hakuajat. Hakukohteella voi olla omat hakuajat.
      |          items:
      |            $ref: '#/components/schemas/Ajanjakso'
      |        metadata:
      |          type: object
      |          $ref: '#/components/schemas/HakuMetadata'
      |        kielivalinta:
      |          type: array
      |          description: Kielet, joille haun nimi, kuvailutiedot ja muut tekstit on käännetty
      |          items:
      |            $ref: '#/components/schemas/Kieli'
      |          example:
      |            - fi
      |            - sv
      |        muokkaaja:
      |          type: string
      |          description: Hakua viimeksi muokanneen virkailijan henkilö-oid
      |          example: 1.2.246.562.10.00101010101
      |        organisaatioOid:
      |           type: string
      |           description: Haun luoneen organisaation oid
      |           example: 1.2.246.562.10.00101010101
      |        modified:
      |           type: string
      |           format: date-time
      |           description: Haun viimeisin muokkausaika. Järjestelmän generoima
      |           example: 2019-08-23T09:55:17
      |""".stripMargin

  val HakuMetadataModel: String =
    """    HakuMetadata:
      |      type: object
      |      properties:
      |        yhteyshenkilot:
      |          type: array
      |          description: Lista haun yhteyshenkilöistä
      |          items:
      |            $ref: '#/components/schemas/Yhteyshenkilo'
      |        tulevaisuudenAikataulu:
      |          type: array
      |          description: Oppijalle Opintopolussa näytettävät haun mahdolliset tulevat hakuajat
      |          items:
      |            $ref: '#/components/schemas/Ajanjakso'
      |        koulutuksenAlkamiskausi:
      |          type: object
      |          description: Koulutuksen alkamiskausi
      |          $ref: '#/components/schemas/KoulutuksenAlkamiskausi'
      |""".stripMargin

  val HakuListItemModel: String =
    """    HakuListItem:
      |      type: object
      |      properties:
      |        oid:
      |          type: string
      |          description: Haun yksilöivä tunniste. Järjestelmän generoima.
      |          example: "1.2.246.562.29.00000000000000000009"
      |        tila:
      |          $ref: '#/components/schemas/Julkaisutila'
      |          description: Haun julkaisutila. Jos haku on julkaistu, se näkyy oppijalle Opintopolussa.
      |        nimi:
      |          type: object
      |          description: Haun Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Nimi'
      |        muokkaaja:
      |          type: string
      |          description: Hakua viimeksi muokanneen virkailijan henkilö-oid
      |          example: 1.2.246.562.10.00101010101
      |        organisaatioOid:
      |           type: string
      |           description: Haun luoneen organisaation oid
      |           example: 1.2.246.562.10.00101010101
      |        modified:
      |           type: string
      |           format: date-time
      |           description: Haun viimeisin muokkausaika. Järjestelmän generoima
      |           example: 2019-08-23T09:55:17
      |""".stripMargin

  def models = List(HakuModel, HakuMetadataModel, HakuListItemModel)
}

case class Haku(oid: Option[HakuOid] = None,
                externalId: Option[String] = None,
                tila: Julkaisutila = Tallennettu,
                nimi: Kielistetty = Map(),
                hakutapaKoodiUri: Option[String] = None,
                hakukohteenLiittamisenTakaraja: Option[LocalDateTime] = None,
                hakukohteenMuokkaamisenTakaraja: Option[LocalDateTime] = None,
                hakukohteenLiittajaOrganisaatiot: List[OrganisaatioOid] = List(),
                ajastettuJulkaisu: Option[LocalDateTime] = None,
                ajastettuHaunJaHakukohteidenArkistointi: Option[LocalDateTime] = None,
                ajastettuHaunJaHakukohteidenArkistointiAjettu: Option[LocalDateTime] = None,
                kohdejoukkoKoodiUri: Option[String] = None,
                kohdejoukonTarkenneKoodiUri: Option[String] = None,
                hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                hakulomakeAtaruId: Option[UUID] = None,
                hakulomakeKuvaus: Kielistetty = Map(),
                hakulomakeLinkki: Kielistetty = Map(),
                metadata: Option[HakuMetadata] = None,
                organisaatioOid: OrganisaatioOid,
                hakuajat: List[Ajanjakso] = List(),
                muokkaaja: UserOid,
                kielivalinta: Seq[Kieli] = Seq(),
                modified: Option[Modified],
                _enrichedData: Option[HakuEnrichedData] = None
               )
  extends PerustiedotWithOid[HakuOid, Haku] {

  def withOid(oid: HakuOid): Haku = copy(oid = Some(oid))

  override def withModified(modified: Modified): Haku = copy(modified = Some(modified))

  def withMuokkaaja(oid: UserOid): Haku = this.copy(muokkaaja = oid)

  def getEntityDescriptionAllative(): String = "haulle"
}

case class HakuListItem(oid: HakuOid,
                        nimi: Kielistetty,
                        tila: Julkaisutila,
                        organisaatioOid: OrganisaatioOid,
                        muokkaaja: UserOid,
                        modified: Modified) extends OidListItem

case class HakuMetadata(yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                        tulevaisuudenAikataulu: Seq[Ajanjakso] = Seq(),
                        koulutuksenAlkamiskausi: Option[KoulutuksenAlkamiskausi],
                        isMuokkaajaOphVirkailija: Option[Boolean])

case class HakuEnrichedData(muokkaajanNimi: Option[String] = None)

case class ExternalHakuRequest(authenticated: Authenticated, haku: Haku) extends ExternalRequest
