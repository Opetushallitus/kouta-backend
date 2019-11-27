package fi.oph.kouta.integration.fixture

import java.util.UUID

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.service.ValintaperusteService
import fi.oph.kouta.servlet.ValintaperusteServlet
import fi.oph.kouta.util.AuditLog
import fi.oph.kouta.{SqsInTransactionServiceIgnoringIndexing, TestData}
import org.scalactic.Equality

trait ValintaperusteFixture { this: KoutaIntegrationSpec =>

  val ValintaperustePath = "/valintaperuste"

  protected lazy val valintaperusteService: ValintaperusteService = new ValintaperusteService(SqsInTransactionServiceIgnoringIndexing, new AuditLog(MockAuditLogger))

  addServlet(new ValintaperusteServlet(valintaperusteService), ValintaperustePath)

  val valintaperuste = TestData.AmmValintaperuste

  def getIds(valintaperuste: Valintaperuste): Valintaperuste = {
    import slick.jdbc.PostgresProfile.api._
    valintaperuste.copy(
      valintakokeet = valintaperuste.valintakokeet.map(valintakoe => valintakoe.copy(id = db.runBlocking(
        sql"""select id from valintaperusteiden_valintakokeet
              where valintaperuste_id = ${valintaperuste.id.map(_.toString)}::uuid
                and tyyppi_koodi_uri = ${valintakoe.tyyppiKoodiUri}""".as[String]).headOption.map(UUID.fromString)))
    )
  }

  def tallennettuValintaperuste(id: UUID): Valintaperuste = getIds(valintaperuste(id))

  def valintaperuste(id:UUID): Valintaperuste = valintaperuste.copy(id = Some(id))
  def valintaperuste(id:UUID, tila:Julkaisutila): Valintaperuste = valintaperuste.copy(id = Some(id), tila = tila)

  def put(valintaperuste: Valintaperuste): UUID = put(ValintaperustePath, valintaperuste, id(_))
  def put(valintaperuste: Valintaperuste, sessionId: UUID): UUID = put(ValintaperustePath, valintaperuste, sessionId, id(_))

  implicit val valintaperusteEquality: Equality[Valintaperuste] = (a: Valintaperuste, b: Any) => b match {
    case v: Valintaperuste =>
      val that = a.copy(valintakokeet = a.valintakokeet.map(_.copy(id = None)))
      val other = v.copy(valintakokeet = a.valintakokeet.map(_.copy(id = None)))
      Equality.default[Valintaperuste].areEqual(that, other)
    case _ => false
  }

  def get(id: UUID, expected: Valintaperuste): String = get(ValintaperustePath, id, expected.copy(modified = Some(readModifiedById(id, "valintaperusteet"))))
  def get(id: UUID, sessionId: UUID, expected: Valintaperuste): String = get(ValintaperustePath, id, sessionId, expected.copy(modified = Some(readModifiedById(id, "valintaperusteet"))))

  def update(valintaperuste: Valintaperuste, lastModified: String, expectedStatus: Int, sessionId: UUID): Unit = update(ValintaperustePath, valintaperuste, lastModified, sessionId, expectedStatus)
  def update(valintaperuste: Valintaperuste, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit = update(ValintaperustePath, valintaperuste, lastModified, expectUpdate, sessionId)
  def update(valintaperuste: Valintaperuste, lastModified: String, expectUpdate: Boolean): Unit = update(ValintaperustePath, valintaperuste, lastModified, expectUpdate)
  def update(valintaperuste: Valintaperuste, lastModified: String): Unit = update(valintaperuste, lastModified, true)

  def valintaperuste(tila: Julkaisutila, organisaatioOid: OrganisaatioOid): Valintaperuste =
    valintaperuste.copy(organisaatioOid = organisaatioOid, tila = tila)

  def addToList(valintaperuste:Valintaperuste) = {
    val id = put(valintaperuste)
    val modified = readModifiedById(id, "valintaperusteet")
    ValintaperusteListItem(id, valintaperuste.nimi, valintaperuste.tila,
      valintaperuste.organisaatioOid, valintaperuste.muokkaaja, modified)
  }
}
