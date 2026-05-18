package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.domain.siirtotiedosto.{SiirtotiedostoDateTimeFormat, SiirtotiedostoOperationResults}
import fi.oph.kouta.security.Role
import fi.oph.kouta.service.SiirtotiedostoService
import org.scalatra.{ActionResult, Forbidden, Ok}

import java.time.LocalDateTime
import java.util.UUID
import scala.util.{Failure, Success, Try}

class SiirtotiedostoServlet(siirtotiedostoService: SiirtotiedostoService) extends KoutaServlet {
  def this() = this(SiirtotiedostoService)

  private def isOphPaakayttaja(authenticated: Authenticated): Boolean =
    authenticated.session.roles.contains(Role.Paakayttaja)

  private val DateTimeExample = SiirtotiedostoDateTimeFormat.format(LocalDateTime.now())

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
          case Failure(_)        => throw new IllegalArgumentException(s"Virheellinen $fieldName '$dateTimeStr'")
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

  private val ForbiddenResponse =
    Forbidden("error" -> "Käyttäjällä ei ole oikeutta siirtotiedostojen muodostamiseen rajapinnan kautta")

  private def saveWithStartAndEndIfOphPaakayttaja(
      saveFunction: (UUID, Option[LocalDateTime], Option[LocalDateTime]) => SiirtotiedostoOperationResults
  ): ActionResult = {
    implicit val authenticated: Authenticated = authenticate()

    if (isOphPaakayttaja(authenticated)) {
      val (startTime, endTime) =
        parseTimeRange(params.get("startTime"), params.get("endTime"), Some(LocalDateTime.now()))

      Ok(resultMap(saveFunction(UUID.randomUUID(), startTime, endTime)))
    } else {
      ForbiddenResponse
    }
  }

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
    saveWithStartAndEndIfOphPaakayttaja(siirtotiedostoService.saveKoulutukset)
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
    saveWithStartAndEndIfOphPaakayttaja(siirtotiedostoService.saveToteutukset)
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
    saveWithStartAndEndIfOphPaakayttaja(siirtotiedostoService.saveHakukohteet)
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
    saveWithStartAndEndIfOphPaakayttaja(siirtotiedostoService.saveHaut)
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
    saveWithStartAndEndIfOphPaakayttaja(siirtotiedostoService.saveValintaperusteet)
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
    saveWithStartAndEndIfOphPaakayttaja(siirtotiedostoService.saveSorakuvaukset)
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
    saveWithStartAndEndIfOphPaakayttaja(siirtotiedostoService.saveOppilaitoksetJaOsat)
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
    saveWithStartAndEndIfOphPaakayttaja(siirtotiedostoService.savePistehistoria)
  }

  private def saveIfOphPaakayttaja(saveFunction: UUID => SiirtotiedostoOperationResults): ActionResult = {
    implicit val authenticated: Authenticated = authenticate()

    if (isOphPaakayttaja(authenticated)) {
      Ok(resultMap(saveFunction(UUID.randomUUID())))
    } else {
      ForbiddenResponse
    }
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
    saveIfOphPaakayttaja(siirtotiedostoService.saveAmmattinimikkeet)
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
    saveIfOphPaakayttaja(siirtotiedostoService.saveAsiasanat)
  }
}
