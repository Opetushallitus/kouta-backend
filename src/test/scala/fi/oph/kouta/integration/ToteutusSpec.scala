package fi.oph.kouta.integration

import fi.oph.kouta.TestData
import fi.oph.kouta.{EventuallyMessages, KonfoIndexingQueues}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.fixture.{KeywordFixture, KoulutusFixture, ToteutusFixture}
import fi.oph.kouta.validation.Validations

class ToteutusSpec extends KoutaIntegrationSpec
  with KoulutusFixture with ToteutusFixture with KeywordFixture with Validations with KonfoIndexingQueues with EventuallyMessages {

  var koulutusOid = ""

  override def beforeAll() = {
    super.beforeAll()
    koulutusOid = put(koulutus)
  }

  "GET /toteutus/:oid" should "return 404 if toteutus not found" in {
    get(s"$ToteutusPath/123", headers = defaultHeaders) {
      status should equal (404)
      body should include ("Unknown toteutus oid")
    }
  }

  it should "return 401 if no session is found" in {
    get(s"$ToteutusPath/123") {
      status should equal (401)
    }
  }

  "PUT /toteutus" should "store toteutus" in {
    val oid = put(toteutus(koulutusOid))
    get(oid, toteutus(oid, koulutusOid))
  }

  it should "store korkeakoulutus toteutus" in {
    val oid = put(TestData.JulkaistuYoToteutus.copy(koulutusOid = KoulutusOid(koulutusOid)))
    get(oid, TestData.JulkaistuYoToteutus.copy(oid = Some(ToteutusOid(oid)), koulutusOid = KoulutusOid(koulutusOid)))
  }

  it should "return 401 if no session is found" in {
    put(s"$ToteutusPath", bytes(toteutus(koulutusOid))) {
      status should equal (401)
    }
  }

  it should "validate new toteutus" in {
    put(ToteutusPath, bytes(toteutus(koulutusOid).copy(tarjoajat = List("katkarapu").map(OrganisaatioOid))), defaultHeaders) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validateErrorBody(invalidOidsMsg(List("katkarapu").map(OrganisaatioOid))))
    }
  }

  it should "send indexing message after creating toteutus" in {
    val oid = put(toteutus(koulutusOid))
    eventuallyIndexingMessages { _ should contain (s"""{"toteutukset":["$oid"]}""") }
  }

  "POST /toteutus" should "update toteutus" in {
    val oid = put(toteutus(koulutusOid))
    val lastModified = get(oid, toteutus(oid, koulutusOid))
    update(toteutus(oid, koulutusOid, Arkistoitu), lastModified)
    get(oid, toteutus(oid, koulutusOid, Arkistoitu))
  }

  it should "not update koulutus" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    update(thisToteutus, lastModified, false)
    get(oid, thisToteutus)
  }

  it should "return 401 if no session is found" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    post(ToteutusPath, bytes(thisToteutus), Seq("If-Unmodified-Since" -> lastModified)) {
      status should equal (401)
    }
  }

  it should "fail update if 'If-Unmodified-Since' header is missing" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    post(ToteutusPath, bytes(thisToteutus), headers = defaultHeaders) {
      status should equal (400)
      body should include ("If-Unmodified-Since")
    }
  }

  it should "fail update if modified in between get and update" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    Thread.sleep(1500)
    update(toteutus(oid, koulutusOid, Arkistoitu), lastModified)
    post(ToteutusPath, bytes(thisToteutus), headersIfUnmodifiedSince(lastModified)) {
      status should equal (409)
    }
  }

  it should "update toteutuksen nimi, metadata ja tarjoajat" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    val uusiToteutus = thisToteutus.copy(
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      metadata = Some(thisToteutus.metadata.get.asInstanceOf[AmmatillinenToteutusMetadata].copy(kuvaus = Map(Fi -> "kuvaus", En -> "description"))),
      tarjoajat = List("2.2", "3.2", "4.2").map(OrganisaatioOid))
    update(uusiToteutus, lastModified, true)
    get(oid, uusiToteutus)
  }

  it should "delete all tarjoajat and read last modified from history" in {
    val oid = put(toteutus(koulutusOid))
    val thisToteutus = toteutus(oid, koulutusOid)
    val lastModified = get(oid, thisToteutus)
    Thread.sleep(1500)
    val uusiToteutus = thisToteutus.copy(tarjoajat = List())
    update(uusiToteutus, lastModified, true)
    get(oid, uusiToteutus) should not equal (lastModified)
  }

  it should "store and update unfinished toteutus" in {
    val unfinishedToteutus = new Toteutus(muokkaaja = UserOid("5.4.3.2"), koulutusOid = KoulutusOid(koulutusOid), organisaatioOid = OrganisaatioOid("1.2"), modified = None)
    val oid = put(unfinishedToteutus)
    val lastModified = get(oid, unfinishedToteutus.copy(oid = Some(ToteutusOid(oid))))
    val newKoulutusOid = put(koulutus)
    val newUnfinishedToteutus = unfinishedToteutus.copy(oid = Some(ToteutusOid(oid)), koulutusOid = KoulutusOid(newKoulutusOid))
    update(newUnfinishedToteutus, lastModified)
    get(oid, newUnfinishedToteutus)
  }

  it should "validate updated toteutus" in {
    val oid = put(toteutus(koulutusOid))
    val lastModified = get(oid, toteutus(oid, koulutusOid))
    post(ToteutusPath, bytes(toteutus(oid, koulutusOid).copy(tarjoajat = List("katkarapu").map(OrganisaatioOid))), headersIfUnmodifiedSince(lastModified)) {
      withClue(body) {
        status should equal(400)
      }
      body should equal (validateErrorBody(invalidOidsMsg(List("katkarapu").map(OrganisaatioOid))))
    }
  }

  it should "send indexing message after updating toteutus" in {
    val oid = put(toteutus(koulutusOid))
    eventuallyIndexingMessages { _ should contain (s"""{"toteutukset":["$oid"]}""") }

    update(toteutus(oid, koulutusOid, Arkistoitu), lastModified = get(oid, toteutus(oid, koulutusOid)))

    eventuallyIndexingMessages { _ should contain (s"""{"toteutukset":["$oid"]}""") }
  }
}
