package fi.oph.kouta.domain

import java.time.LocalDateTime

import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.validation.{IsValid, ValidatableSubEntity}
import fi.oph.kouta.validation.Validations._

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
      |          allOf:
      |            - $ref: '#/components/schemas/Kuvaus'
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
      |          allOf:
      |            - $ref: '#/components/schemas/Kuvaus'
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
      |          allOf:
      |            - $ref: '#/components/schemas/Kuvaus'
      |        onkoMaksullinen:
      |          type: boolean
      |          decription: "Onko koulutus maksullinen?"
      |        maksullisuusKuvaus:
      |          type: object
      |          description: Koulutuksen toteutuksen maksullisuutta tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Kuvaus'
      |        maksunMaara:
      |          type: double
      |          description: "Koulutuksen toteutuksen maksun määrä euroissa?"
      |          example: 220.50
      |        koulutuksenTarkkaAlkamisaika:
      |          type: string
      |          description: Jos alkamisaika on tiedossa niin alkamis- ja päättymispäivämäärä on pakollinen. Muussa tapauksessa kausi ja vuosi on pakollisia tietoja.
      |          example: true
      |        koulutuksenAlkamispaivamaara:
      |          type: string
      |          description: Koulutuksen alkamisen päivämäärä
      |          example: 2019-11-20T12:00
      |        koulutuksenPaattymispaivamaara:
      |          type: string
      |          description: Koulutuksen päättymisen päivämäärä
      |          example: 2019-12-20T12:00
      |        koulutuksenAlkamiskausi:
      |          type: string
      |          description: Koulutuksen alkamiskausi (koodistoarvo)
      |          example: kausi_k#1
      |        koulutuksenAlkamisvuosi:
      |          type: string
      |          description: Koulutuksen alkamisvuosi
      |          example: 2020
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
      |          allOf:
      |            - $ref: '#/components/schemas/Kuvaus'
      |""".stripMargin

  val ToteutusMetadata =
    """    ToteutusMetadata:
      |      type: object
      |      properties:
      |        kuvaus:
      |          type: object
      |          description: Toteutuksen kuvausteksti eri kielillä. Kielet on määritetty toteutuksen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Kuvaus'
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
      |          allOf:
      |            - $ref: '#/components/schemas/Nimi'
      |        kuvaus:
      |          type: object
      |          description: Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. kuvaus
      |          allOf:
      |            - $ref: '#/components/schemas/Kuvaus'
      |        linkki:
      |          type: object
      |          description: Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. linkki
      |          allOf:
      |            - $ref: '#/components/schemas/Linkki'
      |        otsikko:
      |          type: object
      |          description: Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. linkin otsikko
      |          allOf:
      |            - $ref: '#/components/schemas/Teksti'
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
      |          allOf:
      |            - $ref: '#/components/schemas/Linkki'
      |        otsikko:
      |          type: object
      |          description: Osaamisalan linkin otsikko
      |          allOf:
      |            - $ref: '#/components/schemas/Teksti'
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
      |            koulutustyyppi:
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
      |            koulutustyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: amk
      |              enum:
      |                - amk
      |""".stripMargin

  val AmmatillinenToteutusMetadata =
    """    AmmatillinenToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            osaamisalat:
      |              type: array
      |              items:
      |                $ref: '#/components/schemas/Osaamisala'
      |              description: Lista ammatillisen koulutuksen osaamisalojen kuvauksia
      |            koulutustyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: amm
      |              enum:
      |                - amm
      |""".stripMargin

  val models = List(Opetus, ToteutusMetadata, KorkeakouluOsaamisala, Osaamisala, KorkeakouluToteutusMetadata, AmmattikorkeaToteutusMetadata, YliopistoToteutusMetadata, AmmatillinenToteutusMetadata)
}

sealed trait ToteutusMetadata extends ValidatableSubEntity {
  val tyyppi: Koulutustyyppi
  val kuvaus: Kielistetty
  val opetus: Option[Opetus]
  val asiasanat: List[Keyword]
  val ammattinimikkeet: List[Keyword]
  val yhteyshenkilot: Seq[Yhteyshenkilo]

  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli]): IsValid = and(
    validateIfDefined[Opetus](opetus, _.validate(tila, kielivalinta)),
    validateIfNonEmpty[Yhteyshenkilo](yhteyshenkilot, _.validate(tila, kielivalinta)),
    validateIfJulkaistu(tila, and(
      validateOptionalKielistetty(kielivalinta, kuvaus, "kuvaus"),
      assertNotOptional(opetus, "opetus"),
    ))
  )
}

trait KorkeakoulutusToteutusMetadata extends ToteutusMetadata {
  val alemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala]
  val ylemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala]

  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli]): IsValid = and(
    super.validate(tila, kielivalinta),
    validateIfNonEmpty[KorkeakouluOsaamisala](alemmanKorkeakoulututkinnonOsaamisalat, _.validate(tila, kielivalinta)),
    validateIfNonEmpty[KorkeakouluOsaamisala](ylemmanKorkeakoulututkinnonOsaamisalat, _.validate(tila, kielivalinta))
  )
}

case class AmmatillinenToteutusMetadata(tyyppi: Koulutustyyppi = Amm,
                                        kuvaus: Kielistetty = Map(),
                                        osaamisalat: List[AmmatillinenOsaamisala] = List(),
                                        opetus: Option[Opetus] = None,
                                        asiasanat: List[Keyword] = List(),
                                        ammattinimikkeet: List[Keyword] = List(),
                                        yhteyshenkilot: Seq[Yhteyshenkilo] = Seq()) extends ToteutusMetadata {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli]): IsValid = and(
    super.validate(tila, kielivalinta),
    validateIfNonEmpty[AmmatillinenOsaamisala](osaamisalat, _.validate(tila, kielivalinta)),
    validateIfJulkaistu(tila, assertNotEmpty(osaamisalat, "osaamisalat"))
  )
}

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

  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli]): IsValid = and(
    validateIfNonEmpty(linkki.values.toSeq, assertValidUrl),
    validateIfJulkaistu(tila, and(
      validateOptionalKielistetty(kielivalinta, linkki, "linkki"),
      validateOptionalKielistetty(kielivalinta, otsikko, "otsikko"),
    ))
  )
}

case class AmmatillinenOsaamisala(koodiUri: String,
                                  linkki: Kielistetty = Map(),
                                  otsikko: Kielistetty = Map()) extends Osaamisala {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli]): IsValid = and(
    super.validate(tila, kielivalinta),
    assertMatch(koodiUri, OsaamisalaKoodiPattern)
  )
}

case class KorkeakouluOsaamisala(nimi: Kielistetty = Map(),
                                 kuvaus: Kielistetty = Map(),
                                 linkki: Kielistetty = Map(),
                                 otsikko: Kielistetty = Map()) extends Osaamisala {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli]): IsValid = and(
    super.validate(tila, kielivalinta),
    validateIfJulkaistu(tila, and(
      validateKielistetty(kielivalinta, nimi, "nimi"),
      validateOptionalKielistetty(kielivalinta, kuvaus, "kuvaus")
    ))
  )
}

case class Opetus(opetuskieliKoodiUrit: Seq[String] = Seq(),
                  opetuskieletKuvaus: Kielistetty = Map(),
                  opetusaikaKoodiUrit: Seq[String] = Seq(),
                  opetusaikaKuvaus: Kielistetty = Map(),
                  opetustapaKoodiUrit: Seq[String] = Seq(),
                  opetustapaKuvaus: Kielistetty = Map(),
                  onkoMaksullinen: Option[Boolean] = Some(false),
                  maksullisuusKuvaus: Kielistetty = Map(),
                  maksunMaara: Option[Double] = None,
                  koulutuksenTarkkaAlkamisaika: Boolean = false,
                  koulutuksenAlkamispaivamaara: Option[LocalDateTime] = None,
                  koulutuksenPaattymispaivamaara: Option[LocalDateTime] = None,
                  koulutuksenAlkamiskausi: Option[String] = None,
                  koulutuksenAlkamisvuosi: Option[Int] = None,
                  lisatiedot: Seq[Lisatieto] = Seq(),
                  onkoStipendia: Option[Boolean] = Some(false),
                  stipendinMaara: Option[Double] = None,
                  stipendinKuvaus: Kielistetty = Map()) extends ValidatableSubEntity {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli]): IsValid = and(
    validateIfNonEmpty[String](opetuskieliKoodiUrit, k => assertMatch(k, OpetuskieliKoodiPattern)),
    validateIfNonEmpty[String](opetusaikaKoodiUrit, k => assertMatch(k, OpetusaikaKoodiPattern)),
    validateIfNonEmpty[String](opetustapaKoodiUrit, k => assertMatch(k, OpetustapaKoodiPattern)),
    validateIfDefined[String](koulutuksenAlkamiskausi, k => assertMatch(k, KausiKoodiPattern)),
    validateIfDefined[Int](koulutuksenAlkamisvuosi, k => assertMatch(k.toString, VuosiPattern)),
    validateKoulutusPaivamaarat(koulutuksenAlkamispaivamaara, koulutuksenPaattymispaivamaara),
    validateIfNonEmpty[Lisatieto](lisatiedot, _.validate(tila, kielivalinta)),
    validateIfDefined[Double](stipendinMaara, assertNotNegative(_, "stipendinMaara")),
    validateIfDefined[Double](maksunMaara, assertNotNegative(_, "maksunMaara")),
    validateIfJulkaistu(tila, and(
      assertNotEmpty(opetuskieliKoodiUrit, "opetuskieliKoodiUrit"),
      validateOptionalKielistetty(kielivalinta, opetuskieletKuvaus, "opetuskieletKuvaus"),
      assertNotEmpty(opetusaikaKoodiUrit, "opetusaikaKoodiUrit"),
      validateOptionalKielistetty(kielivalinta, opetusaikaKuvaus, "opetusaikaKuvaus"),
      assertNotEmpty(opetustapaKoodiUrit, "opetustapaKoodiUrit"),
      validateOptionalKielistetty(kielivalinta, opetuskieletKuvaus, "opetuskieletKuvaus"),
      validateOptionalKielistetty(kielivalinta, opetusaikaKuvaus, "opetusaikaKuvaus"),
      validateOptionalKielistetty(kielivalinta, opetustapaKuvaus, "opetustapaKuvaus"),
      assertNotOptional(onkoMaksullinen, "onkoMaksullinen"),
      validateOptionalKielistetty(kielivalinta, maksullisuusKuvaus, "maksullisuusKuvaus"),
      validateIfTrue(onkoMaksullinen.contains(true), assertNotOptional(maksunMaara, "maksunMaara")),
      validateIfDefined[LocalDateTime](koulutuksenAlkamispaivamaara, assertInFuture(_, "koulutuksenAlkamispaivamaara")),
      validateIfDefined[LocalDateTime](koulutuksenPaattymispaivamaara, assertInFuture(_, "koulutuksenPaattymispaivamaara")),
      assertNotOptional(onkoStipendia, "onkoStipendia"),
      validateIfTrue(onkoStipendia.contains(true), assertNotOptional(stipendinMaara, "stipendinMaara")),
      validateOptionalKielistetty(kielivalinta, stipendinKuvaus, "stipendinKuvaus"),
      if (koulutuksenTarkkaAlkamisaika) {
        assertNotOptional(koulutuksenAlkamispaivamaara, "koulutuksenAlkamispaivamaara")
      } else and(
        assertNotOptional(koulutuksenAlkamiskausi, "koulutuksenAlkamiskausi"),
        assertNotOptional(koulutuksenAlkamisvuosi, "koulutuksenAlkamisvuosi")
      )
    ))
  )
}
