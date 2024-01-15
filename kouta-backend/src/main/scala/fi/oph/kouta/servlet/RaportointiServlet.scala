package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.service.RaportointiService
import fi.oph.kouta.util.TimeUtils.parseHttpDate
import fi.oph.kouta.servlet.KoutaServlet.SampleHttpDate
import org.scalatra.Ok

import java.net.URLDecoder
import java.time.Instant

class RaportointiServlet(raportointiService: RaportointiService) extends KoutaServlet {
  def this() = this(RaportointiService)

  private def parseDatetime(dateTime: Option[String], default: Option[Instant] = None): Option[Instant] = {
    dateTime match {
      case Some(dateTimeStr) => Some(parseHttpDate(URLDecoder.decode(dateTimeStr, "UTF-8")))
      case None              => default
    }
  }

  registerPath(
    "/raportointi/koulutukset",
    s"""    get:
       |      summary: Tallentaa koulutukset siirtotiedostoon
       |      operationId: reportKoulutukset
       |      description: Hakee annetulla aikavälillä luodut/modifioidut koulutukset ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Raportointi
       |      parameters:
       |        - in: query
       |          name: startTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          example: ${SampleHttpDate}
       |        - in: query
       |          name: endTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          description: Jos arvoa ei ole annettu, asetetaan loppuajaksi nykyinen ajankohta.
       |          example: ${SampleHttpDate}
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/koulutukset") {
    implicit val authenticated: Authenticated = authenticate()

    Ok(
      raportointiService.saveKoulutukset(
        parseDatetime(params.get("startTime")),
        parseDatetime(params.get("endTime"), Some(Instant.now()))
      )
    )
  }

  registerPath(
    "/raportointi/toteutukset",
    s"""    get:
       |      summary: Tallentaa toteutukset siirtotiedostoon
       |      operationId: reportToteutukset
       |      description: Hakee annetulla aikavälillä luodut/modifioidut toteutukset ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Raportointi
       |      parameters:
       |        - in: query
       |          name: startTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          example: ${SampleHttpDate}
       |        - in: query
       |          name: endTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          description: Jos arvoa ei ole annettu, asetetaan loppuajaksi nykyinen ajankohta.
       |          example: ${SampleHttpDate}
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/toteutukset") {
    implicit val authenticated: Authenticated = authenticate()

    Ok(
      raportointiService.saveToteutukset(
        parseDatetime(params.get("startTime")),
        parseDatetime(params.get("endTime"), Some(Instant.now()))
      )
    )
  }

  registerPath(
    "/raportointi/hakukohteet",
    s"""    get:
       |      summary: Tallentaa hakukohteet siirtotiedostoon
       |      operationId: reportHakukohteet
       |      description: Hakee annetulla aikavälillä luodut/modifioidut hakukohteet ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Raportointi
       |      parameters:
       |        - in: query
       |          name: startTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          example: ${SampleHttpDate}
       |        - in: query
       |          name: endTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          description: Jos arvoa ei ole annettu, asetetaan loppuajaksi nykyinen ajankohta.
       |          example: ${SampleHttpDate}
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/hakukohteet") {
    implicit val authenticated: Authenticated = authenticate()

    Ok(
      raportointiService.saveHakukohteet(
        parseDatetime(params.get("startTime")),
        parseDatetime(params.get("endTime"), Some(Instant.now()))
      )
    )
  }

  registerPath(
    "/raportointi/haut",
    s"""    get:
       |      summary: Tallentaa haut siirtotiedostoon
       |      operationId: reportHaut
       |      description: Hakee annetulla aikavälillä luodut/modifioidut haut ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Raportointi
       |      parameters:
       |        - in: query
       |          name: startTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          example: ${SampleHttpDate}
       |        - in: query
       |          name: endTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          description: Jos arvoa ei ole annettu, asetetaan loppuajaksi nykyinen ajankohta.
       |          example: ${SampleHttpDate}
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/haut") {
    implicit val authenticated: Authenticated = authenticate()

    Ok(
      raportointiService.saveHaut(
        parseDatetime(params.get("startTime")),
        parseDatetime(params.get("endTime"), Some(Instant.now()))
      )
    )
  }

  registerPath(
    "/raportointi/valintaperusteet",
    s"""    get:
       |      summary: Tallentaa valintaperusteet siirtotiedostoon
       |      operationId: reportValintaperusteet
       |      description: Hakee annetulla aikavälillä luodut/modifioidut valintaperusteet ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Raportointi
       |      parameters:
       |        - in: query
       |          name: startTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          example: ${SampleHttpDate}
       |        - in: query
       |          name: endTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          description: Jos arvoa ei ole annettu, asetetaan loppuajaksi nykyinen ajankohta.
       |          example: ${SampleHttpDate}
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/valintaperusteet") {
    implicit val authenticated: Authenticated = authenticate()

    Ok(
      raportointiService.saveValintaperusteet(
        parseDatetime(params.get("startTime")),
        parseDatetime(params.get("endTime"), Some(Instant.now()))
      )
    )
  }

  registerPath(
    "/raportointi/sorakuvaukset",
    s"""    get:
       |      summary: Tallentaa sorakuvaukset siirtotiedostoon
       |      operationId: reportSorakuvaukset
       |      description: Hakee annetulla aikavälillä luodut/modifioidut sorakuvaukset ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Raportointi
       |      parameters:
       |        - in: query
       |          name: startTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          example: ${SampleHttpDate}
       |        - in: query
       |          name: endTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          description: Jos arvoa ei ole annettu, asetetaan loppuajaksi nykyinen ajankohta.
       |          example: ${SampleHttpDate}
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/sorakuvaukset") {
    implicit val authenticated: Authenticated = authenticate()

    Ok(
      raportointiService.saveSorakuvaukset(
        parseDatetime(params.get("startTime")),
        parseDatetime(params.get("endTime"), Some(Instant.now()))
      )
    )
  }

  registerPath(
    "/raportointi/oppilaitokset",
    s"""    get:
       |      summary: Tallentaa oppilaitokset siirtotiedostoon
       |      operationId: reportOppilaitokset
       |      description: Hakee annetulla aikavälillä luodut/modifioidut oppilaitokset ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Raportointi
       |      parameters:
       |        - in: query
       |          name: startTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          example: ${SampleHttpDate}
       |        - in: query
       |          name: endTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          description: Jos arvoa ei ole annettu, asetetaan loppuajaksi nykyinen ajankohta.
       |          example: ${SampleHttpDate}
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/oppilaitokset") {
    implicit val authenticated: Authenticated = authenticate()

    Ok(
      raportointiService.saveOppilaitokset(
        parseDatetime(params.get("startTime")),
        parseDatetime(params.get("endTime"), Some(Instant.now()))
      )
    )
  }

  registerPath(
    "/raportointi/oppilaitoksenosat",
    s"""    get:
       |      summary: Tallentaa oppilaitoksen osat siirtotiedostoon
       |      operationId: reportOppilaitoksenOsat
       |      description: Hakee annetulla aikavälillä luodut/modifioidut oppilaitoksen osat ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Raportointi
       |      parameters:
       |        - in: query
       |          name: startTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          example: ${SampleHttpDate}
       |        - in: query
       |          name: endTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          description: Jos arvoa ei ole annettu, asetetaan loppuajaksi nykyinen ajankohta.
       |          example: ${SampleHttpDate}
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/oppilaitoksenosat") {
    implicit val authenticated: Authenticated = authenticate()

    Ok(
      raportointiService.saveOppilaitoksenOsat(
        parseDatetime(params.get("startTime")),
        parseDatetime(params.get("endTime"), Some(Instant.now()))
      )
    )
  }

  registerPath(
    "/raportointi/pistehistoria",
    s"""    get:
       |      summary: Tallentaa pistehistorian siirtotiedostoon
       |      operationId: reportPistehistoria
       |      description: Hakee annetulla aikavälillä luodut/modifioidut pistehistoriatiedot ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Raportointi
       |      parameters:
       |        - in: query
       |          name: startTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          example: ${SampleHttpDate}
       |        - in: query
       |          name: endTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          description: Jos arvoa ei ole annettu, asetetaan loppuajaksi nykyinen ajankohta.
       |          example: ${SampleHttpDate}
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/pistehistoria") {
    implicit val authenticated: Authenticated = authenticate()

    Ok(
      raportointiService.savePistehistoria(
        parseDatetime(params.get("startTime")),
        parseDatetime(params.get("endTime"), Some(Instant.now()))
      )
    )
  }

  registerPath(
    "/raportointi/ammattinimikkeet",
    s"""    get:
       |      summary: Tallentaa ammattinimikkeet siirtotiedostoon
       |      operationId: reportAmmattinimikkeet
       |      description: Hakee kaikki ammattinimikkeet ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Raportointi
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/ammattinimikkeet") {
    implicit val authenticated: Authenticated = authenticate()

    Ok(raportointiService.saveAmmattinimikkeet())
  }

  registerPath(
    "/raportointi/asiasanat",
    s"""    get:
       |      summary: Tallentaa asiasanat siirtotiedostoon
       |      operationId: reportAsiasanat
       |      description: Hakee kaikki asiasanat ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Raportointi
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/asiasanat") {
    implicit val authenticated: Authenticated = authenticate()

    Ok(raportointiService.saveAsiasanat())
  }
}
