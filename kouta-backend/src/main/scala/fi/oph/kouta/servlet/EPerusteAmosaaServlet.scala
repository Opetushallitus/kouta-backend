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
      |          name: organisaatiot
      |          schema:
      |            type: array
      |            items:
      |              type: string
      |          style: form
      |          explode: true
      |          required: false
      |          description: Lista organisaatioiden OIDeja
      |        - in: query
      |          name: nimi
      |          schema:
      |            type: string
      |          required: false
      |          description: Opetussunnitelman nimen osa
      |        - in: query
      |          name: paikallistasisaltoa
      |          schema:
      |            type: boolean
      |          required: false
      |          description: Suodattaa paikallisten tutkinnon osien perusteella. true = sisältää, false = ei sisällä, pois jätettynä suodatinta ei käytetä.
      |        - in: query
      |          name: sivu
      |          schema:
      |            type: integer
      |          required: false
      |          description: Sivunumero (oletus 0)
      |        - in: query
      |          name: sivukoko
      |          schema:
      |            type: integer
      |          required: false
      |          description: Sivun koko (oletus 15)
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
    val organisaatiot = multiParams.get("organisaatiot").map(_.toSet).getOrElse(Set.empty[String])
    val nimi = params.get("nimi")
    val sivu = params.get("sivu").getOrElse("0")
    val sivukoko = params.get("sivukoko").getOrElse("15")
    val paikallistaSisaltoa = params.get("paikallistasisaltoa").map(_.toBoolean)

    Ok(amosaaClient.getOpetussuunnitelmat(organisaatiot, nimi, paikallistaSisaltoa, sivu, sivukoko))
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
