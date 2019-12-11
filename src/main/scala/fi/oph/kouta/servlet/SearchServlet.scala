package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid}
import fi.oph.kouta.service.{HakuService, KoulutusService, OppilaitosService, SorakuvausService, ToteutusService, ValintaperusteService}
import org.scalatra.{NotFound, Ok}

class SearchServlet(koulutusService: KoulutusService,
                    toteutusService: ToteutusService,
                    hakuService: HakuService,
                    valintaperusteService: ValintaperusteService,
                    sorakuvausService: SorakuvausService,
                    oppilaitosService: OppilaitosService) extends KoutaServlet {

  def this() = this(KoulutusService, ToteutusService, HakuService, ValintaperusteService, SorakuvausService, OppilaitosService)

  val SearchParams = Seq("nimi", "muokkaaja", "tila", "arkistoidut", "page", "size", "lng", "order-by", "order")

  val searchParams =
    s"""      parameters:
       |        - in: query
       |          name: organisaatioOid
       |          schema:
       |            type: string
       |          required: true
       |          description: Organisaation oid
       |          example: 1.2.246.562.10.00101010101
       |        - in: query
       |          name: nimi
       |          schema:
       |            type: string
       |          required: false
       |          description: Suodata annetulla nimellä tai oidilla
       |          example: Jalkaterapeutti
       |        - in: query
       |          name: muokkaaja
       |          schema:
       |            type: string
       |          required: false
       |          description: Suodata muokkaajan nimellä
       |          example: Maija
       |        - in: query
       |          name: tila
       |          schema:
       |            type: string
       |          required: false
       |          description: Suodata tilalla (julkaistu/tallennettu/arkistoitu)
       |          example: Julkaistu
       |        - in: query
       |          name: arkistoidut
       |          schema:
       |            type: boolean
       |          required: false
       |          description: Näytetäänkö arkistoidut
       |          example: true
       |        - in: query
       |          name: page
       |          schema:
       |            type: integer
       |          required: false
       |          description: Sivunumero
       |          example: 2
       |        - in: query
       |          name: size
       |          schema:
       |            type: integer
       |          required: false
       |          description: Sivun koko
       |          example: 20
       |        - in: query
       |          name: lng
       |          schema:
       |            type: string
       |          required: false
       |          description: Haun kieli (fi/sv/en)
       |          example: fi
       |        - in: query
       |          name: order-by
       |          schema:
       |            type: string
       |          required: false
       |          description: Kenttä, jonka perusteella hakutulos järjestetään (nimi/tila/muokkaaja/modified)
       |          example: fi
       |        - in: query
       |          name: order
       |          schema:
       |            type: string
       |          required: false
       |          description: Hakutuloksen järjestys (asc/desc)
       |          example: fi
       |""".stripMargin

  registerPath("/search/koulutukset",
    s"""    get:
       |      summary: Hakee organisaation koulutuksia annetuilla parametreilla
       |      operationId: Koulutusten haku
       |      description: Hakee organisaation koulutukset annetuilla parametreilla
       |      tags:
       |        - Search
       |$searchParams
       |      responses:
       |        '200':
       |          description: Ok
       |          content:
       |            application/json:
       |              schema:
       |                type: array
       |                items:
       |                  $$ref: '#/components/schemas/KoulutusSearchResult'
       |""".stripMargin)
  get("/koulutus") {

    implicit val authenticated: Authenticated = authenticate

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) => Ok(koulutusService.search(params.toMap.filterKeys(SearchParams.contains), organisaatioOid))
    }
  }
}
