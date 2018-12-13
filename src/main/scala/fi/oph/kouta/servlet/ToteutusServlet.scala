package fi.oph.kouta.servlet

import fi.oph.kouta.domain.{Julkaisutila, OidListItem, Toteutus}
import fi.oph.kouta.service.ToteutusService
import org.scalatra.{NotFound, Ok}
import org.scalatra.swagger.Swagger

class ToteutusServlet(implicit val swagger:Swagger) extends KoutaServlet {
  override val applicationDescription = "Koulutusten toteutusten API"
  override val modelName = "Toteutus"

  get("/:oid", operation(apiOperation[Toteutus]("Hae toteutus")
    tags modelName
    summary "Hae toteutus"
    parameter pathParam[String]("oid").description("Toteutuksen oid"))) {

    ToteutusService.get(params("oid")) match {
      case None => NotFound("error" -> "Unknown toteutus oid")
      case Some((k, l)) => Ok(k, headers = Map("Last-Modified" -> createLastModifiedHeader(l)))
    }
  }

  put("/", operation(apiOperation[Unit]("Tallenna uusi toteutus")
    tags modelName
    summary "Tallenna uusi toteutus"
    parameter bodyParam[Toteutus])) {

    ToteutusService.put(parsedBody.extract[Toteutus]) match {
      case oid => Ok("oid" -> oid.getOrElse(""))
    }
  }

  post("/", operation(apiOperation[Unit]("Muokkaa toteutusta")
    tags modelName
    summary "Muokkaa olemassa olevaa toteutusta"
    parameter bodyParam[Toteutus])) {

    ToteutusService.update(parsedBody.extract[Toteutus], getIfUnmodifiedSince) match {
      case updated => Ok("updated" -> updated)
    }
  }

  get("/list", operation(apiOperation[List[OidListItem]]("Listaa kaikki toteutukset, joihin käyttäjällä on oikeudet")
    tags modelName
    summary "Listaa kaikki toteutukset, joihin käyttäjällä on oikeudet"
    parameter queryParam[String]("organisaatioOid").description(s"Käyttäjän organisaation oid (TODO: tulee tulevaisuudessa CASista)"))) {
    params.get("organisaatioOid") match {
      case None => NotFound()
      case Some(oid) => Ok(ToteutusService.list(oid))
    }
  }

  prettifySwaggerModels()
}
