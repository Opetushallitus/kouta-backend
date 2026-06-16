package fi.oph.kouta

import ch.qos.logback.access.jetty.RequestLogImpl
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.logging.Logging
import fi.vm.sade.properties.OphProperties
import org.eclipse.jetty.ee10.webapp.WebAppContext
import org.eclipse.jetty.server.handler.CrossOriginHandler
import org.eclipse.jetty.server.{RequestLog, Server}

import java.time.Duration
import scala.jdk.CollectionConverters.setAsJavaSetConverter

object JettyLauncher extends Logging {
  val DEFAULT_PORT = "8080"

  def main(args: Array[String]) {
    val port = System.getProperty("kouta-backend.port", DEFAULT_PORT).toInt
    logger.info(s"Starting standalone Kouta-backend Jetty on port ${port}...")
    new JettyLauncher(port).start.join
  }
}

class JettyLauncher(val port: Int, val enableCors: Boolean = false) {
  val server = new Server(port)
  val context = new WebAppContext()
  context.setBaseResource(context.getResourceFactory.newClassLoaderResource("/webapp"))
  context.setContextPath("/kouta-backend")

  if (enableCors) {
    val handler = new CrossOriginHandler
    handler.setAllowedOriginPatterns(Set("https://localhost:3000").asJava)
    handler.setAllowedMethods(Set("POST", "GET", "OPTIONS", "PUT", "DELETE", "HEAD").asJava)
    handler.setAllowedHeaders(Set("X-PINGOTHER", "Origin", "X-Requested-With", "Content-Type", "Accept").asJava)
    handler.setPreflightMaxAge(Duration.ofSeconds(728000))
    handler.setAllowCredentials(true)
    server.setHandler(handler)
  }

  server.setHandler(context)

  server.setRequestLog(requestLog(KoutaConfigurationFactory.configuration.urlProperties))

  def start: Server = {
    println("JettyLauncher: starting server at http://localhost:" + port)
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
