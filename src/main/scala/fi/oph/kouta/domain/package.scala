package fi.oph.kouta

import java.time.{Instant, LocalDateTime}
import java.util.UUID

import fi.oph.kouta.domain.oid._

//Huom! Älä käytä enumeraatioita, koska Swagger ei tue niitä -> TODO: Voi ehkä käyttää, kun ei ole scalatra-swagger enää käytössä?!
package object domain {

  val KieliModel =
    s"""    Kieli:
       |      type: string
       |      enum:
       |        - fi
       |        - sv
       |        - en
       |""".stripMargin

  val LiitteenToimitustapaModel=
    s"""    LiitteenToimitustapa:
       |      type: string
       |      enum:
       |        - hakijapalvelu
       |        - osoite
       |        - lomake
       |""".stripMargin

  val AjanjaksoModel =
    s"""    Ajanjakso:
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

  val JulkaisutilaModel =
    s"""    Julkaisutila:
       |      type: string
       |      enum:
       |        - julkaistu
       |        - tallennettu
       |        - arkistoitu
       |""".stripMargin

  val HakulomaketyyppiModel =
    s"""    Hakulomaketyyppi:
       |      type: string
       |      enum:
       |        - ataru
       |        - ei sähköistä
       |        - muu
       |""".stripMargin

  val TekstiModel =
    s"""    Teksti:
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

  val NimiModel =
    s"""    Nimi:
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

  val KuvausModel =
    s"""    Kuvaus:
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

  val LinkkiModel =
    s"""    Linkki:
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

  val LisatietoModel =
    s"""    Lisatieto:
       |      type: object
       |      properties:
       |        otsikkoKoodiUri:
       |          type: string
       |          description: Lisätiedon otsikon koodi URI. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/koulutuksenlisatiedot/1)
       |          example: koulutuksenlisatiedot_03#1
       |        teksti:
       |          type: object
       |          description: Lisätiedon teksti eri kielillä. Kielet on määritetty kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Teksti'
       |""".stripMargin

  val YhteyshenkiloModel =
    s"""    Yhteyshenkilo:
       |      type: object
       |      properties:
       |        nimi:
       |          type: object
       |          description: Yhteyshenkilön nimi eri kielillä. Kielet on määritetty kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Teksti'
       |        titteli:
       |          type: object
       |          description: Yhteyshenkilön titteli eri kielillä. Kielet on määritetty kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Teksti'
       |        sahkoposti:
       |          type: object
       |          description: Yhteyshenkilön sähköpostiosoite eri kielillä. Kielet on määritetty kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Teksti'
       |        puhelinnumero:
       |          type: object
       |          description: Yhteyshenkilön puhelinnumero eri kielillä. Kielet on määritetty kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Teksti'
       |        wwwSivu:
       |          type: object
       |          description: Yhteyshenkilön www-sivu eri kielillä. Kielet on määritetty kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Teksti'
       |""".stripMargin

  val OsoiteModel =
    s"""    Osoite:
       |      type: object
       |      properties:
       |        osoite:
       |          type: object
       |          description: Osoite eri kielillä. Kielet on määritetty kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Teksti'
       |        postinumeroKoodiUri:
       |          type: string
       |          description: Postinumero. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/posti/2)
       |          example: "posti_04230#2"
       |""".stripMargin

  val ValintakoeModel =
    s"""    Valintakoe:
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
       |        tilaisuudet:
       |          type: array
       |          description: Valintakokeen järjestämistilaisuudet
       |          items:
       |            $$ref: '#/components/schemas/Valintakoetilaisuus'
       |""".stripMargin

  val ValintakoetilaisuusModel =
    s"""    Valintakoetilaisuus:
       |      type: object
       |      properties:
       |        osoite:
       |          type: object
       |          description: Valintakokeen järjestämispaikan osoite
       |          allOf:
       |            - $$ref: '#/components/schemas/Osoite'
       |        aika:
       |          type: array
       |          description: Valintakokeen järjestämisaika
       |          items:
       |            $$ref: '#/components/schemas/Ajanjakso'
       |        lisatietoja:
       |          type: object
       |          description: Lisätietoja valintakokeesta eri kielillä. Kielet on määritetty kielivalinnassa.
       |          allOf:
       |            - $$ref: '#/components/schemas/Teksti'
       |""".stripMargin

  val ListEverythingModel =
    s"""    ListEverything:
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
       |""".stripMargin


  val models = List(KieliModel, JulkaisutilaModel, TekstiModel, NimiModel, KuvausModel, LinkkiModel,
    LisatietoModel, YhteyshenkiloModel, HakulomaketyyppiModel, AjanjaksoModel, OsoiteModel, ValintakoeModel,
    ValintakoetilaisuusModel, LiitteenToimitustapaModel, ListEverythingModel)

  type Kielistetty = Map[Kieli,String]

  trait EnumType {
    def name:String
    override def toString = name
  }

  trait Enum[T <: EnumType] {
    def name:String
    def values():List[T]
    def withName(n:String):T = values().find(_.name.equals(n))
      .getOrElse(throw new IllegalArgumentException(s"Unknown ${name} '${n}'"))
  }

  sealed trait Julkaisutila extends EnumType

  object Julkaisutila extends Enum[Julkaisutila] {
    override def name: String = "julkaisutila"
    def values() = List(Tallennettu, Julkaistu, Arkistoitu)
  }
  case object Tallennettu extends Julkaisutila { val name = "tallennettu" }
  case object Julkaistu extends Julkaisutila { val name = "julkaistu" }
  case object Arkistoitu extends Julkaisutila { val name = "arkistoitu" }

  sealed trait Kieli extends EnumType

  object Kieli extends Enum[Kieli] {
    override def name: String = "kieli"
    def values = List(Fi, Sv, En)
  }
  case object Fi extends Kieli { val name = "fi" }
  case object Sv extends Kieli { val name = "sv" }
  case object En extends Kieli { val name = "en" }

  sealed trait Koulutustyyppi extends EnumType

  object Koulutustyyppi extends Enum[Koulutustyyppi] {
    override def name: String = "koulutustyyppi"
    def values() = List(Amm, Lk, Muu, Yo, Amk)
  }
  case object Amm extends Koulutustyyppi { val name = "amm" }
  case object Lk extends Koulutustyyppi { val name = "lk" }
  case object Muu extends Koulutustyyppi { val name = "muu" }
  case object Yo extends Koulutustyyppi { val name = "yo" }
  case object Amk extends Koulutustyyppi { val name = "amk" }

  sealed trait Hakulomaketyyppi extends EnumType
  object Hakulomaketyyppi extends Enum[Hakulomaketyyppi] {
    override def name: String = "hakulomaketyyppi"
    def values() = List(Ataru, HakuApp, MuuHakulomake, EiSähköistä)
  }
  case object Ataru extends Hakulomaketyyppi { val name = "ataru"}
  case object HakuApp extends Hakulomaketyyppi { val name = "haku-app"}
  case object MuuHakulomake extends Hakulomaketyyppi { val name = "muu"}
  case object EiSähköistä extends Hakulomaketyyppi { val name = "ei sähköistä"}

  sealed trait LiitteenToimitustapa extends EnumType
  object LiitteenToimitustapa extends Enum[LiitteenToimitustapa] {
    override def name: String = "liitteen toimitusosoite"
    def values() = List(Lomake, Hakijapalvelu, MuuOsoite)
  }
  case object Lomake extends LiitteenToimitustapa { val name = "lomake"}
  case object Hakijapalvelu extends LiitteenToimitustapa { val name = "hakijapalvelu"}
  case object MuuOsoite extends LiitteenToimitustapa { val name = "osoite"}

  case class Yhteyshenkilo(nimi: Kielistetty = Map(),
                           titteli: Kielistetty = Map(),
                           sahkoposti: Kielistetty = Map(),
                           puhelinnumero: Kielistetty = Map(),
                           wwwSivu: Kielistetty = Map())

  case class Ajanjakso(alkaa:LocalDateTime, paattyy:LocalDateTime)

  case class Valintakoe(id: Option[UUID] = None,
                        tyyppiKoodiUri: Option[String] = None,
                        tilaisuudet: List[Valintakoetilaisuus] = List())

  case class Valintakoetilaisuus(osoite: Option[Osoite],
                                 aika: Option[Ajanjakso] = None,
                                 lisatietoja: Kielistetty = Map())

  abstract class OidListItem {
    val oid: Oid
    val nimi: Kielistetty
    val tila: Julkaisutila
    val organisaatioOid: OrganisaatioOid
    val muokkaaja: UserOid
    val modified: LocalDateTime
  }

  abstract class IdListItem {
    val id: UUID
    val nimi: Kielistetty
    val tila: Julkaisutila
    val organisaatioOid: OrganisaatioOid
    val muokkaaja: UserOid
    val modified: LocalDateTime
  }

  case class Lisatieto(otsikkoKoodiUri: String, teksti: Kielistetty)

  case class Osoite(osoite: Kielistetty = Map(),
                    postinumeroKoodiUri: Option[String])

  case class ListEverything(koulutukset: Seq[KoulutusOid] = Seq(),
                            toteutukset: Seq[ToteutusOid] = Seq(),
                            haut: Seq[HakuOid] = Seq(),
                            hakukohteet: Seq[HakukohdeOid] = Seq(),
                            valintaperusteet: Seq[UUID] = Seq())
}