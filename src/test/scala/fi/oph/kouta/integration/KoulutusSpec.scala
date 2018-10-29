package fi.oph.kouta.integration

import fi.oph.kouta.domain._
import fi.oph.kouta.integration.fixture.KoulutusFixture
import org.json4s.jackson.Serialization.read

class KoulutusSpec extends KoutaIntegrationSpec with KoulutusFixture {

  it should "return 404 if koulutus not found" in {
    get("/koulutus/123") {
      status should equal (404)
      body should include ("Unknown koulutus oid")
    }
  }

  it should "store koulutus" in {
    val oid = put(koulutus)
    get(oid, koulutus(oid))
  }

  it should "update koulutus" in {
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

  it should "fail update if 'If-Unmodified-Since' header is missing" in {
    val oid = put(koulutus)
    val lastModified = get(oid, koulutus(oid))
    post(KoulutusPath, bytes(koulutus(oid))) {
      status should equal (400)
      body should include ("If-Unmodified-Since")
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
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      metadata = Some(new KoulutusMetadata(Map(Fi -> "kuvaus", En -> "description"))),
      tarjoajat = List("2.2", "3.2", "4.2"))
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
    val unfinishedKoulutus = new Koulutus(johtaaTutkintoon = true, muokkaaja = "Muikea Muokkaaja")
    val oid = put(unfinishedKoulutus)
    val lastModified = get(oid, unfinishedKoulutus.copy(oid = Some(oid)))
    val newUnfinishedKoulutus = unfinishedKoulutus.copy(oid = Some(oid), johtaaTutkintoon = false)
    update(newUnfinishedKoulutus, lastModified)
    get(oid, newUnfinishedKoulutus)
  }

  /*it should "validate julkaistu koulutus" in {
    val unfinishedKoulutus = new Koulutus(johtaaTutkintoon = true, muokkaaja = "Muikea Muokkaaja", tila = Julkaistu)
    put("/koulutus", bytes(unfinishedKoulutus)) {
      status should equal(400)
      body should include ("Pakollisia tietoja puuttuu")
    }
  }*/

  it should "list koulutukset" in {
    def r(oids:List[String]) = oids.map(OidListResponse(_, koulutus.nimi))
    truncateDatabase()
    val oid1 = put(koulutus.copy(tarjoajat = List("5.5", "6.5")))
    val oid2 = put(koulutus.copy(tarjoajat = List("6.5"), tila = Tallennettu))
    val oid3 = put(koulutus.copy(tarjoajat = List("5.5"), tila = Tallennettu))
    list(List(("tila", "julkaistu"), ("tarjoaja", "6.5")), r(List(oid1)))
    list(List(("tarjoaja", "5.5")), r(List(oid1, oid3)))
    list(List(("tila", "tallennettu")), r(List(oid2, oid3)))
    list(List(), r(List(oid1, oid2, oid3)))
  }
}
