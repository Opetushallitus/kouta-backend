package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain.oid.{HakuOid, KoulutusOid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.service.{HakuService, HakukohdeService, KoulutusService, ToteutusService, ValintaperusteService}
import org.scalatra.{NotFound, Ok}

class SearchServlet(koulutusService: KoulutusService,
                    toteutusService: ToteutusService,
                    hakuService: HakuService,
                    hakukohdeService: HakukohdeService,
                    valintaperusteService: ValintaperusteService) extends KoutaServlet {

  def this() = this(KoulutusService, ToteutusService, HakuService, HakukohdeService, ValintaperusteService)

  val SearchParams = Seq("nimi", "koulutustyyppi", "muokkaaja", "tila", "julkinen", "page", "size", "lng", "order-by", "order")

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
      |          name: page
      |          schema:
      |            type: integer
      |          required: false
      |          description: Sivunumero
      |          example: 2
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

  registerPath("/search/koulutukset",
    s"""    get:
       |      summary: Hakee organisaation koulutuksia annetuilla parametreilla
       |      operationId: Koulutusten haku
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
       |""".stripMargin)
  get("/koulutukset") {

    implicit val authenticated: Authenticated = authenticate()

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) => Ok(koulutusService.search(organisaatioOid, params.toMap.filterKeys(SearchParams.contains)))
    }
  }

  registerPath("/search/koulutus/{oid}",
    s"""    get:
       |      summary: Hakee rikastetun koulutuksen annetulla oidilla
       |      operationId: Koulutuksen haku
       |      description: Hakee rikastetun koulutuksen annetulla oidilla
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
       |                type: object
       |                $$ref: '#/components/schemas/KoulutusSearchItemWithToteutukset'
       |""".stripMargin)
  get("/koulutus/:oid") {

    implicit val authenticated: Authenticated = authenticate()

    (KoulutusOid(params("oid")), params.get("organisaatioOid").map(OrganisaatioOid)) match {
      case (koulutusOid, Some(organisaatioOid)) => Ok(koulutusService.search(organisaatioOid, koulutusOid, params.toMap.filterKeys(SearchParams.contains)))
      case _ => NotFound()
    }
  }

  registerPath("/search/toteutukset",
    s"""    get:
       |      summary: Hakee organisaation toteutuksia annetuilla parametreilla
       |      operationId: Toteutusten haku
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
       |""".stripMargin)
  get("/toteutukset") {

    implicit val authenticated: Authenticated = authenticate()

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) => Ok(toteutusService.search(organisaatioOid, params.toMap.filterKeys(SearchParams.contains)))
    }
  }

  registerPath("/search/toteutus/{oid}",
    s"""    get:
       |      summary: Hakee rikastetun toteutuksen annetulla oidilla
       |      operationId: Toteutuksen haku
       |      description: Hakee rikastetun toteutuksen annetulla oidilla
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
       |                type: object
       |                $$ref: '#/components/schemas/ToteutusSearchItemWithHakukohteet'
       |""".stripMargin)
  get("/toteutus/:oid") {

    implicit val authenticated: Authenticated = authenticate()

    (ToteutusOid(params("oid")), params.get("organisaatioOid").map(OrganisaatioOid)) match {
      case (toteutusOid, Some(organisaatioOid)) => Ok(toteutusService.search(organisaatioOid, toteutusOid, params.toMap.filterKeys(SearchParams.contains)))
      case _ => NotFound()
    }
  }

  registerPath("/search/haut",
    s"""    get:
       |      summary: Hakee organisaation hakuja annetuilla parametreilla
       |      operationId: Hakujen haku
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
       |""".stripMargin)
  get("/haut") {

    implicit val authenticated: Authenticated = authenticate()

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) => Ok(hakuService.search(organisaatioOid, params.toMap.filterKeys(SearchParams.contains)))
    }
  }

  registerPath("/search/haku/{oid}",
    s"""    get:
       |      summary: Hakee rikastetun haun annetulla oidilla
       |      operationId: Haun haku
       |      description: Hakee rikastetun haun annetulla oidilla
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
       |                type: object
       |                $$ref: '#/components/schemas/HakuSearchItemWithHakukohteet'
       |""".stripMargin)
  get("/haku/:oid") {

    implicit val authenticated: Authenticated = authenticate()

    (HakuOid(params("oid")), params.get("organisaatioOid").map(OrganisaatioOid)) match {
      case (hakuOid, Some(organisaatioOid)) => Ok(hakuService.search(organisaatioOid, hakuOid, params.toMap.filterKeys(SearchParams.contains)))
      case _ => NotFound()
    }
  }

  registerPath("/search/hakukohteet",
    s"""    get:
       |      summary: Hakee organisaation hakukohteita annetuilla parametreilla
       |      operationId: Hakujen haku
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
       |""".stripMargin)
  get("/hakukohteet") {

    implicit val authenticated: Authenticated = authenticate()

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) => Ok(hakukohdeService.search(organisaatioOid, params.toMap.filterKeys(SearchParams.contains)))
    }
  }

  registerPath("/search/valintaperusteet",
    s"""    get:
       |      summary: Hakee organisaation valintaperustekuvauksia annetuilla parametreilla
       |      operationId: Valintaperustekuvausten haku
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
       |""".stripMargin)
  get("/valintaperusteet") {

    implicit val authenticated: Authenticated = authenticate()

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) => Ok(valintaperusteService.search(organisaatioOid, params.toMap.filterKeys(SearchParams.contains)))
    }
  }
}
