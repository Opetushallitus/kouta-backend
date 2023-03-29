package fi.oph.kouta.domain

import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid,  UserOid}
import fi.oph.kouta.security.AuthorizableMaybeJulkinen
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.MiscUtils.withoutKoodiVersion
import fi.oph.kouta.validation.IsValid

import java.util.UUID

package object koulutus {

  val KoulutusModel: String =
    """    Koulutus:
      |      type: object
      |      properties:
      |        oid:
      |          type: string
      |          description: Koulutuksen yksilöivä tunniste. Järjestelmän generoima.
      |          example: "1.2.246.562.13.00000000000000000009"
      |        externalId:
      |          type: string
      |          description: Ulkoinen tunniste jota voidaan käyttää Kouta lomakkeiden mäppäykseen oppilaitosten omien tietojärjestelmien kanssa
      |        johtaaTutkintoon:
      |          type: boolean
      |          description: Onko koulutus tutkintoon johtavaa
      |        koulutustyyppi:
      |          type: string
      |          description: "Koulutuksen tyyppi. Sallitut arvot:
      |            'amm' (ammatillinen),
      |            'yo' (yliopisto),
      |            'lk' (lukio),
      |            'amk' (ammattikorkea),
      |            'amm-ope-erityisope-ja-opo' (Ammatillinen opettaja-, erityisopettaja ja opinto-ohjaajakoulutus),
      |            'ope-pedag-opinnot' (Opettajien pedagogiset opinnot),
      |            'kk-opintojakso',
      |            'kk-opintokokonaisuus',
      |            'amm-tutkinnon-osa',
      |            'amm-osaamisala',
      |            'amm-muu',
      |            'tuva' (tutkintokoulutukseen valmentava koulutus),
      |            'telma' (työhön ja itsenäiseen elämään valmentava koulutus),
      |            'vapaa-sivistystyo-opistovuosi',
      |            'vapaa-sivistystyo-muu',
      |            'aikuisten-perusopetus',
      |            'taiteen-perusopetus',
      |            'muu'"
      |          $ref: '#/components/schemas/Koulutustyyppi'
      |          example: amm
      |        koulutuksetKoodiUri:
      |          type: array
      |          description: Koulutuksen koodi URIt. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/koulutus/11)
      |          items:
      |            type: string
      |          example:
      |            - koulutus_371101#1
      |            - koulutus_201000#1
      |        tila:
      |          $ref: '#/components/schemas/Julkaisutila'
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
      |        sorakuvausId:
      |          type: string
      |          description: Koulutukseen liittyvän SORA-kuvauksen yksilöivä tunniste
      |          example: "ea596a9c-5940-497e-b5b7-aded3a2352a7"
      |        metadata:
      |          type: object
      |          oneOf:
      |            - $ref: '#/components/schemas/YliopistoKoulutusMetadata'
      |            - $ref: '#/components/schemas/AmmatillinenKoulutusMetadata'
      |            - $ref: '#/components/schemas/AmmattikorkeaKoulutusMetadata'
      |            - $ref: '#/components/schemas/AmmOpeErityisopeJaOpoKoulutusMetadata'
      |            - $ref: '#/components/schemas/OpePedagOpinnotKoulutusMetadata'
      |            - $ref: '#/components/schemas/AmmatillinenTutkinnonOsaKoulutusMetadata'
      |            - $ref: '#/components/schemas/KkOpintojaksoKoulutusMetadata'
      |            - $ref: '#/components/schemas/KkOpintokokonaisuusKoulutusMetadata'
      |            - $ref: '#/components/schemas/AmmatillinenOsaamisalaKoulutusMetadata'
      |            - $ref: '#/components/schemas/AmmatillinenMuuKoulutusMetadata'
      |            - $ref: '#/components/schemas/LukioKoulutusMetadata'
      |            - $ref: '#/components/schemas/TuvaKoulutusMetadata'
      |            - $ref: '#/components/schemas/TelmaKoulutusMetadata'
      |            - $ref: '#/components/schemas/VapaaSivistystyoKoulutusMetadata'
      |            - $ref: '#/components/schemas/AikuistenPerusopetusKoulutusMetadata'
      |            - $ref: '#/components/schemas/ErikoislaakariKoulutusMetadata'
      |            - $ref: '#/components/schemas/ErikoistumiskoulutusMetadata'
      |            - $ref: '#/components/schemas/TaiteenPerusopetusKoulutusMetadata'
      |            - $ref: '#/components/schemas/MuuKoulutusMetadata'
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
      |          type: integer
      |          description: Koulutuksen käyttämän ePerusteen id.
      |          example: 4804100
      |        modified:
      |           type: string
      |           format: date-time
      |           description: Koulutuksen viimeisin muokkausaika. Järjestelmän generoima.
      |           example: 2019-08-23T09:55:17
      |""".stripMargin

  val KoulutusListItemModel: String =
    """    KoulutusListItem:
      |      type: object
      |      properties:
      |        oid:
      |          type: string
      |          description: Koulutuksen yksilöivä tunniste. Järjestelmän generoima.
      |          example: "1.2.246.562.13.00000000000000000009"
      |        tila:
      |          $ref: '#/components/schemas/Julkaisutila'
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
                    externalId: Option[String] = None,
                    johtaaTutkintoon: Boolean,
                    koulutustyyppi: Koulutustyyppi,
                    koulutuksetKoodiUri: Seq[String] = Seq(),
                    tila: Julkaisutila = Tallennettu,
                    esikatselu: Boolean = false,
                    tarjoajat: List[OrganisaatioOid] = List(),
                    nimi: Kielistetty = Map(),
                    sorakuvausId: Option[UUID] = None,
                    metadata: Option[KoulutusMetadata] = None,
                    julkinen: Boolean = false,
                    muokkaaja: UserOid,
                    organisaatioOid: OrganisaatioOid,
                    kielivalinta: Seq[Kieli] = Seq(),
                    teemakuva: Option[String] = None,
                    ePerusteId: Option[Long] = None,
                    modified: Option[Modified],
                    _enrichedData: Option[KoulutusEnrichedData] = None)
  extends PerustiedotWithOid[KoulutusOid, Koulutus] with HasTeemakuva[Koulutus] with AuthorizableMaybeJulkinen[Koulutus] {

  override def validate(): IsValid =
    super.validate()

  def withOid(oid: KoulutusOid): Koulutus = copy(oid = Some(oid))

  override def withTeemakuva(teemakuva: Option[String]): Koulutus = this.copy(teemakuva = teemakuva)

  override def withModified(modified: Modified): Koulutus = this.copy(modified = Some(modified))

  def withMuokkaaja(oid: UserOid): Koulutus = this.copy(muokkaaja = oid)

  def getEntityDescriptionAllative(): String = "koulutukselle"

  def isAvoinKorkeakoulutus(): Boolean = (metadata match {
    case Some(m: KkOpintokokonaisuusKoulutusMetadata) => m.isAvoinKorkeakoulutus
    case Some(m: KkOpintojaksoKoulutusMetadata)       => m.isAvoinKorkeakoulutus
    case _                                            => None
  }).getOrElse(false)

  def isAmmTutkintoWithoutEPeruste(): Boolean = koulutuksetKoodiUri.headOption match {
    case Some(koodiUri) => koulutustyyppi == Amm && AmmKoulutusKooditWithoutEperuste.koulutusKoodiUrit.contains(withoutKoodiVersion(koodiUri))
    case _              => false
  }

  def isSavingAllowedOnlyForOPH(): Boolean = Koulutustyyppi.onlyOphCanSaveKoulutus.contains(koulutustyyppi) || (koulutustyyppi == Amm && !isAmmTutkintoWithoutEPeruste())
}

case class KoulutusListItem(oid: KoulutusOid,
                            nimi: Kielistetty,
                            tila: Julkaisutila,
                            tarjoajat: List[OrganisaatioOid],
                            organisaatioOid: OrganisaatioOid,
                            muokkaaja: UserOid,
                            modified: Modified) extends OidListItem

case class KoulutusEnrichedData(muokkaajanNimi: Option[String] = None)

case class ExternalKoulutusRequest(authenticated: Authenticated, koulutus: Koulutus) extends ExternalRequest
