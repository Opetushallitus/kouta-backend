package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain.oid.HakukohdeOid
import fi.oph.kouta.service.HakemuspalveluService

class HakemuspalveluServlet(hakemuspalveluService: HakemuspalveluService) extends KoutaServlet {

  def this() = this(HakemuspalveluService)

  registerPath("/hakemuspalvelu/hakukohde/{oid}",
    """    get:
      |      summary: Hakee hakukohteen tietoja hakemuspalvelusta
      |      operationId: Hakee hakukohteen tietoja hakemuspalvelusta
      |      description: Hakee hakukohteen tietoja hakemuspalvelusta
      |      tags:
      |        - Hakemuspalvelu
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Hakukohde-oid
      |          example: 1.2.246.562.20.00000000000000000009
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: object
      |                properties:
      |                  hakemustenMaara:
      |                    type: interger
      |                    format: int32
      |                    minimum: 0
      |""".stripMargin)
  get("/hakukohde/:oid") {

    implicit val authenticated: Authenticated = authenticate()
    val hakukohdeOid = HakukohdeOid(params("oid"))

    hakemuspalveluService.getHakukohdeInfo(hakukohdeOid)

  }

}
