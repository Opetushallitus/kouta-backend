package fi.oph.kouta.domain

import java.util.UUID
import fi.oph.kouta.domain.oid.{KoulutusOid, Oid, OrganisaatioOid, ToteutusOid, UserOid}
import fi.oph.kouta.servlet.Authenticated
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
      |        externalId:
      |          type: string
      |          description: Ulkoinen tunniste jota voidaan käyttää Kouta lomakkeiden mäppäykseen oppilaitosten omien tietojärjestelmien kanssa
      |        koulutusOid:
      |          type: string
      |          description: Toteutukseen liittyvän koulutuksen yksilöivä tunniste.
      |          example: "1.2.246.562.13.00000000000000000009"
      |        tila:
      |          $ref: '#/components/schemas/Julkaisutila'
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
      |          $ref: '#/components/schemas/Nimi'
      |        metadata:
      |          type: object
      |          oneOf:
      |            - $ref: '#/components/schemas/YliopistoToteutusMetadata'
      |            - $ref: '#/components/schemas/AmmatillinenToteutusMetadata'
      |            - $ref: '#/components/schemas/AmmattikorkeaToteutusMetadata'
      |            - $ref: '#/components/schemas/KkOpintojaksoToteutusMetadata'
      |            - $ref: '#/components/schemas/KkOpintokokonaisuusToteutusMetadata'
      |            - $ref: '#/components/schemas/AmmatillinenTutkinnonOsaToteutusMetadata'
      |            - $ref: '#/components/schemas/AmmatillinenOsaamisalaToteutusMetadata'
      |            - $ref: '#/components/schemas/AmmatillinenMuuToteutusMetadata'
      |            - $ref: '#/components/schemas/AmmOpeErityisopeJaOpoToteutusMetadata'
      |            - $ref: '#/components/schemas/OpePedagOpinnotToteutusMetadata'
      |            - $ref: '#/components/schemas/LukioToteutusMetadata'
      |            - $ref: '#/components/schemas/TuvaToteutusMetadata'
      |            - $ref: '#/components/schemas/TelmaToteutusMetadata'
      |            - $ref: '#/components/schemas/VapaaSivistystyoOpistovuosiToteutusMetadata'
      |            - $ref: '#/components/schemas/VapaaSivistystyoMuuToteutusMetadata'
      |            - $ref: '#/components/schemas/AikuistenPerusopetusToteutusMetadata'
      |            - $ref: '#/components/schemas/ErikoislaakariToteutusMetadata'
      |            - $ref: '#/components/schemas/ErikoistumiskoulutusToteutusMetadata'
      |            - $ref: '#/components/schemas/TaiteenPerusopetusToteutusMetadata'
      |            - $ref: '#/components/schemas/MuuToteutusMetadata'
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
      |              maksullisuustyyppi: maksullinen,
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
      |              lukuvuosimaksu:
      |                 fi: 200 lukukaudessa
      |                 sv: 200 på svenska
      |              lukuvuosimaksuKuvaus:
      |                fi: Lukuvuosimaksun suomenkielinen kuvaus
      |                sv: Lukuvuosimaksun ruotsinkielinen kuvaus
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
      |           example: 2019-08-23T09:55:17
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
      |          $ref: '#/components/schemas/Julkaisutila'
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
      |          $ref: '#/components/schemas/Nimi'
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
      |           example: 2019-08-23T09:55:17
      |""".stripMargin

  def models = List(ToteutusModel, ToteutusListItemModel)
}

case class Toteutus(
    oid: Option[ToteutusOid] = None,
    externalId: Option[String] = None,
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
    modified: Option[Modified] = None,
    koulutusMetadata: Option[KoulutusMetadata] = None,
    koulutuksetKoodiUri: Seq[String] = Seq(),
    _enrichedData: Option[ToteutusEnrichedData] = None
) extends PerustiedotWithOidAndOptionalNimi[ToteutusOid, Toteutus]
    with HasTeemakuva[Toteutus] {

  override def validate(): IsValid = and(
    validateIfDefined[Oid](oid, assertValid(_, "oid")),
    assertValid(organisaatioOid, "organisaatioOid"),
    assertNotEmpty(kielivalinta, "kielivalinta")
  )

  def withOid(oid: ToteutusOid): Toteutus = copy(oid = Some(oid))

  override def withTeemakuva(teemakuva: Option[String]): Toteutus = this.copy(teemakuva = teemakuva)

  override def withModified(modified: Modified): Toteutus = copy(modified = Some(modified))

  def withMuokkaaja(oid: UserOid): Toteutus = this.copy(muokkaaja = oid)

  def withEnrichedData(enrichedData: ToteutusEnrichedData): Toteutus = this.copy(_enrichedData = Some(enrichedData))
  def withoutRelatedData(): Toteutus                                 = this.copy(koulutusMetadata = None)

  def getEntityDescriptionAllative(): String = "toteutukselle"
  def isAvoinKorkeakoulutus(): Boolean = (metadata match {
    case Some(m: KkOpintojaksoToteutusMetadata)       => m.isAvoinKorkeakoulutus
    case Some(m: KkOpintokokonaisuusToteutusMetadata) => m.isAvoinKorkeakoulutus
    case _                                            => None
  }).getOrElse(false)
}

case class MaybeToteutus(
    oid: Option[ToteutusOid] = None,
    externalId: Option[String] = None,
    koulutusOid: Option[KoulutusOid] = None,
    tila: Option[Julkaisutila] = None,
    esikatselu: Boolean = false,
    tarjoajat: List[OrganisaatioOid] = List(),
    nimi: Kielistetty = Map(),
    metadata: Option[ToteutusMetadata] = None,
    sorakuvausId: Option[UUID] = None,
    muokkaaja: Option[UserOid],
    organisaatioOid: Option[OrganisaatioOid],
    kielivalinta: Seq[Kieli] = Seq(),
    teemakuva: Option[String] = None,
    modified: Option[Modified] = None,
    koulutusMetadata: Option[KoulutusMetadata] = None,
    koulutuksetKoodiUri: Seq[String] = Seq()
)

object MaybeToteutus {
  def apply(t: MaybeToteutus): Toteutus = {
    new Toteutus(
      t.oid,
      t.externalId,
      t.koulutusOid.get,
      t.tila.get,
      t.esikatselu,
      t.tarjoajat,
      t.nimi,
      t.metadata,
      t.sorakuvausId,
      t.muokkaaja.get,
      t.organisaatioOid.get,
      t.kielivalinta,
      t.teemakuva,
      t.modified,
      t.koulutusMetadata,
      t.koulutuksetKoodiUri
    )
  }
}

case class ToteutusListItem(
    oid: ToteutusOid,
    koulutusOid: KoulutusOid,
    nimi: Kielistetty,
    tila: Julkaisutila,
    tarjoajat: List[OrganisaatioOid],
    organisaatioOid: OrganisaatioOid,
    muokkaaja: UserOid,
    modified: Modified
) extends OidListItem

object ToteutusListItem {
  def apply(t: Toteutus): ToteutusListItem = {
    new ToteutusListItem(
      t.oid.get,
      t.koulutusOid,
      t.nimi,
      t.tila,
      t.tarjoajat,
      t.organisaatioOid,
      t.muokkaaja,
      t.modified.get
    )
  }
}

case class ToteutusLiitettyListItem(
    oid: ToteutusOid,
    nimi: Kielistetty,
    tila: Julkaisutila,
    organisaatioOid: OrganisaatioOid,
    muokkaaja: UserOid,
    modified: Modified,
    koulutustyyppi: Koulutustyyppi,
    julkinen: Boolean = false
) extends LiitettyListItem

case class ToteutusEnrichedData(esitysnimi: Kielistetty = Map(), muokkaajanNimi: Option[String] = None)

case class ExternalToteutusRequest(authenticated: Authenticated, toteutus: Toteutus) extends ExternalRequest

case class OidAndNimi(oid: ToteutusOid, nimi: Kielistetty)

case class ToteutusEnrichmentSourceData(
    toteutusNimi: Kielistetty = Map(),
    koulutuksetKoodiUri: Seq[String] = Seq(),
    muokkaaja: UserOid,
    isLukioToteutus: Boolean = false,
    lukioLinjat: Seq[LukiolinjaTieto] = Seq(),
    hasLukioYleislinja: Boolean = false,
    opintojenLaajuusNumero: Option[Double] = None
)
