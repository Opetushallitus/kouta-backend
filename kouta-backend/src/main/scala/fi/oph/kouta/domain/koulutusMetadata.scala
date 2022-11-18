package fi.oph.kouta.domain

import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.validation.{IsValid, NoErrors, ValidatableSubEntity}
import fi.oph.kouta.validation.Validations.{assertNotOptional, _}

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
      |        opintojenLaajuusKoodiUri:
      |          type: string
      |          description: "Tutkinnon laajuus. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuus/1)"
      |          example: opintojenlaajuus_40#1
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
      |              example: opintojenlaajuusyksikko_6#1
      |            opintojenLaajuusNumero:
      |              type: integer
      |              description: Opintojen laajuus tai kesto numeroarvona
      |              example: 10
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
      |              description: "Opintojen laajuusyksikko. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuusyksikko/1)"
      |              example: opintojenlaajuusyksikko_6#1
      |            opintojenLaajuusNumeroMin:
      |              type: integer
      |              description: Opintojen laajuuden tai keston vähimmäismäärä numeroarvona
      |              example: 10
      |            opintojenLaajuusNumeroMax:
      |              type: integer
      |              description: Opintojen laajuuden tai keston enimmäismäärä numeroarvona
      |              example: 20
      |""".stripMargin

  val AmmatillinenKoulutusMetadataModel: String =
    """    AmmatillinenKoulutusMetadata:
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
      |            opintojenLaajuusKoodiUri:
      |              type: string
      |              description: "Tutkinnon laajuus. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuus/1)"
      |              example: opintojenlaajuus_40#1
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
      |              example: https://eperusteet.opintopolku.fi/beta/#/fi/kooste/telma
      |            opintojenLaajuusKoodiUri:
      |              type: string
      |              description: "Tutkinnon laajuus. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuus/1)"
      |              example: opintojenlaajuus_38#1
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
      |              example: https://eperusteet.opintopolku.fi/beta/#/fi/kooste/telma
      |            opintojenLaajuusKoodiUri:
      |              type: string
      |              description: "Tutkinnon laajuus. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuus/1)"
      |              example: opintojenlaajuus_60#1
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
      |            opintojenLaajuusKoodiUri:
      |              type: string
      |              description: "Tutkinnon laajuus. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuus/1)"
      |              example: opintojenlaajuus_40#1
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
      |              example: https://eperusteet.opintopolku.fi/beta/#/fi/kooste/telma
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

  val models = List(KoulutusMetadataModel, AmmatillinenKoulutusMetadataModel, KorkeakouluMetadataModel, AmmattikorkeaKoulutusMetadataModel, AmmOpeErityisopeJaOpoKoulutusMetadataModel, OpePedagOpinnotKoulutusMetadataModel,
    YliopistoKoulutusMetadataModel, KkOpintojaksoKoulutusMetadataModel, AmmatillinenTutkinnonOsaKoulutusMetadataModel, AmmatillinenOsaamisalaKoulutusMetadataModel, AmmatillinenMuuKoulutusMetadataModel, LukioKoulutusMetadataModel,
    TuvaKoulutusMetadataModel, TelmaKoulutusMetadataModel, VapaaSivistystyoKoulutusMetadataModel, AikuistenPerusopetusKoulutusMetadataModel, ErikoislaakariKoulutusMetadataModel, KkOpintokokonaisuusKoulutusMetadataModel)
}

sealed trait KoulutusMetadata {
  val tyyppi: Koulutustyyppi
  val kuvaus: Kielistetty
  val lisatiedot: Seq[Lisatieto]
  val isMuokkaajaOphVirkailija: Option[Boolean]
}

trait KorkeakoulutusKoulutusMetadata extends KoulutusMetadata {
  val tutkintonimikeKoodiUrit: Seq[String]
  val opintojenLaajuusKoodiUri: Option[String]
  val koulutusalaKoodiUrit: Seq[String]
}

case class AmmatillinenKoulutusMetadata(tyyppi: Koulutustyyppi = Amm,
                                        kuvaus: Kielistetty = Map(),
                                        lisatiedot: Seq[Lisatieto] = Seq(),
                                        isMuokkaajaOphVirkailija: Option[Boolean] = None) extends KoulutusMetadata

case class AmmatillinenTutkinnonOsaKoulutusMetadata(tyyppi: Koulutustyyppi = AmmTutkinnonOsa,
                                                    kuvaus: Kielistetty = Map(),
                                                    lisatiedot: Seq[Lisatieto] = Seq(),
                                                    tutkinnonOsat: Seq[TutkinnonOsa] = Seq(),
                                                    isMuokkaajaOphVirkailija: Option[Boolean] = None) extends KoulutusMetadata

case class AmmatillinenOsaamisalaKoulutusMetadata(tyyppi: Koulutustyyppi = AmmOsaamisala,
                                                  kuvaus: Kielistetty = Map(),
                                                  lisatiedot: Seq[Lisatieto] = Seq(),
                                                  osaamisalaKoodiUri: Option[String] = None,
                                                  isMuokkaajaOphVirkailija: Option[Boolean] = None) extends KoulutusMetadata

case class AmmatillinenMuuKoulutusMetadata(tyyppi: Koulutustyyppi = AmmMuu,
                                           lisatiedot: Seq[Lisatieto] = Seq(),
                                           kuvaus: Kielistetty = Map(),
                                           koulutusalaKoodiUrit: Seq[String] = Seq(),
                                           opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
                                           opintojenLaajuusNumero: Option[Double] = None,
                                           isMuokkaajaOphVirkailija: Option[Boolean] = None
                                          ) extends KoulutusMetadata

case class YliopistoKoulutusMetadata(tyyppi: Koulutustyyppi = Yo,
                                     kuvaus: Kielistetty = Map(),
                                     lisatiedot: Seq[Lisatieto] = Seq(),
                                     koulutusalaKoodiUrit: Seq[String] = Seq(),
                                     tutkintonimikeKoodiUrit: Seq[String] = Seq(),
                                     opintojenLaajuusKoodiUri: Option[String] = None,
                                     isMuokkaajaOphVirkailija: Option[Boolean] = None) extends KorkeakoulutusKoulutusMetadata

case class AmmattikorkeakouluKoulutusMetadata(tyyppi: Koulutustyyppi = Amk,
                                              kuvaus: Kielistetty = Map(),
                                              lisatiedot: Seq[Lisatieto] = Seq(),
                                              koulutusalaKoodiUrit: Seq[String] = Seq(),
                                              tutkintonimikeKoodiUrit: Seq[String] = Seq(),
                                              opintojenLaajuusKoodiUri: Option[String] = None,
                                              isMuokkaajaOphVirkailija: Option[Boolean] = None) extends KorkeakoulutusKoulutusMetadata

case class AmmOpeErityisopeJaOpoKoulutusMetadata(tyyppi: Koulutustyyppi = AmmOpeErityisopeJaOpo,
                                                 kuvaus: Kielistetty = Map(),
                                                 lisatiedot: Seq[Lisatieto] = Seq(),
                                                 koulutusalaKoodiUrit: Seq[String] = Seq(),
                                                 tutkintonimikeKoodiUrit: Seq[String] = Seq(),
                                                 opintojenLaajuusKoodiUri: Option[String] = None,
                                                 isMuokkaajaOphVirkailija: Option[Boolean] = None) extends KorkeakoulutusKoulutusMetadata

case class OpePedagOpinnotKoulutusMetadata(tyyppi: Koulutustyyppi = OpePedagOpinnot,
                                                 kuvaus: Kielistetty = Map(),
                                                 lisatiedot: Seq[Lisatieto] = Seq(),
                                                 koulutusalaKoodiUrit: Seq[String] = Seq(),
                                                 tutkintonimikeKoodiUrit: Seq[String] = Seq(),
                                                 opintojenLaajuusKoodiUri: Option[String] = None,
                                                 isMuokkaajaOphVirkailija: Option[Boolean] = None) extends KorkeakoulutusKoulutusMetadata

case class KkOpintojaksoKoulutusMetadata(tyyppi: Koulutustyyppi = KkOpintojakso,
                                         kuvaus: Kielistetty = Map(),
                                         lisatiedot: Seq[Lisatieto] = Seq(),
                                         koulutusalaKoodiUrit: Seq[String] = Seq(),
                                         opintojenLaajuusNumero: Option[Double] = None,
                                         opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
                                         isMuokkaajaOphVirkailija: Option[Boolean] = None,
                                         isAvoinKorkeakoulutus: Option[Boolean] = None,
                                         tunniste: Option[String] = None,
                                         opinnonTyyppiKoodiUri: Option[String] = None,
                                         jarjestajat: Seq[OrganisaatioOid] = Seq()) extends KoulutusMetadata

case class KkOpintokokonaisuusKoulutusMetadata(tyyppi: Koulutustyyppi = KkOpintokokonaisuus,
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
                                               jarjestajat: Seq[OrganisaatioOid] = Seq()) extends KoulutusMetadata

case class LukioKoulutusMetadata(tyyppi: Koulutustyyppi = Lk,
                                 kuvaus: Kielistetty = Map(),
                                 lisatiedot: Seq[Lisatieto] = Seq(),
                                 opintojenLaajuusKoodiUri: Option[String] = None,
                                 koulutusalaKoodiUrit: Seq[String] = Seq(), // koulutusalaKoodiUrit kovakoodataan koulutusService:ssa
                                 isMuokkaajaOphVirkailija: Option[Boolean] = None) extends KoulutusMetadata

case class TuvaKoulutusMetadata(tyyppi: Koulutustyyppi = Tuva,
                                kuvaus: Kielistetty = Map(),
                                lisatiedot: Seq[Lisatieto] = Seq(),
                                linkkiEPerusteisiin: Kielistetty = Map(),
                                opintojenLaajuusKoodiUri: Option[String] = None,
                                isMuokkaajaOphVirkailija: Option[Boolean] = None) extends KoulutusMetadata

case class TelmaKoulutusMetadata(tyyppi: Koulutustyyppi = Telma,
                                 kuvaus: Kielistetty = Map(),
                                 lisatiedot: Seq[Lisatieto] = Seq(),
                                 linkkiEPerusteisiin: Kielistetty = Map(),
                                 opintojenLaajuusKoodiUri: Option[String] = None,
                                 isMuokkaajaOphVirkailija: Option[Boolean] = None) extends KoulutusMetadata

trait VapaaSivistystyoKoulutusMetadata extends KoulutusMetadata {
  val kuvaus: Kielistetty
  val linkkiEPerusteisiin: Kielistetty
  val koulutusalaKoodiUrit: Seq[String]
}

case class VapaaSivistystyoOpistovuosiKoulutusMetadata(tyyppi: Koulutustyyppi = VapaaSivistystyoOpistovuosi,
                                                       lisatiedot: Seq[Lisatieto] = Seq(),
                                                       kuvaus: Kielistetty = Map(),
                                                       linkkiEPerusteisiin: Kielistetty = Map(),
                                                       koulutusalaKoodiUrit: Seq[String] = Seq(),
                                                       opintojenLaajuusKoodiUri: Option[String] = None,
                                                       isMuokkaajaOphVirkailija: Option[Boolean] = None
                                                      ) extends VapaaSivistystyoKoulutusMetadata

case class VapaaSivistystyoMuuKoulutusMetadata(tyyppi: Koulutustyyppi = VapaaSivistystyoMuu,
                                                       lisatiedot: Seq[Lisatieto] = Seq(),
                                                       kuvaus: Kielistetty = Map(),
                                                       linkkiEPerusteisiin: Kielistetty = Map(),
                                                       koulutusalaKoodiUrit: Seq[String] = Seq(),
                                                       opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
                                                       opintojenLaajuusNumero: Option[Double] = None,
                                                       isMuokkaajaOphVirkailija: Option[Boolean] = None
                                                      ) extends VapaaSivistystyoKoulutusMetadata

case class AikuistenPerusopetusKoulutusMetadata(tyyppi: Koulutustyyppi = AikuistenPerusopetus,
                                                kuvaus: Kielistetty = Map(),
                                                lisatiedot: Seq[Lisatieto] = Seq(),
                                                linkkiEPerusteisiin: Kielistetty = Map(),
                                                opintojenLaajuusyksikkoKoodiUri: Option[String] = None,
                                                opintojenLaajuusNumero: Option[Double] = None,
                                                isMuokkaajaOphVirkailija: Option[Boolean] = None
                                               ) extends KoulutusMetadata

// koulutusalaKoodiUrit kovakoodataan koulutusService:ssa
case class ErikoislaakariKoulutusMetadata(tyyppi: Koulutustyyppi = Erikoislaakari,
                                          kuvaus: Kielistetty = Map(),
                                          lisatiedot: Seq[Lisatieto] = Seq(),
                                          tutkintonimikeKoodiUrit: Seq[String] = Seq(),
                                          koulutusalaKoodiUrit: Seq[String] = Seq(),
                                          isMuokkaajaOphVirkailija: Option[Boolean] = None
                                         ) extends KoulutusMetadata
