package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.client.EPerusteAmosaaClient
import org.scalatra.Ok

class EPerusteAmosaaServlet(amosaaClient: EPerusteAmosaaClient) extends KoutaServlet {
  def this() = this(EPerusteAmosaaClient)

  registerPath(
    "/eperuste-amosaa/opetussuunnitelmat",
    """    get:
      |      summary: Hae opetussuunnitelmat eperusteet-amosaa-palvelusta
      |      operationId: getOpetussuunnitelmat
      |      description: Hakee opetussuunnitelmat eperusteet-amosaa-palvelusta organisaatio-oidien perusteella
      |      tags:
      |        - EPerusteAmosaa
      |      parameters:
      |        - in: query
      |          name: organisaatio
      |          schema:
      |            type: string
      |          required: true
      |          description: Organisaation OID
      |          example: "1.2.246.562.10.00000000001"
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin
  )
  get("/opetussuunnitelmat") {
    implicit val authenticated: Authenticated = authenticate()
    val organisaatio = params("organisaatio")
    Ok(amosaaClient.getOpetussuunnitelmat(organisaatio))
  }
}
