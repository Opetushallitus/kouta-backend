package fi.oph.kouta.integration.fixture

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{EPerusteKoodiClient, KoodistoClient, KoulutusKoodiClient, KoutaSearchClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.{AccessControlSpec, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.{MockAuditLogger, MockS3ImageService}
import fi.oph.kouta.repository.{KoulutusDAO, KoulutusExtractors, SQLHelpers, SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.service.{KoulutusService, KoulutusServiceValidation, OrganisaatioServiceImpl}
import fi.oph.kouta.servlet.KoulutusServlet
import fi.oph.kouta.util.TimeUtils
import fi.oph.kouta.validation.ValidationError
import fi.oph.kouta.{SqsInTransactionServiceIgnoringIndexing, TestData}
import org.scalactic.Equality

import java.util.UUID
import scala.util.Try

trait KoulutusFixture extends KoulutusDbFixture with AccessControlSpec {
  this: KoutaIntegrationSpec =>

  val KoulutusPath = "/koulutus"

  def koulutusService: KoulutusService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    val koodistoClient = new KoulutusKoodiClient(urlProperties.get)
    val ePerusteKoodiClient = new EPerusteKoodiClient(urlProperties.get)
    val koulutusServiceValidation =
      new KoulutusServiceValidation(koodistoClient, ePerusteKoodiClient, organisaatioService, ToteutusDAO, SorakuvausDAO)

    new KoulutusService(
      SqsInTransactionServiceIgnoringIndexing,
      MockS3ImageService,
      new AuditLog(MockAuditLogger),
      organisaatioService,
      mockOppijanumerorekisteriClient,
      mockKayttooikeusClient,
      koodistoClient,
      koulutusServiceValidation,
      mockKoutaSearchClient,
      ePerusteKoodiClient
    )
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(new KoulutusServlet(koulutusService), KoulutusPath)
  }

  val koulutus: Koulutus                    = TestData.AmmKoulutus
  val yoKoulutus: Koulutus                  = TestData.YoKoulutus
  val ammOsaamisalaKoulutus: Koulutus       = TestData.AmmOsaamisalaKoulutus
  val ammTutkinnonOsaKoulutus: Koulutus     = TestData.AmmTutkinnonOsaKoulutus
  val vapaaSivistystyoMuuKoulutus: Koulutus = TestData.VapaaSivistystyoMuuKoulutus
  val tuvaKoulutus: Koulutus                = TestData.TuvaKoulutus

  def koulutus(oid: String): Koulutus                     = koulutus.copy(oid = Some(KoulutusOid(oid)))
  def muokkaus(k: Koulutus): Koulutus                     = k.copy(esikatselu = if (k.esikatselu == true) false else true)
  def koulutus(oid: String, tila: Julkaisutila): Koulutus = koulutus.copy(oid = Some(KoulutusOid(oid)), tila = tila)
  def koulutus(
      julkinen: Boolean,
      organisaatioOid: OrganisaatioOid,
      tila: Julkaisutila,
      sorakuvausId: Option[UUID] = None
  ): Koulutus =
    koulutus.copy(julkinen = julkinen, organisaatioOid = organisaatioOid, tila = tila, sorakuvausId = sorakuvausId)

  def put(koulutus: Koulutus): String = put(KoulutusPath, koulutus, oid(_))

  def put(koulutus: Koulutus, sessionId: UUID): String = put(KoulutusPath, koulutus, sessionId, oid(_))

  def put(koulutus: Koulutus, errors: List[ValidationError]): Unit =
    put(KoulutusPath, koulutus, ophSession, 400, errors)

  implicit val koulutusEquality: Equality[Koulutus] = (a: Koulutus, b: Any) =>
    b match {
      case _: Koulutus =>
        Equality
          .default[Koulutus]
          .areEqual(
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

  def addToList(koulutus: Koulutus): KoulutusListItem = {
    val oid = if (Koulutustyyppi.isKoulutusSaveAllowedOnlyForOph(koulutus.koulutustyyppi)) {
      put(koulutus, ophSession)
    } else {
      put(koulutus)
    }

    val modified = readKoulutusModified(oid)
    KoulutusListItem(
      KoulutusOid(oid),
      koulutus.nimi,
      koulutus.tila,
      koulutus.tarjoajat,
      koulutus.organisaatioOid,
      koulutus.muokkaaja,
      modified
    )
  }

  def readKoulutusModified(oid: String): Modified = readKoulutusModified(KoulutusOid(oid))
  def readKoulutusModified(oid: KoulutusOid): Modified =
    TimeUtils.instantToModifiedAt(db.runBlocking(KoulutusDAO.selectLastModified(oid)).get)
}

trait KoulutusDbFixture extends KoulutusExtractors with SQLHelpers {
  this: KoutaIntegrationSpec =>

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
        sqlu"""ALTER TABLE koulutusten_tarjoajat ENABLE TRIGGER set_temporal_columns_on_koulutusten_tarjoajat_on_update"""
      )
    )
  }
}
