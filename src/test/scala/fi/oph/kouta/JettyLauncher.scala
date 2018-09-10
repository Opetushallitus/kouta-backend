package fi.oph.kouta


import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext

object JettyLauncher {

  private val DEFAULT_PORT = "8099"

  def main(args: Array[String]) {
    TempDb.start()
    System.setProperty("kouta-backend.db.url", s"jdbc:postgresql://localhost:${TempDb.port}/kouta")
    System.setProperty("kouta-backend.db.user", "oph")
    System.setProperty("kouta-backend.db.password", "")
    //System.setProperty("valintatulos.it.postgres.port", PortChecker.findFreeLocalPort.toString)
    new JettyLauncher(System.getProperty("kouta-backend.port",JettyLauncher.DEFAULT_PORT).toInt).start.join
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
