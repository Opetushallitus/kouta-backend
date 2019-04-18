package fi.oph.kouta.servlet

import fi.oph.kouta.domain.oid.{HakuOid, OrganisaatioOid}
import fi.oph.kouta.domain._
import fi.oph.kouta.service.HakuService
import org.scalatra.{NotFound, Ok}
import org.scalatra.swagger.Swagger

class HakuServlet (implicit val swagger: Swagger) extends KoutaServlet {
  override val modelName: String = "Haku"
  override val applicationDescription = "Hakujen API"

  get("/:oid", operation(apiOperation[Haku]("Hae haku")
    tags modelName
    summary "Hae haku"
    parameter pathParam[String]("oid").description("Haun oid"))) {

    implicit val authenticated: Authenticated = authenticate

    HakuService.get(HakuOid(params("oid"))) match {
      case None => NotFound("error" -> "Unknown haku oid")
      case Some((k, l)) => Ok(k, headers = Map("Last-Modified" -> createLastModifiedHeader(l)))
    }
  }

  put("/", operation(apiOperation[Unit]("Tallenna uusi haku")
    tags modelName
    summary "Tallenna uusi haku"
    parameter bodyParam[Haku])) {

    implicit val authenticated: Authenticated = authenticate

    HakuService.put(parsedBody.extract[Haku]) match {
      case oid => Ok("oid" -> oid)
    }
  }

  post("/", operation(apiOperation[Unit]("Muokkaa hakua")
    tags modelName
    summary "Muokkaa olemassa olevaa hakua"
    parameter bodyParam[Haku])) {

    implicit val authenticated: Authenticated = authenticate

    HakuService.update(parsedBody.extract[Haku], getIfUnmodifiedSince) match {
      case updated => Ok("updated" -> updated)
    }
  }

  get("/list", operation(apiOperation[List[HakuListItem]]("Listaa kaikki haut, joihin käyttäjällä on oikeudet")
    tags modelName
    summary "Listaa kaikki haut, joihin käyttäjällä on oikeudet"
    parameter queryParam[String]("organisaatioOid").description(s"Käyttäjän organisaation oid (TODO: tulee tulevaisuudessa CASista)"))) {

    implicit val authenticated: Authenticated = authenticate

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(oid) => Ok(HakuService.list(oid))
    }
  }

  get("/:oid/hakukohteet/list", operation(apiOperation[List[HakukohdeListItem]]("Listaa hakuun liitetyt hakukohteet")
    tags modelName
    summary "Listaa niiden hakuun liitettyjen hakukohteiden perustiedot, joihin organisaatiolla on oikeus"
    parameter pathParam[String]("oid").description("Haun oid")
    parameter queryParam[String]("organisaatioOid").description("Organisaation oid"))) {

    implicit val authenticated: Authenticated = authenticate

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => Ok(HakuService.listHakukohteet(HakuOid(params("oid")))) //TODO: Vain oph/indeksoija saa nähdä kaiken. Koskee myös muiden servletien vastaavia rajapintoja.
      case Some(organisaatioOid) => Ok(HakuService.listHakukohteet(HakuOid(params("oid")), organisaatioOid))
    }
  }
  get("/:oid/koulutukset/list", operation(apiOperation[List[KoulutusListItem]]("Listaa hakuun liittyvät koulutukset")
    tags modelName
    summary "Listaa hakuun liittyvät koulutukset"
    parameter pathParam[String]("oid").description("Haun oid"))) {

    implicit val authenticated: Authenticated = authenticate

    Ok(HakuService.listKoulutukset(HakuOid(params("oid"))))
  }

  prettifySwaggerModels()
}
