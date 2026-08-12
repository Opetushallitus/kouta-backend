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

  registerPath(
    "/eperuste-amosaa/opetussuunnitelma/{opsId}/paikalliset-tutkinnonosat",
    """    get:
      |      summary: Hae opetussuunnitelman paikalliset tutkinnon osat eperusteet-amosaa-palvelusta
      |      operationId: getPaikallisetTutkinnonosat
      |      description: Hakee opetussuunnitelman paikalliset tutkinnon osat eperusteet-amosaa-palvelusta
      |      tags:
      |        - EPerusteAmosaa
      |      parameters:
      |        - in: path
      |          name: opsId
      |          schema:
      |            type: integer
      |          required: true
      |          description: Opetussuunnitelman id
      |          example: 5574388
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin
  )
  get("/opetussuunnitelma/:opsId/paikalliset-tutkinnonosat") {
    implicit val authenticated: Authenticated = authenticate()
    val opsId = params("opsId").toLong
    Ok(amosaaClient.getPaikallisetTutkinnonosat(opsId))
  }
}
