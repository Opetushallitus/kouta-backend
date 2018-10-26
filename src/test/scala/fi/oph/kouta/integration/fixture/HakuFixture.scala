package fi.oph.kouta.integration.fixture

import java.time.Instant

import fi.oph.kouta.domain._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.servlet.HakuServlet
import org.json4s.jackson.Serialization.read

trait HakuFixture { this: KoutaIntegrationSpec =>

  val HakuPath = "/haku"

  addServlet(new HakuServlet(), HakuPath)
  
  val haku = new Haku(
    nimi = Map(Fi -> "Haku fi", Sv -> "Haku sv"),
    hakutapaKoodiUri = Some("hakutapa_03#1"),
    hakukohteenLiittamisenTakaraja = Some(Instant.now()),
    hakukohteenMuokkaamisenTakaraja = Some(Instant.now()),
    alkamiskausiKoodiUri = Some("kausi_k#1"),
    alkamisvuosi = Some("2019"),
    kohdejoukkoKoodiUri = Some("kohdejoukko_05#11"),
    kohdejoukonTarkenneKoodiUri = Some("haunkohdejoukontarkenne_1#11"),
    hakulomaketyyppi = Some(EiSähköistä),
    hakulomake = Some("Hakulomake tulostetaan ja toimitetaan postitse"),
    metadata = Some(HakuMetadata(yhteystieto = Some(new Yhteystieto(nimi = "Iines Ankka")))),
    hakuajat = List(HaunHakuaika(alkaa = Instant.now(), paattyy = Instant.now.plusSeconds(9000))),
    organisaatio = "1.2.3.4",
    muokkaaja = "Harri Muokkaaja",
    kielivalinta = Seq(Fi, Sv))

  def haku(oid:String): Haku = haku.copy(oid = Some(oid))
  def haku(oid:String, tila:Julkaisutila): Haku = haku.copy(oid = Some(oid), tila = tila)

  def put(haku:Haku):String = put(HakuPath, haku, oid(_))
  def get(oid:String, expected:Haku):String = get(HakuPath, oid, expected)
  def update(haku:Haku, lastModified:String, expectUpdate:Boolean):Unit = update(HakuPath, haku, lastModified, expectUpdate)
  def update(haku:Haku, lastModified:String):Unit = update(haku, lastModified, true)
}
