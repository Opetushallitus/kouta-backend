package fi.oph.kouta.integration.fixture

import fi.oph.kouta.auditlog.AuditLog
import fi.oph.kouta.client.OrganisaatioClient
import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.integration.{AccessControlSpec, KoutaIntegrationSpec}
import fi.oph.kouta.mocks.{MockAuditLogger, MockOhjausparametritClient, MockS3ImageService}
import fi.oph.kouta.service._

trait IndexingFixture extends KoulutusFixtureWithIndexing with HakuFixtureWithIndexing with ToteutusFixtureWithIndexing
  with ValintaperusteFixtureWithIndexing with HakukohdeFixtureWithIndexing with SorakuvausFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>
}

trait HakuFixtureWithIndexing extends HakuFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>

  override def hakuService = {
    val organisaatioClient = new OrganisaatioClient(urlProperties.get, "kouta-backend")
    new HakuService(SqsInTransactionService, new AuditLog(MockAuditLogger), MockOhjausparametritClient, organisaatioClient)
  }
}

trait KoulutusFixtureWithIndexing extends KoulutusFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>

  override def koulutusService = {
    val organisaatioClient = new OrganisaatioClient(urlProperties.get, "kouta-backend")
    new KoulutusService(SqsInTransactionService, MockS3ImageService, new AuditLog(MockAuditLogger), organisaatioClient)
  }
}

trait ToteutusFixtureWithIndexing extends ToteutusFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>

  override def toteutusService = {
    val organisaatioClient = new OrganisaatioClient(urlProperties.get, "kouta-backend")
    new ToteutusService(SqsInTransactionService, MockS3ImageService, auditLog, new KeywordService(auditLog, organisaatioClient), organisaatioClient)
  }
}

trait ValintaperusteFixtureWithIndexing extends ValintaperusteFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>

  override def valintaperusteService = {
    val organisaatioClient = new OrganisaatioClient(urlProperties.get, "kouta-backend")
    new ValintaperusteService(SqsInTransactionService, new AuditLog(MockAuditLogger), organisaatioClient)
  }
}

trait HakukohdeFixtureWithIndexing extends HakukohdeFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>

  override def hakukohdeService = {
    val organisaatioClient = new OrganisaatioClient(urlProperties.get, "kouta-backend")
    new HakukohdeService(SqsInTransactionService, new AuditLog(MockAuditLogger), organisaatioClient)
  }
}
