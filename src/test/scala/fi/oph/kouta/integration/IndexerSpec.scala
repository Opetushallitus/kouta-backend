package fi.oph.kouta.integration

import fi.oph.kouta.domain.oid.{KoulutusOid, OrganisaatioOid}
import fi.oph.kouta.domain.{Koulutus, OppilaitoksenOsa, Toteutus}
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.validation.Validations
import org.json4s.jackson.Serialization.read

class IndexerSpec extends KoutaIntegrationSpec with EverythingFixture with IndexerFixture with AccessControlSpec with Validations {

  override val roleEntities = RoleEntity.all

  "List toteutukset related to koulutus" should "return all toteutukset related to koulutus" in {
    val oid = put(koulutus)
    val t1 = put(toteutus(oid))
    val t2 = put(toteutus(oid))
    val t3 = put(toteutus(oid))
    get(s"$IndexerPath/koulutus/$oid/toteutukset", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[Toteutus]](body) should contain theSameElementsAs(List(
        toteutus(t1, oid).copy(modified = Some(readModifiedByOid(t1, "toteutukset"))),
        toteutus(t2, oid).copy(modified = Some(readModifiedByOid(t2, "toteutukset"))),
        toteutus(t3, oid).copy(modified = Some(readModifiedByOid(t3, "toteutukset")))
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
    get(s"$IndexerPath/tarjoaja/$ChildOid/koulutukset", headers = Seq(sessionHeader(indexerSession))) {
      status should equal (200)
      read[List[Koulutus]](body).filter(_.oid.get.s == oid).size should equal (1)
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
        oppilaitoksenOsa(expectedOsat(0), oid).copy(modified = Some(readModifiedByOid(expectedOsat(0), "oppilaitosten_osat"))),
        oppilaitoksenOsa(expectedOsat(1), oid).copy(modified = Some(readModifiedByOid(expectedOsat(1), "oppilaitosten_osat"))),
        oppilaitoksenOsa(expectedOsat(2), oid).copy(modified = Some(readModifiedByOid(expectedOsat(2), "oppilaitosten_osat")))
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
    val oid = OrganisaatioOid("oid")
    get(s"$IndexerPath/oppilaitos/$oid/osat", headers = Seq()) {
      withClue(body) {
        status should equal (401)
      }
    }
  }

  it should "deny access without the indexer role" in {
    val oid = OrganisaatioOid("oid")
    get(s"$IndexerPath/oppilaitos/$oid/osat", defaultSessionId, 403)
  }

  it should "deny access without root organization access to the indexer role" in {
    val oid = OrganisaatioOid("oid")
    get(s"$IndexerPath/oppilaitos/$oid/osat", fakeIndexerSession, 403)
  }
}