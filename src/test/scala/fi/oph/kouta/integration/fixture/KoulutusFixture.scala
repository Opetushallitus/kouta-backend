package fi.oph.kouta.integration.fixture

import java.util.UUID

import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.repository.{KoulutusExtractors, SQLHelpers}
import fi.oph.kouta.service.KoulutusService
import fi.oph.kouta.servlet.KoulutusServlet
import fi.oph.kouta.{SqsInTransactionServiceIgnoringIndexing, TestData}

import scala.util.Try

object KoulutusServiceIgnoringIndexing extends KoulutusService(SqsInTransactionServiceIgnoringIndexing, MockS3Service)

trait KoulutusFixture extends KoulutusDbFixture {
  this: KoutaIntegrationSpec =>

  val KoulutusPath = "/koulutus"

  protected lazy val koulutusService: KoulutusService = KoulutusServiceIgnoringIndexing

  addServlet(new KoulutusServlet(koulutusService), KoulutusPath)

  val koulutus = TestData.AmmKoulutus
  val yoKoulutus = TestData.YoKoulutus

  def koulutus(oid:String): Koulutus = koulutus.copy(oid = Some(KoulutusOid(oid)))
  def koulutus(oid:String, tila:Julkaisutila): Koulutus = koulutus.copy(oid = Some(KoulutusOid(oid)), tila = tila)
  def koulutus(julkinen:Boolean, organisaatioOid: OrganisaatioOid, tila:Julkaisutila):Koulutus =
    koulutus.copy(julkinen = julkinen, organisaatioOid = organisaatioOid, tila = tila)

  def put(koulutus:Koulutus):String = put(KoulutusPath, koulutus, oid(_))

  def put(koulutus: Koulutus, sessionId: UUID): String = put(KoulutusPath, koulutus, sessionId, oid(_))

  def get(oid: String, expected: Koulutus): String =
    get(KoulutusPath, oid, expected.copy(modified = Some(readModifiedByOid(oid, "koulutukset"))))

  def get(oid: String, sessionId: UUID, expected: Koulutus): String =
    get(KoulutusPath, oid, sessionId, expected.copy(modified = Some(readModifiedByOid(oid, "koulutukset"))))

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
    val modified = readModifiedByOid(oid, "koulutukset")
    new KoulutusListItem(KoulutusOid(oid), koulutus.nimi, koulutus.tila, koulutus.tarjoajat, koulutus.organisaatioOid, koulutus.muokkaaja, modified)
  }
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
