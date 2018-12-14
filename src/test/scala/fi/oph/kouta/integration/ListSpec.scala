package fi.oph.kouta.integration

import fi.oph.kouta.OrganisaatioServiceMock
import fi.oph.kouta.domain._

class ListSpec extends KoutaIntegrationSpec with EverythingFixture with OrganisaatioServiceMock {

  val parentOid = "1.2.246.562.10.594252633210"
  val childOid = "1.2.246.562.10.81934895871"
  val grandchildOid = "1.2.246.562.10.67603619189"
  val lonelyOid = "1.2.246.562.10.99999999999"
  val unknownOid = "1.2.246.562.10.99999999998"

  var k1, k2, k3, k4, k5:OidListItem = null
  var t1, t2, t3, t4:OidListItem = null
  var h1, h2, h3, h4:OidListItem = null
  var v1, v2, v3, v4:IdListItem = null

  override def beforeAll() = {
    super.beforeAll()
    startServiceMocking()
    List(parentOid, childOid, grandchildOid).foreach(mockOrganisaatioServiceFromResource(_))
    mockOrganisaatioService(lonelyOid, singleOidOrganisaatioResponse(lonelyOid))
    mockOrganisaatioService(unknownOid, EmptyOrganisaatioResponse)
    createTestData()
  }

  override def afterAll() = {
    super.afterAll()
    stopServiceMocking()
    truncateDatabase()
  }

  def createTestData() = {
    k1 = addToList(koulutus(false, parentOid, Julkaistu))
    k2 = addToList(koulutus(false, childOid, Arkistoitu))
    k3 = addToList(koulutus(false, grandchildOid, Tallennettu))
    k4 = addToList(koulutus(false, lonelyOid, Julkaistu))
    k5 = addToList(koulutus(true, lonelyOid, Julkaistu))
    t1 = addToList(toteutus(k1.oid, Julkaistu, parentOid))
    t2 = addToList(toteutus(k1.oid, Arkistoitu, childOid))
    t3 = addToList(toteutus(k1.oid, Tallennettu, grandchildOid))
    t4 = addToList(toteutus(k4.oid, Julkaistu, lonelyOid))
    h1 = addToList(haku(Julkaistu, parentOid))
    h2 = addToList(haku(Arkistoitu, childOid))
    h3 = addToList(haku(Tallennettu, grandchildOid))
    h4 = addToList(haku(Julkaistu, lonelyOid))
    v1 = addToList(valintaperuste(Julkaistu, parentOid))
    v2 = addToList(valintaperuste(Arkistoitu, childOid))
    v3 = addToList(valintaperuste(Tallennettu, grandchildOid))
    v4 = addToList(valintaperuste(Julkaistu, lonelyOid))
  }

  "Koulutus list" should "list all koulutukset for authorized organizations 1" in {
    list(KoulutusPath, Map("organisaatioOid" -> childOid), List(k1, k2, k3, k5))
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
    list(ToteutusPath, Map("organisaatioOid" -> childOid), List(t1, t2, t3))
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
    list(HakuPath, Map("organisaatioOid" -> childOid), List(h1, h2, h3))
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
    list(ValintaperustePath, Map("organisaatioOid" -> childOid), List(v1, v2, v3))
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
}