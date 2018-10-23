package fi.oph.kouta.integration.fixture

import fi.oph.kouta.domain._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.servlet.KoulutusServlet
import org.json4s.jackson.Serialization.read

trait KoulutusFixture extends CommonFixture { this: KoutaIntegrationSpec =>

  addServlet(new KoulutusServlet(), "/koulutus")

  val koulutus = Koulutus(
    oid = None,
    johtaaTutkintoon = true,
    koulutustyyppi = Some(Amm),
    koulutusKoodiUri = Some("koulutus_123#1"),
    tila = Julkaistu,
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = Some(new KoulutusMetadata()),
    tarjoajat = List("1.2", "2.2", "3.2"),
    muokkaaja = "Mörkö Muokkaaja")

  def koulutus(oid:String): Koulutus = koulutus.copy(oid = Some(oid))
  def koulutus(oid:String, tila:Julkaisutila): Koulutus = koulutus.copy(oid = Some(oid), tila = tila)

  def putKoulutusOk(koulutus:Koulutus) = {
    put("/koulutus", bytes(koulutus)) {
      status should equal(200)
      oid(body)
    }
  }

  def getKoulutusOk(oid:String, expected:Koulutus) = {
    get(s"/koulutus/$oid") {
      status should equal (200)
      read[Koulutus](body) should equal (expected)
      header.get("Last-Modified").get
    }
  }

  def updateKoulutusOk(koulutus:Koulutus, lastModified:String, expectUpdate:Boolean = true) = {
    post("/koulutus", bytes(koulutus), headersIfUnmodifiedSince(lastModified)) {
      status should equal (200)
      updated(body) should equal (expectUpdate)
    }
  }

  def listKoulutusOk(params:List[(String, String)], expectedOids:List[String]) = {
    get("/koulutus/list", params) {
      status should equal (200)
      val result = read[List[ListResponse]](body).map(_.oid)
      result should equal(expectedOids)
      result
    }
  }
}