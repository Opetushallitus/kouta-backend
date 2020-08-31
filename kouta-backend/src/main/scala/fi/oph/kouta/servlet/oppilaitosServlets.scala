package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain.{OppilaitoksenOsa, Oppilaitos}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.service.{OppilaitoksenOsaService, OppilaitosService}
import org.scalatra.{NotFound, Ok}

class OppilaitosServlet(oppilaitosService: OppilaitosService) extends KoutaServlet {

  def this() = this(OppilaitosService)

  registerPath("/oppilaitos/{oid}",
    """    get:
      |      summary: Hae oppilaitoksen kuvailutiedot
      |      operationId: Hae oppilaitos
      |      description: Hakee oppilaitoksen kuvailutiedot
      |      tags:
      |        - Oppilaitos
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Oppilaitoksen organisaatio-oid
      |          example: 1.2.246.562.10.00101010101
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                $ref: '#/components/schemas/Oppilaitos'
      |""".stripMargin)
  get("/:oid") {

    implicit val authenticated: Authenticated = authenticate()

    oppilaitosService.get(OrganisaatioOid(params("oid"))) match {
      case None => NotFound("error" -> "Unknown organisaatio oid")
      case Some((k, l)) => Ok(k, headers = Map(KoutaServlet.LastModifiedHeader -> createLastModifiedHeader(l)))
    }
  }

  registerPath( "/oppilaitos/",
    """    put:
      |      summary: Tallenna uusi oppilaitoksen kuvailutieto
      |      operationId: Tallenna uusi oppilaitos
      |      description: Tallenna uusi oppilaitoksen kuvailutieto.
      |        Palauttaa tallennetun oppilaitoksen organisaatio-oidin.
      |      tags:
      |        - Oppilaitos
      |      requestBody:
      |        description: Tallennettavan oppilaitoksen tiedot
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/Oppilaitos'
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
      |                    example: 1.2.246.562.10.00101010101
      |""".stripMargin)
  put("/") {

    implicit val authenticated: Authenticated = authenticate()

    oppilaitosService.put(parsedBody.extract[Oppilaitos]) match {
      case oid => Ok("oid" -> oid)
    }
  }

  registerPath("/oppilaitos/",
    """    post:
      |      summary: Muokkaa olemassa olevaa oppilaitoksen kuvailutietoa
      |      operationId: Muokkaa oppilaitosta
      |      description: Muokkaa olemassa olevaa oppilaitoksen kuvailutietoa.
      |        Rajapinnalle annetaan oppilaitoksen kaikki tiedot,
      |        ja muuttuneet tiedot tallennetaan kantaan.
      |      tags:
      |        - Oppilaitos
      |      requestBody:
      |        description: Muokattavan oppilaitoksen kaikki tiedot. Kantaan tallennetaan muuttuneet tiedot.
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/Oppilaitos'
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin)
  post("/") {

    implicit val authenticated: Authenticated = authenticate()

    oppilaitosService.update(parsedBody.extract[Oppilaitos], getIfUnmodifiedSince) match {
      case updated => Ok("updated" -> updated)
    }
  }

  registerPath( "/oppilaitos/{oid}/osat/list",
    """    get:
      |      summary: Listaa organisaation käytettävissä olevat oppilaitoksen osien kuvailutiedot
      |      operationId: Listaa oppilaitoksen osat
      |      description: Listaa ne oppilaitoksen osat, jotka ovat organisaation käytettävissä.
      |      tags:
      |        - Oppilaitos
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Oppilaitoksen organisaatio-oid
      |          example: 1.2.246.562.10.00101010101
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
      |                  $ref: '#/components/schemas/OppilaitoksenOsaListItem'
      |""".stripMargin)
  get("/:oid/osat/list") {

    implicit val authenticated: Authenticated = authenticate()

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) => Ok(oppilaitosService.listOppilaitoksenOsat(OrganisaatioOid(params("oid")), organisaatioOid))
    }
  }
}

class OppilaitoksenOsaServlet(oppilaitoksenOsaService: OppilaitoksenOsaService) extends KoutaServlet {

  def this() = this(OppilaitoksenOsaService)

  registerPath("/oppilaitoksen-osa/{oid}",
    """    get:
      |      summary: Hae oppilaitoksen osan kuvailutiedot
      |      operationId: Hae oppilaitoksen osa
      |      description: Hakee oppilaitoksen osan kuvailutiedot
      |      tags:
      |        - Oppilaitoksen osa
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Oppilaitoksen organisaatio-oid
      |          example: 1.2.246.562.10.00101010101
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                $ref: '#/components/schemas/OppilaitoksenOsa'
      |""".stripMargin)
  get("/:oid") {

    implicit val authenticated: Authenticated = authenticate()

    oppilaitoksenOsaService.get(OrganisaatioOid(params("oid"))) match {
      case None => NotFound("error" -> "Unknown organisaatio oid")
      case Some((k, l)) => Ok(k, headers = Map(KoutaServlet.LastModifiedHeader -> createLastModifiedHeader(l)))
    }
  }

  registerPath( "/oppilaitoksen-osa/",
    """    put:
      |      summary: Tallenna uusi oppilaitoksen osan kuvailutieto
      |      operationId: Tallenna uusi oppilaitoksen osa
      |      description: Tallenna uusi oppilaitoksen osan kuvailutieto.
      |        Palauttaa tallennetun oppilaitoksen osan organisaatio-oidin.
      |      tags:
      |        - Oppilaitoksen osa
      |      requestBody:
      |        description: Tallennettavan oppilaitoksen osan tiedot
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/OppilaitoksenOsa'
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
      |                    example: 1.2.246.562.10.00101010101
      |""".stripMargin)
  put("/") {

    implicit val authenticated: Authenticated = authenticate()

    oppilaitoksenOsaService.put(parsedBody.extract[OppilaitoksenOsa]) match {
      case oid => Ok("oid" -> oid)
    }
  }

  registerPath("/oppilaitoksen-osa/",
    """    post:
      |      summary: Muokkaa olemassa olevaa oppilaitoksen osan kuvailutietoa
      |      operationId: Muokkaa oppilaitoksen osaa
      |      description: Muokkaa olemassa olevaa oppilaitoksen osan kuvailutietoa.
      |        Rajapinnalle annetaan oppilaitoksen osan kaikki tiedot,
      |        ja muuttuneet tiedot tallennetaan kantaan.
      |      tags:
      |        - Oppilaitoksen osa
      |      requestBody:
      |        description: Muokattavan oppilaitoksen osan kaikki tiedot. Kantaan tallennetaan muuttuneet tiedot.
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/OppilaitoksenOsa'
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin)
  post("/") {

    implicit val authenticated: Authenticated = authenticate()

    oppilaitoksenOsaService.update(parsedBody.extract[OppilaitoksenOsa], getIfUnmodifiedSince) match {
      case updated => Ok("updated" -> updated)
    }
  }
}
