package fi.oph.kouta.domain

import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, ValidatableSubEntity}

import java.time.LocalDateTime

package object toteutusMetadata {

  val Opetus =
    """    Opetus:
      |      type: object
      |      properties:
      |        opetuskieliKoodiUrit:
      |          type: array
      |          description: Lista koulutuksen toteutuksen opetuskielistä. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/oppilaitoksenopetuskieli/1)
      |          items:
      |            type: string
      |            example:
      |              - oppilaitoksenopetuskieli_1#1
      |              - oppilaitoksenopetuskieli_2#1
      |        opetuskieletKuvaus:
      |          type: object
      |          description: Koulutuksen toteutuksen opetuskieliä tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        opetusaikaKoodiUrit:
      |          type: array
      |          description: Lista koulutuksen toteutuksen opetusajoista. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opetusaikakk/1)
      |          items:
      |            type: string
      |            example:
      |              - opetusaikakk_1#1
      |              - opetusaikakk_2#1
      |        opetusaikaKuvaus:
      |          type: object
      |          description: Koulutuksen toteutuksen opetusaikoja tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        opetustapaKoodiUrit:
      |          type: array
      |          description: Lista koulutuksen toteutuksen opetustavoista. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opetuspaikkakk/1)
      |          items:
      |            type: string
      |            example:
      |              - opetuspaikkakk_2#1
      |              - opetuspaikkakk_2#1
      |        opetustapaKuvaus:
      |          type: object
      |          description: Koulutuksen toteutuksen opetustapoja tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        onkoMaksullinen:
      |          type: boolean
      |          decription: "Onko koulutus maksullinen?"
      |        maksullisuusKuvaus:
      |          type: object
      |          description: Koulutuksen toteutuksen maksullisuutta tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        maksunMaara:
      |          type: double
      |          description: "Koulutuksen toteutuksen maksun määrä euroissa?"
      |          example: 220.50
      |        koulutuksenAlkamiskausiUUSI:
      |          type: object
      |          description: Koulutuksen alkamiskausi
      |          $ref: '#/components/schemas/KoulutuksenAlkamiskausi'
      |        lisatiedot:
      |          type: array
      |          description: Koulutuksen toteutukseen liittyviä lisätietoja, jotka näkyvät oppijalle Opintopolussa
      |          items:
      |            type: object
      |            $ref: '#/components/schemas/Lisatieto'
      |        onkoStipendia:
      |          type: boolean
      |          description: "Onko koulutukseen stipendiä?"
      |        stipendinMaara:
      |          type: double
      |          description: Koulutuksen toteutuksen stipendin määrä.
      |          example: 10.0
      |        stipendinKuvaus:
      |          type: object
      |          description: Koulutuksen toteutuksen stipendiä tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        suunniteltuKestoVuodet:
      |          type: integer
      |          description: "Koulutuksen suunniteltu kesto vuosina"
      |          example: 2
      |        suunniteltuKestoKuukaudet:
      |          type: integer
      |          description: "Koulutuksen suunniteltu kesto kuukausina"
      |          example: 2
      |        suunniteltuKestoKuvaus:
      |          type: object
      |          description: "Koulutuksen suunnitellun keston kuvaus eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa."
      |          $ref: '#/components/schemas/Kuvaus'
      |        ammatillinenPerustutkintoErityisopetuksena:
      |          type: boolean
      |          description: Onko koulutuksen tyyppi \"Ammatillinen perustutkinto erityisopetuksena\"?
      |""".stripMargin

  val ToteutusMetadata =
    """    ToteutusMetadata:
      |      type: object
      |      properties:
      |        kuvaus:
      |          type: object
      |          description: Toteutuksen kuvausteksti eri kielillä. Kielet on määritetty toteutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        opetus:
      |          type: object
      |          $ref: '#/components/schemas/Opetus'
      |        yhteyshenkilot:
      |          type: array
      |          description: Lista toteutuksen yhteyshenkilöistä
      |          items:
      |            $ref: '#/components/schemas/Yhteyshenkilo'
      |        asiasanat:
      |          type: array
      |          description: Lista toteutukseen liittyvistä asiasanoista, joiden avulla opiskelija voi hakea koulutusta Opintopolusta
      |          items:
      |            $ref: '#/components/schemas/Asiasana'
      |        ammattinimikkeet:
      |          type: array
      |          description: Lista toteutukseen liittyvistä ammattinimikkeistä, joiden avulla opiskelija voi hakea koulutusta Opintopolusta
      |          items:
      |            $ref: '#/components/schemas/Ammattinimike'
      |""".stripMargin

  val KorkeakouluOsaamisala =
    """    KorkeakouluOsaamisala:
      |      type: object
      |      properties:
      |        nimi:
      |          type: object
      |          description: Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. nimi
      |          $ref: '#/components/schemas/Nimi'
      |        kuvaus:
      |          type: object
      |          description: Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. kuvaus
      |          $ref: '#/components/schemas/Kuvaus'
      |        linkki:
      |          type: object
      |          description: Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. linkki
      |          $ref: '#/components/schemas/Linkki'
      |        otsikko:
      |          type: object
      |          description: Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. linkin otsikko
      |          $ref: '#/components/schemas/Teksti'
      |""".stripMargin

  val Osaamisala =
    """    Osaamisala:
      |      type: object
      |      properties:
      |        koodiUri:
      |          type: string
      |          description: Osaamisalan koodi URI. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/osaamisala/1)
      |          example: osaamisala_0001#1
      |        linkki:
      |          type: object
      |          description: Osaamisalan linkki
      |          $ref: '#/components/schemas/Linkki'
      |        otsikko:
      |          type: object
      |          description: Osaamisalan linkin otsikko
      |          $ref: '#/components/schemas/Teksti'
      |""".stripMargin

  val KorkeakouluToteutusMetadata =
    """    KorkeakouluToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/ToteutusMetadata'
      |      properties:
      |        alemmanKorkeakoulututkinnonOsaamisalat:
      |          type: array
      |          description: Lista alemman korkeakoulututkinnon erikoistumisalojen, opintosuuntien, pääaineiden tms. kuvauksista.
      |          items:
      |            $ref: '#/components/schemas/KorkeakouluOsaamisala'
      |        ylemmanKorkeakoulututkinnonOsaamisalat:
      |          type: array
      |          items:
      |            $ref: '#/components/schemas/KorkeakouluOsaamisala'
      |          description: Lista ylemmän korkeakoulututkinnon erikoistumisalojen, opintosuuntien, pääaineiden tms. kuvauksista.
      |""".stripMargin

  val YliopistoToteutusMetadata =
    """    YliopistoToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KorkeakouluToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: yo
      |              enum:
      |                - yo
      |""".stripMargin

  val AmmattikorkeaToteutusMetadata =
    """    AmmattikorkeaToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KorkeakouluToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: amk
      |              enum:
      |                - amk
      |""".stripMargin

  val AmmatillinenToteutusMetadata =
    """    AmmatillinenToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/ToteutusMetadata'
      |        - type: object
      |          properties:
      |            osaamisalat:
      |              type: array
      |              items:
      |                $ref: '#/components/schemas/Osaamisala'
      |              description: Lista ammatillisen koulutuksen osaamisalojen kuvauksia
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: amm
      |              enum:
      |                - amm
      |""".stripMargin

  val TutkintoonJohtamatonToteutusMetadata =
    """    TutkintoonJohtamatonToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/ToteutusMetadata'
      |        - type: object
      |          properties:
      |            osaamisalat:
      |              type: array
      |              items:
      |                $ref: '#/components/schemas/Osaamisala'
      |              description: Lista tutkintoon johtamattoman koulutuksen osaamisalojen kuvauksia
      |            hakutermi:
      |              type: object
      |              $ref: '#/components/schemas/Hakutermi'
      |            hakulomaketyyppi:
      |              type: string
      |              description: Hakulomakkeen tyyppi. Kertoo, käytetäänkö Atarun (hakemuspalvelun) hakulomaketta, muuta hakulomaketta
      |                (jolloin voidaan lisätä hakulomakkeeseen linkki) tai onko niin, ettei sähkököistä hakulomaketta ole lainkaan, jolloin sille olisi hyvä lisätä kuvaus.
      |              example: "ataru"
      |              enum:
      |                - ataru
      |                - haku-app
      |                - ei sähköistä
      |                - muu
      |            hakulomakeLinkki:
      |              type: object
      |              description: Hakulomakkeen linkki eri kielillä. Kielet on määritetty haun kielivalinnassa.
      |              $ref: '#/components/schemas/Linkki'
      |            lisatietoaHakeutumisesta:
      |              type: object
      |              description: Lisätietoa hakeutumisesta eri kielillä. Kielet on määritetty haun kielivalinnassa.
      |              $ref: '#/components/schemas/Teksti'
      |            lisatietoaValintaperusteista:
      |              type: object
      |              description: Lisätietoa valintaperusteista eri kielillä. Kielet on määritetty haun kielivalinnassa.
      |              $ref: '#/components/schemas/Teksti'
      |            hakuaika:
      |              type: array
      |              description: Toteutuksen hakuaika
      |              $ref: '#/components/schemas/Ajanjakso'
      |            aloituspaikat:
      |              type: integer
      |              description: Toteutuksen aloituspaikkojen lukumäärä
      |              example: 100
      |""".stripMargin

  val AmmatillinenTutkinnonOsaToteutusMetadata =
    """    AmmatillinenTutkinnonOsaToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/TutkintoonJohtamatonToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: amm-tutkinnon-osa
      |              enum:
      |                - amm-tutkinnon-osa
      |""".stripMargin

  val AmmatillinenOsaamisalaToteutusMetadata =
    """    AmmatillinenOsaamisalaToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/TutkintoonJohtamatonToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: amm-osaamisala
      |              enum:
      |                - amm-osaamisala
      |""".stripMargin

  val models = List(Opetus, ToteutusMetadata, KorkeakouluOsaamisala, Osaamisala, KorkeakouluToteutusMetadata,
    AmmattikorkeaToteutusMetadata, YliopistoToteutusMetadata, AmmatillinenToteutusMetadata,
    TutkintoonJohtamatonToteutusMetadata, AmmatillinenTutkinnonOsaToteutusMetadata, AmmatillinenOsaamisalaToteutusMetadata)
}

sealed trait ToteutusMetadata extends ValidatableSubEntity {
  val tyyppi: Koulutustyyppi
  val kuvaus: Kielistetty
  val opetus: Option[Opetus]
  val asiasanat: List[Keyword]
  val ammattinimikkeet: List[Keyword]
  val yhteyshenkilot: Seq[Yhteyshenkilo]

  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfDefined[Opetus](opetus, _.validate(tila, kielivalinta, s"$path.opetus")),
    validateIfNonEmpty[Yhteyshenkilo](yhteyshenkilot, s"$path.yhteyshenkilot", _.validate(tila, kielivalinta, _)),
    validateIfJulkaistu(tila, and(
      validateOptionalKielistetty(kielivalinta, kuvaus, s"$path.kuvaus"),
      assertNotOptional(opetus, s"$path.opetus"),
    ))
  )

  def allowSorakuvaus: Boolean = false

  override def validateOnJulkaisu(path: String): IsValid =
    validateIfDefined[Opetus](opetus, _.validateOnJulkaisu(s"$path.opetus"))
}

trait KorkeakoulutusToteutusMetadata extends ToteutusMetadata {
  val alemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala]
  val ylemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala]

  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    validateIfNonEmpty[KorkeakouluOsaamisala](alemmanKorkeakoulututkinnonOsaamisalat, s"$path.alemmanKorkeakoulututkinnonOsaamisalat", _.validate(tila, kielivalinta, _)),
    validateIfNonEmpty[KorkeakouluOsaamisala](ylemmanKorkeakoulututkinnonOsaamisalat, s"$path.ylemmanKorkeakoulututkinnonOsaamisalat", _.validate(tila, kielivalinta, _)),
  )
}

case class AmmatillinenToteutusMetadata(tyyppi: Koulutustyyppi = Amm,
                                        kuvaus: Kielistetty = Map(),
                                        osaamisalat: List[AmmatillinenOsaamisala] = List(),
                                        opetus: Option[Opetus] = None,
                                        asiasanat: List[Keyword] = List(),
                                        ammattinimikkeet: List[Keyword] = List(),
                                        yhteyshenkilot: Seq[Yhteyshenkilo] = Seq()) extends ToteutusMetadata {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    validateIfNonEmpty[AmmatillinenOsaamisala](osaamisalat, s"$path.osaamisalat", _.validate(tila, kielivalinta, _))
  )
}

trait TutkintoonJohtamatonToteutusMetadata extends ToteutusMetadata {
  def hakutermi: Option[Hakutermi]
  def hakulomaketyyppi: Option[Hakulomaketyyppi]
  def hakulomakeLinkki: Kielistetty
  def lisatietoaHakeutumisesta: Kielistetty
  def lisatietoaValintaperusteista: Kielistetty
  def hakuaika: Option[Ajanjakso]
  def aloituspaikat: Option[Int]

  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    validateIfNonEmpty(hakulomakeLinkki, s"$path.hakulomakeLinkki", assertValidUrl _),
    validateIfDefined[Ajanjakso](hakuaika, _.validate(tila, kielivalinta, s"$path.hakuaika")),
    validateIfDefined[Int](aloituspaikat, assertNotNegative(_, "aloituspaikat")),
    validateIfJulkaistu(tila, and(
      assertNotOptional(hakutermi, s"$path.hakutermi"),
      assertNotOptional(hakulomaketyyppi, s"$path.hakulomaketyyppi"),
      validateIfTrue(hakulomaketyyppi.exists(_ == MuuHakulomake), and(
        validateKielistetty(kielivalinta, lisatietoaHakeutumisesta, s"$path.lisatietoaHakeutumisesta"),
        validateKielistetty(kielivalinta, hakulomakeLinkki, s"$path.hakulomakeLinkki"),
        validateOptionalKielistetty(kielivalinta, lisatietoaValintaperusteista, s"$path.lisatietoaValintaperusteista"),
        assertNotOptional(hakuaika, s"$path.hakuaika")
      )),
      validateIfTrue(hakulomaketyyppi.exists(_ == EiSähköistä),
        validateKielistetty(kielivalinta, lisatietoaHakeutumisesta, s"$path.lisatietoaHakeutumisesta"))
    )))

  override def allowSorakuvaus: Boolean =
    hakulomaketyyppi.exists(_ == MuuHakulomake)
}

case class AmmatillinenTutkinnonOsaToteutusMetadata(tyyppi: Koulutustyyppi = AmmTutkinnonOsa,
                                                    kuvaus: Kielistetty = Map(),
                                                    opetus: Option[Opetus] = None,
                                                    asiasanat: List[Keyword] = List(),
                                                    ammattinimikkeet: List[Keyword] = List(),
                                                    yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                                    hakutermi: Option[Hakutermi] = None,
                                                    hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                                                    hakulomakeLinkki: Kielistetty = Map(),
                                                    lisatietoaHakeutumisesta: Kielistetty = Map(),
                                                    lisatietoaValintaperusteista: Kielistetty = Map(),
                                                    hakuaika: Option[Ajanjakso] = None,
                                                    aloituspaikat: Option[Int] = None) extends TutkintoonJohtamatonToteutusMetadata

case class AmmatillinenOsaamisalaToteutusMetadata(tyyppi: Koulutustyyppi = AmmOsaamisala,
                                                  kuvaus: Kielistetty = Map(),
                                                  opetus: Option[Opetus] = None,
                                                  asiasanat: List[Keyword] = List(),
                                                  ammattinimikkeet: List[Keyword] = List(),
                                                  yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                                  hakutermi: Option[Hakutermi] = None,
                                                  hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                                                  hakulomakeLinkki: Kielistetty = Map(),
                                                  lisatietoaHakeutumisesta: Kielistetty = Map(),
                                                  lisatietoaValintaperusteista: Kielistetty = Map(),
                                                  hakuaika: Option[Ajanjakso] = None,
                                                  aloituspaikat: Option[Int] = None) extends TutkintoonJohtamatonToteutusMetadata

case class YliopistoToteutusMetadata(tyyppi: Koulutustyyppi = Yo,
                                     kuvaus: Kielistetty = Map(),
                                     opetus: Option[Opetus] = None,
                                     asiasanat: List[Keyword] = List(),
                                     ammattinimikkeet: List[Keyword] = List(),
                                     yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                     alemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala] = Seq(),
                                     ylemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala] = Seq()) extends KorkeakoulutusToteutusMetadata

case class AmmattikorkeakouluToteutusMetadata(tyyppi: Koulutustyyppi = Amk,
                                              kuvaus: Kielistetty = Map(),
                                              opetus: Option[Opetus] = None,
                                              asiasanat: List[Keyword] = List(),
                                              ammattinimikkeet: List[Keyword] = List(),
                                              yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                              alemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala] = Seq(),
                                              ylemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala] = Seq()) extends KorkeakoulutusToteutusMetadata

trait Osaamisala extends ValidatableSubEntity {
  val linkki: Kielistetty
  val otsikko: Kielistetty

  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfNonEmpty(linkki, s"$path.linkki", assertValidUrl _),
    validateIfJulkaistu(tila, and(
      validateOptionalKielistetty(kielivalinta, linkki, s"$path.linkki"),
      validateOptionalKielistetty(kielivalinta, otsikko, s"$path.otsikko"),
    ))
  )
}

case class AmmatillinenOsaamisala(koodiUri: String,
                                  linkki: Kielistetty = Map(),
                                  otsikko: Kielistetty = Map()) extends Osaamisala {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    assertMatch(koodiUri, OsaamisalaKoodiPattern, s"$path.koodiUri")
  )
}

case class KorkeakouluOsaamisala(nimi: Kielistetty = Map(),
                                 kuvaus: Kielistetty = Map(),
                                 linkki: Kielistetty = Map(),
                                 otsikko: Kielistetty = Map()) extends Osaamisala {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    validateIfJulkaistu(tila, and(
      validateKielistetty(kielivalinta, nimi, s"$path.nimi"),
      validateOptionalKielistetty(kielivalinta, kuvaus, s"$path.kuvaus")
    ))
  )
}


case class Apuraha(min: Double,
                   max: Double,
                   yksikko: Apurahayksikko,
                   kuvaus: Kielistetty = Map())

case class Opetus(opetuskieliKoodiUrit: Seq[String] = Seq(),
                  opetuskieletKuvaus: Kielistetty = Map(),
                  opetusaikaKoodiUrit: Seq[String] = Seq(),
                  opetusaikaKuvaus: Kielistetty = Map(),
                  opetustapaKoodiUrit: Seq[String] = Seq(),
                  opetustapaKuvaus: Kielistetty = Map(),
                  onkoMaksullinen: Option[Boolean] = Some(false),
                  maksullisuusKuvaus: Kielistetty = Map(),
                  maksunMaara: Option[Double] = None,
                  koulutuksenAlkamiskausiUUSI: Option[KoulutuksenAlkamiskausi] = None, //Feature flag KTO-1036, myos swagger skeema
                  lisatiedot: Seq[Lisatieto] = Seq(),
                  onkoApuraha: Boolean = false,
                  apuraha: Option[Apuraha] = None,
                  onkoStipendia: Option[Boolean] = Some(false),
                  stipendinMaara: Option[Double] = None,
                  stipendinKuvaus: Kielistetty = Map(),
                  suunniteltuKestoVuodet: Option[Int] = None,
                  suunniteltuKestoKuukaudet: Option [Int] = None,
                  suunniteltuKestoKuvaus: Kielistetty = Map(),
                  ammatillinenPerustutkintoErityisopetuksena: Boolean = false) extends ValidatableSubEntity {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfNonEmpty[String](opetuskieliKoodiUrit, s"$path.opetuskieliKoodiUrit", assertMatch(_, OpetuskieliKoodiPattern, _)),
    validateIfNonEmpty[String](opetusaikaKoodiUrit, s"$path.opetusaikaKoodiUrit", assertMatch(_, OpetusaikaKoodiPattern, _)),
    validateIfNonEmpty[String](opetustapaKoodiUrit, s"$path.opetustapaKoodiUrit", assertMatch(_, OpetustapaKoodiPattern, _)),
    validateIfDefined[KoulutuksenAlkamiskausi](koulutuksenAlkamiskausiUUSI, _.validate(tila, kielivalinta, s"$path.koulutuksenAlkamiskausiUUSI")), //Feature flag KTO-1036
    validateIfNonEmpty[Lisatieto](lisatiedot, s"$path.lisatiedot", _.validate(tila, kielivalinta, _)),
    validateIfDefined[Double](stipendinMaara, assertNotNegative(_, s"$path.stipendinMaara")),
    validateIfDefined[Double](maksunMaara, assertNotNegative(_, s"$path.maksunMaara")),
    validateIfDefined[Int](suunniteltuKestoVuodet, assertNotNegative(_, s"$path.suunniteltuKestoVuodet")),
    validateIfDefined[Int](suunniteltuKestoKuukaudet, assertNotNegative(_, s"$path.suunniteltuKestoKuukaudet")),
    validateIfJulkaistu(tila, and(
      assertNotEmpty(opetuskieliKoodiUrit, s"$path.opetuskieliKoodiUrit"),
      assertNotEmpty(opetusaikaKoodiUrit, s"$path.opetusaikaKoodiUrit"),
      assertNotEmpty(opetustapaKoodiUrit, s"$path.opetustapaKoodiUrit"),
      validateOptionalKielistetty(kielivalinta, opetuskieletKuvaus, s"$path.opetuskieletKuvaus"),
      validateOptionalKielistetty(kielivalinta, opetusaikaKuvaus, s"$path.opetusaikaKuvaus"),
      validateOptionalKielistetty(kielivalinta, opetustapaKuvaus, s"$path.opetustapaKuvaus"),
      assertNotOptional(onkoMaksullinen, s"$path.onkoMaksullinen"),
      validateOptionalKielistetty(kielivalinta, maksullisuusKuvaus, s"$path.maksullisuusKuvaus"),
      validateIfTrue(onkoMaksullinen.contains(true), assertNotOptional(maksunMaara, s"$path.maksunMaara")),
      validateOptionalKielistetty(kielivalinta, suunniteltuKestoKuvaus, s"$path.suunniteltuKestoKuvaus")
    ))
  )

  override def validateOnJulkaisu(path: String): IsValid = and(
    validateIfDefined[KoulutuksenAlkamiskausi](koulutuksenAlkamiskausiUUSI, _.validateOnJulkaisu(s"$path.koulutuksenAlkamiskausiUUSI")) //Feature flag KTO-1036
  )
}
