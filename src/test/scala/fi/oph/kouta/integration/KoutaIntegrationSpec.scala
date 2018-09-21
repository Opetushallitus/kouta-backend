package fi.oph.kouta.integration

import fi.oph.kouta.servlet.KoutaServlet
import fi.oph.kouta.KoutaBackendSwagger
import fi.oph.kouta.TestSetups.{setupWithEmbeddedPostgres, setupWithPort}
import org.json4s.Formats
import org.scalatest.DoNotDiscover
import org.scalatra.test.scalatest.ScalatraFlatSpec

@DoNotDiscover
class KoutaIntegrationSpec extends ScalatraFlatSpec {

  implicit val swagger = new KoutaBackendSwagger
  implicit def jsonFormats: Formats = KoutaServlet.koutaFormats

  override def beforeAll() = {
    super.beforeAll()
    Option(System.getProperty("kouta-backend.test-postgres-port")) match {
      case Some(port) => setupWithPort(port.toInt)
      case None => setupWithEmbeddedPostgres
    }
  }
}
