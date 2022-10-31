package fi.oph.kouta.integration

import fi.oph.kouta.servlet.{IndexerServlet, SearchServlet}

import java.util.UUID

package object fixture {

  case class Oid(oid: String)
  case class Oids(oids: List[String])
  case class Id(id: UUID)
  case class Updated(updated: Boolean)
}

import fi.oph.kouta.integration.fixture._

trait IndexerFixture
    extends KoutaIntegrationSpec
    with KoulutusFixture
    with ToteutusFixture
    with HakuFixture
    with HakukohdeFixture
    with ValintaperusteFixture
    with SorakuvausFixture
    with OppilaitosFixture
    with OppilaitoksenOsaFixture {

  val IndexerPath = "/indexer"

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(
      new IndexerServlet(
        koulutusService,
        toteutusService,
        hakuService,
        hakukohdeService,
        valintaperusteService,
        sorakuvausService,
        oppilaitosService,
        oppilaitoksenOsaService
      ),
      IndexerPath
    )
  }
}

trait SearchFixture
    extends KoutaIntegrationSpec
    with KoulutusFixture
    with ToteutusFixture
    with HakuFixture
    with HakukohdeFixture
    with ValintaperusteFixture {

  val SearchPath = "/search"

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(
      new SearchServlet(koulutusService, toteutusService, hakuService, hakukohdeService, valintaperusteService),
      SearchPath
    )
  }
}
