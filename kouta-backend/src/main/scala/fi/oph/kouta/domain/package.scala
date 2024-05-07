package fi.oph.kouta

import fi.oph.kouta.client.TutkinnonOsaServiceItem
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.servlet.Authenticated
import fi.oph.kouta.util.TimeUtils
import fi.oph.kouta.validation.ExternalQueryResults.ExternalQueryResult
import fi.oph.kouta.validation.Validations.{assertTrue, _}
import fi.oph.kouta.validation.{
  IsValid,
  JulkaisuValidatableSubEntity,
  NoErrors,
  ValidatableSubEntity,
  ValidationContext
}
import java.time.{Instant, LocalDateTime}
import java.util.UUID

//Huom! Älä käytä enumeraatioita, koska Swagger ei tue niitä -> TODO: Voi ehkä käyttää, kun ei ole scalatra-swagger enää käytössä?!
package object domain {

  val KoulutustyyppiModel: String =
    ("""    Koulutustyyppi:
      |      type: string
      |      enum:
""" + Koulutustyyppi.valuesToSwaggerEnum() +
      "      |").stripMargin

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
      |        - poistettu
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
      |          $ref: '#/components/schemas/Linkki'
      |        nimi:
      |          type: object
      |          description: Käyttäjälle näkyvä teksti linkissä
      |          $ref: '#/components/schemas/Teksti'
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
      |          description: Yhteyshenkilön www-sivun linkki eri kielillä. Kielet on määritetty kielivalinnassa.
      |          $ref: '#/components/schemas/Teksti'
      |        wwwSivuTeksti:
      |          type: object
      |          description: Yhteyshenkilön www-sivun linkin kanssa näytettävä teksti eri kielillä. Kielet on määritetty kielivalinnassa.
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
      |          type: number
      |          format: double
      |          minimum: 0
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
      |          type: object
      |          description: Valintakokeen järjestämisaika
      |          $ref: '#/components/schemas/Ajanjakso'
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
      |          type: integer
      |          description: Tutkinnon osan käyttämän ePerusteen id.
      |          example: 4804100
      |        koulutusKoodiUri:
      |          type: string
      |          description: Koulutuksen koodi URI. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/koulutus/11)
      |          example: koulutus_371101#1
      |        tutkinnonosaId:
      |          type: integer
      |          description: Tutkinnon osan id ePerusteissa
      |          example: 12345
      |        tutkinnonosaViite:
      |          type: integer
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

  val HakutermiModel: String =
    """    Hakutermi:
      |      type: string
      |      enum:
      |        - hakeutuminen
      |        - ilmoittautuminen
      |""".stripMargin

  val CopyResultModel: String =
    """    CopyResult:
      |      type: object
      |      properties:
      |        oid:
      |          type: string
      |        status:
      |          type: string
      |        created:
      |          type: array
      |          items:
      |            type: string
      |""".stripMargin

  val PistetietoModel: String =
    """    Pistetieto:
      |      type: object
      |      properties:
      |        tarjoaja:
      |          type: string
      |        hakukohdekoodi:
      |          type: string
      |        pisteet:
      |          type: double
      |        vuosi:
      |          type: string
      |""".stripMargin

  val TilaChangeResultModel: String =
    """    TilaChangeResult:
      |      type: object
      |      properties:
      |        oid:
      |          type: string
      |        status:
      |          type: string
      |        errorPaths:
      |          type: array
      |          items:
      |            type: string
      |        errorMessages:
      |          type: array
      |          items:
      |            type: string
      |        errorTypes:
      |          type: array
      |          items:
      |            type: string
      |""".stripMargin

  val KorkeakoulutustyyppiModel: String =
    """    Korkeakoulutustyyppi:
      |      type: object
      |      properties:
      |        koulutustyyppi:
      |          type: string
      |          description: "Korkeakoulutustyyppi, sallitut arvot:
      |            'yo' (yliopisto),
      |            'amk' (ammattikorkea)"
      |          example: yo
      |        tarjoajat:
      |          type: array
      |          description: Ko. korkeakoulutustyyppiä tarjoavien organisaatioiden yksilöivät organisaatio-oidit.
      |          items:
      |            type: string
      |          example:
      |            - 1.2.246.562.10.00101010101
      |            - 1.2.246.562.10.00101010102
      |""".stripMargin

  val models = List(
    KoulutustyyppiModel,
    KieliModel,
    JulkaisutilaModel,
    TekstiModel,
    NimiModel,
    KuvausModel,
    LinkkiModel,
    LisatietoModel,
    YhteyshenkiloModel,
    HakulomaketyyppiModel,
    AjanjaksoModel,
    OsoiteModel,
    ValintakoeModel,
    ValintakoeMetadataModel,
    ValintakoetilaisuusModel,
    LiitteenToimitustapaModel,
    ListEverythingModel,
    AuthenticatedModel,
    TutkinnonOsaModel,
    KoulutuksenAlkamiskausiModel,
    NimettyLinkkiModel,
    ValintakokeenLisatilaisuudetModel,
    AloituspaikatModel,
    HakutermiModel,
    CopyResultModel,
    PistetietoModel,
    TilaChangeResultModel,
    KorkeakoulutustyyppiModel
  )

  type Kielistetty = Map[Kieli, String]

  case class Yhteyshenkilo(
      nimi: Kielistetty = Map(),
      titteli: Kielistetty = Map(),
      sahkoposti: Kielistetty = Map(),
      puhelinnumero: Kielistetty = Map(),
      wwwSivu: Kielistetty = Map(),
      wwwSivuTeksti: Kielistetty = Map()
  ) extends ValidatableSubEntity {
    def validate(vCtx: ValidationContext, path: String): IsValid = {
      validateIfJulkaistu(
        vCtx.tila,
        and(
          validateKielistetty(vCtx.kielivalinta, nimi, s"$path.nimi"),
          validateOptionalKielistetty(vCtx.kielivalinta, titteli, s"$path.titteli"),
          validateOptionalKielistetty(vCtx.kielivalinta, sahkoposti, s"$path.sahkoposti"),
          validateOptionalKielistetty(vCtx.kielivalinta, puhelinnumero, s"$path.puhelinnumero"),
          validateOptionalKielistetty(vCtx.kielivalinta, wwwSivu, s"$path.wwwSivu"),
          validateIfNonEmpty(wwwSivu, s"$path.wwwSivu", assertValidUrl _),
          validateOptionalKielistetty(vCtx.kielivalinta, wwwSivuTeksti, s"$path.wwwSivuTeksti"),
          assertKielistetytHavingSameLocales(
            (wwwSivu, s"$path.wwwSivu"),
            (wwwSivuTeksti, s"$path.wwwSivuTeksti")
          )
        )
      )
    }
  }

  case class Ajanjakso(alkaa: LocalDateTime, paattyy: Option[LocalDateTime] = None) extends ValidatableSubEntity {
    def validate(vCtx: ValidationContext, path: String): IsValid =
      assertTrue(paattyy.forall(_.isAfter(alkaa)), path, invalidAjanjaksoMsg(this))

    override def validateOnJulkaisu(path: String): IsValid = {
      and(
        assertNotOptional(paattyy, s"$path.paattyy"),
        validateIfDefined[LocalDateTime](paattyy, assertInFuture(_, s"$path.paattyy"))
      )
    }

    def validateOnJulkaisuForHakukohde(path: String): IsValid = {
      validateIfDefined[LocalDateTime](paattyy, assertInFuture(_, s"$path.paattyy"))
    }

    def validateOnJulkaisuForJatkuvaOrJoustavaHaku(path: String): IsValid =
      validateIfDefined[LocalDateTime](paattyy, assertInFuture(_, s"$path.paattyy"))
  }

  // NOTE: Tätä käyttää hakukohde lisäämään tilaisuuksia valintaperusteen valintakokeelle
  case class ValintakokeenLisatilaisuudet(id: Option[UUID] = None, tilaisuudet: Seq[Valintakoetilaisuus] = Seq())
      extends JulkaisuValidatableSubEntity {
    def validate(
        path: String,
        entityWithNewValues: Option[ValintakokeenLisatilaisuudet],
        vCtx: ValidationContext,
        osoiteKoodistoCheckFunc: String => ExternalQueryResult
    ): IsValid = and(
      validateIfNonEmptySeq[Valintakoetilaisuus](
        tilaisuudet,
        entityWithNewValues.map(_.tilaisuudet).getOrElse(Seq()),
        s"$path.tilaisuudet",
        (tilaisuus, newTilaisuus, path) => tilaisuus.validate(path, newTilaisuus, vCtx, osoiteKoodistoCheckFunc)
      )
    )

    override def validateOnJulkaisu(path: String): IsValid =
      validateIfNonEmpty[Valintakoetilaisuus](tilaisuudet, s"$path.tilaisuudet", _.validateOnJulkaisu(_))
  }

  case class Valintakoe(
      id: Option[UUID] = None,
      tyyppiKoodiUri: Option[String] = None,
      nimi: Kielistetty = Map(),
      metadata: Option[ValintakoeMetadata] = None,
      tilaisuudet: Seq[Valintakoetilaisuus] = Seq()
  ) extends JulkaisuValidatableSubEntity {
    def validate(
        path: String,
        entityWithNewValues: Option[Valintakoe],
        vCtx: ValidationContext,
        existingIds: Seq[UUID],
        koodistoCheckFunc: String => ExternalQueryResult,
        osoiteKoodistoCheckFunc: String => ExternalQueryResult
    ): IsValid =
      validateIfSuccessful(
        and(
          validateSubEntityId(id, s"$path.id", vCtx.crudOperation, existingIds, unknownValintakoeId(uuidToString(id))),
          validateIfNonEmptySeq[Valintakoetilaisuus](
            tilaisuudet,
            entityWithNewValues.map(_.tilaisuudet).getOrElse(Seq()),
            s"$path.tilaisuudet",
            (tilaisuus, newTilaisuus, path) => tilaisuus.validate(path, newTilaisuus, vCtx, osoiteKoodistoCheckFunc)
          ),
          validateIfDefined[ValintakoeMetadata](metadata, _.validate(vCtx, s"$path.metadata")),
          validateIfJulkaistu(
            vCtx.tila,
            and(
              validateOptionalKielistetty(vCtx.kielivalinta, nimi, s"$path.nimi")
            )
          )
        ),
        validateIfDefined[String](
          entityWithNewValues.flatMap(_.tyyppiKoodiUri),
          koodiUri =>
            assertKoodistoQueryResult(
              koodiUri,
              koodistoCheckFunc,
              s"$path.tyyppiKoodiUri",
              vCtx,
              invalidValintakoeTyyppiKoodiuri(koodiUri)
            )
        )
      )

    override def validateOnJulkaisu(path: String): IsValid =
      validateIfNonEmpty[Valintakoetilaisuus](tilaisuudet, s"$path.tilaisuudet", _.validateOnJulkaisu(_))

  }

  case class ValintakoeMetadata(
      tietoja: Kielistetty = Map(),
      vahimmaispisteet: Option[Double] = None,
      liittyyEnnakkovalmistautumista: Option[Boolean] = None,
      ohjeetEnnakkovalmistautumiseen: Kielistetty = Map(),
      erityisjarjestelytMahdollisia: Option[Boolean] = None,
      ohjeetErityisjarjestelyihin: Kielistetty = Map()
  ) extends ValidatableSubEntity {
    def validate(vCtx: ValidationContext, path: String): IsValid = and(
      validateIfJulkaistu(
        vCtx.tila,
        and(
          validateOptionalKielistetty(vCtx.kielivalinta, tietoja, s"$path.tietoja"),
          validateIfDefined[Double](vahimmaispisteet, assertNotNegative(_, s"$path.vahimmaispisteet")),
          validateIfTrue(
            liittyyEnnakkovalmistautumista.contains(true),
            validateKielistetty(
              vCtx.kielivalinta,
              ohjeetEnnakkovalmistautumiseen,
              s"$path.ohjeetEnnakkovalmistautumiseen"
            )
          ),
          validateIfTrue(
            erityisjarjestelytMahdollisia.contains(true),
            validateKielistetty(vCtx.kielivalinta, ohjeetErityisjarjestelyihin, s"$path.ohjeetErityisjarjestelyihin")
          )
        )
      )
    )
  }

  case class Valintakoetilaisuus(
      osoite: Option[Osoite],
      aika: Option[Ajanjakso] = None,
      jarjestamispaikka: Kielistetty = Map(),
      lisatietoja: Kielistetty = Map()
  ) extends JulkaisuValidatableSubEntity {
    def validate(
        path: String,
        entityWithNewValues: Option[Valintakoetilaisuus],
        vCtx: ValidationContext,
        osoiteKoodistoCheckFunc: String => ExternalQueryResult
    ): IsValid = and(
      validateIfDefined[Osoite](
        osoite,
        _.validate(
          s"$path.osoite",
          entityWithNewValues.flatMap(_.osoite),
          vCtx,
          osoiteKoodistoCheckFunc
        )
      ),
      validateIfDefined[Ajanjakso](aika, _.validate(vCtx, s"$path.aika")),
      validateIfJulkaistu(
        vCtx.tila,
        and(
          assertNotOptional(osoite, s"$path.osoite"),
          assertNotOptional(aika, s"$path.aika"),
          validateOptionalKielistetty(vCtx.kielivalinta, jarjestamispaikka, s"$path.jarjestamispaikka"),
          validateOptionalKielistetty(vCtx.kielivalinta, lisatietoja, s"$path.lisatietoja")
        )
      )
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

  case class Lisatieto(otsikkoKoodiUri: String, teksti: Kielistetty) {
    def validate(
        path: String,
        entityWithNewValues: Option[Lisatieto],
        vCtx: ValidationContext,
        koodistoCheckFunc: String => ExternalQueryResult
    ): IsValid = and(
      validateIfDefined[Lisatieto](
        entityWithNewValues,
        newValues => {
          val koodiUri = newValues.otsikkoKoodiUri
          assertKoodistoQueryResult(
            koodiUri,
            koodistoCheckFunc,
            path = s"$path.otsikkoKoodiUri",
            vCtx,
            invalidLisatietoOtsikkoKoodiuri(koodiUri)
          )
        }
      ),
      validateIfJulkaistu(vCtx.tila, validateKielistetty(vCtx.kielivalinta, teksti, s"$path.teksti"))
    )
  }

  case class TutkinnonOsa(
      ePerusteId: Option[Long] = None,
      koulutusKoodiUri: Option[String] = None,
      tutkinnonosaId: Option[Long] = None,
      tutkinnonosaViite: Option[Long] = None
  ) {
    def validate(
        vCtx: ValidationContext,
        path: String,
        tutkinnonOsatFromService: Map[Long, Seq[TutkinnonOsaServiceItem]]
    ): IsValid = and(
      validateIfJulkaistu(
        vCtx.tila,
        and(
          assertNotOptional(ePerusteId, s"$path.ePerusteId"),
          assertNotOptional(koulutusKoodiUri, s"$path.koulutusKoodiUri"),
          assertNotOptional(tutkinnonosaId, s"$path.tutkinnonosaId"),
          assertNotOptional(tutkinnonosaViite, s"$path.tutkinnonosaViite")
        )
      ),
      validateIfDefined[Long](
        ePerusteId,
        epId => {
          val viitteetJaIdt = tutkinnonOsatFromService(epId)
          (tutkinnonosaViite, tutkinnonosaId) match {
            case (Some(viite), Some(id)) =>
              val viiteJaId = viitteetJaIdt.find(_.viiteId == viite)
              and(
                assertTrue(
                  viiteJaId.isDefined,
                  s"$path.tutkinnonosaViite",
                  invalidTutkinnonOsaViiteForEPeruste(ePerusteId.get, viite)
                ),
                assertTrue(
                  viiteJaId.isDefined && viiteJaId.get.id == id,
                  s"$path.tutkinnonosaId",
                  invalidTutkinnonOsaIdForEPeruste(ePerusteId.get, id)
                )
              )
            case (Some(viite), None) =>
              assertTrue(
                viitteetJaIdt.find(_.viiteId == viite).isDefined,
                s"$path.tutkinnonosaViite",
                invalidTutkinnonOsaViiteForEPeruste(ePerusteId.get, viite)
              )
            case (None, Some(id)) =>
              assertTrue(
                viitteetJaIdt.find(_.id == id).isDefined,
                s"$path.tutkinnonosaId",
                invalidTutkinnonOsaIdForEPeruste(ePerusteId.get, id)
              )
            case (_, _) => NoErrors
          }
        }
      )
    )

    def idValuesPopulated(): Boolean =
      ePerusteId.isDefined && tutkinnonosaViite.isDefined && tutkinnonosaId.isDefined
  }

  case class Osoite(osoite: Kielistetty = Map(), postinumeroKoodiUri: Option[String]) {
    def validate(
        path: String,
        entityWithNewValues: Option[Osoite],
        vCtx: ValidationContext,
        koodistoCheckFunc: String => ExternalQueryResult
    ): IsValid =
      and(
        validateIfDefined[String](
          entityWithNewValues.flatMap(_.postinumeroKoodiUri),
          koodiUri =>
            assertKoodistoQueryResult(
              koodiUri,
              koodistoCheckFunc,
              s"$path.postinumeroKoodiUri",
              vCtx,
              invalidPostiosoiteKoodiUri(koodiUri)
            )
        ),
        validateIfJulkaistu(
          vCtx.tila,
          and(
            validateKielistetty(vCtx.kielivalinta, osoite, s"$path.osoite"),
            assertNotOptional(postinumeroKoodiUri, s"$path.postinumeroKoodiUri")
          )
        )
      )
  }

  case class KoulutuksenAlkamiskausi(
      alkamiskausityyppi: Option[Alkamiskausityyppi] = None,
      henkilokohtaisenSuunnitelmanLisatiedot: Kielistetty = Map(),
      koulutuksenAlkamispaivamaara: Option[LocalDateTime] = None,
      koulutuksenPaattymispaivamaara: Option[LocalDateTime] = None,
      koulutuksenAlkamiskausiKoodiUri: Option[String] = None,
      koulutuksenAlkamisvuosi: Option[String] = None
  ) extends JulkaisuValidatableSubEntity {
    def validate(
        path: String,
        entityWithNewValues: Option[KoulutuksenAlkamiskausi],
        vCtx: ValidationContext,
        koodistoCheckFunc: String => ExternalQueryResult
    ): IsValid =
      and(
        validateKoulutusPaivamaarat(
          koulutuksenAlkamispaivamaara,
          koulutuksenPaattymispaivamaara,
          s"$path.koulutuksenAlkamispaivamaara"
        ),
        validateIfDefined[String](
          entityWithNewValues.flatMap(_.koulutuksenAlkamiskausiKoodiUri),
          koodiUri =>
            assertKoodistoQueryResult(
              koodiUri,
              koodistoCheckFunc,
              s"$path.koulutuksenAlkamiskausiKoodiUri",
              vCtx,
              invalidKausiKoodiuri(koodiUri)
            )
        ),
        validateIfDefined[String](
          koulutuksenAlkamisvuosi,
          v => assertMatch(v, VuosiPattern, s"$path.koulutuksenAlkamisvuosi")
        ),
        validateIfJulkaistu(
          vCtx.tila,
          and(
            assertNotOptional(alkamiskausityyppi, s"$path.alkamiskausityyppi"),
            validateIfTrue(
              TarkkaAlkamisajankohta == alkamiskausityyppi.getOrElse({}),
              assertNotOptional(koulutuksenAlkamispaivamaara, s"$path.koulutuksenAlkamispaivamaara")
            ),
            validateIfTrue(
              AlkamiskausiJaVuosi == alkamiskausityyppi.getOrElse({}),
              and(
                assertNotOptional(koulutuksenAlkamiskausiKoodiUri, s"$path.koulutuksenAlkamiskausiKoodiUri"),
                assertNotOptional(koulutuksenAlkamisvuosi, s"$path.koulutuksenAlkamisvuosi")
              )
            ),
            validateOptionalKielistetty(
              vCtx.kielivalinta,
              henkilokohtaisenSuunnitelmanLisatiedot,
              s"$path.henkilokohtaisenSuunnitelmanLisatiedot"
            )
          )
        )
      )

    override def validateOnJulkaisu(path: String): IsValid = and(
      validateIfDefined[String](
        koulutuksenAlkamisvuosi,
        v => assertAlkamisvuosiInFuture(v, s"$path.koulutuksenAlkamisvuosi")
      ),
      validateIfDefined[LocalDateTime](
        koulutuksenAlkamispaivamaara,
        assertInFuture(_, s"$path.koulutuksenAlkamispaivamaara")
      ),
      validateIfDefined[LocalDateTime](
        koulutuksenPaattymispaivamaara,
        assertInFuture(_, s"$path.koulutuksenPaattymispaivamaara")
      )
    )

  }

  case class ListEverything(
      koulutukset: Seq[KoulutusOid] = Seq(),
      toteutukset: Seq[ToteutusOid] = Seq(),
      haut: Seq[HakuOid] = Seq(),
      hakukohteet: Seq[HakukohdeOid] = Seq(),
      valintaperusteet: Seq[UUID] = Seq(),
      oppilaitokset: Seq[OrganisaatioOid] = Seq(),
      sorakuvaukset: Seq[UUID] = Seq()
  )

  case class NimettyLinkki(nimi: Kielistetty = Map(), url: Kielistetty = Map()) extends ValidatableSubEntity {
    def validate(vCtx: ValidationContext, path: String): IsValid = and(
      validateIfNonEmpty(url, s"$path.url", assertValidUrl _),
      validateIfJulkaistu(
        vCtx.tila,
        and(
          validateKielistetty(vCtx.kielivalinta, url, s"$path.url"),
          validateOptionalKielistetty(vCtx.kielivalinta, nimi, s"$path.nimi")
        )
      )
    )
  }

  case class Aloituspaikat(lukumaara: Option[Int], ensikertalaisille: Option[Int] = None, kuvaus: Kielistetty = Map())
      extends ValidatableSubEntity {
    override def validate(vCtx: ValidationContext, path: String): IsValid = and(
      validateIfDefined[Int](ensikertalaisille, assertNotNegative(_, s"$path.ensikertalaisille")),
      validateIfJulkaistu(
        vCtx.tila,
        and(
          assertNotOptional(lukumaara, s"$path.lukumaara"),
          validateIfDefined[Int](lukumaara, assertNotNegative(_, s"$path.lukumaara")),
          validateOptionalKielistetty(vCtx.kielivalinta, kuvaus, s"$path.kuvaus")
        )
      )
    )
  }

  case class Korkeakoulutustyyppi(koulutustyyppi: Koulutustyyppi, tarjoajat: Seq[OrganisaatioOid])

  trait HasTeemakuva[T] {
    val teemakuva: Option[String]

    def withTeemakuva(teemakuva: Option[String]): T
  }

  trait HasHakutuloslistauksenKuvake[T] {
    val hakutuloslistauksenKuvake: Option[String]

    def withHakutuloslistauksenKuvake(hakutulos: Option[String]): T
  }

  trait HasPrimaryId[ID, T] {
    def primaryId: Option[ID]

    def withPrimaryID(id: ID): T
  }

  trait HasModified[T] {
    def modified: Option[Modified]
    def withModified(modified: Modified): T
    def withModified(modified: Instant): T       = withModified(TimeUtils.instantToModified(modified))
    def withModified(modified: LocalDateTime): T = withModified(Modified(modified))
  }

  trait ExternalRequest {
    val authenticated: Authenticated
  }

  // HUOM! Nämä ei ole koodiston arvoja, vaan koodiURI-etuliitteitä.
  // Kyseisiä koodiarvoja ei ole koodistossa "painotettavatoppiaineetlukiossa"!
  val oppiaineKielitasoKoodiUriEtuliitteet: Set[String] = Set(
    "a1",
    "a2",
    "b1",
    "b2",
    "b3"
  ).map(OppiaineKoodisto.toString + "_" + _)

  val oppilaitostyypitForAvoinKorkeakoulutus = List(
    "oppilaitostyyppi_41#1", //Ammattikorkeakoulut
    "oppilaitostyyppi_42#1", //Yliopistot
    "oppilaitostyyppi_43#1", //Sotilaskorkeakoulut
    "oppilaitostyyppi_45#1", //Lastentarhanopettajaopistot
    "oppilaitostyyppi_46#1", //Väliaikaiset ammattikorkeakoulut
    "oppilaitostyyppi_62#1", //Liikunnan koulutuskeskukset
    "oppilaitostyyppi_63#1", //Kansanopistot
    "oppilaitostyyppi_64#1", //Kansalaisopistot
    "oppilaitostyyppi_66#1", //Kesäyliopistot
    "oppilaitostyyppi_65#1", //Opintokeskukset
    "oppilaitostyyppi_99#1"  //Muut oppilaitokset,
  )

  val opintojenLaajuusOpintopiste  = "opintojenlaajuusyksikko_2"
  val opintojenLaajuusOsaamispiste = "opintojenlaajuusyksikko_6"
  val opintojenLaajuusViikko       = "opintojenlaajuusyksikko_8"
}
