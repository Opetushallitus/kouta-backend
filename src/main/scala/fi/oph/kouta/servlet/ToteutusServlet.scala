package fi.oph.kouta.servlet

import fi.oph.kouta.domain.oid.{OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.domain._
import fi.oph.kouta.service.ToteutusService
import org.scalatra.{NotFound, Ok}
import org.scalatra.swagger.Swagger

class ToteutusServlet(toteutusService: ToteutusService)(implicit val swagger: Swagger) extends KoutaServlet {
  override val applicationDescription = "Koulutusten toteutusten API"
  override val modelName = "Toteutus"

  def this()(implicit swagger: Swagger) = this(ToteutusService)

  get("/:oid", operation(apiOperation[Toteutus]("Hae toteutus")
    tags modelName
    summary "Hae toteutus"
    parameter pathParam[String]("oid").description("Toteutuksen oid"))) {

    implicit val authenticated: Authenticated = authenticate

    toteutusService.get(ToteutusOid(params("oid"))) match {
      case None => NotFound("error" -> "Unknown toteutus oid")
      case Some((k, l)) => Ok(k, headers = Map("Last-Modified" -> createLastModifiedHeader(l)))
    }
  }

  put("/", operation(apiOperation[Unit]("Tallenna uusi toteutus")
    tags modelName
    summary "Tallenna uusi toteutus"
    parameter bodyParam[Toteutus])) {

    implicit val authenticated: Authenticated = authenticate

    toteutusService.put(parsedBody.extract[Toteutus]) match {
      case oid => Ok("oid" -> oid)
    }
  }

  post("/", operation(apiOperation[Unit]("Muokkaa toteutusta")
    tags modelName
    summary "Muokkaa olemassa olevaa toteutusta"
    parameter bodyParam[Toteutus])) {

    implicit val authenticated: Authenticated = authenticate

    toteutusService.update(parsedBody.extract[Toteutus], getIfUnmodifiedSince) match {
      case updated => Ok("updated" -> updated)
    }
  }

  get("/list", operation(apiOperation[List[ToteutusListItem]]("Listaa kaikki toteutukset, joihin käyttäjällä on oikeudet")
    tags modelName
    summary "Listaa kaikki toteutukset, joihin käyttäjällä on oikeudet"
    parameter queryParam[String]("organisaatioOid").description(s"Käyttäjän organisaation oid (TODO: tulee tulevaisuudessa CASista)"))) {

    implicit val authenticated: Authenticated = authenticate

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(oid) => Ok(toteutusService.list(oid))
    }
  }

  get("/:oid/haut/list", operation(apiOperation[List[HakuListItem]]("Listaa toteutukseen liitetyt haut")
    tags modelName
    summary "Listaa toteutukseen liitetyt haut"
    parameter pathParam[String]("oid").description("Toteutuksen oid"))) {

    implicit val authenticated: Authenticated = authenticate

    Ok(toteutusService.listHaut(ToteutusOid(params("oid"))))
  }

  get("/:oid/hakukohteet/list", operation(apiOperation[List[HakukohdeListItem]]("Listaa toteutukseen liitetyt hakukohteet")
    tags modelName
    summary "Listaa toteutukseen liitetyt hakukohteet"
    parameter pathParam[String]("oid").description("Toteutuksen oid"))) {

    implicit val authenticated: Authenticated = authenticate

    Ok(toteutusService.listHakukohteet(ToteutusOid(params("oid"))))
  }

  prettifySwaggerModels()
}
