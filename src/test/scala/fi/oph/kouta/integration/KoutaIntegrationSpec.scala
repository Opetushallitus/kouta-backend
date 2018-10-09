package fi.oph.kouta.integration

import fi.oph.kouta.KoutaBackendSwagger
import fi.oph.kouta.TestSetups.{setupWithEmbeddedPostgres, setupWithPort}
import fi.oph.kouta.util.KoutaJsonFormats
import org.scalatest.DoNotDiscover
import org.scalatra.test.scalatest.ScalatraFlatSpec

@DoNotDiscover
class KoutaIntegrationSpec extends ScalatraFlatSpec with KoutaJsonFormats {

  implicit val swagger = new KoutaBackendSwagger

  override def beforeAll() = {
    super.beforeAll()
    Option(System.getProperty("kouta-backend.test-postgres-port")) match {
      case Some(port) => setupWithPort(port.toInt)
      case None => setupWithEmbeddedPostgres
    }
  }
}
