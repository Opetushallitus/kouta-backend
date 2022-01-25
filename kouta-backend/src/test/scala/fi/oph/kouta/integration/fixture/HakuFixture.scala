package fi.oph.kouta.integration.fixture

import java.util.UUID
import fi.oph.kouta.SqsInTransactionServiceIgnoringIndexing
import fi.oph.kouta.TestData.JulkaistuHaku
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.{AccessControlSpec, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.{MockAuditLogger, MockOhjausparametritClient, MockOppijanumerorekisteriClient}
import fi.oph.kouta.repository.{HakuDAO, SQLHelpers}
import fi.oph.kouta.service.{HakuService, OrganisaatioServiceImpl}
import fi.oph.kouta.servlet.HakuServlet
import fi.oph.kouta.util.TimeUtils

trait HakuFixture extends SQLHelpers with KoutaIntegrationSpec with AccessControlSpec {

  val HakuPath = "/haku"

  val ohjausparametritClient: MockOhjausparametritClient.type = MockOhjausparametritClient

  def hakuService: HakuService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    new HakuService(SqsInTransactionServiceIgnoringIndexing, new AuditLog(MockAuditLogger), ohjausparametritClient, organisaatioService, oppijanumerorekisteriClient)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(new HakuServlet(hakuService), HakuPath)
  }

  val haku: Haku = JulkaistuHaku
  val yhteishakuWithoutAlkamiskausi: Haku = JulkaistuHaku.copy(
    hakutapaKoodiUri = Some("hakutapa_01#1"),
    metadata = Some(HakuMetadata(koulutuksenAlkamiskausi = None)))
  val jatkuvaHakuWithoutAlkamiskausi: Haku = JulkaistuHaku.copy(metadata = Some(HakuMetadata(koulutuksenAlkamiskausi = None)))

  def hakuWithAlkamisvuosi(haku: Haku, alkamisvuosi: String): Haku = haku.copy(metadata = Some(
    haku.metadata.get.copy(koulutuksenAlkamiskausi = Some(
      haku.metadata.get.koulutuksenAlkamiskausi.get.copy(
        koulutuksenAlkamisvuosi = Some(alkamisvuosi))))))

  def haku(oid: String): Haku = haku.copy(oid = Some(HakuOid(oid)))
  def haku(oid: String, tila: Julkaisutila): Haku = haku.copy(oid = Some(HakuOid(oid)), tila = tila)
  def haku(tila: Julkaisutila, organisaatioOid: OrganisaatioOid): Haku =
    haku.copy(organisaatioOid = organisaatioOid, tila = tila)

  def put(haku: Haku): String = put(HakuPath, haku, oid)
  def put(haku: Haku, sessionId: UUID): String = put(HakuPath, haku, sessionId, oid)
  def get(oid: String, expected: Haku): String = get(HakuPath, oid, expected.copy(modified = Some(readHakuModified(oid))))
  def get(oid: String, sessionId: UUID, expected: Haku): String = get(HakuPath, oid, sessionId, expected.copy(modified = Some(readHakuModified(oid))))
  def update(haku: Haku, lastModified: String, expectedStatus: Int, sessionId: UUID): Unit = update(HakuPath, haku, lastModified, sessionId, expectedStatus)
  def update(haku: Haku, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit = update(HakuPath, haku, lastModified, expectUpdate, sessionId)
  def update(haku: Haku, lastModified: String, expectUpdate: Boolean): Unit = update(HakuPath, haku, lastModified, expectUpdate)
  def update(haku: Haku, lastModified: String): Unit = update(haku, lastModified, expectUpdate = true)

  def addToList(haku: Haku): HakuListItem = {
    val oid = if (haku.hakutapaKoodiUri.get.startsWith("hakutapa_01")) {
      put(haku, ophSession)
    } else {
      put(haku)
    }
    val modified = readHakuModified(oid)
    HakuListItem(HakuOid(oid), haku.nimi, haku.tila, haku.organisaatioOid, haku.muokkaaja, modified)
  }

  def readHakuModified(oid: String): Modified = readHakuModified(HakuOid(oid))
  def readHakuModified(oid: HakuOid): Modified =
    TimeUtils.instantToModifiedAt(db.runBlocking(HakuDAO.selectLastModified(oid)).get)
}
