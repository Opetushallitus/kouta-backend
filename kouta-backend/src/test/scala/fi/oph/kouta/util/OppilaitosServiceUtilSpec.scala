package fi.oph.kouta.util

import fi.oph.kouta.TestOids
import fi.oph.kouta.domain._

class OppilaitosServiceUtilSpec extends UnitSpec {
  val organisaationOsa = OrganisaatioHierarkiaOrg(
    oid = TestOids.GrandChildOid.toString,
    parentOidPath = s"${TestOids.GrandChildOid.toString}/${TestOids.ChildOid}/${TestOids.ParentOid}/${TestOids.OphOid}",
    nimi = Map(Fi -> "Oppilaitoksen osa 1 fi", Sv -> "Oppilaitoksen osa 1 sv", En -> "Oppilaitoksen osa 1 en"),
    kotipaikkaUri = Some("kunta_179"),
    status = "AKTIIVINEN",
    children = List(),
    organisaatiotyypit = List("organisaatiotyyppi_03"))

  val organisaatio = Organisaatio(
    oid = TestOids.ChildOid.toString,
    parentOidPath = s"${TestOids.ChildOid}/${TestOids.ParentOid}/${TestOids.OphOid}",
    oppilaitostyyppi = Some("oppilaitostyyppi_63#1"),
    nimi = Map(Fi -> "Oppilaitos fi", Sv -> "Oppilaitos sv", En -> "Oppilaitos en"),
    yhteystiedot = List(Some(OrgOsoite(osoiteTyyppi = "kaynti", kieli = Fi, osoite = "Opistokatu 1", postinumeroUri = Some("posti_90500")))),
    kotipaikkaUri = Some("kunta_595"),
    status = "AKTIIVINEN",
    organisaatiotyypit = List("organisaatiotyyppi_03"))

  val hierarkiaorganisaatio = OrganisaatioHierarkiaOrg(
    oid = TestOids.ChildOid.toString,
    parentOidPath = s"${TestOids.ChildOid}/${TestOids.ParentOid}/${TestOids.OphOid}",
    oppilaitostyyppi = Some("oppilaitostyyppi_63#1"),
    nimi = Map(Fi -> "Oppilaitos fi", Sv -> "Oppilaitos sv", En -> "Oppilaitos en"),
    status = "AKTIIVINEN",
    kotipaikkaUri = Some("kunta_179"),
    children = List(),
    organisaatiotyypit = List("organisaatiotyyppi_03"))

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
    assert(OppilaitosServiceUtil.getHierarkiaOids(OrganisaatioHierarkia(organisaatiot = List(hierarkiaorganisaatio))) == List(TestOids.ChildOid))
  }

  it should "return two oids for two orgs without children" in {
    assert(OppilaitosServiceUtil.getHierarkiaOids(
      OrganisaatioHierarkia(
        organisaatiot = List(hierarkiaorganisaatio, hierarkiaorganisaatio.copy(oid = TestOids.EvilChildOid.toString)))) == List(TestOids.ChildOid, TestOids.EvilChildOid))
  }

  it should "return distinct oids for all the children recursively" in {
    val organisaatioHierarkia = OrganisaatioHierarkia(organisaatiot = List(
      hierarkiaorganisaatio.copy(
        children = List(
          organisaationOsa)),
      hierarkiaorganisaatio.copy(
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

  val yhteystiedot = List(
    Some(Email(Fi, "koulutus@opisto.fi")),
    None,
    Some(OrgOsoite("kaynti", Fi, "Hankalankuja 228", Some("posti_15110"))),
    Some(Puhelin(Fi, "050 44042961")),
    Some(OrgOsoite("posti", Fi, "Jalanluiskahtamavaarankuja 580", Some("posti_15110"))),
    Some(OrgOsoite("posti", Sv, "Jalanluiskahtamavaaravägen 581", Some("posti_15110"))),
    Some(Www(Fi, "http://www.salpaus.fi")))

  "filterByOsoitetyyppi" should "return postiosoitteet from a list of yhteystiedot" in {
    assert(OppilaitosServiceUtil.filterByOsoitetyyppi(yhteystiedot, "posti") == List(
      OrgOsoite("posti", Fi, "Jalanluiskahtamavaarankuja 580", Some("posti_15110")),
      OrgOsoite("posti", Sv, "Jalanluiskahtamavaaravägen 581", Some("posti_15110"))))
  }

  it should "return käyntiosoite from the list of yhteystiedot" in {
    assert(OppilaitosServiceUtil.filterByOsoitetyyppi(yhteystiedot, "kaynti") ==
      List(OrgOsoite("kaynti", Fi, "Hankalankuja 228", Some("posti_15110"))))
  }

  it should "return empty list as there is no postiosoite in yhteystiedot" in {
    val yhteystiedot = List(
      Some(Email(Fi, "koulutus@opisto.fi")),
      None,
      Some(OrgOsoite("kaynti", Fi, "Hankalankuja 228", Some("posti_15110"))),
      Some(Puhelin(Fi, "050 44042961")),
      Some(Www(Fi, "http://www.salpaus.fi")))

    assert(OppilaitosServiceUtil.filterByOsoitetyyppi(yhteystiedot, "posti") == List())
  }

  "toOsoite" should "return käyntiosoite in Fi and postinumerokoodiuri" in {
    val kayntiosoitteet = List(OrgOsoite("kaynti", Fi, "Hankalankuja 228", Some("posti_15110")))

    assert(OppilaitosServiceUtil.toOsoite(kayntiosoitteet) == Some(Osoite(Map(Fi -> "Hankalankuja 228"), Some("posti_15110"))))
  }

  it should "return käyntiosoite in Fi and Sv and postinumerokoodiuri" in {
    val kayntiosoitteet = List(OrgOsoite("kaynti", Fi, "Hankalankuja 228", Some("posti_15110")), OrgOsoite("kaynti", Sv, "Högskolavägen 228", Some("posti_15110")))

    assert(OppilaitosServiceUtil.toOsoite(kayntiosoitteet) == Some(Osoite(Map(Fi -> "Hankalankuja 228", Sv -> "Högskolavägen 228"), Some("posti_15110"))))
  }

  it should "return None if there is no osoite" in {
    assert(OppilaitosServiceUtil.toOsoite(List()) == None)
  }

  "toYhteystieto" should "return Yhteystieto when given organisaation yhteystiedot from organisaatio-service" in {
    val nimi = Map(Fi -> "Koulutuskeskus fi", Sv -> "Koulutuskeskus sv", En -> "Koulutuskeskus en")
    val yhteystiedot = List(
      Some(Email(Fi, "koulutus@opisto.fi")),
      None,
      Some(OrgOsoite("kaynti", Fi, "Hankalankuja 228", Some("posti_15110"))),
      Some(Puhelin(Fi, "050 44042961")),
      Some(OrgOsoite("posti", Fi, "Jalanluiskahtamavaarankuja 580", Some("posti_15110"))),
      Some(OrgOsoite("posti", Sv, "Jalanluiskahtamavaaravägen 581", Some("posti_15110"))),
      Some(Www(Fi, "http://www.salpaus.fi")))

    assert(OppilaitosServiceUtil.toYhteystieto(nimi, yhteystiedot) ==
      Some(Yhteystieto(
        nimi = Map(Fi -> "Koulutuskeskus fi", Sv -> "Koulutuskeskus sv", En -> "Koulutuskeskus en"),
        postiosoite = Some(Osoite(osoite = Map(Fi -> "Jalanluiskahtamavaarankuja 580", Sv -> "Jalanluiskahtamavaaravägen 581"), postinumeroKoodiUri = Some("posti_15110"))),
        kayntiosoite = Some(Osoite(osoite = Map(Fi -> "Hankalankuja 228"), postinumeroKoodiUri = Some("posti_15110"))),
        puhelinnumero = Map(Fi -> "050 44042961"),
        sahkoposti = Map(Fi -> "koulutus@opisto.fi")))
    )
  }
}
