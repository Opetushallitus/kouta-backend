package fi.oph.kouta.integration.fixture

import java.util.UUID

import fi.oph.kouta.TestData.JulkaistuValintaperuste
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.servlet.ValintaperusteServlet
import org.scalactic.Equality

trait ValintaperusteFixture { this: KoutaIntegrationSpec =>

  val ValintaperustePath = "/valintaperuste"

  addServlet(new ValintaperusteServlet(), ValintaperustePath)

  val valintaperuste = JulkaistuValintaperuste

  def valintaperuste(id:UUID): Valintaperuste = valintaperuste.copy(id = Some(id))
  def valintaperuste(id:UUID, tila:Julkaisutila): Valintaperuste = valintaperuste.copy(id = Some(id), tila = tila)

  def put(valintaperuste:Valintaperuste):UUID = put(ValintaperustePath, valintaperuste, id(_))
  def get(id:UUID, expected:Valintaperuste):String = get(ValintaperustePath, id, expected.copy(modified = Some(readModifiedById(id, "valintaperusteet"))))
  def update(valintaperuste:Valintaperuste, lastModified:String, expectUpdate:Boolean = true):Unit = update(ValintaperustePath, valintaperuste, lastModified, expectUpdate)
  def update(valintaperuste:Valintaperuste, lastModified:String):Unit = update(valintaperuste, lastModified, true)
  //def list: (List[(String, String)], List[IdListResponse]) => List[IdListResponse] = list(ValintaperustePath, _, _)

  def valintaperuste(tila:Julkaisutila, organisaatioOid:String): Valintaperuste =
    valintaperuste.copy(organisaatioOid = OrganisaatioOid(organisaatioOid), tila = tila)

  def addToList(valintaperuste:Valintaperuste) = {
    val id = put(valintaperuste)
    val modified = readModifiedById(id, "valintaperusteet")
    toIdListItem(id, valintaperuste, modified)
  }
}
