package fi.oph.kouta.servlet

import fi.oph.kouta.domain.Haku
import fi.oph.kouta.service.HakuService
import org.scalatra.{NotFound, Ok}
import org.scalatra.swagger.Swagger

class HakuServlet (implicit val swagger:Swagger) extends KoutaServlet {
  override val modelName: String = "Haku"
  override val applicationDescription = "Hakujen API"

  get("/:oid", operation(apiOperation[Haku]("Hae haku")
    tags modelName
    summary "Hae haku"
    parameter pathParam[String]("oid").description("Haun oid"))) {

    HakuService.get(params("oid")) match {
      case None => NotFound("error" -> "Unknown haku oid")
      case Some((k, l)) => Ok(k, headers = Map("Last-Modified" -> createLastModifiedHeader(l)))
    }
  }

  put("/", operation(apiOperation[Unit]("Tallenna uusi haku")
    tags modelName
    summary "Tallenna uusi haku"
    parameter bodyParam[Haku])) {

    HakuService.put(parsedBody.extract[Haku]) match {
      case oid => Ok("oid" -> oid.getOrElse(""))
    }
  }

  post("/", operation(apiOperation[Unit]("Muokkaa hakua")
    tags modelName
    summary "Muokkaa olemassa olevaa hakua"
    parameter bodyParam[Haku])) {

    HakuService.update(parsedBody.extract[Haku], getIfUnmodifiedSince) match {
      case updated => Ok("updated" -> updated)
    }
  }

  prettifySwaggerModels()
}
