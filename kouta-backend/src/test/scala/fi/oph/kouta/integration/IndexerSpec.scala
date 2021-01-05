package fi.oph.kouta.integration

import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain.oid.KoulutusOid
import fi.oph.kouta.domain.{Hakutieto, Koulutus, OppilaitoksenOsa, Toteutus}
import fi.oph.kouta.security.RoleEntity
import org.json4s.jackson.Serialization.read

class IndexerSpec extends KoutaIntegrationSpec with EverythingFixture with IndexerFixture with AccessControlSpec {

  override val roleEntities = RoleEntity.all

  "List toteutukset related to koulutus" should "return all toteutukset related to koulutus" in {
    val oid = put(koulutus)
    val t1 = put(toteutus(oid))
    val t2 = put(toteutus(oid))
    val t3 = put(toteutus(oid))
    get(s"$IndexerPath/koulutus/$oid/toteutukset", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[Toteutus]](body) should contain theSameElementsAs(List(
        toteutus(t1, oid).copy(modified = Some(readToteutusModified(t1))),
        toteutus(t2, oid).copy(modified = Some(readToteutusModified(t2))),
        toteutus(t3, oid).copy(modified = Some(readToteutusModified(t3)))
      ))
    }
  }

  it should "return empty result if koulutus has no toteutukset" in {
    val oid = put(koulutus)
    get(s"$IndexerPath/koulutus/$oid/toteutukset", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[Toteutus]](body) should contain theSameElementsAs(List())
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
    val oid = put(koulutus.copy(tarjoajat = List(ChildOid, GrandChildOid)))
    mockOrganisaatioResponse()
    get(s"$IndexerPath/tarjoaja/$ChildOid/koulutukset", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[Koulutus]](body).filter(_.oid.get.s == oid).size should equal (1)
    }
  }

  "List hakutiedot by koulutus" should "list all hakutiedot distinct" in {
    val koulutusOid = put(koulutus)
    val totetusOid = put(toteutus(koulutusOid))
    val hakuOid = put(haku)
    val hkOid1 = put(julkaistuHakukohde(totetusOid, hakuOid))
    val hkOid2 = put(julkaistuHakukohde(totetusOid, hakuOid))
    val hkOid3 = put(julkaistuHakukohde(totetusOid, hakuOid))

    get(s"$IndexerPath/koulutus/$koulutusOid/hakutiedot", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      val result = read[List[Hakutieto]](body)
      result.size should equal (1)
      result.head.haut.size should equal (1)
      result.head.haut.head.hakukohteet.map(_.hakukohdeOid.toString) should contain theSameElementsAs List(hkOid1, hkOid2, hkOid3)
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
        oppilaitoksenOsa(expectedOsat(0), oid).copy(modified = Some(readOppilaitoksenOsaModified(expectedOsat(0)))),
        oppilaitoksenOsa(expectedOsat(1), oid).copy(modified = Some(readOppilaitoksenOsaModified(expectedOsat(1)))),
        oppilaitoksenOsa(expectedOsat(2), oid).copy(modified = Some(readOppilaitoksenOsaModified(expectedOsat(2))))
      )
    }
  }

  it should "return empty result if oppilaitos has no osat" in {
    val oid = put(oppilaitos)
    get(s"$IndexerPath/oppilaitos/$oid/osat", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[OppilaitoksenOsa]](body) should contain theSameElementsAs(List())
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
}
