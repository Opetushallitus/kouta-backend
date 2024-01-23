package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.service.RaportointiService
import fi.oph.kouta.util.TimeUtils.parseHttpDate
import fi.oph.kouta.servlet.KoutaServlet.SampleHttpDate
import org.scalatra.Ok

import java.net.URLDecoder
import java.time.Instant
import scala.util.{Failure, Success, Try}

class RaportointiServlet(raportointiService: RaportointiService) extends KoutaServlet {
  def this() = this(RaportointiService)

  private def parseDatetime(
      dateTime: Option[String],
      fieldName: String,
      default: Option[Instant] = None
  ): Option[Instant] = {
    dateTime match {
      case Some(dateTimeStr) =>
        Try[Instant] {
          parseHttpDate(URLDecoder.decode(dateTimeStr, "UTF-8"))
        } match {
          case Success(instantVal) => Some(instantVal)
          case Failure(_)          => throw new IllegalArgumentException(s"Virheellinen $fieldName '$dateTimeStr'")
        }
      case None => default
    }
  }

  private def parseTimeRange(
      startTime: Option[String],
      endTime: Option[String],
      defaultEndTime: Option[Instant]
  ): (Option[Instant], Option[Instant]) = {
    val startTimeVal = parseDatetime(startTime, "alkuaika")
    val endTimeVal   = parseDatetime(endTime, "loppuaika", defaultEndTime)
    (startTimeVal, endTimeVal) match {
      case (Some(startTimeVal), _) if startTimeVal.isAfter(Instant.now()) =>
        throw new IllegalArgumentException("Alkuaika ei voi olla tulevaisuudessa")
      case (Some(startTimeVal), Some(endTimeVal)) if startTimeVal.isAfter(endTimeVal) =>
        throw new IllegalArgumentException("Alkuaika ei voi olla loppuajan jälkeen")
      case (_, _) => (startTimeVal, endTimeVal)
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
    val (startTime, endTime)                  = parseTimeRange(params.get("startTime"), params.get("endTime"), Some(Instant.now()))
    Ok(raportointiService.saveKoulutukset(startTime, endTime))
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
    val (startTime, endTime)                  = parseTimeRange(params.get("startTime"), params.get("endTime"), Some(Instant.now()))

    Ok(raportointiService.saveToteutukset(startTime, endTime))
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
    val (startTime, endTime)                  = parseTimeRange(params.get("startTime"), params.get("endTime"), Some(Instant.now()))

    Ok(raportointiService.saveHakukohteet(startTime, endTime))
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
    val (startTime, endTime)                  = parseTimeRange(params.get("startTime"), params.get("endTime"), Some(Instant.now()))

    Ok(raportointiService.saveHaut(startTime, endTime))
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
    val (startTime, endTime)                  = parseTimeRange(params.get("startTime"), params.get("endTime"), Some(Instant.now()))

    Ok(raportointiService.saveValintaperusteet(startTime, endTime))
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
    val (startTime, endTime)                  = parseTimeRange(params.get("startTime"), params.get("endTime"), Some(Instant.now()))

    Ok(raportointiService.saveSorakuvaukset(startTime, endTime))
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
    val (startTime, endTime)                  = parseTimeRange(params.get("startTime"), params.get("endTime"), Some(Instant.now()))

    Ok(raportointiService.saveOppilaitokset(startTime, endTime))
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
    val (startTime, endTime)                  = parseTimeRange(params.get("startTime"), params.get("endTime"), Some(Instant.now()))

    Ok(raportointiService.saveOppilaitoksenOsat(startTime, endTime))
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
    val (startTime, endTime)                  = parseTimeRange(params.get("startTime"), params.get("endTime"), Some(Instant.now()))

    Ok(raportointiService.savePistehistoria(startTime, endTime))
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
