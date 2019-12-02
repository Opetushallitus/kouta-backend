package fi.oph.kouta.integration

import java.util.UUID

import fi.oph.kouta.servlet.IndexerServlet

package object fixture {

  case class Oid(oid:String)
  case class Id(id:UUID)
  case class Updated(updated:Boolean)
}

import fi.oph.kouta.integration.fixture._
trait EverythingFixture extends KoulutusFixture with ToteutusFixture with HakuFixture
  with HakukohdeFixture with ValintaperusteFixture with SorakuvausFixture
  with OppilaitosFixture with OppilaitoksenOsaFixture { this: KoutaIntegrationSpec =>
}

trait IndexerFixture { this: EverythingFixture with KoutaIntegrationSpec =>

  val IndexerPath = "/indexer"

  addServlet(new IndexerServlet(koulutusService, toteutusService, hakuService,
    valintaperusteService, sorakuvausService, oppilaitosService), IndexerPath)
}