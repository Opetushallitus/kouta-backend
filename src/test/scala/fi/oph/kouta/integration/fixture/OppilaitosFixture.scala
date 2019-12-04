package fi.oph.kouta.integration.fixture

import java.util.UUID

import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Julkaisutila, Oppilaitos}
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.mocks.{MockAuditLogger, MockS3Service}
import fi.oph.kouta.service.OppilaitosService
import fi.oph.kouta.servlet.OppilaitosServlet
import fi.oph.kouta.util.AuditLog
import fi.oph.kouta.{SqsInTransactionServiceIgnoringIndexing, TestData}

import scala.util.Random

trait OppilaitosFixture { this: KoutaIntegrationSpec =>

  val OppilaitosPath = "/oppilaitos"

  protected lazy val oppilaitosService: OppilaitosService =
    new OppilaitosService(SqsInTransactionServiceIgnoringIndexing, MockS3Service, new AuditLog(MockAuditLogger))

  addServlet(new OppilaitosServlet(oppilaitosService), OppilaitosPath)

  private lazy val random = new Random()

  def oppilaitos = TestData.JulkaistuOppilaitos.copy(oid = OrganisaatioOid(s"1.2.246.562.10.000${random.nextInt()}"))

  def oppilaitos(oid: String): Oppilaitos = oppilaitos.copy(oid = OrganisaatioOid(oid))
  def oppilaitos(oid: String, tila: Julkaisutila): Oppilaitos = oppilaitos.copy(oid = OrganisaatioOid(oid), tila = tila)

  def oppilaitos(tila: Julkaisutila, organisaatioOid: OrganisaatioOid): Oppilaitos =
    oppilaitos.copy(organisaatioOid = organisaatioOid, tila = tila)

  def put(oppilaitos: Oppilaitos): String = put(OppilaitosPath, oppilaitos, oid(_))
  def put(oppilaitos: Oppilaitos, sessionId: UUID): String = put(OppilaitosPath, oppilaitos, sessionId, oid(_))

  def get(oid: String, expected: Oppilaitos): String = get(OppilaitosPath, oid, expected.copy(modified = Some(readModifiedByOid(oid, "oppilaitokset"))))
  def get(oid: String, sessionId: UUID, expected: Oppilaitos): String = get(OppilaitosPath, oid, sessionId, expected.copy(modified = Some(readModifiedByOid(oid, "oppilaitokset"))))

  def update(oppilaitos: Oppilaitos, lastModified: String, expectedStatus: Int, sessionId: UUID): Unit = update(OppilaitosPath, oppilaitos, lastModified, sessionId, expectedStatus)
  def update(oppilaitos: Oppilaitos, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit = update(OppilaitosPath, oppilaitos, lastModified, expectUpdate, sessionId)
  def update(oppilaitos: Oppilaitos, lastModified: String, expectUpdate: Boolean): Unit = update(OppilaitosPath, oppilaitos, lastModified, expectUpdate)
  def update(oppilaitos: Oppilaitos, lastModified: String): Unit = update(oppilaitos, lastModified, true)

}
