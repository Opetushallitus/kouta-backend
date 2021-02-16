package fi.oph.kouta.domain

import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, RootOrganisaatioOid, UserOid}
import fi.oph.kouta.security.AuthorizableMaybeJulkinen
import fi.oph.kouta.validation.IsValid
import fi.oph.kouta.validation.Validations.{validateIfTrue, _}

package object koulutus {

  val KoulutusModel =
    """    Koulutus:
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
      |          description: "Koulutuksen tyyppi. Sallitut arvot: 'amm' (ammatillinen), 'yo' (yliopisto), 'lk' (lukio), 'amk' (ammattikorkea), 'amm-tutkinnon-osa', 'amm-osaamisala'"
      |          enum:
      |            - amm
      |            - yo
      |            - amk
      |            - lk
      |            - amm-tutkinnon-osa
      |            - amm-osaamisala
      |          example: amm
      |        koulutusKoodiUri:
      |          type: string
      |          description: Koulutuksen koodi URI. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/koulutus/11)
      |          example: koulutus_371101#1
      |        tila:
      |          type: string
      |          example: "julkaistu"
      |          enum:
      |            - julkaistu
      |            - arkistoitu
      |            - tallennettu
      |          description: Koulutuksen julkaisutila. Jos koulutus on julkaistu, se näkyy oppijalle Opintopolussa.
      |        esikatselu:
      |          type: boolean
      |          description: Onko koulutus nähtävissä esikatselussa
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
      |            $ref: '#/components/schemas/Kieli'
      |          example:
      |            - fi
      |            - sv
      |        nimi:
      |          type: object
      |          description: Koulutuksen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Nimi'
      |        metadata:
      |          type: object
      |          oneOf:
      |            - $ref: '#/components/schemas/YliopistoKoulutusMetadata'
      |            - $ref: '#/components/schemas/AmmatillinenKoulutusMetadata'
      |            - $ref: '#/components/schemas/AmmattikorkeaKoulutusMetadata'
      |            - $ref: '#/components/schemas/AmmatillinenTutkinnonOsaKoulutusMetadata'
      |            - $ref: '#/components/schemas/AmmatillinenOsaamisalaKoulutusMetadata'
      |          example:
      |            koulutustyyppi: amm
      |            koulutusalaKoodiUrit:
      |              - kansallinenkoulutusluokitus2016koulutusalataso2_054#1
      |              - kansallinenkoulutusluokitus2016koulutusalataso2_055#1
      |            kuvaus:
      |              fi: Suomenkielinen kuvaus
      |              sv: Ruotsinkielinen kuvaus
      |            lisatiedot:
      |              - otsikkoKoodiUri: koulutuksenlisatiedot_03#1
      |                teksti:
      |                  fi: Opintojen suomenkielinen lisätietokuvaus
      |                  sv: Opintojen ruotsinkielinen lisätietokuvaus
      |        muokkaaja:
      |          type: string
      |          description: Koulutusta viimeksi muokanneen virkailijan henkilö-oid
      |          example: 1.2.246.562.10.00101010101
      |        organisaatioOid:
      |           type: string
      |           description: Koulutuksen luoneen organisaation oid
      |           example: 1.2.246.562.10.00101010101
      |        teemakuva:
      |          type: string
      |          description: Koulutuksen Opintopolussa näytettävän teemakuvan URL.
      |          example: https://konfo-files.opintopolku.fi/koulutus-teemakuva/1.2.246.562.13.00000000000000000009/f4ecc80a-f664-40ef-98e6-eaf8dfa57f6e.png
      |        ePerusteId:
      |          type: number
      |          description: Koulutuksen käyttämän ePerusteen id.
      |          example: 4804100
      |        modified:
      |           type: string
      |           format: date-time
      |           description: Koulutuksen viimeisin muokkausaika. Järjestelmän generoima
      |           example: 2019-08-23T09:55:17
      |""".stripMargin

  val KoulutusListItemModel =
    """    KoulutusListItem:
      |      type: object
      |      properties:
      |        oid:
      |          type: string
      |          description: Koulutuksen yksilöivä tunniste. Järjestelmän generoima.
      |          example: "1.2.246.562.13.00000000000000000009"
      |        tila:
      |          type: string
      |          example: "julkaistu"
      |          enum:
      |            - julkaistu
      |            - arkistoitu
      |            - tallennettu
      |          description: Koulutuksen julkaisutila. Jos koulutus on julkaistu, se näkyy oppijalle Opintopolussa.
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
      |          description: Koulutuksen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Nimi'
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
      |           example: 2019-08-23T09:55:17
      |""".stripMargin

  def models = List(KoulutusModel, KoulutusListItemModel)
}

case class Koulutus(oid: Option[KoulutusOid] = None,
                    johtaaTutkintoon: Boolean,
                    koulutustyyppi: Koulutustyyppi,
                    koulutusKoodiUri: Option[String] = None,
                    tila: Julkaisutila = Tallennettu,
                    esikatselu: Boolean = false,
                    tarjoajat: List[OrganisaatioOid] = List(),
                    nimi: Kielistetty = Map(),
                    metadata: Option[KoulutusMetadata] = None,
                    julkinen: Boolean = false,
                    muokkaaja: UserOid,
                    organisaatioOid: OrganisaatioOid,
                    kielivalinta: Seq[Kieli] = Seq(),
                    teemakuva: Option[String] = None,
                    ePerusteId: Option[Long] = None,
                    modified: Option[Modified])
  extends PerustiedotWithOid[KoulutusOid, Koulutus] with HasTeemakuva[Koulutus] with AuthorizableMaybeJulkinen[Koulutus] {

  override def validate(): IsValid = {
    and(super.validate(),
      validateOidList(tarjoajat, "tarjoajat"),
      validateIfDefined[String](koulutusKoodiUri, assertMatch(_, KoulutusKoodiPattern, "koulutusKoodiUri")),
      validateIfDefined[KoulutusMetadata](metadata, _.validate(tila, kielivalinta, "metadata")),
      validateIfDefined[KoulutusMetadata](metadata, m => assertTrue(m.tyyppi == koulutustyyppi, s"metadata.tyyppi", InvalidMetadataTyyppi)),
      validateIfDefined[Long](ePerusteId, assertNotNegative(_, "ePerusteId")),
      validateIfJulkaistu(tila, and(
        assertTrue(johtaaTutkintoon == Koulutustyyppi.isTutkintoonJohtava(koulutustyyppi), "johtaaTutkintoon", invalidTutkintoonjohtavuus(koulutustyyppi.toString)),
        validateIfTrue(koulutustyyppi != AmmTutkinnonOsa, and(
          validateIfTrue(Koulutustyyppi.isAmmatillinen(koulutustyyppi), assertNotOptional(koulutusKoodiUri, "koulutusKoodiUri")),
          validateIfTrue(Koulutustyyppi.isAmmatillinen(koulutustyyppi), assertNotOptional(ePerusteId, "ePerusteId")))),
        validateIfTrue(koulutustyyppi == AmmTutkinnonOsa, and(
          assertNotDefined(ePerusteId, "ePerusteId"),
          assertNotDefined(koulutusKoodiUri, "koulutusKoodiUri")
        )),
        assertNotOptional(metadata, "metadata"),
        validateIfDefined[String](teemakuva, assertValidUrl(_, "teemakuva")),
        validateIfTrue(!RootOrganisaatioOid.equals(organisaatioOid),
          assertNotEmpty(tarjoajat, "tarjoajat"))
      ))
    )
  }

  def withOid(oid: KoulutusOid): Koulutus = copy(oid = Some(oid))

  override def withTeemakuva(teemakuva: Option[String]): Koulutus = this.copy(teemakuva = teemakuva)

  override def withModified(modified: Modified): Koulutus = this.copy(modified = Some(modified))

  def withMuokkaaja(oid: UserOid): Koulutus = this.copy(muokkaaja = oid)
}

case class KoulutusListItem(oid: KoulutusOid,
                            nimi: Kielistetty,
                            tila: Julkaisutila,
                            tarjoajat: List[OrganisaatioOid],
                            organisaatioOid: OrganisaatioOid,
                            muokkaaja: UserOid,
                            modified: Modified) extends OidListItem
