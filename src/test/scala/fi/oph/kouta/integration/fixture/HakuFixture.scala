package fi.oph.kouta.integration.fixture

import java.time.Instant

import fi.oph.kouta.domain._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.servlet.HakuServlet
import org.json4s.jackson.Serialization.read

trait HakuFixture extends CommonFixture { this: KoutaIntegrationSpec =>

  addServlet(new HakuServlet(), "/haku")
  
  val haku = new Haku(
    nimi = Map(Fi -> "Haku fi", Sv -> "Haku sv"),
    hakutapa = Some(Jatkuvahaku),
    hakukohteen_liittamisen_takaraja = Some(Instant.now()),
    hakukohteen_muokkaamisen_takaraja = Some(Instant.now()),
    alkamiskausi = Some(Kevät),
    alkamisvuosi = Some("2019"),
    kohdejoukko = Some("kohdejoukko_uri#11"),
    kohdejoukon_tarkenne = Some("kohdejoukon_tarkenne_uri#11"),
    hakulomaketyyppi = Some(EiSähköistä),
    hakulomake = Some("Hakulomake tulostetaan ja toimitetaan postitse"),
    metadata = Some(HakuMetadata(yhteystieto = Some(new Yhteystieto(nimi = "Iines Ankka")))),
    hakuajat = List(HaunHakuaika(alkaa = Instant.now(), paattyy = Instant.now.plusSeconds(9000))),
    organisaatio = "1.2.3.4",
    muokkaaja = "Harri Muokkaaja",
    kielivalinta = Seq(Fi, Sv))

  def haku(oid:String): Haku = haku.copy(oid = Some(oid))
  def haku(oid:String, tila:Julkaisutila): Haku = haku.copy(oid = Some(oid), tila = tila)

  def putHakuOk(haku:Haku) = {
    put("/haku", bytes(haku)) {
      status should equal(200)
      oid(body)
    }
  }

  def getHakuOk(oid:String, expected:Haku) = {
    get(s"/haku/$oid") {
      status should equal (200)
      read[Haku](body) should equal (expected)
      header.get("Last-Modified").get
    }
  }

  def updateHakuOk(haku:Haku, lastModified:String, expectUpdate:Boolean = true) = {
    post("/haku", bytes(haku), headersIfUnmodifiedSince(lastModified)) {
      status should equal (200)
      updated(body) should equal (expectUpdate)
    }
  }
}
