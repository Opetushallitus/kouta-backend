package fi.oph.kouta.integration.fixture

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.mocks.{MockAuditLogger, MockS3Service}
import fi.oph.kouta.repository.{KoulutusDAO, KoulutusExtractors, SQLHelpers}
import fi.oph.kouta.service.KoulutusService
import fi.oph.kouta.servlet.KoulutusServlet
import fi.oph.kouta.util.TimeUtils
import fi.oph.kouta.{SqsInTransactionServiceIgnoringIndexing, TestData}
import org.scalactic.Equality

import scala.util.Try

trait KoulutusFixture extends KoulutusDbFixture {
  this: KoutaIntegrationSpec =>

  val KoulutusPath = "/koulutus"

  protected lazy val koulutusService: KoulutusService = new KoulutusService(SqsInTransactionServiceIgnoringIndexing, MockS3Service, new AuditLog(MockAuditLogger))

  addServlet(new KoulutusServlet(koulutusService), KoulutusPath)

  val koulutus = TestData.AmmKoulutus
  val yoKoulutus = TestData.YoKoulutus

  def koulutus(oid:String): Koulutus = koulutus.copy(oid = Some(KoulutusOid(oid)))
  def muokkaus(k: Koulutus): Koulutus = k.copy(nimi = k.nimi.map{case (k, v) => k -> (v + v) })
  def koulutus(oid:String, tila:Julkaisutila): Koulutus = koulutus.copy(oid = Some(KoulutusOid(oid)), tila = tila)
  def koulutus(julkinen:Boolean, organisaatioOid: OrganisaatioOid, tila:Julkaisutila):Koulutus =
    koulutus.copy(julkinen = julkinen, organisaatioOid = organisaatioOid, tila = tila)

  def put(koulutus:Koulutus):String = put(KoulutusPath, koulutus, oid(_))

  def put(koulutus: Koulutus, sessionId: UUID): String = put(KoulutusPath, koulutus, sessionId, oid(_))

  implicit val koulutusEquality: Equality[Koulutus] = (a: Koulutus, b: Any) => b match {
    case _:Koulutus => Equality.default[Koulutus].areEqual(
      a.copy(tarjoajat = a.tarjoajat.sorted),
      b.asInstanceOf[Koulutus].copy(tarjoajat = b.asInstanceOf[Koulutus].tarjoajat.sorted)
    )
    case _ => false
  }

  def get(oid: String, expected: Koulutus): String =
    get(KoulutusPath, oid, expected.copy(modified = Some(readKoulutusModified(oid))))

  def get(oid: String, sessionId: UUID, expected: Koulutus): String =
    get(KoulutusPath, oid, sessionId, expected.copy(modified = Some(readKoulutusModified(oid))))

  def update(koulutus: Koulutus, lastModified: String, sessionId: UUID, expectedStatus: Int): Unit =
    update(KoulutusPath, koulutus, lastModified, sessionId, expectedStatus)

  def update(koulutus: Koulutus, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit =
    update(KoulutusPath, koulutus, lastModified, expectUpdate, sessionId)

  def update(koulutus: Koulutus, lastModified: String, expectUpdate: Boolean): Unit =
    update(KoulutusPath, koulutus, lastModified, expectUpdate)

  def update(koulutus: Koulutus, lastModified: String): Unit =
    update(koulutus, lastModified, expectUpdate = true)

  def addToList(koulutus:Koulutus) = {
    val oid = put(koulutus)
    val modified = readKoulutusModified(oid)
    new KoulutusListItem(KoulutusOid(oid), koulutus.nimi, koulutus.tila, koulutus.tarjoajat, koulutus.organisaatioOid, koulutus.muokkaaja, modified)
  }

  def readKoulutusModified(oid: String): LocalDateTime = readKoulutusModified(KoulutusOid(oid))
  def readKoulutusModified(oid: KoulutusOid): LocalDateTime =
    TimeUtils.instantToModifiedAt(db.runBlocking(KoulutusDAO.selectLastModified(oid)).get)
}

trait KoulutusDbFixture extends KoulutusExtractors with SQLHelpers { this: KoutaIntegrationSpec =>

  import slick.dbio.DBIO
  import slick.jdbc.PostgresProfile.api._

  def setModifiedToPast(oid: String, duration: String): Try[Unit] = {
    db.runBlockingTransactionally(
      DBIO.seq(
        sqlu"""ALTER TABLE koulutukset DISABLE TRIGGER koulutukset_history""",
        sqlu"""ALTER TABLE koulutukset DISABLE TRIGGER set_temporal_columns_on_koulutukset_on_update""",
        sqlu"""ALTER TABLE koulutusten_tarjoajat DISABLE TRIGGER koulutusten_tarjoajat_history""",
        sqlu"""ALTER TABLE koulutusten_tarjoajat DISABLE TRIGGER set_temporal_columns_on_koulutusten_tarjoajat_on_update""",
        sqlu"""update koulutukset set system_time = tstzrange(now() - interval '#$duration', NULL::timestamp with time zone, '[)'::text) where oid = '#$oid'""",
        sqlu"""update koulutusten_tarjoajat set system_time = tstzrange(now() - interval '#$duration', NULL::timestamp with time zone, '[)'::text) where koulutus_oid = '#$oid'""",
        sqlu"""ALTER TABLE koulutukset ENABLE TRIGGER koulutukset_history""",
        sqlu"""ALTER TABLE koulutukset ENABLE TRIGGER set_temporal_columns_on_koulutukset_on_update""",
        sqlu"""ALTER TABLE koulutusten_tarjoajat ENABLE TRIGGER koulutusten_tarjoajat_history""",
        sqlu"""ALTER TABLE koulutusten_tarjoajat ENABLE TRIGGER set_temporal_columns_on_koulutusten_tarjoajat_on_update""",
      )
    )
  }
}
