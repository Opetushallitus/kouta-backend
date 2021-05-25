package fi.oph.kouta.domain

import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, ValidatableSubEntity}

import java.util.regex.Pattern

package object toteutusMetadata {

  val Opetus: String =
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
      |        maksullisuustyyppi:
      |          type: string
      |          description: Maksullisuuden tyyppi
      |          enum:
      |            - 'maksullinen'
      |            - 'maksuton'
      |            - 'lukuvuosimaksu'
      |        maksullisuusKuvaus:
      |          type: object
      |          description: Koulutuksen toteutuksen maksullisuutta tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        maksunMaara:
      |          type: double
      |          description: "Koulutuksen toteutuksen maksun määrä euroissa?"
      |          example: 220.50
      |        kielivalikoima:
      |          type: object
      |          description: Koulutuksen kielivalikoima
      |          $ref: '#/components/schemas/Kielivalikoima'
      |        koulutuksenAlkamiskausi:
      |          type: object
      |          description: Koulutuksen alkamiskausi
      |          $ref: '#/components/schemas/KoulutuksenAlkamiskausi'
      |        lisatiedot:
      |          type: array
      |          description: Koulutuksen toteutukseen liittyviä lisätietoja, jotka näkyvät oppijalle Opintopolussa
      |          items:
      |            type: object
      |            $ref: '#/components/schemas/Lisatieto'
      |        onkoApuraha:
      |          type: boolean
      |          description: Onko koulutukseen apurahaa?
      |        apuraha:
      |          type: object
      |          description: Koulutuksen apurahatiedot
      |          $ref: '#/components/schemas/Apuraha'
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
      |          description: "Koulutuksen toteutuksen suunnitellun keston kuvaus eri kielillä. Kielet on määritetty toteutuksen kielivalinnassa."
      |          $ref: '#/components/schemas/Kuvaus'
      |""".stripMargin

  val Apuraha: String =
    """    Apuraha:
      |      type: object
      |      properties:
      |        min:
      |          type: int
      |          description: Apurahan minimi euromäärä tai minimi prosenttiosuus lukuvuosimaksusta
      |          example: 100
      |        max:
      |          type: int
      |          description: Apurahan maksimi euromäärä tai maksimi prosenttiosuus lukuvuosimaksusta
      |          example: 200
      |        yksikko:
      |          type: string
      |          description: Apurahan yksikkö
      |          enum:
      |            - euro
      |            - prosentti
      |          example: euro
      |        kuvaus:
      |          type: object
      |          description: Koulutuksen toteutuksen apurahaa tarkentava kuvausteksti eri kielillä. Kielet on määritetty toteutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |""".stripMargin

  val Kielivalikoima: String =
    """    Kielivalikoima:
      |      type: object
      |      properties:
      |        A1JaA2Kielet:
      |          type: array
      |          description: Lista koulutuksen toteutuksen A1 ja A2 kielistä. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kieli/1)
      |          items:
      |            type: string
      |            example:
      |              - kieli_EN#1
      |              - kieli_FI#1
      |        B1Kielet:
      |          type: array
      |          description: Lista koulutuksen toteutuksen B1 kielistä. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kieli/1)
      |          items:
      |            type: string
      |            example:
      |              - kieli_EN#1
      |              - kieli_FI#1
      |        B2Kielet:
      |          type: array
      |          description: Lista koulutuksen toteutuksen B2 kielistä. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kieli/1)
      |          items:
      |            type: string
      |            example:
      |              - kieli_EN#1
      |              - kieli_FI#1
      |        B3Kielet:
      |          type: array
      |          description: Lista koulutuksen toteutuksen B3 kielistä. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kieli/1)
      |          items:
      |            type: string
      |            example:
      |              - kieli_EN#1
      |              - kieli_FI#1
      |        aidinkielet:
      |          type: array
      |          description: Lista koulutuksen toteutuksen äidinkielistä. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kieli/1)
      |          items:
      |            type: string
      |            example:
      |              - kieli_EN#1
      |              - kieli_FI#1
      |        muutKielet:
      |          type: array
      |          description: Lista koulutuksen toteutuksen muista kielistä. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kieli/1)
      |          items:
      |            type: string
      |            example:
      |              - kieli_EN#1
      |              - kieli_FI#1
      |""".stripMargin

  val ToteutusMetadata: String =
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

  val KorkeakouluOsaamisala: String =
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

  val Osaamisala: String =
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

  val KorkeakouluToteutusMetadata: String =
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

  val YliopistoToteutusMetadata: String =
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

  val AmmattikorkeaToteutusMetadata: String =
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

  val AmmatillinenToteutusMetadata: String =
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
      |            ammatillinenPerustutkintoErityisopetuksena:
      |              type: boolean
      |              description: Onko koulutuksen tyyppi \"Ammatillinen perustutkinto erityisopetuksena\"?
      |""".stripMargin

  val TutkintoonJohtamatonToteutusMetadata: String =
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

  val AmmatillinenTutkinnonOsaToteutusMetadata: String =
    """    AmmatillinenTutkinnonOsaToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/TutkintoonJohtamatonToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Toteutuksen metatiedon tyyppi
      |              example: amm-tutkinnon-osa
      |              enum:
      |                - amm-tutkinnon-osa
      |""".stripMargin

  val AmmatillinenOsaamisalaToteutusMetadata: String =
    """    AmmatillinenOsaamisalaToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/TutkintoonJohtamatonToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Toteutuksen metatiedon tyyppi
      |              example: amm-osaamisala
      |              enum:
      |                - amm-osaamisala
      |""".stripMargin

  val LukiolinjaTieto: String =
    """    LukiolinjaTieto:
      |      type: object
      |      description: Toteutuksen yksittäisen lukiolinjatiedon kentät
      |      properties:
      |        koodiUri:
      |          type: string
      |          description: Lukiolinjatiedon koodiUri.
      |        kuvaus:
      |          type: object
      |          description: Lukiolinjatiedon kuvaus eri kielillä. Kielet on määritetty toteutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |""".stripMargin

  val LukioToteutusMetadata: String =
    """    LukioToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/ToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Toteutuksen metatiedon tyyppi
      |              example: lk
      |              enum:
      |                - lk
      |            painotukset:
      |              type: array
      |              description: Lukio-toteutuksen painotukset. Taulukon alkioiden koodiUri-kentät viittaavat [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/lukiopainotukset/1).
      |              items:
      |                type: object
      |                $ref: '#/components/schemas/LukiolinjaTieto'
      |            erityisetKoulutustehtavat:
      |              type: array
      |              description: Lukio-toteutuksen erityiset koulutustehtävät. Taulukon alkioiden koodiUri-kentät viittaavat [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/lukiolinjaterityinenkoulutustehtava/1).
      |              items:
      |                type: object
      |                $ref: '#/components/schemas/LukiolinjaTieto'
      |""".stripMargin

  val models = List(Opetus, Apuraha, Kielivalikoima, ToteutusMetadata, KorkeakouluOsaamisala, Osaamisala, KorkeakouluToteutusMetadata,
    AmmattikorkeaToteutusMetadata, YliopistoToteutusMetadata, AmmatillinenToteutusMetadata,
    TutkintoonJohtamatonToteutusMetadata, AmmatillinenTutkinnonOsaToteutusMetadata, AmmatillinenOsaamisalaToteutusMetadata, LukiolinjaTieto, LukioToteutusMetadata)
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
                                        yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                        ammatillinenPerustutkintoErityisopetuksena: Boolean = false) extends ToteutusMetadata {
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
      validateIfTrue(hakulomaketyyppi.contains(MuuHakulomake), and(
        validateKielistetty(kielivalinta, lisatietoaHakeutumisesta, s"$path.lisatietoaHakeutumisesta"),
        validateKielistetty(kielivalinta, hakulomakeLinkki, s"$path.hakulomakeLinkki"),
        validateOptionalKielistetty(kielivalinta, lisatietoaValintaperusteista, s"$path.lisatietoaValintaperusteista"),
        assertNotOptional(hakuaika, s"$path.hakuaika")
      )),
      validateIfTrue(hakulomaketyyppi.contains(EiSähköistä),
        validateKielistetty(kielivalinta, lisatietoaHakeutumisesta, s"$path.lisatietoaHakeutumisesta"))
    )))

  override def allowSorakuvaus: Boolean =
    hakulomaketyyppi.contains(MuuHakulomake)
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

case class LukioToteutusMetadata(tyyppi: Koulutustyyppi = Lk,
                                 kuvaus: Kielistetty = Map(),
                                 opetus: Option[Opetus] = None,
                                 asiasanat: List[Keyword] = List(),
                                 ammattinimikkeet: List[Keyword] = List(),
                                 yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                 painotukset: Seq[LukiolinjaTieto] = Seq(),
                                 erityisetKoulutustehtavat: Seq[LukiolinjaTieto] = Seq()
                                ) extends ToteutusMetadata {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    validateIfNonEmpty[LukiolinjaTieto](
      painotukset, s"$path.painotukset", _.validate(tila, kielivalinta, LukioPainotusKoodiPattern, _)
    ),
    validateIfNonEmpty[LukiolinjaTieto](
      erityisetKoulutustehtavat, s"$path.erityisetKoulutustehtavat",
      _.validate(tila, kielivalinta, LukioErityinenKoulutustehtavaKoodiPattern, _)
    )
  )
}

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


case class Apuraha(min: Option[Int] = None,
                   max: Option[Int] = None,
                   yksikko: Option[Apurahayksikko] = None,
                   kuvaus: Kielistetty = Map()) extends ValidatableSubEntity {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateMinMax(min, max, s"$path.min"),
    validateIfDefined[Int](min, assertNotNegative(_, s"$path.min")),
    validateIfDefined[Int](max, assertNotNegative(_, s"$path.max")),
    validateIfTrue(yksikko.orNull == Prosentti, validateIfDefined(max, assertLessOrEqual(_, 100, s"$path.max"))),
    validateIfJulkaistu(tila, and(
      validateOptionalKielistetty(kielivalinta, kuvaus, s"$path.kuvaus"),
      assertNotOptional(min, s"$path.min"),
      assertNotOptional(max, s"$path.max")
    ))
  )
}

case class Opetus(opetuskieliKoodiUrit: Seq[String] = Seq(),
                  opetuskieletKuvaus: Kielistetty = Map(),
                  opetusaikaKoodiUrit: Seq[String] = Seq(),
                  opetusaikaKuvaus: Kielistetty = Map(),
                  opetustapaKoodiUrit: Seq[String] = Seq(),
                  opetustapaKuvaus: Kielistetty = Map(),
                  maksullisuustyyppi: Option[Maksullisuustyyppi] = None,
                  maksullisuusKuvaus: Kielistetty = Map(),
                  maksunMaara: Option[Double] = None,
                  kielivalikoima: Option[Kielivalikoima] = None,
                  koulutuksenAlkamiskausi: Option[KoulutuksenAlkamiskausi] = None,
                  lisatiedot: Seq[Lisatieto] = Seq(),
                  onkoApuraha: Boolean = false,
                  apuraha: Option[Apuraha] = None,
                  suunniteltuKestoVuodet: Option[Int] = None,
                  suunniteltuKestoKuukaudet: Option[Int] = None,
                  suunniteltuKestoKuvaus: Kielistetty = Map()) extends ValidatableSubEntity {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfNonEmpty[String](opetuskieliKoodiUrit, s"$path.opetuskieliKoodiUrit", assertMatch(_, OpetuskieliKoodiPattern, _)),
    validateIfNonEmpty[String](opetusaikaKoodiUrit, s"$path.opetusaikaKoodiUrit", assertMatch(_, OpetusaikaKoodiPattern, _)),
    validateIfNonEmpty[String](opetustapaKoodiUrit, s"$path.opetustapaKoodiUrit", assertMatch(_, OpetustapaKoodiPattern, _)),
    validateIfDefined[KoulutuksenAlkamiskausi](koulutuksenAlkamiskausi, _.validate(tila, kielivalinta, s"$path.koulutuksenAlkamiskausi")),
    validateIfDefined[Apuraha](apuraha, _.validate(tila, kielivalinta, s"$path.apuraha")),
    validateIfNonEmpty[Lisatieto](lisatiedot, s"$path.lisatiedot", _.validate(tila, kielivalinta, _)),
    validateIfDefined[Double](maksunMaara, assertNotNegative(_, s"$path.maksunMaara")),
    validateIfDefined[Int](suunniteltuKestoVuodet, assertNotNegative(_, s"$path.suunniteltuKestoVuodet")),
    validateIfDefined[Int](suunniteltuKestoKuukaudet, assertNotNegative(_, s"$path.suunniteltuKestoKuukaudet")),
    validateIfJulkaistu(tila, and(
      assertNotEmpty(opetuskieliKoodiUrit, s"$path.opetuskieliKoodiUrit"),
      assertNotEmpty(opetusaikaKoodiUrit, s"$path.opetusaikaKoodiUrit"),
      assertNotEmpty(opetustapaKoodiUrit, s"$path.opetustapaKoodiUrit"),
      validateIfTrue(onkoApuraha, assertNotOptional(apuraha, s"$path.apuraha")),
      validateOptionalKielistetty(kielivalinta, opetuskieletKuvaus, s"$path.opetuskieletKuvaus"),
      validateOptionalKielistetty(kielivalinta, opetusaikaKuvaus, s"$path.opetusaikaKuvaus"),
      validateOptionalKielistetty(kielivalinta, opetustapaKuvaus, s"$path.opetustapaKuvaus"),
      assertNotOptional(maksullisuustyyppi, s"$path.maksullisuustyyppi"),
      validateOptionalKielistetty(kielivalinta, maksullisuusKuvaus, s"$path.maksullisuusKuvaus"),
      validateIfTrue(maksullisuustyyppi.contains(Maksullinen) || maksullisuustyyppi.contains(Lukuvuosimaksu), assertNotOptional(maksunMaara, s"$path.maksunMaara")),
      validateOptionalKielistetty(kielivalinta, suunniteltuKestoKuvaus, s"$path.suunniteltuKestoKuvaus")
    ))
  )

  override def validateOnJulkaisu(path: String): IsValid = and(
    validateIfDefined[KoulutuksenAlkamiskausi](koulutuksenAlkamiskausi, _.validateOnJulkaisu(s"$path.koulutuksenAlkamiskausi"))
  )
}

case class Kielivalikoima(A1JaA2Kielet: Seq[String] = Seq(),
                          B1Kielet: Seq[String] = Seq(),
                          B2Kielet: Seq[String] = Seq(),
                          B3Kielet: Seq[String] = Seq(),
                          aidinkielet: Seq[String] = Seq(),
                          muutKielet: Seq[String] = Seq()) extends ValidatableSubEntity {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfNonEmpty[String](A1JaA2Kielet, s"$path.A1JaA2Kielet", assertMatch(_, KieliKoodiPattern, _)),
    validateIfNonEmpty[String](B1Kielet, s"$path.B1Kielet", assertMatch(_, KieliKoodiPattern, _)),
    validateIfNonEmpty[String](B2Kielet, s"$path.B2Kielet", assertMatch(_, KieliKoodiPattern, _)),
    validateIfNonEmpty[String](B3Kielet, s"$path.B3Kielet", assertMatch(_, KieliKoodiPattern, _)),
    validateIfNonEmpty[String](aidinkielet, s"$path.aidinkielet", assertMatch(_, KieliKoodiPattern, _)),
    validateIfNonEmpty[String](muutKielet, s"$path.muutKielet", assertMatch(_, KieliKoodiPattern, _)))
}

case class LukiolinjaTieto(koodiUri: String, kuvaus: Kielistetty) {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], koodiUriPattern: Pattern, path: String): IsValid = and(
    validateIfJulkaistu(tila,
      validateOptionalKielistetty(kielivalinta, kuvaus, s"$path.kuvaus")
    ),
    assertMatch(koodiUri, koodiUriPattern, s"$path.koodiUri")
  )
}
