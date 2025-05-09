package fi.oph.kouta.integration

import com.softwaremill.diffx._
import com.softwaremill.diffx.generic.auto._
import com.softwaremill.diffx.scalatest.DiffShouldMatcher._
import fi.oph.kouta.TestData
import fi.oph.kouta.TestData.{KkOpintojaksoKoulutuksenMetatieto, KkOpintojaksoToteutuksenMetatieto}
import fi.oph.kouta.TestOids._
import fi.oph.kouta.domain._
import fi.oph.kouta.domain.oid.OrganisaatioOid
import fi.oph.kouta.repository.HakukohdeDAO
import fi.oph.kouta.security.{Role, RoleEntity}
import fi.oph.kouta.util.OrganisaatioServiceUtil
import org.json4s.jackson.Serialization.read

class ListSpec extends KoutaIntegrationSpec with IndexerFixture {

  implicit val koulutusMatcher = ObjectMatcher.seq[KoulutusListItem].byValue(_.oid)
  implicit val koulutusListDiff = Diff.summon[List[KoulutusListItem]]

  // Koulutuksen modified ja tarjoajat kentät muuttuu kun sen toteutusten tarjoajat muuttuu. Siksi jätetään ne testeissä
  // huomiotta, kun käytetään listDiffx-funktiota.
  implicit val koulutusDiff = Diff.summon[KoulutusListItem].ignore(_.modified).ignore(_.tarjoajat)

  override val roleEntities: List[RoleEntity] = RoleEntity.all

  var k1, k2, k3, k4, k5, k6, k7, k8, k9, k10, k11, k12, k13, k14, k15, k16, k17, k18, k19, k20, k21, k22       :KoulutusListItem = _
  var t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19, t20, t21, t22, t23  :ToteutusListItem = _
  var h1, h2, h3, h4, h5                            :HakuListItem = _
  var v1, v2, v3, v4, v5                            :ValintaperusteListItem = _
  var s1, s2, s3, s4, s5                            :SorakuvausListItem = _
  var hk1, hk2, hk3, hk4, hk5, hk6, hk7             :HakukohdeListItem = _
  var o1, o2                                        :OrganisaatioOid = _
  var oo1, oo2, oo3                                 :OppilaitoksenOsaListItem = _

  var ophKoulutus        :  KoulutusListItem = _
  var ophT1, ophT2, ophT3: ToteutusListItem = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    createTestData()
  }

  def createTestData(): Unit = {
    s1 = addToList(sorakuvaus(Julkaistu, OphOid))
    s2 = addToList(sorakuvaus(Arkistoitu, OphOid))
    s3 = addToList(sorakuvaus(Julkaistu, OphOid))
    s4 = addToList(yoSorakuvaus.copy(tila = Julkaistu, organisaatioOid = OphOid))
    s5 = addToList(sorakuvaus.copy(tila = Poistettu))
    k1 = addToList(koulutus.copy(julkinen = false, organisaatioOid = ParentOid, tila = Julkaistu, sorakuvausId = Some(s1.id), tarjoajat = koulutus.tarjoajat ++ List(AmmOid, OtherOid)))
    k2 = addToList(koulutus.copy(julkinen = false, organisaatioOid = ChildOid, tila = Arkistoitu, sorakuvausId = Some(s1.id)))
    k3 = addToList(koulutus(julkinen = false, GrandChildOid, Tallennettu))
    k4 = addToList(koulutus.copy(julkinen = false, organisaatioOid = LonelyOid, tila = Julkaistu, sorakuvausId =  Some(s3.id), tarjoajat = koulutus.tarjoajat ++ List(AmmOid, OtherOid)))
    k5 = addToList(koulutus(julkinen = true, LonelyOid, Julkaistu).copy(ePerusteId = Some(12L)))
    k6 = addToList(yoKoulutus.copy(julkinen = true, organisaatioOid = UnknownOid, tila = Julkaistu, muokkaaja = TestUserOid))
    k7 = addToList(ammTutkinnonOsaKoulutus.copy(organisaatioOid = LonelyOid, tila = Julkaistu, tarjoajat = koulutus.tarjoajat ++ List(AmmOid, OtherOid)))
    k8 = addToList(ammOsaamisalaKoulutus.copy(organisaatioOid = LonelyOid, tila = Julkaistu, tarjoajat = koulutus.tarjoajat ++ List(AmmOid, OtherOid)))
    k9 = addToList(vapaaSivistystyoMuuKoulutus.copy(organisaatioOid = LonelyOid, tila = Julkaistu, tarjoajat = koulutus.tarjoajat ++ List(AmmOid, OtherOid)))
    k10 = addToList(koulutus.copy(ePerusteId = Some(15L), tila = Poistettu))
    k11 = addToList(TestData.VapaaSivistystyoOpistovuosiKoulutus.copy(organisaatioOid = AmmOid, tila = Julkaistu, tarjoajat = koulutus.tarjoajat ++ List(AmmOid, OtherOid)))
    k12 = addToList(TestData.TelmaKoulutus.copy(organisaatioOid = AmmOid, tila = Julkaistu, tarjoajat = koulutus.tarjoajat ++ List(AmmOid, OtherOid)))
    k13 = addToList(TestData.TuvaKoulutus.copy(organisaatioOid = AmmOid, tila = Julkaistu, tarjoajat = koulutus.tarjoajat ++ List(AmmOid, OtherOid)))
    k14 = addToList(TestData.AmmMuuKoulutus.copy(organisaatioOid = AmmOid, tila = Julkaistu, tarjoajat = koulutus.tarjoajat ++ List(AmmOid, OtherOid)))
    k15 = addToList(TestData.AikuistenPerusopetusKoulutus.copy(organisaatioOid = AmmOid, tila = Julkaistu, tarjoajat = koulutus.tarjoajat ++ List(AmmOid, OtherOid)))
    k16 = addToList(TestData.TaiteenPerusopetusKoulutus.copy(organisaatioOid = AmmOid, tila = Julkaistu, tarjoajat = koulutus.tarjoajat ++ List(AmmOid, OtherOid)))
    k17 = addToList(TestData.KkOpintokokonaisuusKoulutus.copy(organisaatioOid = HkiYoOid, tila = Julkaistu, tarjoajat = List(HkiYoOid)))
    k18 = addToList(TestData.KkOpintojaksoKoulutus.copy(organisaatioOid = HkiYoOid, tila = Julkaistu, tarjoajat = List(HkiYoOid)))
    k19 = addToList(TestData.ErikoistumisKoulutus.copy(organisaatioOid = HkiYoOid, tila = Julkaistu, tarjoajat = List(HkiYoOid)))
    k20 = addToList(TestData.YoOpettajaKoulutus.copy(organisaatioOid = HkiYoOid, tila = Julkaistu, tarjoajat = List(HkiYoOid)))
    k21 = addToList(TestData.AmmOpettajaKoulutus.copy(organisaatioOid = AmkOid, tila = Julkaistu, tarjoajat = List(AmkOid)))
    k22 = addToList(TestData.MuuKoulutus.copy(organisaatioOid = PohjoiskalotinKoulutussaatio, tila = Julkaistu, tarjoajat = List(PohjoiskalotinKoulutussaatio)))

    t1 = addToList(toteutus(k1.oid.toString, Julkaistu, ParentOid))
    t2 = addToList(toteutus(k1.oid.toString, Arkistoitu, ChildOid))
    t3 = addToList(toteutus(k1.oid.toString, Tallennettu, GrandChildOid))
    t4 = addToList(toteutus(k4.oid.toString, Julkaistu, LonelyOid))
    t5 = addToList(ammTutkinnonOsaToteutus.copy(koulutusOid = k7.oid, organisaatioOid = LonelyOid, tila = Julkaistu), false)
    t6 = addToList(ammOsaamisalaToteutus.copy(koulutusOid = k8.oid, organisaatioOid = LonelyOid, tila = Julkaistu), false)
    t7 = addToList(ammTutkinnonOsaToteutusAtaru.copy(koulutusOid = k7.oid, organisaatioOid = LonelyOid, tila = Julkaistu), false)
    t8 = addToList(ammOsaamisalaToteutusAtaru.copy(koulutusOid = k8.oid, organisaatioOid = LonelyOid, tila = Arkistoitu), false)
    t9 = addToList(vapaaSivistystyoMuuToteutus.copy(koulutusOid = k9.oid, organisaatioOid = LonelyOid, tila = Julkaistu), false)
    t10 = addToList(vapaaSivistystyoMuuToteutusAtaru.copy(koulutusOid = k9.oid, organisaatioOid = LonelyOid, tila = Julkaistu), false)
    t11 = addToList(toteutus(k1.oid.s, Poistettu, OphOid))
    t12 = addToList(TestData.VapaaSivistystyoOpistovuosiToteutus.copy(koulutusOid = k11.oid, organisaatioOid = AmmOid, tila = Julkaistu), false)
    t13 = addToList(TestData.TelmaToteutus.copy(koulutusOid = k12.oid, organisaatioOid = AmmOid, tila = Julkaistu), false)
    t14 = addToList(TestData.TuvaToteutus.copy(koulutusOid = k13.oid, organisaatioOid = AmmOid, tila = Julkaistu), false)
    t15 = addToList(TestData.AmmMuuToteutus.copy(koulutusOid = k14.oid, organisaatioOid = AmmOid, tila = Julkaistu), false)
    t16 = addToList(TestData.AikuistenPerusopetusToteutus.copy(koulutusOid = k15.oid, organisaatioOid = AmmOid, tila = Julkaistu), false)
    t17 = addToList(TestData.TaiteenPerusopetusToteutus.copy(koulutusOid = k16.oid, organisaatioOid = AmmOid, tila = Julkaistu), false)
    t18 = addToList(TestData.JulkaistuKkOpintokokonaisuusToteutus.copy(koulutusOid = k17.oid, organisaatioOid = HkiYoOid, tila = Julkaistu), false)
    t19 = addToList(TestData.JulkaistuKkOpintojaksoToteutus.copy(koulutusOid = k18.oid, organisaatioOid = HkiYoOid, tila = Julkaistu, metadata =
      Some(KkOpintojaksoToteutuksenMetatieto.copy(isHakukohteetKaytossa = Some(true)))), false)
    t20 = addToList(TestData.JulkaistuErikoistumisKoulutusToteutus.copy(koulutusOid = k19.oid, organisaatioOid = HkiYoOid, tila = Julkaistu))
    t21 = addToList(TestData.JulkaistuYoOpettajaToteutus.copy(koulutusOid = k20.oid, organisaatioOid = HkiYoOid, tila = Julkaistu), false)
    t22 = addToList(TestData.JulkaistuAmmOpettajaToteutus.copy(koulutusOid = k21.oid, organisaatioOid = AmkOid, tila = Julkaistu), false)
    t23 = addToList(TestData.JulkaistuMuuToteutus.copy(koulutusOid = k22.oid, organisaatioOid = PohjoiskalotinKoulutussaatio, tila = Julkaistu))

    h1 = addToList(haku(Julkaistu, ParentOid).copy(hakukohteenLiittajaOrganisaatiot = Seq(LonelyOid)))
    h2 = addToList(haku(Arkistoitu, ChildOid))
    h3 = addToList(haku(Tallennettu, GrandChildOid).copy(kohdejoukkoKoodiUri = Some("haunkohdejoukko_05#2"), kohdejoukonTarkenneKoodiUri = None))
    h4 = addToList(haku(Julkaistu, LonelyOid))
    h5 = addToList(haku(Poistettu, ParentOid))
    v1 = addToList(valintaperuste(Julkaistu, ParentOid).copy(julkinen = false))
    v2 = addToList(valintaperuste(Arkistoitu, ChildOid).copy(julkinen = true))
    v3 = addToList(valintaperuste(Tallennettu, GrandChildOid).copy(kohdejoukkoKoodiUri = Some("haunkohdejoukko_05#2"), julkinen = false))
    v4 = addToList(valintaperuste(Julkaistu, LonelyOid).copy(julkinen = false))
    v5 = addToList(valintaperuste(Poistettu, LonelyOid).copy(julkinen = false))
    hk1 = addToList(hakukohde(t1.oid, h1.oid, v1.id, ParentOid).copy(jarjestyspaikkaOid = Some(t1.tarjoajat.head)))
    hk2 = addToList(hakukohde(t2.oid, h1.oid, v1.id, ChildOid).copy(tila = Tallennettu, jarjestyspaikkaOid = Some(t2.tarjoajat.head)))
    hk3 = addToList(hakukohde(t1.oid, h2.oid, v1.id, GrandChildOid).copy(tila = Arkistoitu, jarjestyspaikkaOid = Some(t1.tarjoajat.head)))
    hk4 = addToList(hakukohde(t4.oid, h1.oid, v1.id, LonelyOid))
    hk5 = addToList(hakukohde(t1.oid, h2.oid, v1.id, GrandChildOid).copy(tila = Tallennettu, jarjestyspaikkaOid = Some(t1.tarjoajat.head)))
    hk6 = addToList(hakukohde(t1.oid, h3.oid, v1.id, GrandChildOid).copy(tila = Tallennettu, jarjestyspaikkaOid = Some(t1.tarjoajat.head)))
    hk7 = addToList(hakukohde(t1.oid, h1.oid, v1.id, ParentOid).copy(tila = Poistettu, jarjestyspaikkaOid = Some(t1.tarjoajat.head)))

    o1 = OrganisaatioOid(put(oppilaitos(Julkaistu, ParentOid)))
    o2 = OrganisaatioOid(put(oppilaitos(Julkaistu, EvilChildOid)))

    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(ChildOid)).
      thenReturn(TestData.organisaatioServiceOrgOrganisaationOsa.copy(oid = ChildOid.s))
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(GrandChildOid)).
      thenReturn(TestData.organisaatioServiceOrgOrganisaationOsa.copy(oid = GrandChildOid.s))

    val evilParentOidPath = s"${EvilChildOid.toString}/1.2.246.562.10.97036773279/1.2.246.562.10.00000000001"
    when(mockOrganisaatioServiceClient.getOrganisaatioWithOidFromCache(EvilGrandChildOid)).
      thenReturn(TestData.organisaatioServiceOrgOrganisaationOsa.copy(oid = EvilGrandChildOid.s, parentOidPath = evilParentOidPath))

    val parentOids = OrganisaatioServiceUtil.getParentOids(TestData.organisaatioServiceOrgOrganisaationOsa.parentOidPath)
    val evilParentOids = OrganisaatioServiceUtil.getParentOids(evilParentOidPath)

    when(mockOrganisaatioServiceClient.getOrganisaatiotWithOidsFromCache(parentOids)).
      thenReturn(List(TestData.organisaatioServiceOrg.copy(oid = o1.s)))

    when(mockOrganisaatioServiceClient.getOrganisaatiotWithOidsFromCache(evilParentOids)).
      thenReturn(List(TestData.organisaatioServiceOrg.copy(oid = o2.s)))

    // oppilaitosOid pois
    oo1 = addToList(oppilaitoksenOsa(ChildOid, o1, Julkaistu, ChildOid))
    oo2 = addToList(oppilaitoksenOsa(GrandChildOid, o1, Julkaistu, GrandChildOid))
    oo3 = addToList(oppilaitoksenOsa(EvilGrandChildOid, o2, Julkaistu, EvilGrandChildOid))

    ophKoulutus = addToList(koulutus(julkinen = true, OphOid, Julkaistu).copy(tarjoajat = koulutus.tarjoajat ++ List(AmmOid, OtherOid)))
    ophT1 = addToList(toteutus(ophKoulutus.oid.toString, Julkaistu, LonelyOid))
    ophT2 = addToList(toteutus(ophKoulutus.oid.toString, Julkaistu, GrandChildOid))
    ophT3 = addToList(toteutus(ophKoulutus.oid.toString, Poistettu, GrandChildOid))
  }

  "Koulutus list" should "list all koulutukset for authorized organizations 1" in {
    listDiffx(KoulutusPath, Map("organisaatioOid" -> ChildOid.s), List(k1, k2, k3, k5, ophKoulutus))
  }
  it should "list all koulutukset for authorized organizations 2" in {
    listDiffx(KoulutusPath, Map("organisaatioOid" -> LonelyOid.s), List(k4, k5, k7, k8, k9, ophKoulutus))
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
    listDiffx(KoulutusPath, Map("organisaatioOid" -> ChildOid.s), List(k1, k2, k3, k5, ophKoulutus), crudSessions(ChildOid))
  }
  it should "deny access without access to the given organization" in {
    list(KoulutusPath, Map("organisaatioOid" -> ChildOid.s), 403, crudSessions(LonelyOid))
  }
  it should "allow access for a user of an ancestor organization" in {
    listDiffx(KoulutusPath, Map("organisaatioOid" -> ChildOid.s), List(k1, k2, k3, k5, ophKoulutus), crudSessions(ParentOid))
  }
  it should "allow access for a user of a descendant organization" in {
    listDiffx(KoulutusPath, Map("organisaatioOid" -> ChildOid.s), List(k1, k2, k3, k5, ophKoulutus), crudSessions(GrandChildOid))
  }
  it should "deny access without an accepted role" in {
    list(KoulutusPath, Map("organisaatioOid" -> ChildOid.s), 403, otherRoleSession)
  }
  it should "allow access to any koulutus with the indexer role" in {
    listDiffx(KoulutusPath, Map("organisaatioOid" -> LonelyOid.s), List(k4, k5, k7, k8, k9, ophKoulutus), indexerSession)
  }
  it should "list public koulutus with the same koulutustyyppi" in {
    listDiffx(KoulutusPath, Map("organisaatioOid" -> YoOid.s), List(k6), readSessions(YoOid))
  }
  it should "list only julkiset and oph koulutukset to oph organization" in {
    listDiffx(KoulutusPath, Map("organisaatioOid" -> OphOid.s), List(k5, k6, ophKoulutus), ophSession)
  }
  it should "by default list arkistoidut also" in {
    listDiffx(KoulutusPath, Map("organisaatioOid" -> ChildOid.s), List(k1, k2, k3, k5, ophKoulutus))
  }
  it should "filter out arkistoidut if instructed" in {
    listDiffx(KoulutusPath, Map("organisaatioOid" -> ChildOid.s, "myosArkistoidut" -> "false"), List(k1, k3, k5, ophKoulutus))
  }
  it should "filter with koulutustyyppi if instructed" in {
    listDiffx(KoulutusPath, Map("organisaatioOid" -> ChildOid.s, "koulutustyyppi" -> Amm.toString), List(k1, k2, k3, k5, ophKoulutus))
  }
  it should "filter all with non-existing koulutustyyppi in organization" in {
    listDiffx(KoulutusPath, Map("organisaatioOid" -> ChildOid.s, "koulutustyyppi" -> Yo.toString), List(k6))
  }

  "Toteutus list" should "list all toteutukset for selected organization" in {
    list(ToteutusPath, Map("organisaatioOid" -> ChildOid.s), List(t1, t2, t3, ophT2))
  }
  it should "list all toteutukset for selected organization 2" in {
    list(ToteutusPath, Map("organisaatioOid" -> LonelyOid.s), List(t4, t5, t6, t7, t8, t9, t10, ophT1))
  }
  it should "list only those toteutukset that can be linked to hakukohde" in {
    list(ToteutusPath, Map("organisaatioOid" -> LonelyOid.s, "vainHakukohteeseenLiitettavat" -> "true"), List(t4, t7, t8, t10, ophT1))
    list(ToteutusPath, Map("organisaatioOid" -> AmmOid.s, "vainHakukohteeseenLiitettavat" -> "true"), List(t1, t2, t3, t4, t7, t8, t10, t12, t13, t14, ophT1, ophT2))
    list(ToteutusPath, Map("organisaatioOid" -> HkiYoOid.s, "vainHakukohteeseenLiitettavat" -> "true"), List(t19, t21))
    list(ToteutusPath, Map("organisaatioOid" -> AmkOid.s, "vainHakukohteeseenLiitettavat" -> "true"), List(t22))
    list(ToteutusPath, Map("organisaatioOid" -> PohjoiskalotinKoulutussaatio.s, "vainHakukohteeseenLiitettavat" -> "true"), List())
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
    list(ToteutusPath, Map("organisaatioOid" -> ChildOid.s), List(t1, t2, t3, ophT2), crudSessions(ChildOid))
  }
  it should "deny access without access to the given organization" in {
    list(ToteutusPath, Map("organisaatioOid" -> ChildOid.s), 403, crudSessions(LonelyOid))
  }
  it should "allow access for a user of an ancestor organization" in {
    list(ToteutusPath, Map("organisaatioOid" -> ChildOid.s), List(t1, t2, t3, ophT2), crudSessions(ParentOid))
  }
  it should "allow access for a user of a descendant organization" in {
    list(ToteutusPath, Map("organisaatioOid" -> ChildOid.s), List(t1, t2, t3, ophT2), crudSessions(GrandChildOid))
  }
  it should "deny access without an accepted role" in {
    list(ToteutusPath, Map("organisaatioOid" -> ChildOid.s), 403, otherRoleSession)
  }
  it should "allow access to any toteutus with the indexer role" in {
    list(ToteutusPath, Map("organisaatioOid" -> ChildOid.s), List(t1, t2, t3, ophT2), indexerSession)
  }
  it should "list all toteutukset linked to oph koulutus for oph organization" in {
    list(ToteutusPath, Map("organisaatioOid" -> OphOid.s), List(), ophSession)
  }
  it should "by default list arkistoidut also" in {
    list(ToteutusPath, Map("organisaatioOid" -> ChildOid.s), List(t1, t2, t3, ophT2))
  }
  it should "filter out arkistoidut if instructed" in {
    list(ToteutusPath, Map("organisaatioOid" -> ChildOid.s, "myosArkistoidut" -> "false"), List(t1, t3, ophT2))
  }
  it should "filter arkistoidut and hakukohteeseen liitettävät if instructed" in {
    list(ToteutusPath, Map("organisaatioOid" -> LonelyOid.s, "vainHakukohteeseenLiitettavat" -> "true", "myosArkistoidut" -> "false"), List(t4, t7, t10, ophT1))
  }
  "Haku list" should "list all haut for authorized organizations" in {
    list(HakuPath, Map("organisaatioOid" -> ChildOid.s), List(h1, h2, h3))
  }
  it should "list all haut for authorized organizations 2" in {
    list(HakuPath, Map("organisaatioOid" -> LonelyOid.s), List(h1, h4))
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
    list(HakuPath, Map("organisaatioOid" -> ChildOid.s), List(h1, h2, h3), crudSessions(ChildOid))
  }
  it should "deny access without access to the given organization" in {
    list(HakuPath, Map("organisaatioOid" -> ChildOid.s), 403, crudSessions(LonelyOid))
  }
  it should "allow access for a user of an ancestor organization" in {
    list(HakuPath, Map("organisaatioOid" -> ChildOid.s), List(h1, h2, h3), crudSessions(ParentOid))
  }
  it should "allow access for a user of a descendant organization" in {
    list(HakuPath, Map("organisaatioOid" -> ChildOid.s), List(h1, h2, h3), crudSessions(GrandChildOid))
  }
  it should "deny access without an accepted role" in {
    list(HakuPath, Map("organisaatioOid" -> ChildOid.s), 403, otherRoleSession)
  }
  it should "allow access to any haku with the indexer role" in {
    list(HakuPath, Map("organisaatioOid" -> ChildOid.s), List(h1, h2, h3), indexerSession)
  }
  it should "by default list arkistoidut also" in {
    list(HakuPath, Map("organisaatioOid" -> ChildOid.s), List(h1, h2, h3))
  }
  it should "filter out arkistoidut if instructed" in {
    list(HakuPath, Map("organisaatioOid" -> ChildOid.s, "myosArkistoidut" -> "false"), List(h1, h3))
  }
  it should "list all hakukohteen liittäjäorganisaatiot for given organisaatioOid" in {
    list(HakuPath, Map("organisaatioOid" -> LonelyOid.s), List(h1, h4))
  }
  "Valintaperuste list" should "list all valintaperustekuvaukset for authorized organizations" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s), List(v1, v2, v3))
  }
  it should "list all valintaperustekuvaukset for authorized organizations 2" in {
    list(ValintaperustePath, Map("organisaatioOid" -> LonelyOid.s), List(v2, v4))
  }
  it should "list all valintaperustekuvaukset that can be joined to given haku" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s, "hakuOid" -> h2.oid.toString, "koulutustyyppi" -> Amm.toString), List(v1, v2))
  }
  it should "list all valinteperustekuvaukset that can be joiden to given haku even when kohdejoukko is null" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s, "hakuOid" -> h3.oid.toString, "koulutustyyppi" -> Amm.toString), List(v3))
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
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s), List(v1, v2, v3), crudSessions(ChildOid))
  }
  it should "deny access without access to the given organization" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s), 403, crudSessions(LonelyOid))
  }
  it should "allow access for a user of an ancestor organization" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s), List(v1, v2, v3), crudSessions(ParentOid))
  }
  it should "allow access for a user of a descendant organization" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s), List(v1, v2, v3), crudSessions(GrandChildOid))
  }
  it should "deny access without an accepted role" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s), 403, otherRoleSession)
  }
  it should "allow access to any valintaperuste with the indexer role" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s), List(v1, v2, v3), indexerSession)
  }
  it should "list public valintaperuste with the same koulutustyyppi" in {
    list(ValintaperustePath, Map("organisaatioOid" -> AmmOid.s), List(v2), readSessions(AmmOid))
  }
  it should "not list public valintaperuste with different koulutustyyppi" in {
    list(ValintaperustePath, Map("organisaatioOid" -> YoOid.s), List(), readSessions(YoOid))
  }
  it should "by default list arkistoidut also" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s), List(v1, v2, v3))
  }
  it should "filter out arkistoidut if instructed" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s, "myosArkistoidut" -> "false"), List(v1, v3))
  }
  it should "filter out arkistoidut and list valintaperusteet that can be joined to given hakukohde" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s, "hakuOid" -> h2.oid.toString, "koulutustyyppi" -> Amm.toString, "myosArkistoidut" -> "false"), List(v1))
  }
  it should "filter out wrong type of koulutustyyppi" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid.s, "hakuOid" -> h2.oid.toString, "koulutustyyppi" -> Lk.toString, "myosArkistoidut" -> "false"), List())
  }

  "Sorakuvaus list" should "list all sorakuvaukset for non oph organisation 1" in {
    list(SorakuvausPath, Map("organisaatioOid" -> ChildOid.s), List(s1, s2, s3))
  }
  it should "list all sorakuvaukset non oph organisation 2" in {
    list(SorakuvausPath, Map("organisaatioOid" -> LonelyOid.s), List(s1, s2, s3))
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
    list(SorakuvausPath, Map("organisaatioOid" -> ChildOid.s), List(s1, s2, s3), crudSessions(ChildOid))
  }
  it should "deny access without access to the given organization" in {
    list(SorakuvausPath, Map("organisaatioOid" -> ChildOid.s), 403, crudSessions(LonelyOid))
  }
  it should "allow access for a user of an ancestor organization" in {
    list(SorakuvausPath, Map("organisaatioOid" -> ChildOid.s), List(s1, s2, s3), crudSessions(ParentOid))
  }
  it should "allow access for a user of a descendant organization" in {
    list(SorakuvausPath, Map("organisaatioOid" -> ChildOid.s), List(s1, s2, s3), crudSessions(GrandChildOid))
  }
  it should "deny access without an accepted role" in {
    list(SorakuvausPath, Map("organisaatioOid" -> ChildOid.s), 403, otherRoleSession)
  }
  it should "allow access to any sorakuvaus with the indexer role" in {
    list(SorakuvausPath, Map("organisaatioOid" -> ChildOid.s), List(s1, s2, s3), indexerSession)
  }
  it should "list all sorakuvaukset with the same koulutustyyppi (amm)" in {
    list(SorakuvausPath, Map("organisaatioOid" -> AmmOid.s), List(s1, s2, s3), readSessions(AmmOid))
  }
  it should "list all sorakuvaukset with different koulutustyyppi (yo)" in {
    list(SorakuvausPath, Map("organisaatioOid" -> YoOid.s), List(s4), readSessions(YoOid))
  }
  it should "by default list arkistoidut also" in {
    list(SorakuvausPath, Map("organisaatioOid" -> ChildOid.s), List(s1, s2, s3))
  }
  it should "filter out arkistoidut if instructed" in {
    list(SorakuvausPath, Map("organisaatioOid" -> ChildOid.s, "myosArkistoidut" -> "false"), List(s1, s3))
  }

  "Valintaperustetta käyttävät hakukohteet for indexer list" should "list all hakukohteet using given valintaperuste id" in {
    list(s"$IndexerPath$ValintaperustePath/${v1.id.toString}/hakukohteet", Map[String,String](), List(hk1, hk2, hk3, hk4, hk5, hk6))
  }
  it should "return 401 if session is not valid" in {
    list(s"$IndexerPath$ValintaperustePath/${v1.id.toString}/hakukohteet", Map[String,String](), 401, Map.empty)
  }
  it should "deny access to non-root users" in {
    list(s"$IndexerPath$ValintaperustePath/${v1.id.toString}/hakukohteet", Map[String,String](), 403, crudSessions(v1.organisaatioOid))
  }
  it should "allow access to the indexer" in {
    list(s"$IndexerPath$ValintaperustePath/${v1.id.toString}/hakukohteet", Map[String,String](), List(hk1, hk2, hk3, hk4, hk5, hk6), indexerSession)
  }
  it should "include also poistetut if instructed" in {
    list(s"$IndexerPath$ValintaperustePath/${v1.id.toString}/hakukohteet", Map("vainOlemassaolevat" -> "false"), List(hk1, hk2, hk3, hk4, hk5, hk6, hk7), indexerSession)
  }

  "Koulutuksen toteutukset list" should "list all toteutukset for this and child organizations" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> ParentOid.s), List(t1, t2, t3))
  }
  it should "list toteutukset for parent organizations" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> GrandChildOid.s), List(t1, t2, t3))
  }
  it should "return 401 if no session is found" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> ParentOid.s), 401, Map.empty)
  }
  it should "return forbidden if organisaatio oid is unknown" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> UnknownOid.s), 403)
  }
  it should "allow access to a user of the koulutus organization" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> k1.organisaatioOid.s), List(t1, t2, t3), crudSessions(k1.organisaatioOid))
  }
  it should "deny access to a user without access to the koulutus organization" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> k1.organisaatioOid.s), 403, crudSessions(LonelyOid))
  }
  it should "allow access to a user of an ancestor organization" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> k3.organisaatioOid.s), List(t1, t2, t3), crudSessions(ParentOid))
  }
  it should "allow access to a user of a descendant organization" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> k1.organisaatioOid.s), List(t1, t2, t3), crudSessions(GrandChildOid))
  }
  it should "deny access without the toteutus read role" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> ChildOid.s), 403, addTestSession(Role.Koulutus.Read, OphOid))
  }
  it should "list all toteutukset linked to oph koulutus for oph organization" in {
    list(s"$KoulutusPath/${ophKoulutus.oid}/toteutukset", Map("organisaatioOid" -> OphOid.s), List(ophT1, ophT2), ophSession)
  }

  "Koulutuksen toteutukset for indexer list" should "return all toteutukset for indexer" in {
    list(s"$IndexerPath$KoulutusPath/${k1.oid}/toteutukset", Map[String, String](), List(t1, t2, t3), indexerSession)
  }
  it should "return also poistetut toteutukset if instructed" in {
    list(s"$IndexerPath$KoulutusPath/${k1.oid}/toteutukset", Map("vainOlemassaolevat" -> "false"), List(t1, t2, t3, t11), indexerSession)
  }
  it should "deny access to root user without indexer role" in {
    list(s"$IndexerPath$KoulutusPath/${k1.oid}/toteutukset", Map[String,String](), 403)
  }
  it should "deny access to all toteutukset without indexer access" in {
    list(s"$IndexerPath$KoulutusPath/${k1.oid}/toteutukset", Map[String, String](), 403, crudSessions(ParentOid))
  }
  it should "return 401 if no session is found" in {
    list(s"$IndexerPath$KoulutusPath/${k1.oid}/toteutukset", Map[String, String](), 401, Map.empty)
  }

  "Toteutukseen liitetyt haut" should "list all haut mapped to given toteutus for indexer" in {
    list(s"$IndexerPath$ToteutusPath/${t1.oid}/haut", Map[String,String](), List(h1, h2, h3), indexerSession)
  }
  it should "deny access to root user without indexer role" in {
    list(s"$IndexerPath$ToteutusPath/${t1.oid}/haut", Map[String,String](), 403)
  }
  it should "return 401 if no session is found" in {
    list(s"$IndexerPath$ToteutusPath/${t1.oid}/haut", Map[String,String](), 401, Map.empty)
  }
  it should "deny access to a non-root user, even if they own the toteutus" in {
    list(s"$IndexerPath$ToteutusPath/${t2.oid}/haut", Map.empty[String, String], 403, crudSessions(ChildOid))
  }
  it should "deny access without access to the toteutus organization" in {
    list(s"$IndexerPath$ToteutusPath/${t2.oid}/haut", Map.empty[String, String], 403, crudSessions(LonelyOid))
  }
  it should "deny access for a non-root user of an ancestor organization" in {
    list(s"$IndexerPath$ToteutusPath/${t2.oid}/haut", Map.empty[String, String], 403, crudSessions(ParentOid))
  }
  it should "deny access for a user of a descendant organization" in {
    list(s"$IndexerPath$ToteutusPath/${t2.oid}/haut", Map.empty[String, String], 403, crudSessions(GrandChildOid))
  }
  it should "deny access without the haku read role" in {
    list(s"$IndexerPath$ToteutusPath/${t2.oid}/haut", Map.empty[String, String], 403, addTestSession(Role.Toteutus.Read, OphOid))
  }

  "Toteutukseen liitetyt hakukohteet" should "list all hakukohteet mapped to given toteutus" in {
    list(s"$ToteutusPath/${t1.oid}/hakukohteet", Map[String,String]("organisaatioOid" -> t1.organisaatioOid.s), List(hk1, hk3, hk5, hk6))
  }
  it should "return 401 if no session is found" in {
    list(s"$ToteutusPath/${t1.oid}/hakukohteet", Map[String,String]("organisaatioOid" -> t1.organisaatioOid.s), 401, Map.empty)
  }
  it should "allow access to a non-root user of the toteutus organization" in {
    list(s"$ToteutusPath/${t2.oid}/hakukohteet", Map[String, String]("organisaatioOid" -> ChildOid.s), List(hk2), crudSessions(ChildOid))
  }
  it should "return 404 if called without organisaatioOid parameter" in {
    list(s"$ToteutusPath/${t2.oid}/hakukohteet", Map.empty[String, String], 404, crudSessions(ChildOid))
  }
  it should "deny access without access to the toteutus organization" in {
    list(s"$ToteutusPath/${t2.oid}/hakukohteet", Map[String, String]("organisaatioOid" -> LonelyOid.s), List.empty[Hakukohde], crudSessions(LonelyOid))
  }
  it should "allow access for a non-root user of an ancestor organization" in {
    list(s"$ToteutusPath/${t2.oid}/hakukohteet", Map[String, String]("organisaatioOid" -> ParentOid.s), List(hk2), crudSessions(ParentOid))
  }
  it should "deny access for a user of a descendant organization" in {
    list(s"$ToteutusPath/${t2.oid}/hakukohteet", Map[String, String]("organisaatioOid" -> GrandChildOid.s), List.empty[Hakukohde], crudSessions(GrandChildOid))
  }
  it should "deny access without the hakukohde read role" in {
    list(s"$ToteutusPath/${t2.oid}/hakukohteet", Map[String, String]("organisaatioOid" -> OphOid.s), 403, addTestSession(Role.Toteutus.Read, OphOid))
  }
  it should "allow access to the hakukohteet of any toteutus with the indexer role with the toteutus organisation" in {
    list(s"$ToteutusPath/${t2.oid}/hakukohteet", Map[String, String]("organisaatioOid" -> ChildOid.s), List(hk2), indexerSession)
  }

  "Toteutukseen liitetyt hakukohteet for indexer" should "list all hakukohteet mapped to given toteutus for indexer" in {
    list(s"$IndexerPath$ToteutusPath/${t1.oid}/hakukohteet", Map[String,String](), List(hk1, hk3, hk5, hk6), indexerSession)
  }
  it should "list also poistetut hakukohteet if instructed" in {
    list(s"$IndexerPath$ToteutusPath/${t1.oid}/hakukohteet", Map("vainOlemassaolevat" -> "false"), List(hk1, hk3, hk5, hk6, hk7), indexerSession)
  }
  it should "deny access to root user without indexer role" in {
    list(s"$IndexerPath$ToteutusPath/${t1.oid}/hakukohteet", Map[String,String](), 403)
  }
  it should "return 401 if no session is found" in {
    list(s"$IndexerPath$ToteutusPath/${t1.oid}/hakukohteet", Map[String,String](), 401, Map.empty)
  }
  it should "deny access to a non-root user of the toteutus organization" in {
    list(s"$IndexerPath$ToteutusPath/${t2.oid}/hakukohteet", Map.empty[String, String], 403, crudSessions(ChildOid))
  }
  it should "deny access without access to the toteutus organization" in {
    list(s"$IndexerPath$ToteutusPath/${t2.oid}/hakukohteet", Map.empty[String, String], 403, crudSessions(LonelyOid))
  }
  it should "deny access for a non-root user of an ancestor organization" in {
    list(s"$IndexerPath$ToteutusPath/${t2.oid}/hakukohteet", Map.empty[String, String], 403, crudSessions(ParentOid))
  }
  it should "deny access for a user of a descendant organization" in {
    list(s"$IndexerPath$ToteutusPath/${t2.oid}/hakukohteet", Map.empty[String, String], 403, crudSessions(GrandChildOid))
  }
  it should "deny access without the hakukohde read role" in {
    list(s"$IndexerPath$ToteutusPath/${t2.oid}/hakukohteet", Map.empty[String, String], 403, addTestSession(Role.Toteutus.Read, OphOid))
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
  it should "return not found if organisaatio oid is not given" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map[String,String](), 404)
  }
  it should "return 401 if there's no valid session" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map("organisaatioOid" -> ParentOid.s), 401, Map.empty)
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

  "Hakuun liitetyt hakukohteet for indexer" should "list all hakukohteet mapped to given haku for indexer" in {
    list(s"$IndexerPath$HakuPath/${h1.oid}/hakukohteet", Map[String, String](), List(hk1, hk2, hk4), indexerSession)
  }
  it should "list also poistetut hakukohteet if instructed" in {
    list(s"$IndexerPath$HakuPath/${h1.oid}/hakukohteet", Map("vainOlemassaolevat" -> "false"), List(hk1, hk2, hk4, hk7), indexerSession)
  }
  it should "deny access to root user without indexer role" in {
    list(s"$IndexerPath$HakuPath/${h1.oid}/hakukohteet", Map[String, String](), 403)
  }
  it should "return 401 if there's no valid session" in {
    list(s"$IndexerPath$HakuPath/${h1.oid}/hakukohteet", Map[String,String](), 401, Map.empty)
  }
  it should "deny access to all hakukohteet without root organization access" in {
    list(s"$IndexerPath$HakuPath/${h1.oid}/hakukohteet", Map[String, String](), 403, crudSessions(ParentOid))
  }

  "Hakuun kuuluvat koulutukset for indexer" should "list all koulutukset mapped to given haku by hakukohde for indexer" in {
    listDiffx(s"$IndexerPath$HakuPath/${h1.oid}/koulutukset", Map[String, String](), List(k1, k4), indexerSession)
  }
  it should "deny access to root user without indexer role" in {
    list(s"$IndexerPath$HakuPath/${h1.oid}/koulutukset", Map[String,String](), 403)
  }
  it should "return 401 if there's no valid session" in {
    list(s"$IndexerPath$HakuPath/${h1.oid}/koulutukset", Map[String,String](), 401, Map.empty)
  }
  it should "deny access to all koulutukset without root organization access" in {
    list(s"$IndexerPath$HakuPath/${h1.oid}/koulutukset", Map[String, String](), 403, crudSessions(ParentOid))
  }

  "Sorakuvausta käyttävät koulutukset for indexer" should "list all koulutukset using given sorakuvaus for indexer" in {
    list(s"$IndexerPath$SorakuvausPath/${s1.id}/koulutukset", Map[String,String](), List(k1.oid, k2.oid), indexerSession)
  }
  it should "list all koulutukset using given sorakuvaus 2" in {
    list(s"$IndexerPath$SorakuvausPath/${s3.id}/koulutukset", Map[String,String](), List(k4.oid), indexerSession)
  }
  it should "deny access to root user without indexer role" in {
    list(s"$IndexerPath$SorakuvausPath/${s1.id}/koulutukset", Map[String, String](), 403)
  }
  it should "deny access to a non-root user, even if they own the toteutus" in {
    list(s"$IndexerPath$SorakuvausPath/${s2.id}/koulutukset", Map.empty[String, String], 403, crudSessions(ChildOid))
  }
  it should "deny access without the valintaperuste read role" in {
    list(s"$IndexerPath$SorakuvausPath/${s2.id}/koulutukset", Map.empty[String, String], 403, addTestSession(Role.Toteutus.Read, OphOid))
  }
  it should "deny access with the valintaperuste read role" in {
    list(s"$IndexerPath$SorakuvausPath/${s2.id}/koulutukset", Map.empty[String, String], 403, addTestSession(Role.Valintaperuste.Read, OphOid))
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
  it should "return not found if no organization oid is given" in {
    list(s"$OppilaitosPath/${o1.s}/osat", Map[String, String](), 404, crudSessions(ParentOid))
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

  "Oppilaitoksen osat list for indexer" should "list all oppilaitoksen osat for this oppilaitos for indexer" in {
    list(s"$IndexerPath$OppilaitosPath/${o1.s}/osat", Map[String, String](), List(oo1, oo2), indexerSession)
  }
  it should "deny access to root user without indexer role" in {
    list(s"$IndexerPath$OppilaitosPath/${o1.s}/osat", Map[String, String](), 403)
  }
  it should "return 401 if no session is found" in {
    list(s"$IndexerPath$OppilaitosPath/${o1.s}/osat", Map[String, String](), 401, Map.empty)
  }
  it should "deny access with the oppilaitos read role" in {
    list(s"$IndexerPath$OppilaitosPath/${o1.s}/osat", Map[String, String](), 403, addTestSession(Role.Koulutus.Read, OphOid))
  }

  "Koulutukset hakutiedot" should "return all hakutiedot related to koulutus" in {
    get(s"$IndexerPath$KoulutusPath/${k1.oid}/hakutiedot", headers = Seq(sessionHeader(indexerSession))) {
      status should equal(200)

      val hk1valintakokeet = HakukohdeDAO.get(hk1.oid, TilaFilter()).map(_._1).map(_.valintakokeet).getOrElse(Seq()).map(_.id).flatten
      val hk3valintakokeet = HakukohdeDAO.get(hk3.oid, TilaFilter()).map(_._1).map(_.valintakokeet).getOrElse(Seq()).map(_.id).flatten
      val hk5valintakokeet = HakukohdeDAO.get(hk5.oid, TilaFilter()).map(_._1).map(_.valintakokeet).getOrElse(Seq()).map(_.id).flatten
      val hk6valintakokeet = HakukohdeDAO.get(hk6.oid, TilaFilter()).map(_._1).map(_.valintakokeet).getOrElse(Seq()).map(_.id).flatten

      val expected = List(Hakutieto(
        toteutusOid = t1.oid,
        haut = Seq(HakutietoHaku(
          hakuOid = h1.oid,
          nimi = h1.nimi,
          hakutapaKoodiUri = TestData.JulkaistuHaku.hakutapaKoodiUri,
          tila = Julkaistu,
          koulutuksenAlkamiskausi = TestData.JulkaistuHaku.metadata.get.koulutuksenAlkamiskausi,
          hakulomaketyyppi = TestData.JulkaistuHaku.hakulomaketyyppi,
          hakulomakeAtaruId = TestData.JulkaistuHaku.hakulomakeAtaruId,
          hakulomakeKuvaus = TestData.JulkaistuHaku.hakulomakeKuvaus,
          hakulomakeLinkki = TestData.JulkaistuHaku.hakulomakeLinkki,
          organisaatioOid = h1.organisaatioOid,
          kohdejoukkoKoodiUri = TestData.JulkaistuHaku.kohdejoukkoKoodiUri,
          hakuajat = TestData.JulkaistuHaku.hakuajat,
          muokkaaja = h1.muokkaaja,
          modified = Some(h1.modified),
          hakukohteet = Seq(
            HakutietoHakukohde(
              hakukohdeOid = hk1.oid,
              toteutusOid = t1.oid,
              hakuOid = h1.oid,
              nimi = hk1.nimi,
              jarjestyspaikkaOid = Some(OtherOid),
              tila = hk1.tila,
              esikatselu = true,
              valintaperusteId = hk1.valintaperusteId,
              koulutuksenAlkamiskausi = TestData.JulkaistuHakukohde.metadata.get.koulutuksenAlkamiskausi,
              kaytetaanHaunAlkamiskautta = TestData.JulkaistuHakukohde.metadata.get.kaytetaanHaunAlkamiskautta,
              hakulomaketyyppi = TestData.JulkaistuHakukohde.hakulomaketyyppi,
              hakulomakeAtaruId = TestData.JulkaistuHakukohde.hakulomakeAtaruId,
              hakulomakeKuvaus = TestData.JulkaistuHakukohde.hakulomakeKuvaus,
              hakulomakeLinkki = TestData.JulkaistuHakukohde.hakulomakeLinkki,
              kaytetaanHaunHakulomaketta = TestData.JulkaistuHakukohde.kaytetaanHaunHakulomaketta,
              aloituspaikat = TestData.JulkaistuHakukohde.metadata.get.aloituspaikat,
              kaytetaanHaunAikataulua = TestData.JulkaistuHakukohde.kaytetaanHaunAikataulua,
              hakuajat = TestData.JulkaistuHakukohde.hakuajat,
              pohjakoulutusvaatimusKoodiUrit = TestData.JulkaistuHakukohde.pohjakoulutusvaatimusKoodiUrit,
              pohjakoulutusvaatimusTarkenne = TestData.JulkaistuHakukohde.pohjakoulutusvaatimusTarkenne,
              muokkaaja = hk1.muokkaaja,
              organisaatioOid = hk1.organisaatioOid,
              valintatapaKoodiUrit = TestData.AmmValintaperusteMetadata.valintatavat.flatMap(_.valintatapaKoodiUri),
              modified = Some(hk1.modified),
              kynnysehto = Map(Fi -> "Kynnysehto fi", Sv -> "Kynnysehto sv"),
              valintakoeIds = hk1valintakokeet))),

          HakutietoHaku(
            hakuOid = h2.oid,
            nimi = h2.nimi,
            hakutapaKoodiUri = TestData.JulkaistuHaku.hakutapaKoodiUri,
            tila = Arkistoitu,
            koulutuksenAlkamiskausi = TestData.JulkaistuHaku.metadata.get.koulutuksenAlkamiskausi,
            hakulomaketyyppi = TestData.JulkaistuHaku.hakulomaketyyppi,
            hakulomakeAtaruId = TestData.JulkaistuHaku.hakulomakeAtaruId,
            hakulomakeKuvaus = TestData.JulkaistuHaku.hakulomakeKuvaus,
            hakulomakeLinkki = TestData.JulkaistuHaku.hakulomakeLinkki,
            organisaatioOid = h2.organisaatioOid,
            kohdejoukkoKoodiUri = Some("haunkohdejoukko_17#1"),
            hakuajat = TestData.JulkaistuHaku.hakuajat,
            muokkaaja = h2.muokkaaja,
            modified = Some(h2.modified),
            hakukohteet = Seq(HakutietoHakukohde(
              hakukohdeOid = hk3.oid,
              toteutusOid = t1.oid,
              hakuOid = h2.oid,
              nimi = hk3.nimi,
              jarjestyspaikkaOid = Some(t1.tarjoajat.head),
              tila = hk3.tila,
              esikatselu = true,
              valintaperusteId = hk3.valintaperusteId,
              koulutuksenAlkamiskausi = TestData.JulkaistuHakukohde.metadata.get.koulutuksenAlkamiskausi,
              kaytetaanHaunAlkamiskautta = TestData.JulkaistuHakukohde.metadata.get.kaytetaanHaunAlkamiskautta,
              hakulomaketyyppi = TestData.JulkaistuHakukohde.hakulomaketyyppi,
              hakulomakeAtaruId = TestData.JulkaistuHakukohde.hakulomakeAtaruId,
              hakulomakeKuvaus = TestData.JulkaistuHakukohde.hakulomakeKuvaus,
              hakulomakeLinkki = TestData.JulkaistuHakukohde.hakulomakeLinkki,
              kaytetaanHaunHakulomaketta = TestData.JulkaistuHakukohde.kaytetaanHaunHakulomaketta,
              aloituspaikat = TestData.JulkaistuHakukohde.metadata.get.aloituspaikat,
              kaytetaanHaunAikataulua = TestData.JulkaistuHakukohde.kaytetaanHaunAikataulua,
              hakuajat = TestData.JulkaistuHakukohde.hakuajat,
              pohjakoulutusvaatimusKoodiUrit = TestData.JulkaistuHakukohde.pohjakoulutusvaatimusKoodiUrit,
              pohjakoulutusvaatimusTarkenne = TestData.JulkaistuHakukohde.pohjakoulutusvaatimusTarkenne,
              muokkaaja = hk3.muokkaaja,
              organisaatioOid = hk3.organisaatioOid,
              valintatapaKoodiUrit = TestData.AmmValintaperusteMetadata.valintatavat.flatMap(_.valintatapaKoodiUri),
              modified = Some(hk3.modified),
              kynnysehto = Map(Fi -> "Kynnysehto fi", Sv -> "Kynnysehto sv"),
              valintakoeIds = hk3valintakokeet),
              HakutietoHakukohde(
                hakukohdeOid = hk5.oid,
                toteutusOid = t1.oid,
                hakuOid = h2.oid,
                nimi = hk5.nimi,
                jarjestyspaikkaOid = Some(t1.tarjoajat.head),
                tila = hk5.tila,
                esikatselu = true,
                valintaperusteId = hk5.valintaperusteId,
                koulutuksenAlkamiskausi = TestData.JulkaistuHakukohde.metadata.get.koulutuksenAlkamiskausi,
                kaytetaanHaunAlkamiskautta = TestData.JulkaistuHakukohde.metadata.get.kaytetaanHaunAlkamiskautta,
                hakulomaketyyppi = TestData.JulkaistuHakukohde.hakulomaketyyppi,
                hakulomakeAtaruId = TestData.JulkaistuHakukohde.hakulomakeAtaruId,
                hakulomakeKuvaus = TestData.JulkaistuHakukohde.hakulomakeKuvaus,
                hakulomakeLinkki = TestData.JulkaistuHakukohde.hakulomakeLinkki,
                kaytetaanHaunHakulomaketta = TestData.JulkaistuHakukohde.kaytetaanHaunHakulomaketta,
                aloituspaikat = TestData.JulkaistuHakukohde.metadata.get.aloituspaikat,
                kaytetaanHaunAikataulua = TestData.JulkaistuHakukohde.kaytetaanHaunAikataulua,
                hakuajat = TestData.JulkaistuHakukohde.hakuajat,
                pohjakoulutusvaatimusKoodiUrit = TestData.JulkaistuHakukohde.pohjakoulutusvaatimusKoodiUrit,
                pohjakoulutusvaatimusTarkenne = TestData.JulkaistuHakukohde.pohjakoulutusvaatimusTarkenne,
                muokkaaja = hk5.muokkaaja,
                organisaatioOid = hk5.organisaatioOid,
                valintatapaKoodiUrit = TestData.AmmValintaperusteMetadata.valintatavat.flatMap(_.valintatapaKoodiUri),
                modified = Some(hk5.modified),
                kynnysehto = Map(Fi -> "Kynnysehto fi", Sv -> "Kynnysehto sv"),
                valintakoeIds = hk5valintakokeet))),

          HakutietoHaku(
            hakuOid = h3.oid,
            nimi = h3.nimi,
            hakutapaKoodiUri = TestData.JulkaistuHaku.hakutapaKoodiUri,
            koulutuksenAlkamiskausi = TestData.JulkaistuHaku.metadata.get.koulutuksenAlkamiskausi,
            hakulomaketyyppi = TestData.JulkaistuHaku.hakulomaketyyppi,
            hakulomakeAtaruId = TestData.JulkaistuHaku.hakulomakeAtaruId,
            hakulomakeKuvaus = TestData.JulkaistuHaku.hakulomakeKuvaus,
            hakulomakeLinkki = TestData.JulkaistuHaku.hakulomakeLinkki,
            organisaatioOid = h3.organisaatioOid,
            kohdejoukkoKoodiUri = Some("haunkohdejoukko_05#2"),
            hakuajat = TestData.JulkaistuHaku.hakuajat,
            muokkaaja = h3.muokkaaja,
            modified = Some(h3.modified),
            hakukohteet = Seq(HakutietoHakukohde(
              hakukohdeOid = hk6.oid,
              toteutusOid = t1.oid,
              hakuOid = h3.oid,
              nimi = hk6.nimi,
              jarjestyspaikkaOid = Some(OtherOid),
              tila = hk6.tila,
              esikatselu = true,
              valintaperusteId = hk6.valintaperusteId,
              koulutuksenAlkamiskausi = TestData.JulkaistuHakukohde.metadata.get.koulutuksenAlkamiskausi,
              kaytetaanHaunAlkamiskautta = TestData.JulkaistuHakukohde.metadata.get.kaytetaanHaunAlkamiskautta,
              hakulomaketyyppi = TestData.JulkaistuHakukohde.hakulomaketyyppi,
              hakulomakeAtaruId = TestData.JulkaistuHakukohde.hakulomakeAtaruId,
              hakulomakeKuvaus = TestData.JulkaistuHakukohde.hakulomakeKuvaus,
              hakulomakeLinkki = TestData.JulkaistuHakukohde.hakulomakeLinkki,
              kaytetaanHaunHakulomaketta = TestData.JulkaistuHakukohde.kaytetaanHaunHakulomaketta,
              aloituspaikat = TestData.JulkaistuHakukohde.metadata.get.aloituspaikat,
              kaytetaanHaunAikataulua = TestData.JulkaistuHakukohde.kaytetaanHaunAikataulua,
              hakuajat = TestData.JulkaistuHakukohde.hakuajat,
              pohjakoulutusvaatimusKoodiUrit = TestData.JulkaistuHakukohde.pohjakoulutusvaatimusKoodiUrit,
              pohjakoulutusvaatimusTarkenne = TestData.JulkaistuHakukohde.pohjakoulutusvaatimusTarkenne,
              muokkaaja = hk6.muokkaaja,
              organisaatioOid = hk6.organisaatioOid,
              valintatapaKoodiUrit = TestData.AmmValintaperusteMetadata.valintatavat.flatMap(_.valintatapaKoodiUri),
              modified = Some(hk6.modified),
              kynnysehto = Map(Fi -> "Kynnysehto fi", Sv -> "Kynnysehto sv"),
              valintakoeIds = hk6valintakokeet))))),
        Hakutieto(
          t2.oid,
          List(HakutietoHaku(
            hakuOid = h1.oid,
            nimi = h1.nimi,
            hakutapaKoodiUri = TestData.JulkaistuHaku.hakutapaKoodiUri,
            tila = Julkaistu,
            koulutuksenAlkamiskausi = TestData.JulkaistuHaku.metadata.get.koulutuksenAlkamiskausi,
            hakulomaketyyppi = TestData.JulkaistuHaku.hakulomaketyyppi,
            hakulomakeAtaruId = TestData.JulkaistuHaku.hakulomakeAtaruId,
            hakulomakeKuvaus = TestData.JulkaistuHaku.hakulomakeKuvaus,
            hakulomakeLinkki = TestData.JulkaistuHaku.hakulomakeLinkki,
            organisaatioOid = h1.organisaatioOid,
            kohdejoukkoKoodiUri = Some("haunkohdejoukko_17#1"),
            hakuajat = TestData.JulkaistuHaku.hakuajat,
            muokkaaja = h1.muokkaaja,
            modified = Some(h1.modified),
            hakukohteet = Seq()))))

      read[List[Hakutieto]](body) shouldMatchTo(expected)
    }
  }

  "ePerusteId list" should "return all ePerusteIds related to existing koulutukset" in {
    list(s"$IndexerPath/koulutukset/eperusteet", Map[String,String](), List("11", "12", "123"), indexerSession)
  }
}
