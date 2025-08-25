package fi.oph.kouta.integration.fixture

import fi.oph.kouta.SqsInTransactionServiceIgnoringIndexing
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{EPerusteKoodiClient, KoodistoClient, LokalisointiClient, MockKoutaIndeksoijaClient}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.integration.{AccessControlSpec, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.{MockAuditLogger, MockOhjausparametritClient, MockS3ImageService}
import fi.oph.kouta.repository._
import fi.oph.kouta.service._
import fi.oph.kouta.service.validation.AmmatillinenKoulutusServiceValidation

trait IndexingFixture
    extends KoulutusFixtureWithIndexing
    with HakuFixtureWithIndexing
    with ToteutusFixtureWithIndexing
    with ValintaperusteFixtureWithIndexing
    with HakukohdeFixtureWithIndexing
    with SorakuvausFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>
}

trait HakuFixtureWithIndexing extends HakuFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>

  override def hakuService = {
    val organisaatioService   = new OrganisaatioServiceImpl(urlProperties.get)
    val koodistoService       = new KoodistoService(new KoodistoClient(urlProperties.get))
    val koutaIndeksoijaClient = new MockKoutaIndeksoijaClient
    val hakuServiceValidation =
      new HakuServiceValidation(koodistoService, mockHakemusPalveluClient, HakukohdeDAO, OrganisaatioServiceImpl)
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
      SqsInTransactionService,
      new AuditLog(MockAuditLogger),
      MockOhjausparametritClient,
      organisaatioService,
      mockOppijanumerorekisteriClient,
      mockKayttooikeusClient,
      hakuServiceValidation,
      koutaIndeksoijaClient,
      hakukohdeService
    )
  }
}

trait KoulutusFixtureWithIndexing extends KoulutusFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>
  override protected lazy val auditLog = new AuditLog(MockAuditLogger)

  override def koulutusService = {
    val organisaatioService          = new OrganisaatioServiceImpl(urlProperties.get)
    val koodistoService              = new KoodistoService(new KoodistoClient(urlProperties.get))
    val ePerusteKoodiClient          = new EPerusteKoodiClient(urlProperties.get)
    val ammKoulutusServiceValidation = new AmmatillinenKoulutusServiceValidation(koodistoService, ePerusteKoodiClient)
    val koutaIndeksoijaClient        = new MockKoutaIndeksoijaClient
    val lokalisointiClient           = new LokalisointiClient(urlProperties.get)
    val keywordService               = new KeywordService(auditLog, organisaatioService)

    val koulutusServiceValidation =
      new KoulutusServiceValidation(
        koodistoService,
        organisaatioService,
        ToteutusDAO,
        SorakuvausDAO,
        ammKoulutusServiceValidation
      )

    new KoulutusService(
      SqsInTransactionService,
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
}

trait ToteutusFixtureWithIndexing extends ToteutusFixture {
  this: KoutaIntegrationSpec with AccessControlSpec with KoulutusFixture =>

  override def toteutusService = {
    val organisaatioService     = new OrganisaatioServiceImpl(urlProperties.get)
    val lokalisointiClient      = new LokalisointiClient(urlProperties.get)
    val koodistoService         = new KoodistoService(new KoodistoClient(urlProperties.get))
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
    val koutaIndeksoijaClient = new MockKoutaIndeksoijaClient
    new ToteutusService(
      SqsInTransactionService,
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
}

trait ValintaperusteFixtureWithIndexing extends ValintaperusteFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>

  override def valintaperusteService = {
    val organisaatioService             = new OrganisaatioServiceImpl(urlProperties.get)
    val koodistoService                 = new KoodistoService(new KoodistoClient(urlProperties.get))
    val koutaIndeksoijaClient           = new MockKoutaIndeksoijaClient
    val valintaperusteServiceValidation = new ValintaperusteServiceValidation(koodistoService, HakukohdeDAO)
    new ValintaperusteService(
      SqsInTransactionService,
      new AuditLog(MockAuditLogger),
      organisaatioService,
      mockOppijanumerorekisteriClient,
      mockKayttooikeusClient,
      valintaperusteServiceValidation,
      koutaIndeksoijaClient
    )
  }
}

trait HakukohdeFixtureWithIndexing extends HakukohdeFixture {
  this: KoutaIntegrationSpec with AccessControlSpec with KoulutusFixture =>

  override def hakukohdeService = {
    val organisaatioService     = new OrganisaatioServiceImpl(urlProperties.get)
    val lokalisointiClient      = new LokalisointiClient(urlProperties.get)
    val koodistoService         = new KoodistoService(new KoodistoClient(urlProperties.get))
    val koutaIndeksoijaClient   = new MockKoutaIndeksoijaClient
    val mockEPerusteKoodiClient = mock[EPerusteKoodiClient]

    val hakukohdeServiceValidation = new HakukohdeServiceValidation(
      koodistoService,
      mockHakemusPalveluClient,
      organisaatioService,
      lokalisointiClient,
      HakukohdeDAO,
      HakuDAO
    )
    val toteutusServiceValidation = new ToteutusServiceValidation(
      koodistoService,
      organisaatioService,
      KoulutusDAO,
      HakukohdeDAO,
      SorakuvausDAO,
      ToteutusDAO,
      mockEPerusteKoodiClient
    )
    new HakukohdeService(
      SqsInTransactionService,
      new AuditLog(MockAuditLogger),
      organisaatioService,
      lokalisointiClient,
      mockOppijanumerorekisteriClient,
      mockKayttooikeusClient,
      koodistoService,
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
      ),
      hakukohdeServiceValidation,
      koutaIndeksoijaClient
    )
  }
}
