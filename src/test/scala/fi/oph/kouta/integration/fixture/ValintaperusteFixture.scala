package fi.oph.kouta.integration.fixture

import java.util.UUID

import fi.oph.kouta.domain._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.servlet.ValintaperusteServlet

trait ValintaperusteFixture { this: KoutaIntegrationSpec =>

  val ValintaperustePath = "/valintaperuste"

  addServlet(new ValintaperusteServlet(), ValintaperustePath)

  val valintaperuste = new Valintaperuste(
    id = None,
    tila = Julkaistu,
    hakutapa = Some("hakutapa_koodi#1"),
    kohdejoukko = Some("kohdejoukko_koodi#2"),
    kohdejoukonTarkenne = Some("kohdejoukonTarkenne_koodi#2"),
    nimi = Map(Fi -> "nimi", Sv -> "nimi sv"),
    metadata = None,
    organisaatio = "1.2.3.4",
    muokkaaja = "Hassu Muokkaaja",
    kielivalinta = List(Fi, Sv))

  def valintaperuste(id:UUID): Valintaperuste = valintaperuste.copy(id = Some(id))
  def valintaperuste(id:UUID, tila:Julkaisutila): Valintaperuste = valintaperuste.copy(id = Some(id), tila = tila)

  def put(valintaperuste:Valintaperuste):UUID = put(ValintaperustePath, valintaperuste, id(_))
  def get(id:UUID, expected:Valintaperuste):String = get(ValintaperustePath, id, expected)
  def update(valintaperuste:Valintaperuste, lastModified:String, expectUpdate:Boolean = true):Unit = update(ValintaperustePath, valintaperuste, lastModified, expectUpdate)
  def update(valintaperuste:Valintaperuste, lastModified:String):Unit = update(valintaperuste, lastModified, true)
  //def list: (List[(String, String)], List[IdListResponse]) => List[IdListResponse] = list(ValintaperustePath, _, _)
}
