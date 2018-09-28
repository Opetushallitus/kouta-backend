package fi.oph.kouta.servlet

import fi.oph.kouta.domain.Julkaisutila.Julkaisutila
import fi.oph.kouta.domain.{Julkaisutila, Koulutus, Koulutustyyppi}
import fi.oph.kouta.domain.Koulutustyyppi.Koulutustyyppi
import fi.oph.kouta.service.KoulutusService
import org.scalatra.{NoContent, NotFound, Ok}
import org.scalatra.swagger._

class KoulutusServlet(implicit val swagger:Swagger) extends KoutaServlet {

  override val applicationDescription = "Koulutusten API"

  get("/:oid", operation(apiOperation[String]("Hae koulutus")
    summary "Hae koulutus"
    parameter pathParam[String]("oid").description("Koulutuksen oid"))) {

    KoulutusService.get(params("oid")) match {
      case None => NotFound("error" -> "Unknown koulutus oid")
      case Some(k) => Ok(k)
    }
  }

  registerModel( Model(
    id = classOf[Koulutus].getSimpleName,
    name = classOf[Koulutus].getSimpleName,
    properties = List(
      "oid" -> ModelProperty(`type` = DataType.String, required = true),
      "johtaaTutkintoon" -> ModelProperty(`type` = DataType.Boolean, required = true),
      "koulutustyyppi" -> ModelProperty(`type` = DataType.String, required = true),
      "koulutusKoodiUri" -> ModelProperty(`type` = DataType.String, required = true),
      "tila" -> ModelProperty(`type` = DataType.String, required = true),
      "tarjoajat" -> ModelProperty(`type` = DataType.GenList(DataType.String), required = true),
      "nimi" -> ModelProperty(`type` = DataType.GenMap(DataType.String), required = true),
      "kuvaus" -> ModelProperty(`type` = DataType.GenMap(DataType.String), required = true),
      "muokkaaja" -> ModelProperty(`type` = DataType.String, required = true))))

  post("/", operation(apiOperation[Unit]("Tallenna uusi koulutus")
    summary "Tallenna uusi koulutus"
    parameter bodyParam[Koulutus])) {

    KoulutusService.put(parsedBody.extract[Koulutus])
    Ok("message" -> "ok")
  }
}






/*
private val hakijanVastaanottoActionModel = Model(
    id = classOf[HakijanVastaanottoAction].getSimpleName,
    name = classOf[HakijanVastaanottoAction].getSimpleName,
    properties = List("action" -> ModelProperty(`type` = DataType.String, required = true, allowableValues = AllowableValues(HakijanVastaanottoAction.values))))
 */