package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain.siirtotiedosto.{SiirtotiedostoDateTimeFormat, SiirtotiedostoOperationResults}
import fi.oph.kouta.repository.{KoutaDatabase, SiirtotiedostoDAO}
import fi.oph.kouta.service.SiirtotiedostoService
import fi.oph.kouta.util.TimeUtils.parseHttpDate
import fi.oph.kouta.servlet.KoutaServlet.SampleHttpDate
import org.scalatra.Ok

import java.net.URLDecoder
import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.util.{Failure, Success, Try}

class SiirtotiedostoServlet(siirtotiedostoService: SiirtotiedostoService) extends KoutaServlet {
  def this() = this(SiirtotiedostoService)


  val DateTimeExample = SiirtotiedostoDateTimeFormat.format(LocalDateTime.now())

  private def parseDatetime(
      dateTime: Option[String],
      fieldName: String,
      default: Option[LocalDateTime] = None
  ): Option[LocalDateTime] = {
    dateTime match {
      case Some(dateTimeStr) =>
        Try[LocalDateTime] {
          LocalDateTime.from(SiirtotiedostoDateTimeFormat.parse(dateTimeStr))
        } match {
          case Success(dateTime) => Some(dateTime)
          case Failure(_)          => throw new IllegalArgumentException(s"Virheellinen $fieldName '$dateTimeStr'")
        }
      case None => default
    }
  }

  private def parseTimeRange(
      startTime: Option[String],
      endTime: Option[String],
      defaultEndTime: Option[LocalDateTime]
  ): (Option[LocalDateTime], Option[LocalDateTime]) = {
    val startTimeVal = parseDatetime(startTime, "alkuaika")
    val endTimeVal   = parseDatetime(endTime, "loppuaika", defaultEndTime)
    (startTimeVal, endTimeVal) match {
      case (Some(startTimeVal), _) if startTimeVal.isAfter(LocalDateTime.now()) =>
        throw new IllegalArgumentException("Alkuaika ei voi olla tulevaisuudessa")
      case (Some(startTimeVal), Some(endTimeVal)) if startTimeVal.isAfter(endTimeVal) =>
        throw new IllegalArgumentException("Alkuaika ei voi olla loppuajan jälkeen")
      case (_, _) => (startTimeVal, endTimeVal)
    }
  }

  private def resultMap(operationResults: SiirtotiedostoOperationResults) =
    Map("keys" -> operationResults.keys.mkString(", "), "count" -> operationResults.count.toString, "success" -> "true")

  registerPath(
    "/siirtotiedosto/koulutukset",
    s"""    get:
       |      summary: Tallentaa koulutukset siirtotiedostoon
       |      operationId: siirtotiedostoKoulutukset
       |      description: Hakee annetulla aikavälillä luodut/modifioidut koulutukset ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Siirtotiedostot
       |      parameters:
       |        - in: query
       |          name: startTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          example: '${DateTimeExample}'
       |        - in: query
       |          name: endTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          description: Jos arvoa ei ole annettu, asetetaan loppuajaksi nykyinen ajankohta.
       |          example: '${DateTimeExample}'
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/koulutukset") {
    implicit val authenticated: Authenticated = authenticate()
    val (startTime, endTime)                  = parseTimeRange(params.get("startTime"), params.get("endTime"), Some(LocalDateTime.now()))
    Ok(resultMap(siirtotiedostoService.saveKoulutukset(UUID.randomUUID(), startTime, endTime)))
  }

  registerPath(
    "/siirtotiedosto/toteutukset",
    s"""    get:
       |      summary: Tallentaa toteutukset siirtotiedostoon
       |      operationId: siirtotiedostoToteutukset
       |      description: Hakee annetulla aikavälillä luodut/modifioidut toteutukset ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Siirtotiedostot
       |      parameters:
       |        - in: query
       |          name: startTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          example: '${DateTimeExample}'
       |        - in: query
       |          name: endTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          description: Jos arvoa ei ole annettu, asetetaan loppuajaksi nykyinen ajankohta.
       |          example: '${DateTimeExample}'
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/toteutukset") {
    implicit val authenticated: Authenticated = authenticate()
    val (startTime, endTime)                  = parseTimeRange(params.get("startTime"), params.get("endTime"), Some(LocalDateTime.now()))

    Ok(resultMap(siirtotiedostoService.saveToteutukset(UUID.randomUUID(), startTime, endTime)))
  }

  registerPath(
    "/siirtotiedosto/hakukohteet",
    s"""    get:
       |      summary: Tallentaa hakukohteet siirtotiedostoon
       |      operationId: siirtotiedostoHakukohteet
       |      description: Hakee annetulla aikavälillä luodut/modifioidut hakukohteet ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Siirtotiedostot
       |      parameters:
       |        - in: query
       |          name: startTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          example: '${DateTimeExample}'
       |        - in: query
       |          name: endTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          description: Jos arvoa ei ole annettu, asetetaan loppuajaksi nykyinen ajankohta.
       |          example: '${DateTimeExample}'
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/hakukohteet") {
    implicit val authenticated: Authenticated = authenticate()
    val (startTime, endTime)                  = parseTimeRange(params.get("startTime"), params.get("endTime"), Some(LocalDateTime.now()))

    Ok(resultMap(siirtotiedostoService.saveHakukohteet(UUID.randomUUID(), startTime, endTime)))
  }

  registerPath(
    "/siirtotiedosto/haut",
    s"""    get:
       |      summary: Tallentaa haut siirtotiedostoon
       |      operationId: siirtotiedostoHaut
       |      description: Hakee annetulla aikavälillä luodut/modifioidut haut ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Siirtotiedostot
       |      parameters:
       |        - in: query
       |          name: startTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          example: '${DateTimeExample}'
       |        - in: query
       |          name: endTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          description: Jos arvoa ei ole annettu, asetetaan loppuajaksi nykyinen ajankohta.
       |          example: '${DateTimeExample}'
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/haut") {
    implicit val authenticated: Authenticated = authenticate()
    val (startTime, endTime)                  = parseTimeRange(params.get("startTime"), params.get("endTime"), Some(LocalDateTime.now()))

    Ok(resultMap(siirtotiedostoService.saveHaut(UUID.randomUUID(), startTime, endTime)))
  }

  registerPath(
    "/siirtotiedosto/valintaperusteet",
    s"""    get:
       |      summary: Tallentaa valintaperusteet siirtotiedostoon
       |      operationId: siirtotiedostoValintaperusteet
       |      description: Hakee annetulla aikavälillä luodut/modifioidut valintaperusteet ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Siirtotiedostot
       |      parameters:
       |        - in: query
       |          name: startTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          example: '${DateTimeExample}'
       |        - in: query
       |          name: endTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          description: Jos arvoa ei ole annettu, asetetaan loppuajaksi nykyinen ajankohta.
       |          example: '${DateTimeExample}'
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/valintaperusteet") {
    implicit val authenticated: Authenticated = authenticate()
    val (startTime, endTime)                  = parseTimeRange(params.get("startTime"), params.get("endTime"), Some(LocalDateTime.now()))

    Ok(resultMap(siirtotiedostoService.saveValintaperusteet(UUID.randomUUID(), startTime, endTime)))
  }

  registerPath(
    "/siirtotiedosto/sorakuvaukset",
    s"""    get:
       |      summary: Tallentaa sorakuvaukset siirtotiedostoon
       |      operationId: siirtotiedostoSorakuvaukset
       |      description: Hakee annetulla aikavälillä luodut/modifioidut sorakuvaukset ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Siirtotiedostot
       |      parameters:
       |        - in: query
       |          name: startTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          example: '${DateTimeExample}'
       |        - in: query
       |          name: endTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          description: Jos arvoa ei ole annettu, asetetaan loppuajaksi nykyinen ajankohta.
       |          example: '${DateTimeExample}'
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/sorakuvaukset") {
    implicit val authenticated: Authenticated = authenticate()
    val (startTime, endTime)                  = parseTimeRange(params.get("startTime"), params.get("endTime"), Some(LocalDateTime.now()))

    Ok(resultMap(siirtotiedostoService.saveSorakuvaukset(UUID.randomUUID(), startTime, endTime)))
  }

  registerPath(
    "/siirtotiedosto/oppilaitoksetJaOsat",
    s"""    get:
       |      summary: Tallentaa oppilaitokset ja niiden osat siirtotiedostoon
       |      operationId: siirtotiedostoOppilaitoksetJaOsat
       |      description: Hakee annetulla aikavälillä luodut/modifioidut oppilaitokset ja osat,
       |        ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Siirtotiedostot
       |      parameters:
       |        - in: query
       |          name: startTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          example: '${DateTimeExample}'
       |        - in: query
       |          name: endTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          description: Jos arvoa ei ole annettu, asetetaan loppuajaksi nykyinen ajankohta.
       |          example: '${DateTimeExample}'
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/oppilaitoksetJaOsat") {
    implicit val authenticated: Authenticated = authenticate()
    val (startTime, endTime)                  = parseTimeRange(params.get("startTime"), params.get("endTime"), Some(LocalDateTime.now()))

    Ok(resultMap(siirtotiedostoService.saveOppilaitoksetJaOsat(UUID.randomUUID(), startTime, endTime)))
  }

  registerPath(
    "/siirtotiedosto/pistehistoria",
    s"""    get:
       |      summary: Tallentaa pistehistorian siirtotiedostoon
       |      operationId: siirtotiedostoPistehistoria
       |      description: Hakee annetulla aikavälillä luodut/modifioidut pistehistoriatiedot ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Siirtotiedostot
       |      parameters:
       |        - in: query
       |          name: startTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          example: '${DateTimeExample}'
       |        - in: query
       |          name: endTime
       |          schema:
       |            type: string
       |            format: date-time
       |          required: false
       |          description: Jos arvoa ei ole annettu, asetetaan loppuajaksi nykyinen ajankohta.
       |          example: '${DateTimeExample}'
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/pistehistoria") {
    implicit val authenticated: Authenticated = authenticate()
    val (startTime, endTime)                  = parseTimeRange(params.get("startTime"), params.get("endTime"), Some(LocalDateTime.now()))

    Ok(resultMap(siirtotiedostoService.savePistehistoria(UUID.randomUUID(), startTime, endTime)))
  }

  registerPath(
    "/siirtotiedosto/ammattinimikkeet",
    s"""    get:
       |      summary: Tallentaa ammattinimikkeet siirtotiedostoon
       |      operationId: siirtotiedostoAmmattinimikkeet
       |      description: Hakee kaikki ammattinimikkeet ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Siirtotiedostot
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/ammattinimikkeet") {
    implicit val authenticated: Authenticated = authenticate()

    Ok(resultMap(siirtotiedostoService.saveAmmattinimikkeet(UUID.randomUUID())))
  }

  registerPath(
    "/siirtotiedosto/asiasanat",
    s"""    get:
       |      summary: Tallentaa asiasanat siirtotiedostoon
       |      operationId: siirtotiedostoAsiasanat
       |      description: Hakee kaikki asiasanat ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Siirtotiedostot
       |      responses:
       |        '200':
       |          description: Ok
       |""".stripMargin
  )
  get("/asiasanat") {
    implicit val authenticated: Authenticated = authenticate()

    Ok(resultMap(siirtotiedostoService.saveAsiasanat(UUID.randomUUID())))
  }
}
