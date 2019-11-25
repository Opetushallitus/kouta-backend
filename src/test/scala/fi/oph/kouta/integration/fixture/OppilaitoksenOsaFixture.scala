package fi.oph.kouta.integration.fixture

import java.util.UUID

import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.domain.{Julkaisutila, OppilaitoksenOsa, OppilaitoksenOsaListItem}
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.mocks.MockS3Service
import fi.oph.kouta.service.OppilaitoksenOsaService
import fi.oph.kouta.servlet.OppilaitoksenOsaServlet
import fi.oph.kouta.{SqsInTransactionServiceIgnoringIndexing, TestData}

import scala.util.Random

trait OppilaitoksenOsaFixture { this: KoutaIntegrationSpec =>

  val OppilaitoksenOsaPath = "/oppilaitoksen-osa"

  protected lazy val oppilaitoksenOsaService: OppilaitoksenOsaService = new OppilaitoksenOsaService(SqsInTransactionServiceIgnoringIndexing, MockS3Service)

  addServlet(new OppilaitoksenOsaServlet(oppilaitoksenOsaService), OppilaitoksenOsaPath)

  private lazy val random = new Random()

  def oppilaitoksenOsa = TestData.JulkaistuOppilaitoksenOsa.copy(oid = OrganisaatioOid(s"1.2.246.562.10.000${random.nextInt()}"))

  def oppilaitoksenOsa(oppilaitosOid: String): OppilaitoksenOsa = oppilaitoksenOsa.copy(oppilaitosOid = OrganisaatioOid(oppilaitosOid))
  def oppilaitoksenOsa(oid: String, oppilaitosOid: String): OppilaitoksenOsa = oppilaitoksenOsa.copy(oid = OrganisaatioOid(oid), oppilaitosOid = OrganisaatioOid(oppilaitosOid))
  def oppilaitoksenOsa(oid: String, oppilaitosOid: String, tila: Julkaisutila): OppilaitoksenOsa = oppilaitoksenOsa.copy(oid = OrganisaatioOid(oid), tila = tila, oppilaitosOid = OrganisaatioOid(oppilaitosOid))

  def oppilaitoksenOsa(oppilaitosOid: OrganisaatioOid, tila: Julkaisutila, organisaatioOid: OrganisaatioOid): OppilaitoksenOsa =
    oppilaitoksenOsa.copy(oppilaitosOid = oppilaitosOid, organisaatioOid = organisaatioOid, tila = tila)

  def put(oppilaitoksenOsa: OppilaitoksenOsa): String = put(OppilaitoksenOsaPath, oppilaitoksenOsa, oid(_))
  def put(oppilaitoksenOsa: OppilaitoksenOsa, sessionId: UUID): String = put(OppilaitoksenOsaPath, oppilaitoksenOsa, sessionId, oid(_))

  def get(oid: String, expected: OppilaitoksenOsa): String = get(OppilaitoksenOsaPath, oid, expected.copy(modified = Some(readModifiedByOid(oid, "oppilaitosten_osat"))))
  def get(oid: String, sessionId: UUID, expected: OppilaitoksenOsa): String = get(OppilaitoksenOsaPath, oid, sessionId, expected.copy(modified = Some(readModifiedByOid(oid, "oppilaitosten_osat"))))

  def update(oppilaitoksenOsa: OppilaitoksenOsa, lastModified: String, expectedStatus: Int, sessionId: UUID): Unit = update(OppilaitoksenOsaPath, oppilaitoksenOsa, lastModified, sessionId, expectedStatus)
  def update(oppilaitoksenOsa: OppilaitoksenOsa, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit = update(OppilaitoksenOsaPath, oppilaitoksenOsa, lastModified, expectUpdate, sessionId)
  def update(oppilaitoksenOsa: OppilaitoksenOsa, lastModified: String, expectUpdate: Boolean): Unit = update(OppilaitoksenOsaPath, oppilaitoksenOsa, lastModified, expectUpdate)
  def update(oppilaitoksenOsa: OppilaitoksenOsa, lastModified: String): Unit = update(oppilaitoksenOsa, lastModified, true)

  def addToList(oppilaitoksenOsa: OppilaitoksenOsa) = {
    val oid = put(oppilaitoksenOsa)
    val modified = readModifiedByOid(oid, "oppilaitosten_osat")
    OppilaitoksenOsaListItem(OrganisaatioOid(oid), oppilaitoksenOsa.oppilaitosOid, oppilaitoksenOsa.tila,
      oppilaitoksenOsa.organisaatioOid, oppilaitoksenOsa.muokkaaja, modified)
  }
}
