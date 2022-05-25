package fi.oph.kouta.domain

import fi.oph.kouta.client.OrganisaatioHierarkia
import fi.oph.kouta.domain.oid.{OrganisaatioOid, UserOid}
import fi.oph.kouta.security.AuthorizableEntity
import fi.oph.kouta.validation.Validations.{validateIfJulkaistu, _}
import fi.oph.kouta.validation.{IsValid, Validatable, ValidatableSubEntity}

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
      |          type: string
      |          example: "julkaistu"
      |          enum:
      |            - julkaistu
      |            - arkistoitu
      |            - tallennettu
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
      |           type: string
      |           format: date-time
      |           description: Oppilaitoksen kuvailutietojen viimeisin muokkausaika. Järjestelmän generoima
      |           example: 2019-08-23T09:55:17
      |""".stripMargin

  val OppilaitosMetadataModel =
    """    OppilaitosMetadata:
      |      type: object
      |      properties:
      |        wwwSivu:
      |          type: object
      |          description: Opintopolussa käytettävä www-sivu ja sivun nimi eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/NimettyLinkki'
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
      |          type: string
      |          example: "julkaistu"
      |          enum:
      |            - julkaistu
      |            - arkistoitu
      |            - tallennettu
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
      |          type: string
      |          example: "julkaistu"
      |          enum:
      |            - julkaistu
      |            - arkistoitu
      |            - tallennettu
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


  def models = Seq(OppilaitosModel, OppilaitoksenOsaModel, OppilaitosMetadataModel, OppilaitoksenOsaMetadataModel,
    OppilaitoksenOsaListItemModel, YhteystietoModel, TietoaOpiskelustaModel)
}

case class Oppilaitos(oid: OrganisaatioOid,
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
                      osat: Option[Seq[OppilaitoksenOsa]] = None)
  extends Validatable
    with AuthorizableEntity[Oppilaitos]
    with HasPrimaryId[OrganisaatioOid, Oppilaitos]
    with HasModified[Oppilaitos]
    with HasMuokkaaja[Oppilaitos]
    with HasTeemakuva[Oppilaitos] {

  override def validate(): IsValid = and(
    assertValid(oid, "oid"),
    assertValid(organisaatioOid, "organisaatioOid"),
    validateIfDefined[OppilaitosMetadata](metadata, _.validate(tila, kielivalinta, "metadata")),
    validateIfDefined[String](teemakuva, assertValidUrl(_, "teemakuva")),
    validateIfDefined[String](logo, assertValidUrl(_, "logo")),
    assertNotEmpty(kielivalinta, "kielivalinta")
  )

  override def primaryId: Option[OrganisaatioOid] = Some(oid)

  override def withPrimaryID(oid: OrganisaatioOid): Oppilaitos = copy(oid = oid)

  override def withTeemakuva(teemakuva: Option[String]): Oppilaitos = copy(teemakuva = teemakuva)

  override def withModified(modified: Modified): Oppilaitos = copy(modified = Some(modified))

  def withMuokkaaja(oid: UserOid): Oppilaitos = this.copy(muokkaaja = oid)

  def withOsat(osat: Seq[OppilaitoksenOsa]): Oppilaitos = this.copy(osat = Some(osat))
}

case class OppilaitoksenOsa(oid: OrganisaatioOid,
                            oppilaitosOid: OrganisaatioOid,
                            tila: Julkaisutila = Tallennettu,
                            esikatselu: Boolean = false,
                            metadata: Option[OppilaitoksenOsaMetadata] = None,
                            kielivalinta: Seq[Kieli] = Seq(),
                            organisaatioOid: OrganisaatioOid,
                            muokkaaja: UserOid,
                            teemakuva: Option[String] = None,
                            modified: Option[Modified] = None,
                           _enrichedData: Option[OppilaitosEnrichedData] = None)
  extends Validatable
    with AuthorizableEntity[OppilaitoksenOsa]
    with HasPrimaryId[OrganisaatioOid, OppilaitoksenOsa]
    with HasModified[OppilaitoksenOsa]
    with HasMuokkaaja[OppilaitoksenOsa]
    with HasTeemakuva[OppilaitoksenOsa] {

  override def validate(): IsValid = and(
    assertValid(oid, "oid"),
    assertValid(oppilaitosOid, "oppilaitosOid"),
    assertValid(organisaatioOid, "organisaatioOid"),
    validateIfDefined[OppilaitoksenOsaMetadata](metadata, _.validate(tila, kielivalinta, "metadata")),
    validateIfDefined[String](teemakuva, assertValidUrl(_, "teemakuva")),
    assertNotEmpty(kielivalinta, "kielivalinta")
  )

  override def primaryId: Option[OrganisaatioOid] = Some(oid)

  override def withPrimaryID(oid: OrganisaatioOid): OppilaitoksenOsa = copy(oid = oid)

  override def withTeemakuva(teemakuva: Option[String]): OppilaitoksenOsa = copy(teemakuva = teemakuva)

  override def withModified(modified: Modified): OppilaitoksenOsa = copy(modified = Some(modified))

  def withMuokkaaja(oid: UserOid): OppilaitoksenOsa = this.copy(muokkaaja = oid)
}

case class OppilaitosMetadata(tietoaOpiskelusta: Seq[TietoaOpiskelusta] = Seq(),
                              wwwSivu: Option[NimettyLinkki] = None,
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
                              jarjestaaUrheilijanAmmKoulutusta: Boolean = false,
                              ) extends ValidatableSubEntity {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfNonEmpty[TietoaOpiskelusta](tietoaOpiskelusta, s"$path.tietoaOpiskelusta", _.validate(tila, kielivalinta, _)),
    validateIfDefined[NimettyLinkki](wwwSivu, _.validate(tila, kielivalinta, s"$path.wwwSivu")),
    validateIfDefined[Yhteystieto](hakijapalveluidenYhteystiedot, _.validate(tila, kielivalinta, s"$path.hakijapalveluidenYhteystiedot")),
    validateIfDefined[Int](opiskelijoita, assertNotNegative(_, s"$path.opiskelijoita")),
    validateIfDefined[Int](korkeakouluja, assertNotNegative(_, s"$path.korkeakouluja")),
    validateIfDefined[Int](tiedekuntia,   assertNotNegative(_, s"$path.tiedekuntia")),
    validateIfDefined[Int](kampuksia,     assertNotNegative(_, s"$path.kampuksia")),
    validateIfDefined[Int](yksikoita,     assertNotNegative(_, s"$path.yksikoita")),
    validateIfDefined[Int](toimipisteita, assertNotNegative(_, s"$path.toimipisteita")),
    validateIfDefined[Int](akatemioita,   assertNotNegative(_, s"$path.akatemioita")),
    validateIfJulkaistu(tila, and(
      validateOptionalKielistetty(kielivalinta, esittely, s"$path.esittely"),
      assertNotOptional(wwwSivu, s"$path.wwwSivu")
    ))
  )
}

case class TietoaOpiskelusta(otsikkoKoodiUri: String, teksti: Kielistetty) extends ValidatableSubEntity {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    assertMatch(otsikkoKoodiUri, TietoaOpiskelustaOtsikkoKoodiPattern, s"$path.otsikkoKoodiUri"),
    validateIfJulkaistu(tila, validateKielistetty(kielivalinta, teksti, s"$path.teksti"))
  )
}

case class OppilaitoksenOsaMetadata(wwwSivu: Option[NimettyLinkki] = None,
                                    opiskelijoita: Option[Int] = None,
                                    kampus: Kielistetty = Map(),
                                    esittely: Kielistetty = Map(),
                                    jarjestaaUrheilijanAmmKoulutusta: Boolean = false,
                                    isMuokkaajaOphVirkailija: Option[Boolean] = None) extends ValidatableSubEntity {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfDefined[NimettyLinkki](wwwSivu, _.validate(tila, kielivalinta, s"$path.wwwSivu")),
    validateIfDefined[Int](opiskelijoita, assertNotNegative(_, s"$path.opiskelijoita")),
    validateIfJulkaistu(tila, and(
      validateOptionalKielistetty(kielivalinta, kampus, s"$path.kampus"),
      validateOptionalKielistetty(kielivalinta, esittely, s"$path.esittely"),
      assertNotOptional(wwwSivu, s"$path.wwwSivu"),
    ))
  )
}

case class OppilaitoksenOsaListItem(oid: OrganisaatioOid,
                                    oppilaitosOid: OrganisaatioOid,
                                    tila: Julkaisutila,
                                    organisaatioOid: OrganisaatioOid,
                                    muokkaaja: UserOid,
                                    modified: Modified)

case class Yhteystieto(nimi: Kielistetty = Map(),
                       postiosoite: Option[Osoite] = None,
                       kayntiosoite: Option[Osoite] = None,
                       puhelinnumero: Kielistetty = Map(),
                       sahkoposti: Kielistetty = Map()) extends ValidatableSubEntity {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfDefined[Osoite](postiosoite, _.validate(tila, kielivalinta, s"$path.postiosoite")),
    validateIfDefined[Osoite](kayntiosoite, _.validate(tila, kielivalinta, s"$path.kayntiosoite")),
    validateIfNonEmpty(sahkoposti, s"$path.sahkoposti", assertValidEmail _),
    validateIfJulkaistu(tila, and(
      validateKielistetty(kielivalinta, nimi, s"$path.nimi"),
      validateOptionalKielistetty(kielivalinta, puhelinnumero, s"$path.puhelinnumero"),
      validateOptionalKielistetty(kielivalinta, sahkoposti, s"$path.sahkoposti")
    ))
  )
}

case class OppilaitosEnrichedData(muokkaajanNimi: Option[String] = None)

case class OppilaitosAndOsa(
    oppilaitos: Oppilaitos,
    osa: Option[OppilaitoksenOsa] = None
)

case class OppilaitosByOid(
    oid: OrganisaatioOid,
    oppilaitos: Option[Oppilaitos] = None,
    organisaatioHierarkia: Option[OrganisaatioHierarkia] = None
)
