package fi.oph.kouta.domain

import fi.oph.kouta.domain.keyword.Keyword

package object toteutusMetadata {

  val Opetus =
    s"""    Opetus:
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
       |            - $$ref: '#/components/schemas/Kuvaus'
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
       |            - $$ref: '#/components/schemas/Kuvaus'
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
       |            - $$ref: '#/components/schemas/Kuvaus'
       |        onkoMaksullinen:
       |          type: boolean
       |          decription: "Onko koulutus maksullinen?"
       |        maksullisuusKuvaus:
       |          type: object
       |          description: Koulutuksen toteutuksen maksullisuutta tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Kuvaus'
       |        maksunMaara:
       |          type: double
       |          description: "Koulutuksen toteutuksen maksun määrä euroissa?"
       |          example: 220.50
       |        alkamiskausiKoodiUri:
       |          type: string
       |          description: Koulutuksen toteutuksen alkamiskausi. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kausi/1)
       |          example: kausi_k#1
       |        alkamisvuosi:
       |          type: string
       |          description: Koulutuksen toteutuksen alkamisvuosi
       |          example: 2020
       |        alkamisaikaKuvaus:
       |          type: object
       |          description: Koulutuksen toteutuksen alkamisaikoja tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Kuvaus'
       |        lisatiedot:
       |          type: array
       |          description: Koulutuksen toteutukseen liittyviä lisätietoja, jotka näkyvät oppijalle Opintopolussa
       |          items:
       |            type: object
       |            $$ref: '#/components/schemas/Lisatieto'
       |        onkoLukuvuosimaksua:
       |          type: boolean
       |          description: "Onko koulutuksella lukuvuosimaksua?"
       |        lukuvuosimaksu:
       |          type: object
       |          description: Koulutuksen toteutuksen lukuvuosimaksu eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Teksti'
       |        lukuvuosimaksuKuvaus:
       |          type: object
       |          description: Koulutuksen toteutuksen lukuvuosimaksua tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Kuvaus'
       |        onkoStipendia:
       |          type: boolean
       |          description: "Onko koulutukseen stipendiä?"
       |        stipendinMaara:
       |          type: object
       |          description: Koulutuksen toteutuksen stipendin määrä eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Teksti'
       |        stipendinKuvaus:
       |          type: object
       |          description: Koulutuksen toteutuksen stipendiä tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Kuvaus'
       |""".stripMargin

  val ToteutusMetadata =
    s"""    ToteutusMetadata:
       |      type: object
       |      properties:
       |        kuvaus:
       |          type: object
       |          description: Toteutuksen kuvausteksti eri kielillä. Kielet on määritetty toteutuksen kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Kuvaus'
       |        opetus:
       |          type: object
       |          $$ref: '#/components/schemas/Opetus'
       |        yhteyshenkilo:
       |          type: object
       |          description: Toteutuksen yhteyshenkilön tiedot
       |          allOf:
       |            - $$ref: '#/components/schemas/Yhteyshenkilo'
       |        asiasanat:
       |          type: array
       |          description: Lista toteutukseen liittyvistä asiasanoista, joiden avulla opiskelija voi hakea koulutusta Opintopolusta
       |          items:
       |            $$ref: '#/components/schemas/Asiasana'
       |        ammattinimikkeet:
       |          type: array
       |          description: Lista toteutukseen liittyvistä ammattinimikkeistä, joiden avulla opiskelija voi hakea koulutusta Opintopolusta
       |          items:
       |            $$ref: '#/components/schemas/Ammattinimike'
       |""".stripMargin

  val KorkeakouluOsaamisala =
    s"""    KorkeakouluOsaamisala:
       |      type: object
       |      properties:
       |        nimi:
       |          type: object
       |          description: Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. nimi
       |          allOf:
       |            - $$ref: '#/components/schemas/Nimi'
       |        kuvaus:
       |          type: object
       |          description: Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. kuvaus
       |          allOf:
       |            - $$ref: '#/components/schemas/Kuvaus'
       |        linkki:
       |          type: object
       |          description: Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. linkki
       |          allOf:
       |            - $$ref: '#/components/schemas/Linkki'
       |        otsikko:
       |          type: object
       |          description: Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. linkin otsikko
       |          allOf:
       |            - $$ref: '#/components/schemas/Teksti'
       |""".stripMargin

  val Osaamisala =
    s"""    Osaamisala:
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
       |            - $$ref: '#/components/schemas/Linkki'
       |        otsikko:
       |          type: object
       |          description: Osaamisalan linkin otsikko
       |          allOf:
       |            - $$ref: '#/components/schemas/Teksti'
       |""".stripMargin

  val KorkeakouluToteutusMetadata =
    s"""    KorkeakouluToteutusMetadata:
       |      allOf:
       |        - $$ref: '#/components/schemas/ToteutusMetadata'
       |      properties:
       |        alemmanKorkeakoulututkinnonOsaamisalat:
       |          type: array
       |          description: Lista alemman korkeakoulututkinnon erikoistumisalojen, opintosuuntien, pääaineiden tms. kuvauksista.
       |          items:
       |            $$ref: '#/components/schemas/KorkeakouluOsaamisala'
       |        ylemmanKorkeakoulututkinnonOsaamisalat:
       |          type: array
       |          items:
       |            $$ref: '#/components/schemas/KorkeakouluOsaamisala'
       |          description: Lista ylemmän korkeakoulututkinnon erikoistumisalojen, opintosuuntien, pääaineiden tms. kuvauksista.
       |""".stripMargin

  val YliopistoToteutusMetadata =
    s"""    YliopistoToteutusMetadata:
       |      allOf:
       |        - $$ref: '#/components/schemas/KorkeakouluToteutusMetadata'
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
    s"""    AmmattikorkeaToteutusMetadata:
       |      allOf:
       |        - $$ref: '#/components/schemas/KorkeakouluToteutusMetadata'
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
    s"""    AmmatillinenToteutusMetadata:
       |      allOf:
       |        - $$ref: '#/components/schemas/KoulutusMetadata'
       |        - type: object
       |          properties:
       |            osaamisalat:
       |              type: array
       |              items:
       |                $$ref: '#/components/schemas/Osaamisala'
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

sealed trait ToteutusMetadata {
  val tyyppi: Koulutustyyppi
  val kuvaus: Kielistetty
  val opetus: Option[Opetus]
  val asiasanat: List[Keyword]
  val ammattinimikkeet: List[Keyword]
  val yhteyshenkilo: Option[Yhteyshenkilo]
}

trait KorkeakoulutusToteutusMetadata extends ToteutusMetadata {
  val alemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala]
  val ylemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala]
}

case class AmmatillinenToteutusMetadata(tyyppi: Koulutustyyppi = Amm,
                                        kuvaus: Kielistetty = Map(),
                                        osaamisalat: List[AmmatillinenOsaamisala] = List(),
                                        opetus: Option[Opetus] = None,
                                        asiasanat: List[Keyword] = List(),
                                        ammattinimikkeet: List[Keyword] = List(),
                                        yhteyshenkilo: Option[Yhteyshenkilo] = None) extends ToteutusMetadata

case class YliopistoToteutusMetadata(tyyppi: Koulutustyyppi = Yo,
                                     kuvaus: Kielistetty = Map(),
                                     opetus: Option[Opetus] = None,
                                     asiasanat: List[Keyword] = List(),
                                     ammattinimikkeet: List[Keyword] = List(),
                                     yhteyshenkilo: Option[Yhteyshenkilo] = None,
                                     alemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala] = Seq(),
                                     ylemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala] = Seq()) extends KorkeakoulutusToteutusMetadata

case class AmmattikorkeakouluToteutusMetadata(tyyppi: Koulutustyyppi = Amk,
                                              kuvaus: Kielistetty = Map(),
                                              opetus: Option[Opetus] = None,
                                              asiasanat: List[Keyword] = List(),
                                              ammattinimikkeet: List[Keyword] = List(),
                                              yhteyshenkilo: Option[Yhteyshenkilo] = None,
                                              alemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala] = Seq(),
                                              ylemmanKorkeakoulututkinnonOsaamisalat: Seq[KorkeakouluOsaamisala] = Seq()) extends KorkeakoulutusToteutusMetadata

trait Osaamisala {
  val linkki: Kielistetty
  val otsikko: Kielistetty
}

case class AmmatillinenOsaamisala(koodiUri: String,
                                  linkki: Kielistetty = Map(),
                                  otsikko: Kielistetty = Map()) extends Osaamisala

case class KorkeakouluOsaamisala(nimi: Kielistetty = Map(),
                                 kuvaus: Kielistetty = Map(),
                                 linkki: Kielistetty = Map(),
                                 otsikko: Kielistetty = Map()) extends Osaamisala

case class Opetus(opetuskieliKoodiUrit: Seq[String],
                  opetuskieletKuvaus: Kielistetty = Map(),
                  opetusaikaKoodiUrit: Seq[String] = Seq(),
                  opetusaikaKuvaus: Kielistetty = Map(),
                  opetustapaKoodiUrit: Seq[String] = Seq(),
                  opetustapaKuvaus: Kielistetty = Map(),
                  onkoMaksullinen: Option[Boolean] = Some(false),
                  maksullisuusKuvaus: Kielistetty = Map(),
                  maksunMaara: Option[Double] = None,
                  alkamiskausiKoodiUri: Option[String] = None,
                  alkamisvuosi: Option[String] = None,
                  alkamisaikaKuvaus: Kielistetty = Map(),
                  lisatiedot: Seq[Lisatieto] = Seq(),
                  onkoLukuvuosimaksua: Option[Boolean] = Some(false),
                  lukuvuosimaksu: Kielistetty = Map(),
                  lukuvuosimaksuKuvaus: Kielistetty = Map(),
                  onkoStipendia: Option[Boolean] = Some(false),
                  stipendinMaara: Kielistetty = Map(),
                  stipendinKuvaus: Kielistetty = Map())
