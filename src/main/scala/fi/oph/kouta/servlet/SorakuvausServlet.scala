package fi.oph.kouta.servlet

import java.util.UUID

import fi.oph.kouta.domain.{Sorakuvaus, SorakuvausListItem, ValintaperusteListItem}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.service.SorakuvausService
import org.scalatra.{NotFound, Ok}
import org.scalatra.swagger.Swagger

class SorakuvausServlet(sorakuvausService: SorakuvausService)(implicit val swagger: Swagger) extends KoutaServlet {
  override val applicationDescription = "SORA-kuvausten API"
  override val modelName = "Sorakuvaus"

  def this()(implicit swagger: Swagger) = this(SorakuvausService)

  get("/:id", operation(apiOperation[Sorakuvaus]("Hae SORA-kuvaus")
    tags modelName
    summary "Hae SORA-kuvaus"
    parameter pathParam[String]("id").description("SORA-kuvauksen UUID"))) {

    implicit val authenticated: Authenticated = authenticate

    sorakuvausService.get(UUID.fromString(params("id"))) match {
      case None => NotFound("error" -> "Unknown SORA-kuvaus id")
      case Some((k, l)) => Ok(k, headers = Map("Last-Modified" -> createLastModifiedHeader(l)))
    }
  }

  put("/", operation(apiOperation[Unit]("Tallenna uusi SORA-kuvaus")
    tags modelName
    summary "Tallenna uusi SORA-kuvaus"
    parameter bodyParam[Sorakuvaus])) {

    implicit val authenticated: Authenticated = authenticate

    sorakuvausService.put(parsedBody.extract[Sorakuvaus]) match {
      case id => Ok("id" -> id)
    }
  }

  post("/", operation(apiOperation[Unit]("Muokkaa SORA-kuvausta")
    tags modelName
    summary "Muokkaa olemassa olevaa SORA-kuvausta"
    parameter bodyParam[Sorakuvaus])) {

    implicit val authenticated: Authenticated = authenticate

    sorakuvausService.update(parsedBody.extract[Sorakuvaus], getIfUnmodifiedSince) match {
      case updated => Ok("updated" -> updated)
    }
  }

  get("/list", operation(apiOperation[List[SorakuvausListItem]]("Listaa kaikki SORA-kuvaukset, joihin käyttäjällä on oikeudet")
    tags modelName
    summary "Listaa kaikki SORA-kuvaukset, joihin käyttäjällä on oikeudet."
    parameter queryParam[String]("organisaatioOid").description(s"Käyttäjän organisaation oid (TODO: tulee tulevaisuudessa CASista)"))) {

    implicit val authenticated: Authenticated = authenticate

    params.get("organisaatioOid") match {
      case None => NotFound()
      case Some(oid) => Ok(sorakuvausService.list(OrganisaatioOid(oid)))
    }
  }

  get("/:id/valintaperusteet/list", operation(apiOperation[List[ValintaperusteListItem]]("Listaa kaikki valintaperusteet, jotka käyttävät annettua SORA-kuvausta")
    tags modelName
    summary "Listaa kaikki valintaperusteet, jotka käyttävät annettua SORA-kuvausta"
    parameter pathParam[String]("id").description("SORA-kuvauksen UUID"))) {

    implicit val authenticated: Authenticated = authenticate

    Ok(sorakuvausService.listValintaperusteet(UUID.fromString(params("id"))))
  }
}