package fi.oph.kouta.integration

import fi.oph.kouta.TestData
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.security.{Role, RoleEntity}
import org.json4s.jackson.Serialization.read

class ListSpec extends KoutaIntegrationSpec with AccessControlSpec with EverythingFixture {

  override val roleEntities = RoleEntity.all

  var k1, k2, k3, k4, k5, k6 :KoulutusListItem = null
  var t1, t2, t3, t4         :ToteutusListItem = null
  var h1, h2, h3, h4         :HakuListItem = null
  var v1, v2, v3, v4         :ValintaperusteListItem = null
  var s1, s2, s3             :SorakuvausListItem = null
  var hk1, hk2, hk3, hk4     :HakukohdeListItem = null
  var o1, o2                 :OrganisaatioOid = null
  var oo1, oo2, oo3          :OppilaitoksenOsaListItem = null

  override def beforeAll(): Unit = {
    super.beforeAll()

    mockOrganisaatioResponse(UnknownOid, NotFoundOrganisaatioResponse)

    createTestData()
  }

  def createTestData(): Unit = {
    k1 = addToList(koulutus(false, ParentOid, Julkaistu))
    k2 = addToList(koulutus(false, ChildOid, Arkistoitu))
    k3 = addToList(koulutus(false, GrandChildOid, Tallennettu))
    k4 = addToList(koulutus(false, LonelyOid, Julkaistu))
    k5 = addToList(koulutus(true, LonelyOid, Julkaistu))
    k6 = addToList(yoKoulutus.copy(julkinen = true, organisaatioOid = UnknownOid, tila = Julkaistu))
    t1 = addToList(toteutus(k1.oid.toString, Julkaistu, ParentOid))
    t2 = addToList(toteutus(k1.oid.toString, Arkistoitu, ChildOid))
    t3 = addToList(toteutus(k1.oid.toString, Tallennettu, GrandChildOid))
    t4 = addToList(toteutus(k4.oid.toString, Julkaistu, LonelyOid))
    h1 = addToList(haku(Julkaistu, ParentOid))
    h2 = addToList(haku(Arkistoitu, ChildOid))
    h3 = addToList(haku(Tallennettu, GrandChildOid).copy(kohdejoukkoKoodiUri = Some("haunkohdejoukko_05#2"), kohdejoukonTarkenneKoodiUri = None))
    h4 = addToList(haku(Julkaistu, LonelyOid))
    s1 = addToList(sorakuvaus(Julkaistu, ParentOid))
    s2 = addToList(sorakuvaus(Arkistoitu, ChildOid))
    s3 = addToList(sorakuvaus(Julkaistu, LonelyOid))
    v1 = addToList(valintaperuste(Julkaistu, ParentOid).copy(sorakuvausId = Some(s1.id)))
    v2 = addToList(valintaperuste(Arkistoitu, ChildOid).copy(sorakuvausId = Some(s1.id)))
    v3 = addToList(valintaperuste(Tallennettu, GrandChildOid).copy(kohdejoukkoKoodiUri = Some("haunkohdejoukko_05#2"), kohdejoukonTarkenneKoodiUri = None))
    v4 = addToList(valintaperuste(Julkaistu, LonelyOid).copy(sorakuvausId = Some(s3.id)))
    hk1 = addToList(hakukohde(t1.oid, h1.oid, v1.id, ParentOid))
    hk2 = addToList(hakukohde(t2.oid, h1.oid, v1.id, ChildOid))
    hk3 = addToList(hakukohde(t1.oid, h2.oid, v1.id, GrandChildOid))
    hk4 = addToList(hakukohde(t4.oid, h1.oid, v1.id, LonelyOid))

    o1 = OrganisaatioOid(put(oppilaitos(Julkaistu, ParentOid)))
    o2 = OrganisaatioOid(put(oppilaitos(Julkaistu, EvilChildOid)))

    oo1 = addToList(oppilaitoksenOsa(o1, Julkaistu, ChildOid))
    oo2 = addToList(oppilaitoksenOsa(o1, Julkaistu, GrandChildOid))
    oo3 = addToList(oppilaitoksenOsa(o2, Julkaistu, EvilGrandChildOid))
  }

  "Koulutus list" should "list all koulutukset for authorized organizations 1" in {
    list(KoulutusPath, Map("organisaatioOid" -> ChildOid.s), List(k2, k3, k5))
  }
  it should "list all koulutukset for authorized organizations 2" in {
    list(KoulutusPath, Map("organisaatioOid" -> LonelyOid.s), List(k4, k5))
  }
  it should "return forbidden if oid is unknown" in {
    list(KoulutusPath, Map("organisaatioOid" -> UnknownOid.s), 403)
  }
  it should "return 404 if oid not given" in {
    list(KoulutusPath, Map[String, String](), 404)
  }
  it should "return 401 if no session is found" in {
    list(KoulutusPath, Map("organisaatioOid" -> LonelyOid.s), 401, Map())
  }
  it should "allow access to user of the selected organization" in {
    list(KoulutusPath, Map("organisaatioOid" -> ChildOid.s), List(k2, k3, k5), crudSessions(ChildOid))
  }
  it should "deny access without access to the given organization" in {
    list(KoulutusPath, Map("organisaatioOid" -> ChildOid.s), 403, crudSessions(LonelyOid))
  }
  it should "allow access for a user of an ancestor organization" in {
    list(KoulutusPath, Map("organisaatioOid" -> ChildOid.s), List(k2, k3, k5), crudSessions(ParentOid))
  }
  it should "deny access for a user of a descendant organization" in {
    list(KoulutusPath, Map("organisaatioOid" -> ChildOid.s), 403, crudSessions(GrandChildOid))
  }
  it should "deny access without an accepted role" in {
    list(KoulutusPath, Map("organisaatioOid" -> ChildOid.s), 403, otherRoleSession)
  }
  it should "allow access to any koulutus with the indexer role" in {
    list(KoulutusPath, Map("organisaatioOid" -> LonelyOid.s), List(k4, k5), indexerSession)
  }
  it should "list public koulutus with the same koulutustyyppi" in {
    list(KoulutusPath, Map("organisaatioOid" -> YoOid.s), List(k6), readSessions(YoOid))
  }

  "Toteutus list" should "list all toteutukset for selected organization" in {
    list(ToteutusPath, Map("organisaatioOid" -> ChildOid.s), List(t2, t3))
  }
  it should "list all toteutukset for selected organization 2" in {
    list(ToteutusPath, Map("organisaatioOid" -> LonelyOid.s), List(t4))
  }
  it should "return forbidden if oid is unknown" in {
    list(ToteutusPath, Map("organisaatioOid" -> UnknownOid.s), 403)
  }
  it should "return 404 if oid not given" in {
    list(ToteutusPath, Map[String,String](), 404)
  }
  it should "return 401 if oid not given" in {
    list(ToteutusPath, Map[String,String](), 401, Map.empty)
  }
  it should "allow access to user of the selected organization" in {
    list(ToteutusPath, Map("organisaatioOid" -> ChildOid.s), List(t2, t3), crudSessions(ChildOid))
  }
  it should "deny access without access to the given organization" in {
    list(ToteutusPath, Map("organisaatioOid" -> ChildOid.s), 403, crudSessions(LonelyOid))
  }
  it should "allow access for a user of an ancestor organization" in {
    list(ToteutusPath, Map("organisaatioOid" -> ChildOid.s), List(t2, t3), crudSessions(ParentOid))
  }
  it should "deny access for a user of a descendant organization" in {
    list(ToteutusPath, Map("organisaatioOid" -> ChildOid.s), 403, crudSessions(GrandChildOid))
  }
  it should "deny access without an accepted role" in {
    list(ToteutusPath, Map("organisaatioOid" -> ChildOid.s), 403, otherRoleSession)
  }
  it should "allow access to any toteutus with the indexer role" in {
    list(ToteutusPath, Map("organisaatioOid" -> ChildOid.s), List(t2, t3), indexerSession)
  }

  "Haku list" should "list all haut for authorized organizations" in {
    list(HakuPath, Map("organisaatioOid" -> ChildOid.s), List(h2, h3))
  }
  it should "list all haut for authorized organizations 2" in {
    list(HakuPath, Map("organisaatioOid" -> LonelyOid.s), List(h4))
  }
  it should "return forbidden if oid is unknown" in {
    list(HakuPath, Map("organisaatioOid" -> UnknownOid.s), 403)
  }
  it should "return 404 if oid not given" in {
    list(HakuPath, Map[String,String](), 404)
  }
  it should "return 401 if session is not valid" in {
    list(HakuPath, Map[String,String](), 401, Map.empty)
  }
  it should "allow access to user of the selected organization" in {
    list(HakuPath, Map("organisaatioOid" -> ChildOid.s), List(h2, h3), crudSessions(ChildOid))
  }
  it should "deny access without access to the given organization" in {
    list(HakuPath, Map("organisaatioOid" -> ChildOid.s), 403, crudSessions(LonelyOid))
  }
  it should "allow access for a user of an ancestor organization" in {
    list(HakuPath, Map("organisaatioOid" -> ChildOid.s), List(h2, h3), crudSessions(ParentOid))
  }
  it should "deny access for a user of a descendant organization" in {
    list(HakuPath, Map("organisaatioOid" -> ChildOid.s), 403, crudSessions(GrandChildOid))
  }
  it should "deny access without an accepted role" in {
    list(HakuPath, Map("organisaatioOid" -> ChildOid.s), 403, otherRoleSession)
  }
  it should "allow access to any haku with the indexer role" in {
    list(HakuPath, Map("organisaatioOid" -> ChildOid.s), List(h2, h3), indexerSession)
  }

  "Valintaperuste list" should "list all valintaperustekuvaukset for authorized organizations" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s), List(v2, v3))
  }
  it should "list all valintaperustekuvaukset for authorized organizations 2" in {
    list(ValintaperustePath, Map("organisaatioOid" -> LonelyOid.s), List(v4))
  }
  it should "list all valintaperustekuvaukset that can be joined to given haku" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s, "hakuOid" -> h2.oid.toString), List(v2))
  }
  it should "list all valinteperustekuvaukset that can be joiden to given haku even when kohdejoukko is null" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s, "hakuOid" -> h3.oid.toString), List(v3))
  }
  it should "return forbidden if oid is unknown" in {
    list(ValintaperustePath, Map("organisaatioOid" -> UnknownOid.s), 403)
  }
  it should "return 404 if oid not given" in {
    list(ValintaperustePath, Map[String,String](), 404)
  }
  it should "return 401 if session is not valid" in {
    list(ValintaperustePath, Map[String,String](), 401, Map.empty)
  }
  it should "allow access to user of the selected organization" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s), List(v2, v3), crudSessions(ChildOid))
  }
  it should "deny access without access to the given organization" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s), 403, crudSessions(LonelyOid))
  }
  it should "allow access for a user of an ancestor organization" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s), List(v2, v3), crudSessions(ParentOid))
  }
  it should "deny access for a user of a descendant organization" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s), 403, crudSessions(GrandChildOid))
  }
  it should "deny access without an accepted role" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s), 403, otherRoleSession)
  }
  it should "allow access to any valintaperuste with the indexer role" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s), List(v2, v3), indexerSession)
  }

  "Sorakuvaus list" should "list all sorakuvaukset for authorized organisations" in {
    list(SorakuvausPath, Map("organisaatioOid" -> ChildOid.s), List(s2))
  }
  it should "list all sorakuvaukset for authorized organizations 2" in {
    list(SorakuvausPath, Map("organisaatioOid" -> LonelyOid.s), List(s3))
  }
  it should "return forbidden if oid is unknown" in {
    list(SorakuvausPath, Map("organisaatioOid" -> UnknownOid.s), 403)
  }
  it should "return 404 if oid not given" in {
    list(SorakuvausPath, Map[String,String](), 404)
  }
  it should "return 401 if session is not valid" in {
    list(SorakuvausPath, Map[String,String](), 401, Map.empty)
  }
  it should "allow access to user of the selected organization" in {
    list(SorakuvausPath, Map("organisaatioOid" -> ChildOid.s), List(s2), crudSessions(ChildOid))
  }
  it should "deny access without access to the given organization" in {
    list(SorakuvausPath, Map("organisaatioOid" -> ChildOid.s), 403, crudSessions(LonelyOid))
  }
  it should "allow access for a user of an ancestor organization" in {
    list(SorakuvausPath, Map("organisaatioOid" -> ChildOid.s), List(s2), crudSessions(ParentOid))
  }
  it should "deny access for a user of a descendant organization" in {
    list(SorakuvausPath, Map("organisaatioOid" -> ChildOid.s), 403, crudSessions(GrandChildOid))
  }
  it should "deny access without an accepted role" in {
    list(SorakuvausPath, Map("organisaatioOid" -> ChildOid.s), 403, otherRoleSession)
  }
  it should "allow access to any sorakuvaus with the indexer role" in {
    list(SorakuvausPath, Map("organisaatioOid" -> ChildOid.s), List(s2), indexerSession)
  }

  "Valintaperustetta käyttävät hakukohteet list" should "list all hakukohteet using given valintaperuste id" in {
    list(s"$ValintaperustePath/${v1.id.toString}/hakukohteet", Map[String,String](), List(hk1, hk2, hk3, hk4))
  }
  it should "return 401 if session is not valid" in {
    list(s"$ValintaperustePath/${v1.id.toString}/hakukohteet", Map[String,String](), 401, Map.empty)
  }
  it should "deny access to non-root users" in {
    list(s"$ValintaperustePath/${v1.id.toString}/hakukohteet", Map[String,String](), 403, crudSessions(v1.organisaatioOid))
  }
  it should "allow access to the indexer" in {
    list(s"$ValintaperustePath/${v1.id.toString}/hakukohteet", Map[String,String](), List(hk1, hk2, hk3, hk4), indexerSession)
  }

  "Koulutuksen toteutukset list" should "list all toteutukset for this and child organizations" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> ParentOid.s), List(t1, t2, t3))
  }
  it should "not list toteutukset for parent organizations" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> GrandChildOid.s), List(t3))
  }
  it should "return 401 if no session is found" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> ParentOid.s), 401, Map.empty)
  }
  it should "return forbidden if organisaatio oid is unknown" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> UnknownOid.s), 403)
  }
  it should "return all toteutukset if organisaatio oid not given" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map[String,String](), List(t1, t2, t3))
  }
  it should "deny access to all toteutukset without root organization access" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map[String, String](), 403, crudSessions(ParentOid))
  }
  it should "allow access to a user of the koulutus organization" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> k1.organisaatioOid.s), List(t1, t2, t3), crudSessions(k1.organisaatioOid))
  }
  it should "deny access to a user without access to the koulutus organization" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> k1.organisaatioOid.s), 403, crudSessions(LonelyOid))
  }
  it should "allow access to a user of an ancestor organization" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> k3.organisaatioOid.s), List(t3), crudSessions(ParentOid))
  }
  it should "deny access to a user of a descendant organization" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> k1.organisaatioOid.s), 403, crudSessions(GrandChildOid))
  }
  it should "deny access without the toteutus read role" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> ChildOid.s), 403, addTestSession(Role.Koulutus.Read, OphOid))
  }
  it should "allow access to the indexer" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map[String, String](), List(t1, t2, t3), indexerSession)
  }

  "Toteutukseen liitetyt haut" should "list all haut mapped to given toteutus" in {
    list(s"$ToteutusPath/${t1.oid}/haut", Map[String,String](), List(h1, h2))
  }
  it should "return 401 if no session is found" in {
    list(s"$ToteutusPath/${t1.oid}/haut", Map[String,String](), 401, Map.empty)
  }
  it should "deny access to a non-root user, even if they own the toteutus" in {
    list(s"$ToteutusPath/${t2.oid}/haut", Map.empty[String, String], 403, crudSessions(ChildOid))
  }
  it should "deny access without access to the toteutus organization" in {
    list(s"$ToteutusPath/${t2.oid}/haut", Map.empty[String, String], 403, crudSessions(LonelyOid))
  }
  it should "deny access for a non-root user of an ancestor organization" in {
    list(s"$ToteutusPath/${t2.oid}/haut", Map.empty[String, String], 403, crudSessions(ParentOid))
  }
  it should "deny access for a user of a descendant organization" in {
    list(s"$ToteutusPath/${t2.oid}/haut", Map.empty[String, String], 403, crudSessions(GrandChildOid))
  }
  it should "deny access without the haku read role" in {
    list(s"$ToteutusPath/${t2.oid}/haut", Map.empty[String, String], 403, addTestSession(Role.Toteutus.Read, OphOid))
  }
  it should "allow access to the haut of any toteutus with the indexer role" in {
    list(s"$ToteutusPath/${t2.oid}/haut", Map.empty[String, String], List(h1), indexerSession)
  }

  "Toteutukseen liitetyt hakukohteet" should "list all hakukohteet mapped to given toteutus" in {
    list(s"$ToteutusPath/${t1.oid}/hakukohteet", Map[String,String]("organisaatioOid" -> t1.organisaatioOid.s), List(hk1, hk3))
  }
  it should "return 401 if no session is found" in {
    list(s"$ToteutusPath/${t1.oid}/hakukohteet", Map.empty[String,String], 401, Map.empty)
  }
  it should "deny access to a non-root user of the toteutus organization" in {
    list(s"$ToteutusPath/${t2.oid}/hakukohteet", Map[String, String]("organisaatioOid" -> ChildOid.s), List.empty[Hakukohde], crudSessions(ChildOid))
  }
  it should "deny access without access to the toteutus organization" in {
    list(s"$ToteutusPath/${t2.oid}/hakukohteet", Map[String, String]("organisaatioOid" -> LonelyOid.s), List.empty[Hakukohde], crudSessions(LonelyOid))
  }
  it should "deny access for a non-root user of an ancestor organization" in {
    list(s"$ToteutusPath/${t2.oid}/hakukohteet", Map[String, String]("organisaatioOid" -> ParentOid.s), List.empty[Hakukohde], crudSessions(ParentOid))
  }
  it should "deny access for a user of a descendant organization" in {
    list(s"$ToteutusPath/${t2.oid}/hakukohteet", Map[String, String]("organisaatioOid" -> GrandChildOid.s), List.empty[Hakukohde], crudSessions(GrandChildOid))
  }
  it should "deny access without the hakukohde read role" in {
    list(s"$ToteutusPath/${t2.oid}/hakukohteet", Map[String, String]("organisaatioOid" -> OphOid.s), List.empty[Hakukohde], addTestSession(Role.Toteutus.Read, OphOid))
  }
  it should "allow access to the hakukohteet of any toteutus with the indexer role" in {
    list(s"$ToteutusPath/${t2.oid}/hakukohteet", Map[String, String]("organisaatioOid" -> indexerSession.toString), List(hk2), indexerSession)
  }

  "Hakuun liitetyt hakukohteet" should "list all hakukohteet mapped to given haku for authorized organizations" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map("organisaatioOid" -> ParentOid.s), List(hk1, hk2))
  }
  it should "list all hakukohteet mapped to given haku for authorized organizations 2" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map("organisaatioOid" -> LonelyOid.s), List(hk4))
  }
  it should "not list hakukohteet belonging to parent organisations" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map("organisaatioOid" -> ChildOid.s), List(hk2))
  }
  it should "return forbidden if organisaatio oid is unknown" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map("organisaatioOid" -> UnknownOid.s), 403)
  }
  it should "return all if organisaatio oid not given" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map[String,String](), List(hk1, hk2, hk4))
  }
  it should "return 401 if there's no valid session" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map[String,String](), 401, Map.empty)
  }
  it should "deny access to all hakukohteet without root organization access" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map[String, String](), 403, crudSessions(ParentOid))
  }
  it should "allow access to a user of the haku organization" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map("organisaatioOid" -> h1.organisaatioOid.s), List(hk1, hk2), crudSessions(h1.organisaatioOid))
  }
  it should "deny access to a user without access to the haku organization" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map("organisaatioOid" -> h1.organisaatioOid.s), 403, crudSessions(LonelyOid))
  }
  it should "allow access to a user of an ancestor organization" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map("organisaatioOid" -> h2.organisaatioOid.s), List(hk2), crudSessions(ParentOid))
  }
  it should "deny access to a user of a descendant organization" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map("organisaatioOid" -> h1.organisaatioOid.s), 403, crudSessions(GrandChildOid))
  }
  it should "deny access without the hakukohde read role" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map("organisaatioOid" -> ChildOid.s), 403, addTestSession(Role.Haku.Read, OphOid))
  }
  it should "allow access to the indexer" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map[String, String](), List(hk1, hk2, hk4), indexerSession)
  }

  "Hakuun kuuluvat koulutukset" should "list all koulutukset mapped to given haku by hakukohde" in {
    list(s"$HakuPath/${h1.oid}/koulutukset", Map[String,String](), List(k1, k4))
  }
  it should "return 401 if there's no valid session" in {
    list(s"$HakuPath/${h1.oid}/koulutukset", Map[String,String](), 401, Map.empty)
  }
  it should "deny access to all koulutukset without root organization access" in {
    list(s"$HakuPath/${h1.oid}/koulutukset", Map[String, String](), 403, crudSessions(ParentOid))
  }
  it should "deny access to a non-root user of the haku organization" in {
    list(s"$HakuPath/${h1.oid}/koulutukset", Map("organisaatioOid" -> h1.organisaatioOid.s), 403, crudSessions(h1.organisaatioOid))
  }
  it should "deny access to a user without access to the haku organization" in {
    list(s"$HakuPath/${h1.oid}/koulutukset", Map("organisaatioOid" -> h1.organisaatioOid.s), 403, crudSessions(LonelyOid))
  }
  it should "deny access to a non-root user of an ancestor organization" in {
    list(s"$HakuPath/${h1.oid}/koulutukset", Map("organisaatioOid" -> h2.organisaatioOid.s), 403, crudSessions(ParentOid))
  }
  it should "deny access to a user of a descendant organization" in {
    list(s"$HakuPath/${h1.oid}/koulutukset", Map("organisaatioOid" -> h1.organisaatioOid.s), 403, crudSessions(GrandChildOid))
  }
  it should "deny access without the koulutus read role" in {
    list(s"$HakuPath/${h1.oid}/koulutukset", Map("organisaatioOid" -> ChildOid.s), 403, addTestSession(Role.Haku.Read, OphOid))
  }
  it should "allow access to the indexer" in {
    list(s"$HakuPath/${h1.oid}/koulutukset", Map[String, String](), List(k1, k4), indexerSession)
  }

  "Sorakuvausta käyttävät valintaperusteet" should "list all valintaperusteet using given sorakuvaus" in {
    list(s"$SorakuvausPath/${s1.id}/valintaperusteet", Map[String,String](), List(v1, v2))
  }
  it should "list all valintaperusteet using given sorakuvaus 2" in {
    list(s"$SorakuvausPath/${s3.id}/valintaperusteet", Map[String,String](), List(v4))
  }
  it should "deny access to a non-root user, even if they own the toteutus" in {
    list(s"$SorakuvausPath/${s2.id}/valintaperusteet", Map.empty[String, String], 403, crudSessions(ChildOid))
  }
  it should "deny access without access to the toteutus organization" in {
    list(s"$SorakuvausPath/${s2.id}/valintaperusteet", Map.empty[String, String], 403, crudSessions(LonelyOid))
  }
  it should "deny access for a non-root user of an ancestor organization" in {
    list(s"$SorakuvausPath/${s2.id}/valintaperusteet", Map.empty[String, String], 403, crudSessions(ParentOid))
  }
  it should "deny access for a user of a descendant organization" in {
    list(s"$SorakuvausPath/${s2.id}/valintaperusteet", Map.empty[String, String], 403, crudSessions(GrandChildOid))
  }
  it should "deny access without the valintaperuste read role" in {
    list(s"$SorakuvausPath/${s2.id}/valintaperusteet", Map.empty[String, String], 403, addTestSession(Role.Toteutus.Read, OphOid))
  }
  it should "allow access with the valintaperuste read role" in {
    list(s"$SorakuvausPath/${s2.id}/valintaperusteet", Map.empty[String, String], List(), addTestSession(Role.Valintaperuste.Read, OphOid))
  }
  it should "allow access to the haut of any toteutus with the indexer role" in {
    list(s"$SorakuvausPath/${s2.id}/valintaperusteet", Map.empty[String, String], List(), indexerSession)
  }

  "Oppilaitoksen osat list" should "list all oppilaitoksen osat for this oppilaitos" in {
    list(s"$OppilaitosPath/${o1.s}/osat", Map("organisaatioOid" -> ParentOid.s), List(oo1, oo2))
  }
  it should "return 401 if no session is found" in {
    list(s"$OppilaitosPath/${o1.s}/osat", Map("organisaatioOid" -> ParentOid.s), 401, Map.empty)
  }
  it should "return forbidden if organisaatio oid is unknown" in {
    list(s"$OppilaitosPath/${o1.s}/osat", Map("organisaatioOid" -> UnknownOid.s), 403)
  }
  it should "deny access to all oppilaitosten osat without root organization access" in {
    list(s"$OppilaitosPath/${o1.s}/osat", Map[String, String](), 403, crudSessions(ParentOid))
  }
  it should "deny access to a user without access to the oppilaitos organization" in {
    list(s"$OppilaitosPath/${o1.s}/osat", Map("organisaatioOid" -> ChildOid.s), 403, crudSessions(LonelyOid))
  }
  it should "allow access to a user of an ancestor organization" in {
    list(s"$OppilaitosPath/${o1.s}/osat", Map("organisaatioOid" -> GrandChildOid.s), List(oo2), crudSessions(GrandChildOid))
  }
  it should "deny access without the oppilaitos read role" in {
    list(s"$OppilaitosPath/${o1.s}/osat", Map("organisaatioOid" -> ChildOid.s), 403, addTestSession(Role.Koulutus.Read, OphOid))
  }
  it should "allow access with the oppilaitos read role" in {
    list(s"$OppilaitosPath/${o1.s}/osat", Map("organisaatioOid" -> ChildOid.s), 200, addTestSession(Role.Oppilaitos.Read, OphOid))
  }
  it should "allow access to the indexer" in {
    list(s"$OppilaitosPath/${o1.s}/osat", Map[String, String](), List(oo1, oo2), indexerSession)
  }

  //TODO: Paremmat testit sitten, kun indeksointi on vakiintunut muotoonsa
  "Koulutukset hakutiedot" should "return all hakutiedot related to koulutus" in {
    get(s"$KoulutusPath/${k1.oid}/hakutiedot", headers = Seq(sessionHeader(indexerSession))) {
      status should equal(200)
      //debugJson[List[Hakutieto]](body)

      val expected = List(Hakutieto(
        toteutusOid = t1.oid,
        haut = Seq(HakutietoHaku(
          hakuOid = h1.oid,
          nimi = h1.nimi,
          hakutapaKoodiUri = TestData.JulkaistuHaku.hakutapaKoodiUri,
          alkamiskausiKoodiUri = TestData.JulkaistuHaku.alkamiskausiKoodiUri,
          alkamisvuosi = TestData.JulkaistuHaku.alkamisvuosi,
          hakulomaketyyppi = TestData.JulkaistuHaku.hakulomaketyyppi,
          hakulomakeAtaruId = TestData.JulkaistuHaku.hakulomakeAtaruId,
          hakulomakeKuvaus = TestData.JulkaistuHaku.hakulomakeKuvaus,
          hakulomakeLinkki = TestData.JulkaistuHaku.hakulomakeLinkki,
          organisaatioOid = h1.organisaatioOid,
          hakuajat = TestData.JulkaistuHaku.hakuajat,
          muokkaaja = h1.muokkaaja,
          modified = Some(h1.modified),
          hakukohteet = Seq(HakutietoHakukohde(
            hakukohdeOid = hk1.oid,
            nimi = hk1.nimi,
            alkamiskausiKoodiUri = TestData.JulkaistuHakukohde.alkamiskausiKoodiUri,
            alkamisvuosi = TestData.JulkaistuHakukohde.alkamisvuosi,
            kaytetaanHaunAlkamiskautta = TestData.JulkaistuHakukohde.kaytetaanHaunAlkamiskautta,
            hakulomaketyyppi = TestData.JulkaistuHakukohde.hakulomaketyyppi,
            hakulomakeAtaruId = TestData.JulkaistuHakukohde.hakulomakeAtaruId,
            hakulomakeKuvaus = TestData.JulkaistuHakukohde.hakulomakeKuvaus,
            hakulomakeLinkki = TestData.JulkaistuHakukohde.hakulomakeLinkki,
            kaytetaanHaunHakulomaketta = TestData.JulkaistuHakukohde.kaytetaanHaunHakulomaketta,
            aloituspaikat = TestData.JulkaistuHakukohde.aloituspaikat,
            minAloituspaikat = TestData.JulkaistuHakukohde.minAloituspaikat,
            maxAloituspaikat = TestData.JulkaistuHakukohde.maxAloituspaikat,
            ensikertalaisenAloituspaikat = TestData.JulkaistuHakukohde.ensikertalaisenAloituspaikat,
            minEnsikertalaisenAloituspaikat = TestData.JulkaistuHakukohde.minEnsikertalaisenAloituspaikat,
            maxEnsikertalaisenAloituspaikat = TestData.JulkaistuHakukohde.maxEnsikertalaisenAloituspaikat,
            kaytetaanHaunAikataulua = TestData.JulkaistuHakukohde.kaytetaanHaunAikataulua,
            hakuajat = TestData.JulkaistuHakukohde.hakuajat,
            muokkaaja = hk1.muokkaaja,
            organisaatioOid = hk1.organisaatioOid,
            modified = Some(hk1.modified)))))))

      read[List[Hakutieto]](body) should equal(expected)
    }
  }
}
