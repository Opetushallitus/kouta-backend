package fi.oph.kouta

import java.time.{Instant, LocalDateTime}
import java.util.UUID

import fi.oph.kouta.domain.{Hakulomaketyyppi, Julkaisutila, Kieli, LiitteenToimitustapa}
import fi.oph.kouta.util.KoutaJsonFormats
import org.scalatra.ScalatraServlet
import org.scalatra.swagger.DataType.ValueDataType
import org.scalatra.swagger._

class KoutaBackendSwagger extends Swagger(
  Swagger.SpecVersion,
  "0.1-SNAPSHOT",
  ApiInfo("kouta-backend",
    "Uusi tarjonta",
    "https://opintopolku.fi/wp/fi/opintopolku/tietoa-palvelusta/",
    "verkkotoimitus_opintopolku@oph.fi",
    "EUPL 1.1 or latest approved by the European Commission",
    "http://www.osor.eu/eupl/"))

class SwaggerServlet(implicit val swagger: Swagger) extends ScalatraServlet with JacksonSwaggerBase

trait PrettySwaggerSupport extends SwaggerSupport with KoutaJsonFormats {

  val EXAMPLE_DATE_TIME = ISO_LOCAL_DATE_TIME_FORMATTER.format(LocalDateTime.MIN)
  val EXAMPLE_UUID = UUID.randomUUID().toString

  val modelName:String

  private def modelProperty(position:Int, values:List[String]) = ModelProperty(DataType.String, position, required = false, description = Some(values.mkString("/")))

  def prettifySwaggerModels() = {
    models.update(modelName, models(modelName).copy(properties = prettifyEnumModels(models(modelName))))
    removeRedundantModels()
    prettifyKielistetty()
    models.foreach(m => models.update(m._1, m._2.copy(properties = prettifyDatatypes(m._2))))
  }

  private def removeRedundantModels() = {
    models.remove("Hakulomaketyyppi")
    models.remove("Julkaisutila")
    models.remove("Koulutustyyppi")
    models.remove("LiitteenToimitustapa")
    models.remove("Kieli")
    models.remove("UUID")
    models.remove("Pattern")
    models.remove("LocalDate")
    models.remove("LocalDateTime")
    models.remove("LocalTime")
  }

  private def prettifyEnumModels(model:Model) = model.properties.map {
    case ("hakulomaketyyppi", mp) => ("hakulomaketyyppi", modelProperty(mp.position, Hakulomaketyyppi.values().map(_.toString)))
    case ("tila", mp) => ("tila", modelProperty(mp.position, Julkaisutila.values().map(_.toString)))
    //case ("koulutustyyppi", mp) => ("koulutustyyppi", modelProperty(mp.position, Koulutustyyppi.values().map(_.toString)))
    case ("kieli", mp) => ("kieli", modelProperty(mp.position, Kieli.values.map(_.toString)))
    case ("kielivalinta", mp) => ("kielivalinta", ModelProperty(DataType.GenList(DataType.String), mp.position, description = Some(s"[${Kieli.values.mkString(",")}]")))
    case ("liitteidenToimitustapa", mp) => ("liitteidenToimitustapa", modelProperty(mp.position, LiitteenToimitustapa.values().map(_.toString)))
    case ("toimitustapa", mp) => ("toimitustapa", modelProperty(mp.position, LiitteenToimitustapa.values().map(_.toString)))
    case ("id", mp) => ("id", modelProperty(mp.position, List(EXAMPLE_UUID)))
    case p => p
  }

  private def prettifyKielistetty() = {
    models.put("Kielistetty", new Model("Kielistetty", "Kielistetty", properties = Kieli.values.zipWithIndex.map { case (k, i) =>
      (k.toString, modelProperty(i+1, List(s"nimi ${k.toString}")))
    }))
    models.put("Keyword", new Model("Keyword", "Keyword", properties = List(
      ("kieli", modelProperty(0, Kieli.values().map(_.toString))),
      ("arvo", ModelProperty(DataType.String, 1))
    )))
  }

  private def prettifyDatatypes(model:Model) = model.properties.map {
    case (name, mp) if mp.`type`.name.equals("Instant") => (name, modelProperty(mp.position, List(EXAMPLE_DATE_TIME)))
    case (name, mp) if mp.`type`.name.equals("Map") => (name, ModelProperty(new ValueDataType("Kielistetty", None, Some("fi.oph.kouta.domain.Kielistetty")), mp.position))
    case (name, mp) if mp.`type`.name.equals("UUID") => (name, modelProperty(mp.position, List(EXAMPLE_UUID)))
    //case (name, mp) if mp.`type`.name.equals("Kieli") => (name, ModelProperty(DataType.GenList(DataType.String), mp.position, description = Some(s"[${Kieli.values.mkString(",")}]")))
    case p => p
  }
}