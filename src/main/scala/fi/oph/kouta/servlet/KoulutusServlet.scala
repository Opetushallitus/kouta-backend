package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid}
import fi.oph.kouta.service.KoulutusService
import org.scalatra.{NotFound, Ok}

class KoulutusServlet(koulutusService: KoulutusService) extends KoutaServlet {

  def this() = this(KoulutusService)

  registerPath( "/koulutus/{oid}",
    """    get:
      |      summary: Hae koulutus
      |      description: Hae koulutuksen tiedot annetulla koulutus-oidilla
      |      operationId: Hae koulutus
      |      tags:
      |        - Koulutus
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Koulutus-oid
      |          example: 1.2.246.562.13.00000000000000000009
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                $ref: '#/components/schemas/Koulutus'
      |""".stripMargin)
  get("/:oid") {

    implicit val authenticated: Authenticated = authenticate

    koulutusService.get(KoulutusOid(params("oid"))) match {
      case None => NotFound("error" -> "Unknown koulutus oid")
      case Some((k, l)) => Ok(k, headers = Map(KoutaServlet.LastModifiedHeader -> createLastModifiedHeader(l)))
    }
  }

  registerPath( "/koulutus/",
    """    put:
      |      summary: Tallenna uusi koulutus
      |      operationId: Tallenna uusi koulutus
      |      description: Tallenna uuden koulutuksen tiedot.
      |        Rajapinta palauttaa koulutukselle generoidun yksilöivän koulutus-oidin.
      |      tags:
      |        - Koulutus
      |      requestBody:
      |        description: Tallennettava koulutus
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/Koulutus'
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
      |                    description: Uuden koulutuksen yksilöivä oid
      |                    example: 1.2.246.562.13.00000000000000000009
      |""".stripMargin)
  put("/") {

    implicit val authenticated: Authenticated = authenticate

    koulutusService.put(parsedBody.extract[Koulutus]) match {
      case oid => Ok("oid" -> oid)
    }
  }

  registerPath("/koulutus/",
    """    post:
      |      summary: Muokkaa olemassa olevaa koulutusta
      |      operationId: Muokkaa koulutusta
      |      description: Muokkaa olemassa olevaa koulutusta. Rajapinnalle annetaan koulutuksen kaikki tiedot,
      |        ja muuttuneet tiedot tallennetaan kantaan.
      |      tags:
      |        - Koulutus
      |      requestBody:
      |        description: Muokattavan koulutuksen kaikki tiedot. Kantaan tallennetaan muuttuneet tiedot.
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              $ref: '#/components/schemas/Koulutus'
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin)
  post("/") {

    implicit val authenticated: Authenticated = authenticate

    koulutusService.update(parsedBody.extract[Koulutus], getIfUnmodifiedSince) match {
      case updated => Ok("updated" -> updated)
    }
  }

  registerPath( "/koulutus/list",
    """    get:
      |      summary: Listaa organisaation käytettävissä olevat koulutukset
      |      operationId: Listaa koulutukset
      |      description: Listaa niiden koulutusten tiedot, jotka ovat organisaation käytettävissä
      |        esim. uutta toteutusta luotaessa
      |      tags:
      |        - Koulutus
      |      parameters:
      |        - in: query
      |          name: organisaatioOid
      |          schema:
      |            type: string
      |          required: true
      |          description: Organisaatio-oid
      |          example: 1.2.246.562.10.00101010101
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/KoulutusListItem'
      |""".stripMargin)
  get("/list") {

    implicit val authenticated: Authenticated = authenticate

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(oid) => Ok(koulutusService.list(oid))
    }
  }

  registerPath( "/koulutus/{oid}/toteutukset/list",
    """    get:
      |      summary: Listaa organisaation käytettävissä olevat tietyn koulutuksen toteutukset
      |      operationId: Listaa koulutuksen toteutukset
      |      description: Listaa ne tietyn koulutuksen toteutukset, jotka ovat organisaation käytettävissä.
      |      tags:
      |        - Koulutus
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Koulutus-oid
      |          example: 1.2.246.562.13.00000000000000000009
      |        - in: query
      |          name: organisaatioOid
      |          schema:
      |            type: string
      |          required: true
      |          description: Organisaatio-oid
      |          example: 1.2.246.562.10.00101010101
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/ToteutusListItem'
      |""".stripMargin)
  get("/:oid/toteutukset/list") {

    implicit val authenticated: Authenticated = authenticate

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) => Ok(koulutusService.listToteutukset(KoulutusOid(params("oid")), organisaatioOid))
    }
  }

  registerPath("/koulutus/{oid}/tarjoajat/{tarjoajaOid}",
    s"""    put:
       |      summary: Lisää koulutukseen tarjoajan
       |      operationId: Lisää koulutukseen tarjoajan
       |      description: Lisää koulutukseen tarjoajan
       |      tags:
       |        - Koulutus
       |      parameters:
       |        - in: path
       |          name: oid
       |          schema:
       |            type: string
       |          required: true
       |          description: Koulutus-oid
       |          example: 1.2.246.562.13.00000000000000000009
       |        - in: path
       |          name: tarjoajaOid
       |          schema:
       |            type: string
       |          required: true
       |          description: Organisaatio-oid
       |          example: 1.2.246.562.13.00000000000000000009
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  put("/:oid/tarjoajat/:tarjoajaOid") {
    implicit val authenticated: Authenticated = authenticate

    params.get("tarjoajaOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) => Ok(koulutusService.update(KoulutusOid(params("oid")),
        k => k.copy(tarjoajat = (organisaatioOid :: k.tarjoajat).distinct)
        , getIfUnmodifiedSince))
    }
  }

  registerPath("/koulutus/{oid}/tarjoajat/{tarjoajaOid}",
    s"""    delete:
       |      summary: Poistaa koulutuksen tarjoajan
       |      operationId: Poistaa koulutuksen tarjoajan
       |      description: Poistaa koulutuksen tarjoajan
       |      tags:
       |        - Koulutus
       |      parameters:
       |        - in: path
       |          name: oid
       |          schema:
       |            type: string
       |          required: true
       |          description: Koulutus-oid
       |          example: 1.2.246.562.13.00000000000000000009
       |        - in: path
       |          name: tarjoajaOid
       |          schema:
       |            type: string
       |          required: true
       |          description: Organisaatio-oid
       |          example: 1.2.246.562.13.00000000000000000009
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  delete("/:oid/tarjoajat/:tarjoajaOid") {
    implicit val authenticated: Authenticated = authenticate

    params.get("tarjoajaOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) => Ok(koulutusService.update(KoulutusOid(params("oid")),
        k => k.copy(tarjoajat = k.tarjoajat.filterNot(o => o == organisaatioOid))
        , getIfUnmodifiedSince))
    }
  }
}
