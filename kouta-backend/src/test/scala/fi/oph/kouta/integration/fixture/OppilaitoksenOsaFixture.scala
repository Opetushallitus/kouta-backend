package fi.oph.kouta.integration.fixture

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{KoodistoClient, OrganisaatioServiceClient}
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Julkaisutila, Modified, OppilaitoksenOsa, OppilaitoksenOsaListItem}
import fi.oph.kouta.integration.{AccessControlSpec, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.{MockAuditLogger, MockS3ImageService}
import fi.oph.kouta.repository.{OppilaitoksenOsaDAO, OppilaitosDAO}
import fi.oph.kouta.service._
import fi.oph.kouta.servlet.OppilaitoksenOsaServlet
import fi.oph.kouta.util.TimeUtils
import fi.oph.kouta.{SqsInTransactionServiceIgnoringIndexing, TestData, TestOids}

import java.util.UUID

trait OppilaitoksenOsaFixture extends AccessControlSpec {
  this: KoutaIntegrationSpec =>

  val OppilaitoksenOsaPath = "/oppilaitoksen-osa"
  val mockOrganisaatioServiceClient = mock[OrganisaatioServiceClient]
  val mockOppilaitoksenOsaDao       = mock[OppilaitoksenOsaDAO]

  def oppilaitoksenOsaService: OppilaitoksenOsaService = {
    val organisaatioService               = new OrganisaatioServiceImpl(urlProperties.get, mockOrganisaatioServiceClient)
    val koodistoService                   = new KoodistoService(new KoodistoClient(urlProperties.get))
    val oppilaitosServiceValidation       = new OppilaitosServiceValidation(koodistoService)
    val oppilaitoksenOsaServiceValidation = new OppilaitoksenOsaServiceValidation(koodistoService, OppilaitosDAO)
    new OppilaitoksenOsaService(
      SqsInTransactionServiceIgnoringIndexing,
      MockS3ImageService,
      new AuditLog(MockAuditLogger),
      organisaatioService,
      mockOppijanumerorekisteriClient,
      mockKayttooikeusClient,
      oppilaitosServiceValidation,
      oppilaitoksenOsaServiceValidation
    )
  }

  def pistehistoriaService: PistehistoriaService = PistehistoriaService

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(new OppilaitoksenOsaServlet(oppilaitoksenOsaService), OppilaitoksenOsaPath)
  }

  def oppilaitoksenOsa = TestData.JulkaistuOppilaitoksenOsa.copy(oid = TestOids.randomOrganisaatioOid)

  def oppilaitoksenOsa(oid: String): OppilaitoksenOsa = {
    oppilaitoksenOsa.copy(oid = OrganisaatioOid(oid))
  }
  def oppilaitoksenOsa(oid: String, oppilaitosOid: String): OppilaitoksenOsa = {
    oppilaitoksenOsa.copy(oid = OrganisaatioOid(oid), oppilaitosOid = Some(OrganisaatioOid(oppilaitosOid)))
  }

  def oppilaitoksenOsa(oid: String, tila: Julkaisutila): OppilaitoksenOsa =
    oppilaitoksenOsa.copy(oid = OrganisaatioOid(oid), tila = tila)

  def oppilaitoksenOsa(oid: OrganisaatioOid,
                       oppilaitosOid: OrganisaatioOid,
                       tila: Julkaisutila,
                       organisaatioOid: OrganisaatioOid
                      ): OppilaitoksenOsa = {
    oppilaitoksenOsa.copy(oid = oid, oppilaitosOid = Some(oppilaitosOid), organisaatioOid = organisaatioOid, tila = tila)
  }

  def put(oppilaitoksenOsa: OppilaitoksenOsa): String = put(OppilaitoksenOsaPath, oppilaitoksenOsa, oid(_))
  def put(oppilaitoksenOsa: OppilaitoksenOsa, sessionId: UUID): String =
    put(OppilaitoksenOsaPath, oppilaitoksenOsa, sessionId, oid(_))

  def get(oid: String, expected: OppilaitoksenOsa): String =
    get(OppilaitoksenOsaPath, oid, expected.copy(modified = Some(readOppilaitoksenOsaModified(oid))))
  def get(oid: String, sessionId: UUID, expected: OppilaitoksenOsa): String =
    get(OppilaitoksenOsaPath, oid, sessionId, expected.copy(modified = Some(readOppilaitoksenOsaModified(oid))))

  def update(oppilaitoksenOsa: OppilaitoksenOsa, lastModified: String, expectedStatus: Int, sessionId: UUID): Unit =
    update(OppilaitoksenOsaPath, oppilaitoksenOsa, lastModified, sessionId, expectedStatus)
  def update(oppilaitoksenOsa: OppilaitoksenOsa, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit =
    update(OppilaitoksenOsaPath, oppilaitoksenOsa, lastModified, expectUpdate, sessionId)
  def update(oppilaitoksenOsa: OppilaitoksenOsa, lastModified: String, expectUpdate: Boolean): Unit =
    update(OppilaitoksenOsaPath, oppilaitoksenOsa, lastModified, expectUpdate)
  def update(oppilaitoksenOsa: OppilaitoksenOsa, lastModified: String): Unit =
    update(oppilaitoksenOsa, lastModified, true)

  def addToList(oppilaitoksenOsa: OppilaitoksenOsa) = {
    val oid      = put(oppilaitoksenOsa)
    val modified = readOppilaitoksenOsaModified(oid)
    OppilaitoksenOsaListItem(
      OrganisaatioOid(oid),
      oppilaitoksenOsa.oppilaitosOid.get,
      oppilaitoksenOsa.tila,
      oppilaitoksenOsa.organisaatioOid,
      oppilaitoksenOsa.muokkaaja,
      modified
    )
  }

  def readOppilaitoksenOsaModified(oid: String): Modified = readOppilaitoksenOsaModified(OrganisaatioOid(oid))
  def readOppilaitoksenOsaModified(oid: OrganisaatioOid): Modified =
    TimeUtils.instantToModifiedAt(db.runBlocking(OppilaitoksenOsaDAO.selectLastModified(oid)).get)
}
