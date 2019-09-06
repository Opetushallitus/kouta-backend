package fi.oph.kouta.integration

import java.util.UUID

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
