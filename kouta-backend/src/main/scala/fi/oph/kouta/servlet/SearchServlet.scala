package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain.oid.{HakuOid, KoulutusOid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.domain.{EnumType, _}
import fi.oph.kouta.service._
import org.scalatra.{NotFound, Ok}

import scala.util.Try

case class SearchParams(
    nimi: Option[String] = None,
    hakuNimi: Option[String] = None,
    koulutustyyppi: Seq[Koulutustyyppi] = Seq.empty,
    muokkaaja: Option[String] = None,
    tila: Seq[Julkaisutila] = Seq.empty,
    julkinen: Option[Boolean] = None,
    hakutapa: Seq[String] = Seq.empty,
    koulutuksenAlkamiskausi: Seq[String] = Seq.empty,
    koulutuksenAlkamisvuosi: Seq[String] = Seq.empty,
    hakuOid: Option[HakuOid] = None,
    toteutusOid: Option[ToteutusOid] = None,
    koulutusOid: Option[KoulutusOid] = None,
    orgWhitelist: Seq[OrganisaatioOid] = Seq.empty,
    page: Int = 1,
    size: Int = 10,
    lng: Kieli = Fi,
    orderBy: Option[String] = None,
    order: String = "asc"
)

object SearchParams {
  def toEnum[T <: EnumType](value: Option[String], e: Enum[T]) = {
    value match {
      case Some(v: String) => Try(Some(e.withName(v))).getOrElse(None)
      case _ => None
    }
  }
  def commaSepStringValToSeq(str: Option[String]): Seq[String] = str.map(_.split(",").toSeq).getOrElse(Seq.empty)
  def apply(v: Map[String, String]): SearchParams = {
    val values = v.filter(_._2.nonEmpty) // Suodatetaan pois tyhjät parametrit
    val nimi = values.get("nimi")
    val hakuNimi = values.get("hakuNimi")
    SearchParams(
      nimi = nimi,
      hakuNimi = hakuNimi,
      koulutustyyppi = commaSepStringValToSeq(values.get("koulutustyyppi")).flatMap(s => toEnum[Koulutustyyppi](Some(s), Koulutustyyppi)),
      muokkaaja = values.get("muokkaaja"),
      tila = commaSepStringValToSeq(values.get("tila")).flatMap(s => toEnum[Julkaisutila](Some(s), Julkaisutila)),
      julkinen = values.get("julkinen").map(_.toBoolean),
      hakutapa = commaSepStringValToSeq(values.get("hakutapa")),
      koulutuksenAlkamiskausi = commaSepStringValToSeq(values.get("koulutuksenAlkamiskausi")),
      koulutuksenAlkamisvuosi = commaSepStringValToSeq(values.get("koulutuksenAlkamisvuosi")),
      hakuOid = values.get("hakuOid").map(HakuOid(_)),
      toteutusOid = values.get("toteutusOid").map(ToteutusOid(_)),
      koulutusOid = values.get("koulutusOid").map(KoulutusOid(_)),
      orgWhitelist = commaSepStringValToSeq(values.get("orgWhitelist")).map(OrganisaatioOid(_)),
      page = values.get("page").map(_.toInt).getOrElse(1),
      size = values.get("size").map(_.toInt).getOrElse(10),
      lng = values.get("lng").map(Kieli.withName(_)).getOrElse(Fi),
      orderBy = values.get("order-by"),
      order = values.getOrElse("order", "asc")
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

  val koulutustyyppiParamModel =
    """        - in: query
      |          name: koulutustyyppi
      |          style: form
      |          explode: false
      |          schema:
      |            type: array
      |            items:
      |              $ref: '#/components/schemas/Koulutustyyppi'
      |          required: false
      |          description: Suodata pilkulla erotetuilla koulutustyypeillä""".stripMargin

  def searchParamsModel(hasKoulutustyyppi: Boolean = false) =
    s"""${if (hasKoulutustyyppi) koulutustyyppiParamModel else ""}
      |        - in: query
      |          name: organisaatioOid
      |          schema:
      |            type: string
      |          required: true
      |          description: Organisaation oid
      |        - in: query
      |          name: nimi
      |          schema:
      |            type: string
      |          required: false
      |          description: Suodata annetulla nimellä tai oidilla
      |        - in: query
      |          name: muokkaaja
      |          schema:
      |            type: string
      |          required: false
      |          description: Suodata muokkaajan nimellä tai oidilla
      |        - in: query
      |          name: tila
      |          style: form
      |          explode: false
      |          schema:
      |            type: array
      |            items:
      |              type: string
      |          required: false
      |          description: Suodata pilkulla erotetuilla tiloilla (julkaistu/tallennettu/arkistoitu/poistettu)
      |        - in: query
      |          name: julkinen
      |          schema:
      |            type: boolean
      |          required: false
      |          description: Suodata entiteetin näkyvyydellä
      |        - in: query
      |          name: hakutapa
      |          style: form
      |          explode: false
      |          schema:
      |            type: array
      |            items:
      |              type: string
      |          required: false
      |          description: Suodata pilkulla erotetuilla hakutapakoodiureilla
      |        - in: query
      |          name: koulutuksenAlkamiskausi
      |          style: form
      |          explode: false
      |          schema:
      |            type: array
      |            items:
      |              type: string
      |          required: false
      |          description: Suodata koulutuksen alkamiskausi-koodiureilla
      |        - in: query
      |          name: koulutuksenAlkamisvuosi
      |          style: form
      |          explode: false
      |          schema:
      |            type: array
      |            items:
      |              type: integer
      |          required: false
      |          description: Suodata pilkulla erotetuilla vuosilla
      |        - in: query
      |          name: hakuOid
      |          schema:
      |            type: string
      |          required: false
      |          description: Suodata haun oidilla
      |        - in: query
      |          name: toteutusOid
      |          schema:
      |            type: string
      |          required: false
      |          description: Suodata toteutuksen oidilla
      |        - in: query
      |          name: koulutusOid
      |          schema:
      |            type: string
      |          required: false
      |          description: Suodata koulutuksen oidilla
      |        - in: query
      |          name: page
      |          schema:
      |            type: integer
      |          required: false
      |          description: Sivunumero
      |          example: 1
      |        - in: query
      |          name: size
      |          schema:
      |            type: integer
      |          required: false
      |          description: Sivun koko
      |          example: 10
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
      |          example: nimi
      |        - in: query
      |          name: order
      |          schema:
      |            type: string
      |            enum:
      |              - asc
      |              - desc
      |            default: asc
      |          required: false
      |          description: Hakutuloksen järjestys (asc/desc)
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
       |${searchParamsModel(hasKoulutustyyppi = true)}
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
    "/search/toteutukset",
    s"""    get:
       |      summary: Hakee organisaation toteutuksia annetuilla parametreilla
       |      operationId: searchToteutukset
       |      description: Hakee organisaation toteutukset annetuilla parametreilla
       |      tags:
       |        - Search
       |      parameters:
       |${searchParamsModel(hasKoulutustyyppi = true)}
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
        Ok(toteutusService.search(organisaatioOid, SearchParams(params.toMap)))
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
       |${searchParamsModel()}
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
        Ok(hakuService.search(organisaatioOid, SearchParams(params.toMap)))
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
       |${searchParamsModel(hasKoulutustyyppi = true)}
       |        - in: query
       |          name: hakuNimi
       |          schema:
       |            type: string
       |          required: false
       |          description: Suodata annetulla haun nimellä tai oidilla
       |        - in: query
       |          name: orgWhitelist
       |          style: form
       |          explode: false
       |          schema:
       |            type: array
       |            items:
       |              type: string
       |          required: false
       |          description: Rajaa palautuvia hakukohteita organisaation mukaan. Pilkulla erotettuja organisaatio-oideja.
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
        Ok(hakukohdeService.search(organisaatioOid, SearchParams(params.toMap)))
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
       |${searchParamsModel(hasKoulutustyyppi = true)}
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
        Ok(valintaperusteService.search(organisaatioOid, SearchParams(params.toMap)))
    }
  }
}
