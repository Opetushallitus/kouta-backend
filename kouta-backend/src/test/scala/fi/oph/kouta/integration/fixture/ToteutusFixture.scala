package fi.oph.kouta.integration.fixture

import java.util.UUID

import fi.oph.kouta.{SqsInTransactionServiceIgnoringIndexing, TestData}
import fi.oph.kouta.TestData._
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.{AccessControlSpec, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.{MockAuditLogger, MockS3ImageService}
import fi.oph.kouta.repository.ToteutusDAO
import fi.oph.kouta.service.{KeywordService, OrganisaatioServiceImpl, ToteutusService}
import fi.oph.kouta.servlet.ToteutusServlet
import fi.oph.kouta.util.TimeUtils
import org.scalactic.Equality

trait ToteutusFixture extends KoutaIntegrationSpec with AccessControlSpec {
  this: KoulutusFixture =>

  val ToteutusPath = "/toteutus"

  protected lazy val auditLog = new AuditLog(MockAuditLogger)

  def toteutusService: ToteutusService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    new ToteutusService(SqsInTransactionServiceIgnoringIndexing, MockS3ImageService, auditLog,
      new KeywordService(auditLog, organisaatioService), organisaatioService, koulutusService)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(new ToteutusServlet(toteutusService), ToteutusPath)
  }

  val opetus: Opetus = ToteutuksenOpetus
  val ammMetatieto: AmmatillinenToteutusMetadata = AmmToteutuksenMetatieto
  val toteutus: Toteutus = JulkaistuAmmToteutus
  val ammTutkinnonOsaToteutus: Toteutus = TestData.AmmTutkinnonOsaToteutus
  val ammOsaamisalaToteutus: Toteutus = TestData.AmmOsaamisalaToteutus
  val ammTutkinnonOsaToteutusAtaru: Toteutus = TestData.AmmTutkinnonOsaToteutus.copy(metadata = Some(TestData.AmmTutkinnonOsaToteutusMetadataHakemuspalvelu))
  val ammOsaamisalaToteutusAtaru: Toteutus = TestData.AmmOsaamisalaToteutus.copy(metadata = Some(TestData.AmmOsaamisalaToteutusMetadataHakemuspalvelu))

  def toteutus(koulutusOid:String): Toteutus = toteutus.copy(koulutusOid = KoulutusOid(koulutusOid))
  def toteutus(oid:String, koulutusOid:String): Toteutus = toteutus.copy(oid = Some(ToteutusOid(oid)), koulutusOid = KoulutusOid(koulutusOid))
  def toteutus(oid:String, koulutusOid:String, tila:Julkaisutila): Toteutus = toteutus.copy(oid = Some(ToteutusOid(oid)), koulutusOid = KoulutusOid(koulutusOid), tila = tila)
  def toteutus(koulutusOid: String, tila: Julkaisutila, organisaatioOid: OrganisaatioOid): Toteutus =
    toteutus.copy(koulutusOid = KoulutusOid(koulutusOid), organisaatioOid = organisaatioOid, tila = tila)

  implicit val toteutusEquality: Equality[Toteutus] = (a: Toteutus, b: Any) => b match {
    case _:Toteutus => Equality.default[Toteutus].areEqual(
      a.copy(tarjoajat = a.tarjoajat.sorted),
      b.asInstanceOf[Toteutus].copy(tarjoajat = b.asInstanceOf[Toteutus].tarjoajat.sorted)
    )
    case _ => false
  }

  def put(toteutus:Toteutus):String = put(ToteutusPath, toteutus, oid(_))
  def put(toteutus:Toteutus, sessionId: UUID):String = put(ToteutusPath, toteutus, sessionId, oid(_))

  def get(oid: String, expected: Toteutus): String =
    get(ToteutusPath, oid, expected.copy(modified = Some(readToteutusModified(oid))))

  def get(oid: String, sessionId: UUID, expected: Toteutus): String =
    get(ToteutusPath, oid, sessionId, expected.copy(modified = Some(readToteutusModified(oid))))

  def update(toteutus:Toteutus, lastModified:String, expectedStatus: Int, sessionId: UUID): Unit = update(ToteutusPath, toteutus, lastModified, sessionId, expectedStatus)
  def update(toteutus:Toteutus, lastModified:String, expectUpdate:Boolean, sessionId: UUID): Unit = update(ToteutusPath, toteutus, lastModified, expectUpdate, sessionId)
  def update(toteutus:Toteutus, lastModified:String, expectUpdate:Boolean): Unit = update(ToteutusPath, toteutus, lastModified, expectUpdate)
  def update(toteutus:Toteutus, lastModified:String): Unit = update(toteutus, lastModified, expectUpdate = true)

  def addToList(toteutus:Toteutus): ToteutusListItem = {
    val oid = put(toteutus)
    val modified = readToteutusModified(oid)
    ToteutusListItem(ToteutusOid(oid), toteutus.koulutusOid, toteutus.nimi, toteutus.tila,
      toteutus.tarjoajat, toteutus.organisaatioOid, toteutus.muokkaaja, modified)
  }

  def readToteutusModified(oid: String): Modified = readToteutusModified(ToteutusOid(oid))
  def readToteutusModified(oid: ToteutusOid): Modified =
    TimeUtils.instantToModifiedAt(db.runBlocking(ToteutusDAO.selectLastModified(oid)).get)
}
