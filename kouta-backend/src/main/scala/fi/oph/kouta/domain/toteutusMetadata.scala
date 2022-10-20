package fi.oph.kouta.domain

import fi.oph.kouta.domain.keyword.Keyword
import fi.oph.kouta.domain.oid.ToteutusOid
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, NoErrors, ValidatableSubEntity}

package object toteutusMetadata {

  val OpetusModel: String =
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

  val ApurahaModel: String =
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

  val ToteutusMetadataModel: String =
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

  val KorkeakouluOsaamisalaModel: String =
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

  val OsaamisalaModel: String =
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

  val YliopistoToteutusMetadataModel: String =
    """    YliopistoToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/ToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: yo
      |              enum:
      |                - yo
      |""".stripMargin

  val AmmattikorkeaToteutusMetadataModel: String =
    """    AmmattikorkeaToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/ToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: amk
      |              enum:
      |                - amk
      |""".stripMargin

  val AmmOpeErityisopeJaOpoToteutusMetadataModel: String =
    """    AmmOpeErityisopeJaOpoToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/ToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: amm-ope-erityisope-ja-opo
      |              enum:
      |                - amm-ope-erityisope-ja-opo
      |""".stripMargin

  val OpePedagOpinnotToteutusMetadataModel: String =
    """    OpePedagOpinnotToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/ToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: ope-pedag-opinnot
      |              enum:
      |                - ope-pedag-opinnot
      |""".stripMargin

  val KkOpintojaksoToteutusMetadataModel: String =
    """    KkOpintojaksoToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/TutkintoonJohtamatonToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: kk-opintojakso
      |              enum:
      |                - kk-opintojakso
      |""".stripMargin

  val KkOpintokokonaisuusToteutusMetadataModel: String =
    """    KkOpintokokonaisuusToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/TutkintoonJohtamatonToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: kk-opintokokonaisuus
      |              enum:
      |                - kk-opintokokonaisuus
      |            opintojenLaajuusyksikkoKoodiUri:
      |              type: string
      |              description: "Opintojen laajuusyksikko. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuusyksikko/1)"
      |              example: opintojenlaajuusyksikko_6#1
      |            opintojenLaajuusNumero:
      |              type: integer
      |              description: Opintojen laajuus tai kesto numeroarvona
      |              example: 10
      |""".stripMargin

  val ErikoislaakariToteutusMetadataModel: String =
    """    ErikoislaakariToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/ToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: erikoislaakari
      |              enum:
      |                - erikoislaakari
      |""".stripMargin

  val AmmatillinenToteutusMetadataModel: String =
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

  val TutkintoonJohtamatonToteutusMetadataModel: String =
    """    TutkintoonJohtamatonToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/ToteutusMetadata'
      |        - type: object
      |          properties:
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

  val AmmatillinenTutkinnonOsaToteutusMetadataModel: String =
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

  val AmmatillinenOsaamisalaToteutusMetadataModel: String =
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

  val AmmatillinenMuuToteutusMetadataModel: String =
    """    AmmatillinenMuuToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/TutkintoonJohtamatonToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Toteutuksen metatiedon tyyppi
      |              example: amm-muu
      |              enum:
      |                - amm-muu
      |""".stripMargin

  val TuvaToteutusMetadataModel: String =
    """    TuvaToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/ToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Toteutuksen metatiedon tyyppi
      |              example: tuva
      |              enum:
      |                - tuva
      |            jarjestetaanErityisopetuksena:
      |              type: boolean
      |              description: Tieto siitä järjestetäänkö toteutus erityisopetuksena
      |""".stripMargin

  val TelmaToteutusMetadataModel: String =
    """    TelmaToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/ToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Toteutuksen metatiedon tyyppi
      |              example: telma
      |              enum:
      |                - telma
      |""".stripMargin

  val VapaaSivistystyoOpistovuosiToteutusMetadataModel: String =
    """    VapaaSivistystyoOpistovuosiToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/ToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Toteutuksen metatiedon tyyppi
      |              example: vapaa-sivistystyo-opistovuosi
      |              enum:
      |                - vapaa-sivistystyo-opistovuosi
      |""".stripMargin

  val VapaaSivistystyoMuuToteutusMetadataModel: String =
    """    VapaaSivistystyoMuuToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/TutkintoonJohtamatonToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Toteutuksen metatiedon tyyppi
      |              example: vapaa-sivistystyo-muu
      |              enum:
      |                - vapaa-sivistystyo-muu
      |""".stripMargin

  val AikuistenPerusopetusToteutusMetadataModel: String =
    """    AikuistenPerusopetusToteutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/TutkintoonJohtamatonToteutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Toteutuksen metatiedon tyyppi
      |              example: aikuisten-perusopetus
      |              enum:
      |                - aikuisten-perusopetus
      |""".stripMargin

  val LukiolinjaTietoModel: String =
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

  val LukiodiplomiTietoModel: String =
    """    LukiodiplomiTieto:
      |      type: object
      |      description: Toteutuksen yksittäisen lukiodiplomitiedon kentät
      |      properties:
      |        koodiUri:
      |          type: string
      |          description: Lukiodiplomin koodiUri. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/moduulikoodistolops2021/1).
      |        linkki:
      |          type: object
      |          description: Lukiodiplomin kielistetyt lisätietolinkit. Kielet on määritetty toteutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Linkki'
      |        linkinAltTeksti:
      |          type: object
      |          description: Lukiodiplomin kielistettyjen lisätietolinkkien alt-tekstit. Kielet on määritetty toteutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Teksti'
      |""".stripMargin

  val LukioToteutusMetadataModel: String =
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
      |            kielivalikoima:
      |              type: object
      |              description: Koulutuksen kielivalikoima
      |              $ref: '#/components/schemas/Kielivalikoima'
      |            yleislinja:
      |              type: boolean,
      |              description: Onko lukio-toteutuksella yleislinja?
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
      |            diplomit:
      |              type: array
      |              description: Lukio-toteutuksen diplomit
      |              items:
      |                type: object
      |                $ref: '#/components/schemas/LukiodiplomiTieto'
      |""".stripMargin

  val KielivalikoimaModel: String =
    """    Kielivalikoima:
      |      type: object
      |      properties:
      |        A1Kielet:
      |          type: array
      |          description: Lista koulutuksen toteutuksen A1 kielistä. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kieli/1)
      |          items:
      |            type: string
      |            example:
      |              - kieli_EN#1
      |              - kieli_FI#1
      |        A2Kielet:
      |          type: array
      |          description: Lista koulutuksen toteutuksen A2 kielistä. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kieli/1)
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

  val models = List(OpetusModel, ApurahaModel, KielivalikoimaModel, ToteutusMetadataModel, KorkeakouluOsaamisalaModel, OsaamisalaModel,
    AmmattikorkeaToteutusMetadataModel, AmmOpeErityisopeJaOpoToteutusMetadataModel, OpePedagOpinnotToteutusMetadataModel, KkOpintojaksoToteutusMetadataModel, YliopistoToteutusMetadataModel, AmmatillinenToteutusMetadataModel, TutkintoonJohtamatonToteutusMetadataModel,
    AmmatillinenTutkinnonOsaToteutusMetadataModel, AmmatillinenOsaamisalaToteutusMetadataModel, AmmatillinenMuuToteutusMetadataModel, TuvaToteutusMetadataModel, LukiolinjaTietoModel, LukioToteutusMetadataModel,
    LukiodiplomiTietoModel, VapaaSivistystyoOpistovuosiToteutusMetadataModel, VapaaSivistystyoMuuToteutusMetadataModel, TelmaToteutusMetadataModel, AikuistenPerusopetusToteutusMetadataModel, ErikoislaakariToteutusMetadataModel, KkOpintokokonaisuusToteutusMetadataModel)
}

sealed trait ToteutusMetadata {
  val tyyppi: Koulutustyyppi
  val kuvaus: Kielistetty
  val opetus: Option[Opetus]
  val asiasanat: List[Keyword]
  val ammattinimikkeet: List[Keyword]
  val yhteyshenkilot: Seq[Yhteyshenkilo]
  val isMuokkaajaOphVirkailija: Option[Boolean]
  val hasJotpaRahoitus: Option[Boolean]

  def allowSorakuvaus: Boolean = false
}

case class AmmatillinenToteutusMetadata(tyyppi: Koulutustyyppi = Amm,
                                        kuvaus: Kielistetty = Map(),
                                        osaamisalat: List[AmmatillinenOsaamisala] = List(),
                                        opetus: Option[Opetus] = None,
                                        asiasanat: List[Keyword] = List(),
                                        ammattinimikkeet: List[Keyword] = List(),
                                        yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                        ammatillinenPerustutkintoErityisopetuksena: Option[Boolean] = None,
                                        isMuokkaajaOphVirkailija: Option[Boolean] = None,
                                        hasJotpaRahoitus: Option[Boolean] = None) extends ToteutusMetadata

trait TutkintoonJohtamatonToteutusMetadata extends ToteutusMetadata {
  def hakutermi: Option[Hakutermi]
  def hakulomaketyyppi: Option[Hakulomaketyyppi]
  def hakulomakeLinkki: Kielistetty
  def lisatietoaHakeutumisesta: Kielistetty
  def lisatietoaValintaperusteista: Kielistetty
  def hakuaika: Option[Ajanjakso]
  def aloituspaikat: Option[Int]

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
                                                    aloituspaikat: Option[Int] = None,
                                                    isMuokkaajaOphVirkailija: Option[Boolean] = None,
                                                    hasJotpaRahoitus: Option[Boolean] = None) extends TutkintoonJohtamatonToteutusMetadata

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
                                                  aloituspaikat: Option[Int] = None,
                                                  isMuokkaajaOphVirkailija: Option[Boolean] = None,
                                                  hasJotpaRahoitus: Option[Boolean] = None) extends TutkintoonJohtamatonToteutusMetadata

case class AmmatillinenMuuToteutusMetadata(tyyppi: Koulutustyyppi = AmmMuu,
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
                                           aloituspaikat: Option[Int] = None,
                                           isMuokkaajaOphVirkailija: Option[Boolean] = None,
                                           hasJotpaRahoitus: Option[Boolean] = None
                                          ) extends TutkintoonJohtamatonToteutusMetadata {
  override def allowSorakuvaus: Boolean = false
}

case class YliopistoToteutusMetadata(tyyppi: Koulutustyyppi = Yo,
                                     kuvaus: Kielistetty = Map(),
                                     opetus: Option[Opetus] = None,
                                     asiasanat: List[Keyword] = List(),
                                     ammattinimikkeet: List[Keyword] = List(),
                                     yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                     isMuokkaajaOphVirkailija: Option[Boolean] = None,
                                     hasJotpaRahoitus: Option[Boolean] = None) extends ToteutusMetadata

case class AmmattikorkeakouluToteutusMetadata(tyyppi: Koulutustyyppi = Amk,
                                              kuvaus: Kielistetty = Map(),
                                              opetus: Option[Opetus] = None,
                                              asiasanat: List[Keyword] = List(),
                                              ammattinimikkeet: List[Keyword] = List(),
                                              yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                              isMuokkaajaOphVirkailija: Option[Boolean] = None,
                                              hasJotpaRahoitus: Option[Boolean] = None) extends ToteutusMetadata

case class AmmOpeErityisopeJaOpoToteutusMetadata(tyyppi: Koulutustyyppi = AmmOpeErityisopeJaOpo,
                                                 kuvaus: Kielistetty = Map(),
                                                 opetus: Option[Opetus] = None,
                                                 asiasanat: List[Keyword] = List(),
                                                 ammattinimikkeet: List[Keyword] = List(),
                                                 yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                                 aloituspaikat: Option[Int] = None,
                                                 isMuokkaajaOphVirkailija: Option[Boolean] = None,
                                                 hasJotpaRahoitus: Option[Boolean] = None) extends ToteutusMetadata

case class OpePedagOpinnotToteutusMetadata(tyyppi: Koulutustyyppi = AmmOpeErityisopeJaOpo,
                                           kuvaus: Kielistetty = Map(),
                                           opetus: Option[Opetus] = None,
                                           asiasanat: List[Keyword] = List(),
                                           ammattinimikkeet: List[Keyword] = List(),
                                           yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                           aloituspaikat: Option[Int] = None,
                                           isMuokkaajaOphVirkailija: Option[Boolean] = None,
                                           hasJotpaRahoitus: Option[Boolean] = None) extends ToteutusMetadata

case class KkOpintojaksoToteutusMetadata(tyyppi: Koulutustyyppi = KkOpintojakso,
                                         kuvaus: Kielistetty = Map(),
                                         opetus: Option[Opetus] = None,
                                         asiasanat: List[Keyword] = List(),
                                         ammattinimikkeet: List[Keyword] = List(),
                                         yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                         lisatietoaHakeutumisesta: Kielistetty = Map(),
                                         lisatietoaValintaperusteista: Kielistetty = Map(),
                                         hakutermi: Option[Hakutermi] = None,
                                         hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                                         hakulomakeLinkki: Kielistetty = Map(),
                                         hakuaika: Option[Ajanjakso] = None,
                                         aloituspaikat: Option[Int] = None,
                                         isMuokkaajaOphVirkailija: Option[Boolean] = None,
                                         hasJotpaRahoitus: Option[Boolean] = None,
                                         avoinKorkeakoulutus: Option[Boolean] = None,
                                         tunniste: Option[String] = None,
                                         opinnonTyyppiKoodiUri: Option[String] = None) extends TutkintoonJohtamatonToteutusMetadata

case class KkOpintokokonaisuusToteutusMetadata(tyyppi: Koulutustyyppi = KkOpintokokonaisuus,
                                               kuvaus: Kielistetty = Map(),
                                               opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
                                               opintojenLaajuusNumero: Option[Double] = None,
                                               opetus: Option[Opetus] = None,
                                               asiasanat: List[Keyword] = List(),
                                               ammattinimikkeet: List[Keyword] = List(),
                                               yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                               lisatietoaHakeutumisesta: Kielistetty = Map(),
                                               lisatietoaValintaperusteista: Kielistetty = Map(),
                                               hakutermi: Option[Hakutermi] = None,
                                               hakulomaketyyppi: Option[Hakulomaketyyppi] = None,
                                               hakulomakeLinkki: Kielistetty = Map(),
                                               hakuaika: Option[Ajanjakso] = None,
                                               aloituspaikat: Option[Int] = None,
                                               isMuokkaajaOphVirkailija: Option[Boolean] = None,
                                               hasJotpaRahoitus: Option[Boolean] = None,
                                               liitetytOpintojaksot: Seq[ToteutusOid] = Seq(),
                                               avoinKorkeakoulutus: Option[Boolean] = None,
                                               tunniste: Option[String] = None,
                                               opinnonTyyppiKoodiUri: Option[String] = None) extends TutkintoonJohtamatonToteutusMetadata

case class LukioToteutusMetadata(tyyppi: Koulutustyyppi = Lk,
                                 kuvaus: Kielistetty = Map(),
                                 opetus: Option[Opetus] = None,
                                 asiasanat: List[Keyword] = List(),
                                 ammattinimikkeet: List[Keyword] = List(),
                                 yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                 kielivalikoima: Option[Kielivalikoima] = None,
                                 yleislinja: Boolean = false,
                                 painotukset: Seq[LukiolinjaTieto] = Seq(),
                                 erityisetKoulutustehtavat: Seq[LukiolinjaTieto] = Seq(),
                                 diplomit: Seq[LukiodiplomiTieto] = Seq(),
                                 isMuokkaajaOphVirkailija: Option[Boolean] = None,
                                 hasJotpaRahoitus: Option[Boolean] = None
                                ) extends ToteutusMetadata

case class TuvaToteutusMetadata(tyyppi: Koulutustyyppi = Tuva,
                                kuvaus: Kielistetty = Map(),
                                opetus: Option[Opetus] = None,
                                asiasanat: List[Keyword] = List(),
                                ammattinimikkeet: List[Keyword] = List(),
                                yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                aloituspaikat: Option[Int] = None,
                                jarjestetaanErityisopetuksena: Boolean = false,
                                isMuokkaajaOphVirkailija: Option[Boolean] = None,
                                hasJotpaRahoitus: Option[Boolean] = None
                               ) extends ToteutusMetadata

case class TelmaToteutusMetadata(tyyppi: Koulutustyyppi = Telma,
                                 kuvaus: Kielistetty = Map(),
                                 opetus: Option[Opetus] = None,
                                 asiasanat: List[Keyword] = List(),
                                 ammattinimikkeet: List[Keyword] = List(),
                                 yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                 aloituspaikat: Option[Int] = None,
                                 isMuokkaajaOphVirkailija: Option[Boolean] = None,
                                 hasJotpaRahoitus: Option[Boolean] = None) extends ToteutusMetadata

case class AmmatillinenOsaamisala(koodiUri: String,
                                  linkki: Kielistetty = Map(),
                                  otsikko: Kielistetty = Map())

case class Apuraha(min: Option[Int] = None,
                   max: Option[Int] = None,
                   yksikko: Option[Apurahayksikko] = None,
                   kuvaus: Kielistetty = Map())

case class Opetus(opetuskieliKoodiUrit: Seq[String] = Seq(),
                  opetuskieletKuvaus: Kielistetty = Map(),
                  opetusaikaKoodiUrit: Seq[String] = Seq(),
                  opetusaikaKuvaus: Kielistetty = Map(),
                  opetustapaKoodiUrit: Seq[String] = Seq(),
                  opetustapaKuvaus: Kielistetty = Map(),
                  maksullisuustyyppi: Option[Maksullisuustyyppi] = None,
                  maksullisuusKuvaus: Kielistetty = Map(),
                  maksunMaara: Option[Double] = None,
                  koulutuksenAlkamiskausi: Option[KoulutuksenAlkamiskausi] = None,
                  lisatiedot: Seq[Lisatieto] = Seq(),
                  onkoApuraha: Boolean = false,
                  apuraha: Option[Apuraha] = None,
                  suunniteltuKestoVuodet: Option[Int] = None,
                  suunniteltuKestoKuukaudet: Option[Int] = None,
                  suunniteltuKestoKuvaus: Kielistetty = Map()) extends ValidatableSubEntity {
  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = NoErrors

  override def validateOnJulkaisu(path: String): IsValid = and(
    validateIfDefined[KoulutuksenAlkamiskausi](koulutuksenAlkamiskausi, _.validateOnJulkaisu(s"$path.koulutuksenAlkamiskausi"))
  )
}

case class Kielivalikoima(A1Kielet: Seq[String] = Seq(),
                          A2Kielet: Seq[String] = Seq(),
                          B1Kielet: Seq[String] = Seq(),
                          B2Kielet: Seq[String] = Seq(),
                          B3Kielet: Seq[String] = Seq(),
                          aidinkielet: Seq[String] = Seq(),
                          muutKielet: Seq[String] = Seq())

case class LukiolinjaTieto(koodiUri: String, kuvaus: Kielistetty)

case class LukiodiplomiTieto(koodiUri: String, linkki: Kielistetty = Map(), linkinAltTeksti: Kielistetty = Map())

case class VapaaSivistystyoOpistovuosiToteutusMetadata(tyyppi: Koulutustyyppi = VapaaSivistystyoOpistovuosi,
                                                       kuvaus: Kielistetty = Map(),
                                                       opetus: Option[Opetus] = None,
                                                       asiasanat: List[Keyword] = List(),
                                                       ammattinimikkeet: List[Keyword] = List(),
                                                       yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                                       isMuokkaajaOphVirkailija: Option[Boolean] = None,
                                                       hasJotpaRahoitus: Option[Boolean] = None) extends ToteutusMetadata

case class VapaaSivistystyoMuuToteutusMetadata(tyyppi: Koulutustyyppi = VapaaSivistystyoMuu,
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
                                               aloituspaikat: Option[Int] = None,
                                               isMuokkaajaOphVirkailija: Option[Boolean] = None,
                                               hasJotpaRahoitus: Option[Boolean] = None) extends TutkintoonJohtamatonToteutusMetadata {

  override def allowSorakuvaus: Boolean = false
}

case class AikuistenPerusopetusToteutusMetadata(tyyppi: Koulutustyyppi = AikuistenPerusopetus,
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
                                                aloituspaikat: Option[Int] = None,
                                                isMuokkaajaOphVirkailija: Option[Boolean] = None,
                                                hasJotpaRahoitus: Option[Boolean] = None
                                               ) extends TutkintoonJohtamatonToteutusMetadata

case class ErikoislaakariToteutusMetadata(tyyppi: Koulutustyyppi = Erikoislaakari,
                                          kuvaus: Kielistetty = Map(),
                                          opetus: Option[Opetus] = None,
                                          asiasanat: List[Keyword] = List(),
                                          ammattinimikkeet: List[Keyword] = List(),
                                          yhteyshenkilot: Seq[Yhteyshenkilo] = Seq(),
                                          isMuokkaajaOphVirkailija: Option[Boolean] = None,
                                          hasJotpaRahoitus: Option[Boolean] = None) extends ToteutusMetadata
