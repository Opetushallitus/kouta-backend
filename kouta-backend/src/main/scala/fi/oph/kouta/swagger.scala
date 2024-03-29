package fi.oph.kouta

import fi.oph.kouta.servlet.KoutaServlet
import org.scalatra.ScalatraServlet

object SwaggerPaths {

  var paths: Map[String, List[String]] = Map[String, List[String]]()

  def registerPath(path: String, yaml: String) =
    paths += (path -> (paths.getOrElse(path, List[String]()) ++ List(yaml)).distinct)
}

class SwaggerServlet extends ScalatraServlet {

  get("/swagger.yaml") {
    response.setContentType("text/yaml")
    renderOpenapi3Yaml()
  }

  protected def renderOpenapi3Yaml(): String = {
    var yaml =
      """
        |openapi: 3.0.3
        |info:
        |  title: kouta-backend
        |  description: "Uusi koulutustarjonta"
        |  version: 0.1-SNAPSHOT
        |  termsOfService: https://opintopolku.fi/wp/fi/opintopolku/tietoa-palvelusta/
        |  contact:
        |    name: "Opetushallitus"
        |    email: verkkotoimitus_opintopolku@oph.fi
        |    url: "https://www.oph.fi/"
        |  license:
        |    name: "EUPL 1.1 or latest approved by the European Commission"
        |    url: "http://www.osor.eu/eupl/"
        |servers:
        |  - url: /kouta-backend/
        |paths:
        |""".stripMargin

    SwaggerPaths.paths.keySet.map{ path =>
      yaml += s"""  ${path}:
                 |""".stripMargin
      SwaggerPaths.paths.get(path).map { op =>
        yaml += op.mkString
      }
    }
    yaml +=
      s"""
         |components:
         |  parameters:
         |    xIfUnmodifiedSince:
         |      in: header
         |      name: ${KoutaServlet.IfUnmodifiedSinceHeader}
         |      schema:
         |        type: string
         |        default: ${KoutaServlet.SampleHttpDate}
         |      required: true
         |      description: Vastaavan GETin ${KoutaServlet.LastModifiedHeader}
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
        fi.oph.kouta.domain.sorakuvaus.models.mkString +
        fi.oph.kouta.domain.oppilaitos.models.mkString +
        fi.oph.kouta.domain.organisaatio.models.mkString +
        fi.oph.kouta.domain.searchResults.models.mkString +
        fi.oph.kouta.domain.koulutustyyppiToOppilaitostyyppiResult.models.mkString
    yaml
  }
}
