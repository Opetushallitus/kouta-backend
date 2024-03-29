package fi.oph.kouta.integration

import fi.oph.kouta.domain._
import fi.oph.kouta.integration.fixture.IndexingFixture
import fi.oph.kouta.{EventuallyMessages, KonfoIndexingQueues, TestData}

import java.util.UUID

class IndexingSpec extends KoutaIntegrationSpec
  with IndexingFixture with KonfoIndexingQueues with EventuallyMessages with AccessControlSpec {

  var (koulutusOid, toteutusOid, hakuOid) = ("", "", "")
  var valintaperusteId: UUID = _
  var sorakuvausId: UUID = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    koulutusOid = put(koulutus, ophSession)
    toteutusOid = put(toteutus(koulutusOid))
    hakuOid = put(haku)
    sorakuvausId = put(sorakuvaus)
    valintaperusteId = put(valintaperuste)
  }

  lazy val uusiHakukohde: Hakukohde = hakukohde(toteutusOid, hakuOid, valintaperusteId)
  lazy val tallennettuHakukohde: String => Hakukohde = {oid:String => getIds(hakukohde(oid, toteutusOid, hakuOid, valintaperusteId))}

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
    val oid = put(koulutus, ophSession)
    eventuallyIndexingMessages { _ should contain (s"""{"koulutukset":["$oid"]}""") }
  }

  "Update koulutus" should "send indexing message after updating koulutus" in {
    val oid = put(koulutus, ophSession)
    eventuallyIndexingMessages { _ should contain (s"""{"koulutukset":["$oid"]}""") }

    update(koulutus(oid, Arkistoitu), lastModified = get(oid, koulutus(oid)), ophSession, 200)

    eventuallyIndexingMessages { _ should contain (s"""{"koulutukset":["$oid"]}""") }
  }

  "Create toteutus" should "send indexing message after creating toteutus" in {
    val oid = put(toteutus(koulutusOid))
    eventuallyIndexingMessages { _ should contain (s"""{"toteutukset":["$oid"]}""") }
  }

  "Update toteutus" should "send indexing message after updating toteutus" in {
    val oid = put(toteutus(koulutusOid))
    eventuallyIndexingMessages { _ should contain (s"""{"toteutukset":["$oid"]}""") }

    update(toteutus(oid, koulutusOid, Arkistoitu), lastModified = get(oid, toteutus(oid, koulutusOid)))
    eventuallyIndexingMessages { _ should contain (s"""{"toteutukset":["$oid"]}""") }
  }

  "Create valintaperuste" should "send indexing message after creating valintaperuste" in {
    val id = put(valintaperuste)
    eventuallyIndexingMessages { _ should contain (s"""{"valintaperusteet":["$id"]}""") }
  }

  "Update valintaperuste"  should "send indexing message after updating valintaperuste" in {
    val id = put(valintaperuste)
    eventuallyIndexingMessages { _ should contain (s"""{"valintaperusteet":["$id"]}""") }

    update(valintaperuste(id, Arkistoitu), lastModified = get(id, valintaperuste(id)))

    eventuallyIndexingMessages { _ should contain (s"""{"valintaperusteet":["$id"]}""") }
  }

  "Create hakukohde" should "send indexing message after creating hakukohde" in {
    val oid = put(withValintaperusteenValintakokeet(uusiHakukohde))
    eventuallyIndexingMessages {
      _ should contain(s"""{"hakukohteet":["$oid"]}""")
    }
  }

  "Update hakukohde" should "send indexing message after updating hakukohde" in {
    val oid = put(withValintaperusteenValintakokeet(uusiHakukohde))
    eventuallyIndexingMessages {
      _ should contain(s"""{"hakukohteet":["$oid"]}""")
    }

    val lastModified = get(oid, withValintaperusteenValintakokeet(tallennettuHakukohde(oid)))
    val muokattuHakukohde = withValintaperusteenValintakokeet(tallennettuHakukohde(oid)).copy(
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv"),
      hakulomaketyyppi = Some(Ataru),
      hakulomakeAtaruId = Some(UUID.randomUUID()),
      hakulomakeKuvaus = Map(),
      hakuajat = List(Ajanjakso(alkaa = TestData.now(), paattyy = Some(TestData.inFuture(12000)))))
    update(muokattuHakukohde, lastModified, expectUpdate = true)

    eventuallyIndexingMessages {
      _ should contain(s"""{"hakukohteet":["$oid"]}""")
    }
  }
}
