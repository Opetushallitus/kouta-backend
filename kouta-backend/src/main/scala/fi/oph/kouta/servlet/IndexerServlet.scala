package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain.TilaFilter
import fi.oph.kouta.domain.oid.{HakuOid, KoulutusOid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.service._
import fi.oph.kouta.servlet.KoutaServlet.SampleHttpDate
import fi.oph.kouta.util.TimeUtils.parseHttpDate
import org.scalatra.{BadRequest, InternalServerError, NotFound, Ok}

import java.net.URLDecoder
import java.util.UUID

class IndexerServlet(
    koulutusService: KoulutusService,
    toteutusService: ToteutusService,
    hakuService: HakuService,
    hakukohdeService: HakukohdeService,
    valintaperusteService: ValintaperusteService,
    sorakuvausService: SorakuvausService,
    oppilaitosService: OppilaitosService,
    oppilaitoksenOsaService: OppilaitoksenOsaService,
    pistehistoriaService: PistehistoriaService
) extends KoutaServlet {

  def this() = this(
    KoulutusService,
    ToteutusService,
    HakuService,
    HakukohdeService,
    ValintaperusteService,
    SorakuvausService,
    OppilaitosService,
    OppilaitoksenOsaService,
    PistehistoriaService
  )

  registerPath(
    "/indexer/modifiedSince/{since}",
    s"""    get:
       |      summary: Hakee listan kaikesta, mikä on muuttunut tietyn ajanhetken jälkeen
       |      operationId: indexerModifiedSince
       |      description: Hakee listan kaikesta, mikä on muuttunut tietyn ajanhetken jälkeen. Tämä rajapinta on indeksointia varten
       |      tags:
       |        - Indexer
       |      parameters:
       |        - in: path
       |          name: since
       |          schema:
       |            type: string
       |            format: date-time
       |          required: true
       |          example: ${SampleHttpDate}
       |      responses:
       |        '200':
       |          description: Ok
       |          content:
       |            application/json:
       |              schema:
       |                $$ref: '#/components/schemas/ListEverything'
       |""".stripMargin
  )
  get("/modifiedSince/:since") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(ModificationService.getModifiedSince(parseHttpDate(URLDecoder.decode(params("since"), "UTF-8"))))
  }

  registerPath(
    "/indexer/tarjoaja/{organisaatioOid}/koulutukset",
    """    get:
      |      summary: Hakee julkaistut koulutukset, joissa organisaatio tai sen aliorganisaatio on tarjoajana
      |      operationId: indexerJulkaistutKoulutukset
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
      |""".stripMargin
  )
  get("/tarjoaja/:organisaatioOid/koulutukset") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(koulutusService.getTarjoajanJulkaistutKoulutuksetJaToteutukset(OrganisaatioOid(params("organisaatioOid"))))
  }

  registerPath(
    "/indexer/koulutus/{oid}/toteutukset",
    """    get:
      |      summary: Hae koulutuksen toteutukset
      |      operationId: indexerKoulutusToteutukset
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
      |            default: false
      |          required: false
      |          description: Palautetaanko vain julkaistut, Opintopolussa näytettävät toteutukset
      |        - in: query
      |          name: vainOlemassaolevat
      |          schema:
      |            type: boolean
      |            default: true
      |          required: false
      |          description: Palautetaanko ainoastaan olemassaolevat (=ei poistetut) toteutukset
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/Toteutus'
      |""".stripMargin
  )
  get("/koulutus/:oid/toteutukset") {

    implicit val authenticated: Authenticated = authenticate()

    val vainJulkaistut     = params.getOrElse("vainJulkaistut", "false").toBoolean
    val vainOlemassaolevat = params.getOrElse("vainOlemassaolevat", "true").toBoolean
    Ok(
      koulutusService.toteutukset(
        KoulutusOid(params("oid")),
        TilaFilter.vainJulkaistutOrVainOlemassaolevat(vainJulkaistut, vainOlemassaolevat)
      )
    )
  }

  registerPath(
    "/indexer/koulutus/{oid}/toteutukset/list",
    """    get:
      |      summary: Listaa kaikki koulutuksen toteutukset
      |      operationId: indexerListKoulutusToteutukset
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
      |        - in: query
      |          name: vainOlemassaolevat
      |          schema:
      |            type: boolean
      |            default: true
      |          required: false
      |          description: Palautetaanko ainoastaan olemassaolevat (=ei poistetut) toteutukset
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/ToteutusListItem'
      |""".stripMargin
  )
  get("/koulutus/:oid/toteutukset/list") {

    implicit val authenticated: Authenticated = authenticate()

    val myosPoistetut = !params.getOrElse("vainOlemassaolevat", "true").toBoolean
    Ok(
      koulutusService.listToteutukset(KoulutusOid(params("oid")), TilaFilter.alsoPoistetutAddedToOthers(myosPoistetut))
    )
  }

  registerPath(
    "/indexer/koulutus/{oid}/hakutiedot",
    """    get:
      |      summary: Hae koulutukseen liittyvät hakutiedot
      |      operationId: indexerKoulutusHakutiedot
      |      description: Hakee koulutuksen olemassaolevat (=ei poistetut) ja arkistoimattomat hakutiedot. Tämä rajapinta on indeksointia varten
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
      |""".stripMargin
  )
  get("/koulutus/:oid/hakutiedot") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(koulutusService.hakutiedot(KoulutusOid(params("oid"))))
  }

  registerPath(
    "/indexer/toteutus/{oid}/haut/list",
    """    get:
      |      summary: Listaa kaikki toteutukseen liitetyt haut
      |      operationId: indexerListToteutusHaut
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
      |        - in: query
      |          name: vainOlemassaolevat
      |          schema:
      |            type: boolean
      |            default: true
      |          required: false
      |          description: Palautetaanko ainoastaan olemassaolevat (=ei poistetut) haut
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/HakuListItem'
      |""".stripMargin
  )
  get("/toteutus/:oid/haut/list") {

    implicit val authenticated: Authenticated = authenticate()

    val myosPoistetut = !params.getOrElse("vainOlemassaolevat", "true").toBoolean
    Ok(toteutusService.listHaut(ToteutusOid(params("oid")), TilaFilter.alsoPoistetutAddedToOthers(myosPoistetut)))
  }

  registerPath(
    "/indexer/toteutus/{oid}/hakukohteet/list",
    """    get:
      |      summary: Listaa kaikki toteutukseen liitetyt hakukohteet
      |      operationId: listToteutusHakukohteet
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
      |        - in: query
      |          name: vainOlemassaolevat
      |          schema:
      |            type: boolean
      |            default: true
      |          required: false
      |          description: Palautetaanko ainoastaan olemassaolevat (=ei poistetut) hakukohteet
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/HakukohdeListItem'
      |""".stripMargin
  )
  get("/toteutus/:oid/hakukohteet/list") {
    implicit val authenticated: Authenticated = authenticate()

    val myosPoistetut = !params.getOrElse("vainOlemassaolevat", "true").toBoolean
    Ok(
      toteutusService.listHakukohteet(ToteutusOid(params("oid")), TilaFilter.alsoPoistetutAddedToOthers(myosPoistetut))
    )
  }

  registerPath(
    "/indexer/haku/{oid}/hakukohteet/list",
    """    get:
      |      summary: Listaa kaikki hakukohteet, jotka on liitetty hakuun
      |      operationId: indexerListHakuHakukohteet
      |      description: Listaa hakuun liitetyt hakukohteet. Tämä rajapinta on indeksointia varten
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
      |        - in: query
      |          name: vainOlemassaolevat
      |          schema:
      |            type: boolean
      |            default: true
      |          required: false
      |          description: Palautetaanko ainoastaan olemassaolevat (=ei poistetut) hakukohteet
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/HakukohdeListItem'
      |""".stripMargin
  )
  get("/haku/:oid/hakukohteet/list") {

    implicit val authenticated: Authenticated = authenticate()
    val myosPoistetut                         = !params.getOrElse("vainOlemassaolevat", "true").toBoolean
    Ok(hakuService.listHakukohteet(HakuOid(params("oid")), TilaFilter.alsoPoistetutAddedToOthers(myosPoistetut)))
  }

  registerPath(
    "/indexer/haku/{oid}/koulutukset/list",
    """    get:
      |      summary: Listaa kaikki hakuun liitetyt koulutukset
      |      operationId: indexerListHakuKoulutukset
      |      description: Listaa kaikki hakuun liitetyt olemassaolevat (=ei poistetut) koulutukset. Tämä rajapinta on indeksointia varten
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
      |""".stripMargin
  )
  get("/haku/:oid/koulutukset/list") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(hakuService.listKoulutukset(HakuOid(params("oid"))))
  }

  registerPath(
    "/indexer/haku/{oid}/toteutukset/list",
    """    get:
      |      summary: Listaa kaikki hakuun liitetyt toteutukset
      |      operationId: indexerListHakuToteutukset
      |      description: Listaa kaikki hakuun liitetyt olemassaolevat (=ei poistetut) toteutukset. Tämä rajapinta on indeksointia varten
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
      |""".stripMargin
  )
  get("/haku/:oid/toteutukset/list") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(hakuService.listToteutukset(HakuOid(params("oid"))))
  }

  registerPath(
    "/indexer/valintaperuste/{id}/hakukohteet/list",
    """    get:
      |      summary: Listaa kaikki hakukohteet, joihin valintaperustekuvaus on liitetty
      |      operationId: indexerListValintaperusteHakukohteet
      |      description: Listaa kaikki hakukohteet, joihin valintaperustekuvaus on liitetty, mikäli käyttäjällä on oikeus nähdä ne
      |      tags:
      |        - Indexer
      |      parameters:
      |        - in: path
      |          name: id
      |          schema:
      |            type: string
      |          required: true
      |          description: Valintaperusteen id
      |          example: ea596a9c-5940-497e-b5b7-aded3a2352a7
      |        - in: query
      |          name: vainOlemassaolevat
      |          schema:
      |            type: boolean
      |            default: true
      |          required: false
      |          description: Palautetaanko ainoastaan olemassaolevat (=ei poistetut) hakukohteet
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/HakukohdeListItem'
      |""".stripMargin
  )
  get("/valintaperuste/:id/hakukohteet/list") {

    implicit val authenticated: Authenticated = authenticate()

    val myosPoistetut = !params.getOrElse("vainOlemassaolevat", "true").toBoolean
    Ok(
      valintaperusteService.listHakukohteet(
        UUID.fromString(params("id")),
        TilaFilter.alsoPoistetutAddedToOthers(myosPoistetut)
      )
    )
  }

  registerPath(
    "/indexer/sorakuvaus/{id}/koulutukset/list",
    """    get:
      |      summary: Listaa kaikki koulutukset, joihin SORA-kuvaus on liitetty
      |      operationId: indexerListSorakuvausKoulutukset
      |      description: Listaa kaikki koulutukset, joihin SORA-kuvaus on liitetty, mikäli käyttäjällä on oikeus nähdä ne
      |      tags:
      |        - Indexer
      |      parameters:
      |        - in: path
      |          name: id
      |          schema:
      |            type: string
      |          required: true
      |          description: SORA-kuvauksen id
      |          example: ea596a9c-5940-497e-b5b7-aded3a2352a7
      |        - in: query
      |          name: vainOlemassaolevat
      |          schema:
      |            type: boolean
      |            default: true
      |          required: false
      |          description: Palautetaanko ainoastaan olemassaolevat (=ei poistetut) koulutukset
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  type: string
      |""".stripMargin
  )
  get("/sorakuvaus/:id/koulutukset/list") {

    implicit val authenticated: Authenticated = authenticate()

    val myosPoistetut = !params.getOrElse("vainOlemassaolevat", "true").toBoolean
    Ok(
      sorakuvausService.listKoulutusOids(
        UUID.fromString(params("id")),
        TilaFilter.alsoPoistetutAddedToOthers(myosPoistetut)
      )
    )
  }

  registerPath(
    "/indexer/oppilaitos/{oid}/osat/list",
    """    get:
      |      summary: Listaa kaikki oppilaitoksen osien kuvailutiedot
      |      operationId: indexerListOppilaitosOsat
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
      |""".stripMargin
  )
  get("/oppilaitos/:oid/osat/list") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(oppilaitosService.listOppilaitoksenOsat(OrganisaatioOid(params("oid"))))
  }

  registerPath(
    "/indexer/oppilaitos/{oid}/osat",
    """    get:
      |      summary: Hakee oppilaitoksen kaikkien osien kuvailutiedot
      |      operationId: indexerGetOppilaitosOsat
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
      |""".stripMargin
  )
  get("/oppilaitos/:oid/osat") {

    implicit val authenticated: Authenticated = authenticate()

    Ok(oppilaitosService.getOppilaitoksenOsat(OrganisaatioOid(params("oid"))))
  }

  registerPath(
    "/indexer/list-koulutus-oids-by-tarjoajat",
    """    post:
      |      summary: Hakee tarjoajaa (oppilaitos tai toimipiste) vastaavat koulutukset
      |      operationId: indexerListKoulutusOidsByTarjoajat
      |      description: Hakee kaikkien niiden koulutusten oidit, joissa annettu oppilaitos/toimipiste on sen tarjoajana suoraan tai oppilaitoksen kautta (jos toimipiste). Tämä rajapinta on indeksointia varten
      |      tags:
      |        - Indexer
      |      requestBody:
      |        description: Lista tarjoajien organisaatio-oideja
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: array
      |              items:
      |                type: string
      |                example: 1.2.246.562.10.56753942459
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin
  )
  post("/list-koulutus-oids-by-tarjoajat") {

    implicit val authenticated: Authenticated = authenticate()
    Ok(KoulutusService.getOidsByTarjoajat(parsedBody.extract[Seq[OrganisaatioOid]], TilaFilter.all()))
  }

  registerPath(
    "/indexer/list-hakukohde-oids-by-jarjestyspaikat",
    """    post:
      |      summary: Hakee järjestyspaikkaa (oppilaitos tai toimipiste) vastaavat hakukohteet
      |      operationId: indexerListHakukohdeOidsByJarjestyspaikat
      |      description: Hakee kaikkien niiden hakukohteiden oidit, joissa annettu oppilaitos/toimipiste on sen järjestyspaikkana suoraan tai oppilaitoksen kautta (jos toimipiste). Tämä rajapinta on indeksointia varten
      |      tags:
      |        - Indexer
      |      requestBody:
      |        description: Lista järjestyspaikkojen organisaatio-oideja
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: array
      |              items:
      |                type: string
      |                example: 1.2.246.562.10.56753942459
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin
  )
  post("/list-hakukohde-oids-by-jarjestyspaikat") {

    implicit val authenticated: Authenticated = authenticate()
    Ok(
      hakukohdeService.getOidsByJarjestyspaikat(
        parsedBody.extract[Seq[OrganisaatioOid]],
        TilaFilter.onlyOlemassaolevat()
      )
    )
  }

  registerPath(
    "/indexer/list-toteutus-oids-by-tarjoajat",
    """    post:
      |      summary: Hakee tarjoajaa (oppilaitos tai toimipiste) vastaavat toteutukset
      |      operationId: indexerListToteutusOidsByTarjoajat
      |      description: Hakee kaikkien niiden toteutusten oidit, joissa annettu oppilaitos/toimipiste on sen tarjoajana suoraan tai oppilaitoksen kautta (jos toimipiste). Tämä rajapinta on indeksointia varten
      |      tags:
      |        - Indexer
      |      requestBody:
      |        description: Lista tarjoajien organisaatio-oideja
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: array
      |              items:
      |                type: string
      |                example: 1.2.246.562.10.56753942459
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin
  )
  post("/list-toteutus-oids-by-tarjoajat") {

    implicit val authenticated: Authenticated = authenticate()
    Ok(ToteutusService.getOidsByTarjoajat(parsedBody.extract[Seq[OrganisaatioOid]], TilaFilter.onlyOlemassaolevat()))
  }

  registerPath(
    "/indexer/toteutukset",
    """    post:
      |      summary: Hakee toteutukset, joiden oidit annettu requestBodyssä
      |      operationId: indexerToteutukset
      |      description: Hakee toteutukset, joiden oidit annettu requestBodyssä. Tämä rajapinta on indeksointia varten
      |      tags:
      |        - Indexer
      |      requestBody:
      |        description: Lista toteutusten oideja
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: array
      |              items:
      |                type: string
      |                example: 1.2.246.562.17.00000000000000000009
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin
  )
  post("/toteutukset") {

    implicit val authenticated: Authenticated = authenticate()
    Ok(ToteutusService.getToteutukset(parsedBody.extract[List[ToteutusOid]]))
  }

  registerPath(
    "/indexer/list-opintokokonaisuudet",
    """    post:
      |      summary: Hakee tiedot niille opintokokonaisuuksille, joihin requestBodyssä annetut toteutus-oidit on liitetty
      |      operationId: indexerListOpintokokonaisuudet
      |      description: Hakee tiedot niille opintokokonaisuuksille, joihin requestBodyssä annetut toteutus-oidit on liitetty. Tämä rajapinta on indeksointia varten
      |      tags:
      |        - Indexer
      |      requestBody:
      |        description: Lista toteutusten (opintojaksojen) oideja
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: array
      |              items:
      |                type: string
      |                example: 1.2.246.562.17.00000000000000000009
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin
  )
  post("/list-opintokokonaisuudet") {

    implicit val authenticated: Authenticated = authenticate()
    Ok(ToteutusService.listOpintokokonaisuudet(parsedBody.extract[List[ToteutusOid]]))
  }

  registerPath(
    "/indexer/koulutukset",
    """    post:
      |      summary: Hakee koulutukset, joiden oidit annettu requestBodyssä
      |      operationId: indexerKoulutukset
      |      description: Hakee koulutukset, joiden oidit annettu requestBodyssä. Tämä rajapinta on indeksointia varten
      |      tags:
      |        - Indexer
      |      requestBody:
      |        description: Lista koulutusOideja
      |        required: true
      |        content:
      |          application/json:
      |            schema:
      |              type: array
      |              items:
      |                type: string
      |                example: 1.2.246.562.13.00000000000000007752
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin
  )
  post("/koulutukset") {
    implicit val authenticated: Authenticated = authenticate()
    Ok(KoulutusService.get(koulutusOids = parsedBody.extract[List[KoulutusOid]]))
  }

  registerPath(
    "/indexer/pistehistoria",
    """    get:
      |      summary: Palauttaa tarjoajan ja hakukohdekoodin tai lukiolinjakoodin yhdistelmään liittyvät pistetiedot
      |      operationId: indexerListPistetiedot
      |      description: Listaa pistetiedot. Tarjoaja JA joko hakukohdekoodi TAI lukiolinjakoodi annettava.
      |      tags:
      |        - Indexer
      |      parameters:
      |        - in: query
      |          name: tarjoaja
      |          schema:
      |            type: String
      |          example: 1.2.246.562.10.00101010101
      |          description: Tarjoajaorganisaation oid
      |        - in: query
      |          name: hakukohdekoodi
      |          schema:
      |            type: String
      |          example: hakukohteet_000
      |          description: hakukohdekoodi
      |        - in: query
      |          name: lukiolinjakoodi
      |          schema:
      |            type: String
      |          description: lukiolinjakoodi
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  $ref: '#/components/schemas/Pistetieto'
      |""".stripMargin
  )
  get("/pistehistoria") {

    implicit val authenticated: Authenticated = authenticate()

    val tarjoaja   = params.get("tarjoaja").map(OrganisaatioOid)
    val hk         = params.get("hakukohdekoodi").map(koodi => koodi.split("#").head)
    val lukiolinja = if (hk.isDefined) None else params.get("lukiolinjakoodi").map(koodi => koodi.split("#").head)

    (tarjoaja, hk, lukiolinja) match {
      case (None, _, _)           => BadRequest("error" -> "Pakollinen parametri puuttui: tarjoaja")
      case (_, None, None)        => BadRequest("error" -> "Pakollinen parametri puuttui: hakukohdekoodi TAI lukiolinjakoodi")
      case (Some(t), Some(h), _)  => Ok(pistehistoriaService.getPistehistoria(t, h))
      case (Some(t), _, Some(ll)) => Ok(pistehistoriaService.getPistehistoriaForLukiolinja(t, ll))
    }
  }

  registerPath(
    "/indexer/pistehistoria/sync",
    """    get:
      |      summary: Hakee haun hakukohteiden alimmat pisteet ja tallentaa ne kantaan
      |      operationId: indexerSyncHaunPistetiedot
      |      description: Hakee haun hakukohteiden alimmat pisteet ja tallentaa ne kantaan
      |      tags:
      |        - Indexer
      |      parameters:
      |        - in: query
      |          name: hakuOid
      |          schema:
      |            type: String
      |          example: 1.2.246.562.29.00000000000000021303
      |          description: Yksittäisen haun oid tai "defaults" viiden edellisen toisen asteen yhteishaun synkronoimiseksi (2023)
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            text/plain:
      |              schema:
      |                type: string
      |        '400':
      |          description: Pakollinen parametri puuttuu
      |          content:
      |            text/plain:
      |              schema:
      |                type: string
      |        '500':
      |          description: Palvelinvirhe
      |          content:
      |            text/plain:
      |              schema:
      |                type: string
      |""".stripMargin
  )
  get("/pistehistoria/sync") {

    implicit val authenticated: Authenticated = authenticate()

    try {
      val defaults = params.get("hakuOid").exists(_.equals("defaults"))

      if (defaults) {
        val result = pistehistoriaService.syncDefaults()
        logger.info("Oletushaut synkattu: " + result)
        Ok(result, Map("Content-Type" -> "text/plain;charset=utf-8"))
      } else {
        val hakuOid: Option[HakuOid] = params.get("hakuOid").map(HakuOid).filter(_.isValid)
        hakuOid match {
          case None => BadRequest("error" -> "Pakollinen parametri puuttuu: hakuOid")
          case Some(oid) =>
            Ok(pistehistoriaService.syncPistehistoriaForHaku(oid), Map("Content-Type" -> "text/plain;charset=utf-8"))
        }
      }
    } catch {
      case t: Throwable =>
        logger.error(s"Jokin meni pieleen pistehistorian synkkauksessa: $t")
        InternalServerError("error" -> t.getMessage)
    }
  }

  registerPath(
    "/indexer/koulutukset/eperusteet/list",
    """    get:
      |      summary: Listaa kaikki ePerusteIDt joita on liitetty olemassaoleviin koulutuksiin.
      |      operationId: indexerListKoulutuksetEPerusteIds
      |      description: Listaa kaikki ePerusteIDt joita on liitetty olemassaoleviin koulutuksiin. Tämä rajapinta on indeksointia varten.
      |      tags:
      |        - Indexer
      |      responses:
      |        '200':
      |          description: Ok
      |          content:
      |            application/json:
      |              schema:
      |                type: array
      |                items:
      |                  type: string
      |""".stripMargin
  )
  get("/koulutukset/eperusteet/list") {
    implicit val authenticated: Authenticated = authenticate()
    Ok(koulutusService.getAllUsedEPerusteIds())
  }
}
