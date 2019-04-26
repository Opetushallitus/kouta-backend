package fi.oph.kouta.integration

import fi.oph.kouta.TestData
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.fixture.{KoulutusFixture, ToteutusFixture}
import fi.oph.kouta.validation.Validations
import org.json4s.jackson.Serialization.read

class KoulutusSpec extends KoutaIntegrationSpec with KoulutusFixture with ToteutusFixture with Validations {

  "Get koulutus by oid" should "return 404 if koulutus not found" in {
    get(s"$KoulutusPath/123", headers = defaultHeaders) {
      status should equal (404)
      body should include ("Unknown koulutus oid")
    }
  }

  it should "return 401 without a session" in {
    get(s"$KoulutusPath/123", headers = Map.empty) {
      status should equal (401)
    }
  }

  "Create koulutus" should "store koulutus" in {
    val oid = put(koulutus)
    get(oid, koulutus(oid))
  }

  it should "store korkeakoulutus koulutus" in {
    val oid = put(TestData.YoKoulutus)
    get(oid, TestData.YoKoulutus.copy(oid = Some(KoulutusOid(oid))))
  }

  it should "validate new koulutus" in {
    put(KoulutusPath, bytes(koulutus.copy(koulutusKoodiUri = None)), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validateErrorBody(missingMsg("koulutusKoodiUri")))
    }
  }

  it should "return 401 without a session" in {
    put(KoulutusPath, bytes(koulutus), Seq(jsonHeader)) {
      withClue(body) {
        status should equal(401)
      }
    }
  }

  it should "fail if the user doesn't have the right role" in {
    put(KoulutusPath, bytes(koulutus), Seq(jsonHeader, sessionHeader(rolelessUser.sessionId))) {
      withClue(body) {
        status should equal(403)
      }
    }
  }

  "Update koulutus" should "update koulutus" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    update(koulutus(oid, Arkistoitu), lastModified)
    get(oid, koulutus(oid, Arkistoitu))
  }

  it should "not update koulutus" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    update(koulutus(oid), lastModified, false)
    get(oid, koulutus(oid))
  }

  it should "return 401 without a session" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    post(KoulutusPath, bytes(koulutus(oid)), Seq("If-Unmodified-Since" -> lastModified)) {
      status should equal (401)
    }
   }

  it should "fail update if 'If-Unmodified-Since' header is missing" in {
    val oid = put(koulutus)
    post(KoulutusPath, bytes(koulutus(oid)), defaultHeaders) {
      status should equal (400)
      body should equal (errorBody("Otsake If-Unmodified-Since on pakollinen."))
    }
  }

  it should "fail update if modified in between get and update" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    Thread.sleep(1500)
    update(koulutus(oid, Arkistoitu), lastModified)
    post(KoulutusPath, bytes(koulutus(oid)), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "update koulutuksen tekstit ja tarjoajat" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    val uusiKoulutus = koulutus(oid).copy(
      kielivalinta = Seq(Fi, Sv, En),
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      tarjoajat = List("2.2", "3.2", "4.2").map(OrganisaatioOid))
    update(uusiKoulutus, lastModified, true)
    get(oid, uusiKoulutus)
  }

  it should "delete all tarjoajat and read last modified from history" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    Thread.sleep(1500)
    val uusiKoulutus = koulutus(oid).copy(tarjoajat = List())
    update(uusiKoulutus, lastModified, true)
    get(oid, uusiKoulutus) should not equal (lastModified)
  }

  it should "store and update unfinished koulutus" in {
    val unfinishedKoulutus = new Koulutus(johtaaTutkintoon = true, muokkaaja = UserOid("5.4.3.2.1"), organisaatioOid = OrganisaatioOid("1.2"), modified = None)
    val oid = put(unfinishedKoulutus)
    val lastModified = get(oid, unfinishedKoulutus.copy(oid = Some(KoulutusOid(oid))))
    val newUnfinishedKoulutus = unfinishedKoulutus.copy(oid = Some(KoulutusOid(oid)), johtaaTutkintoon = false)
    update(newUnfinishedKoulutus, lastModified)
    get(oid, newUnfinishedKoulutus)
  }

  it should "validate updated koulutus" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    post(KoulutusPath, bytes(koulutus(oid).copy(koulutusKoodiUri = None)), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validateErrorBody(missingMsg("koulutusKoodiUri")))
    }
  }

  "List toteutukset related to koulutus" should "return all toteutukset related to koulutus" in {
    val oid = put(koulutus)
    val t1 = put(toteutus(oid))
    val t2 = put(toteutus(oid))
    val t3 = put(toteutus(oid))
    get(s"$KoulutusPath/$oid/toteutukset", headers = defaultHeaders) {
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
    get(s"$KoulutusPath/$oid/toteutukset", headers = defaultHeaders) {
      status should equal (200)
      read[List[Toteutus]](body) should contain theSameElementsAs(List())
    }
  }

}
