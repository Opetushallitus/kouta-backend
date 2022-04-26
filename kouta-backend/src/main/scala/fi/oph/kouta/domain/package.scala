package fi.oph.kouta

import fi.oph.kouta.domain.oid._
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.TimeUtils
import fi.oph.kouta.validation.Validations._
import fi.oph.kouta.validation.{IsValid, NoErrors, ValidatableSubEntity}

import java.time.{Instant, LocalDateTime}
import java.util.UUID

//Huom! Älä käytä enumeraatioita, koska Swagger ei tue niitä -> TODO: Voi ehkä käyttää, kun ei ole scalatra-swagger enää käytössä?!
package object domain {

  val KieliModel: String =
    """    Kieli:
      |      type: string
      |      enum:
      |        - fi
      |        - sv
      |        - en
      |""".stripMargin

  val LiitteenToimitustapaModel: String =
    """    LiitteenToimitustapa:
      |      type: string
      |      enum:
      |        - hakijapalvelu
      |        - osoite
      |        - lomake
      |""".stripMargin

  val AjanjaksoModel: String =
    """    Ajanjakso:
      |      type: object
      |      properties:
      |        alkaa:
      |           type: string
      |           format: date-time
      |           description: Ajanjakson alkuaika
      |           example: 2019-08-23T09:55
      |        paattyy:
      |           type: string
      |           format: date-time
      |           description: Ajanjakson päättymisaika
      |           example: 2019-08-23T09:55
      |""".stripMargin

  val JulkaisutilaModel: String =
    """    Julkaisutila:
      |      type: string
      |      enum:
      |        - julkaistu
      |        - tallennettu
      |        - arkistoitu
      |""".stripMargin

  val HakulomaketyyppiModel: String =
    """    Hakulomaketyyppi:
      |      type: string
      |      enum:
      |        - ataru
      |        - ei sähköistä
      |        - muu
      |""".stripMargin

  val TekstiModel: String =
    """    Teksti:
      |      type: object
      |      properties:
      |        fi:
      |          type: string
      |          example: Suomenkielinen teksti
      |          description: "Suomenkielinen teksti, jos kielivalinnassa on 'fi'"
      |        sv:
      |          type: string
      |          example: Ruotsinkielinen teksti
      |          description: "Ruotsinkielinen teksti, jos kielivalinnassa on 'sv'"
      |        en:
      |          type: string
      |          example: Englanninkielinen teksti
      |          description: "Englanninkielinen teksti, jos kielivalinnassa on 'en'"
      |""".stripMargin

  val NimiModel: String =
    """    Nimi:
      |      type: object
      |      properties:
      |        fi:
      |          type: string
      |          example: Suomenkielinen nimi
      |          description: "Suomenkielinen nimi, jos kielivalinnassa on 'fi'"
      |        sv:
      |          type: string
      |          example: Ruotsinkielinen nimi
      |          description: "Ruotsinkielinen nimi, jos kielivalinnassa on 'sv'"
      |        en:
      |          type: string
      |          example: Englanninkielinen nimi
      |          description: "Englanninkielinen nimi, jos kielivalinnassa on 'en'"
      |""".stripMargin

  val KuvausModel: String =
    """    Kuvaus:
      |      type: object
      |      properties:
      |        fi:
      |          type: string
      |          example: Suomenkielinen kuvaus
      |          description: "Suomenkielinen kuvaus, jos kielivalinnassa on 'fi'"
      |        sv:
      |          type: string
      |          example: Ruotsinkielinen kuvaus
      |          description: "Ruotsinkielinen kuvaus, jos kielivalinnassa on 'sv'"
      |        en:
      |          type: string
      |          example: Englanninkielinen kuvaus
      |          description: "Englanninkielinen kuvaus, jos kielivalinnassa on 'en'"
      |""".stripMargin

  val LinkkiModel: String =
    """    Linkki:
      |      type: object
      |      properties:
      |        fi:
      |          type: string
      |          example: Linkki suomenkieliselle sivulle
      |          description: "Linkki suomenkieliselle sivulle, jos kielivalinnassa on 'fi'"
      |        sv:
      |          type: string
      |          example: Linkki ruotsinkieliselle sivulle
      |          description: "Linkki ruotsinkieliselle sivulle, jos kielivalinnassa on 'sv'"
      |        en:
      |          type: string
      |          example: Linkki englanninkieliselle sivulle
      |          description: "Linkki englanninkieliselle sivulle, jos kielivalinnassa on 'en'"
      |""".stripMargin

  val NimettyLinkkiModel: String =
    """    NimettyLinkki:
      |      type: object
      |      properties:
      |        url:
      |          type: object
      |          description: Linkin url eri kielillä
      |          $ref: '#/components/schemas/Linkki
      |        nimi:
      |          type: object
      |          description: Käyttäjälle näkyvä teksti linkissä
      |          $ref: '#/components/schemas/Teksti
      |""".stripMargin

  val LisatietoModel: String =
    """    Lisatieto:
      |      type: object
      |      properties:
      |        otsikkoKoodiUri:
      |          type: string
      |          description: Lisätiedon otsikon koodi URI. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/koulutuksenlisatiedot/1)
      |          example: koulutuksenlisatiedot_03#1
      |        teksti:
      |          type: object
      |          description: Lisätiedon teksti eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/Teksti'
      |""".stripMargin

  val YhteyshenkiloModel: String =
    """    Yhteyshenkilo:
      |      type: object
      |      properties:
      |        nimi:
      |          type: object
      |          description: Yhteyshenkilön nimi eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/Teksti'
      |        titteli:
      |          type: object
      |          description: Yhteyshenkilön titteli eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/Teksti'
      |        sahkoposti:
      |          type: object
      |          description: Yhteyshenkilön sähköpostiosoite eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/Teksti'
      |        puhelinnumero:
      |          type: object
      |          description: Yhteyshenkilön puhelinnumero eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/Teksti'
      |        wwwSivu:
      |          type: object
      |          description: Yhteyshenkilön www-sivu eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/Teksti'
      |""".stripMargin

  val OsoiteModel: String =
    """    Osoite:
      |      type: object
      |      properties:
      |        osoite:
      |          type: object
      |          description: Osoite eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/Teksti'
      |        postinumeroKoodiUri:
      |          type: string
      |          description: Postinumero. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/posti/2)
      |          example: "posti_04230#2"
      |""".stripMargin

  val ValintakokeenLisatilaisuudetModel: String =
    """    ValintakokeenLisatilaisuudet:
      |      type: object
      |      description: Hakukohteella lisätyt valintakokeen lisätilaisuudet
      |      properties:
      |        id:
      |          type: string
      |          description: Valintakokeen yksilöivä tunniste. Järjestelmän generoima.
      |          example: "ea596a9c-5940-497e-b5b7-aded3a2352a7"
      |        tilaisuudet:
      |          type: array
      |          description: Hakukohteella syötetyt valintaperusteen valintakokeen lisäjärjestämistilaisuudet
      |          items:
      |            $ref: '#/components/schemas/Valintakoetilaisuus'
      |""".stripMargin

  val ValintakoeModel: String =
    """    Valintakoe:
      |      type: object
      |      description: Valintakokeen tiedot
      |      properties:
      |        id:
      |          type: string
      |          description: Valintakokeen yksilöivä tunniste. Järjestelmän generoima.
      |          example: "ea596a9c-5940-497e-b5b7-aded3a2352a7"
      |        tyyppiKoodiUri:
      |          type: string
      |          description: Valintakokeen tyyppi. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/valintakokeentyyppi/1)
      |          example: valintakokeentyyppi_1#1
      |        nimi:
      |          type: object
      |          description: Valintakokeen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/Nimi'
      |        metadata:
      |          type: object
      |          $ref: '#/components/schemas/ValintakoeMetadata'
      |        tilaisuudet:
      |          type: array
      |          description: Valintakokeen järjestämistilaisuudet
      |          items:
      |            $ref: '#/components/schemas/Valintakoetilaisuus'
      |""".stripMargin

  val ValintakoeMetadataModel: String =
    """    ValintakoeMetadata:
      |      type: object
      |      properties:
      |        tietoja:
      |          type: object
      |          description: Tietoa valintakokeesta
      |          $ref: '#/components/schemas/Teksti'
      |        vahimmaispisteet:
      |          type: double
      |          description: Valintakokeen vähimmäispisteet
      |          example: 10.0
      |        liittyyEnnakkovalmistautumista:
      |          type: boolean
      |          description: Liittyykö valintakokeeseen ennakkovalmistautumista
      |        ohjeetEnnakkovalmistautumiseen:
      |          type: object
      |          description: Ohjeet valintakokeen ennakkojärjestelyihin
      |          $ref: '#/components/schemas/Teksti'
      |        erityisjarjestelytMahdollisia:
      |          type: boolean
      |          description: Ovatko erityisjärjestelyt mahdollisia valintakokeessa
      |        ohjeetErityisjarjestelyihin:
      |          type: object
      |          description: Ohjeet valintakokeen erityisjärjestelyihin
      |          $ref: '#/components/schemas/Teksti'
      |""".stripMargin

  val ValintakoetilaisuusModel: String =
    """    Valintakoetilaisuus:
      |      type: object
      |      properties:
      |        osoite:
      |          type: object
      |          description: Valintakokeen järjestämispaikan osoite
      |          $ref: '#/components/schemas/Osoite'
      |        aika:
      |          type: array
      |          description: Valintakokeen järjestämisaika
      |          items:
      |            $ref: '#/components/schemas/Ajanjakso'
      |        jarjestamispaikka:
      |          type: object
      |          description: Valintakokeen järjestämispaikka eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/Teksti'
      |        lisatietoja:
      |          type: object
      |          description: Lisätietoja valintakokeesta eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/Teksti'
      |""".stripMargin

  val ListEverythingModel: String =
    """    ListEverything:
      |      type: object
      |      properties:
      |        koulutukset:
      |          type: array
      |          items:
      |            type: string
      |            example:
      |              - 1.2.246.562.13.00000000000000000009
      |              - 1.2.246.562.13.00000000000000000008
      |        toteutukset:
      |          type: array
      |          items:
      |            type: string
      |            example:
      |              - 1.2.246.562.17.00000000000000000009
      |              - 1.2.246.562.17.00000000000000000008
      |        haut:
      |          type: array
      |          items:
      |            type: string
      |            example:
      |              - 1.2.246.562.29.00000000000000000009
      |              - 1.2.246.562.29.00000000000000000008
      |        hakukohteet:
      |          type: array
      |          items:
      |            type: string
      |            example:
      |              - 1.2.246.562.20.00000000000000000009
      |              - 1.2.246.562.20.00000000000000000008
      |        valintaperusteet:
      |          type: array
      |          items:
      |            type: string
      |            example:
      |              - ea596a9c-5940-497e-b5b7-aded3a2352a7
      |              - ea596a9c-5940-497e-b5b7-aded3a2352a8
      |        oppilaitokset:
      |          type: array
      |          items:
      |            type: string
      |            example:
      |              - 1.2.246.562.10.00000000000000000009
      |              - 1.2.246.562.10.00000000000000000008
      |        sorakuvaukset:
      |          type: array
      |          items:
      |            type: string
      |            example:
      |              - 09a23d0c-3a6e-403b-b3d6-cd659ec763f8
      |              - d9afdef2-ae7a-4e78-8366-ab8f97b1fd25
      |""".stripMargin

  val AuthenticatedModel: String =
    """    Authenticated:
      |      type: object
      |      properties:
      |        id:
      |          type: string
      |          description: Session id (UUID)
      |          example: b0c9ccd3-9f56-4d20-8df4-21a1239fcf89
      |        ip:
      |          type: string
      |          description: Kutsujan IP
      |          example: 127.0.0.1
      |        userAgent:
      |          type: string
      |          description: Kutsujan user-agent
      |          example: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36"
      |        session:
      |          type: object
      |          properties:
      |            personOid:
      |              type: string
      |              description: Henkilön oid
      |              example: 1.2.246.562.10.00101010101
      |            authorities:
      |              type: array
      |              items:
      |                type: object
      |                properties:
      |                  authority:
      |                    type: string
      |                    description: Yksittäinen käyttöoikeus
      |                    example: APP_KOUTA_OPHPAAKAYTTAJA_1.2.246.562.10.00000000001
      |""".stripMargin

  val TutkinnonOsaModel: String =
    """    TutkinnonOsa:
      |      type: object
      |      properties:
      |        ePerusteId:
      |          type: number
      |          description: Tutkinnon osan käyttämän ePerusteen id.
      |          example: 4804100
      |        koulutusKoodiUri:
      |          type: string
      |          description: Koulutuksen koodi URI. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/koulutus/11)
      |          example: koulutus_371101#1
      |        tutkinnonosaId:
      |          type: number
      |          description: Tutkinnon osan id ePerusteissa
      |          example: 12345
      |        tutkinnonosaViite:
      |          type: number
      |          description: Tutkinnon osan viite
      |          example: 2449201
      |""".stripMargin

  val KoulutuksenAlkamiskausiModel: String =
    """    KoulutuksenAlkamiskausi:
      |      type: object
      |      properties:
      |        alkamiskausityyppi:
      |          type: string
      |          description: Alkamiskauden tyyppi
      |          enum:
      |            - 'henkilokohtainen suunnitelma'
      |            - 'tarkka alkamisajankohta'
      |            - 'alkamiskausi ja -vuosi'
      |        koulutuksenAlkamispaivamaara:
      |          type: string
      |          description: Koulutuksen tarkka alkamisen päivämäärä
      |          example: 2019-11-20T12:00
      |        koulutuksenPaattymispaivamaara:
      |          type: string
      |          description: Koulutuksen päättymisen päivämäärä
      |          example: 2019-11-20T12:00
      |        koulutuksenAlkamiskausiKoodiUri:
      |          type: string
      |          description: Haun koulutusten alkamiskausi. Hakukohteella voi olla eri alkamiskausi kuin haulla.
      |            Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kausi/1)
      |          example: kausi_k#1
      |        koulutuksenAlkamisvuosi:
      |          type: string
      |          description: Haun koulutusten alkamisvuosi. Hakukohteella voi olla eri alkamisvuosi kuin haulla.
      |          example: 2020
      |        henkilokohtaisenSuunnitelmanLisatiedot:
      |          type: object
      |          description: Lisätietoa koulutuksen alkamisesta henkilökohtaisen suunnitelman mukaan eri kielillä. Kielet on määritetty haun kielivalinnassa.
      |          $ref: '#/components/schemas/Teksti'
      |""".stripMargin

  val AloituspaikatModel: String =
    """    Aloituspaikat:
      |      type: object
      |      properties:
      |        lukumaara:
      |          type: integer
      |          description: Hakukohteen aloituspaikkojen lukumäärä
      |          example: 100
      |        ensikertalaisille:
      |          type: integer
      |          description: Hakukohteen ensikertalaisten aloituspaikkojen lukumäärä
      |          example: 50
      |        kuvaus:
      |          type: object
      |          description: Tarkempi kuvaus aloituspaikoista
      |          $ref: '#/components/schemas/Kuvaus'
      |""".stripMargin

  val models = List(KieliModel, JulkaisutilaModel, TekstiModel, NimiModel, KuvausModel, LinkkiModel, LisatietoModel,
    YhteyshenkiloModel, HakulomaketyyppiModel, AjanjaksoModel, OsoiteModel, ValintakoeModel, ValintakoeMetadataModel,
    ValintakoetilaisuusModel, LiitteenToimitustapaModel, ListEverythingModel, AuthenticatedModel, TutkinnonOsaModel,
    KoulutuksenAlkamiskausiModel, NimettyLinkkiModel, ValintakokeenLisatilaisuudetModel, AloituspaikatModel)

  type Kielistetty = Map[Kieli,String]

  case class Yhteyshenkilo(nimi: Kielistetty = Map(),
                           titteli: Kielistetty = Map(),
                           sahkoposti: Kielistetty = Map(),
                           puhelinnumero: Kielistetty = Map(),
                           wwwSivu: Kielistetty = Map()) extends ValidatableSubEntity {
    def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = {
      validateIfJulkaistu(tila, and(
        validateKielistetty(kielivalinta, nimi, s"$path.nimi"),
        validateOptionalKielistetty(kielivalinta, titteli, s"$path.titteli"),
        validateOptionalKielistetty(kielivalinta, sahkoposti, s"$path.sahkoposti"),
        validateOptionalKielistetty(kielivalinta, puhelinnumero, s"$path.puhelinnumero"),
        validateOptionalKielistetty(kielivalinta, wwwSivu, s"$path.wwwSivu"),
        validateIfNonEmpty(wwwSivu, s"$path.wwwSivu", assertValidUrl _)
      ))
    }
  }

  case class Ajanjakso(alkaa: LocalDateTime,
                       paattyy: Option[LocalDateTime] = None) extends ValidatableSubEntity {
    def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid =
      assertTrue(paattyy.forall(_.isAfter(alkaa)), path, invalidAjanjaksoMsg(this))

    override def validateOnJulkaisu(path: String): IsValid = {
      and(
        assertNotOptional(paattyy, s"$path.paattyy"),
        validateIfDefined[LocalDateTime](paattyy, assertInFuture(_, s"$path.paattyy")),
      )
    }

    def validateOnJulkaisuForJatkuvaHaku(path: String): IsValid =
      validateIfDefined[LocalDateTime](paattyy, assertInFuture(_, s"$path.paattyy")),
  }

  // NOTE: Tätä käyttää hakukohde lisäämään tilaisuuksia valintaperusteen valintakokeelle
  case class ValintakokeenLisatilaisuudet(id: Option[UUID] = None,
                                          tilaisuudet: Seq[Valintakoetilaisuus] = Seq()) extends ValidatableSubEntity {
    def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
      validateIfNonEmpty[Valintakoetilaisuus](tilaisuudet, s"$path.tilaisuudet", _.validate(tila, kielivalinta, _)),
    )

    override def validateOnJulkaisu(path: String): IsValid =
      validateIfNonEmpty[Valintakoetilaisuus](tilaisuudet, s"$path.tilaisuudet", _.validateOnJulkaisu(_))
  }

  case class Valintakoe(id: Option[UUID] = None,
                        tyyppiKoodiUri: Option[String] = None,
                        nimi: Kielistetty = Map(),
                        metadata: Option[ValintakoeMetadata] = None,
                        tilaisuudet: Seq[Valintakoetilaisuus] = Seq()) extends ValidatableSubEntity {
    def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
      validateIfDefined[String](tyyppiKoodiUri, assertMatch(_, ValintakokeenTyyppiKoodiPattern, s"$path.tyyppiKoodiUri")),
      validateIfNonEmpty[Valintakoetilaisuus](tilaisuudet, s"$path.tilaisuudet", _.validate(tila, kielivalinta, _)),
      validateIfDefined[ValintakoeMetadata](metadata, _.validate(tila, kielivalinta, s"$path.metadata")),
      validateIfJulkaistu(tila, and(
        validateOptionalKielistetty(kielivalinta, nimi, s"$path.nimi"),
      ))
    )

    override def validateOnJulkaisu(path: String): IsValid =
      validateIfNonEmpty[Valintakoetilaisuus](tilaisuudet, s"$path.tilaisuudet", _.validateOnJulkaisu(_))
  }

  case class ValintakoeMetadata(tietoja: Kielistetty = Map(),
                                vahimmaispisteet: Option[Double] = None,
                                liittyyEnnakkovalmistautumista: Option[Boolean] = None,
                                ohjeetEnnakkovalmistautumiseen: Kielistetty = Map(),
                                erityisjarjestelytMahdollisia: Option[Boolean] = None,
                                ohjeetErityisjarjestelyihin: Kielistetty = Map()) extends ValidatableSubEntity {
    def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
      validateIfJulkaistu(tila, and(
        validateOptionalKielistetty(kielivalinta, tietoja, s"$path.tietoja"),
        validateIfDefined[Double](vahimmaispisteet, assertNotNegative(_, s"$path.vahimmaispisteet")),
        validateIfTrue(liittyyEnnakkovalmistautumista.contains(true), validateKielistetty(kielivalinta, ohjeetEnnakkovalmistautumiseen, s"$path.ohjeetEnnakkovalmistautumiseen")),
        validateIfTrue(erityisjarjestelytMahdollisia.contains(true), validateKielistetty(kielivalinta, ohjeetErityisjarjestelyihin, s"$path.ohjeetErityisjarjestelyihin"))
      ))
    )
  }

  case class Valintakoetilaisuus(osoite: Option[Osoite],
                                 aika: Option[Ajanjakso] = None,
                                 jarjestamispaikka: Kielistetty = Map(),
                                 lisatietoja: Kielistetty = Map()) extends ValidatableSubEntity {
    def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
      validateIfDefined[Osoite](osoite, _.validate(tila, kielivalinta, s"$path.osoite")),
      validateIfDefined[Ajanjakso](aika, _.validate(tila, kielivalinta, s"$path.aika")),
      validateIfJulkaistu(tila, and(
        assertNotOptional(osoite, s"$path.osoite"),
        assertNotOptional(aika, s"$path.aika"),
        validateOptionalKielistetty(kielivalinta, jarjestamispaikka, s"$path.jarjestamispaikka"),
        validateOptionalKielistetty(kielivalinta, lisatietoja, s"$path.lisatietoja")
      ))
    )

    override def validateOnJulkaisu(path: String): IsValid =
      validateIfDefined[Ajanjakso](aika, _.validateOnJulkaisu(s"$path.aika"))
  }

  abstract class OidListItem {
    val oid: Oid
    val nimi: Kielistetty
    val tila: Julkaisutila
    val organisaatioOid: OrganisaatioOid
    val muokkaaja: UserOid
    val modified: Modified
  }

  abstract class IdListItem {
    val id: UUID
    val nimi: Kielistetty
    val tila: Julkaisutila
    val organisaatioOid: OrganisaatioOid
    val muokkaaja: UserOid
    val modified: Modified
  }

  case class Lisatieto(otsikkoKoodiUri: String, teksti: Kielistetty) extends ValidatableSubEntity {
    def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
      assertMatch(otsikkoKoodiUri, KoulutuksenLisatiedotOtsikkoKoodiPattern, s"$path.otsikkoKoodiUri"),
      validateIfJulkaistu(tila, validateKielistetty(kielivalinta, teksti, s"$path.teksti"))
    )
  }

  case class TutkinnonOsa(ePerusteId: Option[Long] = None,
                          koulutusKoodiUri: Option[String] = None,
                          tutkinnonosaId: Option[Long] = None,
                          tutkinnonosaViite: Option[Long] = None) extends ValidatableSubEntity {
    def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
      validateIfDefined(koulutusKoodiUri,
        assertMatch(_, KoulutusKoodiPattern, s"$path.koulutusKoodiUri")),
      validateIfJulkaistu(tila, and(
        assertNotOptional(ePerusteId, s"$path.ePerusteId"),
        assertNotOptional(koulutusKoodiUri, s"$path.koulutusKoodiUri"),
        assertNotOptional(tutkinnonosaId, s"$path.tutkinnonosaId"),
        assertNotOptional(tutkinnonosaViite, s"$path.tutkinnonosaViite"))))}

  case class Osoite(osoite: Kielistetty = Map(),
                    postinumeroKoodiUri: Option[String]) extends ValidatableSubEntity {
    def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
      validateIfDefined[String](postinumeroKoodiUri, assertMatch(_, PostinumeroKoodiPattern, s"$path.postinumeroKoodiUri")),
      validateIfJulkaistu(tila, and(
        validateKielistetty(kielivalinta, osoite, s"$path.osoite"),
        assertNotOptional(postinumeroKoodiUri, s"$path.postinumeroKoodiUri")
      ))
    )
  }

  case class KoulutuksenAlkamiskausi(alkamiskausityyppi: Option[Alkamiskausityyppi] = None,
                                     henkilokohtaisenSuunnitelmanLisatiedot: Kielistetty = Map(),
                                     koulutuksenAlkamispaivamaara: Option[LocalDateTime] = None,
                                     koulutuksenPaattymispaivamaara: Option[LocalDateTime] = None,
                                     koulutuksenAlkamiskausiKoodiUri: Option[String] = None,
                                     koulutuksenAlkamisvuosi: Option[String] = None) extends ValidatableSubEntity {
    override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
      validateKoulutusPaivamaarat(koulutuksenAlkamispaivamaara, koulutuksenPaattymispaivamaara, s"$path.koulutuksenAlkamispaivamaara"),
      validateIfDefined[String](koulutuksenAlkamiskausiKoodiUri, assertMatch(_, KausiKoodiPattern, s"$path.koulutuksenAlkamiskausiKoodiUri")),
      validateIfDefined[String](koulutuksenAlkamisvuosi, v => assertMatch(v, VuosiPattern, s"$path.koulutuksenAlkamisvuosi")),
      validateIfJulkaistu(tila, and(
        assertNotOptional(alkamiskausityyppi, s"$path.alkamiskausityyppi"),
        validateIfTrue(TarkkaAlkamisajankohta == alkamiskausityyppi.getOrElse({}), assertNotOptional(koulutuksenAlkamispaivamaara, s"$path.koulutuksenAlkamispaivamaara")),
        validateIfTrue(AlkamiskausiJaVuosi == alkamiskausityyppi.getOrElse({}), and(
          assertNotOptional(koulutuksenAlkamiskausiKoodiUri, s"$path.koulutuksenAlkamiskausiKoodiUri"),
          assertNotOptional(koulutuksenAlkamisvuosi, s"$path.koulutuksenAlkamisvuosi"))),
        validateOptionalKielistetty(kielivalinta, henkilokohtaisenSuunnitelmanLisatiedot, s"$path.henkilokohtaisenSuunnitelmanLisatiedot")
      )))

    override def validateOnJulkaisu(path: String): IsValid = and(
      validateIfDefined[String](koulutuksenAlkamisvuosi, v => assertAlkamisvuosiInFuture(v, s"$path.koulutuksenAlkamisvuosi")),
      validateIfDefined[LocalDateTime](koulutuksenAlkamispaivamaara, assertInFuture(_, s"$path.koulutuksenAlkamispaivamaara")),
      validateIfDefined[LocalDateTime](koulutuksenPaattymispaivamaara, assertInFuture(_, s"$path.koulutuksenPaattymispaivamaara")))
  }

  case class ListEverything(koulutukset: Seq[KoulutusOid] = Seq(),
                            toteutukset: Seq[ToteutusOid] = Seq(),
                            haut: Seq[HakuOid] = Seq(),
                            hakukohteet: Seq[HakukohdeOid] = Seq(),
                            valintaperusteet: Seq[UUID] = Seq(),
                            oppilaitokset: Seq[OrganisaatioOid] = Seq(),
                            sorakuvaukset: Seq[UUID] = Seq())

  case class NimettyLinkki(nimi: Kielistetty = Map(),
                           url: Kielistetty = Map()) extends ValidatableSubEntity {
    def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
      validateIfNonEmpty(url, s"$path.url", assertValidUrl _),
      validateIfJulkaistu(tila, and(
        validateKielistetty(kielivalinta, url, s"$path.url"),
        validateOptionalKielistetty(kielivalinta, nimi, s"$path.nimi")
      ))
    )
  }

  case class Aloituspaikat(lukumaara: Option[Int],
                           ensikertalaisille: Option[Int] = None,
                           kuvaus: Kielistetty = Map()) extends ValidatableSubEntity{
    override def validate(tila: Julkaisutila, kielivalinta: Seq[Kieli], path: String): IsValid = and(
      validateIfDefined[Int](ensikertalaisille, assertNotNegative(_, s"$path.ensikertalaisille")),
      validateIfJulkaistu(tila, and(
        assertNotOptional(lukumaara, s"$path.lukumaara"),
        validateIfDefined[Int](lukumaara, assertNotNegative(_, s"$path.lukumaara")),
        validateOptionalKielistetty(kielivalinta, kuvaus, s"$path.kuvaus"),
      ))
    )
  }

  trait HasTeemakuva[T] {
    val teemakuva: Option[String]

    def withTeemakuva(teemakuva: Option[String]): T
  }

  trait HasPrimaryId[ID, T] {
    def primaryId: Option[ID]

    def withPrimaryID(id: ID): T
  }

  trait HasModified[T] {
    def modified: Option[Modified]
    def withModified(modified: Modified): T
    def withModified(modified: Instant): T = withModified(TimeUtils.instantToModified(modified))
    def withModified(modified: LocalDateTime): T = withModified(Modified(modified))
  }

  trait ExternalRequest {
    val authenticated: Authenticated
  }
}
