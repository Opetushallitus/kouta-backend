package fi.oph.kouta

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import fi.vm.sade.utils.slf4j.Logging
import org.eclipse.jetty.util.resource.Resource

object JettyLauncher extends Logging {
  val DEFAULT_PORT = "8077"

  def main(args: Array[String]) {
    val port = System.getProperty("kouta-backend.port", DEFAULT_PORT).toInt
    logger.info(s"Starting standalone Kouta-backend Jetty on port ${port}...")
    new JettyLauncher(port).start.join
  }
}

class JettyLauncher(val port:Int) {
  val server = new Server(port)
  val context = new WebAppContext()
  context.setBaseResource(Resource.newClassPathResource("webapp"))
  context.setDescriptor("WEB-INF/web.xml")
  context.setContextPath("/kouta-backend")
  server.setHandler(context)

  def start = {
    server.start
    server
  }
}