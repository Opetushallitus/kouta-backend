package fi.oph.kouta

import java.time.{Instant, LocalDateTime}
import java.util.UUID

import fi.oph.kouta.util.KoutaJsonFormats
import org.scalatra.{Ok, ScalatraServlet}
import org.scalatra.swagger.DataType.{ContainerDataType, ValueDataType}
import org.scalatra.swagger._

import scala.collection.immutable.ListMap
import scala.util.Try

class KoutaBackendSwagger extends Swagger(
  Swagger.SpecVersion,
  "0.1-SNAPSHOT",
  ApiInfo("kouta-backend",
    "Uusi tarjonta",
    "https://opintopolku.fi/wp/fi/opintopolku/tietoa-palvelusta/",
    ContactInfo("", "", "verkkotoimitus_opintopolku@oph.fi"),
    LicenseInfo("\"EUPL 1.1 or latest approved by the European Commission\"", "http://www.osor.eu/eupl/")))

object SwaggerYaml {

  var paths: Map[String, List[String]] = Map[String, List[String]]()
  var models: List[String] = List[String]()

  def registerPath(path: String, yaml: String) =
    paths += (path -> (paths.getOrElse(path, List[String]()) ++ List(yaml)))

  def registerModel(model: String) =
    models = models ++ List(model)


}

//case class SwaggerAnntotated(yaml: String) extends scala.annotation.StaticAnnotation {}

class SwaggerServlet(implicit val swagger: Swagger) extends ScalatraServlet with JacksonSwaggerBase {

 get("/swagger.yaml") {
    response.setContentType("text/yaml")
    renderSwagger3Yaml(swagger.docs.toList.asInstanceOf[List[ApiType]])
  }



  protected def renderSwagger3Yaml(docs: List[ApiType]): String = {
    var yaml =
      s"""
         |openapi: 3.0.0\n
         |info:
         |  title: ${swagger.apiInfo.title}
         |  description: ${swagger.apiInfo.description}
         |  version: ${swagger.apiVersion}
         |  termsOfService: ${swagger.apiInfo.termsOfServiceUrl}
         |  contact:
         |    name: ${swagger.apiInfo.contact.name}
         |    email: ${swagger.apiInfo.contact.email}
         |    url: ${swagger.apiInfo.contact.url}
         |  license:
         |    name: ${swagger.apiInfo.license.name}
         |    url: ${swagger.apiInfo.license.url}
         |paths:
         |""".stripMargin

    SwaggerYaml.paths.keySet.map{ path =>
      yaml += s"""  ${path}:
                 |""".stripMargin
      SwaggerYaml.paths.get(path).map {op =>
        yaml += op.mkString
      }
    }
    yaml = yaml +
      s"""
         |components:
         |  schemas:
         |""".stripMargin +
      fi.oph.kouta.domain.models.mkString +
      fi.oph.kouta.domain.keyword.models.mkString +
      fi.oph.kouta.domain.koulutus.models.mkString +
      fi.oph.kouta.domain.koulutusMetadata.models.mkString +
      fi.oph.kouta.domain.toteutus.models.mkString +
      fi.oph.kouta.domain.toteutusMetadata.models.mkString +
      fi.oph.kouta.domain.hakutieto.models.mkString +
      fi.oph.kouta.domain.hakukohde.models.mkString +
      fi.oph.kouta.domain.haku.models.mkString +
      fi.oph.kouta.domain.valintatapa.models.mkString +
      fi.oph.kouta.domain.valintaperusteMetadata.models.mkString +
      fi.oph.kouta.domain.valintaperuste.models.mkString
    yaml
  }
}

trait PrettySwaggerSupport extends SwaggerSupport with KoutaJsonFormats {
  val EXAMPLE_DATE_TIME = ISO_LOCAL_DATE_TIME_FORMATTER.format(LocalDateTime.MIN)
  val EXAMPLE_UUID = UUID.randomUUID().toString

  val modelName: String

  private def modelProperty(position: Int, values: List[String]) = ModelProperty(DataType.String, position, required = false, description = Some(values.mkString("/")))

  def registerSwaggerModels() = {
    /*registerModel[fi.oph.kouta.api.koulutus.KoulutusMetadata]
    registerModel[fi.oph.kouta.api.koulutus.Koulutus]
    registerModel[fi.oph.kouta.api.koulutus.KoulutusNimi]
    registerModel[fi.oph.kouta.api.koulutus.KoulutusKuvaus]
    registerModel[fi.oph.kouta.api.koulutus.KoulutusKuvausNimi]
    registerModel[fi.oph.kouta.api.koulutus.KoulutusListItem]

    registerModel[fi.oph.kouta.api.toteutus.ToteutusMetadata]
    registerModel[fi.oph.kouta.api.toteutus.Toteutus]
    registerModel[fi.oph.kouta.api.toteutus.ToteutusNimi]
    registerModel[fi.oph.kouta.api.toteutus.ToteutusKuvaus]
    registerModel[fi.oph.kouta.api.toteutus.Asiasana]
    registerModel[fi.oph.kouta.api.toteutus.Ammattinimike]
    registerModel[fi.oph.kouta.api.toteutus.ToteutusListItem]
    registerModel[fi.oph.kouta.api.toteutus.KorkeakouluOsaamisala]
    registerModel[fi.oph.kouta.api.toteutus.KorkeakouluOsaamisalaNimi]
    registerModel[fi.oph.kouta.api.toteutus.KorkeakouluOsaamisalaKuvaus]
    registerModel[fi.oph.kouta.api.toteutus.KorkeakouluOsaamisalaLinkki]
    registerModel[fi.oph.kouta.api.toteutus.KorkeakouluOsaamisalaOtsikko]
    registerModel[fi.oph.kouta.api.toteutus.ToteutuksenOpetusMetadata]
    registerModel[fi.oph.kouta.api.toteutus.Yhteyshenkilo]*/
    //registerModel[fi.oph.kouta.api.toteutus]
  }

  def prettifySwaggerModels(model: String = modelName) = {
//    val foo = models(model)
  //  foo
    //models.update(model, models(model).copy(properties = prettifyEnumModels(models(model))))
    //removeRedundantModels()
    //prettifyKielistetty()
    //models.foreach(m => models.update(m._1, m._2.copy(properties = prettifyDatatypes(m._2))))
  }

  /*private def removeRedundantModels() = {
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
    models.remove("HakuOid")
    models.remove("HakukohdeOid")
    models.remove("KoulutusOid")
    models.remove("ToteutusOid")
    models.remove("UserOid")
    models.remove("OrganisaatioOid")
    models.remove("GenericOid")
    models.remove("Oid")
  }

  private def prettifyEnumModels(model: Model) = model.properties.map {
    case ("hakulomaketyyppi", mp) => ("hakulomaketyyppi", modelProperty(mp.position, Hakulomaketyyppi.values().map(_.toString)))
    case ("tila", mp) => ("tila", modelProperty(mp.position, Julkaisutila.values().map(_.toString)))
    //case ("koulutustyyppi", mp) => ("koulutustyyppi", modelProperty(mp.position, Koulutustyyppi.values().map(_.toString)))
    case ("kieli", mp) => ("kieli", modelProperty(mp.position, Kieli.values.map(_.toString)))
    case ("kielivalinta", mp) => ("kielivalinta", ModelProperty(DataType.GenList(DataType.String), mp.position, description = Some(s"[${Kieli.values.mkString(",")}]")))
    case ("liitteidenToimitustapa", mp) => ("liitteidenToimitustapa", modelProperty(mp.position, LiitteenToimitustapa.values().map(_.toString)))
    case ("toimitustapa", mp) => ("toimitustapa", modelProperty(mp.position, LiitteenToimitustapa.values().map(_.toString)))
    case ("id", mp) => ("id", modelProperty(mp.position, List(EXAMPLE_UUID)))
    case ("valintaperusteet", mp) => ("valintaperusteet", ModelProperty(DataType.GenList(DataType.String), mp.position, description = Some(EXAMPLE_UUID)))
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

  private def prettifyDatatypes(model: Model) = model.properties.map {
    case (name, mp) if mp.`type`.name.equals("Instant") => (name, modelProperty(mp.position, List(EXAMPLE_DATE_TIME)))
    case (name, mp) if mp.`type`.name.equals("Map") => (name, ModelProperty(new ValueDataType("Kielistetty", None, Some("fi.oph.kouta.domain.Kielistetty")), mp.position))
    case (name, mp) if mp.`type`.name.equals("UUID") => (name, modelProperty(mp.position, List(EXAMPLE_UUID)))
    case (name, mp) if mp.`type`.name.equals("KoulutusOid") => (name, modelProperty(mp.position, List("1.2.246.562.13.123456")))
    case (name, mp) if mp.`type`.name.equals("ToteutusOid") => (name, modelProperty(mp.position, List("1.2.246.562.17.123456")))
    case (name, mp) if mp.`type`.name.equals("HakuOid") => (name, modelProperty(mp.position, List("1.2.246.562.29.123456")))
    case (name, mp) if mp.`type`.name.equals("HakukohdeOid") => (name, modelProperty(mp.position, List("1.2.246.562.20.123456")))
    case (name, mp) if mp.`type`.name.equals("UserOid") => (name, modelProperty(mp.position, List("1.2.246.123.123.123456")))
    case (name, mp) if mp.`type`.name.equals("OrganisaatioOid") => (name, modelProperty(mp.position, List("1.2.246.123.123.123456")))
    case (name, mp) if mp.`type`.name.equals("Oid") => (name, modelProperty(mp.position, List("1.2.246.123.123.123456")))
    //case (name, mp) if mp.`type`.name.equals("Kieli") => (name, ModelProperty(DataType.GenList(DataType.String), mp.position, description = Some(s"[${Kieli.values.mkString(",")}]")))
    case p => p
  }*/
}