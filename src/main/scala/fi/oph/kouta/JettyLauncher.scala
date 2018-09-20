package fi.oph.kouta

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import fi.vm.sade.utils.slf4j.Logging

object JettyLauncher extends Logging {
  val DEFAULT_PORT = "8080"

  def main(args: Array[String]) {
    val port = System.getProperty("kouta-backend.port", DEFAULT_PORT).toInt
    logger.info(s"Starting standalone Kouta-backend Jetty on port ${port}...")
    new JettyLauncher(port).start.join
  }
}

class JettyLauncher(val port:Int) {
  val server = new Server(port)
  val context = new WebAppContext()

  context.setResourceBase("src/main/webapp")
  context.setContextPath("/kouta-backend")
  context.setDescriptor("src/main/webapp/WEB-INF/web.xml")
  server.setHandler(context)

  def start = {
    server.start
    server
  }
}