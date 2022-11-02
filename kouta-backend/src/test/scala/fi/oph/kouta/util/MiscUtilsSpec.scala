package fi.oph.kouta.util

import fi.oph.kouta.TestData
import fi.oph.kouta.TestOids.{ChildOid, EvilChildOid, GrandChildOid, ParentOid}

class MiscUtilsSpec extends UnitSpec {
  val organisaatiotyyppi = "organisaatiotyyppi_02"
  "filterOrganisaatiotWithOppilaitostyypit" should "return both organisaation osat with correct organisaatiotyyppi and oppilaitostyyppi" in {
    val organisaatiot = List(
      TestData.organisaatio.copy(oppilaitostyyppi = Some("oppilaitostyyppi_42#1"), organisaatiotyypit = List("organisaatiotyyppi_02"), children = List()),
      TestData.organisaatio.copy(oppilaitostyyppi = Some("oppilaitostyyppi_43#1"), organisaatiotyypit = List("organisaatiotyyppi_02"), children = List())
    )
    assert(MiscUtils.filterOrganisaatiotWithOrganisaatiotyyppi(organisaatiot, organisaatiotyyppi) == organisaatiot)
  }

  it should "remove one organisaation osa with wrong organisaatiotyyppi" in {
    val organisaatiot = List(
      TestData.organisaatio.copy(oppilaitostyyppi = Some("oppilaitostyyppi_61#1"), organisaatiotyypit = List("organisaatiotyyppi_03"), children = List()),
      TestData.organisaatio.copy(oppilaitostyyppi = Some("oppilaitostyyppi_42#1"), organisaatiotyypit = List("organisaatiotyyppi_02"), children = List())
    )
    assert(MiscUtils.filterOrganisaatiotWithOrganisaatiotyyppi(organisaatiot, organisaatiotyyppi) ==
      List(TestData.organisaatio.copy(oppilaitostyyppi = Some("oppilaitostyyppi_42#1"), organisaatiotyypit = List("organisaatiotyyppi_02"), children = List())))
  }

  it should "return both organisaation osa and its parent because organisaation osa has correct organisaatiotyyppi" in {
    val organisaationOsa = TestData.organisaationOsa.copy(
      oid = ChildOid.toString,
      parentOidPath = s"${ChildOid.toString}/${ParentOid.toString}/1.2.246.562.10.00000000001",
      oppilaitostyyppi = Some("oppilaitostyyppi_43#1"),
      organisaatiotyypit = List("organisaatiotyyppi_02", "organisaatiotyyppi_03"),
      children = List()
    )
    val organisaatio = TestData.organisaationOsa.copy(
      oid = ParentOid.toString,
      parentOidPath = s"${ParentOid.toString}/1.2.246.562.10.00000000001",
      organisaatiotyypit = List("organisaatiotyyppi_03"),
      children = List(organisaationOsa)
    )
    val organisaatiot = List(organisaatio)
    assert(MiscUtils.filterOrganisaatiotWithOrganisaatiotyyppi(organisaatiot, organisaatiotyyppi) == List(organisaatio))
  }

  it should "return empty list as the only organisation has wrong oppilaitostyyppi" in {
    val organisaatiot = List(TestData.parentOrganisaatio.copy(
      oppilaitostyyppi = Some("oppilaitostyyppi_01#1"),
      organisaatiotyypit = List("organisaatiotyyppi_02"),
      children = List()))
    assert(MiscUtils.filterOrganisaatiotWithOrganisaatiotyyppi(organisaatiot, organisaatiotyyppi) == List())
  }

  it should "remove organisaatio and its only child from the list if the child has a wrong oppilaitostyyppi" in {
    val organisaationOsa = TestData.organisaationOsa.copy(
      oid = ChildOid.toString,
      parentOidPath = s"${ChildOid.toString}/${ParentOid.toString}/1.2.246.562.10.00000000001",
      oppilaitostyyppi = Some("oppilaitostyyppi_01#1"),
      organisaatiotyypit = List("organisaatiotyyppi_02"),
      children = List()
    )
    val organisaatiot = List(TestData.parentOrganisaatio.copy(
      oid = ParentOid.toString,
      parentOidPath = s"${ParentOid.toString}/1.2.246.562.10.00000000001",
      children = List(organisaationOsa))
    )
    assert(MiscUtils.filterOrganisaatiotWithOrganisaatiotyyppi(organisaatiot, organisaatiotyyppi) == List())
  }

  it should "return organisaatio, its child and grandchild because grandchild has the correct oppilaitostyyppi and organisaatiotyyppi" in {
    val grandChild = TestData.organisaationOsa.copy(
      oid = GrandChildOid.toString,
      parentOidPath = s"${GrandChildOid.toString}//${ChildOid.toString}/${ParentOid.toString}/1.2.246.562.10.00000000001",
      oppilaitostyyppi = Some("oppilaitostyyppi_43#1"),
      organisaatiotyypit = List("organisaatiotyyppi_02"),
    )
    val child = TestData.organisaationOsa.copy(
      oid = ChildOid.toString,
      parentOidPath = s"${ChildOid.toString}/${ParentOid.toString}/1.2.246.562.10.00000000001",
      oppilaitostyyppi = Some("oppilaitostyyppi_43#1"),
      organisaatiotyypit = List("organisaatiotyyppi_02"),
      children = List(grandChild)
    )
    val childWithWrongTypes = TestData.organisaationOsa.copy(
      oid = EvilChildOid.toString,
      parentOidPath = s"${EvilChildOid.toString}/${ParentOid.toString}/1.2.246.562.10.00000000001",
      oppilaitostyyppi = Some("oppilaitostyyppi_02#1"),
      organisaatiotyypit = List("organisaatiotyyppi_03"),
    )
    val organisaatiot = List(TestData.parentOrganisaatio.copy(
      oid = ParentOid.toString,
      parentOidPath = s"${ParentOid.toString}/1.2.246.562.10.00000000001",
      oppilaitostyyppi = None,
      children = List(child, childWithWrongTypes))
    )
    val result = List(TestData.parentOrganisaatio.copy(
      oid = ParentOid.toString,
      parentOidPath = s"${ParentOid.toString}/1.2.246.562.10.00000000001",
      oppilaitostyyppi = None,
      children = List(child))
    )
    assert(MiscUtils.filterOrganisaatiotWithOrganisaatiotyyppi(organisaatiot, organisaatiotyyppi) == result)
  }
}
