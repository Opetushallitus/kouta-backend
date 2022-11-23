package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.service.{OrganisaatioServiceImpl => OrganisaatioService}
import org.scalatra.{NotFound, Ok}

class OrganisaatioServlet(organisaatioService: OrganisaatioService) extends KoutaServlet {
  def this() = this(OrganisaatioService)

  registerPath("/organisaatio/{oid}",
    """    get:
      |      summary: Hae organisaation tiedot organisaatiopalvelusta
      |      operationId: getOrganisaatio
      |      description: Hakee organisaation tiedot organisaatiopalvelusta
      |      tags:
      |        - Organisaatio
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
      |                $ref: '#/components/schemas/OrganisaatioServiceOrganisaatio'
      |""".stripMargin)
  get("/:oid") {

    implicit val authenticated: Authenticated = authenticate()

    organisaatioService.get(OrganisaatioOid(params("oid"))) match {
      case None => NotFound("error" -> "Unknown organisaatio oid")
      case Some(organisaatio) => Ok(organisaatio)
    }
  }

  registerPath("/organisaatio/organisaatiot",
    """    post:
      |      summary: Hae organisaatioiden tiedot organisaatiopalvelusta
      |      operationId: getOrganisaatiot
      |      description: Hakee organisaatioiden tiedot organisaatiopalvelusta
      |      tags:
      |        - Organisaatio
      |      requestBody:
      |        description: Lista haettavien organisaatioiden oideja
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: array
      |              items:
      |                type: string
      |              example: ["1.2.246.562.10.60198812368", "1.2.246.562.10.60198812360"]
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/OrganisaatioServiceOrganisaatio'
      |""".stripMargin)
  post("/organisaatiot") {

    implicit val authenticated: Authenticated = authenticate()

    organisaatioService.get(parsedBody.extract[List[OrganisaatioOid]]) match {
      case List() => NotFound("error" -> "Unknown organisaatio oids")
      case organisaatiot => Ok(organisaatiot)
    }
  }
}
