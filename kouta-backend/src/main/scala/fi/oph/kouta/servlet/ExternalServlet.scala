package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.config.{KoutaConfigurationFactory, SecurityConfiguration}
import fi.oph.kouta.domain.{
  ExternalHakuRequest,
  ExternalHakukohdeRequest,
  ExternalKoulutusRequest,
  ExternalRequest,
  ExternalSorakuvausRequest,
  ExternalToteutusRequest,
  ExternalValintaperusteRequest,
  Haku,
  Sorakuvaus,
  Valintaperuste
}
import fi.oph.kouta.domain.oid.RootOrganisaatioOid
import fi.oph.kouta.security.Role
import fi.oph.kouta.service._
import fi.vm.sade.scalaproperties.OphProperties
import org.scalatra.{ActionResult, Ok}

class ExternalServlet(
    koulutusService: KoulutusService,
    toteutusService: ToteutusService,
    hakuService: HakuService,
    hakukohdeService: HakukohdeService,
    valintaperusteService: ValintaperusteService,
    sorakuvausService: SorakuvausService
) extends KoutaServlet {

  def this() =
    this(KoulutusService, ToteutusService, HakuService, HakukohdeService, ValintaperusteService, SorakuvausService)

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
      |                koulutus:
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
    if (securityConfiguration.externalApiModifyEnabled) {
      val koulutusRequest                       = parsedBody.extract[ExternalKoulutusRequest]
      implicit val authenticated: Authenticated = authenticateExternal(koulutusRequest)

      koulutusService.put(koulutusRequest.koulutus) match {
        case oid => Ok("oid" -> oid)
      }
    } else {
      ActionResult(403, "Rajapinnan käyttö estetty tässä ympäristössä", Map.empty)
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
      |                koulutus:
      |                  type: object
      |                  $ref: '#/components/schemas/Koulutus'
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin
  )
  post("/koulutus") {
    if (securityConfiguration.externalApiModifyEnabled) {
      val koulutusRequest                       = parsedBody.extract[ExternalKoulutusRequest]
      implicit val authenticated: Authenticated = authenticateExternal(koulutusRequest)

      val updated = koulutusService.update(koulutusRequest.koulutus, getIfUnmodifiedSince)
      Ok("updated" -> updated)
    } else {
      ActionResult(403, "Rajapinnan käyttö estetty tässä ympäristössä", Map.empty)
    }
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
      |                toteutus:
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
    if (securityConfiguration.externalApiModifyEnabled) {
      val toteutusRequest                       = parsedBody.extract[ExternalToteutusRequest]
      implicit val authenticated: Authenticated = authenticateExternal(toteutusRequest)

      toteutusService.put(toteutusRequest.toteutus) match {
        case oid => Ok("oid" -> oid)
      }
    } else {
      ActionResult(403, "Rajapinnan käyttö estetty tässä ympäristössä", Map.empty)
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
      |                toteutus:
      |                  type: object
      |                  $ref: '#/components/schemas/Toteutus'
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin
  )
  post("/toteutus") {
    if (securityConfiguration.externalApiModifyEnabled) {
      val toteutusRequest                       = parsedBody.extract[ExternalToteutusRequest]
      implicit val authenticated: Authenticated = authenticateExternal(toteutusRequest)

      val updated = toteutusService.update(toteutusRequest.toteutus, getIfUnmodifiedSince)
      Ok("updated" -> updated)
    } else {
      ActionResult(403, "Rajapinnan käyttö estetty tässä ympäristössä", Map.empty)
    }
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
    if (securityConfiguration.externalApiModifyEnabled) {
      val hakuRequest                           = parsedBody.extract[ExternalHakuRequest]
      implicit val authenticated: Authenticated = authenticateExternal(hakuRequest)

      hakuService.put(hakuRequest.haku) match {
        case oid => Ok("oid" -> oid)
      }
    } else {
      ActionResult(403, "Rajapinnan käyttö estetty tässä ympäristössä", Map.empty)
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
      |""".stripMargin
  )
  post("/haku") {
    if (securityConfiguration.externalApiModifyEnabled) {
      val hakuRequest                           = parsedBody.extract[ExternalHakuRequest]
      implicit val authenticated: Authenticated = authenticateExternal(hakuRequest)

      val updated = hakuService.update(hakuRequest.haku, getIfUnmodifiedSince)
      Ok("updated" -> updated)
    } else {
      ActionResult(403, "Rajapinnan käyttö estetty tässä ympäristössä", Map.empty)
    }
  }

  registerPath(
    "/external/hakukohde",
    """    put:
      |      summary: Tallenna uusi hakukohde
      |      operationId: Tallenna uusi hakukohde
      |      description: Tallenna uuden hakukohteen tiedot.
      |        Rajapinta palauttaa hakukohteelle generoidun yksilöivän hakukohde-oidin.
      |      tags:
      |        - External
      |      requestBody:
      |        description: Tallennettava hakukohde
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: object
      |              properties:
      |                authenticated:
      |                  type: object
      |                  $ref: '#/components/schemas/Authenticated'
      |                hakukohde:
      |                  type: object
      |                  $ref: '#/components/schemas/Hakukohde'
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
      |""".stripMargin
  )
  put("/hakukohde") {
    if (securityConfiguration.externalApiModifyEnabled) {
      val hakukohdeRequest                      = parsedBody.extract[ExternalHakukohdeRequest]
      implicit val authenticated: Authenticated = authenticateExternal(hakukohdeRequest)

      hakukohdeService.put(hakukohdeRequest.hakukohde) match {
        case oid => Ok("oid" -> oid)
      }
    } else {
      ActionResult(403, "Rajapinnan käyttö estetty tässä ympäristössä", Map.empty)
    }
  }

  registerPath(
    "/external/hakukohde",
    """    post:
      |      summary: Muokkaa olemassa olevaa hakukohdetta
      |      operationId: Muokkaa hakukohdetta
      |      description: Muokkaa olemassa olevaa hakukohdetta. Rajapinnalle annetaan hakukohteen kaikki tiedot,
      |        ja muuttuneet tiedot tallennetaan kantaan.
      |      tags:
      |        - External
      |      requestBody:
      |        description: Muokattavan hakukohteen kaikki tiedot. Kantaan tallennetaan muuttuneet tiedot.
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: object
      |              properties:
      |                authenticated:
      |                  type: object
      |                  $ref: '#/components/schemas/Authenticated'
      |                hakukohde:
      |                  type: object
      |                  $ref: '#/components/schemas/Hakukohde'
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin
  )
  post("/hakukohde") {
    if (securityConfiguration.externalApiModifyEnabled) {
      val hakukohdeRequest                      = parsedBody.extract[ExternalHakukohdeRequest]
      implicit val authenticated: Authenticated = authenticateExternal(hakukohdeRequest)

      val updated = hakukohdeService.update(hakukohdeRequest.hakukohde, getIfUnmodifiedSince)
      Ok("updated" -> updated)
    } else {
      ActionResult(403, "Rajapinnan käyttö estetty tässä ympäristössä", Map.empty)
    }
  }

  registerPath(
    "/external/valintaperuste/",
    """    put:
      |      summary: Tallenna uusi valintaperustekuvaus
      |      operationId: Tallenna uusi valintaperuste
      |      description: Tallenna uuden valintaperustekuvauksen tiedot.
      |        Rajapinta palauttaa valintaperustekuvaukselle generoidun yksilöivän id:n
      |      tags:
      |        - External
      |      requestBody:
      |        description: Tallennettava valintaperustekuvaus
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: object
      |              properties:
      |                authenticated:
      |                  type: object
      |                  $ref: '#/components/schemas/Authenticated'
      |                valintaperuste:
      |                  type: object
      |                  $ref: '#/components/schemas/Valintaperuste'
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
      |""".stripMargin
  )
  put("/valintaperuste") {
    if (securityConfiguration.externalApiModifyEnabled) {
      val valintaperusteRequest                 = parsedBody.extract[ExternalValintaperusteRequest]
      implicit val authenticated: Authenticated = authenticateExternal(valintaperusteRequest)

      valintaperusteService.put(valintaperusteRequest.valintaperuste) match {
        case id => Ok("id" -> id)
      }
    } else {
      ActionResult(403, "Rajapinnan käyttö estetty tässä ympäristössä", Map.empty)
    }
  }

  registerPath(
    "/external/valintaperuste/",
    """    post:
      |      summary: Muokkaa olemassa olevaa valintaperustekuvausta
      |      operationId: Muokkaa valintaperustetta
      |      description: Muokkaa olemassa olevaa valintaperustekuvausta. Rajapinnalle annetaan valintaperusteen kaikki tiedot,
      |        ja muuttuneet tiedot tallennetaan kantaan.
      |      tags:
      |        - External
      |      requestBody:
      |        description: Muokattavan valintaperustekuvauksen kaikki tiedot. Kantaan tallennetaan muuttuneet tiedot.
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: object
      |              properties:
      |                authenticated:
      |                  type: object
      |                  $ref: '#/components/schemas/Authenticated'
      |                valintaperuste:
      |                  type: object
      |                  $ref: '#/components/schemas/Valintaperuste'
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin
  )
  post("/valintaperuste") {
    if (securityConfiguration.externalApiModifyEnabled) {
      val valintaperusteRequest                 = parsedBody.extract[ExternalValintaperusteRequest]
      implicit val authenticated: Authenticated = authenticateExternal(valintaperusteRequest)

      val updated = valintaperusteService.update(valintaperusteRequest.valintaperuste, getIfUnmodifiedSince)
      Ok("updated" -> updated)
    } else {
      ActionResult(403, "Rajapinnan käyttö estetty tässä ympäristössä", Map.empty)
    }
  }

  registerPath(
    "/external/sorakuvaus/",
    """    put:
      |      summary: Tallenna uusi SORA-kuvaus
      |      operationId: Tallenna uusi sorakuvaus
      |      description: Tallenna uuden SORA-kuvauksen tiedot.
      |        Rajapinta palauttaa SORA-kuvaukselle generoidun yksilöivän id:n
      |      tags:
      |        - External
      |      requestBody:
      |        description: Tallennettava SORA-kuvaus
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: object
      |              properties:
      |                authenticated:
      |                  type: object
      |                  $ref: '#/components/schemas/Authenticated'
      |                sorakuvaus:
      |                  type: object
      |                  $ref: '#/components/schemas/Sorakuvaus'
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
      |                    description: Uuden SORA-kuvauksen yksilöivä id
      |                    example: ea596a9c-5940-497e-b5b7-aded3a2352a7
      |""".stripMargin
  )
  put("/sorakuvaus") {
    if (securityConfiguration.externalApiModifyEnabled) {
      val sorakuvausRequest                     = parsedBody.extract[ExternalSorakuvausRequest]
      implicit val authenticated: Authenticated = authenticateExternal(sorakuvausRequest)

      sorakuvausService.put(sorakuvausRequest.sorakuvaus) match {
        case id => Ok("id" -> id)
      }
    } else {
      ActionResult(403, "Rajapinnan käyttö estetty tässä ympäristössä", Map.empty)
    }
  }

  registerPath(
    "/external/sorakuvaus/",
    """    post:
      |      summary: Muokkaa olemassa olevaa SORA-kuvausta
      |      operationId: Muokkaa sorakuvausta
      |      description: Muokkaa olemassa olevaa SORA-kuvausta. Rajapinnalle annetaan SORA-kuvauksen kaikki tiedot,
      |        ja muuttuneet tiedot tallennetaan kantaan.
      |      tags:
      |        - External
      |      requestBody:
      |        description: Muokattavan SORA-kuvauksen kaikki tiedot. Kantaan tallennetaan muuttuneet tiedot.
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: object
      |              properties:
      |                authenticated:
      |                  type: object
      |                  $ref: '#/components/schemas/Authenticated'
      |                sorakuvaus:
      |                  type: object
      |                  $ref: '#/components/schemas/Sorakuvaus'
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin
  )
  post("/sorakuvaus") {
    if (securityConfiguration.externalApiModifyEnabled) {
      val sorakuvausRequest                     = parsedBody.extract[ExternalSorakuvausRequest]
      implicit val authenticated: Authenticated = authenticateExternal(sorakuvausRequest)

      val updated = sorakuvausService.update(sorakuvausRequest.sorakuvaus, getIfUnmodifiedSince)
      Ok("updated" -> updated)
    } else {
      ActionResult(403, "Rajapinnan käyttö estetty tässä ympäristössä", Map.empty)
    }
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
