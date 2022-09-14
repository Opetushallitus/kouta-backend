package fi.oph.kouta.servlet

import fi.oph.kouta.domain.oid.{OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.domain._
import fi.oph.kouta.service.ToteutusService
import org.scalatra.{NotFound, Ok}
import fi.oph.kouta.SwaggerPaths.registerPath

class ToteutusServlet(toteutusService: ToteutusService) extends KoutaServlet {

  def this() = this(ToteutusService)

  registerPath("/toteutus/{oid}",
    """    get:
      |      summary: Hae koulutuksen toteutus
      |      operationId: Hae toteutus
      |      description: Hakee koulutuksen toteutuksen tiedot
      |      tags:
      |        - Toteutus
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: toteutus-oid
      |          example: 1.2.246.562.17.00000000000000000009
      |        - in: query
      |          name: myosPoistetut
      |          schema:
      |            type: boolean
      |            default: false
      |          required: false
      |          description: Palautetaanko myös mahdollisesti poistettu toteutus
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                $ref: '#/components/schemas/Toteutus'
      |""".stripMargin)
  get("/:oid") {

    implicit val authenticated: Authenticated = authenticate()

    val myosPoistetut = params.getOrElse("myosPoistetut", "false").toBoolean
    toteutusService.get(ToteutusOid(params("oid")), TilaFilter.alsoPoistetutAddedToOthers(myosPoistetut)) match {
      case None => NotFound("error" -> "Unknown toteutus oid")
      case Some((k, l)) => Ok(k, headers = Map(KoutaServlet.LastModifiedHeader -> createLastModifiedHeader(l)))
    }
  }

  registerPath( "/toteutus/",
    """    put:
      |      summary: Tallenna uusi toteutus
      |      operationId: Tallenna uusi toteutus
      |      description: Tallenna uuden toteutuksen tiedot.
      |        Rajapinta palauttaa toteutukselle generoidun yksilöivän toteutus-oidin.
      |      tags:
      |        - Toteutus
      |      requestBody:
      |        description: Tallennettava toteutus
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/Toteutus'
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: object
      |                properties:
      |                  oid:
      |                    type: string
      |                    description: Uuden toteutuksen yksilöivä oid
      |                    example: 1.2.246.562.17.00000000000000000009
      |""".stripMargin)
  put("/") {

    implicit val authenticated: Authenticated = authenticate()

    toteutusService.put(parsedBody.extract[Toteutus]) match {
      case oid => Ok("oid" -> oid)
    }
  }

  registerPath("/toteutus/",
    """    post:
      |      summary: Muokkaa olemassa olevaa toteutusta
      |      operationId: Muokkaa toteutusta
      |      description: Muokkaa olemassa olevaa toteutusta. Rajapinnalle annetaan toteutuksen kaikki tiedot,
      |        ja muuttuneet tiedot tallennetaan kantaan.
      |      tags:
      |        - Toteutus
      |      parameters:
      |        - $ref: '#/components/parameters/xIfUnmodifiedSince'
      |      requestBody:
      |        description: Muokattavan toteutuksen kaikki tiedot. Kantaan tallennetaan muuttuneet tiedot.
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/Toteutus'
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin)
  post("/") {

    implicit val authenticated: Authenticated = authenticate()

    toteutusService.update(parsedBody.extract[Toteutus], getIfUnmodifiedSince) match {
      case updated => Ok("updated" -> updated)
    }
  }

  registerPath("/toteutus/list",
    """    get:
      |      summary: Listaa organisaation käytettävissä olevat toteutukset
      |      operationId: Listaa toteutukset
      |      description: Listaa niiden toteutusten tiedot, jotka ovat organisaation käytettävissä
      |      tags:
      |        - Toteutus
      |      parameters:
      |        - in: query
      |          name: organisaatioOid
      |          schema:
      |            type: string
      |          required: true
      |          description: Organisaatio-oid
      |          example: 1.2.246.562.10.00101010101
      |        - in: query
      |          name: vainHakukohteeseenLiitettavat
      |          schema:
      |            type: boolean
      |            default: false
      |          required: false
      |          description: Listataanko vain ne toteutukset, jotka voi liittää hakukohteeseen
      |        - in: query
      |          name: myosArkistoidut
      |          schema:
      |            type: boolean
      |            default: true
      |          required: false
      |          description: Listataanko myös arkistoidut toteutukset
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/ToteutusListItem'
      |""".stripMargin)
  get("/list") {

    implicit val authenticated: Authenticated = authenticate()

    (params.get("organisaatioOid").map(OrganisaatioOid),
     params.get("vainHakukohteeseenLiitettavat").exists(_.toBoolean),
     params.getOrElse("myosArkistoidut", "true").toBoolean)
    match {
      case (None, _, _) => NotFound()
      case (Some(oid), vainHakukohteeseenLiitettavat, myosArkistoidut) => Ok(toteutusService.list(oid, vainHakukohteeseenLiitettavat, TilaFilter.alsoArkistoidutAddedToOlemassaolevat(myosArkistoidut)))
    }
  }

  registerPath( "/toteutus/{oid}/hakukohteet/list",
    """    get:
      |      summary: Listaa kaikki toteutukseen liitetyt hakukohteet
      |      operationId: Listaa toteutuksen hakukohteet
      |      description: Listaa kaikki toteutukseen liitetyt hakukohteet, mikäli käyttäjällä on oikeus nähdä ne
      |      tags:
      |        - Toteutus
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Toteutus-oid
      |          example: 1.2.246.562.17.00000000000000000009
      |        - in: query
      |          name: organisaatioOid
      |          schema:
      |            type: string
      |          required: true
      |          description: Organisaatio-oid
      |          example: 1.2.246.562.10.00101010101
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/HakukohdeListItem'
      |""".stripMargin)
  get("/:oid/hakukohteet/list") {
    implicit val authenticated: Authenticated = authenticate()

    val toteutusOid = ToteutusOid(params("oid"))

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) => Ok(toteutusService.listHakukohteet(toteutusOid, organisaatioOid))
    }
  }

  registerPath( "/toteutus/copy",
    """    put:
      |      summary: Tallenna kopiot toteutuksista
      |      operationId: Tallenna kopiot toteutuksista
      |      description: Tallennetaan kopioitavien toteutusten tiedot tietokantaan.
      |        Kopiototeutukset tallennetaan luonnostilaisina.
      |        Rajapinta palauttaa toteutuksille generoidut yksilöivät toteutus-oidit.
      |      tags:
      |        - Toteutus
      |      requestBody:
      |        description: Lista kopioitavien toteutusten id:itä
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: array
      |              items:
      |                type: string
      |              example: ["1.2.246.562.17.00000000000000000004", "1.2.246.562.17.00000000000000000005"]
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/CopyResult'
      |""".stripMargin)
  put("/copy") {

    implicit val authenticated: Authenticated = authenticate()

    val copyResult = toteutusService.copy(parsedBody.extract[List[ToteutusOid]])
    if (copyResult.isEmpty) {
      NotFound("error" -> "Unknown toteutus oid")
    } else {
      Ok(copyResult)
    }
  }

  registerPath( "/toteutus/opintojaksot/list",
    """    get:
      |      summary: Listaa organisaation käytettävissä olevat opintojaksot
      |      operationId: Listaa opintojaksot
      |      description: Listaa organisaation käytettävissä olevat opintojaksot
      |        uutta opintokokonaisuustoteutusta luotaessa
      |      tags:
      |        - Toteutus
      |      parameters:
      |        - in: query
      |          name: organisaatioOid
      |          schema:
      |            type: string
      |          required: true
      |          description: Organisaatio-oid
      |          example: 1.2.246.562.10.00101010101
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/ToteutusListItem'
      |""".stripMargin)
  get("/opintojaksot/list") {
    implicit val authenticated: Authenticated = authenticate()

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) => Ok(toteutusService.listOpintojaksot(organisaatioOid))
    }
  }
}
