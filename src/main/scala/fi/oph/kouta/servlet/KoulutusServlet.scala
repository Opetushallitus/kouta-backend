package fi.oph.kouta.servlet

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid}
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

    KoulutusService.get(KoulutusOid(params("oid"))) match {
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

  get("/list", operation(apiOperation[List[OidListItem]]("Listaa käytettävissä olevat koulutukset")
    tags modelName
    summary "Listaa niiden koulutusten perustiedot, joita organisaatio voi käyttää"
    parameter queryParam[String]("organisaatioOid").description("Organisaation oid").required)) {
    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(oid) => Ok(KoulutusService.list(oid))
    }
  }

  get("/:oid/toteutukset", operation(apiOperation[List[Toteutus]]("Hae koulutuksen kaikki toteutukset")
    tags modelName
    summary "Palauttaa koulutuksen kaikki toteutukset indeksointia varten"
    parameter pathParam[String]("oid").description("Koulutuksen oid")
    parameter queryParam[Boolean]("vainJulkaistut").description("Palautetaanko vain julkaistut toteutukset").defaultValue(false))) {
    Ok(KoulutusService.toteutukset(KoulutusOid(params("oid")), params.get("vainJulkaistut").map(_.toBoolean)))
  }

  get("/:oid/toteutukset/list", operation(apiOperation[List[OidListItem]]("Listaa koulutuksen toteutukset")
    tags modelName
    summary "Listaa niiden koulutukseen kuuluvien toteutusten perustiedot, joihin organisaatiolla on oikeus"
    parameter pathParam[String]("oid").description("Koulutuksen oid")
    parameter queryParam[String]("organisaatioOid").description("Organisaation oid"))) {
    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => Ok(KoulutusService.listToteutukset(KoulutusOid(params("oid")))) //TODO: Vain oph/indeksoija saa hakea kaiken
      case Some(organisaatioOid) => Ok(KoulutusService.listToteutukset(KoulutusOid(params("oid")), organisaatioOid))
    }
  }

  get("/:oid/hakutiedot", operation(apiOperation[List[Hakutieto]]("Hae koulutuksen kaikki hakutiedot")
    tags modelName
    summary "Palauttaa koulutuksen kaikki julkaistut hakutiedot indeksointia varten"
    parameter pathParam[String]("oid").description("Koulutuksen oid"))) {
    Ok(KoulutusService.hakutiedot(KoulutusOid(params("oid"))))
  }

  prettifySwaggerModels()
}