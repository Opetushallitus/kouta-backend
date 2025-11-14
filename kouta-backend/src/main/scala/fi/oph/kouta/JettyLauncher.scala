package fi.oph.kouta

import ch.qos.logback.access.jetty.RequestLogImpl
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.properties.OphProperties
import fi.oph.kouta.logging.Logging
import org.eclipse.jetty.server.{RequestLog, Server}
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlets.CrossOriginFilter

import javax.servlet.DispatcherType

object JettyLauncher extends Logging {
  val DEFAULT_PORT = "8080"

  def main(args: Array[String]): Unit = {
    val port = System.getProperty("kouta-backend.port", DEFAULT_PORT).toInt
    logger.info(s"Starting standalone Kouta-backend Jetty on port ${port}...")
    new JettyLauncher(port).start.join
  }
}

class JettyLauncher(val port: Int, val enableCors: Boolean = false) {
  val server = new Server(port)
  val context = new WebAppContext()
  context.setBaseResource(Resource.newClassPathResource("webapp"))
  context.setDescriptor("WEB-INF/web.xml")
  context.setContextPath("/kouta-backend")

  if (enableCors) {
    val filter = new FilterHolder
    filter.setInitParameter("allowedOrigins", "https://localhost:3000")
    filter.setInitParameter("allowedMethods", "POST,GET,OPTIONS,PUT,DELETE,HEAD")
    filter.setInitParameter("allowedHeaders", "X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept")
    filter.setInitParameter("preflightMaxAge", "728000")
    filter.setInitParameter("allowCredentials", "true")
    filter.setFilter(new CrossOriginFilter)

    context.addFilter(filter, "/*", java.util.EnumSet.of[DispatcherType](DispatcherType.REQUEST))
  }

  server.setHandler(context)

  server.setRequestLog(requestLog(KoutaConfigurationFactory.configuration.urlProperties))

  def start = {
    server.start
    server
  }

  private def requestLog(properties: OphProperties): RequestLog = {
    val requestLog = new RequestLogImpl
    val logbackAccess = properties.getOrElse("logback.access", null)
    if (logbackAccess != null) {
      requestLog.setFileName(logbackAccess)
    } else {
      println("JettyLauncher: Jetty access log is printed to console, use -Dlogback.access to set configuration file")
      requestLog.setResource("/logback-access.xml")
    }
    requestLog.start()
    requestLog
  }
}
