package fi.oph.kouta.integration.fixture

import fi.oph.kouta.TestData._
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{EPerusteKoodiClient, KoodistoClient, LokalisointiClient, MockKoutaIndeksoijaClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.{AccessControlSpec, DefaultMocks, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.{MockAuditLogger, MockS3ImageService}
import fi.oph.kouta.repository.{HakukohdeDAO, KoulutusDAO, SQLHelpers, SorakuvausDAO, ToteutusDAO, ToteutusExtractors}
import fi.oph.kouta.service.{KeywordService, KoodistoService, OrganisaatioServiceImpl, ToteutusCopyResultObject, ToteutusService, ToteutusServiceValidation, ToteutusTilaChangeResultObject}
import fi.oph.kouta.servlet.ToteutusServlet
import fi.oph.kouta.util.TimeUtils
import fi.oph.kouta.{SqsInTransactionServiceIgnoringIndexing, TestData}
import org.scalactic.Equality

import java.util.UUID
import scala.util.Try

trait ToteutusFixture extends KoulutusFixture with ToteutusDbFixture with AccessControlSpec with DefaultMocks {
  this: KoutaIntegrationSpec =>

  val ToteutusPath     = "/toteutus"
  val ToteutusCopyPath = "/toteutus/copy"
  val ToteutusChangeTilaPath = s"/toteutus/tila/"
  val mockEPerusteKoodiClient: EPerusteKoodiClient = mock[EPerusteKoodiClient]

  protected lazy val auditLog = new AuditLog(MockAuditLogger)

  def toteutusService: ToteutusService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    val lokalisointiClient  = new LokalisointiClient(urlProperties.get)
    val koutaIndeksoijaClient = new MockKoutaIndeksoijaClient
    val koodistoService       = new KoodistoService(new KoodistoClient(urlProperties.get))

    val toteutusServiceValidation = new ToteutusServiceValidation(
      koodistoService,
      organisaatioService,
      KoulutusDAO,
      HakukohdeDAO,
      SorakuvausDAO,
      ToteutusDAO,
      mockEPerusteKoodiClient
    )
    new ToteutusService(
      SqsInTransactionServiceIgnoringIndexing,
      MockS3ImageService,
      auditLog,
      new KeywordService(auditLog, organisaatioService),
      organisaatioService,
      koulutusService,
      lokalisointiClient,
      koodistoService,
      mockOppijanumerorekisteriClient,
      mockKayttooikeusClient,
      toteutusServiceValidation,
      koutaIndeksoijaClient
    )
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(new ToteutusServlet(toteutusService), ToteutusPath)
  }

  val opetus: Opetus                             = ToteutuksenOpetus
  val ammMetatieto: AmmatillinenToteutusMetadata = AmmToteutuksenMetatieto
  val toteutus: Toteutus                         = JulkaistuAmmToteutus
  val ammTutkinnonOsaToteutus: Toteutus          = TestData.AmmTutkinnonOsaToteutus
  val ammOsaamisalaToteutus: Toteutus            = TestData.AmmOsaamisalaToteutus
  val ammTutkinnonOsaToteutusAtaru: Toteutus =
    TestData.AmmTutkinnonOsaToteutus.copy(metadata = Some(TestData.AmmTutkinnonOsaToteutusMetadataHakemuspalvelu))
  val ammOsaamisalaToteutusAtaru: Toteutus =
    TestData.AmmOsaamisalaToteutus.copy(metadata = Some(TestData.AmmOsaamisalaToteutusMetadataHakemuspalvelu))
  val vapaaSivistystyoMuuToteutus: Toteutus = TestData.VapaaSivistystyoMuuToteutus
  val vapaaSivistystyoMuuToteutusAtaru: Toteutus = TestData.VapaaSivistystyoMuuToteutus.copy(metadata =
    Some(TestData.VapaaSivistystyoMuuToteutusHakemuspalveluMetatieto)
  )
  val tuvaToteutus: Toteutus = TestData.TuvaToteutus

  def toteutus(koulutusOid: String): Toteutus = toteutus.copy(koulutusOid = KoulutusOid(koulutusOid))
  def toteutus(oid: String, koulutusOid: String): Toteutus =
    toteutus.copy(oid = Some(ToteutusOid(oid)), koulutusOid = KoulutusOid(koulutusOid))
  def toteutus(oid: String, koulutusOid: String, tila: Julkaisutila): Toteutus =
    toteutus.copy(oid = Some(ToteutusOid(oid)), koulutusOid = KoulutusOid(koulutusOid), tila = tila)
  def toteutus(koulutusOid: String, tila: Julkaisutila, organisaatioOid: OrganisaatioOid): Toteutus =
    toteutus.copy(koulutusOid = KoulutusOid(koulutusOid), organisaatioOid = organisaatioOid, tila = tila)
  def tuvaToteutus(koulutusOid: String): Toteutus = tuvaToteutus.copy(koulutusOid = KoulutusOid(koulutusOid))
  def lukioToteutus(koulutusOid: String): Toteutus =
    LukioToteutus.copy(koulutusOid = KoulutusOid(koulutusOid), nimi = Map())
  def toteutus(oid: String, koulutusOid: String, externalId: String): Toteutus = toteutus.copy(
    oid = Some(ToteutusOid(oid)),
    koulutusOid = KoulutusOid(koulutusOid),
    tila = Tallennettu,
    externalId = Some(externalId)
  )

  implicit val toteutusEquality: Equality[Toteutus] = (a: Toteutus, b: Any) =>
    b match {
      case _: Toteutus =>
        Equality
          .default[Toteutus]
          .areEqual(
            a.copy(tarjoajat = a.tarjoajat.sorted),
            b.asInstanceOf[Toteutus].copy(tarjoajat = b.asInstanceOf[Toteutus].tarjoajat.sorted)
          )
      case _ => false
    }

  def put(toteutus: Toteutus): String                  = put(ToteutusPath, toteutus, oid(_))
  def put(toteutus: Toteutus, sessionId: UUID): String = put(ToteutusPath, toteutus, sessionId, oid(_))
  def put(toteutukset: List[String]): List[ToteutusCopyResultObject] =
    put(ToteutusCopyPath, toteutukset, listResponse[ToteutusCopyResultObject])

  def get(oid: String, expected: Toteutus): String =
    get(ToteutusPath, oid, expected.copy(modified = Some(readToteutusModified(oid))))

  def get(oid: String, sessionId: UUID, expected: Toteutus): String =
    get(ToteutusPath, oid, sessionId, expected.copy(modified = Some(readToteutusModified(oid))))

  def update(toteutus: Toteutus, lastModified: String, expectedStatus: Int, sessionId: UUID): Unit =
    update(ToteutusPath, toteutus, lastModified, sessionId, expectedStatus)
  def update(toteutus: Toteutus, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit =
    update(ToteutusPath, toteutus, lastModified, expectUpdate, sessionId)
  def update(toteutus: Toteutus, lastModified: String, expectUpdate: Boolean): Unit =
    update(ToteutusPath, toteutus, lastModified, expectUpdate)
  def update(toteutus: Toteutus, lastModified: String): Unit = update(toteutus, lastModified, expectUpdate = true)

  def addToList(toteutus: Toteutus, johtaaTutkintoon: Boolean = true): ToteutusListItem = {
    val oid      = put(toteutus)
    val modified = readToteutusModified(oid)
    ToteutusListItem(
      ToteutusOid(oid),
      toteutus.koulutusOid,
      johtaaTutkintoon,
      toteutus.nimi,
      toteutus.tila,
      toteutus.tarjoajat,
      toteutus.organisaatioOid,
      toteutus.muokkaaja,
      modified
    )
  }

  def readToteutusMuokkaaja(oid: String): String = {
    getStringColumnValue("toteutukset", "muokkaaja", "oid", oid)
  }
  def readToteutusModified(oid: String): Modified = readToteutusModified(ToteutusOid(oid))
  def readToteutusModified(oid: ToteutusOid): Modified =
    TimeUtils.instantToModifiedAt(db.runBlocking(ToteutusDAO.selectLastModified(oid)).get)

  def changeToteutusTila(toteutukset: List[String], tila: String, lastModified: String, sessionId: UUID, expectedStatus: Int): List[ToteutusTilaChangeResultObject] =
    post(s"$ToteutusChangeTilaPath$tila", toteutukset, lastModified, sessionId, expectedStatus, listResponse[ToteutusTilaChangeResultObject])
}

trait ToteutusDbFixture extends ToteutusExtractors with SQLHelpers {
  this: KoutaIntegrationSpec =>

  import slick.dbio.DBIO
  import slick.jdbc.PostgresProfile.api._
  import scala.concurrent.ExecutionContext.Implicits.global
  import java.time.Instant

  private def getPutActions(toteutus: Toteutus): DBIO[Toteutus] =
    for {
      oid <- ToteutusDAO.insertToteutus(toteutus)
      _   <- ToteutusDAO.insertToteutuksenTarjoajat(toteutus.withOid(oid))
    } yield toteutus.withOid(oid)

  def insertToteutus(t: Toteutus) = db
    .runBlockingTransactionally(for {
      t <- getPutActions(t)
      n <- sql"""select now()::timestamptz""".as[Instant].head
    } yield (t, n))
    .get

  def updateToteutuksenTarjoajat(t: Toteutus) = db
    .runBlockingTransactionally(for {
      _ <- ToteutusDAO.updateToteutuksenTarjoajat(t)
      n <- sql"""select now()::timestamptz""".as[Instant].head
    } yield n)
    .get

  def getToteutusHistorySize(t: Toteutus): Int = getTableHistorySize("toteutukset", "oid", t.oid.get.toString)

  def getToteutusTarjoajatHistorySize(t: Toteutus): Int =
    getTableHistorySize("toteutusten_tarjoajat", "toteutus_oid", t.oid.get.toString)
}
