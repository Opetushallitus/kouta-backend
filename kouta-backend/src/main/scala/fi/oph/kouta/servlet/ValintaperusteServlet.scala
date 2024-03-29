package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain.{Koulutustyyppi, TilaFilter, UpdateResult, Valintaperuste, ValintaperusteCreateResult}
import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.service.ValintaperusteService
import org.scalatra.{BadRequest, NotFound, Ok}

import java.util.UUID

class ValintaperusteServlet(valintaperusteService: ValintaperusteService) extends KoutaServlet {

  def this() = this(ValintaperusteService)

  registerPath("/valintaperuste/{id}",
    """    get:
      |      summary: Hae valintaperustekuvauksen tiedot
      |      operationId: Hae valintaperuste
      |      description: Hakee valintaperustekuvauksen kaikki tiedot
      |      tags:
      |        - Valintaperuste
      |      parameters:
      |        - in: path
      |          name: id
      |          schema:
      |            type: string
      |          required: true
      |          description: Valintaperuste-id
      |          example: ea596a9c-5940-497e-b5b7-aded3a2352a7
      |        - in: query
      |          name: myosPoistetut
      |          schema:
      |            type: boolean
      |            default: false
      |          required: false
      |          description: Palautetaanko myös mahdollisesti poistettu valintaperuste
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                $ref: '#/components/schemas/Valintaperuste'
      |""".stripMargin)
  get("/:id") {

    implicit val authenticated: Authenticated = authenticate()

    val myosPoistetut = params.getOrElse("myosPoistetut", "false").toBoolean
    valintaperusteService.get(UUID.fromString(params("id")), TilaFilter.alsoPoistetutAddedToOthers(myosPoistetut)) match {
      case None => NotFound("error" -> "Unknown valintaperuste id")
      case Some((k, l)) => Ok(k, headers = Map(KoutaServlet.LastModifiedHeader -> createLastModifiedHeader(l)))
    }
  }

  registerPath( "/valintaperuste/",
    """    put:
      |      summary: Tallenna uusi valintaperustekuvaus
      |      operationId: Tallenna uusi valintaperuste
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
      |              $ref: '#/components/schemas/Valintaperuste'
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: object
      |                properties:
      |                  id:
      |                    type: string
      |                    description: Uuden valintaperustekuvauksen yksilöivä id
      |                    example: ea596a9c-5940-497e-b5b7-aded3a2352a7
      |""".stripMargin)
  put("/") {

    implicit val authenticated: Authenticated = authenticate()

    valintaperusteService.put(parsedBody.extract[Valintaperuste]) match {
      case res: ValintaperusteCreateResult => Ok(res)
    }
  }

  registerPath("/valintaperuste/",
    """    post:
      |      summary: Muokkaa olemassa olevaa valintaperustekuvausta
      |      operationId: Muokkaa valintaperustetta
      |      description: Muokkaa olemassa olevaa valintaperustekuvausta. Rajapinnalle annetaan valintaperusteen kaikki tiedot,
      |        ja muuttuneet tiedot tallennetaan kantaan.
      |      tags:
      |        - Valintaperuste
      |      parameters:
      |        - $ref: '#/components/parameters/xIfUnmodifiedSince'
      |      requestBody:
      |        description: Muokattavan valintaperustekuvauksen kaikki tiedot. Kantaan tallennetaan muuttuneet tiedot.
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/Valintaperuste'
      |      responses:
      |        '200':
      |          description: O
      |""".stripMargin)
  post("/") {

    implicit val authenticated: Authenticated = authenticate()

    valintaperusteService.update(parsedBody.extract[Valintaperuste], getIfUnmodifiedSince) match {
      case res: UpdateResult => Ok(res)
    }
  }

  registerPath("/valintaperuste/list",
    """    get:
      |      summary: Listaa organisaation tietyn haun hakukohteille käytettävissä olevat valintaperustekuvaukset
      |      operationId: Listaa valintaperusteet
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
      |          required: false
      |          description: Haku-oid
      |          example: 1.2.246.562.29.00000000000000000009
      |        - in: query
      |          name: koulutustyyppi
      |          schema:
      |            type: string
      |          required: false
      |          description: Koulutustyyppi
      |          example: amm
      |        - in: query
      |          name: myosArkistoidut
      |          schema:
      |            type: boolean
      |            default: true
      |          required: false
      |          description: Listataanko myös arkistoidut valintaperusteet
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/ValintaperusteListItem'
      |""".stripMargin)
  get("/list") {

    implicit val authenticated: Authenticated = authenticate()

    (params.get("organisaatioOid"), params.get("hakuOid"), params.get("koulutustyyppi"), params.getOrElse("myosArkistoidut", "true").toBoolean) match {
      case (None, _, _, _) => NotFound()
      case (Some(oid), None, None, myosArkistoidut) => Ok(valintaperusteService.list(OrganisaatioOid(oid),
        TilaFilter.alsoArkistoidutAddedToOlemassaolevat(myosArkistoidut)))
      case (Some(oid), Some(hakuOid), Some(koulutustyyppi), myosArkistoidut) =>
        Ok(valintaperusteService.listByHakuAndKoulutustyyppi(OrganisaatioOid(oid), HakuOid(hakuOid), Koulutustyyppi.withName(koulutustyyppi),
          TilaFilter.alsoArkistoidutAddedToOlemassaolevat(myosArkistoidut)))
      case _ => BadRequest()
    }
  }
}
