package fi.oph.kouta.domain

import java.util.UUID
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.security.AuthorizableMaybeJulkinen
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.validation.IsValid
import fi.oph.kouta.validation.Validations._

package object valintaperuste {

  val ValintaperusteModel: String =
    """    Valintaperuste:
      |      type: object
      |      properties:
      |        id:
      |          type: string
      |          description: Valintaperustekuvauksen yksilöivä tunniste. Järjestelmän generoima.
      |          example: "ea596a9c-5940-497e-b5b7-aded3a2352a7"
      |        externalId:
      |          type: string
      |          description: Ulkoinen tunniste jota voidaan käyttää Kouta lomakkeiden mäppäykseen oppilaitosten omien tietojärjestelmien kanssa
      |        tila:
      |          type: string
      |          example: "julkaistu"
      |          enum:
      |            - julkaistu
      |            - arkistoitu
      |            - tallennettu
      |            - poistettu
      |          description: Valintaperustekuvauksen julkaisutila. Jos kuvaus on julkaistu, se näkyy oppijalle Opintopolussa.
      |        esikatselu:
      |          type: boolean
      |          description: Onko valintaperuste nähtävissä esikatselussa
      |        koulutustyyppi:
      |          type: string
      |          description: Minkä tyyppisille koulutuksille valintaperustekuvaus on tarkoitettu käytettäväksi?
      |          enum:
      |            - amm
      |            - yo
      |            - amk
      |            - kk-opintojakso
      |            - lk
      |            - amm-tutkinnon-osa
      |            - amm-osaamisala
      |            - amm-muu
      |            - tuva
      |            - telma
      |            - vapaa-sivistystyo-opistovuosi
      |            - vapaa-sivistystyo-muu
      |            - aikuisten-perusopetus
      |          example: amm
      |        hakutapaKoodiUri:
      |          type: string
      |          description: Valintaperustekuvaukseen liittyvä hakutapa. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/hakutapa/11)
      |          example: hakutapa_03#1
      |        kohdejoukkoKoodiUri:
      |          type: string
      |          description: Valintaperustekuvaukseen liittyvä kohdejoukko. Valintaperusteen ja siihen hakukohteen kautta liittyvän haun kohdejoukon tulee olla sama. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/haunkohdejoukko/1)
      |          example: haunkohdejoukko_17#1
      |        sorakuvausId:
      |          type: string
      |          deprecated: true
      |          description: Valintaperustekuvaukseen liittyvän SORA-kuvauksen yksilöivä tunniste
      |          example: "ea596a9c-5940-497e-b5b7-aded3a2352a7"
      |        julkinen:
      |          type: boolean
      |          description: Voivatko muut oppilaitokset käyttää valintaperustekuvausta
      |        kielivalinta:
      |          type: array
      |          description: Kielet, joille valintaperustekuvauksen nimi, kuvailutiedot ja muut tekstit on käännetty
      |          items:
      |            $ref: '#/components/schemas/Kieli'
      |          example:
      |            - fi
      |            - sv
      |        nimi:
      |          type: object
      |          description: Valintaperustekuvauksen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Nimi'
      |        valintakokeet:
      |          type: array
      |          description: Hakuun liittyvät valintakokeet
      |          items:
      |            $ref: '#/components/schemas/Valintakoe'
      |        metadata:
      |          type: object
      |          $ref: '#/components/schemas/ValintaperusteMetadata'
      |          example:
      |            tyyppi: amm
      |            valintatavat:
      |              - valintatapaKoodiUri: valintatapajono_tv#1
      |                kuvaus:
      |                  fi: Valintatavan suomenkielinen kuvaus
      |                  sv: Valintatavan ruotsinkielinen kuvaus
      |                sisalto:
      |                  - tyyppi: teksti
      |                    data:
      |                      fi: Suomenkielinen sisältöteksti
      |                      sv: Ruotsinkielinen sisältöteksti
      |                  - tyyppi: taulukko
      |                    data:
      |                      nimi:
      |                        fi: Taulukon nimi suomeksi
      |                        sv: Taulukon nimi ruotsiksi
      |                      rows:
      |                        - index: 0
      |                          isHeader: true
      |                          columns:
      |                            - index: 0
      |                              text:
      |                                fi: Otsikko suomeksi
      |                                sv: Otsikko ruotsiksi
      |                kaytaMuuntotaulukkoa: true
      |                kynnysehto:
      |                  fi: Kynnysehto suomeksi
      |                  sv: Kynnysehto ruotsiksi
      |                enimmaispisteet: 18.1
      |                vahimmaispisteet: 10.1
      |            koulutusalaKoodiUrit:
      |              - kansallinenkoulutusluokitus2016koulutusalataso2_054#1
      |              - kansallinenkoulutusluokitus2016koulutusalataso2_055#1
      |            kuvaus:
      |              fi: Suomenkielinen kuvaus
      |              sv: Ruotsinkielinen kuvaus
      |        muokkaaja:
      |          type: string
      |          description: Valintaperustekuvausta viimeksi muokanneen virkailijan henkilö-oid
      |          example: 1.2.246.562.10.00101010101
      |        organisaatioOid:
      |           type: string
      |           description: Valintaperustekuvauksen luoneen organisaation oid
      |           example: 1.2.246.562.10.00101010101
      |        modified:
      |           type: string
      |           format: date-time
      |           description: Valintaperustekuvauksen viimeisin muokkausaika. Järjestelmän generoima
      |           example: 2019-08-23T09:55:17
      |""".stripMargin

  val ValintaperusteListItemModel: String =
    """    ValintaperusteListItem:
      |      type: object
      |      properties:
      |        id:
      |          type: string
      |          description: Valintaperustekuvauksen yksilöivä tunniste. Järjestelmän generoima.
      |          example: "ea596a9c-5940-497e-b5b7-aded3a2352a7"
      |        tila:
      |          type: string
      |          example: "julkaistu"
      |          enum:
      |            - julkaistu
      |            - arkistoitu
      |            - tallennettu
      |            - poistettu
      |          description: Valintaperustekuvauksen julkaisutila. Jos kuvaus on julkaistu, se näkyy oppijalle Opintopolussa.
      |        nimi:
      |          type: object
      |          description: Valintaperustekuvauksen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Nimi'
      |        muokkaaja:
      |          type: string
      |          description: Valintaperustekuvausta viimeksi muokanneen virkailijan henkilö-oid
      |          example: 1.2.246.562.10.00101010101
      |        organisaatioOid:
      |           type: string
      |           description: Valintaperustekuvauksen luoneen organisaation oid
      |           example: 1.2.246.562.10.00101010101
      |        modified:
      |           type: string
      |           format: date-time
      |           description: Valintaperustekuvauksen viimeisin muokkausaika. Järjestelmän generoima
      |           example: 2019-08-23T09:55:17
      |""".stripMargin

  def models = List(ValintaperusteModel, ValintaperusteListItemModel)
}

case class Valintaperuste(
    id: Option[UUID] = None,
    externalId: Option[String] = None,
    tila: Julkaisutila = Tallennettu,
    esikatselu: Boolean = false,
    koulutustyyppi: Koulutustyyppi,
    hakutapaKoodiUri: Option[String] = None,
    kohdejoukkoKoodiUri: Option[String] = None,
    nimi: Kielistetty = Map(),
    julkinen: Boolean = false,
    valintakokeet: Seq[Valintakoe] = Seq(),
    metadata: Option[ValintaperusteMetadata] = None,
    organisaatioOid: OrganisaatioOid,
    muokkaaja: UserOid,
    kielivalinta: Seq[Kieli] = Seq(),
    modified: Option[Modified],
    _enrichedData: Option[ValintaperusteEnrichedData] = None
) extends PerustiedotWithId[Valintaperuste]
    with AuthorizableMaybeJulkinen[Valintaperuste] {

  override def validate(): IsValid = and(
    super.validate(),
    assertValid(organisaatioOid, "organisaatioOid"),
    validateIfDefined[String](hakutapaKoodiUri, assertMatch(_, HakutapaKoodiPattern, "hakutapaKoodiUri")),
    validateIfDefined[String](kohdejoukkoKoodiUri, assertMatch(_, KohdejoukkoKoodiPattern, "kohdejoukkoKoodiUri")),
    validateIfNonEmpty[Valintakoe](valintakokeet, "valintakokeet", _.validate(tila, kielivalinta, _)),
    validateIfDefined[ValintaperusteMetadata](metadata, _.validate(tila, kielivalinta, "metadata")),
    validateIfDefined[ValintaperusteMetadata](
      metadata,
      m => assertTrue(m.tyyppi == koulutustyyppi, "koulutustyyppi", InvalidMetadataTyyppi)
    ),
    validateIfJulkaistu(
      tila,
      and(
        assertNotOptional(hakutapaKoodiUri, "hakutapaKoodiUri"),
        assertNotOptional(kohdejoukkoKoodiUri, "kohdejoukkoKoodiUri")
      )
    )
  )

  override def validateOnJulkaisu(): IsValid =
    validateIfNonEmpty[Valintakoe](valintakokeet, "valintakokeet", _.validateOnJulkaisu(_))

  override def withId(id: UUID): Valintaperuste = copy(id = Some(id))

  override def withModified(modified: Modified): Valintaperuste = copy(modified = Some(modified))

  def withMuokkaaja(oid: UserOid): Valintaperuste = this.copy(muokkaaja = oid)

  def getEntityDescriptionAllative(): String = "valintaperusteelle"
}

case class ValintaperusteListItem(
    id: UUID,
    nimi: Kielistetty,
    tila: Julkaisutila,
    organisaatioOid: OrganisaatioOid,
    muokkaaja: UserOid,
    modified: Modified
) extends IdListItem

case class ValintaperusteEnrichedData(muokkaajanNimi: Option[String] = None)

case class ExternalValintaperusteRequest(authenticated: Authenticated, valintaperuste: Valintaperuste)
    extends ExternalRequest
