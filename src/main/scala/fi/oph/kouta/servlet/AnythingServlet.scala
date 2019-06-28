package fi.oph.kouta.servlet

import java.net.URLDecoder

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.service.ModificationService
import fi.oph.kouta.util.TimeUtils.parseHttpDate
import org.scalatra.Ok

class AnythingServlet extends KoutaServlet {

  registerPath("/anything/modifiedSince/{since}",
    s"""    get:
       |      summary: Hakee listan kaikesta, mikä on muuttunut tietyn ajanhetken jälkeen
       |      operationId: Hae lista muuttuneista
       |      description: Hakee listan kaikesta, mikä on muuttunut tietyn ajanhetken jälkeen
       |      tags:
       |        - Anything
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

    implicit val authenticated: Authenticated = authenticate

    Ok(ModificationService.getModifiedSince(parseHttpDate(URLDecoder.decode(params("since"), "UTF-8"))))
  }
}
