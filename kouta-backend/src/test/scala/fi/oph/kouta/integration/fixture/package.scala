package fi.oph.kouta.integration

import fi.oph.kouta.domain.oid.{HakukohdeOid, ToteutusOid}

import java.util.UUID
import fi.oph.kouta.servlet.{IndexerServlet, SearchServlet}

package object fixture {

  case class Oid(oid:String)
  case class Oids(oids:List[String])
  case class CreatedOids(hakukohdeOid: HakukohdeOid, toteutusOid: ToteutusOid)
  case class HakukohdeCopyResult(oid: HakukohdeOid, status: String, created: CreatedOids)
  case class Id(id:UUID)
  case class Updated(updated:Boolean)
}

import fi.oph.kouta.integration.fixture._
trait EverythingFixture extends KoulutusFixture with ToteutusFixture with HakuFixture
  with HakukohdeFixture with ValintaperusteFixture with SorakuvausFixture
  with OppilaitosFixture with OppilaitoksenOsaFixture {
  this: KoutaIntegrationSpec with AccessControlSpec =>
}

trait IndexerFixture extends EverythingFixture with KoutaIntegrationSpec {

  val IndexerPath = "/indexer"

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(new IndexerServlet(koulutusService, toteutusService, hakuService,
      valintaperusteService, sorakuvausService, oppilaitosService, oppilaitoksenOsaService), IndexerPath)
  }
}

trait SearchFixture extends EverythingFixture with KoutaIntegrationSpec {

  val SearchPath = "/search"

  override def beforeAll(): Unit = {
    super.beforeAll()
    addServlet(new SearchServlet(koulutusService, toteutusService, hakuService,
      hakukohdeService, valintaperusteService), SearchPath)
  }
}
