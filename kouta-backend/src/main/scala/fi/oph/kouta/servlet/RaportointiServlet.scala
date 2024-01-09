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

  private def parseDatetime(dateTime: Option[String]): Option[Instant] = {
    dateTime match {
      case Some(dateTimeStr) => Some(parseHttpDate(URLDecoder.decode(dateTimeStr, "UTF-8")))
      case None => None
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
      |          example: ${SampleHttpDate}
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin
  )
  get("/koulutukset") {
    implicit val authenticated: Authenticated = authenticate()

    Ok(raportointiService.saveKoulutukset(parseDatetime(params.get("startTime")), parseDatetime(params.get("endTime"))))
  }
}
