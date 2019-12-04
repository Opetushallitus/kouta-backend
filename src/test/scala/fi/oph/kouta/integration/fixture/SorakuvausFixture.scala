package fi.oph.kouta.integration.fixture

import java.util.UUID

import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Julkaisutila, Sorakuvaus, SorakuvausListItem}
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.{SqsInTransactionServiceIgnoringIndexing, TestData}
import fi.oph.kouta.service.SorakuvausService
import fi.oph.kouta.servlet.SorakuvausServlet
import fi.oph.kouta.util.AuditLog

trait SorakuvausFixture { this: KoutaIntegrationSpec =>

  val SorakuvausPath = "/sorakuvaus"

  protected lazy val sorakuvausService: SorakuvausService =
    new SorakuvausService(SqsInTransactionServiceIgnoringIndexing, new AuditLog(MockAuditLogger))

  addServlet(new SorakuvausServlet(sorakuvausService), SorakuvausPath)

  val sorakuvaus = TestData.AmmSorakuvaus

  def sorakuvaus(id:UUID): Sorakuvaus = sorakuvaus.copy(id = Some(id))
  def sorakuvaus(id:UUID, tila:Julkaisutila): Sorakuvaus = sorakuvaus.copy(id = Some(id), tila = tila)

  def sorakuvaus(tila: Julkaisutila, organisaatioOid: OrganisaatioOid): Sorakuvaus =
    sorakuvaus.copy(organisaatioOid = organisaatioOid, tila = tila)

  def put(sorakuvaus: Sorakuvaus): UUID = put(SorakuvausPath, sorakuvaus, id(_))
  def put(sorakuvaus: Sorakuvaus, sessionId: UUID): UUID = put(SorakuvausPath, sorakuvaus, sessionId, id(_))

  def get(id: UUID, expected: Sorakuvaus): String = get(SorakuvausPath, id, expected.copy(modified = Some(readModifiedById(id, "sorakuvaukset"))))
  def get(id: UUID, sessionId: UUID, expected: Sorakuvaus): String = get(SorakuvausPath, id, sessionId, expected.copy(modified = Some(readModifiedById(id, "sorakuvaukset"))))

  def update(sorakuvaus: Sorakuvaus, lastModified: String, expectedStatus: Int, sessionId: UUID): Unit = update(SorakuvausPath, sorakuvaus, lastModified, sessionId, expectedStatus)
  def update(sorakuvaus: Sorakuvaus, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit = update(SorakuvausPath, sorakuvaus, lastModified, expectUpdate, sessionId)
  def update(sorakuvaus: Sorakuvaus, lastModified: String, expectUpdate: Boolean): Unit = update(SorakuvausPath, sorakuvaus, lastModified, expectUpdate)
  def update(sorakuvaus: Sorakuvaus, lastModified: String): Unit = update(sorakuvaus, lastModified, true)

  def addToList(sorakuvaus: Sorakuvaus) = {
    val id = put(sorakuvaus)
    val modified = readModifiedById(id, "sorakuvaukset")
    SorakuvausListItem(id, sorakuvaus.nimi, sorakuvaus.tila,
      sorakuvaus.organisaatioOid, sorakuvaus.muokkaaja, modified)
  }
}
