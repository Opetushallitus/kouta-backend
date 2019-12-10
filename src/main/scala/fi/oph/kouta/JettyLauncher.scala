package fi.oph.kouta

import ch.qos.logback.access.jetty.RequestLogImpl
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.vm.sade.properties.OphProperties
import fi.vm.sade.utils.slf4j.Logging
import org.eclipse.jetty.server.{RequestLog, Server}
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.webapp.WebAppContext

object JettyLauncher extends Logging {
  val DEFAULT_PORT = "8080"

  def main(args: Array[String]) {
    val port = System.getProperty("kouta-backend.port", DEFAULT_PORT).toInt
    logger.info(s"Starting standalone Kouta-backend Jetty on port ${port}...")
    new JettyLauncher(port).start.join
  }
}

class JettyLauncher(val port: Int) {
  val server = new Server(port)
  val context = new WebAppContext()
  context.setBaseResource(Resource.newClassPathResource("webapp"))
  context.setDescriptor("WEB-INF/web.xml")
  context.setContextPath("/kouta-backend")
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
