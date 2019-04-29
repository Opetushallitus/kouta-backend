package fi.oph.kouta.integration.fixture

import fi.oph.kouta.integration.KoutaIntegrationSpec
import fi.oph.kouta.service._

trait HakuFixtureWithIndexing extends HakuFixture {
  this: KoutaIntegrationSpec =>
  override protected lazy val hakuService = HakuService
}

trait KoulutusFixtureWithIndexing extends KoulutusFixture {
  this: KoutaIntegrationSpec =>
  override protected lazy val koulutusService = KoulutusService
}

trait ToteutusFixtureWithIndexing extends ToteutusFixture {
  this: KoutaIntegrationSpec =>
  override protected lazy val toteutusService = ToteutusService
}

trait EverythingFixtureWithIndexing extends KoulutusFixtureWithIndexing with HakuFixtureWithIndexing with ToteutusFixtureWithIndexing {
  this: KoutaIntegrationSpec =>
}
