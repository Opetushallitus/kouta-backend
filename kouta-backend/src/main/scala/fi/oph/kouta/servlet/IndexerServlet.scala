package fi.oph.kouta.servlet

import java.net.URLDecoder
import java.util.UUID

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain.oid.{HakuOid, KoulutusOid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.service.{HakuService, KoulutusService, ModificationService, OppilaitosService, SorakuvausService, ToteutusService, ValintaperusteService}
import fi.oph.kouta.servlet.KoutaServlet.SampleHttpDate
import fi.oph.kouta.util.TimeUtils.parseHttpDate
import org.scalatra.Ok

class IndexerServlet(koulutusService: KoulutusService,
                     toteutusService: ToteutusService,
                     hakuService: HakuService,
                     valintaperusteService: ValintaperusteService,
                     sorakuvausService: SorakuvausService,
                     oppilaitosService: OppilaitosService) extends KoutaServlet {

  def this() = this(KoulutusService, ToteutusService, HakuService, ValintaperusteService, SorakuvausService, OppilaitosService)

  registerPath("/indexer/modifiedSince/{since}",
    s"""    get:
      |      summary: Hakee listan kaikesta, mikä on muuttunut tietyn ajanhetken jälkeen
      |      operationId: Hae lista muuttuneista
      |      description: Hakee listan kaikesta, mikä on muuttunut tietyn ajanhetken jälkeen. Tämä rajapinta on indeksointia varten
      |      tags:
      |        - Indexer
      |      parameters:
      |        - in: path
      |          name: since
      |          schema:
      |            type: string
      |          format: date-time
      |          required: true
      |          example: ${SampleHttpDate}
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                $$ref: '#/components/schemas/ListEverything'
      |""".stripMargin)
  get("/modifiedSince/:since") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(ModificationService.getModifiedSince(parseHttpDate(URLDecoder.decode(params("since"), "UTF-8"))))
  }

  registerPath( "/indexer/tarjoaja/{organisaatioOid}/koulutukset",
    """    get:
      |      summary: Hakee julkaistut koulutukset, joissa organisaatio tai sen aliorganisaatio on tarjoajana
      |      operationId: Tarjoajan julkaistut koulutukset
      |      description: Hakee kaikkien niiden koulutusten kaikki tiedot, joissa organisaatio tai sen aliorganisaatio
      |        on tarjoajana ja jotka on julkaistu. Tämä rajapinta on indeksointia varten
      |      tags:
      |        - Indexer
      |      parameters:
      |        - in: path
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
  get("/tarjoaja/:organisaatioOid/koulutukset") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(koulutusService.getTarjoajanJulkaistutKoulutukset(OrganisaatioOid(params("organisaatioOid"))))
  }

  registerPath( "/indexer/koulutus/{oid}/toteutukset",
    """    get:
      |      summary: Hae koulutuksen toteutukset
      |      operationId: Hae koulutuksen toteutukset
      |      description: Hakee koulutuksen kaikkien toteutusten kaikki tiedot. Tämä rajapinta on ideksointia varten
      |      tags:
      |        - Indexer
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
      |                  $ref: '#/components/schemas/Toteutus'
      |""".stripMargin)
  get("/koulutus/:oid/toteutukset") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(koulutusService.toteutukset(KoulutusOid(params("oid")), params.get("vainJulkaistut").exists(_.toBoolean)))
  }

  registerPath( "/indexer/koulutus/{oid}/toteutukset/list",
    """    get:
      |      summary: Listaa kaikki koulutuksen toteutukset
      |      operationId: Listaa koulutuksen toteutukset
      |      description: Listaa kaikki koulutuksen toteutukset. Tämä rajapinta on indeksointia varten
      |      tags:
      |        - Indexer
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
      |                  $ref: '#/components/schemas/ToteutusListItem'
      |""".stripMargin)
  get("/koulutus/:oid/toteutukset/list") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(koulutusService.listToteutukset(KoulutusOid(params("oid"))))
  }

  registerPath( "/indexer/koulutus/{oid}/hakutiedot",
    """    get:
      |      summary: Hae koulutukseen liittyvät hakutiedot
      |      operationId: Hae koulutuksen hakutiedot
      |      description: Hakee koulutuksen kaikki hakutiedot. Tämä rajapinta on indeksointia varten
      |      tags:
      |        - Indexer
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
      |                  $ref: '#/components/schemas/Hakutieto'
      |""".stripMargin)
  get("/koulutus/:oid/hakutiedot") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(koulutusService.hakutiedot(KoulutusOid(params("oid"))))
  }

  registerPath( "/indexer/toteutus/{oid}/haut/list",
    """    get:
      |      summary: Listaa kaikki toteutukseen liitetyt haut
      |      operationId: Listaa toteutuksen haut
      |      description: Listaa kaikki toteutukseen liitetyt hakukohteet. Tämä rajapinta on indeksointia varten
      |      tags:
      |        - Indexer
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Toteutus-oid
      |          example: 1.2.246.562.17.00000000000000000009
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/HakuListItem'
      |""".stripMargin)
  get("/toteutus/:oid/haut/list") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(toteutusService.listHaut(ToteutusOid(params("oid"))))
  }

  registerPath( "/indexer/toteutus/{oid}/hakukohteet/list",
    """    get:
      |      summary: Listaa kaikki toteutukseen liitetyt hakukohteet
      |      operationId: Listaa toteutuksen hakukohteet
      |      description: Listaa kaikki toteutukseen liitetyt hakukohteet. Tämä rajapinta on indeksointia varten
      |      tags:
      |        - Indexer
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Toteutus-oid
      |          example: 1.2.246.562.17.00000000000000000009
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/HakukohdeListItem'
      |""".stripMargin)
  get("/toteutus/:oid/hakukohteet/list") {
    implicit val authenticated: Authenticated = authenticate()

    Ok(toteutusService.listHakukohteet(ToteutusOid(params("oid"))))
  }

  registerPath( "/indexer/haku/{oid}/hakukohteet/list",
    """    get:
      |      summary: Listaa kaikki hakukohteet, jotka on liitetty hakuun
      |      operationId: Listaa haun hakukohteet
      |      description: Listaa ne hakuun liitetyt hakukohteet. Tämä rajapinta on indeksointia varten
      |      tags:
      |        - Indexer
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Haku-oid
      |          example: 1.2.246.562.29.00000000000000000009
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/HakukohdeListItem'
      |""".stripMargin)
  get("/haku/:oid/hakukohteet/list") {

    implicit val authenticated: Authenticated = authenticate()
    Ok(hakuService.listHakukohteet(HakuOid(params("oid"))))
  }

  registerPath("/indexer/haku/{oid}/koulutukset/list",
    """    get:
      |      summary: Listaa kaikki hakuun liitetyt koulutukset
      |      operationId: Listaa haun koulutukset
      |      description: Listaa kaikki hakuun liitetyt koulutukset. Tämä rajapinta on indeksointia varten
      |      tags:
      |        - Indexer
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Haku-oid
      |          example: 1.2.246.562.29.00000000000000000009
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
  get("/haku/:oid/koulutukset/list") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(hakuService.listKoulutukset(HakuOid(params("oid"))))
  }

  registerPath("/indexer/haku/{oid}/toteutukset/list",
    """    get:
      |      summary: Listaa kaikki hakuun liitetyt toteutukset
      |      operationId: Listaa haun toteutukset
      |      description: Listaa kaikki hakuun liitetyt toteutukset. Tämä rajapinta on indeksointia varten
      |      tags:
      |        - Indexer
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Haku-oid
      |          example: 1.2.246.562.29.00000000000000000009
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
  get("/haku/:oid/toteutukset/list") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(hakuService.listToteutukset(HakuOid(params("oid"))))
  }

  registerPath( "/indexer/valintaperuste/{id}/hakukohteet/list",
    """    get:
      |      summary: Listaa kaikki hakukohteet, joihin valintaperustekuvaus on liitetty
      |      operationId: Lista valintaperusteen hakukohteet
      |      description: Listaa kaikki hakukohteet, joihin valintaperustekuvaus on liitetty, mikäli käyttäjällä on oikeus nähdä ne
      |      tags:
      |        - Indexer
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Valintaperusteen id
      |          example: ea596a9c-5940-497e-b5b7-aded3a2352a7
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/HakukohdeListItem'
      |""".stripMargin)
  get("/valintaperuste/:id/hakukohteet/list") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(valintaperusteService.listHakukohteet(UUID.fromString(params("id"))))
  }

  registerPath( "/indexer/sorakuvaus/{id}/koulutukset/list",
    """    get:
      |      summary: Listaa kaikki koulutukset, joihin SORA-kuvaus on liitetty
      |      operationId: Listaa sorakuvauksen koulutukset
      |      description: Listaa kaikki koulutukset, joihin SORA-kuvaus on liitetty, mikäli käyttäjällä on oikeus nähdä ne
      |      tags:
      |        - Indexer
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: SORA-kuvauksen id
      |          example: ea596a9c-5940-497e-b5b7-aded3a2352a7
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                 type: string
      |""".stripMargin)
  get("/sorakuvaus/:id/koulutukset/list") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(sorakuvausService.listKoulutusOids(UUID.fromString(params("id"))))
  }

  registerPath( "/indexer/oppilaitos/{oid}/osat/list",
    """    get:
      |      summary: Listaa kaikki oppilaitoksen osien kuvailutiedot
      |      operationId: Listaa oppilaitoksen osat
      |      description: Listaa oppilaitoksen kaikki osat. Tämä rajapinta on ideksointia varten
      |      tags:
      |        - Indexer
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Oppilaitoksen organisaatio-oid
      |          example: 1.2.246.562.10.00101010101
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/OppilaitoksenOsaListItem'
      |""".stripMargin)
  get("/oppilaitos/:oid/osat/list") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(oppilaitosService.listOppilaitoksenOsat(OrganisaatioOid(params("oid"))))
  }

  registerPath( "/indexer/oppilaitos/{oid}/osat",
    """    get:
      |      summary: Hakee oppilaitoksen kaikkien osien kuvailutiedot
      |      operationId: Hae oppilaitoksen osat
      |      description: Hakee oppilaitoksen kaikkien osien kuvailutiedot. Tämä rajapinta on ideksointia varten
      |      tags:
      |        - Indexer
      |      parameters:
      |        - in: path
      |          name: oid
      |          schema:
      |            type: string
      |          required: true
      |          description: Oppilaitoksen organisaatio-oid
      |          example: 1.2.246.562.10.00101010101
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/OppilaitoksenOsa'
      |""".stripMargin)
  get("/oppilaitos/:oid/osat") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(oppilaitosService.getOppilaitoksenOsat(OrganisaatioOid(params("oid"))))
  }
}
