package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain.{ExternalHakuRequest, ExternalKoulutusRequest, ExternalRequest, ExternalToteutusRequest, Haku}
import fi.oph.kouta.domain.oid.RootOrganisaatioOid
import fi.oph.kouta.security.Role
import fi.oph.kouta.service._
import org.scalatra.Ok

class ExternalServlet(koulutusService: KoulutusService, toteutusService: ToteutusService, hakuService: HakuService) extends KoutaServlet {

  def this() = this(KoulutusService, ToteutusService, HakuService)

  registerPath(
    "/external/koulutus",
    """    put:
      |      summary: Tallenna uusi koulutus
      |      operationId: Tallenna uusi koulutus
      |      description: Tallenna uuden koulutuksen tiedot.
      |        Rajapinta palauttaa koulutukselle generoidun yksilöivän koulutus-oidin.
      |      tags:
      |        - External
      |      requestBody:
      |        description: Tallennettava koulutus
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: object
      |              properties:
      |                authenticated:
      |                  type: object
      |                  $ref: '#/components/schemas/Authenticated'
      |                haku:
      |                  type: object
      |                  $ref: '#/components/schemas/Koulutus'
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
      |                    description: Uuden koulutuksen yksilöivä oid
      |                    example: 1.2.246.562.13.00000000000000000009
      |""".stripMargin
  )
  put("/koulutus") {
    val koulutusRequest = parsedBody.extract[ExternalKoulutusRequest]
    implicit val authenticated: Authenticated = authenticateExternal(koulutusRequest)

    koulutusService.put(koulutusRequest.koulutus) match {
      case oid => Ok("oid" -> oid)
    }
  }

  registerPath(
    "/external/koulutus",
    """    post:
      |      summary: Muokkaa olemassa olevaa koulutusta
      |      operationId: Muokkaa koulutusta
      |      description: Muokkaa olemassa olevaa koulutusta. Rajapinnalle annetaan koulutuksen kaikki tiedot,
      |        ja muuttuneet tiedot tallennetaan kantaan.
      |      tags:
      |        - External
      |      parameters:
      |        - $ref: '#/components/parameters/xIfUnmodifiedSince'
      |      requestBody:
      |        description: Muokattavan koulutuksen kaikki tiedot. Kantaan tallennetaan muuttuneet tiedot.
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: object
      |              properties:
      |                authenticated:
      |                  type: object
      |                  $ref: '#/components/schemas/Authenticated'
      |                haku:
      |                  type: object
      |                  $ref: '#/components/schemas/Koulutus'
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
      |                    description: Uuden koulutuksen yksilöivä oid
      |                    example: 1.2.246.562.13.00000000000000000009
      |""".stripMargin)
  post("/koulutus") {
    val koulutusRequest = parsedBody.extract[ExternalKoulutusRequest]
    implicit val authenticated: Authenticated = authenticateExternal(koulutusRequest)

    val updated = koulutusService.update(koulutusRequest.koulutus, getIfUnmodifiedSince)
    Ok("updated" -> updated)
  }

  registerPath(
    "/external/toteutus",
    """    put:
      |      summary: Tallenna uusi toteutus
      |      operationId: Tallenna uusi toteutus
      |      description: Tallenna uuden toteutuksen tiedot.
      |        Rajapinta palauttaa toteutukselle generoidun yksilöivän toteutus-oidin.
      |      tags:
      |        - External
      |      requestBody:
      |        description: Tallennettava toteutus
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: object
      |              properties:
      |                authenticated:
      |                  type: object
      |                  $ref: '#/components/schemas/Authenticated'
      |                haku:
      |                  type: object
      |                  $ref: '#/components/schemas/Toteutus'
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
      |""".stripMargin
  )
  put("/toteutus") {
    val toteutusRequest = parsedBody.extract[ExternalToteutusRequest]
    implicit val authenticated: Authenticated = authenticateExternal(toteutusRequest)

    toteutusService.put(toteutusRequest.toteutus) match {
      case oid => Ok("oid" -> oid)
    }
  }

  registerPath(
    "/external/toteutus",
    """    post:
      |      summary: Muokkaa olemassa olevaa toteutusta
      |      operationId: Muokkaa toteutusta
      |      description: Muokkaa olemassa olevaa toteutusta. Rajapinnalle annetaan toteutuksen kaikki tiedot,
      |        ja muuttuneet tiedot tallennetaan kantaan.
      |      tags:
      |        - External
      |      parameters:
      |        - $ref: '#/components/parameters/xIfUnmodifiedSince'
      |      requestBody:
      |        description: Muokattavan toteutuksen kaikki tiedot. Kantaan tallennetaan muuttuneet tiedot.
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: object
      |              properties:
      |                authenticated:
      |                  type: object
      |                  $ref: '#/components/schemas/Authenticated'
      |                haku:
      |                  type: object
      |                  $ref: '#/components/schemas/Toteutus'
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
  post("/toteutus") {
    val toteutusRequest = parsedBody.extract[ExternalToteutusRequest]
    implicit val authenticated: Authenticated = authenticateExternal(toteutusRequest)

    val updated = toteutusService.update(toteutusRequest.toteutus, getIfUnmodifiedSince)
    Ok("updated" -> updated)
  }

  registerPath(
    "/external/haku",
    """    put:
      |      summary: Tallenna uusi haku
      |      operationId: Tallenna uusi haku
      |      description: Tallenna uuden haun tiedot.
      |        Rajapinta palauttaa haulle generoidun yksilöivän haku-oidin.
      |      tags:
      |        - External
      |      requestBody:
      |        description: Tallennettava haku
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: object
      |              properties:
      |                authenticated:
      |                  type: object
      |                  $ref: '#/components/schemas/Authenticated'
      |                haku:
      |                  type: object
      |                  $ref: '#/components/schemas/Haku'
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
      |""".stripMargin
  )
  put("/haku") {
    val hakuRequest = parsedBody.extract[ExternalHakuRequest]
    implicit val authenticated: Authenticated = authenticateExternal(hakuRequest)

    hakuService.put(hakuRequest.haku) match {
      case oid => Ok("oid" -> oid)
    }
  }

  registerPath(
    "/external/haku",
    """    post:
      |      summary: Muokkaa olemassa olevaa hakua
      |      operationId: Muokkaa hakua
      |      description: Muokkaa olemassa olevaa hakua. Rajapinnalle annetaan haun kaikki tiedot,
      |        ja muuttuneet tiedot tallennetaan kantaan.
      |      tags:
      |        - External
      |      parameters:
      |        - $ref: '#/components/parameters/xIfUnmodifiedSince'
      |      requestBody:
      |        description: Muokattavan haun kaikki tiedot. Kantaan tallennetaan muuttuneet tiedot.
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: object
      |              properties:
      |                authenticated:
      |                  type: object
      |                  $ref: '#/components/schemas/Authenticated'
      |                haku:
      |                  type: object
      |                  $ref: '#/components/schemas/Haku'
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
  post("/haku") {
    val hakuRequest = parsedBody.extract[ExternalHakuRequest]
    implicit val authenticated: Authenticated = authenticateExternal(hakuRequest)

    val updated = hakuService.update(hakuRequest.haku, getIfUnmodifiedSince)
    Ok("updated" -> updated)
  }

  private def authenticateExternal(request: ExternalRequest): Authenticated = {
    val session = authenticate().session

    val acceptedRoles = Set(Role.External, Role.Paakayttaja)
    if (session.roles.intersect(acceptedRoles).isEmpty) {
      throw RoleAuthorizationFailedException(acceptedRoles.toSeq, session.roles)
    }
    request.authenticated.copy(id = s"kouta-external-${request.authenticated.id}")
  }
}
