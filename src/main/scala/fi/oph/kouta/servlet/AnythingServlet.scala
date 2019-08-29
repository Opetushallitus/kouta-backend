package fi.oph.kouta.servlet

import java.net.URLDecoder

import fi.oph.kouta.SwaggerYaml.registerPath
import fi.oph.kouta.service.ModificationService
import org.scalatra.Ok
import org.scalatra.swagger.Swagger

class AnythingServlet(implicit val swagger:Swagger) extends KoutaServlet {
  override val modelName: String = "Anything"
  override val applicationDescription = "Tarjonnan muutosten API"

  registerPath("/anything/modifiedSince/{since}",
    s"""    get:
       |      summary: Hakee listan kaikesta, mikä on muuttunut tietyn ajanhetken jälkeen
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
