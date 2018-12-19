package fi.oph.kouta.servlet

import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.domain.{Haku, OidListItem}
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

    HakuService.get(HakuOid(params("oid"))) match {
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

  get("/list", operation(apiOperation[List[OidListItem]]("Listaa kaikki haut, joihin käyttäjällä on oikeudet")
    tags modelName
    summary "Listaa kaikki haut, joihin käyttäjällä on oikeudet"
    parameter queryParam[String]("organisaatioOid").description(s"Käyttäjän organisaation oid (TODO: tulee tulevaisuudessa CASista)"))) {
    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(oid) => Ok(HakuService.list(oid))
    }
  }

  get("/:oid/hakukohteet/list", operation(apiOperation[List[OidListItem]]("Listaa hakuun liitetyt hakukohteet")
    tags modelName
    summary "Listaa niiden hakuun liitettyjen hakukohteiden perustiedot, joihin organisaatiolla on oikeus"
    parameter pathParam[String]("oid").description("Haun oid")
    parameter queryParam[String]("organisaatioOid").description("Organisaation oid").required)) {
    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(organisaatioOid) => Ok(HakuService.listHakukohteet(HakuOid(params("oid")), organisaatioOid))
    }
  }

  prettifySwaggerModels()
}
