package fi.oph.kouta.domain

import fi.oph.kouta.validation.{IsValid, ValidatableSubEntity}
import fi.oph.kouta.validation.Validations._

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
      |        kuvauksenNimi:
      |          type: object
      |          description: Koulutuksen kuvauksen nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          $ref: '#/components/schemas/Nimi'
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
      |            koulutustyyppi:
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
      |            koulutustyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: amk
      |              enum:
      |                - amk
      |""".stripMargin

  val AmmatillinenKoulutusMetadataModel: String =
    """    AmmatillinenKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            koulutustyyppi:
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
      |            koulutustyyppi:
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
      |            koulutustyyppi:
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
      |            koulutustyyppi:
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
      |            opintojenLaajuusKoodiUri:
      |              type: string
      |              description: "Tutkinnon laajuus. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuus/1)"
      |              example: opintojenlaajuus_40#1
      |""".stripMargin

  val LukioKoulutusMetadataModel: String =
    """    LukioKoulutusMetadata:
      |      allOf:
      |        - $ref: '#/components/schemas/KoulutusMetadata'
      |        - type: object
      |          properties:
      |            koulutustyyppi:
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
      |            koulutustyyppi:
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
      |            koulutustyyppi:
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
      |            koulutustyyppi:
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
      |            koulutustyyppi:
      |              type: string
      |              description: Koulutuksen metatiedon tyyppi
      |              example: aikuisten-perusopetus
      |              enum:
      |                - aikuisten-perusopetus
      |            linkkiEPerusteisiin:
      |              type: string
      |              description: Linkki koulutuksen eperusteisiin
      |              example: https://eperusteet.opintopolku.fi/beta/#/fi/kooste/telma
      |            opintojenLaajuusKoodiUri:
      |              type: string
      |              description: "Tutkinnon laajuus. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuus/1)"
      |              example: opintojenlaajuus_38#1
      |""".stripMargin

  val models = List(KoulutusMetadataModel, AmmatillinenKoulutusMetadataModel, KorkeakouluMetadataModel, AmmattikorkeaKoulutusMetadataModel,
    YliopistoKoulutusMetadataModel, AmmatillinenTutkinnonOsaKoulutusMetadataModel, AmmatillinenOsaamisalaKoulutusMetadataModel, AmmatillinenMuuKoulutusMetadataModel, LukioKoulutusMetadataModel,
    TuvaKoulutusMetadataModel, TelmaKoulutusMetadataModel, VapaaSivistystyoKoulutusMetadataModel, AikuistenPerusopetusKoulutusMetadataModel)
}

sealed trait KoulutusMetadata extends ValidatableSubEntity {
  val tyyppi: Koulutustyyppi
  val kuvaus: Kielistetty
  val lisatiedot: Seq[Lisatieto]
  val isMuokkaajaOphVirkailija: Option[Boolean]

  def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    validateIfJulkaistu(tila, validateOptionalKielistetty(kielivalinta, kuvaus, s"$path.kuvaus")),
    validateIfNonEmpty[Lisatieto](lisatiedot, s"$path.lisatiedot", _.validate(tila, kielivalinta, _))
  )
}

trait KorkeakoulutusKoulutusMetadata extends KoulutusMetadata {
  val kuvauksenNimi: Kielistetty
  val tutkintonimikeKoodiUrit: Seq[String]
  val opintojenLaajuusKoodiUri: Option[String]
  val koulutusalaKoodiUrit: Seq[String]

  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    validateIfJulkaistu(tila, validateKielistetty(kielivalinta, kuvauksenNimi, s"$path.kuvauksenNimi")),
    validateIfNonEmpty[String](tutkintonimikeKoodiUrit, s"$path.tutkintonimikeKoodiUrit", assertMatch(_, TutkintonimikeKoodiPattern, _)),
    validateIfDefined[String](opintojenLaajuusKoodiUri, assertMatch(_, OpintojenLaajuusKoodiPattern, s"$path.opintojenLaajuusKoodiUri")),
    validateIfNonEmpty[String](koulutusalaKoodiUrit, s"$path.koulutusalaKoodiUrit", assertMatch(_, KoulutusalaKoodiPattern, _))
  )
}

case class AmmatillinenKoulutusMetadata(tyyppi: Koulutustyyppi = Amm,
                                        kuvaus: Kielistetty = Map(),
                                        lisatiedot: Seq[Lisatieto] = Seq(),
                                        isMuokkaajaOphVirkailija: Option[Boolean] = None) extends KoulutusMetadata

case class AmmatillinenTutkinnonOsaKoulutusMetadata(tyyppi: Koulutustyyppi = AmmTutkinnonOsa,
                                                    kuvaus: Kielistetty = Map(),
                                                    lisatiedot: Seq[Lisatieto] = Seq(),
                                                    tutkinnonOsat: Seq[TutkinnonOsa] = Seq(),
                                                    isMuokkaajaOphVirkailija: Option[Boolean] = None) extends KoulutusMetadata {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    validateIfNonEmpty[TutkinnonOsa](tutkinnonOsat, s"$path.tutkinnonOsat", _.validate(tila, kielivalinta, _)),
    validateIfJulkaistu(tila,
      assertNotEmpty(tutkinnonOsat, s"$path.tutkinnonOsat"))
  )
}

case class AmmatillinenOsaamisalaKoulutusMetadata(tyyppi: Koulutustyyppi = AmmOsaamisala,
                                                  kuvaus: Kielistetty = Map(),
                                                  lisatiedot: Seq[Lisatieto] = Seq(),
                                                  osaamisalaKoodiUri: Option[String] = None,
                                                  isMuokkaajaOphVirkailija: Option[Boolean] = None) extends KoulutusMetadata {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    validateIfDefined[String](osaamisalaKoodiUri, assertMatch(_, OsaamisalaKoodiPattern, s"$path.osaamisalaKoodiUri")),
    validateIfJulkaistu(tila, assertNotOptional(osaamisalaKoodiUri, s"$path.osaamisalaKoodiUri"))
  )
}

case class AmmatillinenMuuKoulutusMetadata(tyyppi: Koulutustyyppi = AmmMuu,
                                           lisatiedot: Seq[Lisatieto] = Seq(),
                                           kuvaus: Kielistetty = Map(),
                                           koulutusalaKoodiUrit: Seq[String] = Seq(),
                                           opintojenLaajuusKoodiUri: Option[String] = None,
                                           isMuokkaajaOphVirkailija: Option[Boolean] = None
                                          ) extends KoulutusMetadata {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    validateIfJulkaistu(tila, assertNotOptional(opintojenLaajuusKoodiUri, s"$path.opintojenLaajuusKoodiUri")),
    validateIfDefined[String](opintojenLaajuusKoodiUri, assertMatch(_, OpintojenLaajuusKoodiPattern, s"$path.opintojenLaajuusKoodiUri")),
    validateIfNonEmpty[String](koulutusalaKoodiUrit, s"$path.koulutusalaKoodiUrit", assertMatch(_, KoulutusalaKoodiPattern, _)),
    validateIfJulkaistu(tila, validateKielistetty(kielivalinta, kuvaus, s"$path.kuvaus"))
  )
}

case class YliopistoKoulutusMetadata(tyyppi: Koulutustyyppi = Yo,
                                     kuvaus: Kielistetty = Map(),
                                     lisatiedot: Seq[Lisatieto] = Seq(),
                                     koulutusalaKoodiUrit: Seq[String] = Seq(),
                                     tutkintonimikeKoodiUrit: Seq[String] = Seq(),
                                     opintojenLaajuusKoodiUri: Option[String] = None,
                                     kuvauksenNimi: Kielistetty = Map(),
                                     isMuokkaajaOphVirkailija: Option[Boolean] = None) extends KorkeakoulutusKoulutusMetadata

case class AmmattikorkeakouluKoulutusMetadata(tyyppi: Koulutustyyppi = Amk,
                                              kuvaus: Kielistetty = Map(),
                                              lisatiedot: Seq[Lisatieto] = Seq(),
                                              koulutusalaKoodiUrit: Seq[String] = Seq(),
                                              tutkintonimikeKoodiUrit: Seq[String] = Seq(),
                                              opintojenLaajuusKoodiUri: Option[String] = None,
                                              kuvauksenNimi: Kielistetty = Map(),
                                              isMuokkaajaOphVirkailija: Option[Boolean] = None) extends KorkeakoulutusKoulutusMetadata

case class LukioKoulutusMetadata(tyyppi: Koulutustyyppi = Lk,
                                 kuvaus: Kielistetty = Map(),
                                 lisatiedot: Seq[Lisatieto] = Seq(),
                                 opintojenLaajuusKoodiUri: Option[String] = None,
                                 koulutusalaKoodiUrit: Seq[String] = Seq(),
                                 isMuokkaajaOphVirkailija: Option[Boolean] = None) extends KoulutusMetadata {
  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    validateIfDefined[String](opintojenLaajuusKoodiUri, assertMatch(_, OpintojenLaajuusKoodiPattern, s"$path.opintojenLaajuusKoodiUri")),
    validateIfNonEmpty[String](koulutusalaKoodiUrit, s"$path.koulutusalaKoodiUrit", assertMatch(_, KoulutusalaKoodiPattern, _))
  )
}

case class TuvaKoulutusMetadata(tyyppi: Koulutustyyppi = Tuva,
                                kuvaus: Kielistetty = Map(),
                                lisatiedot: Seq[Lisatieto] = Seq(),
                                linkkiEPerusteisiin: Kielistetty = Map(),
                                opintojenLaajuusKoodiUri: Option[String] = None,
                                isMuokkaajaOphVirkailija: Option[Boolean] = None) extends KoulutusMetadata {

  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    // Tuvalla ei ole lisätiedot-kenttää lomakkeessa
    assertEmpty(lisatiedot, path),
    // OpintojenLaajuusKoodiUri on pakollinen kenttä Tuvalle
    validateIfJulkaistu(tila, assertNotOptional(opintojenLaajuusKoodiUri, s"$path.opintojenLaajuusKoodiUri")),
    // Kuvaus on pakollinen kenttä Tuvalle
    validateIfJulkaistu(tila, validateKielistetty(kielivalinta, kuvaus, s"$path.kuvaus")),
    validateIfJulkaistu(tila, validateOptionalKielistetty(kielivalinta, linkkiEPerusteisiin, s"$path.linkkiEPerusteisiin")),
    validateIfNonEmpty(linkkiEPerusteisiin, s"$path.linkkiEPerusteisiin", assertValidUrl _),
  )
}

case class TelmaKoulutusMetadata(tyyppi: Koulutustyyppi = Telma,
                                 kuvaus: Kielistetty = Map(),
                                 lisatiedot: Seq[Lisatieto] = Seq(),
                                 linkkiEPerusteisiin: Kielistetty = Map(),
                                 opintojenLaajuusKoodiUri: Option[String] = None,
                                 isMuokkaajaOphVirkailija: Option[Boolean] = None) extends KoulutusMetadata {

  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    // Telmalla ei ole lisätiedot-kenttää lomakkeessa
    assertEmpty(lisatiedot, path),
    // OpintojenLaajuusKoodiUri on pakollinen kenttä Telmalle
    validateIfJulkaistu(tila, assertNotOptional(opintojenLaajuusKoodiUri, s"$path.opintojenLaajuusKoodiUri")),
    // Kuvaus on pakollinen kenttä Telmalle
    validateIfJulkaistu(tila, validateKielistetty(kielivalinta, kuvaus, s"$path.kuvaus")),
    validateIfJulkaistu(tila, validateOptionalKielistetty(kielivalinta, linkkiEPerusteisiin, s"$path.linkkiEPerusteisiin")),
    validateIfNonEmpty(linkkiEPerusteisiin, s"$path.linkkiEPerusteisiin", assertValidUrl _),
  )
}

trait VapaaSivistystyoKoulutusMetadata extends KoulutusMetadata {
  val kuvaus: Kielistetty
  val linkkiEPerusteisiin: Kielistetty
  val opintojenLaajuusKoodiUri: Option[String]
  val koulutusalaKoodiUrit: Seq[String]

  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    validateIfJulkaistu(tila, assertNotOptional(opintojenLaajuusKoodiUri, s"$path.opintojenLaajuusKoodiUri")),
    validateIfDefined[String](opintojenLaajuusKoodiUri, assertMatch(_, OpintojenLaajuusKoodiPattern, s"$path.opintojenLaajuusKoodiUri")),
    validateIfNonEmpty[String](koulutusalaKoodiUrit, s"$path.koulutusalaKoodiUrit", assertMatch(_, KoulutusalaKoodiPattern, _)),
    validateIfJulkaistu(tila, validateKielistetty(kielivalinta, kuvaus, s"$path.kuvaus")),
    validateIfJulkaistu(tila, validateOptionalKielistetty(kielivalinta, linkkiEPerusteisiin, s"$path.linkkiEPerusteisiin")),
    validateIfNonEmpty(linkkiEPerusteisiin, s"$path.linkkiEPerusteisiin", assertValidUrl _),
  )
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
                                                       opintojenLaajuusKoodiUri: Option[String] = None,
                                                       isMuokkaajaOphVirkailija: Option[Boolean] = None
                                                      ) extends VapaaSivistystyoKoulutusMetadata

case class AikuistenPerusopetusKoulutusMetadata(tyyppi: Koulutustyyppi = AikuistenPerusopetus,
                                                kuvaus: Kielistetty = Map(),
                                                lisatiedot: Seq[Lisatieto] = Seq(),
                                                linkkiEPerusteisiin: Kielistetty = Map(),
                                                opintojenLaajuusKoodiUri: Option[String] = None,
                                                isMuokkaajaOphVirkailija: Option[Boolean] = None
                                               ) extends KoulutusMetadata {

  override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
    super.validate(tila, kielivalinta, path),
    // Aikuisten perusopetuksella ei ole lisätiedot-kenttää lomakkeessa
    assertEmpty(lisatiedot, path),
    // OpintojenLaajuusKoodiUri on pakollinen kenttä Aikuisten perusopetukselle
    validateIfJulkaistu(tila, assertNotOptional(opintojenLaajuusKoodiUri, s"$path.opintojenLaajuusKoodiUri")),
    // Kuvaus on pakollinen kenttä Aikuisten perusopetukselle
    validateIfJulkaistu(tila, validateKielistetty(kielivalinta, kuvaus, s"$path.kuvaus")),
    validateIfJulkaistu(tila, validateOptionalKielistetty(kielivalinta, linkkiEPerusteisiin, s"$path.linkkiEPerusteisiin")),
    validateIfNonEmpty(linkkiEPerusteisiin, s"$path.linkkiEPerusteisiin", assertValidUrl _),
  )
}
