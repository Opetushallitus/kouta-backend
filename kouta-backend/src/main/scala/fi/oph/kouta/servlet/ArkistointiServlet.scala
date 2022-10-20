package fi.oph.kouta.servlet

import fi.oph.kouta.SwaggerPaths.registerPath
import fi.oph.kouta.scheduler.ArkistointiTask
import org.scalatra.Ok

class ArkistointiServlet(arkistointiScheduler: ArkistointiTask) extends KoutaServlet {

  def this() = this(ArkistointiTask)
  registerPath(
    "/archiver/start",
    """    post:
      |      summary: Käynnistä hakujen arkistointi
      |      operationId: Käynnistä hakujen arkistointi
      |      description: Käynnistä hakujen arkistointi manuaalisesti. Arkistoi ja uudelleenindeksoi kaikki haut,
      |        joilla arkistointipäivämäärä on määritelty tai haun loppumisesta on yli 10 kuukautta.
      |        Arkistoi myös hakujen hakukohteet.
      |      tags:
      |        - Arkistointi
      |      responses:
      |        '200':
      |          description: Ok
      |""".stripMargin
  )
  post("/start") {
    implicit val authenticated: Authenticated = authenticate()
    Ok("started" -> arkistointiScheduler.runScheduler())
  }
}
