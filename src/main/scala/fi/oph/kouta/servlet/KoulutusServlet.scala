package fi.oph.kouta.servlet

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid}
import fi.oph.kouta.service.KoulutusService
import org.scalatra.swagger._
import org.scalatra.{NotFound, Ok}

class KoulutusServlet(koulutusService: KoulutusService)(implicit val swagger:Swagger) extends KoutaServlet {
  override val modelName = "Koulutus"
  override val applicationDescription = "Koulutusten API"

  def this()(implicit swagger:Swagger) = this(KoulutusService)

  registerModel[fi.oph.kouta.api.koulutus.Metadata]
  registerModel[fi.oph.kouta.api.koulutus.Koulutus]
  registerModel[fi.oph.kouta.api.koulutus.Nimi]
  registerModel[fi.oph.kouta.api.koulutus.Kuvaus]
  registerModel[fi.oph.kouta.api.koulutus.KuvausNimi]

  get("/:oid", operation(apiOperation[Koulutus]("Hae koulutus")
    tags modelName
    summary "Hae koulutus"
    parameter pathParam[String]("oid").description("Koulutuksen oid"))) {

    implicit val authenticated: Authenticated = authenticate

    koulutusService.get(KoulutusOid(params("oid"))) match {
      case None => NotFound("error" -> "Unknown koulutus oid")
      case Some((k, l)) => Ok(k, headers = Map("Last-Modified" -> createLastModifiedHeader(l)))
    }
  }

  put("/", operation(apiOperation[Unit]("Tallenna uusi koulutus")
    tags modelName
    summary "Tallenna uusi koulutus"
    parameter bodyParam[Koulutus])) {

    implicit val authenticated: Authenticated = authenticate

    koulutusService.put(parsedBody.extract[Koulutus]) match {
      case oid => Ok("oid" -> oid)
    }
  }

  post("/", operation(apiOperation[Unit]("Muokkaa koulutusta")
    tags modelName
    summary "Muokkaa olemassa olevaa koulutusta"
    parameter bodyParam[Koulutus])) {

    implicit val authenticated: Authenticated = authenticate

    koulutusService.update(parsedBody.extract[Koulutus], getIfUnmodifiedSince) match {
      case updated => Ok("updated" -> updated)
    }
  }

  get("/list", operation(apiOperation[List[KoulutusListItem]]("Listaa käytettävissä olevat koulutukset")
    tags modelName
    summary "Listaa niiden koulutusten perustiedot, joita organisaatio voi käyttää"
    parameter queryParam[String]("organisaatioOid").description("Valitun organisaation oid").required)) {

    implicit val authenticated: Authenticated = authenticate

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => NotFound()
      case Some(oid) => Ok(koulutusService.list(oid))
    }
  }

  get("/:oid/toteutukset", operation(apiOperation[List[Toteutus]]("Hae koulutuksen kaikki toteutukset")
    tags modelName
    summary "Palauttaa koulutuksen kaikki toteutukset indeksointia varten"
    parameter pathParam[String]("oid").description("Koulutuksen oid")
    parameter queryParam[Boolean]("vainJulkaistut").description("Palautetaanko vain julkaistut toteutukset").defaultValue(false))) {

    implicit val authenticated: Authenticated = authenticate

    Ok(koulutusService.toteutukset(KoulutusOid(params("oid")), params.get("vainJulkaistut").exists(_.toBoolean)))
  }

  get("/:oid/toteutukset/list", operation(apiOperation[List[ToteutusListItem]]("Listaa koulutuksen toteutukset")
    tags modelName
    summary "Listaa niiden koulutukseen kuuluvien toteutusten perustiedot, joihin organisaatiolla on oikeus"
    parameter pathParam[String]("oid").description("Koulutuksen oid")
    parameter queryParam[String]("organisaatioOid").description("Organisaation oid"))) {

    implicit val authenticated: Authenticated = authenticate

    params.get("organisaatioOid").map(OrganisaatioOid) match {
      case None => Ok(koulutusService.listToteutukset(KoulutusOid(params("oid"))))
      case Some(organisaatioOid) => Ok(koulutusService.listToteutukset(KoulutusOid(params("oid")), organisaatioOid))
    }
  }

  get("/:oid/hakutiedot", operation(apiOperation[List[Hakutieto]]("Hae koulutuksen kaikki hakutiedot")
    tags modelName
    summary "Palauttaa koulutuksen kaikki julkaistut hakutiedot indeksointia varten"
    parameter pathParam[String]("oid").description("Koulutuksen oid"))) {

    implicit val authenticated: Authenticated = authenticate

    Ok(koulutusService.hakutiedot(KoulutusOid(params("oid"))))
  }

  //prettifySwaggerModels()
}
