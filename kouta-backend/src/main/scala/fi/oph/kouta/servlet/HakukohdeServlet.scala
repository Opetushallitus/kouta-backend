package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain.{CreateResult, Hakukohde, TilaFilter, UpdateResult}
import fi.oph.kouta.domain.oid.{HakuOid, HakukohdeOid}
import fi.oph.kouta.service.HakukohdeService
import org.scalatra.{NotFound, Ok}

class HakukohdeServlet(hakukohdeService: HakukohdeService) extends KoutaServlet {

  def this() = this(HakukohdeService)

  registerPath("/hakukohde/{oid}",
    """    get:
      |      summary: Hae hakukohteen tiedot
      |      operationId: Hae hakukohde
      |      description: Hakee hakukohteen kaikki tiedot
      |      tags:
      |        - Hakukohde
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Hakukohde-oid
      |          example: 1.2.246.562.20.00000000000000000009
      |        - in: query
      |          name: myosPoistetut
      |          schema:
      |            type: boolean
      |            default: false
      |          required: false
      |          description: Palautetaanko myös mahdollisesti poistettu hakukohde
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                $ref: '#/components/schemas/Hakukohde'
      |""".stripMargin)
  get("/:oid") {

    implicit val authenticated: Authenticated = authenticate()

    val myosPoistetut = params.getOrElse("myosPoistetut", "false").toBoolean
    hakukohdeService.get(HakukohdeOid(params("oid")), TilaFilter.alsoPoistetutAddedToOthers(myosPoistetut)) match {
      case None => NotFound("error" -> "Unknown hakukohde oid")
      case Some((k, l)) => Ok(k, headers = Map(KoutaServlet.LastModifiedHeader -> createLastModifiedHeader(l)))
    }
  }

  registerPath( "/hakukohde/",
    """    put:
      |      summary: Tallenna uusi hakukohde
      |      operationId: Tallenna uusi hakukohde
      |      description: Tallenna uuden hakukohteen tiedot.
      |        Rajapinta palauttaa hakukohteelle generoidun yksilöivän hakukohde-oidin.
      |      tags:
      |        - Hakukohde
      |      requestBody:
      |        description: Tallennettava hakukohde
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/Hakukohde'
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
      |                    description: Uuden hakukohteen yksilöivä oid
      |                    example: 1.2.246.562.20.00000000000000000009
      |""".stripMargin)
  put("/") {

    implicit val authenticated: Authenticated = authenticate()

    hakukohdeService.put(parsedBody.extract[Hakukohde]) match {
      case res: CreateResult => Ok(res)
    }
  }
  registerPath( "/hakukohde/copy/{hakuOid}",
    """    put:
      |      summary: Tallenna kopiot hakukohteista ja niihin liitetyistä toteutuksista
      |      operationId: Tallenna kopiot hakukohteista
      |      description: Luodaan kopiot pyynnössä listatuista hakukohteista sekä niihin liitetyistä toteutuksista.
      |        Hakukohteesta tehtävä kopio liitetään hakuun, jonka oid annetaan parametrina.
      |        Kopiot tallennetaan luonnostilaisina.
      |        Rajapinta palauttaa tallennettujen hakukohteiden ja toteutusten yksilöivät oidit.
      |      tags:
      |        - Hakukohde
      |      parameters:
      |        - in: path
      |          name: hakuOid
      |          required: true
      |          schema:
      |            type: string
      |          description: Sen haun oid, johon kopioitavat hakukohteen liitetään
      |          example: 1.2.246.562.29.00000000000000011030
      |      requestBody:
      |        description: Lista kopioitavien hakukohteiden oideja
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: array
      |              items:
      |                type: string
      |              example: ["1.2.246.562.20.00000000000000011083", "1.2.246.562.20.00000000000000011084"]
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
  put("/copy/:hakuOid") {

    implicit val authenticated: Authenticated = authenticate()

    val hakukohdeCopyResults = hakukohdeService.copy(parsedBody.extract[List[HakukohdeOid]], HakuOid(params("hakuOid")))
    if (hakukohdeCopyResults.isEmpty) {
      NotFound("error" -> "No hakukohde was copied")
    } else {
      Ok(hakukohdeCopyResults)
    }
  }

  registerPath("/hakukohde/",
    """    post:
      |      summary: Muokkaa olemassa olevaa hakukohdetta
      |      operationId: Muokkaa hakukohdetta
      |      description: Muokkaa olemassa olevaa hakukohdetta. Rajapinnalle annetaan hakukohteen kaikki tiedot,
      |        ja muuttuneet tiedot tallennetaan kantaan.
      |      tags:
      |        - Hakukohde
      |      parameters:
      |        - $ref: '#/components/parameters/xIfUnmodifiedSince'
      |      requestBody:
      |        description: Muokattavan hakukohteen kaikki tiedot. Kantaan tallennetaan muuttuneet tiedot.
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/Hakukohde'
      |      responses:
      |        '200':
      |          description: O
      |""".stripMargin)
  post("/") {

    implicit val authenticated: Authenticated = authenticate()

    hakukohdeService.update(parsedBody.extract[Hakukohde], getIfUnmodifiedSince) match {
      case res: UpdateResult => Ok(res)
    }
  }

  registerPath("/hakukohde/:oid/tila/{tila}",
    """    get:
      |      summary: Onko tilamuutos hakukohteelle sallittu
      |      operationId: Onko tilamuutos sallittu hakukohteelle
      |      description: Muuttaa annettavien hakukohdeoidien tilat. Tila annetaan parametrina.
      |        Rajapinta palauttaa true/false sen perusteella onko tilamuutos sallittu.
      |      tags:
      |        - Hakukohde
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Hakukohde-oid
      |          example: 1.2.246.562.20.00000000000000000009
      |        - in: path
      |          name: tila
      |          required: true
      |          schema:
      |            type: string
      |          description: Tila jolle kysytään
      |          example: poistettu
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: boolean
      |""".stripMargin)

  get("/:oid/tila/:tila") {
    implicit val authenticated: Authenticated = authenticate()
    val result: Boolean = hakukohdeService.tilaChangeAllowed(HakukohdeOid(params("oid")), params("tila"))
    Ok(result)
  }

  registerPath("/hakukohde/tila/{tila}",
    """    post:
      |      summary: Muuttaa usean hakukohteen tilat
      |      operationId: Muuttaa hakukohteiden tilat
      |      description: Muuttaa annettavien hakukohdeoidien tilat. Tila annetaan parametrina.
      |        Rajapinta palauttaa muutettujen hakukohteiden yksilöivät oidit.
      |      tags:
      |        - Hakukohde
      |      parameters:
      |        - $ref: '#/components/parameters/xIfUnmodifiedSince'
      |        - in: path
      |          name: tila
      |          required: true
      |          schema:
      |            type: string
      |          description: Hakukohteen julkaisutila, joka päivitetään hakukohteille
      |          example: tallennettu
      |      requestBody:
      |        description: Lista muutettavien hakukohteiden oideja
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: array
      |              items:
      |                type: string
      |              example: ["1.2.246.562.20.00000000000000011083", "1.2.246.562.20.00000000000000011084"]
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/TilaChangeResult'
      |""".stripMargin)

  post("/tila/:tila") {

    implicit val authenticated: Authenticated = authenticate()

    val hakukohdeStateChangeResults = hakukohdeService.changeTila(parsedBody.extract[List[HakukohdeOid]], params("tila"), getIfUnmodifiedSince)
    if (hakukohdeStateChangeResults.isEmpty) {
      NotFound("error" -> "No hakukohde state was changed")
    } else {
      Ok(hakukohdeStateChangeResults)
    }
  }
}
