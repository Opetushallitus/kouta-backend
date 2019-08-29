package fi.oph.kouta.servlet

import java.util.UUID

import fi.oph.kouta.SwaggerYaml.registerPath
import fi.oph.kouta.domain.{HakukohdeListItem, Valintaperuste, ValintaperusteListItem}
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.service.ValintaperusteService
import org.scalatra.swagger.Swagger
import org.scalatra.{NotFound, Ok}

class ValintaperusteServlet(valintaperusteService: ValintaperusteService)(implicit val swagger: Swagger) extends KoutaServlet {
  override val applicationDescription = "Valintaperustekuvausten API"
  override val modelName = "Valintaperuste"

  def this()(implicit swagger: Swagger) = this(ValintaperusteService)

  registerPath("/valintaperuste/{id}",
    s"""    get:
       |      summary: Hae valintaperustekuvauksen tiedot
       |      description: Hakee valintaperustekuvauksen kaikki tiedot
       |      tags:
       |        - Valintaperuste
       |      parameters:
       |        - in: path
       |          name: oid
       |          schema:
       |            type: string
       |          required: true
       |          description: Valintaperuste-id
       |          example: ea596a9c-5940-497e-b5b7-aded3a2352a7
       |      responses:
       |        '200':
       |          description: Ok
       |          content:
       |            application/json:
       |              schema:
       |                $$ref: '#/components/schemas/Valintaperuste'
       |""".stripMargin)
  get("/:id") {

    implicit val authenticated: Authenticated = authenticate

    valintaperusteService.get(UUID.fromString(params("id"))) match {
      case None => NotFound("error" -> "Unknown valintaperuste id")
      case Some((k, l)) => Ok(k, headers = Map("Last-Modified" -> createLastModifiedHeader(l)))
    }
  }

  registerPath( "/valintaperuste/",
    s"""    put:
       |      summary: Tallenna uusi valintaperustekuvaus
       |      description: Tallenna uuden valintaperustekuvauksen tiedot.
       |        Rajapinta palauttaa valintaperustekuvaukselle generoidun yksilöivän id:n
       |      tags:
       |        - Valintaperuste
       |      requestBody:
       |        description: Tallennettava valintaperustekuvaus
       |        required: true
       |        content:
       |          application/json:
       |            schema:
       |              $$ref: '#/components/schemas/Valintaperuste'
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
       |                    description: Uuden valintaperustekuvauksen yksilöivä id
       |                    example: ea596a9c-5940-497e-b5b7-aded3a2352a7
       |""".stripMargin)
  put("/") {

    implicit val authenticated: Authenticated = authenticate

    valintaperusteService.put(parsedBody.extract[Valintaperuste]) match {
      case id => Ok("id" -> id)
    }
  }

  registerPath("/valintaperuste/",
    s"""    post:
       |      summary: Muokkaa olemassa olevaa valintaperustekuvausta
       |      description: Muokkaa olemassa olevaa valintaperustekuvausta. Rajapinnalle annetaan valintaperusteen kaikki tiedot,
       |        ja muuttuneet tiedot tallennetaan kantaan.
       |      tags:
       |        - Valintaperuste
       |      requestBody:
       |        description: Muokattavan valintaperustekuvauksen kaikki tiedot. Kantaan tallennetaan muuttuneet tiedot.
       |        required: true
       |        content:
       |          application/json:
       |            schema:
       |              $$ref: '#/components/schemas/Valintaperuste'
       |      responses:
       |        '200':
       |          description: O
       |""".stripMargin)
  post("/") {

    implicit val authenticated: Authenticated = authenticate

    valintaperusteService.update(parsedBody.extract[Valintaperuste], getIfUnmodifiedSince) match {
      case updated => Ok("updated" -> updated)
    }
  }

  registerPath("/valintaperuste/list",
    s"""    get:
       |      summary: Listaa organisaation tietyn haun hakukohteille käytettävissä olevat valintaperustekuvaukset
       |      description: Listaa niiden valintaperustekuvausten tiedot, jotka ovat organisaation käytettävissä.
       |        Jos haku-oid on annettu, listataan ne kuvaukset, joita voi käyttää kyseisen haun hakukohteille
       |      tags:
       |        - Valintaperuste
       |      parameters:
       |        - in: query
       |          name: organisaatioOid
       |          schema:
       |            type: string
       |          required: true
       |          description: Organisaatio-oid
       |          example: 1.2.246.562.10.00101010101
       |        - in: query
       |          name: hakuOid
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
       |                type: array
       |                items:
       |                  $$ref: '#/components/schemas/ValintaperusteListItem'
       |""".stripMargin)
  get("/list") {

    implicit val authenticated: Authenticated = authenticate

    ( params.get("organisaatioOid"), params.get("hakuOid") ) match {
      case (None, _) => NotFound()
      case (Some(oid), None) => Ok(valintaperusteService.list(OrganisaatioOid(oid)))
      case (Some(oid), Some(hakuOid)) => Ok(valintaperusteService.listByHaunKohdejoukko(OrganisaatioOid(oid), HakuOid(hakuOid)))
    }
  }

  registerPath( "/valintaperuste/{id}/hakukohteet/list",
    s"""    get:
       |      summary: Listaa kaikki hakukohteet, joihin valintaperustekuvaus on liitetty
       |      description: Listaa kaikki hakukohteet, joihin valintaperustekuvaus on liitetty, mikäli käyttäjällä on oikeus nähdä ne
       |      tags:
       |        - Valintaperuste
       |      parameters:
       |        - in: path
       |          name: oid
       |          schema:
       |            type: string
       |          required: true
       |          description: Valintaperusteen id
       |          example: ea596a9c-5940-497e-b5b7-aded3a2352a7
       |      responses:
       |        '200':
       |          description: Ok
       |          content:
       |            application/json:
       |              schema:
       |                type: array
       |                items:
       |                  $$ref: '#/components/schemas/HakukohdeListItem'
       |""".stripMargin)
  get("/:id/hakukohteet/list") {

    implicit val authenticated: Authenticated = authenticate

    Ok(valintaperusteService.listHakukohteet(UUID.fromString(params("id"))))
  }

  prettifySwaggerModels()
}
