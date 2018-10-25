package fi.oph.kouta.integration.fixture

import java.util.UUID

import fi.oph.kouta.domain._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.servlet.ValintaperusteServlet
import org.json4s.jackson.Serialization.read

trait ValintaperusteFixture extends CommonFixture { this: KoutaIntegrationSpec =>

  addServlet(new ValintaperusteServlet(), "/valintaperuste")

  val valintaperuste = new Valintaperuste(
    id = None,
    tila = Julkaistu,
    kohde = Some(KohdeAmmatillinen),
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = None,
    organisaatio = "1.2.3.4",
    muokkaaja = "Hassu Muokkaaja",
    kielivalinta = List(Fi, Sv))

  def valintaperuste(id:UUID): Valintaperuste = valintaperuste.copy(id = Some(id))
  def valintaperuste(id:UUID, tila:Julkaisutila): Valintaperuste = valintaperuste.copy(id = Some(id), tila = tila)

  def putValintaperusteOk(valintaperuste:Valintaperuste) = {
    put("/valintaperuste", bytes(valintaperuste)) {
      status should equal(200)
      id(body)
    }
  }

  def getValintaperusteOk(id:UUID, expected:Valintaperuste) = {
    get(s"/valintaperuste/$id") {
      status should equal (200)
      read[Valintaperuste](body) should equal (expected)
      header.get("Last-Modified").get
    }
  }

  def updateValintaperusteOk(valintaperuste:Valintaperuste, lastModified:String, expectUpdate:Boolean = true) = {
    post("/valintaperuste", bytes(valintaperuste), headersIfUnmodifiedSince(lastModified)) {
      status should equal (200)
      updated(body) should equal (expectUpdate)
    }
  }
}
