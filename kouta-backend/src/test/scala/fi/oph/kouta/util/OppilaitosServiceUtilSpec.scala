package fi.oph.kouta.util

import fi.oph.kouta.TestOids
import fi.oph.kouta.domain.{En, Fi, Organisaatio, OrganisaatioHierarkia, OrganisaationOsa, Sv}

class OppilaitosServiceUtilSpec extends UnitSpec {
  val organisaationOsa = OrganisaationOsa(
    oid = TestOids.GrandChildOid.toString,
    parentOidPath = s"${TestOids.GrandChildOid.toString}/${TestOids.ChildOid}/${TestOids.ParentOid}/${TestOids.OphOid}",
    nimi = Map(Fi -> "Oppilaitoksen osa 1 fi", Sv -> "Oppilaitoksen osa 1 sv", En -> "Oppilaitoksen osa 1 en"),
    kotipaikkaUri = Some("kunta_179"),
    children = List(),
    organisaatiotyypit = List("organisaatiotyyppi_03"))

  val organisaatio = Organisaatio(
    oid = TestOids.ChildOid.toString,
    parentOidPath = s"${TestOids.ChildOid}/${TestOids.ParentOid}/${TestOids.OphOid}",
    oppilaitostyyppi = Some("oppilaitostyyppi_63#1"),
    nimi = Map(Fi -> "Oppilaitos fi", Sv -> "Oppilaitos sv", En -> "Oppilaitos en"),
    kotipaikkaUri = Some("kunta_179"),
    children = List(),
    organisaatiotyypit = Some(List("organisaatiotyyppi_03")))

  "getOidsFromChildren" should "return one oid for one child org" in {
    assert(OppilaitosServiceUtil.getOidsFromChildren(List(organisaationOsa)) == List(TestOids.GrandChildOid))
  }

  "getOidsFromChildren" should "return two oids for org and one child org" in {
    assert(OppilaitosServiceUtil.getOidsFromChildren(
      List(organisaationOsa.copy(children = List(organisaationOsa.copy(oid = TestOids.GrandGrandChildOid.toString))))) == List(TestOids.GrandChildOid, TestOids.GrandGrandChildOid))
  }

  it should "return two oids for two child orgs" in {
    assert(OppilaitosServiceUtil.getOidsFromChildren(List(organisaationOsa, organisaationOsa.copy(oid = TestOids.EvilGrandChildOid.toString))) == List(TestOids.GrandChildOid, TestOids.EvilGrandChildOid))
  }

  it should "return oids for two child orgs and their sub orgs" in {
    assert(OppilaitosServiceUtil.getOidsFromChildren(
      List(
        organisaationOsa.copy(children = List(organisaationOsa.copy(oid = TestOids.GrandGrandChildOid.toString))),
        organisaationOsa.copy(oid = TestOids.EvilGrandChildOid.toString, children = List(organisaationOsa.copy(oid = TestOids.EvilGrandGrandChildOid.toString)))))
      == List(TestOids.GrandChildOid, TestOids.GrandGrandChildOid, TestOids.EvilGrandChildOid, TestOids.EvilGrandGrandChildOid))
  }

  "getHierarkiaOids" should "return one oid for one org without children" in {
    assert(OppilaitosServiceUtil.getHierarkiaOids(OrganisaatioHierarkia(organisaatiot = List(organisaatio))) == List(TestOids.ChildOid))
  }

  it should "return two oids for two orgs without children" in {
    assert(OppilaitosServiceUtil.getHierarkiaOids(
      OrganisaatioHierarkia(
        organisaatiot = List(organisaatio, organisaatio.copy(oid = TestOids.EvilChildOid.toString)))) == List(TestOids.ChildOid, TestOids.EvilChildOid))
  }

  it should "return distinct oids for all the children recursively" in {
    val organisaatioHierarkia = OrganisaatioHierarkia(organisaatiot = List(
      organisaatio.copy(
        children = List(
          organisaationOsa)),
      organisaatio.copy(
        oid = TestOids.EvilChildOid.toString,
        children = List(
          organisaationOsa,
          organisaationOsa.copy(oid = TestOids.EvilGrandChildOid.toString, children = List(
            organisaationOsa.copy(oid = TestOids.EvilGrandGrandChildOid.toString),
            organisaationOsa.copy(oid = TestOids.GrandGrandChildOid.toString)))))
    ))

    assert(OppilaitosServiceUtil.getHierarkiaOids(organisaatioHierarkia) ==
      List(TestOids.GrandChildOid, TestOids.ChildOid, TestOids.EvilGrandChildOid, TestOids.EvilGrandGrandChildOid, TestOids.GrandGrandChildOid, TestOids.EvilChildOid))
  }
}
