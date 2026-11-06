package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.client.{AmosaaOpetussuunnitelmatResponse, EPerusteAmosaaClient}
import fi.oph.kouta.domain.{Fi, Kieli}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.service.OrganisaatioServiceImpl
import org.scalatra.Ok

import scala.util.Try

class EPerusteAmosaaServlet(amosaaClient: EPerusteAmosaaClient, organisaatioService: OrganisaatioServiceImpl) extends KoutaServlet {
  def this() = this(EPerusteAmosaaClient, OrganisaatioServiceImpl)

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
      |          description: Lista organisaatioiden OIDeja, joiden käytössä olevat opetussuunnitelmat haetaan. Huomioitavia oppilaitoksia ovat ne, joiden oppilaitostyyppi sallii tutkinnon osa -koulutusten järjestämisen. Koulutustoimijalle haetaan koulutustoimijan ja sen alla olevien huomioitavien oppilaitosten opetussuunnitelmat, oppilaitokselle oppilaitoksen opetussuunnitelmat jos se on huomioitava oppilaitos ja toimipisteelle sen yläpuolella olevan oppilaitoksen opetussuunnitelmat jos se on huomioitava oppilaitos. Jos sopivia organisaatioita ei löydy, palautetaan tyhjä lista.
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
      |        - in: query
      |          name: kieli
      |          schema:
      |            type: string
      |          required: false
      |          description: Kieli jolla opetussuunnitelmia haetaan (oletus fi)
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
    val organisaatioOids = multiParams.get("organisaatiot").map(_.toSet).getOrElse(Set.empty[String])
    val nimi = params.get("nimi")
    val sivu = params.get("sivu").getOrElse("0")
    val sivukoko = params.get("sivukoko").getOrElse("15")
    val paikallistaSisaltoa = params.get("paikallistasisaltoa").map(_.toBoolean)
    val kieli = params.get("kieli").map(Kieli.withName).getOrElse(Fi)
    val rajausOids =
      if (organisaatioOids.isEmpty) Set.empty[String]
      else
        organisaatioService
          .findOrganisaatioOidsForAmmatillisetOpetussuunnitelmat(organisaatioOids.map(OrganisaatioOid))
          .map(_.toString)

    if (organisaatioOids.nonEmpty && rajausOids.isEmpty) {
      Ok(
        AmosaaOpetussuunnitelmatResponse(
          data = List(),
          sivuja = Some(0),
          `kokonaismäärä` = Some(0),
          sivu = Try(sivu.toInt).toOption,
          sivukoko = Try(sivukoko.toInt).toOption
        )
      )
    } else {
      Ok(amosaaClient.getOpetussuunnitelmat(rajausOids, nimi, paikallistaSisaltoa, sivu, sivukoko, kieli))
    }
  }

  registerPath(
    "/eperuste-amosaa/opetussuunnitelma/{opsId}",
    """    get:
      |      summary: Hae yksittäinen opetussuunnitelma eperusteet-amosaa-palvelusta
      |      operationId: getOpetussuunnitelma
      |      description: Hakee yksittäisen opetussuunnitelman eperusteet-amosaa-palvelusta tunnisteen perusteella
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
      |          description: Opetussuunnitelma
      |          content:
      |            application/json:
      |              schema:
      |                $ref: '#/components/schemas/AmosaaOpetussuunnitelma'
      |""".stripMargin
  )
  get("/opetussuunnitelma/:opsId") {
    implicit val authenticated: Authenticated = authenticate()
    val opsId = params("opsId").toLong
    Ok(amosaaClient.getOpetussuunnitelma(opsId))
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
