package fi.oph.kouta.servlet

import fi.oph.kouta.domain._
import fi.oph.kouta.service.KoulutusService
import org.scalatra.{NotFound, Ok}
import org.scalatra.swagger._

class KoulutusServlet(implicit val swagger:Swagger) extends KoutaServlet {
  override val modelName = "Koulutus"
  override val applicationDescription = "Koulutusten API"

  get("/:oid", operation(apiOperation[Koulutus]("Hae koulutus")
    tags modelName
    summary "Hae koulutus"
    parameter pathParam[String]("oid").description("Koulutuksen oid"))) {

    KoulutusService.get(params("oid")) match {
      case None => NotFound("error" -> "Unknown koulutus oid")
      case Some((k, l)) => Ok(k, headers = Map("Last-Modified" -> createLastModifiedHeader(l)))
    }
  }

  put("/", operation(apiOperation[Unit]("Tallenna uusi koulutus")
    tags modelName
    summary "Tallenna uusi koulutus"
    parameter bodyParam[Koulutus])) {

    KoulutusService.put(parsedBody.extract[Koulutus]) match {
      case oid => Ok("oid" -> oid.getOrElse(""))
    }
  }

  post("/", operation(apiOperation[Unit]("Muokkaa koulutusta")
    tags modelName
    summary "Muokkaa olemassa olevaa koulutusta"
    parameter bodyParam[Koulutus])) {

    KoulutusService.update(parsedBody.extract[Koulutus], getIfUnmodifiedSince) match {
      case updated => Ok("updated" -> updated)
    }
  }

  get("/list", operation(apiOperation[List[OidListItem]]("Listaa kaikki koulutukset, joihin käyttäjällä on oikeudet")
    tags modelName
    summary "Listaa kaikki koulutukset, joihin käyttäjällä on oikeudet"
    parameter queryParam[String]("organisaatioOid").description(s"Käyttäjän organisaation oid (TODO: tulee tulevaisuudessa CASista)"))) {
    params.get("organisaatioOid") match {
      case None => NotFound()
      case Some(oid) => Ok(KoulutusService.list(oid))
    }
  }

  get("/:oid/toteutukset", operation(apiOperation[List[Toteutus]]("Palauttaa koulutuksen kaikki toteutukset")
    tags modelName
    summary "Palauttaa koulutuksen kaikki toteutukset"
    parameter pathParam[String]("oid").description("Koulutuksen oid"))) {

    Ok(KoulutusService.toteutukset(params("oid")))
  }

  prettifySwaggerModels()
}