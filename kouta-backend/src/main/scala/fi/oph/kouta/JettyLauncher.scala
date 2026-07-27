package fi.oph.kouta

import ch.qos.logback.access.jetty.RequestLogImpl
import fi.oph.kouta.config.KoutaConfigurationFactory
import fi.oph.kouta.logging.Logging
import fi.vm.sade.properties.OphProperties
import org.eclipse.jetty.ee10.webapp.WebAppContext
import org.eclipse.jetty.http.UriCompliance
import org.eclipse.jetty.server.handler.CrossOriginHandler
import org.eclipse.jetty.server.{HttpConfiguration, HttpConnectionFactory, RequestLog, Server, ServerConnector}

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
  val server = new Server()

  // Kouta-indeksoija enkoodaa lastModified-aikaleiman kahteen kertaan polkusegmenttiin
  // (esim. /indexer/modifiedSince/Thu%252C...), koska IndexerServlet purkaa sen itse vielä
  // kerran URLDecoderilla Scalatran oman purun jälkeen. Jetty 12 tulkitsee tällaisen
  // moniselitteisesti enkoodatun polun oletuksena vaaralliseksi ja palauttaa 400:n
  // ("Ambiguous URI path encoding"), joten sallitaan se eksplisiittisesti.
  val httpConfig = new HttpConfiguration()
  httpConfig.setUriCompliance(UriCompliance.LEGACY)
  val connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig))
  connector.setPort(port)
  server.setConnectors(Array(connector))

  val context = new WebAppContext()
  context.setBaseResource(context.getResourceFactory.newClassLoaderResource("/webapp"))
  context.setContextPath("/kouta-backend")
  // HttpConfiguration.setUriCompliance yllä sallii moniselitteisesti enkoodatun polun jo
  // HTTP-tason parsinnassa, mutta ee10-servlettikerros (ServletHandler) tekee tästä vielä oman,
  // erillisen tarkistuksensa ja heittää HttpException.IllegalArgumentExceptionin heti kun
  // getServletPath()/getPathInfo()-metodeja kutsutaan, ellei tätä myös sallita erikseen.
  context.getServletHandler.setDecodeAmbiguousURIs(true)

  if (enableCors) {
    val handler = new CrossOriginHandler
    handler.setAllowedOriginPatterns(Set("https://localhost:3000").asJava)
    handler.setAllowedMethods(Set("POST", "GET", "OPTIONS", "PUT", "DELETE", "HEAD").asJava)
    handler.setAllowedHeaders(Set("X-PINGOTHER", "Origin", "X-Requested-With", "Content-Type", "Accept").asJava)
    handler.setPreflightMaxAge(Duration.ofSeconds(728000))
    handler.setAllowCredentials(true)
    handler.setHandler(context)
    server.setHandler(handler)
  } else {
    server.setHandler(context)
  }

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
