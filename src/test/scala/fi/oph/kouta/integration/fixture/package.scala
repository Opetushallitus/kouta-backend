package fi.oph.kouta.integration

import java.time.LocalDateTime
import java.util.UUID

import fi.oph.kouta.domain.oid.GenericOid
import fi.oph.kouta.domain.{IdListItem, OidListItem, Perustiedot}

package object fixture {

  case class Oid(oid:String)
  case class Id(id:UUID)
  case class Updated(updated:Boolean)

  def toOidListItem(oid:String, e:Perustiedot, modified:LocalDateTime) =
    new OidListItem(GenericOid(oid), e.nimi, e.tila, e.organisaatioOid, e.muokkaaja, modified)

  def toIdListItem(id:UUID, e:Perustiedot, modified:LocalDateTime) =
    new IdListItem(id, e.nimi, e.tila, e.organisaatioOid, e.muokkaaja, modified)
}

import fi.oph.kouta.integration.fixture._
trait EverythingFixture extends KoulutusFixture with ToteutusFixture with HakuFixture
  with HakukohdeFixture with ValintaperusteFixture { this: KoutaIntegrationSpec =>
}
