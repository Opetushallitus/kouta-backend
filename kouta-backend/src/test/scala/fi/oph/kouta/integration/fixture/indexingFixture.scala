package fi.oph.kouta.integration.fixture

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{KoodistoClient, LokalisointiClient}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.integration.{AccessControlSpec, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.{MockAuditLogger, MockKayttooikeusClient, MockOhjausparametritClient, MockOppijanumerorekisteriClient, MockS3ImageService}
import fi.oph.kouta.service.{OrganisaatioServiceImpl, _}

trait IndexingFixture extends KoulutusFixtureWithIndexing with HakuFixtureWithIndexing with ToteutusFixtureWithIndexing
  with ValintaperusteFixtureWithIndexing with HakukohdeFixtureWithIndexing with SorakuvausFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>
}

trait HakuFixtureWithIndexing extends HakuFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>

  override def hakuService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    new HakuService(SqsInTransactionService, new AuditLog(MockAuditLogger), MockOhjausparametritClient, organisaatioService, oppijanumerorekisteriClient, mockKayttooikeusClient)
  }
}

trait KoulutusFixtureWithIndexing extends KoulutusFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>

  override def koulutusService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    new KoulutusService(SqsInTransactionService, MockS3ImageService, new AuditLog(MockAuditLogger), organisaatioService, MockOppijanumerorekisteriClient, mockKayttooikeusClient)
  }
}

trait ToteutusFixtureWithIndexing extends ToteutusFixture {
  this: KoutaIntegrationSpec with AccessControlSpec with KoulutusFixture =>

  override def toteutusService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    val lokalisointiClient  = new LokalisointiClient(urlProperties.get)
    val koodistoClient = new KoodistoClient(urlProperties.get)
    new ToteutusService(SqsInTransactionService, MockS3ImageService, auditLog,
      new KeywordService(auditLog, organisaatioService), organisaatioService, koulutusService, lokalisointiClient, koodistoClient, MockOppijanumerorekisteriClient, mockKayttooikeusClient)
  }
}

trait ValintaperusteFixtureWithIndexing extends ValintaperusteFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>

  override def valintaperusteService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    new ValintaperusteService(SqsInTransactionService, new AuditLog(MockAuditLogger), organisaatioService)
  }
}

trait HakukohdeFixtureWithIndexing extends HakukohdeFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>

  override def hakukohdeService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    val lokalisointiClient  = new LokalisointiClient(urlProperties.get)
    new HakukohdeService(SqsInTransactionService, new AuditLog(MockAuditLogger), organisaatioService, lokalisointiClient)
  }
}
