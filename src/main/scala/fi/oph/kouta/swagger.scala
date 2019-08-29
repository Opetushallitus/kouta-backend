package fi.oph.kouta

import org.scalatra.ScalatraServlet

object SwaggerPaths {

  var paths: Map[String, List[String]] = Map[String, List[String]]()

  def registerPath(path: String, yaml: String) =
    paths += (path -> (paths.getOrElse(path, List[String]()) ++ List(yaml)))

}

class SwaggerServlet extends ScalatraServlet {

  get("/swagger.yaml") {
    response.setContentType("text/yaml")
    renderSwagger3Yaml()
  }

  protected def renderSwagger3Yaml(): String = {
    var yaml =
      s"""
         |openapi: 3.0.0\n
         |info:
         |  title: kouta-backend
         |  description: "Uusi koulutustarjonta"
         |  version: 0.1-SNAPSHOT
         |  termsOfService: https://opintopolku.fi/wp/fi/opintopolku/tietoa-palvelusta/
         |  contact:
         |    name: ""
         |    email: verkkotoimitus_opintopolku@oph.fi
         |    url: ""
         |  license:
         |    name: "EUPL 1.1 or latest approved by the European Commission"
         |    url: "http://www.osor.eu/eupl/"
         |servers:
         |  - url: https://virkailija.testiopintopolku.fi/kouta-backend/
         |paths:
         |""".stripMargin

    SwaggerPaths.paths.keySet.map{ path =>
      yaml += s"""  ${path}:
                 |""".stripMargin
      SwaggerPaths.paths.get(path).map { op =>
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
      fi.oph.kouta.domain.valintaperuste.models.mkString +
      fi.oph.kouta.domain.sorakuvaus.models.mkString
    yaml
  }
}

/*trait PrettySwaggerSupport extends SwaggerSupport with KoutaJsonFormats {
  val EXAMPLE_DATE_TIME = ISO_LOCAL_DATE_TIME_FORMATTER.format(LocalDateTime.MIN)
  val EXAMPLE_UUID = UUID.randomUUID().toString

}*/