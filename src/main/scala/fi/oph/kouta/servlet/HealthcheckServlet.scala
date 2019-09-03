package fi.oph.kouta.servlet

import fi.oph.kouta.swagger.SwaggerPaths.registerPath
import org.scalatra.Ok

class HealthcheckServlet extends KoutaServlet {

  registerPath("/healthcheck/",
    """    get:
      |      summary: Healthcheck-rajapinta
      |      description: Healthcheck-rajapinta
      |      tags:
      |        - Admin
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin)
  get("/") {
    Ok("message" -> "ok")
  }

}
