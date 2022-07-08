package fi.oph.kouta.integration.fixture

import fi.oph.kouta.SqsInTransactionServiceIgnoringIndexing
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{EPerusteKoodiClient, HakuKoodiClient, KoodistoClient, KoodistoKaannosClient, KoulutusKoodiClient, LokalisointiClient}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.integration.{AccessControlSpec, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.{MockAuditLogger, MockOhjausparametritClient, MockS3ImageService}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, KoulutusDAO, SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.service.{OrganisaatioServiceImpl, _}

trait IndexingFixture extends KoulutusFixtureWithIndexing with HakuFixtureWithIndexing with ToteutusFixtureWithIndexing
  with ValintaperusteFixtureWithIndexing with HakukohdeFixtureWithIndexing with SorakuvausFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>
}

trait HakuFixtureWithIndexing extends HakuFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>

  override def hakuService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    new HakuService(SqsInTransactionService, new AuditLog(MockAuditLogger), MockOhjausparametritClient, organisaatioService, mockOppijanumerorekisteriClient, mockKayttooikeusClient)
  }
}

trait KoulutusFixtureWithIndexing extends KoulutusFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>

  override def koulutusService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    val koodistoClient = new KoulutusKoodiClient(urlProperties.get)
    val ePerusteKoodiClient = new EPerusteKoodiClient(urlProperties.get)
    val koulutusServiceValidation =
      new KoulutusServiceValidation(koodistoClient, ePerusteKoodiClient, organisaatioService, ToteutusDAO, SorakuvausDAO)

    new KoulutusService(SqsInTransactionService, MockS3ImageService, new AuditLog(MockAuditLogger), organisaatioService, mockOppijanumerorekisteriClient, mockKayttooikeusClient, koodistoClient, koulutusServiceValidation)
  }
}

trait ToteutusFixtureWithIndexing extends ToteutusFixture {
  this: KoutaIntegrationSpec with AccessControlSpec with KoulutusFixture =>

  override def toteutusService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    val lokalisointiClient  = new LokalisointiClient(urlProperties.get)
    val koodistoClient = new KoodistoKaannosClient(urlProperties.get)
    val koulutusKoodiClient = new KoulutusKoodiClient(urlProperties.get)
    val hakuKoodiClient = new HakuKoodiClient(urlProperties.get)
    val toteutusServiceValidation = new ToteutusServiceValidation(koulutusKoodiClient, organisaatioService, hakuKoodiClient, KoulutusDAO, HakukohdeDAO, SorakuvausDAO)
    new ToteutusService(SqsInTransactionService, MockS3ImageService, auditLog,
      new KeywordService(auditLog, organisaatioService), organisaatioService, koulutusService, lokalisointiClient, koodistoClient, mockOppijanumerorekisteriClient, mockKayttooikeusClient, toteutusServiceValidation)
  }
}

trait ValintaperusteFixtureWithIndexing extends ValintaperusteFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>

  override def valintaperusteService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    new ValintaperusteService(SqsInTransactionService, new AuditLog(MockAuditLogger), organisaatioService, mockOppijanumerorekisteriClient, mockKayttooikeusClient)
  }
}

trait HakukohdeFixtureWithIndexing extends HakukohdeFixture {
  this: KoutaIntegrationSpec with AccessControlSpec with KoulutusFixture =>

  override def hakukohdeService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    val lokalisointiClient  = new LokalisointiClient(urlProperties.get)
    val koodistoClient = new KoodistoKaannosClient(urlProperties.get)
    val koulutusKoodiClient = new KoulutusKoodiClient(urlProperties.get)
    val hakuKoodiClient = new HakuKoodiClient(urlProperties.get)
    val hakukohdeServiceValidation = new HakukohdeServiceValidation(organisaatioService, hakuKoodiClient, null, HakukohdeDAO, HakuDAO)
    val toteutusServiceValidation = new ToteutusServiceValidation(koulutusKoodiClient, organisaatioService, hakuKoodiClient, KoulutusDAO, HakukohdeDAO, SorakuvausDAO)
    new HakukohdeService(SqsInTransactionService, new AuditLog(MockAuditLogger), organisaatioService, lokalisointiClient, mockOppijanumerorekisteriClient, mockKayttooikeusClient,
      new ToteutusService(SqsInTransactionServiceIgnoringIndexing, MockS3ImageService, auditLog,
        new KeywordService(auditLog, organisaatioService), organisaatioService, koulutusService, lokalisointiClient,
        koodistoClient, mockOppijanumerorekisteriClient, mockKayttooikeusClient, toteutusServiceValidation),
    hakukohdeServiceValidation)
  }
}
