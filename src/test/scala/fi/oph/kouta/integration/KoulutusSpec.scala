package fi.oph.kouta.integration

import fi.oph.kouta.domain.Julkaisutila._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.Koulutustyyppi._
import fi.oph.kouta.servlet.KoulutusServlet
import org.json4s.jackson.Serialization.{read, write}

class KoulutusSpec extends KoutaIntegrationSpec {

  addServlet(new KoulutusServlet(), "/koulutus")

  val koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = true,
    koulutustyyppi = Amm,
    koulutusKoodiUri = "koulutus_123#1",
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = new KoulutusMetadata(),
    tarjoajat = List("1.2", "2.2", "3.2"),
    muokkaaja = "Mörkö Muokkaaja")

  def koulutus(oid:String): Koulutus = koulutus.copy(oid = Some(oid))
  def koulutus(oid:String, tila:Julkaisutila): Koulutus = koulutus.copy(oid = Some(oid), tila = tila)

  def bytes = {
    val json = write(koulutus)
    json.getBytes
  }

  case class Oid(oid:String)
  def oid(body: String) = (read[Oid](body)).oid

  case class Updated(updated:Boolean)
  def updated(body: String) = read[Updated](body).updated


  def putOk(koulutus:Koulutus) = {
    put("/koulutus", bytes) {
      status should equal(200)
      oid(body)
    }
  }

  def getOk(oid:String, expected:Koulutus) = {
    get(s"/koulutus/$oid") {
      status should equal (200)
      read[Koulutus](body) should equal (expected)
      header.get("Last-Modified").get
    }
  }

  def updateOk(koulutus:Koulutus, lastModified:String, expectUpdate:Boolean = true) = {
    post("/koulutus", write(koulutus).getBytes, List(("If-Unmodified-Since", lastModified))) {
      status should equal (200)
      updated(body) should equal (expectUpdate)
    }
  }

  it should "return 404 if koulutus not found" in {
    get("/koulutus/123") {
      status should equal (404)
      body should include ("Unknown koulutus oid")
    }
  }

  it should "store koulutus" in {
    val oid = putOk(koulutus)
    getOk(oid, koulutus(oid))
  }

  it should "update koulutus" in {
    val oid = putOk(koulutus)
    val lastModified = getOk(oid, koulutus(oid))
    updateOk(koulutus(oid, Arkistoitu), lastModified)
    getOk(oid, koulutus(oid, Arkistoitu))
  }

  it should "not update koulutus" in {
    val oid = putOk(koulutus)
    val lastModified = getOk(oid, koulutus(oid))
    updateOk(koulutus(oid), lastModified, false)
    getOk(oid, koulutus(oid))
  }

  it should "fail update if 'If-Unmodified-Since' header is missing" in {
    val oid = putOk(koulutus)
    val lastModified = getOk(oid, koulutus(oid))
    post("/koulutus", write(koulutus(oid)).getBytes) {
      status should equal (400)
      body should include ("If-Unmodified-Since")
    }
  }

  it should "fail update if modified in between get and update" in {
    val oid = putOk(koulutus)
    val lastModified = getOk(oid, koulutus(oid))
    Thread.sleep(1500)
    updateOk(koulutus(oid, Arkistoitu), lastModified)
    post("/koulutus", write(koulutus(oid)).getBytes, List(("If-Unmodified-Since", lastModified))) {
      status should equal (409)
    }
  }

  it should "update koulutuksen tekstit ja tarjoajat" in {
    val oid = putOk(koulutus)
    val lastModified = getOk(oid, koulutus(oid))
    val uusiKoulutus = koulutus(oid).copy(
      nimi = Map(Fi -> "kiva nimi", Sv -> "nimi sv", En -> "nice name"),
      metadata = new KoulutusMetadata(Map(Fi -> "kuvaus", En -> "description")),
      tarjoajat = List("2.2", "3.2", "4.2"))
    updateOk(uusiKoulutus, lastModified, true)
    getOk(oid, uusiKoulutus)
  }

  it should "delete all tarjoajat and read last modified from history" in {
    val oid = putOk(koulutus)
    val lastModified = getOk(oid, koulutus(oid))
    Thread.sleep(1500)
    val uusiKoulutus = koulutus(oid).copy(tarjoajat = List())
    updateOk(uusiKoulutus, lastModified, true)
    getOk(oid, uusiKoulutus) should not equal (lastModified)
  }
}
