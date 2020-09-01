package fi.oph.kouta.domain

import java.time.LocalDateTime

import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, ValidatableSubEntity}

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

  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfDefined[Opetus](opetus, _.validate(tila, kielivalinta, s"$path.opetus")),
    validateIfNonEmpty[Yhteyshenkilo](yhteyshenkilot, s"$path.yhteyshenkilot", _.validate(tila, kielivalinta, _)),
    validateIfJulkaistu(tila, and(
      validateOptionalKielistetty(kielivalinta, kuvaus, s"$path.kuvaus"),
      assertNotOptional(opetus, s"$path.opetus"),
    ))
  )

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
  def osaamisalat: List[AmmatillinenOsaamisala]
  def hakutermi: Hakutermi
  def hakulomaketyyppi: Hakulomaketyyppi
  def hakulomakeLinkki: Kielistetty
  def lisatietoaHakeutumisesta: Kielistetty
  def lisatietoaValintaperusteista: Kielistetty
  def hakuaika: Option[Ajanjakso]

  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    validateIfNonEmpty(hakulomakeLinkki, s"$path/hakulomakeLinkki", assertValidUrl _),
    validateIfDefined[Ajanjakso](hakuaika, _.validate(tila, kielivalinta, path)),
    validateIfJulkaistu(tila, and(
      validateIfTrue(hakulomaketyyppi == MuuHakulomake, and(
        validateKielistetty(kielivalinta, hakulomakeLinkki, "$path/hakulomakeLinkki"),
        assertNotOptional(hakuaika, s"$path/hakuaika"),
        validateOptionalKielistetty(kielivalinta, lisatietoaHakeutumisesta, "$path/lisatietoaHakeutumisesta")
      )),
      validateIfTrue(hakulomaketyyppi == EiSähköistä, validateKielistetty(kielivalinta, lisatietoaHakeutumisesta, "$path/lisatietoaHakeutumisesta")),
      validateOptionalKielistetty(kielivalinta, lisatietoaValintaperusteista, s"$path/lisatietoaValintaperusteista"),
    ))
  )
}

case class TutkinnonOsaToteutusMetadata(tyyppi: Koulutustyyppi = AmmTutkinnonOsa,
                                        kuvaus: Kielistetty = Map(),
                                        osaamisalat: List[AmmatillinenOsaamisala] = List(),
                                        opetus: Option[Opetus] = None,
                                        asiasanat: List[Keyword] = List(),
                                        ammattinimikkeet: List[Keyword] = List(),
                                        yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                        hakutermi: Hakutermi,
                                        hakulomaketyyppi: Hakulomaketyyppi,
                                        hakulomakeLinkki: Kielistetty = Map(),
                                        lisatietoaHakeutumisesta: Kielistetty = Map(),
                                        lisatietoaValintaperusteista: Kielistetty = Map(),
                                        hakuaika: Option[Ajanjakso] = None) extends TutkintoonJohtamatonToteutusMetadata

case class OsaamisalaToteutusMetadata(tyyppi: Koulutustyyppi = AmmTutkinnonOsa,
                                      kuvaus: Kielistetty = Map(),
                                      osaamisalat: List[AmmatillinenOsaamisala] = List(),
                                      opetus: Option[Opetus] = None,
                                      asiasanat: List[Keyword] = List(),
                                      ammattinimikkeet: List[Keyword] = List(),
                                      yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                      hakutermi: Hakutermi,
                                      hakulomaketyyppi: Hakulomaketyyppi,
                                      hakulomakeLinkki: Kielistetty = Map(),
                                      lisatietoaHakeutumisesta: Kielistetty = Map(),
                                      lisatietoaValintaperusteista: Kielistetty = Map(),
                                      hakuaika: Option[Ajanjakso] = None) extends TutkintoonJohtamatonToteutusMetadata

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
                  stipendinKuvaus: Kielistetty = Map(),
                  suunniteltuKestoVuodet: Option[Int] = None,
                  suunniteltuKestoKuukaudet: Option [Int] = None,
                  suunniteltuKestoKuvaus: Kielistetty = Map()) extends ValidatableSubEntity {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfNonEmpty[String](opetuskieliKoodiUrit, s"$path.opetuskieliKoodiUrit", assertMatch(_, OpetuskieliKoodiPattern, _)),
    validateIfNonEmpty[String](opetusaikaKoodiUrit, s"$path.opetusaikaKoodiUrit", assertMatch(_, OpetusaikaKoodiPattern, _)),
    validateIfNonEmpty[String](opetustapaKoodiUrit, s"$path.opetustapaKoodiUrit", assertMatch(_, OpetustapaKoodiPattern, _)),
    validateIfDefined[String](koulutuksenAlkamiskausi, assertMatch(_, KausiKoodiPattern, s"$path.koulutuksenAlkamiskausi")),
    validateIfDefined[Int](koulutuksenAlkamisvuosi, v => assertMatch(v.toString, VuosiPattern, s"$path.koulutuksenAlkamisvuosi")),
    validateKoulutusPaivamaarat(koulutuksenAlkamispaivamaara, koulutuksenPaattymispaivamaara, s"$path.koulutuksenAlkamispaivamaara"),
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
      assertNotOptional(onkoStipendia, s"$path.onkoStipendia"),
      validateIfTrue(onkoStipendia.contains(true), assertNotOptional(stipendinMaara, s"$path.stipendinMaara")),
      validateOptionalKielistetty(kielivalinta, stipendinKuvaus, s"$path.stipendinKuvaus"),
      validateOptionalKielistetty(kielivalinta, suunniteltuKestoKuvaus, s"$path.suunniteltuKestoKuvaus"),
      if (koulutuksenTarkkaAlkamisaika) {
        assertNotOptional(koulutuksenAlkamispaivamaara, s"$path.koulutuksenAlkamispaivamaara")
      } else and(
        assertNotOptional(koulutuksenAlkamiskausi, s"$path.koulutuksenAlkamiskausi"),
        assertNotOptional(koulutuksenAlkamisvuosi, s"$path.koulutuksenAlkamisvuosi")
      )
    ))
  )

  override def validateOnJulkaisu(path: String): IsValid = and(
    validateIfDefined[LocalDateTime](koulutuksenAlkamispaivamaara, assertInFuture(_, s"$path.koulutuksenAlkamispaivamaara")),
    validateIfDefined[LocalDateTime](koulutuksenPaattymispaivamaara, assertInFuture(_, s"$path.koulutuksenPaattymispaivamaara")),
  )
}
