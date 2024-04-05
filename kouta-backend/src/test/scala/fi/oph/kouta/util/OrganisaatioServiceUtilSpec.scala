package fi.oph.kouta.util

import fi.oph.kouta.TestOids
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid

class OrganisaatioServiceUtilSpec extends UnitSpec {
  val organisaationOsa: Organisaatio = Organisaatio(
    oid = TestOids.GrandChildOid.toString,
    parentOids = List(TestOids.GrandChildOid, TestOids.ChildOid, TestOids.ParentOid, TestOids.OphOid),
    nimi = Map(Fi -> "Oppilaitoksen osa 1 fi", Sv -> "Oppilaitoksen osa 1 sv", En -> "Oppilaitoksen osa 1 en"),
    status = "AKTIIVINEN",
    children = None,
    organisaatiotyyppiUris = Some(List("organisaatiotyyppi_03")))

  val hierarkiaorganisaatio: Organisaatio = Organisaatio(
    oid = TestOids.ChildOid.toString,
    parentOids = List(TestOids.ChildOid, TestOids.ParentOid, TestOids.OphOid),
    oppilaitostyyppiUri = Some("oppilaitostyyppi_63#1"),
    nimi = Map(Fi -> "Oppilaitos fi", Sv -> "Oppilaitos sv", En -> "Oppilaitos en"),
    status = "AKTIIVINEN",
    children = None,
    organisaatiotyyppiUris = Some(List("organisaatiotyyppi_03")))

  val orgServiceHierarkiaOrganisaationOsa: OrganisaatioServiceOrg = OrganisaatioServiceOrg(
    oid = TestOids.GrandChildOid.toString,
    parentOidPath = s"${TestOids.ChildOid}/${TestOids.ParentOid}/${TestOids.OphOid}",
    oppilaitostyyppi = Some("oppilaitostyyppi_63#1"),
    status = "AKTIIVINEN",
    nimi = Map(Fi -> "Oppilaitoksen osa fi", Sv -> "Oppilaitoksen osa sv", En -> "Oppilaitoksen osa en"),
    children = None,
    organisaatiotyypit = Some(List("organisaatiotyyppi_03")))

  val orgServiceHierarkiaOrganisaatio: OrganisaatioServiceOrg = OrganisaatioServiceOrg(
    oid = TestOids.ChildOid.toString,
    parentOid = Some(TestOids.ParentOid.toString),
    parentOidPath = s"${TestOids.ChildOid}/${TestOids.ParentOid}/${TestOids.OphOid}",
    oppilaitostyyppi = Some("oppilaitostyyppi_64#1"),
    status = "AKTIIVINEN",
    nimi = Map(Fi -> "Oppilaitos fi", Sv -> "Oppilaitos sv", En -> "Oppilaitos en"),
    children = Some(List(orgServiceHierarkiaOrganisaationOsa)),
    organisaatiotyypit = Some(List("organisaatiotyyppi_04")))

  "getOidsFromChildren" should "return one oid for one child org" in {
    assert(OrganisaatioServiceUtil.getOidsFromChildren(Some(List(organisaationOsa))) == List(TestOids.GrandChildOid))
  }

  it should "return two oids for org and one child org" in {
    assert(OrganisaatioServiceUtil.getOidsFromChildren(Some(
      List(organisaationOsa.copy(children = Some(List(organisaationOsa.copy(oid = TestOids.GrandGrandChildOid.toString))))))) == List(TestOids.GrandChildOid, TestOids.GrandGrandChildOid))
  }

  it should "return two oids for two child orgs" in {
    assert(OrganisaatioServiceUtil.getOidsFromChildren(Some(List(organisaationOsa, organisaationOsa.copy(oid = TestOids.EvilGrandChildOid.toString)))) == List(TestOids.GrandChildOid, TestOids.EvilGrandChildOid))
  }

  it should "return oids for two child orgs and their sub orgs" in {
    assert(OrganisaatioServiceUtil.getOidsFromChildren(
      Some(List(
        organisaationOsa.copy(children = Some(List(organisaationOsa.copy(oid = TestOids.GrandGrandChildOid.toString)))),
        organisaationOsa.copy(oid = TestOids.EvilGrandChildOid.toString, children = Some(List(organisaationOsa.copy(oid = TestOids.EvilGrandGrandChildOid.toString)))))))
      == List(TestOids.GrandChildOid, TestOids.GrandGrandChildOid, TestOids.EvilGrandChildOid, TestOids.EvilGrandGrandChildOid))
  }

  "getHierarkiaOids" should "return one oid for one org without children" in {
    assert(OrganisaatioServiceUtil.getHierarkiaOids(OrganisaatioHierarkia(organisaatiot = List(hierarkiaorganisaatio))) == List(TestOids.ChildOid))
  }

  it should "return two oids for two orgs without children" in {
    assert(OrganisaatioServiceUtil.getHierarkiaOids(
      OrganisaatioHierarkia(
        organisaatiot = List(hierarkiaorganisaatio, hierarkiaorganisaatio.copy(oid = TestOids.EvilChildOid.toString)))) == List(TestOids.ChildOid, TestOids.EvilChildOid))
  }

  it should "return distinct oids for all the children recursively" in {
    val organisaatioHierarkia = OrganisaatioHierarkia(organisaatiot = List(
      hierarkiaorganisaatio.copy(
        children = Some(List(
          organisaationOsa))),
      hierarkiaorganisaatio.copy(
        oid = TestOids.EvilChildOid.toString,
        children = Some(List(
          organisaationOsa,
          organisaationOsa.copy(oid = TestOids.EvilGrandChildOid.toString, children = Some(List(
            organisaationOsa.copy(oid = TestOids.EvilGrandGrandChildOid.toString),
            organisaationOsa.copy(oid = TestOids.GrandGrandChildOid.toString)))))))
    ))

    assert(OrganisaatioServiceUtil.getHierarkiaOids(organisaatioHierarkia) ==
      List(TestOids.GrandChildOid, TestOids.ChildOid, TestOids.EvilGrandChildOid, TestOids.EvilGrandGrandChildOid, TestOids.GrandGrandChildOid, TestOids.EvilChildOid))
  }

  val yhteystiedot: List[OrganisaationYhteystieto] = List(
    Email(Fi, "koulutus@opisto.fi"),
    OrgOsoite("kaynti", Fi, "Hankalankuja 228", Some("posti_15110")),
    Puhelin(Fi, "050 44042961"),
    OrgOsoite("posti", Fi, "Jalanluiskahtamavaarankuja 580", Some("posti_15110")),
    OrgOsoite("posti", Sv, "Jalanluiskahtamavaaravägen 581", Some("posti_15110")),
    Www(Fi, "http://www.salpaus.fi"))

  "filterByOsoitetyyppi" should "return postiosoitteet from a list of yhteystiedot" in {
    assert(OrganisaatioServiceUtil.filterByOsoitetyyppi(yhteystiedot, "posti") == List(
      OrgOsoite("posti", Fi, "Jalanluiskahtamavaarankuja 580", Some("posti_15110")),
      OrgOsoite("posti", Sv, "Jalanluiskahtamavaaravägen 581", Some("posti_15110"))))
  }

  it should "return käyntiosoite from the list of yhteystiedot" in {
    assert(OrganisaatioServiceUtil.filterByOsoitetyyppi(yhteystiedot, "kaynti") ==
      List(OrgOsoite("kaynti", Fi, "Hankalankuja 228", Some("posti_15110"))))
  }

  it should "return empty list as there is no postiosoite in yhteystiedot" in {
    val yhteystiedot = List(
      Email(Fi, "koulutus@opisto.fi"),
      OrgOsoite("kaynti", Fi, "Hankalankuja 228", Some("posti_15110")),
      Puhelin(Fi, "050 44042961"),
      Www(Fi, "http://www.salpaus.fi"))

    assert(OrganisaatioServiceUtil.filterByOsoitetyyppi(yhteystiedot, "posti") == List())
  }

  "toOsoite" should "return käyntiosoite in Fi and postinumerokoodiuri" in {
    val kayntiosoitteet = List(OrgOsoite("kaynti", Fi, "Hankalankuja 228", Some("posti_15110")))

    assert(OrganisaatioServiceUtil.toOsoite(kayntiosoitteet).contains(Osoite(Map(Fi -> "Hankalankuja 228"), Map(Fi -> "posti_15110"))))
  }

  it should "return käyntiosoite in Fi and Sv and postinumerokoodiuri" in {
    val kayntiosoitteet = List(
      OrgOsoite("kaynti", Fi, "Hankalankuja 228", Some("posti_15110")),
      OrgOsoite("kaynti", Sv, "Högskolavägen 228", Some("posti_15111")))

    assert(OrganisaatioServiceUtil.toOsoite(kayntiosoitteet).contains(Osoite(Map(Fi -> "Hankalankuja 228", Sv -> "Högskolavägen 228"), Map(Fi -> "posti_15110", Sv -> "posti_15111"))))
  }

  it should "return käyntiosoite in Fi and Sv and postinumerokoodiuri only for Fi" in {
    val kayntiosoitteet = List(
      OrgOsoite("kaynti", Fi, "Hankalankuja 228", Some("posti_15110")),
      OrgOsoite("kaynti", Sv, "Högskolavägen 228", None))

    assert(OrganisaatioServiceUtil.toOsoite(kayntiosoitteet).contains(Osoite(Map(Fi -> "Hankalankuja 228", Sv -> "Högskolavägen 228"), Map(Fi -> "posti_15110"))))
  }

  it should "return käyntiosoite without postinumero" in {
    val kayntiosoitteet = List(
      OrgOsoite("kaynti", Fi, "Hankalankuja 228", None),
      OrgOsoite("kaynti", Sv, "Högskolavägen 228", None))

    assert(OrganisaatioServiceUtil.toOsoite(kayntiosoitteet).contains(
      Osoite(Map(Fi -> "Hankalankuja 228", Sv -> "Högskolavägen 228"), Map())))
  }

  it should "return None if there is no osoite" in {
    assert(OrganisaatioServiceUtil.toOsoite(List()).isEmpty)
  }

  "toYhteystieto" should "return Yhteystieto when given organisaation yhteystiedot from organisaatio-service" in {
    val nimi = Map(Fi -> "Koulutuskeskus fi", Sv -> "Koulutuskeskus sv", En -> "Koulutuskeskus en")
    val yhteystiedot = List(
      Email(Fi, "koulutus@opisto.fi"),
      OrgOsoite("kaynti", Fi, "Hankalankuja 228", Some("posti_15110")),
      Puhelin(Fi, "050 44042961"),
      OrgOsoite("posti", Fi, "Jalanluiskahtamavaarankuja 580", Some("posti_15110")),
      OrgOsoite("posti", Sv, "Jalanluiskahtamavaaravägen 581", Some("posti_15111")),
      Www(Fi, "http://www.salpaus.fi"))

    assert(OrganisaatioServiceUtil.toYhteystieto(nimi, yhteystiedot).contains(Yhteystieto(
      nimi = Map(Fi -> "Koulutuskeskus fi", Sv -> "Koulutuskeskus sv", En -> "Koulutuskeskus en"),
      postiosoite = Some(Osoite(
        osoite = Map(Fi -> "Jalanluiskahtamavaarankuja 580", Sv -> "Jalanluiskahtamavaaravägen 581"),
        postinumeroKoodiUri = Map(Fi -> "posti_15110", Sv -> "posti_15111"))),
      kayntiosoite = Some(Osoite(osoite = Map(Fi -> "Hankalankuja 228"), postinumeroKoodiUri = Map(Fi -> "posti_15110"))),
      puhelinnumero = Map(Fi -> "050 44042961"),
      sahkoposti = Map(Fi -> "koulutus@opisto.fi"),
      www = Map(Fi -> "http://www.salpaus.fi")
    ))
    )
  }

  it should "return None when organisaatio has no yhteystiedot" in {
    val nimi = Map(Fi -> "Koulutuskeskus fi", Sv -> "Koulutuskeskus sv", En -> "Koulutuskeskus en")
    val yhteystiedot = List()

    assert(OrganisaatioServiceUtil.toYhteystieto(nimi, yhteystiedot).isEmpty)
  }

  "getParentOids" should "return empty list when parentOidPath is empty string" in {
    assert(OrganisaatioServiceUtil.getParentOids("") == List())
  }

  it should "return one oid when path consists of one oid" in {
    assert(OrganisaatioServiceUtil.getParentOids("1.2.246.562.10.44413919323") == List(OrganisaatioOid("1.2.246.562.10.44413919323")))
  }

  it should "return one oid when path consists of one oid preceded and followed by |" in {
    assert(OrganisaatioServiceUtil.getParentOids("|1.2.246.562.10.44413919323|") == List(OrganisaatioOid("1.2.246.562.10.44413919323")))
  }

  it should "return two oids when path consists of two oids preceded and followed by |" in {
    assert(OrganisaatioServiceUtil.getParentOids("|1.2.246.562.10.00000000001|1.2.246.562.10.44413919323|") == List(OrganisaatioOid("1.2.246.562.10.00000000001"), OrganisaatioOid("1.2.246.562.10.44413919323")))
  }

  it should "return two oids when path consists of two oids separated by /" in {
    assert(OrganisaatioServiceUtil.getParentOids("1.2.246.562.10.00000000001/1.2.246.562.10.44413919323") == List(OrganisaatioOid("1.2.246.562.10.00000000001"), OrganisaatioOid("1.2.246.562.10.44413919323")))
  }

  "organisaatioServiceOrgToOrganisaatio" should "return basic organisaatio data with children" in {
    assert(OrganisaatioServiceUtil.organisaatioServiceOrgToOrganisaatio(orgServiceHierarkiaOrganisaatio) == Organisaatio(
      oid = TestOids.ChildOid.toString,
      parentOid = Some(TestOids.ParentOid),
      parentOids = List(TestOids.ChildOid, TestOids.ParentOid, TestOids.OphOid),
      oppilaitostyyppiUri = Some("oppilaitostyyppi_64"),
      nimi = Map(Fi -> "Oppilaitos fi", Sv -> "Oppilaitos sv", En -> "Oppilaitos en"),
      status = "AKTIIVINEN",
      organisaatiotyyppiUris = Some(List("organisaatiotyyppi_04")),
      children = Some(List(Organisaatio(
        oid = TestOids.GrandChildOid.toString,
        parentOids = List(TestOids.ChildOid, TestOids.ParentOid, TestOids.OphOid),
        oppilaitostyyppiUri =  Some("oppilaitostyyppi_63"),
        nimi = Map(Fi -> "Oppilaitoksen osa fi", Sv -> "Oppilaitoksen osa sv", En -> "Oppilaitoksen osa en"),
        status = "AKTIIVINEN",
        organisaatiotyyppiUris = Some(List("organisaatiotyyppi_03")),
        children = None
      )))))
  }
}
