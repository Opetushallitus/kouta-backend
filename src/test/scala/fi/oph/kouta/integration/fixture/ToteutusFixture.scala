package fi.oph.kouta.integration.fixture

import java.util.UUID

import fi.oph.kouta.SqsInTransactionServiceIgnoringIndexing
import fi.oph.kouta.TestData._
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.mocks.{MockAuditLogger, MockS3Service}
import fi.oph.kouta.service.{KeywordService, ToteutusService}
import fi.oph.kouta.servlet.ToteutusServlet
import org.scalactic.Equality

trait ToteutusFixture { this: KoutaIntegrationSpec =>

  val ToteutusPath = "/toteutus"

  private lazy val auditLog = new AuditLog(MockAuditLogger)
  protected lazy val toteutusService: ToteutusService =
    new ToteutusService(SqsInTransactionServiceIgnoringIndexing, MockS3Service, auditLog, new KeywordService(auditLog))

  addServlet(new ToteutusServlet(toteutusService), ToteutusPath)

  val opetus = ToteutuksenOpetus
  val ammMetatieto = AmmToteutuksenMetatieto
  val toteutus = JulkaistuAmmToteutus

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
    get(ToteutusPath, oid, expected.copy(modified = Some(readModifiedByOid(oid, "toteutukset"))))

  def get(oid: String, sessionId: UUID, expected: Toteutus): String =
    get(ToteutusPath, oid, sessionId, expected.copy(modified = Some(readModifiedByOid(oid, "toteutukset"))))

  def update(toteutus:Toteutus, lastModified:String, expectedStatus: Int, sessionId: UUID): Unit = update(ToteutusPath, toteutus, lastModified, sessionId, expectedStatus)
  def update(toteutus:Toteutus, lastModified:String, expectUpdate:Boolean, sessionId: UUID): Unit = update(ToteutusPath, toteutus, lastModified, expectUpdate, sessionId)
  def update(toteutus:Toteutus, lastModified:String, expectUpdate:Boolean): Unit = update(ToteutusPath, toteutus, lastModified, expectUpdate)
  def update(toteutus:Toteutus, lastModified:String): Unit = update(toteutus, lastModified, true)

  def addToList(toteutus:Toteutus) = {
    val oid = put(toteutus)
    val modified = readModifiedByOid(oid, "toteutukset")
    new ToteutusListItem(ToteutusOid(oid), toteutus.koulutusOid, toteutus.nimi, toteutus.tila,
      toteutus.tarjoajat, toteutus.organisaatioOid, toteutus.muokkaaja, modified)
  }
}
