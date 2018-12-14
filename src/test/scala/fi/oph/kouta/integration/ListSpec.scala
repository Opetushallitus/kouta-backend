package fi.oph.kouta.integration

import fi.oph.kouta.OrganisaatioServiceMock
import fi.oph.kouta.domain._

class ListSpec extends KoutaIntegrationSpec with EverythingFixture with OrganisaatioServiceMock {

  val lonelyOid = "1.2.246.562.10.99999999999"
  val unknownOid = "1.2.246.562.10.99999999998"

  var k1, k2, k3, k4, k5:OidListItem = null
  var t1, t2, t3, t4:OidListItem = null
  var h1, h2, h3, h4:OidListItem = null
  var v1, v2, v3, v4:IdListItem = null

  override def beforeAll() = {
    super.beforeAll()
    startServiceMocking()

    List(ParentOid, ChildOid, GrandChildOid).foreach(mockOrganisaatioResponse(_))
    mockOrganisaatioResponse(lonelyOid, singleOidOrganisaatioResponse(lonelyOid))
    mockOrganisaatioResponse(unknownOid, NotFoundOrganisaatioResponse)

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
    k4 = addToList(koulutus(false, lonelyOid, Julkaistu))
    k5 = addToList(koulutus(true, lonelyOid, Julkaistu))
    t1 = addToList(toteutus(k1.oid, Julkaistu, ParentOid))
    t2 = addToList(toteutus(k1.oid, Arkistoitu, ChildOid))
    t3 = addToList(toteutus(k1.oid, Tallennettu, GrandChildOid))
    t4 = addToList(toteutus(k4.oid, Julkaistu, lonelyOid))
    h1 = addToList(haku(Julkaistu, ParentOid))
    h2 = addToList(haku(Arkistoitu, ChildOid))
    h3 = addToList(haku(Tallennettu, GrandChildOid))
    h4 = addToList(haku(Julkaistu, lonelyOid))
    v1 = addToList(valintaperuste(Julkaistu, ParentOid))
    v2 = addToList(valintaperuste(Arkistoitu, ChildOid))
    v3 = addToList(valintaperuste(Tallennettu, GrandChildOid))
    v4 = addToList(valintaperuste(Julkaistu, lonelyOid))
  }

  "Koulutus list" should "list all koulutukset for authorized organizations 1" in {
    list(KoulutusPath, Map("organisaatioOid" -> ChildOid), List(k1, k2, k3, k5))
  }
  it should "list all koulutukset for authorized organizations 2" in {
    list(KoulutusPath, Map("organisaatioOid" -> lonelyOid), List(k4, k5))
  }
  it should "return forbidden if oid is unknown" in {
    list(KoulutusPath, Map("organisaatioOid" -> unknownOid), 403)
  }
  it should "return 404 if oid not given" in {
    list(KoulutusPath, Map[String,String](), 404)
  }

  "Toteutus list" should "list all toteutukset for authorized organizations" in {
    list(ToteutusPath, Map("organisaatioOid" -> ChildOid), List(t1, t2, t3))
  }
  it should "list all toteutukset for authorized organizations 2" in {
    list(ToteutusPath, Map("organisaatioOid" -> lonelyOid), List(t4))
  }
  it should "return forbidden if oid is unknown" in {
    list(ToteutusPath, Map("organisaatioOid" -> unknownOid), 403)
  }
  it should "return 404 if oid not given" in {
    list(ToteutusPath, Map[String,String](), 404)
  }

  "Haku list" should "list all haut for authorized organizations" in {
    list(HakuPath, Map("organisaatioOid" -> ChildOid), List(h1, h2, h3))
  }
  it should "list all haut for authorized organizations 2" in {
    list(HakuPath, Map("organisaatioOid" -> lonelyOid), List(h4))
  }
  it should "return forbidden if oid is unknown" in {
    list(HakuPath, Map("organisaatioOid" -> unknownOid), 403)
  }
  it should "return 404 if oid not given" in {
    list(HakuPath, Map[String,String](), 404)
  }

  "Valintaperuste list" should "list all valintaperustekuvaukset for authorized organizations" in {
    list(ValintaperustePath, Map("organisaatioOid" -> ChildOid), List(v1, v2, v3))
  }
  it should "list all valintaperustekuvaukset for authorized organizations 2" in {
    list(ValintaperustePath, Map("organisaatioOid" -> lonelyOid), List(v4))
  }
  it should "return forbidden if oid is unknown" in {
    list(ValintaperustePath, Map("organisaatioOid" -> unknownOid), 403)
  }
  it should "return 404 if oid not given" in {
    list(ValintaperustePath, Map[String,String](), 404)
  }

  "Koulutuksen toteutukset list" should "list all toteutukset for this and child organizations" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> ParentOid), List(t1, t2, t3))
  }
  it should "not list toteutukset for parent organizations" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> GrandChildOid), List(t3))
  }
  it should "return forbidden if organisaatio oid is unknown" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map("organisaatioOid" -> unknownOid), 403)
  }
  it should "return 404 if organisaatio oid not given" in {
    list(s"$KoulutusPath/${k1.oid}/toteutukset", Map[String,String](), 404)
  }
}