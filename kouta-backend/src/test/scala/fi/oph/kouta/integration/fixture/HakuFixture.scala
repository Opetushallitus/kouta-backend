package fi.oph.kouta.integration.fixture

import fi.oph.kouta.SqsInTransactionServiceIgnoringIndexing
import fi.oph.kouta.TestData.JulkaistuHaku
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{EPerusteKoodiClient, KoodistoClient, LokalisointiClient, MockKoutaIndeksoijaClient}
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid._
import fi.oph.kouta.integration.{AccessControlSpec, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.{MockAuditLogger, MockOhjausparametritClient, MockS3ImageService}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, KoulutusDAO, SQLHelpers, SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.service.validation.AmmatillinenKoulutusServiceValidation
import fi.oph.kouta.service.{HakuService, HakuServiceValidation, HakukohdeService, HakukohdeServiceValidation, KeywordService, KoodistoService, KoulutusService, KoulutusServiceValidation, OrganisaatioServiceImpl, ToteutusService, ToteutusServiceValidation}
import fi.oph.kouta.servlet.HakuServlet
import fi.oph.kouta.util.TimeUtils

import java.util.UUID

trait HakuFixture extends SQLHelpers with AccessControlSpec {
  this: KoutaIntegrationSpec =>

  val HakuPath = "/haku"

  val ohjausparametritClient: MockOhjausparametritClient.type = MockOhjausparametritClient
  val auditLogger = new AuditLog(MockAuditLogger)

  def kService: KoulutusService = {
    val organisaatioService          = new OrganisaatioServiceImpl(urlProperties.get)
    val koodistoService              = new KoodistoService(new KoodistoClient(urlProperties.get))
    val ePerusteKoodiClient          = new EPerusteKoodiClient(urlProperties.get)
    val ammKoulutusServiceValidation = new AmmatillinenKoulutusServiceValidation(koodistoService, ePerusteKoodiClient)
    val koutaIndeksoijaClient        = new MockKoutaIndeksoijaClient
    val lokalisointiClient           = new LokalisointiClient(urlProperties.get)
    val keywordService               = new KeywordService(auditLogger, organisaatioService)

    val koulutusServiceValidation =
      new KoulutusServiceValidation(
        koodistoService,
        organisaatioService,
        ToteutusDAO,
        SorakuvausDAO,
        ammKoulutusServiceValidation
      )

    new KoulutusService(
      SqsInTransactionServiceIgnoringIndexing,
      MockS3ImageService,
      new AuditLog(MockAuditLogger),
      organisaatioService,
      mockOppijanumerorekisteriClient,
      mockKayttooikeusClient,
      koodistoService,
      koulutusServiceValidation,
      mockKoutaSearchClient,
      ePerusteKoodiClient,
      koutaIndeksoijaClient,
      lokalisointiClient,
      keywordService
    )
  }

  def hakuService: HakuService = {
    val organisaatioService   = new OrganisaatioServiceImpl(urlProperties.get)
    val koodistoService       = new KoodistoService(new KoodistoClient(urlProperties.get))
    val koutaIndeksoijaClient = new MockKoutaIndeksoijaClient
    val hakuServiceValidation = new HakuServiceValidation(koodistoService, mockHakemusPalveluClient, HakukohdeDAO, OrganisaatioServiceImpl)
    val lokalisointiClient = new LokalisointiClient(urlProperties.get)
    val mockEPerusteKoodiClient = mock[EPerusteKoodiClient]

    val toteutusServiceValidation = new ToteutusServiceValidation(
      koodistoService,
      organisaatioService,
      KoulutusDAO,
      HakukohdeDAO,
      SorakuvausDAO,
      ToteutusDAO,
      mockEPerusteKoodiClient
    )
    val hakukohdeServiceValidation = new HakukohdeServiceValidation(
      koodistoService,
      mockHakemusPalveluClient,
      organisaatioService,
      lokalisointiClient,
      HakukohdeDAO,
      HakuDAO
    )

    val hakukohdeService = new HakukohdeService(
      SqsInTransactionServiceCheckingRowExistsWhenIndexing,
      new AuditLog(MockAuditLogger),
      organisaatioService,
      lokalisointiClient,
      mockOppijanumerorekisteriClient,
      mockKayttooikeusClient,
      koodistoService,
      new ToteutusService(
        SqsInTransactionServiceCheckingRowExistsWhenIndexing,
        MockS3ImageService,
        auditLogger,
        new KeywordService(auditLogger, organisaatioService),
        organisaatioService,
        kService,
        lokalisointiClient,
        koodistoService,
        mockOppijanumerorekisteriClient,
        mockKayttooikeusClient,
        toteutusServiceValidation,
        koutaIndeksoijaClient
      ),
      hakukohdeServiceValidation,
      koutaIndeksoijaClient
    )
    new HakuService(
      SqsInTransactionServiceIgnoringIndexing,
      auditLogger,
      ohjausparametritClient,
      organisaatioService,
      mockOppijanumerorekisteriClient,
      mockKayttooikeusClient,
      hakuServiceValidation,
      koutaIndeksoijaClient,
      hakukohdeService
    )
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(new HakuServlet(hakuService), HakuPath)
  }

  val haku: Haku = JulkaistuHaku
  val yhteishakuWithoutAlkamiskausi: Haku = JulkaistuHaku.copy(
    hakutapaKoodiUri = Some("hakutapa_01#1"),
    metadata = Some(HakuMetadata(koulutuksenAlkamiskausi = None, isMuokkaajaOphVirkailija = Some(false)))
  )
  val jatkuvaHakuWithoutAlkamiskausi: Haku = JulkaistuHaku.copy(metadata =
    Some(HakuMetadata(koulutuksenAlkamiskausi = None, isMuokkaajaOphVirkailija = Some(false)))
  )

  def hakuWithAlkamisvuosi(haku: Haku, alkamisvuosi: String): Haku = haku.copy(metadata =
    Some(
      haku.metadata.get.copy(koulutuksenAlkamiskausi =
        Some(haku.metadata.get.koulutuksenAlkamiskausi.get.copy(koulutuksenAlkamisvuosi = Some(alkamisvuosi)))
      )
    )
  )

  def haku(oid: String): Haku                     = haku.copy(oid = Some(HakuOid(oid)))
  def haku(oid: String, tila: Julkaisutila): Haku = haku.copy(oid = Some(HakuOid(oid)), tila = tila)
  def haku(tila: Julkaisutila, organisaatioOid: OrganisaatioOid): Haku =
    haku.copy(organisaatioOid = organisaatioOid, tila = tila)

  def put(haku: Haku): String                  = put(HakuPath, haku, oid)
  def put(haku: Haku, sessionId: UUID): String = put(HakuPath, haku, sessionId, oid)
  def get(oid: String, expected: Haku): String =
    get(HakuPath, oid, expected.copy(modified = Some(readHakuModified(oid))))
  def get(oid: String, sessionId: UUID, expected: Haku): String =
    get(HakuPath, oid, sessionId, expected.copy(modified = Some(readHakuModified(oid))))
  def update(haku: Haku, lastModified: String, expectedStatus: Int, sessionId: UUID): Unit =
    update(HakuPath, haku, lastModified, sessionId, expectedStatus)
  def update(haku: Haku, lastModified: String, expectUpdate: Boolean, sessionId: UUID): Unit =
    update(HakuPath, haku, lastModified, expectUpdate, sessionId)
  def update(haku: Haku, lastModified: String, expectUpdate: Boolean): Unit =
    update(HakuPath, haku, lastModified, expectUpdate)
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

  def readHakuMuokkaaja(oid: String): String = {
    getStringColumnValue("haut", "muokkaaja", "oid", oid)
  }
  def readHakuModified(oid: String): Modified = readHakuModified(HakuOid(oid))
  def readHakuModified(oid: HakuOid): Modified =
    TimeUtils.instantToModifiedAt(db.runBlocking(HakuDAO.selectLastModified(oid)).get)
}
