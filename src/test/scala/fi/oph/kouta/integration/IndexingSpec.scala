package fi.oph.kouta.integration

import fi.oph.kouta.{EventuallyMessages, KonfoIndexingQueues}
import fi.oph.kouta.domain.Arkistoitu
import fi.oph.kouta.integration.fixture.HakuFixture
import fi.oph.kouta.servlet.HakuServlet
import fi.oph.kouta.validation.Validations

trait HakuFixtureWithIndexing extends HakuFixture { this: KoutaIntegrationSpec =>
  override def init(): Unit = addServlet(new HakuServlet(), HakuPath)
}

class IndexingSpec extends KoutaIntegrationSpec
  with HakuFixtureWithIndexing with Validations with KonfoIndexingQueues with EventuallyMessages {

  "Create haku" should "send indexing message after creating haku" in {
    val oid = put(haku)
    eventuallyIndexingMessages { _ should contain (s"""{"haut":["$oid"]}""") }
  }

  "Update haku" should "send indexing message after updating haku" in {
    val oid = put(haku)
    eventuallyIndexingMessages { _ should contain (s"""{"haut":["$oid"]}""") }

    update(haku(oid, Arkistoitu), lastModified = get(oid, haku(oid)))

    eventuallyIndexingMessages { _ should contain (s"""{"haut":["$oid"]}""") }
  }
}
