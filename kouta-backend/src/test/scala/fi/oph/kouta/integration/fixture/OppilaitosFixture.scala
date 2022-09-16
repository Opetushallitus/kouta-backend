package fi.oph.kouta.integration.fixture

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.domain.oid.{OrganisaatioOid, RootOrganisaatioOid}
import fi.oph.kouta.domain.{Julkaisutila, Modified, Oppilaitos}
import fi.oph.kouta.integration.{AccessControlSpec, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.{MockAuditLogger, MockS3ImageService}
import fi.oph.kouta.repository.OppilaitosDAO
import fi.oph.kouta.service.OppilaitosService
import fi.oph.kouta.servlet.OppilaitosServlet
import fi.oph.kouta.util.TimeUtils
import fi.oph.kouta.{SqsInTransactionServiceIgnoringIndexing, TestData, TestOids}

import java.util.UUID

trait OppilaitosFixture extends KoulutusFixture with KoutaIntegrationSpec with AccessControlSpec {

  val OppilaitosPath = "/oppilaitos"

  def oppilaitosService: OppilaitosService = {
    new OppilaitosService(SqsInTransactionServiceIgnoringIndexing, MockS3ImageService, new AuditLog(MockAuditLogger), new OrganisaatioServiceImpl(s"http://localhost:$mockPort/organisaatio-service/rest/organisaatio/v4/$RootOrganisaatioOid/jalkelaiset"), mockOppijanumerorekisteriClient, mockKayttooikeusClient, mockOrganisaatioClient)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(new OppilaitosServlet(oppilaitosService), OppilaitosPath)
  }

  def oppilaitos = TestData.JulkaistuOppilaitos.copy(oid = TestOids.randomOrganisaatioOid)

  def oppilaitos(oid: String): Oppilaitos = oppilaitos.copy(oid = OrganisaatioOid(oid))
  def oppilaitos(oid: String, tila: Julkaisutila): Oppilaitos = oppilaitos.copy(oid = OrganisaatioOid(oid), tila = tila)

  def oppilaitos(tila: Julkaisutila, organisaatioOid: OrganisaatioOid): Oppilaitos =
    oppilaitos.copy(organisaatioOid = organisaatioOid, tila = tila)

  def put(oppilaitos: Oppilaitos): String = put(OppilaitosPath, oppilaitos, oid(_))
  def put(oppilaitos: Oppilaitos, sessionId: UUID): String = put(OppilaitosPath, oppilaitos, sessionId, oid(_))

  def get(oid: String, expected: Oppilaitos): String = get(OppilaitosPath, oid, expected.copy(modified = Some(readOppilaitosModified(oid))))
  def get(oid: String, sessionId: UUID, expected: Oppilaitos): String = get(OppilaitosPath, oid, sessionId, expected.copy(modified = Some(readOppilaitosModified(oid))))

  def update(oppilaitos: Oppilaitos, lastModified: String, expectedStatus: Int, sessionId: UUID): Unit = update(OppilaitosPath, oppilaitos, lastModified, sessionId, expectedStatus)
  def update(oppilaitos: Oppilaitos, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit = update(OppilaitosPath, oppilaitos, lastModified, expectUpdate, sessionId)
  def update(oppilaitos: Oppilaitos, lastModified: String, expectUpdate: Boolean): Unit = update(OppilaitosPath, oppilaitos, lastModified, expectUpdate)
  def update(oppilaitos: Oppilaitos, lastModified: String): Unit = update(oppilaitos, lastModified, true)

  def readOppilaitosModified(oid: String): Modified = readOppilaitosModified(OrganisaatioOid(oid))
  def readOppilaitosModified(oid: OrganisaatioOid): Modified =
    TimeUtils.instantToModifiedAt(db.runBlocking(OppilaitosDAO.selectLastModified(oid)).get)
}
