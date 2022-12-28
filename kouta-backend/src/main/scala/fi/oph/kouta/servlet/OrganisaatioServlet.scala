package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.client.OrganisaatioServiceQueryException
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.service.{OrganisaatioServiceImpl => OrganisaatioService}
import org.scalatra.{ActionResult, NotFound, Ok}

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

    Ok(organisaatioService.getOrganisaatio(OrganisaatioOid(params("oid"))))
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
      |                  $ref: '#/components/schemas/Organisaatio'
      |""".stripMargin)
  post("/organisaatiot") {

    implicit val authenticated: Authenticated = authenticate()

    organisaatioService.getOrganisaatiot(parsedBody.extract[Seq[OrganisaatioOid]]) match {
      case Left(e: OrganisaatioServiceQueryException) => ActionResult(status = e.status, body = e.message, headers = Map())
      case Right(organisaatiot) => Ok(organisaatiot)
      case Left(e) => throw e
    }
  }

  registerPath("/organisaatio/hierarkia",
    """    get:
      |      summary: Hae organisaatiohierarkian organisaatiopalvelusta
      |      operationId: getHierarkia
      |      description: Hakee organisaatiohierarkian organisaatiopalvelusta
      |      tags:
      |        - Organisaatio
      |      parameters:
      |        - in: query
      |          name: searchStr
      |          schema:
      |            type: string
      |          required: false
      |          description: Hakumerkkijono
      |          example: "Tampereen yliopisto"
      |        - in: query
      |          name: oid
      |          schema:
      |            type: string
      |          required: false
      |          description: Haettavan organisaation oid
      |          example: "1.2.246.562.10.60198812360"
      |        - in: query
      |          name: oidRestrictionList
      |          style: form
      |          explode: false
      |          schema:
      |            type: array
      |            items:
      |              type: string
      |          required: false
      |          description: Lista haettavien organisaatioiden oideja
      |          example: ["1.2.246.562.10.60198812368", "1.2.246.562.10.60198812360"]
      |        - in: query
      |          name: aktiiviset
      |          schema:
      |            type: boolean
      |          required: false
      |          description: Otetaanko aktiiviset organisaatiot mukaan hakutuloksiin
      |          example: false
      |        - in: query
      |          name: suunnitellut
      |          schema:
      |            type: boolean
      |          required: false
      |          description: Otetaanko suunnitellut organisaatiot mukaan hakutuloksiin
      |          example: false
      |        - in: query
      |          name: lakkautetut
      |          schema:
      |            type: boolean
      |          required: false
      |          description: Otetaanko lakkautetut organisaatiot mukaan hakutuloksiin
      |          example: false
      |        - in: query
      |          name: skipParents
      |          schema:
      |            type: boolean
      |          required: false
      |          description: Jätetäänkö yläorganisaatiot pois hakutuloksista
      |          example: false
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                  $ref: '#/components/schemas/OrganisaatioHierarkia'
      |""".stripMargin)
  get("/hierarkia") {

    implicit val authenticated: Authenticated = authenticate()
    Ok(organisaatioService.getOrganisaatioHierarkia(params, multiParams))
  }

  registerPath("/organisaatio/oppilaitokset-for-avoin-korkeakoulutus",
    """    get:
      |      summary: Listaa avoimen korkeakoulutuksen oppilaitokset (ei osia)
      |      operationId: getHierarkiaForAvoinKorkeakoulutus
      |      description: Listaa avoimen korkeakoulutuksen oppilaitokset organisaatiopalvelusta
      |      tags:
      |        - Organisaatio
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                  $ref: '#/components/schemas/OrganisaatioHierarkia'
      |""".stripMargin)
  get("/oppilaitokset-for-avoin-korkeakoulutus") {
    implicit val authenticated: Authenticated = authenticate()
    Ok(organisaatioService.getOppilaitoksetForAvoinKorkeakoulutus())
  }
}
