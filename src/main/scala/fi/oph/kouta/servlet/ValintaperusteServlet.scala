package fi.oph.kouta.servlet

import java.util.UUID

import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.domain.{IdListItem, Valintaperuste}
import fi.oph.kouta.service.ValintaperusteService
import org.scalatra.{NotFound, Ok}
import org.scalatra.swagger.Swagger

class ValintaperusteServlet(implicit val swagger:Swagger) extends KoutaServlet {
  override val applicationDescription = "Valintaperustekuvausten API"
  override val modelName = "Valintaperuste"

  get("/:id", operation(apiOperation[Valintaperuste]("Hae valintaperuste")
    tags modelName
    summary "Hae valintaperuste"
    parameter pathParam[String]("id").description("Valintaperusteen UUID"))) {

    ValintaperusteService.get(UUID.fromString(params("id"))) match {
      case None => NotFound("error" -> "Unknown valintaperuste id")
      case Some((k, l)) => Ok(k, headers = Map("Last-Modified" -> createLastModifiedHeader(l)))
    }
  }

  put("/", operation(apiOperation[Unit]("Tallenna uusi valintaperuste")
    tags modelName
    summary "Tallenna uusi valintaperuste"
    parameter bodyParam[Valintaperuste])) {

    ValintaperusteService.put(parsedBody.extract[Valintaperuste]) match {
      case id => Ok("id" -> id.getOrElse(""))
    }
  }

  post("/", operation(apiOperation[Unit]("Muokkaa valintaperusteta")
    tags modelName
    summary "Muokkaa olemassa olevaa valintaperusteta"
    parameter bodyParam[Valintaperuste])) {

    ValintaperusteService.update(parsedBody.extract[Valintaperuste], getIfUnmodifiedSince) match {
      case updated => Ok("updated" -> updated)
    }
  }

  get("/list", operation(apiOperation[List[IdListItem]]("Listaa kaikki valintaperustekuvaukset, joihin käyttäjällä on oikeudet")
    tags modelName
    summary "Listaa kaikki valintaperustekuvaukset, joihin käyttäjällä on oikeudet. Listaa voidaan rajata myös haun oidilla, jolloin kuvaukset rajataan haun kohdejoukoun perusteella."
    parameter queryParam[String]("organisaatioOid").description(s"Käyttäjän organisaation oid (TODO: tulee tulevaisuudessa CASista)")
    parameter queryParam[String]("hakuOid").description(s"Haun oid"))) {
    ( params.get("organisaatioOid"), params.get("hakuOid") ) match {
      case (None, _) => NotFound()
      case (Some(oid), None) => Ok(ValintaperusteService.list(OrganisaatioOid(oid)))
      case (Some(oid), Some(hakuOid)) => Ok(ValintaperusteService.listByHaunKohdejoukko(OrganisaatioOid(oid), HakuOid(hakuOid)))
    }
  }

  prettifySwaggerModels()

}
