package fi.oph.kouta.servlet

import fi.oph.kouta.domain.Komo
import fi.oph.kouta.service.KomoService
import org.scalatra.{NoContent, NotFound, Ok}
import org.scalatra.swagger.Swagger

class KomoServlet(implicit val swagger:Swagger) extends KoutaServlet {

  override val applicationDescription = "Koulutusmoduulien API"

  get("/:oid", operation(apiOperation[String]("Hae koulutusmoduuli")
    summary "Hae koulutusmoduuli"
    parameter pathParam[String]("oid").description("Koulutusmoduulin oid"))) {

    KomoService.get(params("oid")) match {
      case None => NotFound("error" -> "Unknown Komo")
      case Some(k) => Ok(k)
    }
  }

  post("/", operation(apiOperation[Unit]("Tallenna uusi koulutusmoduuli")
    summary "Tallenna uusi koulutusmoduuli"
    parameter bodyParam[Komo].description("Uusi koulutusmoduuli").required)) {

    KomoService.put(parsedBody.extract[Komo])
    Ok("message" -> "ok")
  }
}
