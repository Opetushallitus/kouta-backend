package fi.oph.kouta

import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{ApiInfo, JacksonSwaggerBase, Swagger}

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