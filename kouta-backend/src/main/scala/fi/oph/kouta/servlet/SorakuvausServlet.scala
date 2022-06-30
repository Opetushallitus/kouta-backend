package fi.oph.kouta.servlet

import java.util.UUID
import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain.{Sorakuvaus, TilaFilter}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.service.SorakuvausService
import org.scalatra.{NotFound, Ok}

class SorakuvausServlet(sorakuvausService: SorakuvausService) extends KoutaServlet {

  def this() = this(SorakuvausService)

  registerPath("/sorakuvaus/{id}",
    """    get:
      |      summary: Hae SORA-kuvauksen tiedot
      |      operationId: Hae sorakuvaus
      |      description: Hakee SORA-kuvauksen kaikki tiedot
      |      tags:
      |        - Sorakuvaus
      |      parameters:
      |        - in: path
      |          name: id
      |          schema:
      |            type: string
      |          required: true
      |          description: Sorakuvaus-id
      |          example: ea596a9c-5940-497e-b5b7-aded3a2352a7
      |        - in: query
      |          name: myosPoistetut
      |          schema:
      |            type: boolean
      |            default: false
      |          required: false
      |          description: Palautetaanko myös mahdollisesti poistettu SORA-kuvaus
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                $ref: '#/components/schemas/Sorakuvaus'
      |""".stripMargin)
  get("/:id") {

    implicit val authenticated: Authenticated = authenticate()

    val myosPoistetut = params.getOrElse("myosPoistetut", "false").toBoolean
    sorakuvausService.get(UUID.fromString(params("id")), TilaFilter.alsoPoistetutAddedToOthers(myosPoistetut)) match {
      case None => NotFound("error" -> "Unknown SORA-kuvaus id")
      case Some((k, l)) => Ok(k, headers = Map(KoutaServlet.LastModifiedHeader -> createLastModifiedHeader(l)))
    }
  }

  registerPath( "/sorakuvaus/",
    """    put:
      |      summary: Tallenna uusi SORA-kuvaus
      |      operationId: Tallenna uusi sorakuvaus
      |      description: Tallenna uuden SORA-kuvauksen tiedot.
      |        Rajapinta palauttaa SORA-kuvaukselle generoidun yksilöivän id:n
      |      tags:
      |        - Sorakuvaus
      |      requestBody:
      |        description: Tallennettava SORA-kuvaus
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/Sorakuvaus'
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
      |                    description: Uuden SORA-kuvauksen yksilöivä id
      |                    example: ea596a9c-5940-497e-b5b7-aded3a2352a7
      |""".stripMargin)
  put("/") {

    implicit val authenticated: Authenticated = authenticate()

    sorakuvausService.put(parsedBody.extract[Sorakuvaus]) match {
      case id => Ok("id" -> id)
    }
  }

  registerPath("/sorakuvaus/",
    """    post:
      |      summary: Muokkaa olemassa olevaa SORA-kuvausta
      |      operationId: Muokkaa sorakuvausta
      |      description: Muokkaa olemassa olevaa SORA-kuvausta. Rajapinnalle annetaan SORA-kuvauksen kaikki tiedot,
      |        ja muuttuneet tiedot tallennetaan kantaan.
      |      tags:
      |        - Sorakuvaus
      |      requestBody:
      |        description: Muokattavan SORA-kuvauksen kaikki tiedot. Kantaan tallennetaan muuttuneet tiedot.
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/Sorakuvaus'
      |      responses:
      |        '200':
      |          description: O
      |""".stripMargin)
  post("/") {

    implicit val authenticated: Authenticated = authenticate()

    sorakuvausService.update(parsedBody.extract[Sorakuvaus], getIfUnmodifiedSince) match {
      case updated => Ok("updated" -> updated)
    }
  }

  registerPath("/sorakuvaus/list",
    """    get:
      |      summary: Listaa organisaation käytettävissä olevat SORA-kuvaukset
      |      operationId: Listaa sorakuvaukset
      |      description: Listaa niiden SORA-kuvausten tiedot, jotka ovat organisaation käytettävissä.
      |      tags:
      |        - Sorakuvaus
      |      parameters:
      |        - in: query
      |          name: organisaatioOid
      |          schema:
      |            type: string
      |          required: true
      |          description: Organisaatio-oid
      |          example: 1.2.246.562.10.00101010101
      |        - in: query
      |          name: myosArkistoidut
      |          schema:
      |            type: boolean
      |            default: true
      |          required: false
      |          description: Listataanko myös arkistoidut sorakuvaukset
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/SorakuvausListItem'
      |""".stripMargin)
  get("/list") {

    implicit val authenticated: Authenticated = authenticate()

    (params.get("organisaatioOid"), params.getOrElse("myosArkistoidut", "true").toBoolean) match {
      case (None, _) => NotFound()
      case (Some(oid), myosArkistoidut) => Ok(sorakuvausService.list(OrganisaatioOid(oid),
        TilaFilter.alsoArkistoidutAddedToOlemassaolevat(myosArkistoidut)))
    }
  }
}
