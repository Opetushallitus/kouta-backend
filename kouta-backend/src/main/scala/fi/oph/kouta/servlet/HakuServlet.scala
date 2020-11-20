package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.service.HakuService
import org.scalatra.{NotFound, Ok}

class HakuServlet(hakuService: HakuService) extends KoutaServlet {

  def this() = this(HakuService)

  registerPath("/haku/{oid}",
    """    get:
      |      summary: Hae haun tiedot
      |      operationId: Hae haku
      |      description: Hakee haun kaikki tiedot
      |      tags:
      |        - Haku
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Haku-oid
      |          example: 1.2.246.562.29.00000000000000000009
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                $ref: '#/components/schemas/Haku'
      |""".stripMargin)
  get("/:oid") {

    implicit val authenticated: Authenticated = authenticate()

    hakuService.get(HakuOid(params("oid"))) match {
      case None => NotFound("error" -> "Unknown haku oid")
      case Some((k, l)) => Ok(k, headers = Map(KoutaServlet.LastModifiedHeader -> createLastModifiedHeader(l)))
    }
  }

  registerPath( "/haku/",
    """    put:
      |      summary: Tallenna uusi haku
      |      operationId: Tallenna uusi haku
      |      description: Tallenna uuden haun tiedot.
      |        Rajapinta palauttaa haulle generoidun yksilöivän haku-oidin.
      |      tags:
      |        - Haku
      |      requestBody:
      |        description: Tallennettava haku
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/Haku'
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
      |                    description: Uuden haun yksilöivä oid
      |                    example: 1.2.246.562.29.00000000000000000009
      |""".stripMargin)
  put("/") {

    implicit val authenticated: Authenticated = authenticate()

    hakuService.put(parsedBody.extract[Haku]) match {
      case oid => Ok("oid" -> oid)
    }
  }

  registerPath("/haku/",
    """    post:
      |      summary: Muokkaa olemassa olevaa hakua
      |      operationId: Muokkaa hakua
      |      description: Muokkaa olemassa olevaa hakua. Rajapinnalle annetaan haun kaikki tiedot,
      |        ja muuttuneet tiedot tallennetaan kantaan.
      |      tags:
      |        - Haku
      |      requestBody:
      |        description: Muokattavan haun kaikki tiedot. Kantaan tallennetaan muuttuneet tiedot.
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/Haku'
      |      responses:
      |        '200':
      |          description: O
      |""".stripMargin)
  post("/") {

    implicit val authenticated: Authenticated = authenticate()

    hakuService.update(parsedBody.extract[Haku], getIfUnmodifiedSince) match {
      case updated => Ok("updated" -> updated)
    }
  }

  registerPath("/haku/list",
    """    get:
      |      summary: Listaa organisaation käytettävissä olevat haut
      |      operationId: Listaa haut
      |      description: Listaa niiden hakujen tiedot, jotka ovat organisaation käytettävissä
      |      tags:
      |        - Haku
      |      parameters:
      |        - in: query
      |          name: organisaatioOid
      |          schema:
      |            type: string
      |          required: true
      |          description: Organisaatio-oid
      |          example: 1.2.246.562.10.00101010101
      |        - in: query
      |          name: myosArkistoidut
      |          schema:
      |            type: boolean
      |          required: false
      |          default: true
      |          description: Listataanko myös arkistoidut haut
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/HakuListItem'
      |""".stripMargin)
  get("/list") {

    implicit val authenticated: Authenticated = authenticate()

    (params.get("organisaatioOid").map(OrganisaatioOid), params.getOrElse("myosArkistoidut", "true").toBoolean) match {
      case (None, _) => NotFound()
      case (Some(oid), myosArkistoidut) => Ok(hakuService.list(oid, myosArkistoidut))
    }
  }

  registerPath( "/haku/{oid}/hakukohteet/list",
    """    get:
      |      summary: Listaa kaikki organisaatiolle kuuluvat hakukohteet, jotka on liitetty hakuun
      |      operationId: Listaa haun hakukohteet
      |      description: Listaa ne hakuun liitetyt hakukohteet, jotka ovat organisaatiolla on oikeus nähdä.
      |        Jos organisaatio-oidia ei ole annettu,
      |        listaa haun kaikki hakukohteet, mikäli käyttäjällä on oikeus nähdä ne
      |      tags:
      |        - Haku
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Haku-oid
      |          example: 1.2.246.562.29.00000000000000000009
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

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) => Ok(hakuService.listHakukohteet(HakuOid(params("oid")), organisaatioOid))
    }
  }
}
