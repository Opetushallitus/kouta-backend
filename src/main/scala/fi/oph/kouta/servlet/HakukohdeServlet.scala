package fi.oph.kouta.servlet

import fi.oph.kouta.domain.Hakukohde
import fi.oph.kouta.domain.oid.HakukohdeOid
import fi.oph.kouta.service.HakukohdeService
import org.scalatra.{NotFound, Ok}
import org.scalatra.swagger.Swagger

class HakukohdeServlet (implicit val swagger:Swagger) extends KoutaServlet {
  override val modelName: String = "Hakukohde"
  override val applicationDescription = "Hakukohteiden API"

  get("/:oid", operation(apiOperation[Hakukohde]("Hae hakukohde")
    tags modelName
    summary "Hae hakukohde"
    parameter pathParam[String]("oid").description("Hakukohteen oid"))) {

    HakukohdeService.get(HakukohdeOid(params("oid"))) match {
      case None => NotFound("error" -> "Unknown hakukohde oid")
      case Some((k, l)) => Ok(k, headers = Map("Last-Modified" -> createLastModifiedHeader(l)))
    }
  }

  put("/", operation(apiOperation[Unit]("Tallenna uusi hakukohde")
    tags modelName
    summary "Tallenna uusi hakukohde"
    parameter bodyParam[Hakukohde])) {

    HakukohdeService.put(parsedBody.extract[Hakukohde]) match {
      case oid => Ok("oid" -> oid)
    }
  }

  post("/", operation(apiOperation[Unit]("Muokkaa hakua")
    tags modelName
    summary "Muokkaa olemassa olevaa hakukohdetta"
    parameter bodyParam[Hakukohde])) {

    HakukohdeService.update(parsedBody.extract[Hakukohde], getIfUnmodifiedSince) match {
      case updated => Ok("updated" -> updated)
    }
  }

  prettifySwaggerModels()
}
