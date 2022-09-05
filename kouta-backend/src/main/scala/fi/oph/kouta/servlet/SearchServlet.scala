package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain.oid.{HakuOid, KoulutusOid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.domain.{EnumType, _}
import fi.oph.kouta.service._
import org.scalatra.{NotFound, Ok}

import scala.util.Try

case class SearchParams(
    nimi: Option[String] = None,
    koulutustyyppi: Option[Koulutustyyppi] = None,
    muokkaaja: Option[String] = None,
    tila: Option[Julkaisutila] = None,
    julkinen: Boolean = false,
    hakutapa: Seq[String] = Seq.empty,
    koulutuksenAlkamiskausi: Seq[String] = Seq.empty,
    koulutuksenAlkamisvuosi: Seq[String] = Seq.empty,
    hakuOid: Option[HakuOid] = None,
    toteutusOid: Option[ToteutusOid] = None,
    orgWhitelist: Seq[OrganisaatioOid] = Seq.empty,
    page: Option[Int] = None,
    size: Option[Int] = None,
    lng: Kieli = Fi,
    `order-by`: Option[String] = None,
    order: Option[String] = Some("asc")
)

object SearchParams {
  def toEnum[T <: EnumType](value: Option[String], e: Enum[T]) = {
    value match {
      case Some(v: String) => Try(Some(e.withName(v))).getOrElse(None)
      case _ => None
    }
  }
  def commaSepStringValToSeq(str: Option[String]): Seq[String] = str.map(_.split(",").toSeq).getOrElse(Seq.empty)
  def apply(values: Map[String, String]): SearchParams = {
    val nimi = values.get("nimi")
    SearchParams(
      nimi = nimi,
      koulutustyyppi = toEnum[Koulutustyyppi](values.get("koulutustyyppi"), Koulutustyyppi),
      muokkaaja = values.get("muokkaaja"),
      //tila = values.get("tila").map(Julkaisutila.withName(_)),
      julkinen = values.get("julkinen").map(_.toBoolean).getOrElse(false),
      hakutapa = commaSepStringValToSeq(values.get("hakutapa")),
      koulutuksenAlkamiskausi = commaSepStringValToSeq(values.get("koulutuksenAlkamiskausi")),
      koulutuksenAlkamisvuosi = commaSepStringValToSeq(values.get("koulutuksenAlkamisvuosi")),
      hakuOid = values.get("hakuOid").map(HakuOid(_)),
      toteutusOid = values.get("toteutusOid").map(ToteutusOid(_)),
      orgWhitelist = commaSepStringValToSeq(values.get("orgWhitelist")).map(OrganisaatioOid(_)),
      page = values.get("page").map(_.toInt),
      size = values.get("size").map(_.toInt),
      lng = values.get("lng").map(Kieli.withName(_)).getOrElse(Fi),
      `order-by` = values.get("order-by"),
      order = values.get("order")
    )
  }
}

class SearchServlet(
    koulutusService: KoulutusService,
    toteutusService: ToteutusService,
    hakuService: HakuService,
    hakukohdeService: HakukohdeService,
    valintaperusteService: ValintaperusteService
) extends KoutaServlet {

  def this() = this(KoulutusService, ToteutusService, HakuService, HakukohdeService, ValintaperusteService)

  val searchParams =
    """        - in: query
      |          name: organisaatioOid
      |          schema:
      |            type: string
      |          required: true
      |          description: Organisaation oid
      |          example: 1.2.246.562.10.00101010101
      |        - in: query
      |          name: nimi
      |          schema:
      |            type: string
      |          required: false
      |          description: Suodata annetulla nimellä tai oidilla
      |          example: Jalkaterapeutti
      |        - in: query
      |          name: muokkaaja
      |          schema:
      |            type: string
      |          required: false
      |          description: Suodata muokkaajan nimellä
      |          example: Maija
      |        - in: query
      |          name: tila
      |          schema:
      |            type: array
      |            items:
      |              type: string
      |          required: false
      |          description: Suodata pilkulla erotetuilla tiloilla (julkaistu/tallennettu/arkistoitu/poistettu)
      |          example: Julkaistu
      |        - in: query
      |          name: julkinen
      |          schema:
      |            type: boolean
      |          required: false
      |          description: Suodata entiteetin näkyvyydellä
      |        - in: query
      |          name: page
      |          schema:
      |            type: integer
      |          required: false
      |          description: Sivunumero
      |          example: 2
      |        - in: query
      |          name: hakutapa
      |          schema:
      |            type: array
      |            items:
      |              type: string
      |          required: false
      |          description: Suodata pilkulla erotetuilla hakutapakoodiureilla
      |          example: hakutapa_03#1
      |        - in: query
      |          name: koulutuksenAlkamiskausi
      |          schema:
      |            type: array
      |            items:
      |              type: string
      |          required: false
      |          description: Suodata koulutuksen alkamiskausikoodiureilla
      |          example: kausi_s#1
      |        - in: query
      |          name: koulutuksenAlkamisvuosi
      |          schema:
      |            type: string
      |          required: false
      |          description: Suodata pilkulla erotetuilla vuosilla
      |          example: 2022
      |        - in: query
      |          name: hakuOid
      |          schema:
      |            type: string
      |          required: false
      |          description: Suodata haun oidilla
      |          example: 1.2.246.562.29.00000000000000002128
      |        - in: query
      |          name: toteutusOid
      |          schema:
      |            type: string
      |          required: false
      |          description: Suodata haun oidilla
      |          example: 1.2.246.562.17.00000000000000001116
      |        - in: query
      |          name: size
      |          schema:
      |            type: integer
      |          required: false
      |          description: Sivun koko
      |          example: 20
      |        - in: query
      |          name: lng
      |          schema:
      |            type: string
      |          required: false
      |          description: Haun kieli (fi/sv/en)
      |          example: fi
      |        - in: query
      |          name: order-by
      |          schema:
      |            type: string
      |          required: false
      |          description: Kenttä, jonka perusteella hakutulos järjestetään (nimi/tila/muokkaaja/modified)
      |          example: fi
      |        - in: query
      |          name: order
      |          schema:
      |            type: string
      |          required: false
      |          description: Hakutuloksen järjestys (asc/desc)
      |          example: fi
      |""".stripMargin

  registerPath(
    "/search/koulutukset",
    s"""    get:
       |      summary: Hakee organisaation koulutuksia annetuilla parametreilla
       |      operationId: searchKoulutukset
       |      description: Hakee organisaation koulutukset annetuilla parametreilla
       |      tags:
       |        - Search
       |      parameters:
       |        - in: query
       |          name: koulutustyyppi
       |          schema:
       |            type: string
       |          required: false
       |          description: Suodata koulutustyypillä
       |          example: yo
       |$searchParams
       |      responses:
       |        '200':
       |          description: Ok
       |          content:
       |            application/json:
       |              schema:
       |                type: array
       |                items:
       |                  $$ref: '#/components/schemas/KoulutusSearchResult'
       |""".stripMargin
  )
  get("/koulutukset") {

    implicit val authenticated: Authenticated = authenticate()

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) =>
        Ok(koulutusService.search(organisaatioOid, SearchParams(params.toMap)))
    }
  }

  registerPath(
    "/search/koulutus/{oid}",
    s"""    get:
       |      summary: Hakee rikastetun koulutuksen annetulla oidilla
       |      operationId: searchKoulutusByOid
       |      description: Hakee rikastetun koulutuksen annetulla oidilla
       |      tags:
       |        - Search
       |      parameters:
       |        - in: path
       |          name: oid
       |          schema:
       |            type: string
       |          required: true
       |          description: Koulutuksen oid
       |        - in: query
       |          name: koulutustyyppi
       |          schema:
       |            type: string
       |          required: false
       |          description: Suodata koulutustyypillä
       |          example: yo
       |$searchParams
       |      responses:
       |        '200':
       |          description: Ok
       |          content:
       |            application/json:
       |              schema:
       |                type: object
       |                $$ref: '#/components/schemas/KoulutusSearchItemWithToteutukset'
       |""".stripMargin
  )
  get("/koulutus/:oid") {

    implicit val authenticated: Authenticated = authenticate()

    (KoulutusOid(params("oid")), params.get("organisaatioOid").map(OrganisaatioOid)) match {
      case (koulutusOid, Some(organisaatioOid)) =>
        Ok(koulutusService.search(organisaatioOid, koulutusOid, SearchParams(params.toMap)))
      case _ => NotFound()
    }
  }

  registerPath(
    "/search/toteutukset",
    s"""    get:
       |      summary: Hakee organisaation toteutuksia annetuilla parametreilla
       |      operationId: searchToteutukset
       |      description: Hakee organisaation toteutukset annetuilla parametreilla
       |      tags:
       |        - Search
       |      parameters:
       |        - in: query
       |          name: koulutustyyppi
       |          schema:
       |            type: string
       |          required: false
       |          description: Suodata koulutustyypillä
       |          example: yo
       |$searchParams
       |      responses:
       |        '200':
       |          description: Ok
       |          content:
       |            application/json:
       |              schema:
       |                type: array
       |                items:
       |                  $$ref: '#/components/schemas/ToteutusSearchResult'
       |""".stripMargin
  )
  get("/toteutukset") {

    implicit val authenticated: Authenticated = authenticate()

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) =>
        Ok(toteutusService.search(organisaatioOid, params.toMap))
    }
  }

  registerPath(
    "/search/toteutus/{oid}",
    s"""    get:
       |      summary: Hakee rikastetun toteutuksen annetulla oidilla
       |      operationId: searchToteutusByOid
       |      description: Hakee rikastetun toteutuksen annetulla oidilla
       |      tags:
       |        - Search
       |      parameters:
       |        - in: path
       |          name: oid
       |          schema:
       |            type: string
       |          required: true
       |          description: Toteutuksen oid
       |        - in: query
       |          name: koulutustyyppi
       |          schema:
       |            type: string
       |          required: false
       |          description: Suodata koulutustyypillä
       |          example: yo
       |$searchParams
       |      responses:
       |        '200':
       |          description: Ok
       |          content:
       |            application/json:
       |              schema:
       |                type: object
       |                $$ref: '#/components/schemas/ToteutusSearchItemWithHakukohteet'
       |""".stripMargin
  )
  get("/toteutus/:oid") {

    implicit val authenticated: Authenticated = authenticate()

    (ToteutusOid(params("oid")), params.get("organisaatioOid").map(OrganisaatioOid)) match {
      case (toteutusOid, Some(organisaatioOid)) =>
        Ok(toteutusService.search(organisaatioOid, toteutusOid, params.toMap))
      case _ => NotFound()
    }
  }

  registerPath(
    "/search/haut",
    s"""    get:
       |      summary: Hakee organisaation hakuja annetuilla parametreilla
       |      operationId: searchHaut
       |      description: Hakee organisaation haut annetuilla parametreilla
       |      tags:
       |        - Search
       |      parameters:
       |$searchParams
       |      responses:
       |        '200':
       |          description: Ok
       |          content:
       |            application/json:
       |              schema:
       |                type: array
       |                items:
       |                  $$ref: '#/components/schemas/HakuSearchResult'
       |""".stripMargin
  )
  get("/haut") {

    implicit val authenticated: Authenticated = authenticate()

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) =>
        Ok(hakuService.search(organisaatioOid, params.toMap))
    }
  }

  registerPath(
    "/search/haku/{oid}",
    s"""    get:
       |      summary: Hakee rikastetun haun annetulla oidilla
       |      operationId: searchHakuByOid
       |      description: Hakee rikastetun haun annetulla oidilla
       |      tags:
       |        - Search
       |      parameters:
       |        - in: path
       |          name: oid
       |          schema:
       |            type: string
       |          required: true
       |          description: Haun oid
       |$searchParams
       |      responses:
       |        '200':
       |          description: Ok
       |          content:
       |            application/json:
       |              schema:
       |                type: object
       |                $$ref: '#/components/schemas/HakuSearchItemWithHakukohteet'
       |""".stripMargin
  )
  get("/haku/:oid") {

    implicit val authenticated: Authenticated = authenticate()

    (HakuOid(params("oid")), params.get("organisaatioOid").map(OrganisaatioOid)) match {
      case (hakuOid, Some(organisaatioOid)) =>
        Ok(hakuService.search(organisaatioOid, hakuOid, params.toMap))
      case _ => NotFound()
    }
  }

  registerPath(
    "/search/hakukohteet",
    s"""    get:
       |      summary: Hakee organisaation hakukohteita annetuilla parametreilla
       |      operationId: searchHakukohteet
       |      description: Hakee organisaation hakukohteet annetuilla parametreilla
       |      tags:
       |        - Search
       |      parameters:
       |        - in: query
       |          name: koulutustyyppi
       |          schema:
       |            type: string
       |          required: false
       |          description: Suodata koulutustyypillä
       |          example: yo
       |        - in: query
       |          name: orgWhitelist
       |          schema:
       |            type: string
       |          required: false
       |          description: Rajaa palautuvia hakukohteita organisaation mukaan. Pilkulla erotettuja organisaatio-oideja.
       |$searchParams
       |      responses:
       |        '200':
       |          description: Ok
       |          content:
       |            application/json:
       |              schema:
       |                type: array
       |                items:
       |                  $$ref: '#/components/schemas/HakukohdeSearchResult'
       |""".stripMargin
  )
  get("/hakukohteet") {

    implicit val authenticated: Authenticated = authenticate()

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) =>
        Ok(hakukohdeService.search(organisaatioOid, params.toMap))
    }
  }

  registerPath(
    "/search/valintaperusteet",
    s"""    get:
       |      summary: Hakee organisaation valintaperustekuvauksia annetuilla parametreilla
       |      operationId: searchValintaperusteet
       |      description: Hakee organisaation valintaperustekuvaukset annetuilla parametreilla
       |      tags:
       |        - Search
       |      parameters:
       |        - in: query
       |          name: koulutustyyppi
       |          schema:
       |            type: string
       |          required: false
       |          description: Suodata koulutustyypillä
       |          example: yo
       |$searchParams
       |      responses:
       |        '200':
       |          description: Ok
       |          content:
       |            application/json:
       |              schema:
       |                type: array
       |                items:
       |                  $$ref: '#/components/schemas/ValintaperusteSearchResult'
       |""".stripMargin
  )
  get("/valintaperusteet") {

    implicit val authenticated: Authenticated = authenticate()

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) =>
        Ok(valintaperusteService.search(organisaatioOid, params.toMap))
    }
  }
}
