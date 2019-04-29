package fi.oph.kouta.integration

import fi.oph.kouta.domain.Arkistoitu
import fi.oph.kouta.integration.fixture.EverythingFixtureWithIndexing
import fi.oph.kouta.validation.Validations
import fi.oph.kouta.{EventuallyMessages, KonfoIndexingQueues}

class IndexingSpec extends KoutaIntegrationSpec
  with EverythingFixtureWithIndexing with Validations with KonfoIndexingQueues with EventuallyMessages {

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

  "Create koulutus" should "send indexing message after creating koulutus" in {
    val oid = put(koulutus)
    eventuallyIndexingMessages { _ should contain (s"""{"koulutukset":["$oid"]}""") }
  }

  "Update koulutus" should "send indexing message after updating koulutus" in {
    val oid = put(koulutus)
    eventuallyIndexingMessages { _ should contain (s"""{"koulutukset":["$oid"]}""") }

    update(koulutus(oid, Arkistoitu), lastModified = get(oid, koulutus(oid)))

    eventuallyIndexingMessages { _ should contain (s"""{"koulutukset":["$oid"]}""") }
  }

  "Create toteutus" should "send indexing message after creating toteutus" in {
    val koulutusOid = put(koulutus)
    eventuallyIndexingMessages { _ should contain (s"""{"koulutukset":["$koulutusOid"]}""") }

    val oid = put(toteutus(koulutusOid))
    eventuallyIndexingMessages { _ should contain (s"""{"toteutukset":["$oid"]}""") }
  }

  "Update toteutus" should "send indexing message after updating toteutus" in {
    val koulutusOid = put(koulutus)
    eventuallyIndexingMessages { _ should contain (s"""{"koulutukset":["$koulutusOid"]}""") }

    val oid = put(toteutus(koulutusOid))
    eventuallyIndexingMessages { _ should contain (s"""{"toteutukset":["$oid"]}""") }

    update(toteutus(oid, koulutusOid, Arkistoitu), lastModified = get(oid, toteutus(oid, koulutusOid)))
    eventuallyIndexingMessages { _ should contain (s"""{"toteutukset":["$oid"]}""") }
  }
}
