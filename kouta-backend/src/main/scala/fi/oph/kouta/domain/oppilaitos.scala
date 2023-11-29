package fi.oph.kouta.domain

import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.security.AuthorizableEntity
import fi.oph.kouta.validation.ExternalQueryResults.ExternalQueryResult
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, NoErrors, Validatable, ValidationContext}

package object oppilaitos {

  val OppilaitosModel =
    """    Oppilaitos:
      |      type: object
      |      properties:
      |        oid:
      |          type: string
      |          description: Oppilaitoksen organisaatio-oid
      |          example: "1.2.246.562.10.00101010101"
      |        tila:
      |          $ref: '#/components/schemas/Julkaisutila'
      |          description: Oppilaitoksen julkaisutila. Jos oppilaitos on julkaistu, se näkyy oppijalle Opintopolussa.
      |        esikatselu:
      |          type: boolean
      |          description: Onko oppilaitos nähtävissä esikatselussa
      |        kielivalinta:
      |          type: array
      |          description: Kielet, joille oppilaitoksen kuvailutiedot ja muut tekstit on käännetty
      |          items:
      |            $ref: '#/components/schemas/Kieli'
      |          example:
      |            - fi
      |            - sv
      |        metadata:
      |          type: object
      |          $ref: '#/components/schemas/OppilaitosMetadata'
      |        muokkaaja:
      |          type: string
      |          description: Oppilaitosta kuvailutietoja viimeksi muokanneen virkailijan henkilö-oid
      |          example: 1.2.246.562.24.00101010101.
      |        organisaatioOid:
      |           type: string
      |           description: Oppilaitoksen kuvailutiedot luoneen organisaation oid
      |           example: 1.2.246.562.10.00101010101
      |        teemakuva:
      |          type: string
      |          description: Oppilaitoksen Opintopolussa näytettävän teemakuvan URL.
      |          example: https://konfo-files.opintopolku.fi/oppilaitos-teemakuva/1.2.246.562.10.00000000000000000009/f4ecc80a-f664-40ef-98e6-eaf8dfa57f6e.png
      |        logo:
      |          type: string
      |          description: Oppilaitoksen Opintopolussa näytettävän logon URL.
      |          example: https://konfo-files.opintopolku.fi/oppilaitos-logo/1.2.246.562.10.00000000000000000009/ba9dd816-81fb-44ea-aafd-14ee3014e086.png
      |        modified:
      |          type: string
      |          format: date-time
      |          description: Oppilaitoksen kuvailutietojen viimeisin muokkausaika. Järjestelmän generoima
      |          example: 2019-08-23T09:55:17
      |        osat:
      |          type: array
      |          items:
      |            $ref: '#/components/schemas/OppilaitoksenOsa'
      |""".stripMargin

  val OppilaitosMetadataModel =
    """    OppilaitosMetadata:
      |      type: object
      |      properties:
      |        wwwSivu:
      |          type: object
      |          description: Opintopolussa käytettävä www-sivu ja sivun nimi eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/NimettyLinkki'
      |        some:
      |         type: object
      |         description: Opintopolussa näytettävien sosiaalisen median kanavien osoitteita. Koodiurit toimivat avaimena.
      |        tietoaOpiskelusta:
      |          type: array
      |          description: Oppilaitokseen liittyviä lisätietoja, jotka näkyvät oppijalle Opintopolussa
      |          items:
      |            type: object
      |            $ref: '#/components/schemas/TietoaOpiskelusta'
      |        esittely:
      |          type: object
      |          description: Oppilaitoksen Opintopolussa näytettävä esittely eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        opiskelijoita:
      |          type: integer
      |          description: Oppilaitoksen opiskelijoiden lkm
      |        korkeakouluja:
      |          type: integer
      |          description: Oppilaitoksen korkeakoulujen lkm
      |        tiedekuntia:
      |          type: integer
      |          description: Oppilaitoksen tiedekuntien lkm
      |        kampuksia:
      |          type: integer
      |          description: Oppilaitoksen kampuksien lkm
      |        yksikoita:
      |          type: integer
      |          description: Oppilaitoksen yksiköiden lkm
      |        toimipisteita:
      |          type: integer
      |          description: Oppilaitoksen toimipisteiden lkm
      |        akatemioita:
      |          type: integer
      |          description: Oppilaitoksen akatemioiden lkm
      |        teemakuva:
      |          type: string
      |          description: Oppilaitoksen Opintopolussa näytettävän teemakuvan URL.
      |          example: https://konfo-files.opintopolku.fi/toteutus-teema/1.2.246.562.10.00000000000000000009/f4ecc80a-f664-40ef-98e6-eaf8dfa57f6e.png
      |        jarjestaaUrheilijanAmmKoulutusta:
      |          type: boolean
      |          description: Järjestääkö oppilaitos urheilijan ammatillista koulutusta?
      |""".stripMargin

  val OppilaitoksenOsaModel =
    """    OppilaitoksenOsa:
      |      type: object
      |      properties:
      |        oid:
      |          type: string
      |          description: Oppilaitoksen osan organisaatio-oid
      |          example: "1.2.246.562.10.00101010102"
      |        oppilaitosOid:
      |          type: string
      |          description: Oppilaitoksen osan oppilaitoksen organisaatio-oid
      |          example: "1.2.246.562.10.00101010101"
      |        tila:
      |          $ref: '#/components/schemas/Julkaisutila'
      |          description: Oppilaitoksen osan julkaisutila. Jos oppilaitoksen osa on julkaistu, se näkyy oppijalle Opintopolussa.
      |        esikatselu:
      |          type: boolean
      |          description: Onko oppilaitoksen osa nähtävissä esikatselussa
      |        kielivalinta:
      |          type: array
      |          description: Kielet, joille oppilaitoksen osan kuvailutiedot ja muut tekstit on käännetty
      |          items:
      |            $ref: '#/components/schemas/Kieli'
      |          example:
      |            - fi
      |            - sv
      |        metadata:
      |          type: object
      |          $ref: '#/components/schemas/OppilaitoksenOsaMetadata'
      |        muokkaaja:
      |          type: string
      |          description: Oppilaitoksen osan kuvailutietoja viimeksi muokanneen virkailijan henkilö-oid
      |          example: 1.2.246.562.10.00101010101
      |        organisaatioOid:
      |           type: string
      |           description: Oppilaitoksen osan kuvailutiedot luoneen organisaation oid
      |           example: 1.2.246.562.10.00101010101
      |        teemakuva:
      |          type: string
      |          description: Oppilaitoksen osan Opintopolussa näytettävän teemakuvan URL.
      |          example: https://konfo-files.opintopolku.fi/oppilaitoksen-osa-teemakuva/1.2.246.562.10.00000000000000000009/f4ecc80a-f664-40ef-98e6-eaf8dfa57f6e.png
      |        modified:
      |           type: string
      |           format: date-time
      |           description: Oppilaitoksen osan kuvailutietojen viimeisin muokkausaika. Järjestelmän generoima
      |           example: 2019-08-23T09:55:17
      |""".stripMargin

  val OppilaitoksenOsaMetadataModel =
    """    OppilaitoksenOsaMetadata:
      |      type: object
      |      properties:
      |        wwwSivu:
      |          type: object
      |          description: Opintopolussa käytettävä www-sivu ja sivun nimi eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/NimettyLinkki'
      |        hakijapalveluidenYhteystiedot:
      |          type: object
      |          description: Oppilaitoksen Opintopolussa näytettävät hakijapalveluiden yhteystiedot
      |          $ref: '#/components/schemas/Yhteystieto'
      |        esittely:
      |          type: object
      |          description: Oppilaitoksen osan Opintopolussa näytettävä esittely eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        kampus:
      |          type: object
      |          description: Oppilaitoksen osan kampuksen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Nimi'
      |        opiskelijoita:
      |          type: integer
      |          description: Oppilaitoksen osan opiskelijoiden lkm
      |        teemakuva:
      |          type: string
      |          description: Oppilaitoksen osan Opintopolussa näytettävän teemakuvan URL.
      |          example: https://konfo-files.opintopolku.fi/toteutus-teema/1.2.246.562.10.00000000000000000009/f4ecc80a-f664-40ef-98e6-eaf8dfa57f6e.png
      |        jarjestaaUrheilijanAmmKoulutusta: 
      |          type: boolean
      |          description: Järjestääkö oppilaitoksen osa urheilijan ammatillista koulutusta?
      |""".stripMargin

  val OppilaitoksenOsaListItemModel =
    """    OppilaitoksenOsaListItem:
      |      type: object
      |      properties:
      |        oid:
      |          type: string
      |          description: Oppilaitoksen osan organisaatio-oid
      |          example: "1.2.246.562.10.00101010102"
      |        oppilaitosOid:
      |          type: string
      |          description: Oppilaitoksen osan oppilaitoksen organisaatio-oid
      |          example: "1.2.246.562.10.00101010101"
      |        tila:
      |          $ref: '#/components/schemas/Julkaisutila'
      |          description: Oppilaitoksen osan julkaisutila. Jos oppilaitoksen osa on julkaistu, se näkyy oppijalle Opintopolussa.
      |        muokkaaja:
      |          type: string
      |          description: Oppilaitoksen osan kuvailutietoja viimeksi muokanneen virkailijan henkilö-oid
      |          example: 1.2.246.562.10.00101010101
      |        organisaatioOid:
      |           type: string
      |           description: Oppilaitoksen osan kuvailutiedot luoneen organisaation oid
      |           example: 1.2.246.562.10.00101010101
      |        modified:
      |           type: string
      |           format: date-time
      |           description: Oppilaitoksen osan kuvailutietojen viimeisin muokkausaika. Järjestelmän generoima
      |           example: 2019-08-23T09:55:17
      |""".stripMargin

  val YhteystietoModel =
    """    Yhteystieto:
      |      type: object
      |      properties:
      |        nimi:
      |          type: object
      |          description: Opintopolussa näytettävä yhteystiedon nimi eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/Teksti'
      |        postiosoite:
      |          type: object
      |          description: Opintopolussa näytettävä postiosoite eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/Osoite'
      |        kayntiosoite:
      |          type: object
      |          description: Opintopolussa näytettävä käyntiosoite eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/Osoite'
      |        sahkoposti:
      |          type: object
      |          description: Opintopolussa näytettävä sähköpostiosoite eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/Teksti'
      |        puhelinnumero:
      |          type: object
      |          description: Opintopolussa näytettävä puhelinnumero eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/Teksti'
      |""".stripMargin

  val TietoaOpiskelustaModel =
    """    TietoaOpiskelusta:
      |      type: object
      |      properties:
      |        otsikkoKoodiUri:
      |          type: string
      |          description: Lisätiedon otsikon koodi URI. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/organisaationkuvaustiedot/1)
      |          example: organisaationkuvaustiedot_03#1
      |        teksti:
      |          type: object
      |          description: Lisätiedon teksti eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/Teksti'
      |""".stripMargin

  val OppilaitoksetResponseModel =
    """    OppilaitoksetResponse:
      |      type: object
      |      properties:
      |        oppilaitokset:
      |          type: array
      |          items:
      |            $ref: '#/components/schemas/Oppilaitos'
      |        organisaatioHierarkia:
      |          type: object
      |          $ref: '#/components/schemas/OrganisaatioHierarkia'
      |""".stripMargin

  def models = Seq(
    OppilaitosModel,
    OppilaitoksenOsaModel,
    OppilaitosMetadataModel,
    OppilaitoksenOsaMetadataModel,
    OppilaitoksenOsaListItemModel,
    YhteystietoModel,
    TietoaOpiskelustaModel,
    OppilaitoksetResponseModel,
  )
}

case class Oppilaitos(
    oid: OrganisaatioOid,
    tila: Julkaisutila = Tallennettu,
    esikatselu: Boolean = false,
    metadata: Option[OppilaitosMetadata] = None,
    kielivalinta: Seq[Kieli] = Seq(),
    organisaatioOid: OrganisaatioOid,
    muokkaaja: UserOid,
    teemakuva: Option[String] = None,
    logo: Option[String] = None,
    modified: Option[Modified] = None,
    _enrichedData: Option[OppilaitosEnrichedData] = None,
    osat: Option[Seq[OppilaitoksenOsa]] = None
) extends OppilaitosBase
    with Validatable
    with AuthorizableEntity[Oppilaitos]
    with HasPrimaryId[OrganisaatioOid, Oppilaitos]
    with HasModified[Oppilaitos]
    with HasMuokkaaja[Oppilaitos]
    with HasTeemakuva[Oppilaitos] {

  override def validate(): IsValid = NoErrors

  override def primaryId: Option[OrganisaatioOid] = Some(oid)

  override def withPrimaryID(oid: OrganisaatioOid): Oppilaitos = copy(oid = oid)

  override def withTeemakuva(teemakuva: Option[String]): Oppilaitos = copy(teemakuva = teemakuva)

  override def withModified(modified: Modified): Oppilaitos = copy(modified = Some(modified))

  def withMuokkaaja(oid: UserOid): Oppilaitos = this.copy(muokkaaja = oid)

  def withOsat(osat: Seq[OppilaitoksenOsa]): Oppilaitos = this.copy(osat = Some(osat))

  def getEntityDescriptionAllative(): String = "oppilaitokselle"

  def withEnrichedData(enrichedData: OppilaitosEnrichedData): Oppilaitos = this.copy(_enrichedData = Some(enrichedData))

}

case class OppilaitoksenOsa(
    oid: OrganisaatioOid,
    oppilaitosOid: OrganisaatioOid,
    tila: Julkaisutila = Tallennettu,
    esikatselu: Boolean = false,
    metadata: Option[OppilaitoksenOsaMetadata] = None,
    kielivalinta: Seq[Kieli] = Seq(),
    organisaatioOid: OrganisaatioOid,
    muokkaaja: UserOid,
    teemakuva: Option[String] = None,
    modified: Option[Modified] = None,
    _enrichedData: Option[OppilaitosEnrichedData] = None
) extends OppilaitosBase
    with Validatable
    with AuthorizableEntity[OppilaitoksenOsa]
    with HasPrimaryId[OrganisaatioOid, OppilaitoksenOsa]
    with HasModified[OppilaitoksenOsa]
    with HasMuokkaaja[OppilaitoksenOsa]
    with HasTeemakuva[OppilaitoksenOsa] {

  override def validate(): IsValid = NoErrors

  override def primaryId: Option[OrganisaatioOid] = Some(oid)

  override def withPrimaryID(oid: OrganisaatioOid): OppilaitoksenOsa = copy(oid = oid)

  override def withTeemakuva(teemakuva: Option[String]): OppilaitoksenOsa = copy(teemakuva = teemakuva)

  override def withModified(modified: Modified): OppilaitoksenOsa = copy(modified = Some(modified))

  def withMuokkaaja(oid: UserOid): OppilaitoksenOsa = this.copy(muokkaaja = oid)
  def getEntityDescriptionAllative(): String        = "oppilaitoksen osalle"

  def withEnrichedData(enrichedData: OppilaitosEnrichedData): OppilaitoksenOsa = this.copy(_enrichedData = Some(enrichedData))
}

case class OppilaitosMetadata(
    tietoaOpiskelusta: Seq[TietoaOpiskelusta] = Seq(),
    wwwSivu: Option[NimettyLinkki] = None,
    some: Map[String, Option[String]] = Map(),
    hakijapalveluidenYhteystiedot: Option[Yhteystieto] = None,
    esittely: Kielistetty = Map(),
    opiskelijoita: Option[Int] = None,
    korkeakouluja: Option[Int] = None,
    tiedekuntia: Option[Int] = None,
    kampuksia: Option[Int] = None,
    yksikoita: Option[Int] = None,
    toimipisteita: Option[Int] = None,
    akatemioita: Option[Int] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    jarjestaaUrheilijanAmmKoulutusta: Option[Boolean] = None
)

case class TietoaOpiskelusta(otsikkoKoodiUri: String, teksti: Kielistetty)

case class OppilaitoksenOsaMetadata(
    wwwSivu: Option[NimettyLinkki] = None,
    hakijapalveluidenYhteystiedot: Option[Yhteystieto] = None,
    opiskelijoita: Option[Int] = None,
    kampus: Kielistetty = Map(),
    esittely: Kielistetty = Map(),
    jarjestaaUrheilijanAmmKoulutusta: Option[Boolean] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
)

case class OppilaitoksenOsaListItem(
    oid: OrganisaatioOid,
    oppilaitosOid: OrganisaatioOid,
    tila: Julkaisutila,
    organisaatioOid: OrganisaatioOid,
    muokkaaja: UserOid,
    modified: Modified
)

case class Yhteystieto(
    nimi: Kielistetty = Map(),
    postiosoite: Option[Osoite] = None,
    kayntiosoite: Option[Osoite] = None,
    puhelinnumero: Kielistetty = Map(),
    sahkoposti: Kielistetty = Map()
) {
  def validate(path: String, entityWithNewValues: Option[Yhteystieto], vCtx: ValidationContext, osoiteKoodistoCheckFunc: String => ExternalQueryResult): IsValid =
    and(
      validateIfDefined[Osoite](
        postiosoite,
        _.validate(
          s"$path.postiosoite",
          entityWithNewValues.flatMap(_.postiosoite),
          vCtx,
          osoiteKoodistoCheckFunc
        )
      ),
      validateIfDefined[Osoite](
        kayntiosoite,
        _.validate(
          s"$path.kayntiosoite",
          entityWithNewValues.flatMap(_.kayntiosoite),
          vCtx,
          osoiteKoodistoCheckFunc
        )
      ),
      validateIfNonEmpty(sahkoposti, s"$path.sahkoposti", assertValidEmail _),
      validateIfJulkaistu(
        vCtx.tila,
        and(
          validateKielistetty(vCtx.kielivalinta, nimi, s"$path.nimi"),
          validateOptionalKielistetty(
            vCtx.kielivalinta,
            puhelinnumero,
            s"$path.puhelinnumero"
          ),
          validateOptionalKielistetty(
            vCtx.kielivalinta,
            sahkoposti,
            s"$path.sahkoposti"
          )
        )
      )
    )
}

case class OppilaitosEnrichedData(muokkaajanNimi: Option[String] = None,
                                  organisaationYhteystiedot: Option[Yhteystieto] = None)

case class OppilaitosAndOsa(
    oppilaitos: Oppilaitos,
    osa: Option[OppilaitoksenOsa] = None
)

case class OppilaitoksetResponse(
    oppilaitokset: List[Oppilaitos],
    organisaatioHierarkia: OrganisaatioHierarkia
)

sealed trait OppilaitosBase {
  val oid: OrganisaatioOid
  val _enrichedData: Option[OppilaitosEnrichedData]
}

case class OppilaitosWithOrganisaatioData(oid: OrganisaatioOid,
                                          _enrichedData: Option[OppilaitosEnrichedData]
                                         ) extends OppilaitosBase
