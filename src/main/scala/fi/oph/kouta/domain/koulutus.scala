package fi.oph.kouta.domain

import java.time.LocalDateTime

import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid, UserOid}
import fi.oph.kouta.validation.IsValid
import fi.oph.kouta.security.AuthorizableMaybeJulkinen

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
      |          allOf:
      |            - $ref: '#/components/schemas/Nimi'
      |        metadata:
      |          type: object
      |          oneOf:
      |            - $ref: '#/components/schemas/YliopistoKoulutusMetadata'
      |            - $ref: '#/components/schemas/AmmatillinenKoulutusMetadata'
      |            - $ref: '#/components/schemas/AmmattikorkeaKoulutusMetadata'
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
      |        modified:
      |           type: string
      |           format: date-time
      |           description: Koulutuksen viimeisin muokkausaika. Järjestelmän generoima
      |           example: 2019-08-23T09:55
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
      |          allOf:
      |            - $ref: '#/components/schemas/Nimi'
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

  def models = List(KoulutusModel, KoulutusListItemModel)
}

case class Koulutus(oid: Option[KoulutusOid] = None,
                    johtaaTutkintoon: Boolean,
                    koulutustyyppi: Koulutustyyppi,
                    koulutusKoodiUri: Option[String] = None,
                    tila: Julkaisutila = Tallennettu,
                    tarjoajat: List[OrganisaatioOid] = List(),
                    nimi:  Kielistetty = Map(),
                    metadata: Option[KoulutusMetadata] = None,
                    julkinen: Boolean = false,
                    muokkaaja: UserOid,
                    organisaatioOid: OrganisaatioOid,
                    kielivalinta: Seq[Kieli] = Seq(),
                    modified: Option[LocalDateTime])
  extends PerustiedotWithOid[KoulutusOid, Koulutus] with HasTeemakuvaMetadata[Koulutus, KoulutusMetadata] with AuthorizableMaybeJulkinen {

  override def validate(): IsValid = {
    and(super.validate(),
      validateIfDefined[KoulutusOid](oid, assertValid(_)),
      validateOidList(tarjoajat),
      validateIfDefined[String](koulutusKoodiUri, assertMatch(_, KoulutusKoodiPattern)),
      validateIfDefined[KoulutusMetadata](metadata, _.validate(koulutustyyppi, tila, kielivalinta)),
      validateIfJulkaistu(tila, and(
        assertTrue(koulutustyyppi == Muu | johtaaTutkintoon, invalidTutkintoonjohtavuus(koulutustyyppi.toString)),
        validateIfTrue(koulutustyyppi == Amm, assertNotOptional(koulutusKoodiUri, "koulutusKoodiUri")),
        assertNotOptional(koulutusKoodiUri, "koulutusKoodiUri"),
        assertNotOptional(metadata, "metadata"),
        validateIfTrue(!OrganisaatioOid("1.2.246.562.10.00000000001").equals(organisaatioOid), //TODO: !KoutaConfigurationFactory.configuration.securityConfiguration.rootOrganisaatio.equals(organisaatioOid), (rikkoo mm. indeksoijan testit)
          assertNotEmpty(tarjoajat, MissingTarjoajat)))
      )
    )
  }

  def withOid(oid: KoulutusOid): Koulutus = copy(oid = Some(oid))

  override def withMetadata(metadata: KoulutusMetadata): Koulutus = this.copy(metadata = Some(metadata))

  override def withModified(modified: LocalDateTime): Koulutus = this.copy(modified = Some(modified))
}

case class KoulutusListItem(oid: KoulutusOid,
                            nimi: Kielistetty,
                            tila: Julkaisutila,
                            tarjoajat: List[OrganisaatioOid],
                            organisaatioOid: OrganisaatioOid,
                            muokkaaja: UserOid,
                            modified: LocalDateTime) extends OidListItem
