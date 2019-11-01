package fi.oph.kouta.integration.fixture

import fi.oph.kouta.indexing.SqsInTransactionService
import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.service._

trait IndexingFixture extends KoulutusFixtureWithIndexing with HakuFixtureWithIndexing with ToteutusFixtureWithIndexing
  with ValintaperusteFixtureWithIndexing with HakukohdeFixtureWithIndexing {
  this: KoutaIntegrationSpec =>
}

trait HakuFixtureWithIndexing extends HakuFixture {
  this: KoutaIntegrationSpec =>
  override protected lazy val hakuService = HakuService
}

trait KoulutusFixtureWithIndexing extends KoulutusFixture {
  this: KoutaIntegrationSpec =>
  override protected lazy val koulutusService = new KoulutusService(SqsInTransactionService, MockS3Service)
}

trait ToteutusFixtureWithIndexing extends ToteutusFixture {
  this: KoutaIntegrationSpec =>
  override protected lazy val toteutusService = ToteutusService
}

trait ValintaperusteFixtureWithIndexing extends ValintaperusteFixture {
  this: KoutaIntegrationSpec =>
  override protected lazy val valintaperusteService = ValintaperusteService
}

trait HakukohdeFixtureWithIndexing extends HakukohdeFixture {
  this: KoutaIntegrationSpec =>
  override protected lazy val hakukohdeService = HakukohdeService
}
