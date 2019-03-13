package fi.oph.kouta.integration

import fi.oph.kouta.domain._
import fi.oph.kouta.{KonfoIndexingQueues, OrganisaatioServiceMock, TestData}
import org.json4s.jackson.Serialization.read

class ListSpec extends KoutaIntegrationSpec with EverythingFixture with OrganisaatioServiceMock with KonfoIndexingQueues {

  val LonelyOid = "1.2.246.562.10.99999999999"
  val UnknownOid = "1.2.246.562.10.99999999998"

  var k1, k2, k3, k4, k5 :KoulutusListItem = null
  var t1, t2, t3, t4     :ToteutusListItem = null
  var h1, h2, h3, h4     :HakuListItem = null
  var v1, v2, v3, v4     :ValintaperusteListItem = null
  var hk1, hk2, hk3, hk4 :HakukohdeListItem = null

  override def beforeAll() = {
    super.beforeAll()
    startServiceMocking()

    List(ParentOid, ChildOid, GrandChildOid).foreach(mockOrganisaatioResponse(_))
    mockOrganisaatioResponse(LonelyOid, singleOidOrganisaatioResponse(LonelyOid))
    mockOrganisaatioResponse(UnknownOid, NotFoundOrganisaatioResponse)

    createTestData()
  }

  override def afterAll() = {
    super.afterAll()
    stopServiceMocking()
    truncateDatabase()
  }

  def createTestData() = {
    k1 = addToList(koulutus(false, ParentOid, Julkaistu))
    k2 = addToList(koulutus(false, ChildOid, Arkistoitu))
    k3 = addToList(koulutus(false, GrandChildOid, Tallennettu))
    k4 = addToList(koulutus(false, LonelyOid, Julkaistu))
    k5 = addToList(koulutus(true, LonelyOid, Julkaistu))
    t1 = addToList(toteutus(k1.oid.toString, Julkaistu, ParentOid))
    t2 = addToList(toteutus(k1.oid.toString, Arkistoitu, ChildOid))
    t3 = addToList(toteutus(k1.oid.toString, Tallennettu, GrandChildOid))
    t4 = addToList(toteutus(k4.oid.toString, Julkaistu, LonelyOid))
    h1 = addToList(haku(Julkaistu, ParentOid))
    h2 = addToList(haku(Arkistoitu, ChildOid))
    h3 = addToList(haku(Tallennettu, GrandChildOid).copy(kohdejoukkoKoodiUri = Some("haunkohdejoukko_05#2"), kohdejoukonTarkenneKoodiUri = None))
    h4 = addToList(haku(Julkaistu, LonelyOid))
    v1 = addToList(valintaperuste(Julkaistu, ParentOid))
    v2 = addToList(valintaperuste(Arkistoitu, ChildOid))
    v3 = addToList(valintaperuste(Tallennettu, GrandChildOid).copy(kohdejoukkoKoodiUri = Some("haunkohdejoukko_05#2"), kohdejoukonTarkenneKoodiUri = None))
    v4 = addToList(valintaperuste(Julkaistu, LonelyOid))

    hk1 = addToList(hakukohde(t1.oid.toString, h1.oid.toString, v1.id, ParentOid))
    hk2 = addToList(hakukohde(t2.oid.toString, h1.oid.toString, v1.id, ChildOid))
    hk3 = addToList(hakukohde(t1.oid.toString, h2.oid.toString, v1.id, GrandChildOid))
    hk4 = addToList(hakukohde(t4.oid.toString, h1.oid.toString, v1.id, LonelyOid))
  }

  "Koulutus list" should "list all koulutukset for authorized organizations 1" in {
    list(KoulutusPath, Map("organisaatioOid" -> ChildOid), List(k1, k2, k3, k5))
  }
  it should "list all koulutukset for authorized organizations 2" in {
    list(KoulutusPath, Map("organisaatioOid" -> LonelyOid), List(k4, k5))
  }
  it should "return forbidden if oid is unknown" in {
    list(KoulutusPath, Map("organisaatioOid" -> UnknownOid), 403)
  }
  it should "return 404 if oid not given" in {
    list(KoulutusPath, Map[String,String](), 404)
  }

  "Toteutus list" should "list all toteutukset for authorized organizations" in {
    list(ToteutusPath, Map("organisaatioOid" -> ChildOid), List(t1, t2, t3))
  }
  it should "list all toteutukset for authorized organizations 2" in {
    list(ToteutusPath, Map("organisaatioOid" -> LonelyOid), List(t4))
  }
  it should "return forbidden if oid is unknown" in {
    list(ToteutusPath, Map("organisaatioOid" -> UnknownOid), 403)
  }
  it should "return 404 if oid not given" in {
    list(ToteutusPath, Map[String,String](), 404)
  }

  "Haku list" should "list all haut for authorized organizations" in {
    list(HakuPath, Map("organisaatioOid" -> ChildOid), List(h1, h2, h3))
  }
  it should "list all haut for authorized organizations 2" in {
    list(HakuPath, Map("organisaatioOid" -> LonelyOid), List(h4))
  }
  it should "return forbidden if oid is unknown" in {
    list(HakuPath, Map("organisaatioOid" -> UnknownOid), 403)
  }
  it should "return 404 if oid not given" in {
    list(HakuPath, Map[String,String](), 404)
  }

  "Valintaperuste list" should "list all valintaperustekuvaukset for authorized organizations" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid), List(v1, v2, v3))
  }
  it should "list all valintaperustekuvaukset for authorized organizations 2" in {
    list(ValintaperustePath, Map("organisaatioOid" -> LonelyOid), List(v4))
  }
  it should "list all valintaperustekuvaukset that can be joined to given haku" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid, "hakuOid" -> h2.oid.toString), List(v1, v2))
  }
  it should "list all valinteperustekuvaukset that can be joiden to given haku even when kohdejoukko is null" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid, "hakuOid" -> h3.oid.toString), List(v3))
  }
  it should "return forbidden if oid is unknown" in {
    list(ValintaperustePath, Map("organisaatioOid" -> UnknownOid), 403)
  }
  it should "return 404 if oid not given" in {
    list(ValintaperustePath, Map[String,String](), 404)
  }

  "Valintaperustetta käyttävät hakukohteet list" should "list all hakukohteet using given valintaperuste id" in {
    list(s"$ValintaperustePath/${v1.id.toString}/hakukohteet", Map[String,String](), List(hk1, hk2, hk3, hk4))
  }

  "Koulutuksen toteutukset list" should "list all toteutukset for this and child organizations" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> ParentOid), List(t1, t2, t3))
  }
  it should "not list toteutukset for parent organizations" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> GrandChildOid), List(t3))
  }
  it should "return forbidden if organisaatio oid is unknown" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> UnknownOid), 403)
  }
  it should "return all toteutukset if organisaatio oid not given" in { //TODO: oikeudet
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map[String,String](), List(t1, t2, t3))
  }

  "Toteutukseen liitetyt haut" should "list all haut mapped to given toteutus" in {
    list(s"$ToteutusPath/${t1.oid}/haut", Map[String,String](), List(h1, h2))
  }

  "Toteutukseen liitetyt hakukohteet" should "list all hakukohteet mapped to given toteutus" in {
    list(s"$ToteutusPath/${t1.oid}/hakukohteet", Map[String,String](), List(hk1, hk3))
  }

  "Hakuun liitetyt hakukohteet" should "list all hakukohteet mapped to given haku for authorized organizations" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map("organisaatioOid" -> ParentOid), List(hk1, hk2))
  }
  it should "list all hakukohteet mapped to given haku for authorized organizations 2" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map("organisaatioOid" -> LonelyOid), List(hk4))
  }
  it should "not list hakukohteet belonging to parent organisations" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map("organisaatioOid" -> ChildOid), List(hk2))
  }
  it should "return forbidden if organisaatio oid is unknown" in {
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map("organisaatioOid" -> UnknownOid), 403)
  }
  it should "return all if organisaatio oid not given" in { //TODO: OIKEUDET!
    list(s"$HakuPath/${h1.oid}/hakukohteet", Map[String,String](), List(hk1, hk2, hk4))
  }

  "Hakuun kuuluvat koulutukset" should "list all koulutukset mapped to given haku by hakukohde" in {
    list(s"$HakuPath/${h1.oid}/koulutukset", Map[String,String](), List(k1, k4))
  }

  //TODO: Paremmat testit sitten, kun indeksointi on vakiintunut muotoonsa
  "Koulutukset hakutiedot" should "return all hakutiedot related to koulutus" in {
    get(s"$KoulutusPath/${k1.oid}/hakutiedot") {
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
          hakulomake = TestData.JulkaistuHaku.hakulomake,
          organisaatioOid = h1.organisaatioOid,
          hakuajat = TestData.JulkaistuHaku.hakuajat,
          muokkaaja = h1.muokkaaja,
          modified = Some(h1.modified),
          hakukohteet = Seq(HakutietoHakukohde(
            hakukohdeOid = hk1.oid,
            nimi = hk1.nimi,
            alkamiskausiKoodiUri = TestData.JulkaistuHakukohde.alkamiskausiKoodiUri,
            alkamisvuosi = TestData.JulkaistuHakukohde.alkamisvuosi,
            hakulomaketyyppi = TestData.JulkaistuHakukohde.hakulomaketyyppi,
            hakulomake = TestData.JulkaistuHakukohde.hakulomake,
            aloituspaikat = TestData.JulkaistuHakukohde.aloituspaikat,
            ensikertalaisenAloituspaikat = TestData.JulkaistuHakukohde.ensikertalaisenAloituspaikat,
            kaytetaanHaunAikataulua = TestData.JulkaistuHakukohde.kaytetaanHaunAikataulua,
            hakuajat = TestData.JulkaistuHakukohde.hakuajat,
            muokkaaja = hk1.muokkaaja,
            organisaatioOid = hk1.organisaatioOid,
            modified = Some(hk1.modified)))))))

      read[List[Hakutieto]](body) should equal(expected)
    }
  }
}
