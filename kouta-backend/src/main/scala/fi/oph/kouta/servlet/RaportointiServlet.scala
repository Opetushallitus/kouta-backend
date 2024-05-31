package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain.raportointi.{RaportointiDateTimeFormat, SiirtotiedostoOperationResults}
import fi.oph.kouta.repository.{KoutaDatabase, RaportointiDAO}
import fi.oph.kouta.service.RaportointiService
import fi.oph.kouta.util.TimeUtils.parseHttpDate
import fi.oph.kouta.servlet.KoutaServlet.SampleHttpDate
import org.scalatra.Ok

import java.net.URLDecoder
import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.util.{Failure, Success, Try}

class RaportointiServlet(raportointiService: RaportointiService) extends KoutaServlet {
  def this() = this(RaportointiService)


  val DateTimeExample = RaportointiDateTimeFormat.format(LocalDateTime.now())

  private def parseDatetime(
      dateTime: Option[String],
      fieldName: String,
      default: Option[LocalDateTime] = None
  ): Option[LocalDateTime] = {
    dateTime match {
      case Some(dateTimeStr) =>
        Try[LocalDateTime] {
          LocalDateTime.from(RaportointiDateTimeFormat.parse(dateTimeStr))
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
    Ok(resultMap(raportointiService.saveKoulutukset(UUID.randomUUID(), startTime, endTime)))
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

    Ok(resultMap(raportointiService.saveToteutukset(UUID.randomUUID(), startTime, endTime)))
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

    Ok(resultMap(raportointiService.saveHakukohteet(UUID.randomUUID(), startTime, endTime)))
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

    Ok(resultMap(raportointiService.saveHaut(UUID.randomUUID(), startTime, endTime)))
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

    Ok(resultMap(raportointiService.saveValintaperusteet(UUID.randomUUID(), startTime, endTime)))
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

    Ok(resultMap(raportointiService.saveSorakuvaukset(UUID.randomUUID(), startTime, endTime)))
  }

  registerPath(
    "/raportointi/oppilaitoksetJaOsat",
    s"""    get:
       |      summary: Tallentaa oppilaitokset ja niiden osat siirtotiedostoon
       |      operationId: reportOppilaitoksetJaOsat
       |      description: Hakee annetulla aikavälillä luodut/modifioidut oppilaitokset ja osat,
       |        ja tallentaa ne siirtotiedostoon (S3 -bucketiin)
       |      tags:
       |        - Raportointi
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

    Ok(resultMap(raportointiService.saveOppilaitoksetJaOsat(UUID.randomUUID(), startTime, endTime)))
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

    Ok(resultMap(raportointiService.savePistehistoria(UUID.randomUUID(), startTime, endTime)))
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

    Ok(resultMap(raportointiService.saveAmmattinimikkeet(UUID.randomUUID())))
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

    Ok(resultMap(raportointiService.saveAsiasanat(UUID.randomUUID())))
  }
}
