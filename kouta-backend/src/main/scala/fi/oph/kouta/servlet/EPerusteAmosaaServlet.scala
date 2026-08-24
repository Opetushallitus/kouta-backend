package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.client.EPerusteAmosaaClient
import org.scalatra.Ok

class EPerusteAmosaaServlet(amosaaClient: EPerusteAmosaaClient) extends KoutaServlet {
  def this() = this(EPerusteAmosaaClient)

  registerPath(
    "/eperuste-amosaa/opetussuunnitelmat",
    """    get:
      |      summary: Hae opetussuunnitelmia eperusteet-amosaa-palvelusta parametrien perusteella sivutettuna
      |      operationId: getOpetussuunnitelmat
      |      description: Hakee opetussuunnitelmat eperusteet-amosaa-palvelusta organisaatio-oidien perusteella
      |      tags:
      |        - EPerusteAmosaa
      |      parameters:
      |        - in: query
      |          name: organisaatio
      |          schema:
      |            type: string
      |          required: false
      |          description: Organisaation OID
      |        - in: query
      |          name: nimi
      |          schema:
      |            type: string
      |          required: false
      |          description: Opetussunnitelman nimen osa
      |        - in: query
      |          name: sivu
      |          schema:
      |            type: integer
      |          required: false
      |          description: Sivunumero (oletus 1)
      |      responses:
      |        '200':
      |          description: Opetussuunnitelmat
      |          content:
      |            application/json:
      |              schema:
      |                $ref: '#/components/schemas/AmosaaOpetussuunnitelmatResponse'
      |""".stripMargin
  )
  get("/opetussuunnitelmat") {
    implicit val authenticated: Authenticated = authenticate()
    val organisaatio = params.get("organisaatio")
    val nimi = params.get("nimi")
    val sivu = params.get("sivu")

    Ok(amosaaClient.getOpetussuunnitelmat(organisaatio, nimi, sivu))
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
      |          description: Paikalliset tutkinnon osat
      |          content:
      |            application/json:
      |              schema:
      |                $ref: '#/components/schemas/AmosaaPaikallisetTutkinnonOsatResponse'
      |""".stripMargin
  )
  get("/opetussuunnitelma/:opsId/paikalliset-tutkinnonosat") {
    implicit val authenticated: Authenticated = authenticate()
    val opsId = params("opsId").toLong
    Ok(amosaaClient.getPaikallisetTutkinnonosat(opsId))
  }
}
