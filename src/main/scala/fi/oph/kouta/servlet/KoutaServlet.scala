package fi.oph.kouta.servlet

import org.json4s.{DefaultFormats, Formats}
import org.scalatra.ScalatraServlet
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.swagger.SwaggerSupport

trait KoutaServlet extends ScalatraServlet with SwaggerSupport with JacksonJsonSupport {
  override protected implicit def jsonFormats: Formats = DefaultFormats
}

