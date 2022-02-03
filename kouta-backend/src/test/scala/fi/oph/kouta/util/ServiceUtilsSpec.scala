package fi.oph.kouta.util

import fi.oph.kouta.TestOids.{ChildOid, OphOid, ParentOid}
import fi.oph.kouta.client.OrganisaatioHenkilo

class ServiceUtilsSpec extends UnitSpec {
  "hasOphOrganisaatioOid" should "return true when the only entity in list has OPH organisaatioOid" in {
    val organisaatiohenkilot = List(OrganisaatioHenkilo(organisaatioOid = OphOid.toString))
    assert(ServiceUtils.hasOphOrganisaatioOid(organisaatiohenkilot) === true)
  }

  "hasOphOrganisaatioOid" should "return false when the only entity in list has some random organisaatioOid" in {
    val organisaatiohenkilot = List(OrganisaatioHenkilo(organisaatioOid = ParentOid.toString))
    assert(ServiceUtils.hasOphOrganisaatioOid(organisaatiohenkilot) === false)
  }

  "hasOphOrganisaatioOid" should "return true when OPH organisaatioOid is one of the oids" in {
    val organisaatiohenkilot = List(OrganisaatioHenkilo(organisaatioOid = ParentOid.toString), OrganisaatioHenkilo(organisaatioOid = OphOid.toString), OrganisaatioHenkilo(organisaatioOid = ChildOid.toString))
    assert(ServiceUtils.hasOphOrganisaatioOid(organisaatiohenkilot) === true)
  }

  "hasOphOrganisaatioOid" should "return false when list of organisaatiohenkilot is empty" in {
    val organisaatiohenkilot = List()
    assert(ServiceUtils.hasOphOrganisaatioOid(organisaatiohenkilot) === false)
  }
}
