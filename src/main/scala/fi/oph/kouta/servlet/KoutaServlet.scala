package fi.oph.kouta.servlet

import fi.oph.kouta.domain.{Julkaisutila, Koulutustyyppi}
import org.json4s.{DefaultFormats, Formats}
import org.json4s.ext.EnumNameSerializer
import org.scalatra.ScalatraServlet
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.swagger.SwaggerSupport

trait KoutaServlet extends ScalatraServlet with SwaggerSupport with JacksonJsonSupport {
  override protected implicit def jsonFormats: Formats = DefaultFormats +
    new EnumNameSerializer(Julkaisutila) + new EnumNameSerializer(Koulutustyyppi)
}

