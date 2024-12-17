package fi.oph.kouta.domain

package object koulutusMetadata {

  val KoulutusMetadataModel: String =
    """    KoulutusMetadata:
      |      type: object
      |      properties:
      |        kuvaus:
      |          type: object
      |          description: Koulutuksen kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Kuvaus'
      |        lisatiedot:
      |          type: array
      |          description: Koulutukseen liittyviä lisätietoja, jotka näkyvät oppijalle Opintopolussa
      |          items:
      |            type: object
      |            $ref: '#/components/schemas/Lisatieto'
      |""".stripMargin

  val KorkeakouluMetadataModel: String =
    """    KorkeakouluMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |      properties:
      |        koulutusalaKoodiUrit:
      |          type: array
      |          description: Lista koulutusaloja. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kansallinenkoulutusluokitus2016koulutusalataso2/1)
      |          items:
      |            type: string
      |            example:
      |              - kansallinenkoulutusluokitus2016koulutusalataso2_054#1
      |              - kansallinenkoulutusluokitus2016koulutusalataso2_055#1
      |        tutkintonimikeKoodiUrit:
      |          type: array
      |          description: Lista koulutuksen tutkintonimikkeistä. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/tutkintonimikekk/2)
      |          items:
      |            type: string
      |          example:
      |            - tutkintonimikekk_110#2
      |            - tutkintonimikekk_111#2
      |        opintojenLaajuusyksikkoKoodiUri:
      |          type: string
      |          description: "Opintojen laajuusyksikko. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuusyksikko/1)"
      |          example: opintojenlaajuusyksikko_2#1
      |        opintojenLaajuusNumero:
      |          type: integer
      |          description: Opintojen laajuus tai kesto numeroarvona
      |          example: 10
      |""".stripMargin

  val YliopistoKoulutusMetadataModel: String =
    """    YliopistoKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KorkeakouluMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: yo
      |              enum:
      |                - yo
      |""".stripMargin

  val AmmattikorkeaKoulutusMetadataModel: String =
    """    AmmattikorkeaKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KorkeakouluMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: amk
      |              enum:
      |                - amk
      |""".stripMargin

  val AmmOpeErityisopeJaOpoKoulutusMetadataModel: String =
    """    AmmOpeErityisopeJaOpoKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KorkeakouluMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: amm-ope-erityisope-ja-opo
      |              enum:
      |                - amm-ope-erityisope-ja-opo
      |""".stripMargin

  val OpePedagOpinnotKoulutusMetadataModel: String =
    """    OpePedagOpinnotKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KorkeakouluMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: ope-pedag-opinnot
      |              enum:
      |                - ope-pedag-opinnot
      |""".stripMargin

  val KkOpintojaksoKoulutusMetadataModel: String =
    """    KkOpintojaksoKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: kk-opintojakso
      |              enum:
      |                - kk-opintojakso
      |            koulutusalaKoodiUrit:
      |              type: array
      |              description: Lista koulutusaloja. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kansallinenkoulutusluokitus2016koulutusalataso1/1)
      |              items:
      |                type: string
      |                example:
      |                  - kansallinenkoulutusluokitus2016koulutusalataso1_001#1
      |            opintojenLaajuusyksikkoKoodiUri:
      |              type: string
      |              description: "Opintojen laajuusyksikko. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuusyksikko/1)"
      |              example: opintojenlaajuusyksikko_2#1
      |            opintojenLaajuusNumeroMin:
      |              type: integer
      |              description: Opintojen laajuuden vähimmäismäärä numeroarvona
      |              example: 10
      |            opintojenLaajuusNumeroMax:
      |              type: integer
      |              description: Opintojen laajuuden enimmäismäärä numeroarvona
      |              example: 20
      |            isAvoinKorkeakoulutus:
      |              type: boolean
      |              description: Onko koulutus avointa korkeakoulutusta?
      |            tunniste:
      |              type: string
      |              description: Hakijalle näkyvä tunniste
      |            opinnonTyyppiKoodiUri:
      |              type: string
      |              description: Opinnon tyyppi. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-app/html/koodisto/opinnontyyppi/1)
      |              example: opinnontyyppi_1#1
      |            korkeakoulutustyypit:
      |              type: array
      |              description: Lista korkeakoulutustyypeistä (amk, yo) minkä tyyppisenä ko. koulutus käytännössä järjestetään. Jos tyyppejä on useita, listataan jokaiselle tyypille tarjoajat erikseen.
      |              items:
      |                $ref: '#/components/schemas/Korkeakoulutustyyppi'
      |""".stripMargin

  val KkOpintokokonaisuusKoulutusMetadataModel: String =
    """    KkOpintokokonaisuusKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: kk-opintokokonaisuus
      |              enum:
      |                - kk-opintokokonaisuus
      |            koulutusalaKoodiUrit:
      |              type: array
      |              description: Lista koulutusaloja. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kansallinenkoulutusluokitus2016koulutusalataso1/1)
      |              items:
      |                type: string
      |                example:
      |                  - kansallinenkoulutusluokitus2016koulutusalataso1_001#1
      |            opintojenLaajuusyksikkoKoodiUri:
      |              type: string
      |              description: "Opintojen laajuusyksikko. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuusyksikko/1)"
      |              example: opintojenlaajuusyksikko_2#1
      |            opintojenLaajuusNumeroMin:
      |              type: integer
      |              description: Opintojen laajuuden vähimmäismäärä numeroarvona
      |              example: 10
      |            opintojenLaajuusNumeroMax:
      |              type: integer
      |              description: Opintojen laajuuden enimmäismäärä numeroarvona
      |              example: 20
      |            isAvoinKorkeakoulutus:
      |              type: boolean
      |              description: Onko koulutus avointa korkeakoulutusta?
      |            tunniste:
      |              type: string
      |              description: Hakijalle näkyvä tunniste
      |            opinnonTyyppiKoodiUri:
      |              type: string
      |              description: Opinnon tyyppi. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-app/html/koodisto/opinnontyyppi/1)
      |              example: opinnontyyppi_1#1
      |            korkeakoulutustyypit:
      |              type: array
      |              description: Lista korkeakoulutustyypeistä (amk, yo) minkä tyyppisenä ko. koulutus käytännössä järjestetään. Jos tyyppejä on useita, listataan jokaiselle tyypille tarjoajat erikseen.
      |              items:
      |                $ref: '#/components/schemas/Korkeakoulutustyyppi'
      |""".stripMargin

  val AmmTutkintoWithoutEperusteFieldDescription =
    s"""HUOM! Syötettävissä vain kun koulutuksetKoodiUri-kenttään on valittu jokin seuraavista&#58; ${AmmKoulutusKooditWithoutEperuste.koulutusKoodiUrit
      .mkString(", ")}. Muuten käytetään valitulta ePerusteelta (ePerusteId) tulevaa arvoa."""

  val AmmatillinenKoulutusMetadataModel: String =
    s"""    AmmatillinenKoulutusMetadata:
       |      allOf:
       |        - $$ref: '#/components/schemas/KoulutusMetadata'
       |        - type: object
       |          properties:
       |            tyyppi:
       |              type: string
       |              description: Koulutuksen metatiedon tyyppi
       |              example: amm
       |              enum:
       |                - amm
       |            koulutusalaKoodiUrit:
       |              type: array
       |              description: |
       |                Lista koulutusaloja. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-app/koodisto/view/kansallinenkoulutusluokitus2016koulutusalataso2).
       |                ${AmmTutkintoWithoutEperusteFieldDescription}
       |              items:
       |                type: string
       |              example:
       |                - kansallinenkoulutusluokitus2016koulutusalataso2_054#1
       |                - kansallinenkoulutusluokitus2016koulutusalataso2_055#1
       |            tutkintonimikeKoodiUrit:
       |              type: array
       |              description: |
       |                Lista koulutuksen tutkintonimikkeistä. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-app/koodisto/view/tutkintonimikkeet).
       |                ${AmmTutkintoWithoutEperusteFieldDescription}
       |              items:
       |                type: string
       |              example:
       |                - tutkintonimikkeet_10091#2
       |                - tutkintonimikkeet_10015#2
       |            opintojenLaajuusyksikkoKoodiUri:
       |              type: string
       |              description: |
       |                Opintojen laajuusyksikko. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-app/koodisto/view/opintojenlaajuusyksikko).
       |                ${AmmTutkintoWithoutEperusteFieldDescription}
       |              example: opintojenlaajuusyksikko_2#1
       |            opintojenLaajuusNumero:
       |              type: integer
       |              description: Opintojen laajuus tai kesto numeroarvona. ${AmmTutkintoWithoutEperusteFieldDescription}
       |              example: 10
       |""".stripMargin

  val AmmatillinenTutkinnonOsaKoulutusMetadataModel: String =
    """    AmmatillinenTutkinnonOsaKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: amm-tutkinnon-osa
      |              enum:
      |                - amm-tutkinnon-osa
      |            tutkinnonOsat:
      |              type: array
      |              description: Tutkinnon osat
      |              items:
      |                type: object
      |                $ref: '#/components/schemas/TutkinnonOsa'
      |""".stripMargin

  val AmmatillinenOsaamisalaKoulutusMetadataModel: String =
    """    AmmatillinenOsaamisalaKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: amm
      |              enum:
      |                - amm
      |            osaamisalaKoodiUri:
      |              type: string
      |              description: Osaamisala. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/osaamisala/1)
      |              example: osaamisala_10#1
      |""".stripMargin

  val AmmatillinenMuuKoulutusMetadataModel: String =
    """    AmmatillinenMuuKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: amm-muu
      |              enum:
      |                - amm-muu
      |            koulutusalaKoodiUrit:
      |              type: array
      |              description: Lista koulutusaloja. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kansallinenkoulutusluokitus2016koulutusalataso1/1)
      |              items:
      |                type: string
      |                example:
      |                  - kansallinenkoulutusluokitus2016koulutusalataso1_001#1
      |            opintojenLaajuusyksikkoKoodiUri:
      |              type: string
      |              description: "Opintojen laajuusyksikko. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuusyksikko/1)"
      |              example: opintojenlaajuusyksikko_6#1
      |            opintojenLaajuusNumero:
      |              type: integer
      |              description: Opintojen laajuus tai kesto numeroarvona
      |              example: 10
      |""".stripMargin

  val LukioKoulutusMetadataModel: String =
    """    LukioKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: lk
      |              enum:
      |                - lk
      |            koulutusalaKoodiUrit:
      |              type: array
      |              description: Lista koulutusaloja. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kansallinenkoulutusluokitus2016koulutusalataso1/1)
      |              items:
      |                type: string
      |                example:
      |                  - kansallinenkoulutusluokitus2016koulutusalataso1_001#1
      |            opintojenLaajuusyksikkoKoodiUri:
      |              type: string
      |              description: "Opintojen laajuusyksikko. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuusyksikko/1)"
      |              example: opintojenlaajuusyksikko_2#1
      |            opintojenLaajuusNumero:
      |              type: integer
      |              description: Opintojen laajuus tai kesto numeroarvona
      |              example: 10
      |""".stripMargin

  val TuvaKoulutusMetadataModel: String =
    """    TuvaKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: tuva
      |              enum:
      |                - tuva
      |            linkkiEPerusteisiin:
      |              type: string
      |              description: Linkki koulutuksen eperusteisiin
      |              example: https://eperusteet.opintopolku.fi/#/fi/kooste/telma
      |            opintojenLaajuusyksikkoKoodiUri:
      |              type: string
      |              description: "Opintojen laajuusyksikko. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuusyksikko/1)"
      |              example: opintojenlaajuusyksikko_8#1
      |            opintojenLaajuusNumero:
      |              type: integer
      |              description: Opintojen laajuus tai kesto numeroarvona
      |              example: 10
      |""".stripMargin

  val TelmaKoulutusMetadataModel: String =
    """    TelmaKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: telma
      |              enum:
      |                - telma
      |            linkkiEPerusteisiin:
      |              type: string
      |              description: Linkki koulutuksen eperusteisiin
      |              example: https://eperusteet.opintopolku.fi/#/fi/kooste/telma
      |            opintojenLaajuusyksikkoKoodiUri:
      |              type: string
      |              description: "Opintojen laajuusyksikko. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuusyksikko/1)"
      |              example: opintojenlaajuusyksikko_6#1
      |            opintojenLaajuusNumero:
      |              type: integer
      |              description: Opintojen laajuus tai kesto numeroarvona
      |              example: 10
      |""".stripMargin

  val VapaaSivistystyoKoulutusMetadataModel: String =
    """    VapaaSivistystyoKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: vapaa-sivistystyo-opistovuosi
      |              enum:
      |                - vapaa-sivistystyo-opistovuosi
      |                - vapaa-sivistystyo-muu
      |            koulutusalaKoodiUrit:
      |              type: array
      |              description: Lista koulutusaloja. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kansallinenkoulutusluokitus2016koulutusalataso1/1)
      |              items:
      |                type: string
      |                example:
      |                  - kansallinenkoulutusluokitus2016koulutusalataso1_001#1
      |            opintojenLaajuusyksikkoKoodiUri:
      |              type: string
      |              description: "Opintojen laajuusyksikko. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuusyksikko/1)"
      |              example: opintojenlaajuusyksikko_2#1
      |            opintojenLaajuusNumero:
      |              type: integer
      |              description: Opintojen laajuus tai kesto numeroarvona
      |              example: 10
      |""".stripMargin

  val VapaaSivistystyoOsaamismerkkiKoulutusMetadataModel: String =
    """    VapaaSivistystyoOsaamismerkkiKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: vapaa-sivistystyo-osaamismerkki
      |              enum:
      |                - vapaa-sivistystyo-osaamismerkki
      |            koulutusalaKoodiUrit:
      |              type: array
      |              description: Lista koulutusaloja. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kansallinenkoulutusluokitus2016koulutusalataso1/1)
      |              items:
      |                type: string
      |                example:
      |                  - kansallinenkoulutusluokitus2016koulutusalataso1_001#1
      |            osaamismerkkiKoodiUri:
      |              type: string
      |              description: Osaamismerkki. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-app/koodisto/view/osaamismerkit/1)
      |              example: osaamismerkit_1009#1
      |""".stripMargin

  val AikuistenPerusopetusKoulutusMetadataModel: String =
    """    AikuistenPerusopetusKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: aikuisten-perusopetus
      |              enum:
      |                - aikuisten-perusopetus
      |            linkkiEPerusteisiin:
      |              type: string
      |              description: Linkki koulutuksen eperusteisiin
      |              example: https://eperusteet.opintopolku.fi/#/fi/kooste/telma
      |            opintojenLaajuusyksikkoKoodiUri:
      |              type: string
      |              description: "Opintojen laajuusyksikko. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuusyksikko/1)"
      |              example: opintojenlaajuusyksikko_6#1
      |            opintojenLaajuusNumero:
      |              type: integer
      |              description: Opintojen laajuus tai kesto numeroarvona
      |              example: 10
      |""".stripMargin

  val ErikoislaakariKoulutusMetadataModel: String =
    """    ErikoislaakariKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: erikoislaakari
      |              enum:
      |                - erikoislaakari
      |            tutkintonimikeKoodiUrit:
      |              type: array
      |              description: Lista koulutuksen tutkintonimikkeistä. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/tutkintonimikekk/2)
      |              items:
      |                type: string
      |              example:
      |                - tutkintonimikekk_110#2
      |                - tutkintonimikekk_111#2
      |""".stripMargin

  val ErikoistumiskoulutusMetadataModel: String =
    """    ErikoistumiskoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: erikoistumiskoulutus
      |              enum:
      |                - erikoistumiskoulutus
      |            erikoistumiskoulutusKoodiUri:
      |              type: string
      |              description: Erikoistumiskoulutuksen koodiURI. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/erikoistumiskoulutukset/2)
      |              example:
      |                - erikoistumiskoulutukset_001#2
      |            koulutusalaKoodiUrit:
      |              type: array
      |              description: Lista koulutusaloja. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kansallinenkoulutusluokitus2016koulutusalataso1/1)
      |              items:
      |                type: string
      |                example:
      |                  - kansallinenkoulutusluokitus2016koulutusalataso1_001#1
      |            opintojenLaajuusyksikkoKoodiUri:
      |              type: string
      |              description: Opintojen laajuusyksikko. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuusyksikko/1)
      |              example: opintojenlaajuusyksikko_2#1
      |            korkeakoulutustyypit:
      |              type: array
      |              description: Lista korkeakoulutustyypeistä (amk, yo) minkä tyyppisenä ko. koulutus käytännössä järjestetään. Jos tyyppejä on useita, listataan jokaiselle tyypille tarjoajat erikseen.
      |              items:
      |                $ref: '#/components/schemas/Korkeakoulutustyyppi'
      |            opintojenLaajuusNumeroMin:
      |              type: integer
      |              description: Opintojen laajuuden vähimmäismäärä numeroarvona
      |              example: 10
      |            opintojenLaajuusNumeroMax:
      |              type: integer
      |              description: Opintojen laajuuden enimmäismäärä numeroarvona
      |              example: 20
      |""".stripMargin

  val TaiteenPerusopetusKoulutusMetadataModel: String =
    """    TaiteenPerusopetusKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: taiteen-perusopetus
      |              enum:
      |                - taiteen-perusopetus
      |            linkkiEPerusteisiin:
      |              type: string
      |              description: Linkki koulutuksen eperusteisiin
      |              example: https://eperusteet.opintopolku.fi/#/fi/kooste/taiteenperusopetus
      |""".stripMargin

  val MuuKoulutusMetadataModel: String =
    """    MuuKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            tyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: muu
      |              enum:
      |                - muu
      |            koulutusalaKoodiUrit:
      |              type: array
      |              description: Lista koulutusaloja. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kansallinenkoulutusluokitus2016koulutusalataso1/1)
      |              items:
      |                type: string
      |                example:
      |                  - kansallinenkoulutusluokitus2016koulutusalataso1_001#1
      |            opintojenLaajuusyksikkoKoodiUri:
      |              type: string
      |              description: Opintojen laajuusyksikko. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuusyksikko/1)
      |              example: opintojenlaajuusyksikko_2#1
      |            opintojenLaajuusNumeroMin:
      |              type: integer
      |              description: Opintojen laajuuden vähimmäismäärä numeroarvona
      |              example: 10
      |            opintojenLaajuusNumeroMax:
      |              type: integer
      |              description: Opintojen laajuuden enimmäismäärä numeroarvona
      |              example: 20
      |""".stripMargin

  val models = List(
    KoulutusMetadataModel,
    AmmatillinenKoulutusMetadataModel,
    KorkeakouluMetadataModel,
    AmmattikorkeaKoulutusMetadataModel,
    AmmOpeErityisopeJaOpoKoulutusMetadataModel,
    OpePedagOpinnotKoulutusMetadataModel,
    YliopistoKoulutusMetadataModel,
    KkOpintojaksoKoulutusMetadataModel,
    AmmatillinenTutkinnonOsaKoulutusMetadataModel,
    AmmatillinenOsaamisalaKoulutusMetadataModel,
    AmmatillinenMuuKoulutusMetadataModel,
    LukioKoulutusMetadataModel,
    TuvaKoulutusMetadataModel,
    TelmaKoulutusMetadataModel,
    VapaaSivistystyoKoulutusMetadataModel,
    VapaaSivistystyoOsaamismerkkiKoulutusMetadataModel,
    AikuistenPerusopetusKoulutusMetadataModel,
    ErikoislaakariKoulutusMetadataModel,
    KkOpintokokonaisuusKoulutusMetadataModel,
    ErikoistumiskoulutusMetadataModel,
    TaiteenPerusopetusKoulutusMetadataModel,
    MuuKoulutusMetadataModel
  )
}

trait LaajuusMinMax {
  val opintojenLaajuusyksikkoKoodiUri: Option[String]
  val opintojenLaajuusNumeroMin: Option[Double]
  val opintojenLaajuusNumeroMax: Option[Double]
}

trait LaajuusSingle {
  val opintojenLaajuusyksikkoKoodiUri: Option[String]
  val opintojenLaajuusNumero: Option[Double]
}

sealed trait KoulutusMetadata {
  val tyyppi: Koulutustyyppi
  val kuvaus: Kielistetty
  val lisatiedot: Seq[Lisatieto]
  val isMuokkaajaOphVirkailija: Option[Boolean]
}

trait KorkeakoulutusKoulutusMetadata extends KoulutusMetadata with LaajuusSingle {
  val tutkintonimikeKoodiUrit: Seq[String]
  val koulutusalaKoodiUrit: Seq[String]
}

trait KorkeakoulutusRelatedKoulutusMetadata extends KoulutusMetadata with LaajuusMinMax {
  val korkeakoulutustyypit: Seq[Korkeakoulutustyyppi]
}

case class AmmatillinenKoulutusMetadata(
    tyyppi: Koulutustyyppi = Amm,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    // Alla olevat kentät vain ammatillisilla tutkintoon johtavilla koulutuksilla, joilla ei ole ePerustetta (pelastusala ja rikosseuraamusala)!
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    tutkintonimikeKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None
) extends KoulutusMetadata

case class AmmatillinenTutkinnonOsaKoulutusMetadata(
    tyyppi: Koulutustyyppi = AmmTutkinnonOsa,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    tutkinnonOsat: Seq[TutkinnonOsa] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadata

case class AmmatillinenOsaamisalaKoulutusMetadata(
    tyyppi: Koulutustyyppi = AmmOsaamisala,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    osaamisalaKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadata

case class AmmatillinenMuuKoulutusMetadata(
    tyyppi: Koulutustyyppi = AmmMuu,
    lisatiedot: Seq[Lisatieto] = Seq(),
    kuvaus: Kielistetty = Map(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    opintojenLaajuusNumero: Option[Double] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadata
    with LaajuusSingle

case class YliopistoKoulutusMetadata(
    tyyppi: Koulutustyyppi = Yo,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    tutkintonimikeKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KorkeakoulutusKoulutusMetadata

case class AmmattikorkeakouluKoulutusMetadata(
    tyyppi: Koulutustyyppi = Amk,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    tutkintonimikeKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KorkeakoulutusKoulutusMetadata

case class AmmOpeErityisopeJaOpoKoulutusMetadata(
    tyyppi: Koulutustyyppi = AmmOpeErityisopeJaOpo,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    tutkintonimikeKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KorkeakoulutusKoulutusMetadata

case class OpePedagOpinnotKoulutusMetadata(
    tyyppi: Koulutustyyppi = OpePedagOpinnot,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    tutkintonimikeKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KorkeakoulutusKoulutusMetadata

case class KkOpintojaksoKoulutusMetadata(
    tyyppi: Koulutustyyppi = KkOpintojakso,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumeroMin: Option[Double] = None,
    opintojenLaajuusNumeroMax: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    isAvoinKorkeakoulutus: Option[Boolean] = None,
    tunniste: Option[String] = None,
    opinnonTyyppiKoodiUri: Option[String] = None,
    korkeakoulutustyypit: Seq[Korkeakoulutustyyppi] = Seq()
) extends KorkeakoulutusRelatedKoulutusMetadata

case class KkOpintokokonaisuusKoulutusMetadata(
    tyyppi: Koulutustyyppi = KkOpintokokonaisuus,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumeroMin: Option[Double] = None,
    opintojenLaajuusNumeroMax: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    isAvoinKorkeakoulutus: Option[Boolean] = None,
    tunniste: Option[String] = None,
    opinnonTyyppiKoodiUri: Option[String] = None,
    korkeakoulutustyypit: Seq[Korkeakoulutustyyppi] = Seq()
) extends KorkeakoulutusRelatedKoulutusMetadata

case class LukioKoulutusMetadata(
    tyyppi: Koulutustyyppi = Lk,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    koulutusalaKoodiUrit: Seq[String] = Seq(), // koulutusalaKoodiUrit kovakoodataan koulutusService:ssa
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadata
    with LaajuusSingle

case class TuvaKoulutusMetadata(
    tyyppi: Koulutustyyppi = Tuva,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadata
    with LaajuusSingle

case class TelmaKoulutusMetadata(
    tyyppi: Koulutustyyppi = Telma,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadata
    with LaajuusSingle

trait VapaaSivistystyoKoulutusMetadata extends KoulutusMetadata with LaajuusSingle {
  val kuvaus: Kielistetty
  val linkkiEPerusteisiin: Kielistetty
  val koulutusalaKoodiUrit: Seq[String]
}

case class VapaaSivistystyoOpistovuosiKoulutusMetadata(
    tyyppi: Koulutustyyppi = VapaaSivistystyoOpistovuosi,
    lisatiedot: Seq[Lisatieto] = Seq(),
    kuvaus: Kielistetty = Map(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends VapaaSivistystyoKoulutusMetadata

case class VapaaSivistystyoMuuKoulutusMetadata(
    tyyppi: Koulutustyyppi = VapaaSivistystyoMuu,
    lisatiedot: Seq[Lisatieto] = Seq(),
    kuvaus: Kielistetty = Map(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends VapaaSivistystyoKoulutusMetadata

case class VapaaSivistystyoOsaamismerkkiKoulutusMetadata(
    tyyppi: Koulutustyyppi = VapaaSivistystyoOsaamismerkki,
    lisatiedot: Seq[Lisatieto] = Seq(),
    kuvaus: Kielistetty = Map(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusNumero: Option[Double] = None,
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    osaamismerkkiKoodiUri: Option[String],
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends VapaaSivistystyoKoulutusMetadata

case class AikuistenPerusopetusKoulutusMetadata(
    tyyppi: Koulutustyyppi = AikuistenPerusopetus,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    opintojenLaajuusNumero: Option[Double] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadata
    with LaajuusSingle

// koulutusalaKoodiUrit kovakoodataan koulutusService:ssa
case class ErikoislaakariKoulutusMetadata(
    tyyppi: Koulutustyyppi = Erikoislaakari,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    tutkintonimikeKoodiUrit: Seq[String] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadata

case class ErikoistumiskoulutusMetadata(
    tyyppi: Koulutustyyppi = Erikoistumiskoulutus,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    erikoistumiskoulutusKoodiUri: Option[String] = None,
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    opintojenLaajuusNumeroMin: Option[Double] = None,
    opintojenLaajuusNumeroMax: Option[Double] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None,
    korkeakoulutustyypit: Seq[Korkeakoulutustyyppi] = Seq()
) extends KorkeakoulutusRelatedKoulutusMetadata

case class TaiteenPerusopetusKoulutusMetadata(
    tyyppi: Koulutustyyppi = TaiteenPerusopetus,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    linkkiEPerusteisiin: Kielistetty = Map(),
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadata

case class MuuKoulutusMetadata(
    tyyppi: Koulutustyyppi = Muu,
    kuvaus: Kielistetty = Map(),
    lisatiedot: Seq[Lisatieto] = Seq(),
    koulutusalaKoodiUrit: Seq[String] = Seq(),
    opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
    opintojenLaajuusNumeroMin: Option[Double] = None,
    opintojenLaajuusNumeroMax: Option[Double] = None,
    isMuokkaajaOphVirkailija: Option[Boolean] = None
) extends KoulutusMetadata
    with LaajuusMinMax
