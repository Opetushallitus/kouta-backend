package fi.oph.kouta.domain

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.domain.oid._
import fi.oph.kouta.validation.{IsValid, ValidatableSubEntity}
import fi.oph.kouta.validation.Validations._

package object hakukohde {

  val HakukohdeModel =
    """    Hakukohde:
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
      |        tila:
      |          type: string
      |          example: "julkaistu"
      |          enum:
      |            - julkaistu
      |            - arkistoitu
      |            - tallennettu
      |          description: Haun julkaisutila. Jos hakukohde on julkaistu, se näkyy oppijalle Opintopolussa.
      |        nimi:
      |          type: object
      |          description: Hakukohteen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Nimi'
      |        alkamiskausiKoodiUri:
      |          type: string
      |          description: Hakukohteen koulutusten alkamiskausi, jos ei käytetä haun alkamiskautta.
      |            Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kausi/1)
      |          example: kausi_k#1
      |        alkamisvuosi:
      |          type: string
      |          description: Hakukohteen koulutusten alkamisvuosi, jos ei käytetä haun alkamisvuotta
      |          example: 2020
      |        kaytetaanHaunAlkamiskautta:
      |          type: boolean
      |          description: Käytetäänkö haun alkamiskautta ja -vuotta vai onko hakukohteelle määritelty oma alkamisajankohta?
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
      |          allOf:
      |            - $ref: '#/components/schemas/Kuvaus'
      |        hakulomakeLinkki:
      |          type: object
      |          description: Hakulomakkeen linkki eri kielillä. Kielet on määritetty haun kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Linkki'
      |        kaytetaanHaunHakulomaketta:
      |          type: boolean
      |          description: Käytetäänkö haun hakulomaketta vai onko hakukohteelle määritelty oma hakulomake?
      |        aloituspaikat:
      |          type: integer
      |          description: Hakukohteen aloituspaikkojen lukumäärä
      |          example: 100
      |        minAloituspaikat:
      |          type: integer
      |          description: Hakukohteen aloituspaikkojen minimimäärä
      |          example: 75
      |        maxAloituspaikat:
      |          type: integer
      |          description: Hakukohteen aloituspaikkojen maksimimäärä
      |          example: 110
      |        ensikertalaisenAloituspaikat:
      |          type: integer
      |          description: Hakukohteen ensikertalaisen aloituspaikkojen lukumäärä
      |          example: 50
      |        minEnsikertalaisenAloituspaikat:
      |          type: integer
      |          description: Hakukohteen ensikertalaisen aloituspaikkojen minimimäärä
      |          example: 45
      |        maxEnsikertalaisenAloituspaikat:
      |          type: integer
      |          description: Hakukohteen ensikertalaisen aloituspaikkojen maksimimäärä
      |          example: 60
      |        pohjakoulutusvaatimusKoodiUrit:
      |          type: array
      |          description: Lista toisen asteen hakukohteen pohjakoulutusvaatimuksista. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/pohjakoulutusvaatimustoinenaste/1)
      |          items:
      |            type: string
      |          example:
      |            - pohjakoulutusvaatimustoinenaste_pk#1
      |            - pohjakoulutusvaatimustoinenaste_yo#1
      |        muuPohjakoulutusvaatimus:
      |          type: object
      |          description: Hakukohteen muiden pohjakoulutusvaatimusten kuvaus eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Kuvaus'
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
      |          allOf:
      |            - $ref: '#/components/schemas/LiitteenToimitusosoite'
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
      |           example: 2019-08-23T09:55
      |""".stripMargin

  val HakukohdeListItemModel =
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
      |          description: Hakukohteen julkaisutila. Jos haku on julkaistu, se näkyy oppijalle Opintopolussa.
      |        nimi:
      |          type: object
      |          description: Hakukohteen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Nimi'
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
      |           example: 2019-08-23T09:55
      |""".stripMargin

  val LiitteenToimitusosoiteModel =
    """    LiitteenToimitusosoite:
      |      type: object
      |      properties:
      |        osoite:
      |          type: object
      |          description: Liitteen toimitusosoite
      |          allOf:
      |            - $ref: '#/components/schemas/Osoite'
      |        sahkoposti:
      |          type: object
      |          description: Sähköpostiosoite, johon liite voidaan toimittaa
      |          allOf:
      |            - $ref: '#/components/schemas/Teksti'
      |""".stripMargin

  val LiiteModel =
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
      |          allOf:
      |            - $ref: '#/components/schemas/Nimi'
      |        kuvaus:
      |          type: object
      |          description: Liitteen Opintopolussa näytettävä kuvaus eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Kuvaus'
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
      |          allOf:
      |            - $ref: '#/components/schemas/LiitteenToimitusosoite'
      |""".stripMargin

  def models = List(HakukohdeListItemModel, HakukohdeModel, LiiteModel, LiitteenToimitusosoiteModel)
}

case class Hakukohde(oid: Option[HakukohdeOid] = None,
                     toteutusOid: ToteutusOid,
                     hakuOid: HakuOid,
                     tila: Julkaisutila = Tallennettu,
                     nimi: Kielistetty = Map(),
                     alkamiskausiKoodiUri: Option[String] = None,
                     alkamisvuosi: Option[String] = None,
                     kaytetaanHaunAlkamiskautta: Option[Boolean] = None,
                     hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                     hakulomakeAtaruId: Option[UUID] = None,
                     hakulomakeKuvaus: Kielistetty = Map(),
                     hakulomakeLinkki: Kielistetty = Map(),
                     kaytetaanHaunHakulomaketta: Option[Boolean] = None,
                     aloituspaikat: Option[Int] = None,
                     minAloituspaikat: Option[Int] = None,
                     maxAloituspaikat: Option[Int] = None,
                     ensikertalaisenAloituspaikat: Option[Int] = None,
                     minEnsikertalaisenAloituspaikat: Option[Int] = None,
                     maxEnsikertalaisenAloituspaikat: Option[Int] = None,
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
                     liitteet: List[Liite] = List(),
                     valintakokeet: List[Valintakoe] = List(),
                     hakuajat: List[Ajanjakso] = List(),
                     muokkaaja: UserOid,
                     organisaatioOid: OrganisaatioOid,
                     kielivalinta: Seq[Kieli] = Seq(),
                     modified: Option[LocalDateTime]) extends PerustiedotWithOid[HakukohdeOid, Hakukohde] {

  override def validate(): IsValid = and(
    super.validate(),
    assertValid(toteutusOid, "toteutusOid"),
    assertValid(hakuOid, "hakuOid"),
    validateIfDefined[String](alkamiskausiKoodiUri, assertMatch(_, KausiKoodiPattern, "alkamiskausiKoodiUri")),
    validateIfNonEmpty[Ajanjakso](hakuajat, "hakuajat", _.validate(tila, kielivalinta, _)),
    validateIfNonEmpty[String](pohjakoulutusvaatimusKoodiUrit, "pohjakoulutusvaatimusKoodiUrit", assertMatch(_, PohjakoulutusvaatimusKoodiPattern, _)),
    validateIfDefined[Int](aloituspaikat, assertNotNegative(_, "aloituspaikat")),
    validateIfDefined[Int](minAloituspaikat, assertNotNegative(_, "minAloituspaikat")),
    validateIfDefined[Int](maxAloituspaikat, assertNotNegative(_, "maxAloituspaikat")),
    validateMinMax(minAloituspaikat, maxAloituspaikat, "minAloituspaikat"),
    validateIfDefined[Int](ensikertalaisenAloituspaikat, assertNotNegative(_, "ensikertalaisenAloituspaikat")),
    validateIfDefined[Int](minEnsikertalaisenAloituspaikat, assertNotNegative(_, "minEnsikertalaisenAloituspaikat")),
    validateIfDefined[Int](maxEnsikertalaisenAloituspaikat, assertNotNegative(_, "maxEnsikertalaisenAloituspaikat")),
    validateMinMax(minEnsikertalaisenAloituspaikat, maxEnsikertalaisenAloituspaikat, "minEnsikertalaisenAloituspaikat"),
    validateIfDefined[LiitteenToimitusosoite](liitteidenToimitusosoite, _.validate(tila, kielivalinta, "liitteidenToimitusosoite")),
    validateIfNonEmpty[Liite](liitteet, "liitteet", _.validate(tila, kielivalinta, _)),
    validateIfNonEmpty[Valintakoe](valintakokeet, "valintakokeet", _.validate(tila, kielivalinta, _)),
    validateIfJulkaistu(tila, and(
      validateIfDefined[String](alkamisvuosi, assertMatch(_, VuosiPattern,"alkamisvuosi")),
      validateIfTrue(liitteetOnkoSamaToimitusaika.contains(true), assertNotOptional(liitteidenToimitusaika, "liitteidenToimitusaika")),
      validateIfTrue(liitteetOnkoSamaToimitusosoite.contains(true), assertNotOptional(liitteidenToimitusosoite, "liitteidenToimitusosoite")),
      validateHakulomake(hakulomaketyyppi, hakulomakeAtaruId, hakulomakeKuvaus, hakulomakeLinkki, kielivalinta),
      assertNotEmpty(pohjakoulutusvaatimusKoodiUrit, "pohjakoulutusvaatimusKoodiUrit"),
      validateOptionalKielistetty(kielivalinta, pohjakoulutusvaatimusTarkenne, "pohjakoulutusvaatimusTarkenne"),
      validateOptionalKielistetty(kielivalinta, muuPohjakoulutusvaatimus, "muuPohjakoulutusvaatimus"),
      assertNotOptional(kaytetaanHaunAikataulua, "kaytetaanHaunAikataulua"),
      assertNotOptional(kaytetaanHaunHakulomaketta, "kaytetaanHaunHakulomaketta"),
      assertNotOptional(kaytetaanHaunAlkamiskautta, "kaytetaanHaunAlkamiskautta"),
      validateIfTrue(kaytetaanHaunAikataulua.contains(false), assertNotEmpty(hakuajat, "hakuajat")),
      validateIfTrue(kaytetaanHaunHakulomaketta.contains(false), assertNotOptional(hakulomaketyyppi, "hakulomaketyyppi")),
      validateIfTrue(kaytetaanHaunAlkamiskautta.contains(false), and(
        assertNotOptional(alkamisvuosi, "alkamisvuosi"),
        assertNotOptional(alkamiskausiKoodiUri, "alkamiskausiKoodiUri")
      )),
    ))
  )

  override def validateOnJulkaisu(): IsValid = and(
    validateIfNonEmpty[Ajanjakso](hakuajat, "hakuajat", _.validateOnJulkaisu(_)),
    validateIfDefined[String](alkamisvuosi, assertAlkamisvuosiInFuture(_, "alkamisvuosi")),
    validateIfDefined[LocalDateTime](liitteidenToimitusaika, assertInFuture(_, "liitteidenToimitusaika")),
    validateIfNonEmpty[Liite](liitteet, "liitteet", _.validateOnJulkaisu(_)),
    validateIfNonEmpty[Valintakoe](valintakokeet, "valintakokeet", _.validateOnJulkaisu(_))
  )

  def withOid(oid: HakukohdeOid): Hakukohde = copy(oid = Some(oid))

  override def withModified(modified: LocalDateTime): Hakukohde = copy(modified = Some(modified))
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
                                  sahkoposti: Option[String] = None) extends ValidatableSubEntity {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    osoite.validate(tila, kielivalinta, s"$path.osoite"),
    validateIfDefined[String](sahkoposti, assertValidEmail(_, s"$path.sahkoposti"))
  )
}

case class HakukohdeListItem(oid: HakukohdeOid,
                             toteutusOid: ToteutusOid,
                             hakuOid: HakuOid,
                             valintaperusteId: Option[UUID],
                             nimi: Kielistetty,
                             tila: Julkaisutila,
                             organisaatioOid: OrganisaatioOid,
                             muokkaaja: UserOid,
                             modified: LocalDateTime) extends OidListItem
