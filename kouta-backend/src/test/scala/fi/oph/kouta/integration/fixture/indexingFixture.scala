package fi.oph.kouta.integration.fixture

import fi.oph.kouta.SqsInTransactionServiceIgnoringIndexing
import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.{EPerusteKoodiClient, CachedKoodistoClient, LokalisointiClient}
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.integration.{AccessControlSpec, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.{MockAuditLogger, MockOhjausparametritClient, MockS3ImageService}
import fi.oph.kouta.repository.{HakuDAO, HakukohdeDAO, KoulutusDAO, SorakuvausDAO, ToteutusDAO}
import fi.oph.kouta.service.validation.AmmatillinenKoulutusServiceValidation
import fi.oph.kouta.service._

trait IndexingFixture extends KoulutusFixtureWithIndexing with HakuFixtureWithIndexing with ToteutusFixtureWithIndexing
  with ValintaperusteFixtureWithIndexing with HakukohdeFixtureWithIndexing with SorakuvausFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>
}

trait HakuFixtureWithIndexing extends HakuFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>

  override def hakuService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    val koodistoClient = new CachedKoodistoClient(urlProperties.get)
    val hakuServiceValidation = new HakuServiceValidation(koodistoClient, mockHakemusPalveluClient, HakukohdeDAO)
    new HakuService(SqsInTransactionService, new AuditLog(MockAuditLogger), MockOhjausparametritClient, organisaatioService, mockOppijanumerorekisteriClient, mockKayttooikeusClient, hakuServiceValidation)
  }
}

trait KoulutusFixtureWithIndexing extends KoulutusFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>

  override def koulutusService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    val koodistoClient = new CachedKoodistoClient(urlProperties.get)
    val ePerusteKoodiClient = new EPerusteKoodiClient(urlProperties.get)
    val ammKoulutusServiceValidation = new AmmatillinenKoulutusServiceValidation(koodistoClient, ePerusteKoodiClient)

    val koulutusServiceValidation =
      new KoulutusServiceValidation(koodistoClient, organisaatioService, ToteutusDAO, SorakuvausDAO, ammKoulutusServiceValidation)

    new KoulutusService(SqsInTransactionService, MockS3ImageService, new AuditLog(MockAuditLogger), organisaatioService, mockOppijanumerorekisteriClient, mockKayttooikeusClient, koodistoClient, koulutusServiceValidation, mockKoutaSearchClient, ePerusteKoodiClient)
  }
}

trait ToteutusFixtureWithIndexing extends ToteutusFixture {
  this: KoutaIntegrationSpec with AccessControlSpec with KoulutusFixture =>

  override def toteutusService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    val lokalisointiClient  = new LokalisointiClient(urlProperties.get)
    val koodistoClient = new CachedKoodistoClient(urlProperties.get)
    val toteutusServiceValidation = new ToteutusServiceValidation(koodistoClient, organisaatioService, KoulutusDAO, HakukohdeDAO, SorakuvausDAO, ToteutusDAO)
    new ToteutusService(SqsInTransactionService, MockS3ImageService, auditLog,
      new KeywordService(auditLog, organisaatioService), organisaatioService, koulutusService, lokalisointiClient, koodistoClient, mockOppijanumerorekisteriClient, mockKayttooikeusClient, toteutusServiceValidation)
  }
}

trait ValintaperusteFixtureWithIndexing extends ValintaperusteFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>

  override def valintaperusteService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    val koodistoClient = new CachedKoodistoClient(urlProperties.get)
    val valintaperusteServiceValidation = new ValintaperusteServiceValidation(koodistoClient, HakukohdeDAO)
    new ValintaperusteService(SqsInTransactionService, new AuditLog(MockAuditLogger), organisaatioService, mockOppijanumerorekisteriClient, mockKayttooikeusClient, valintaperusteServiceValidation)
  }
}

trait HakukohdeFixtureWithIndexing extends HakukohdeFixture {
  this: KoutaIntegrationSpec with AccessControlSpec with KoulutusFixture =>

  override def hakukohdeService = {
    val organisaatioService = new OrganisaatioServiceImpl(urlProperties.get)
    val lokalisointiClient  = new LokalisointiClient(urlProperties.get)
    val koodistoClient = new CachedKoodistoClient(urlProperties.get)
    val hakukohdeServiceValidation = new HakukohdeServiceValidation(koodistoClient, mockHakemusPalveluClient, organisaatioService, lokalisointiClient, HakukohdeDAO, HakuDAO)
    val toteutusServiceValidation = new ToteutusServiceValidation(koodistoClient, organisaatioService, KoulutusDAO, HakukohdeDAO, SorakuvausDAO, ToteutusDAO)
    new HakukohdeService(SqsInTransactionService, new AuditLog(MockAuditLogger), organisaatioService, lokalisointiClient, mockOppijanumerorekisteriClient, mockKayttooikeusClient, koodistoClient,
      new ToteutusService(SqsInTransactionServiceIgnoringIndexing, MockS3ImageService, auditLog,
        new KeywordService(auditLog, organisaatioService), organisaatioService, koulutusService, lokalisointiClient,
        koodistoClient, mockOppijanumerorekisteriClient, mockKayttooikeusClient, toteutusServiceValidation),
    hakukohdeServiceValidation)
  }
}
