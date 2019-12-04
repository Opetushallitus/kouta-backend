package fi.oph.kouta.integration.fixture

import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.mocks.{MockAuditLogger, MockS3Service}
import fi.oph.kouta.service._
import fi.oph.kouta.util.AuditLog

trait IndexingFixture extends KoulutusFixtureWithIndexing with HakuFixtureWithIndexing with ToteutusFixtureWithIndexing
  with ValintaperusteFixtureWithIndexing with HakukohdeFixtureWithIndexing {
  this: KoutaIntegrationSpec =>
}

trait HakuFixtureWithIndexing extends HakuFixture {
  this: KoutaIntegrationSpec =>
  override protected lazy val hakuService = new HakuService(SqsInTransactionService, new AuditLog(MockAuditLogger))
}

trait KoulutusFixtureWithIndexing extends KoulutusFixture {
  this: KoutaIntegrationSpec =>
  override protected lazy val koulutusService = new KoulutusService(SqsInTransactionService, MockS3Service, new AuditLog(MockAuditLogger))
}

trait ToteutusFixtureWithIndexing extends ToteutusFixture {
  this: KoutaIntegrationSpec =>
  private lazy val auditLog = new AuditLog(MockAuditLogger)
  override protected lazy val toteutusService = new ToteutusService(SqsInTransactionService, MockS3Service, auditLog, new KeywordService(auditLog))
}

trait ValintaperusteFixtureWithIndexing extends ValintaperusteFixture {
  this: KoutaIntegrationSpec =>
  override protected lazy val valintaperusteService = new ValintaperusteService(SqsInTransactionService, new AuditLog(MockAuditLogger))
}

trait HakukohdeFixtureWithIndexing extends HakukohdeFixture {
  this: KoutaIntegrationSpec =>
  override protected lazy val hakukohdeService = new HakukohdeService(SqsInTransactionService, new AuditLog(MockAuditLogger))
}
