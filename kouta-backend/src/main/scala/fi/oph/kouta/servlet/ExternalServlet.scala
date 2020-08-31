package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain.Haku
import fi.oph.kouta.domain.oid.RootOrganisaatioOid
import fi.oph.kouta.security.Role
import fi.oph.kouta.service._
import org.scalatra.Ok

trait ExternalRequest {
  def authenticated: Authenticated
}

case class ExternalHakuRequest(authenticated: Authenticated, haku: Haku) extends ExternalRequest

class ExternalServlet(hakuService: HakuService) extends KoutaServlet {

  def this() = this(HakuService)

  val externalRole: Role = Role.Indexer

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

  private def authenticateExternal[R <: ExternalRequest](request: R): Authenticated = {
    val session = authenticate().session

    session.roleMap.get(externalRole) match {
      case None =>
        throw RoleAuthorizationFailedException(Seq(externalRole), session.roles)
      case Some(orgs) =>
        if (!orgs.contains(RootOrganisaatioOid)) {
          throw OrganizationAuthorizationFailedException(Seq(RootOrganisaatioOid), orgs)
        }
        request.authenticated.copy(id = s"kouta-external-${request.authenticated.id}")
    }
  }
}
