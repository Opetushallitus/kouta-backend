package fi.oph.kouta.integration

import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain.oid.{HakuOid, KoulutusOid, OrganisaatioOid, ToteutusOid}
import fi.oph.kouta.domain._
import fi.oph.kouta.security.RoleEntity
import org.json4s.jackson.Serialization.read

class IndexerSpec extends KoutaIntegrationSpec with IndexerFixture {

  override val roleEntities: Seq[RoleEntity] = RoleEntity.all

  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  "List hakukohteet by järjestyspaikka oids" should "List hakukohteet by järjestyspaikka oids" in {
    val oppilaitosOid = put(oppilaitos, ophSession)
    val jarjestyspaikkaOid = put(oppilaitoksenOsa.copy(oppilaitosOid = OrganisaatioOid(oppilaitosOid)), ophSession)

    val koulutusOid = put(koulutus, ophSession)
    val toteutusOid = put(toteutus.copy(koulutusOid = KoulutusOid(koulutusOid)), ophSession)
    val hakuOid = put(haku, ophSession)
    val hakukohdeOid = put(hakukohdeWoValintaperusteenValintakokeet.copy(toteutusOid = ToteutusOid(toteutusOid), hakuOid = HakuOid(hakuOid),
      jarjestyspaikkaOid = Some(OrganisaatioOid(jarjestyspaikkaOid))), ophSession)
    put(hakukohdeWoValintaperusteenValintakokeet.copy(toteutusOid = ToteutusOid(toteutusOid), hakuOid = HakuOid(hakuOid),
      jarjestyspaikkaOid = Some(OrganisaatioOid(jarjestyspaikkaOid)), tila = Poistettu), ophSession)

    post(s"$IndexerPath/list-hakukohde-oids-by-jarjestyspaikat", body=bytes(Seq(jarjestyspaikkaOid)), headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[String]](body) should contain theSameElementsAs(List(hakukohdeOid))
    }
  }

  "List toteutukset related to koulutus" should "return all toteutukset related to koulutus" in {
    val oid = put(koulutus, ophSession)
    val t1 = put(toteutus(oid))
    val t2 = put(toteutus(oid))
    val t3 = put(toteutus(oid))
    val t4 = put(toteutus(oid).copy(tila = Poistettu))
    get(s"$IndexerPath/koulutus/$oid/toteutukset", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[Toteutus]](body) should contain theSameElementsAs List(
        toteutus(t1, oid).copy(modified = Some(readToteutusModified(t1)), _enrichedData = Some(ToteutusEnrichedData(esitysnimi = toteutus(oid).nimi, muokkaajanNimi = None))),
        toteutus(t2, oid).copy(modified = Some(readToteutusModified(t2)), _enrichedData = Some(ToteutusEnrichedData(esitysnimi = toteutus(oid).nimi, muokkaajanNimi = None))),
        toteutus(t3, oid).copy(modified = Some(readToteutusModified(t3)), _enrichedData = Some(ToteutusEnrichedData(esitysnimi = toteutus(oid).nimi, muokkaajanNimi = None)))
      )
    }
  }

  "List only julkaistut toteutukset related to koulutus" should "return julkaistut toteutukset related to koulutus" in {
    val oid = put(koulutus, ophSession)
    val t1 = put(toteutus(oid))
    val t2 = put(toteutus(oid))
    val t3 = put(toteutus(oid))
    val t4 = put(toteutus(oid).copy(tila = Poistettu))
    get(s"$IndexerPath/koulutus/$oid/toteutukset?vainJulkaistut=true", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[Toteutus]](body) should contain theSameElementsAs List(
        toteutus(t1, oid).copy(modified = Some(readToteutusModified(t1)), _enrichedData = Some(ToteutusEnrichedData(esitysnimi = toteutus(oid).nimi, muokkaajanNimi = None))),
        toteutus(t2, oid).copy(modified = Some(readToteutusModified(t2)), _enrichedData = Some(ToteutusEnrichedData(esitysnimi = toteutus(oid).nimi, muokkaajanNimi = None))),
        toteutus(t3, oid).copy(modified = Some(readToteutusModified(t3)), _enrichedData = Some(ToteutusEnrichedData(esitysnimi = toteutus(oid).nimi, muokkaajanNimi = None)))
      )
    }
  }

  "List also poistetut toteutukset related to koulutus" should "return all toteutukset related to koulutus" in {
    val oid = put(koulutus, ophSession)
    val t1 = put(toteutus(oid))
    val t2 = put(toteutus(oid))
    val t3 = put(toteutus(oid))
    val t4 = put(toteutus(oid).copy(tila = Poistettu))
    get(s"$IndexerPath/koulutus/$oid/toteutukset?vainOlemassaolevat=false", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[Toteutus]](body) should contain theSameElementsAs List(
        toteutus(t1, oid).copy(modified = Some(readToteutusModified(t1)), _enrichedData = Some(ToteutusEnrichedData(esitysnimi = toteutus(oid).nimi, muokkaajanNimi = None))),
        toteutus(t2, oid).copy(modified = Some(readToteutusModified(t2)), _enrichedData = Some(ToteutusEnrichedData(esitysnimi = toteutus(oid).nimi, muokkaajanNimi = None))),
        toteutus(t3, oid).copy(modified = Some(readToteutusModified(t3)), _enrichedData = Some(ToteutusEnrichedData(esitysnimi = toteutus(oid).nimi, muokkaajanNimi = None))),
        toteutus(t4, oid).copy(tila = Poistettu, modified = Some(readToteutusModified(t4)), _enrichedData = Some(ToteutusEnrichedData(esitysnimi = toteutus(oid).nimi, muokkaajanNimi = None))),
      )
    }
  }

  it should "return empty result if koulutus has no toteutukset" in {
    val oid = put(koulutus, ophSession)
    get(s"$IndexerPath/koulutus/$oid/toteutukset", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[Toteutus]](body) should contain theSameElementsAs List()
    }
  }

  it should "deny access without a valid session" in {
    val oid = KoulutusOid("oid")
    get(s"$IndexerPath/koulutus/$oid/toteutukset", headers = Seq()) {
      withClue(body) {
        status should equal (401)
      }
    }
  }

  it should "deny access without the indexer role" in {
    val oid = KoulutusOid("oid")
    get(s"$IndexerPath/koulutus/$oid/toteutukset", defaultSessionId, 403)
  }

  it should "deny access without root organization access to the indexer role" in {
    val oid = KoulutusOid("oid")
    get(s"$IndexerPath/koulutus/$oid/toteutukset", fakeIndexerSession, 403)
  }

  "List koulutukset by tarjoaja" should "list all koulutukset distinct" in {
    val oid = put(koulutus.copy(tarjoajat = List(ChildOid, GrandChildOid)), ophSession)
    get(s"$IndexerPath/tarjoaja/$ChildOid/koulutukset", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[Koulutus]](body).count(_.oid.get.s == oid) should equal (1)
    }
  }

  "Get oppilaitoksen osat" should "return oppilaitoksen osat for indexer" in {
    val oid = put(oppilaitos)
    val anotherOid = put(oppilaitos)
    val expectedOsat = Seq(put(oppilaitoksenOsa(oid)), put(oppilaitoksenOsa(oid)), put(oppilaitoksenOsa(oid)))
    val unexpected = put(oppilaitoksenOsa(anotherOid))
    get(s"$IndexerPath/oppilaitos/$oid/osat", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[OppilaitoksenOsa]](body) should contain theSameElementsAs List(
        oppilaitoksenOsa(expectedOsat(0), oid).copy(modified = Some(readOppilaitoksenOsaModified(expectedOsat(0))), _enrichedData = None),
        oppilaitoksenOsa(expectedOsat(1), oid).copy(modified = Some(readOppilaitoksenOsaModified(expectedOsat(1))), _enrichedData = None),
        oppilaitoksenOsa(expectedOsat(2), oid).copy(modified = Some(readOppilaitoksenOsaModified(expectedOsat(2))), _enrichedData = None)
      )
    }
  }

  it should "return empty result if oppilaitos has no osat" in {
    val oid = put(oppilaitos)
    get(s"$IndexerPath/oppilaitos/$oid/osat", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[OppilaitoksenOsa]](body) should contain theSameElementsAs List()
    }
  }

  it should "deny access without a valid session" in {
    val oid = ChildOid
    get(s"$IndexerPath/oppilaitos/$oid/osat", headers = Seq()) {
      withClue(body) {
        status should equal (401)
      }
    }
  }

  it should "deny access without the indexer role" in {
    val oid = ChildOid
    get(s"$IndexerPath/oppilaitos/$oid/osat", defaultSessionId, 403)
  }

  it should "deny access without root organization access to the indexer role" in {
    val oid = ChildOid
    get(s"$IndexerPath/oppilaitos/$oid/osat", fakeIndexerSession, 403)
  }

  "List koulutukset by sorakuvausId" should "list all koulutukset distinct" in {
    val sorakuvausId = put(sorakuvaus, ophSession)
    val koulutusOid = put(koulutus.copy(tila = Tallennettu, sorakuvausId = Some(sorakuvausId)), ophSession)
    val koulutusOid2 = put(koulutus.copy(tila = Tallennettu, sorakuvausId = Some(sorakuvausId)), ophSession)
    val koulutusOid3 = put(koulutus.copy(tila = Poistettu, sorakuvausId = Some(sorakuvausId)), ophSession)

    get(s"$IndexerPath/sorakuvaus/$sorakuvausId/koulutukset/list", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[String]](body) should contain theSameElementsAs(List(koulutusOid, koulutusOid2))
    }
  }

  "List koulutukset by sorakuvausId" should "list also poistetut koulutukset if instructed" in {
    val sorakuvausId = put(sorakuvaus, ophSession)
    val koulutusOid = put(koulutus.copy(tila = Tallennettu, sorakuvausId = Some(sorakuvausId)), ophSession)
    val koulutusOid2 = put(koulutus.copy(tila = Tallennettu, sorakuvausId = Some(sorakuvausId)), ophSession)
    val koulutusOid3 = put(koulutus.copy(tila = Poistettu, sorakuvausId = Some(sorakuvausId)), ophSession)

    get(s"$IndexerPath/sorakuvaus/$sorakuvausId/koulutukset/list?vainOlemassaolevat=false", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[String]](body) should contain theSameElementsAs(List(koulutusOid, koulutusOid2, koulutusOid3))
    }
  }

  it should "return empty list if no koulutukset related to sorakuvaus" in {
    val sorakuvausId = put(sorakuvaus, ophSession)

    get(s"$IndexerPath/sorakuvaus/$sorakuvausId/koulutukset/list", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[String]](body) should contain theSameElementsAs(List())
    }
  }

  it should "deny access without a valid session" in {
    val sorakuvausId = put(sorakuvaus, ophSession)
    get(s"$IndexerPath/sorakuvaus/$sorakuvausId/koulutukset/list", headers = Seq()) {
      withClue(body) {
        status should equal (401)
      }
    }
  }

  it should "deny access without the indexer role" in {
    val sorakuvausId = put(sorakuvaus, ophSession)
    get(s"$IndexerPath/sorakuvaus/$sorakuvausId/koulutukset/list", defaultSessionId, 403)
  }

  it should "deny access without root organization access to the indexer role" in {
    val sorakuvausId = put(sorakuvaus, ophSession)
    get(s"$IndexerPath/sorakuvaus/$sorakuvausId/koulutukset/list", fakeIndexerSession, 403)
  }
}
