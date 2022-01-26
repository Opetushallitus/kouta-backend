package fi.oph.kouta.domain

import java.time.LocalDateTime
import java.util.UUID
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.service.HakukohdeService
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, ValidatableSubEntity}

package object hakukohde {

  val HakukohdeModel: String =
    """    Hakukohde:
      |      type: object
      |      properties:
      |        oid:
      |          type: string
      |          description: Hakukohteen yksilöivä tunniste. Järjestelmän generoima.
      |          example: "1.2.246.562.20.00000000000000000009"
      |        externalId:
      |          type: string
      |          description: Ulkoinen tunniste jota voidaan käyttää Kouta lomakkeiden mäppäykseen oppilaitosten omien tietojärjestelmien kanssa
      |        toteutusOid:
      |          type: string
      |          description: Hakukohteeseen liitetyn toteutuksen yksilöivä tunniste.
      |          example: "1.2.246.562.17.00000000000000000009"
      |        hakuOid:
      |          type: string
      |          description: Hakukohteeseen liitetyn haun yksilöivä tunniste.
      |          example: "1.2.246.562.29.00000000000000000009"
      |        tila:
      |          type: string
      |          example: "julkaistu"
      |          enum:
      |            - julkaistu
      |            - arkistoitu
      |            - tallennettu
      |            - poistettu
      |          description: Haun julkaisutila. Jos hakukohde on julkaistu, se näkyy oppijalle Opintopolussa.
      |        esikatselu:
      |          type: boolean
      |          description: Onko hakukohde nähtävissä esikatselussa
      |        nimi:
      |          type: object
      |          description: Hakukohteen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Nimi'
      |        hakukohdeKoodiUri:
      |          type: string
      |          description: KoodiUri, josta hakukohteen nimi muodostetaan. Hakukohteella täytyy olla joko hakukohdeKoodiUri tai nimi. Jos koodiUri löytyy, muodostetaan nimi sen avulla.
      |        jarjestyspaikkaOid:
      |          type: string
      |          description: Hakukohteen järjestyspaikan organisaatio
      |          example: 1.2.246.562.10.00101010101
      |        hakulomaketyyppi:
      |          type: string
      |          description: Hakulomakkeen tyyppi. Kertoo, käytetäänkö Atarun (hakemuspalvelun) hakulomaketta, muuta hakulomaketta
      |            (jolloin voidaan lisätä hakulomakkeeseen linkki) tai onko niin, ettei sähkököistä hakulomaketta ole lainkaan, jolloin sille olisi hyvä lisätä kuvaus.
      |          example: "ataru"
      |          enum:
      |            - ataru
      |            - ei sähköistä
      |            - muu
      |        hakulomakeAtaruId:
      |          type: string
      |          description: Hakulomakkeen yksilöivä tunniste, jos käytössä on Atarun (hakemuspalvelun) hakulomake
      |          example: "ea596a9c-5940-497e-b5b7-aded3a2352a7"
      |        hakulomakeKuvaus:
      |          type: object
      |          description: Hakulomakkeen kuvausteksti eri kielillä. Kielet on määritetty haun kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        hakulomakeLinkki:
      |          type: object
      |          description: Hakulomakkeen linkki eri kielillä. Kielet on määritetty haun kielivalinnassa.
      |          $ref: '#/components/schemas/Linkki'
      |        kaytetaanHaunHakulomaketta:
      |          type: boolean
      |          description: Käytetäänkö haun hakulomaketta vai onko hakukohteelle määritelty oma hakulomake?
      |        aloituspaikat:
      |          type: integer
      |          deprecated: true
      |          description: Hakukohteen aloituspaikkojen lukumäärä
      |          example: 100
      |        ensikertalaisenAloituspaikat:
      |          type: integer
      |          deprecated: true
      |          description: Hakukohteen ensikertalaisen aloituspaikkojen lukumäärä
      |          example: 50
      |        pohjakoulutusvaatimusKoodiUrit:
      |          type: array
      |          description: Lista hakukohteen pohjakoulutusvaatimuksista. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/pohjakoulutusvaatimuskouta/1)
      |          items:
      |            type: string
      |          example:
      |            - pohjakoulutusvaatimuskouta_104#1
      |            - pohjakoulutusvaatimuskouta_109#1
      |        muuPohjakoulutusvaatimus:
      |          type: object
      |          description: Hakukohteen muiden pohjakoulutusvaatimusten kuvaus eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        toinenAsteOnkoKaksoistutkinto:
      |          type: boolean
      |          description: Onko hakukohteen toisen asteen koulutuksessa mahdollista suorittaa kaksoistutkinto?
      |        kaytetaanHaunAikataulua:
      |          type: boolean
      |          description: Käytetäänkö haun hakuaikoja vai onko hakukohteelle määritelty omat hakuajat?
      |        hakuajat:
      |          type: array
      |          description: Hakukohteen hakuajat, jos ei käytetä haun hakuaikoja
      |          items:
      |            $ref: '#/components/schemas/Ajanjakso'
      |        valintaperusteId:
      |          type: string
      |          description: Hakukohteeseen liittyvän valintaperustekuvauksen yksilöivä tunniste
      |          example: "ea596a9c-5940-497e-b5b7-aded3a2352a7"
      |        liitteetOnkoSamaToimitusaika:
      |          type: boolean
      |          description: Onko kaikilla hakukohteen liitteillä sama toimitusaika?
      |        liitteetOnkoSamaToimitusosoite:
      |          type: boolean
      |          description: Onko kaikilla hakukohteen liitteillä sama toimitusosoite?
      |        liitteidenToimitusaika:
      |          type: string
      |          description: Jos liitteillä on sama toimitusaika, se ilmoitetaan tässä
      |          format: date-time
      |          example: 2019-08-23T09:55
      |        liitteidenToimitustapa:
      |          type: string
      |          description: Jos liitteillä on sama toimitustapa, se ilmoitetaan tässä
      |          example: "hakijapalvelu"
      |          enum:
      |            - hakijapalvelu
      |            - osoite
      |            - lomake
      |        liitteidenToimitusosoite:
      |          type: object
      |          description: Jos liitteillä on sama toimitusosoite, se ilmoitetaan tässä
      |          $ref: '#/components/schemas/LiitteenToimitusosoite'
      |        liitteet:
      |          type: array
      |          description: Hakukohteen liitteet
      |          items:
      |            $ref: '#/components/schemas/Liite'
      |        valintakokeet:
      |          type: array
      |          description: Hakukohteeseen liittyvät valintakokeet
      |          items:
      |            $ref: '#/components/schemas/Valintakoe'
      |        kielivalinta:
      |          type: array
      |          description: Kielet, joille hakukohteen nimi, kuvailutiedot ja muut tekstit on käännetty
      |          items:
      |            $ref: '#/components/schemas/Kieli'
      |          example:
      |            - fi
      |            - sv
      |        metadata:
      |          type: object
      |          $ref: '#/components/schemas/HakukohdeMetadata'
      |        muokkaaja:
      |          type: string
      |          description: Hakukohdetta viimeksi muokanneen virkailijan henkilö-oid
      |          example: 1.2.246.562.10.00101010101
      |        organisaatioOid:
      |           type: string
      |           description: Hakukohteen luoneen organisaation oid
      |           example: 1.2.246.562.10.00101010101
      |        modified:
      |           type: string
      |           format: date-time
      |           description: Hakukohteen viimeisin muokkausaika. Järjestelmän generoima
      |           example: 2019-08-23T09:55:17
      |        _enrichedData:
      |          type: object
      |          $ref: '#/components/schemas/EnrichedData'
      |""".stripMargin

  val HakukohdeListItemModel: String =
    """    HakukohdeListItem:
      |      type: object
      |      properties:
      |        oid:
      |          type: string
      |          description: Hakukohteen yksilöivä tunniste. Järjestelmän generoima.
      |          example: "1.2.246.562.20.00000000000000000009"
      |        toteutusOid:
      |          type: string
      |          description: Hakukohteeseen liitetyn toteutuksen yksilöivä tunniste.
      |          example: "1.2.246.562.17.00000000000000000009"
      |        hakuOid:
      |          type: string
      |          description: Hakukohteeseen liitetyn haun yksilöivä tunniste.
      |          example: "1.2.246.562.29.00000000000000000009"
      |        valintaperusteId:
      |          type: string
      |          description: Hakukohteeseen liitetyn valintaperusteen yksilöivä tunniste.
      |          example: "ea596a9c-5940-497e-b5b7-aded3a2352a7"
      |        tila:
      |          type: string
      |          example: "julkaistu"
      |          enum:
      |            - julkaistu
      |            - arkistoitu
      |            - tallennettu
      |            - poistettu
      |          description: Hakukohteen julkaisutila. Jos haku on julkaistu, se näkyy oppijalle Opintopolussa.
      |        nimi:
      |          type: object
      |          description: Hakukohteen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Nimi'
      |        muokkaaja:
      |          type: string
      |          description: Hakukohdetta viimeksi muokanneen virkailijan henkilö-oid
      |          example: 1.2.246.562.10.00101010101
      |        jarjestyspaikkaOid:
      |          type: string
      |          description: Hakukohteen järjestyspaikan organisaatio
      |          example: 1.2.246.562.10.00101010101
      |        organisaatioOid:
      |           type: string
      |           description: Hakukohteen luoneen organisaation oid
      |           example: 1.2.246.562.10.00101010101
      |        modified:
      |           type: string
      |           format: date-time
      |           description: Hakukohteen viimeisin muokkausaika. Järjestelmän generoima
      |           example: 2019-08-23T09:55:17
      |""".stripMargin

  val HakukohdeMetadataModel: String =
    """    HakukohdeMetadata:
      |      type: object
      |      properties:
      |        valintakokeidenYleiskuvaus:
      |          type: object
      |          description: Valintakokeiden yleiskuvaus eri kielillä. Kielet on määritetty hakukohteen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        kynnysehto:
      |          type: object
      |          description: Hakukohteen kynnysehto eri kielillä. Kielet on määritetty hakukohteen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        valintaperusteenValintakokeidenLisatilaisuudet:
      |          type: array
      |          description: Hakukohteeseen liitetyn valintaperusteen valintakokeisiin liitetyt lisätilaisuudet
      |          items:
      |            $ref: '#/components/schemas/ValintakokeenLisatilaisuudet'
      |        koulutuksenAlkamiskausi:
      |          type: object
      |          description: Koulutuksen alkamiskausi
      |          $ref: '#/components/schemas/KoulutuksenAlkamiskausi'
      |        kaytetaanHaunAlkamiskautta:
      |          type: boolean
      |          description: Käytetäänkö haun alkamiskautta ja -vuotta vai onko hakukohteelle määritelty oma alkamisajankohta?
      |        aloituspaikat:
      |          type: object
      |          description: Hakukohteen aloituspaikkojen tiedot
      |          $ref: '#/components/schemas/Aloituspaikat'
      |        uudenOpiskelijanUrl:
      |          type: string
      |          description: Uuden opiskelijan ohjeita sisältävän verkkosivun URL
      |""".stripMargin

  val LiitteenToimitusosoiteModel: String =
    """    LiitteenToimitusosoite:
      |      type: object
      |      properties:
      |        osoite:
      |          type: object
      |          description: Liitteen toimitusosoite
      |          $ref: '#/components/schemas/Osoite'
      |        sahkoposti:
      |          type: object
      |          description: Sähköpostiosoite, johon liite voidaan toimittaa
      |          $ref: '#/components/schemas/Teksti'
      |        verkkosivu:
      |          type: object
      |          description: Verkkosivu, jonka kautta liitteet voidaan toimittaa
      |          $ref: '#/components/schemas/Teksti'
      |""".stripMargin

  val LiiteModel: String =
    """    Liite:
      |      type: object
      |      properties:
      |        id:
      |          type: string
      |          description: Liitteen yksilöivä tunniste. Järjestelmän generoima.
      |          example: "ea596a9c-5940-497e-b5b7-aded3a2352a7"
      |        tyyppiKoodiUri:
      |          type: string
      |          description: Liitteen tyyppi. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/liitetyypitamm/1)
      |          example: liitetyypitamm_3#1
      |        nimi:
      |          type: object
      |          description: Liitteen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Nimi'
      |        kuvaus:
      |          type: object
      |          description: Liitteen Opintopolussa näytettävä kuvaus eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        toimitusaika:
      |          type: string
      |          description: Liitteen toimitusaika, jos ei ole sama kuin kaikilla hakukohteen liitteillä
      |          format: date-time
      |          example: 2019-08-23T09:55
      |        toimitustapa:
      |          type: string
      |          description: Liitteen toimitustapa, jos ei ole sama kuin kaikilla hakukohteen liitteillä
      |          example: "hakijapalvelu"
      |          enum:
      |            - hakijapalvelu
      |            - osoite
      |            - lomake
      |        toimitusosoite:
      |          type: object
      |          description: Liitteen toimitusosoite, jos ei ole sama kuin kaikilla hakukohteen liitteillä
      |          $ref: '#/components/schemas/LiitteenToimitusosoite'
      |""".stripMargin

  val PainotettuOppiaine: String =
    """    PainotettuOppiaine:
      |      type: object
      |      properties:
      |        koodiUrit:
      |          type: object
      |          description: Oppiaineen (ja mahdollisesti kielen) koodistokoodiURI
      |          $ref: '#/components/schemas/OppiaineKoodiUrit'
      |        painokerroin:
      |          type: number
      |          description: Oppiaineelle määritty opiskelijavalinnassa käytettävä painokerroin
      |          example: 1.2
      |""".stripMargin

  val OppiaineKoodiUrit: String =
    """    OppiaineKoodiUrit:
      |      type: object
      |      properties:
      |        oppiaine:
      |          type: string
      |          description: Oppiaineen koodiURI koodistossa
      |          example: "oppiaineetyleissivistava_b2"
      |        kieli:
      |          type: string
      |          description: Kielen koodiURI koodistossa
      |          example: "kieli_en"
      |""".stripMargin

  val HakukohteenLinjaModel: String =
    """    HakukohteenLinja:
      |      type: object
      |      properties:
      |        linja:
      |          type: string
      |          description: Linjan koodiUri, tai tyhjä arvo (= yleislinja)
      |          example: lukiopainotukset_0102#1
      |        alinHyvaksyttyKeskiarvo:
      |          type: number
      |          description: Linjan alin hyväksytty keskiarvo
      |          example: 8,2
      |        lisatietoa:
      |          type: object
      |          description: Lisätietoa keskiarvosta
      |          $ref: '#/components/schemas/Kuvaus'
      |        painotetutArvosanat:
      |          type: array
      |          description: Opiskelijavalinnassa käytettävät oppiaineet, joita painotetaan valintaa tehdessä
      |          items:
      |            $ref: '#/components/schemas/PainotettuOppiaine'
      |""".stripMargin

  val EnrichedDataModel: String =
    """    EnrichedData:
      |      type: object
      |      properties:
      |        esitysnimi:
      |          description: Koulutustyyppikohtainen esittämistä varten muodostettu nimi käytettäväksi kouta-uin puolella
      |          $ref: '#/components/schemas/Nimi'
      |""".stripMargin

  def models = List(HakukohdeListItemModel, HakukohdeModel, HakukohdeMetadataModel, LiiteModel, LiitteenToimitusosoiteModel,
    PainotettuOppiaine, OppiaineKoodiUrit, HakukohteenLinjaModel, EnrichedDataModel)
}

case class Hakukohde(oid: Option[HakukohdeOid] = None,
                     externalId: Option[String] = None,
                     toteutusOid: ToteutusOid,
                     hakuOid: HakuOid,
                     tila: Julkaisutila = Tallennettu,
                     esikatselu: Boolean = false,
                     nimi: Kielistetty = Map(),
                     hakukohdeKoodiUri: Option[String] = None,
                     jarjestyspaikkaOid: Option[OrganisaatioOid] = None,
                     hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                     hakulomakeAtaruId: Option[UUID] = None,
                     hakulomakeKuvaus: Kielistetty = Map(),
                     hakulomakeLinkki: Kielistetty = Map(),
                     kaytetaanHaunHakulomaketta: Option[Boolean] = None,
                     pohjakoulutusvaatimusKoodiUrit: Seq[String] = Seq(),
                     pohjakoulutusvaatimusTarkenne: Kielistetty = Map(),
                     muuPohjakoulutusvaatimus: Kielistetty = Map(),
                     toinenAsteOnkoKaksoistutkinto: Option[Boolean] = None,
                     kaytetaanHaunAikataulua: Option[Boolean] = None,
                     valintaperusteId: Option[UUID] = None,
                     liitteetOnkoSamaToimitusaika: Option[Boolean] = None,
                     liitteetOnkoSamaToimitusosoite: Option[Boolean] = None,
                     liitteidenToimitusaika: Option[LocalDateTime] = None,
                     liitteidenToimitustapa: Option[LiitteenToimitustapa] = None,
                     liitteidenToimitusosoite: Option[LiitteenToimitusosoite] = None,
                     liitteet: Seq[Liite] = Seq(),
                     valintakokeet: Seq[Valintakoe] = Seq(),
                     hakuajat: Seq[Ajanjakso] = Seq(),
                     metadata: Option[HakukohdeMetadata] = None, //TODO: Suurin osa hakukohteen kentistä pitäisi siirtää metadatan sisään!
                     muokkaaja: UserOid,
                     organisaatioOid: OrganisaatioOid,
                     kielivalinta: Seq[Kieli] = Seq(),
                     modified: Option[Modified],
                     _enrichedData: Option[EnrichedData] = None) extends PerustiedotWithOidAndOptionalNimi[HakukohdeOid, Hakukohde] {

  override def validate(): IsValid = and(
    super.validate(),
    assertValid(toteutusOid, "toteutusOid"),
    assertValid(hakuOid, "hakuOid"),
    // Joko hakukohdeKoodiUri tai nimi täytyy olla, mutta ei molempia!
    assertTrue(hakukohdeKoodiUri.nonEmpty != nimi.nonEmpty, "nimi", oneNotBoth("nimi", "hakukohdeKoodiUri")),
    validateIfDefined[String](
      hakukohdeKoodiUri,
      assertMatch(_, HakukohdeKoodiPattern, "hakukohdeKoodiUri")
    ),
    validateIfTrue(
      nimi.nonEmpty,
      validateKielistetty(kielivalinta, nimi, "nimi")
    ),
    validateIfNonEmpty[Ajanjakso](hakuajat, "hakuajat", _.validate(tila, kielivalinta, _)),
    validateIfNonEmpty[String](pohjakoulutusvaatimusKoodiUrit, "pohjakoulutusvaatimusKoodiUrit", assertMatch(_, PohjakoulutusvaatimusKoodiPattern, _)),
    validateIfDefined[LiitteenToimitusosoite](liitteidenToimitusosoite, _.validate(tila, kielivalinta, "liitteidenToimitusosoite")),
    validateIfNonEmpty[Liite](liitteet, "liitteet", _.validate(tila, kielivalinta, _)),
    validateIfNonEmpty[Valintakoe](valintakokeet, "valintakokeet", _.validate(tila, kielivalinta, _)),
    validateIfDefined[HakukohdeMetadata](metadata, _.validate(tila, kielivalinta, "metadata")),
    validateIfJulkaistu(tila, and(
      assertNotOptional(jarjestyspaikkaOid, "jarjestyspaikkaOid"),
      validateIfTrue(liitteetOnkoSamaToimitusaika.contains(true), assertNotOptional(liitteidenToimitusaika, "liitteidenToimitusaika")),
      validateIfTrue(liitteetOnkoSamaToimitusosoite.contains(true), assertNotOptional(liitteidenToimitustapa, "liitteidenToimitustapa")),
      validateIfTrue(liitteetOnkoSamaToimitusosoite.contains(true) && liitteidenToimitustapa.contains(MuuOsoite), assertNotOptional(liitteidenToimitusosoite, "liitteidenToimitusosoite")),
      validateHakulomake(hakulomaketyyppi, hakulomakeAtaruId, hakulomakeKuvaus, hakulomakeLinkki, kielivalinta),
      assertNotEmpty(pohjakoulutusvaatimusKoodiUrit, "pohjakoulutusvaatimusKoodiUrit"),
      validateOptionalKielistetty(kielivalinta, pohjakoulutusvaatimusTarkenne, "pohjakoulutusvaatimusTarkenne"),
      validateOptionalKielistetty(kielivalinta, muuPohjakoulutusvaatimus, "muuPohjakoulutusvaatimus"),
      assertNotOptional(kaytetaanHaunAikataulua, "kaytetaanHaunAikataulua"),
      assertNotOptional(kaytetaanHaunHakulomaketta, "kaytetaanHaunHakulomaketta"),
      validateIfTrue(kaytetaanHaunAikataulua.contains(false), assertNotEmpty(hakuajat, "hakuajat")),
      validateIfTrue(kaytetaanHaunHakulomaketta.contains(false), assertNotOptional(hakulomaketyyppi, "hakulomaketyyppi")),
    ))
  )

  override def validateOnJulkaisu(): IsValid = and(
    validateIfNonEmpty[Ajanjakso](hakuajat, "hakuajat", _.validateOnJulkaisu(_)),
    validateIfDefined[LocalDateTime](liitteidenToimitusaika, assertInFuture(_, "liitteidenToimitusaika")),
    validateIfNonEmpty[Liite](liitteet, "liitteet", _.validateOnJulkaisu(_)),
    validateIfNonEmpty[Valintakoe](valintakokeet, "valintakokeet", _.validateOnJulkaisu(_))
  )

  def withOid(oid: HakukohdeOid): Hakukohde = copy(oid = Some(oid))

  override def withModified(modified: Modified): Hakukohde = copy(modified = Some(modified))

  def withMuokkaaja(oid: UserOid): Hakukohde = this.copy(muokkaaja = oid)
}

case class Liite(id: Option[UUID] = None,
                 tyyppiKoodiUri: Option[String],
                 nimi: Kielistetty = Map(),
                 kuvaus: Kielistetty = Map(),
                 toimitusaika: Option[LocalDateTime] = None,
                 toimitustapa: Option[LiitteenToimitustapa] = None,
                 toimitusosoite: Option[LiitteenToimitusosoite] = None) extends ValidatableSubEntity {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = {
    and(
      validateIfDefined[LiitteenToimitusosoite](toimitusosoite, _.validate(tila, kielivalinta, s"$path.toimitusosoite")),
      validateIfDefined[String](tyyppiKoodiUri, assertMatch(_, LiiteTyyppiKoodiPattern, s"$path.tyyppiKoodiUri")),
      validateIfJulkaistu(tila, and(
        validateOptionalKielistetty(kielivalinta, nimi, s"$path.nimi"),
        validateOptionalKielistetty(kielivalinta, kuvaus, s"$path.kuvaus"),
        validateIfTrue(toimitustapa.contains(MuuOsoite), assertNotOptional(toimitusosoite, s"$path.toimitusosoite"))
      ))
    )
  }

  override def validateOnJulkaisu(path: String): IsValid =
    validateIfDefined[LocalDateTime](toimitusaika, assertInFuture(_, s"$path.toimitusaika"))
}

case class LiitteenToimitusosoite(osoite: Osoite,
                                  sahkoposti: Option[String] = None,
                                  verkkosivu: Option[String] = None
                                 ) extends ValidatableSubEntity {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    osoite.validate(tila, kielivalinta, s"$path.osoite"),
    validateIfDefined[String](sahkoposti, assertValidEmail(_, s"$path.sahkoposti")),
    validateIfDefined[String](verkkosivu, assertValidUrl(_, s"$path.verkkosivu")),
  )
}

case class OppiaineKoodiUrit(oppiaine: Option[String], kieli: Option[String]) extends ValidatableSubEntity {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    assertNotOptional(oppiaine, s"$path.oppiaine"),
    validateIfDefined(oppiaine, assertMatch(_, OppiaineKoodiPattern, s"$path.oppiaine")),
    validateIfDefined(kieli, assertMatch(_, KieliKoodiPattern, s"$path.kieli"))
  )
}

case class PainotettuOppiaine(koodiUrit: Option[OppiaineKoodiUrit] = None, painokerroin: Option[Double]) extends ValidatableSubEntity{
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfJulkaistu(tila, and(
      assertNotOptional(koodiUrit, s"$path.koodiUrit"),
      validateIfDefined[OppiaineKoodiUrit](koodiUrit, _.validate(tila, kielivalinta, s"$path.koodiUrit")),
      assertNotOptional(painokerroin, s"$path.painokerroin"),
      validateIfDefined[Double](painokerroin, assertNotNegative(_, s"$path.painokerroin")))
    ),
  )
}

case class HakukohteenLinja(linja: Option[String] = None, // NOTE: None tarkoittaa Yleislinjaa
                            alinHyvaksyttyKeskiarvo: Option[Double] = None,
                            lisatietoa: Kielistetty = Map(),
                            painotetutArvosanat: Seq[PainotettuOppiaine] = Seq()) extends ValidatableSubEntity{
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfDefined[Double](alinHyvaksyttyKeskiarvo, assertNotNegative(_, s"$path.alinHyvaksyttyKeskiarvo")),
    validateIfJulkaistu(tila, validateOptionalKielistetty(kielivalinta, lisatietoa, s"$path.lisatietoa")),
    validateIfNonEmpty[PainotettuOppiaine](painotetutArvosanat, s"$path.painotetutArvosanat", _.validate(tila, kielivalinta, _))
  )
}

case class HakukohdeMetadata(valintakokeidenYleiskuvaus: Kielistetty = Map(),
                             valintaperusteenValintakokeidenLisatilaisuudet: Seq[ValintakokeenLisatilaisuudet] = Seq(),
                             kynnysehto: Kielistetty = Map(),
                             koulutuksenAlkamiskausi: Option[KoulutuksenAlkamiskausi] = None,
                             kaytetaanHaunAlkamiskautta: Option[Boolean] = None,
                             aloituspaikat: Option[Aloituspaikat] = None,
                             // hakukohteenLinja löytyy vain lukiohakukohteilta (pakollisena)
                             hakukohteenLinja: Option[HakukohteenLinja] = None,
                             uudenOpiskelijanUrl: Option[String] = None,
                             isMuokkaajaOphVirkailija: Option[Boolean]
                            ) extends ValidatableSubEntity {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfDefined[KoulutuksenAlkamiskausi](koulutuksenAlkamiskausi, _.validate(tila, kielivalinta, s"$path.koulutuksenAlkamiskausi")),
    assertNotOptional(kaytetaanHaunAlkamiskautta, s"$path.kaytetaanHaunAlkamiskautta"),
    validateIfTrue(kaytetaanHaunAlkamiskautta.contains(false), assertNotOptional(koulutuksenAlkamiskausi, s"$path.koulutuksenAlkamiskausi")),
    validateIfNonEmpty[ValintakokeenLisatilaisuudet](valintaperusteenValintakokeidenLisatilaisuudet, s"$path.valintaperusteenValintakokeidenLisatilaisuudet", _.validate(tila, kielivalinta, _)),
    validateIfJulkaistu(tila, and(
      validateOptionalKielistetty(kielivalinta, valintakokeidenYleiskuvaus, s"$path.valintakokeidenYleiskuvaus"),
      validateOptionalKielistetty(kielivalinta, kynnysehto, s"$path.kynnysehto"),
      assertNotOptional(aloituspaikat, s"$path.aloituspaikat"),
      validateIfDefined[Aloituspaikat](aloituspaikat, _.validate(tila, kielivalinta, s"$path.aloituspaikat")),
      // NOTE: hakukohteenLinja validoidaan pakolliseksi lukiotyyppisille HakukohdeServicessä
      validateIfDefined[HakukohteenLinja](hakukohteenLinja, _.validate(tila, kielivalinta, s"$path.hakukohteenLinja"))
    )),
    validateIfDefined[String](uudenOpiskelijanUrl, assertValidUrl(_, s"$path.uudenOpiskelijanUrl"))
  )

  override def validateOnJulkaisu(path: String): IsValid = and(
    validateIfDefined[KoulutuksenAlkamiskausi](koulutuksenAlkamiskausi, _.validateOnJulkaisu(s"$path.koulutuksenAlkamiskausi")),
    validateIfNonEmpty[ValintakokeenLisatilaisuudet](valintaperusteenValintakokeidenLisatilaisuudet, s"$path.valintaperusteenValintakokeidenLisatilaisuudet", _.validateOnJulkaisu(_))
  )
}

case class HakukohdeListItem(oid: HakukohdeOid,
                             toteutusOid: ToteutusOid,
                             hakuOid: HakuOid,
                             valintaperusteId: Option[UUID],
                             nimi: Kielistetty,
                             hakukohdeKoodiUri: Option[String] = None,
                             tila: Julkaisutila,
                             jarjestyspaikkaOid: Option[OrganisaatioOid],
                             organisaatioOid: OrganisaatioOid,
                             muokkaaja: UserOid,
                             modified: Modified) extends OidListItem
object HakukohdeListItem {
  def apply(e: HakukohdeListItemEnriched): HakukohdeListItem = {
    new HakukohdeListItem(e.oid, e.toteutusOid, e.hakuOid, e.valintaperusteId, e.nimi, e.hakukohdeKoodiUri, e.tila, e.jarjestyspaikkaOid, e.organisaatioOid, e.muokkaaja, e.modified)
  }
}

case class HakukohdeListItemEnriched(
  oid: HakukohdeOid,
  toteutusOid: ToteutusOid,
  hakuOid: HakuOid,
  valintaperusteId: Option[UUID],
  nimi: Kielistetty,
  hakukohdeKoodiUri: Option[String] = None,
  tila: Julkaisutila,
  jarjestyspaikkaOid: Option[OrganisaatioOid],
  organisaatioOid: OrganisaatioOid,
  muokkaaja: UserOid,
  modified: Modified,
  metadata: Option[ToteutusMetadata]
)

object HakukohdeListItemEnriched {
  def apply(
    oid: HakukohdeOid,
    toteutusOid: ToteutusOid,
    hakuOid: HakuOid,
    valintaperusteId: Option[UUID],
    nimi: Kielistetty,
    hakukohdeKoodiUri: Option[String] = None,
    tila: Julkaisutila,
    jarjestyspaikkaOid: Option[OrganisaatioOid],
    organisaatioOid: OrganisaatioOid,
    muokkaaja: UserOid,
    modified: Modified,
    metadata: Option[ToteutusMetadata]
  ): HakukohdeListItemEnriched = {
    val esitysnimi = HakukohdeService.generateHakukohdeEsitysnimi(
      Hakukohde(
        oid = Some(oid),
        toteutusOid = toteutusOid,
        hakuOid = hakuOid,
        nimi = nimi,
        muokkaaja = muokkaaja,
        organisaatioOid = organisaatioOid,
        modified = Some(modified)),
      metadata)
    new HakukohdeListItemEnriched(
      oid,
      toteutusOid,
      hakuOid,
      valintaperusteId,
      esitysnimi,
      hakukohdeKoodiUri,
      tila,
      jarjestyspaikkaOid,
      organisaatioOid,
      muokkaaja,
      modified,
      metadata)
  }
}

case class EnrichedData(esitysnimi: Kielistetty = Map(), muokkaajanNimi: String)
