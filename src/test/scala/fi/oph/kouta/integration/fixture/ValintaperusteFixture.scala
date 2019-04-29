package fi.oph.kouta.integration.fixture

import java.util.UUID

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.service.ValintaperusteService
import fi.oph.kouta.servlet.ValintaperusteServlet
import fi.oph.kouta.{SqsInTransactionServiceIgnoringIndexing, TestData}

object ValintaperusteServiceIgnoringIndexing extends ValintaperusteService(SqsInTransactionServiceIgnoringIndexing)

trait ValintaperusteFixture { this: KoutaIntegrationSpec =>

  val ValintaperustePath = "/valintaperuste"

  protected lazy val valintaperusteService: ValintaperusteService = ValintaperusteServiceIgnoringIndexing

  addServlet(new ValintaperusteServlet(valintaperusteService), ValintaperustePath)

  val valintaperuste = TestData.AmmValintaperuste

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
    new ValintaperusteListItem(id, valintaperuste.nimi, valintaperuste.tila,
      valintaperuste.organisaatioOid, valintaperuste.muokkaaja, modified)
  }
}
