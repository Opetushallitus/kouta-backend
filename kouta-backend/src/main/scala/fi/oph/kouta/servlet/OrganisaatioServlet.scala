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
      |                $ref: '#/components/schemas/Organisaatio'
      |""".stripMargin)
  get("/:oid") {

    implicit val authenticated: Authenticated = authenticate()

    organisaatioService.get(OrganisaatioOid(params("oid"))) match {
      case None => NotFound("error" -> "Unknown organisaatio oid")
      case Some(organisaatio) => Ok(organisaatio)
    }
  }
}
