package fi.oph.kouta.servlet

import java.net.{URLDecoder}

import fi.oph.kouta.domain.ListEverything
import fi.oph.kouta.service.ModificationService
import org.scalatra.Ok
import org.scalatra.swagger.Swagger

class AnythingServlet(implicit val swagger:Swagger) extends KoutaServlet {
  override val modelName: String = "Anything"
  override val applicationDescription = "Tarjonnan muutosten API"

  get("/modifiedSince/:since", operation(apiOperation[ListEverything]("Hae kaikki tietyn ajan jälkeen muuttuneet oidit")
    tags modelName
    summary "Hae kaikki tietyn ajan jälkeen muuttuneet oidit"
    parameter pathParam[String]("since").description(SampleHttpDate))) {

    Ok(ModificationService.getModifiedSince(parseHttpDate(URLDecoder.decode(params("since"), "UTF-8"))))
  }

  prettifySwaggerModels("ListEverything")

}