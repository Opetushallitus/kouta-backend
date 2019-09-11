package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid}
import fi.oph.kouta.service.KoulutusService
import org.scalatra.{NotFound, Ok}

class KoulutusServlet(koulutusService: KoulutusService) extends KoutaServlet {

  def this() = this(KoulutusService)

  registerPath( "/koulutus/{oid}",
    s"""    get:
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
       |                $$ref: '#/components/schemas/Koulutus'
       |""".stripMargin)
  get("/:oid") {

    implicit val authenticated: Authenticated = authenticate

    koulutusService.get(KoulutusOid(params("oid"))) match {
      case None => NotFound("error" -> "Unknown koulutus oid")
      case Some((k, l)) => Ok(k, headers = Map(KoutaServlet.LastModifiedHeader -> createLastModifiedHeader(l)))
    }
  }

  registerPath( "/koulutus/",
    s"""    put:
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
       |              $$ref: '#/components/schemas/Koulutus'
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
    s"""    post:
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
       |              $$ref: '#/components/schemas/Koulutus'
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
    s"""    get:
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
       |                  $$ref: '#/components/schemas/KoulutusListItem'
       |""".stripMargin)
  get("/list") {

    implicit val authenticated: Authenticated = authenticate

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(oid) => Ok(koulutusService.list(oid))
    }
  }

  registerPath( "/koulutus/{oid}/toteutukset",
    s"""    get:
       |      summary: Hae koulutuksen toteutukset
       |      operationId: Hae koulutuksen toteutukset
       |      description: Hakee koulutuksen kaikkien toteutusten kaikki tiedot. Tämä rajapinta on ideksointia varten
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
       |          name: vainJulkaistut
       |          schema:
       |            type: boolean
       |          required: false
       |          default: false
       |          description: Palautetaanko vain julkaistut, Opintopolussa näytettävät toteutukset
       |      responses:
       |        '200':
       |          description: Ok
       |          content:
       |            application/json:
       |              schema:
       |                type: array
       |                items:
       |                  $$ref: '#/components/schemas/Toteutus'
       |""".stripMargin)
  get("/:oid/toteutukset") {

    implicit val authenticated: Authenticated = authenticate

    Ok(koulutusService.toteutukset(KoulutusOid(params("oid")), params.get("vainJulkaistut").exists(_.toBoolean)))
  }

  registerPath( "/koulutus/{oid}/toteutukset/list",
    s"""    get:
       |      summary: Listaa organisaation käytettävissä olevat tietyn koulutuksen toteutukset
       |      operationId: Listaa koulutuksen toteutukset
       |      description: Listaa ne tietyn koulutuksen toteutukset, jotka ovat organisaation käytettävissä.
       |        Jos organisaatio-oidia ei ole annettu,
       |        listaa koulutuksen kaikki toteutukset, mikäli käyttäjällä on oikeus nähdä ne
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
       |          required: false
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
       |                  $$ref: '#/components/schemas/ToteutusListItem'
       |""".stripMargin)
  get("/:oid/toteutukset/list") {

    implicit val authenticated: Authenticated = authenticate

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => Ok(koulutusService.listToteutukset(KoulutusOid(params("oid"))))
      case Some(organisaatioOid) => Ok(koulutusService.listToteutukset(KoulutusOid(params("oid")), organisaatioOid))
    }
  }

  registerPath( "/koulutus/{oid}/hakutiedot",
    s"""    get:
       |      summary: Hae koulutukseen liittyvät hakutiedot
       |      operationId: Hae koulutuksen hakutiedot
       |      description: Hakee koulutuksen kaikki hakutiedot. Tämä rajapinta on indeksointia varten
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
       |                type: array
       |                items:
       |                  $$ref: '#/components/schemas/Hakutieto'
       |""".stripMargin)
  get("/:oid/hakutiedot") {

    implicit val authenticated: Authenticated = authenticate

    Ok(koulutusService.hakutiedot(KoulutusOid(params("oid"))))
  }
}
