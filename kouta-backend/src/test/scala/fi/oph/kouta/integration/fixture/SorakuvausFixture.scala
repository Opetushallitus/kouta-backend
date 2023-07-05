package fi.oph.kouta.integration.fixture

import java.util.UUID
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{KoodistoClient, MockKoutaIndeksoijaClient}
import fi.oph.kouta.domain.oid.{OrganisaatioOid}
import fi.oph.kouta.domain.{Julkaisutila, Modified, Sorakuvaus, SorakuvausListItem}
import fi.oph.kouta.integration.{AccessControlSpec, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.MockAuditLogger
import fi.oph.kouta.repository.{KoulutusDAO, SorakuvausDAO}
import fi.oph.kouta.service.{KoodistoService, OrganisaatioServiceImpl, SorakuvausService, SorakuvausServiceValidation}
import fi.oph.kouta.servlet.SorakuvausServlet
import fi.oph.kouta.util.TimeUtils
import fi.oph.kouta.{SqsInTransactionServiceIgnoringIndexing, TestData, TestOids}

trait SorakuvausFixture extends AccessControlSpec {
  this: KoutaIntegrationSpec =>

  val SorakuvausPath = "/sorakuvaus"

  def sorakuvausService: SorakuvausService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    val koodistoService = new KoodistoService(new KoodistoClient(urlProperties.get))
    val koutaIndeksoijaClient = new MockKoutaIndeksoijaClient
    val sorakuvausServiceValidation =
      new SorakuvausServiceValidation(koodistoService, KoulutusDAO)
    new SorakuvausService(
      SqsInTransactionServiceIgnoringIndexing,
      new AuditLog(MockAuditLogger),
      organisaatioService,
      mockOppijanumerorekisteriClient,
      mockKayttooikeusClient,
      sorakuvausServiceValidation,
      koutaIndeksoijaClient
    )
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(new SorakuvausServlet(sorakuvausService), SorakuvausPath)
  }

  val sorakuvaus   = TestData.AmmSorakuvaus
  val yoSorakuvaus = TestData.YoSorakuvaus

  def sorakuvaus(id: UUID): Sorakuvaus                     = sorakuvaus.copy(id = Some(id))
  def sorakuvaus(id: UUID, tila: Julkaisutila): Sorakuvaus = sorakuvaus.copy(id = Some(id), tila = tila)

  def sorakuvaus(tila: Julkaisutila, organisaatioOid: OrganisaatioOid): Sorakuvaus =
    sorakuvaus.copy(organisaatioOid = organisaatioOid, tila = tila)

  def put(sorakuvaus: Sorakuvaus): UUID                  = put(SorakuvausPath, sorakuvaus, ophSession, id(_))
  def put(sorakuvaus: Sorakuvaus, sessionId: UUID): UUID = put(SorakuvausPath, sorakuvaus, sessionId, id(_))

  def get(id: UUID, expected: Sorakuvaus): String =
    get(SorakuvausPath, id, expected.copy(modified = Some(readSorakuvausModified(id))))
  def get(id: UUID, sessionId: UUID, expected: Sorakuvaus): String =
    get(SorakuvausPath, id, sessionId, expected.copy(modified = Some(readSorakuvausModified(id))))

  def update(sorakuvaus: Sorakuvaus, lastModified: String, expectedStatus: Int, sessionId: UUID): Unit =
    update(SorakuvausPath, sorakuvaus, lastModified, sessionId, expectedStatus)
  def update(sorakuvaus: Sorakuvaus, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit =
    update(SorakuvausPath, sorakuvaus, lastModified, expectUpdate, sessionId)
  def update(sorakuvaus: Sorakuvaus, lastModified: String, expectUpdate: Boolean): Unit =
    update(SorakuvausPath, sorakuvaus, lastModified, expectUpdate, ophSession)
  def update(sorakuvaus: Sorakuvaus, lastModified: String): Unit = update(sorakuvaus, lastModified, true, ophSession)
  def update(sorakuvaus: Sorakuvaus, lastModified: String, sessionId: UUID): Unit =
    update(SorakuvausPath, sorakuvaus, lastModified, true, sessionId)

  def addToList(sorakuvaus: Sorakuvaus) = {
    val id       = put(sorakuvaus, ophSession)
    val modified = readSorakuvausModified(id)
    SorakuvausListItem(id, sorakuvaus.nimi, sorakuvaus.tila, sorakuvaus.organisaatioOid, sorakuvaus.muokkaaja, modified)
  }

  def readSorakuvausModified(id: String): Modified = readSorakuvausModified(UUID.fromString(id))
  def readSorakuvausModified(id: UUID): Modified =
    TimeUtils.instantToModifiedAt(db.runBlocking(SorakuvausDAO.selectLastModified(id)).get)
}
